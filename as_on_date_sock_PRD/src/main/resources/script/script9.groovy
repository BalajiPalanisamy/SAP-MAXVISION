import com.sap.gateway.ip.core.customdev.util.Message
import groovy.util.XmlSlurper
import groovy.xml.MarkupBuilder

import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.TimeZone

def Message processData(Message message) {

    String body = message.getBody(String)?.trim()

    if (!body) {
        throw new IllegalArgumentException("Input payload is empty")
    }

    /*
     * Remove XML declaration and add a common root when the input contains
     * multiple Receipt nodes without a root element.
     */
    body = body.replaceFirst(/(?s)^\s*<\?xml[^>]*\?>\s*/, "")

    if (!body.startsWith("<root")) {
        body = "<root>${body}</root>"
    }

    def inputXml = new XmlSlurper(false, false).parseText(body)
    def allReceipts = inputXml.Receipt

    if (allReceipts == null || allReceipts.size() == 0) {
        throw new IllegalArgumentException("No Receipt records found")
    }

    /*
     * Read the branch from the first record.
     */
    String branchName =
            allReceipts[0].BranchName.text()?.trim()

    if (!branchName) {
        throw new IllegalArgumentException("BranchName is missing")
    }

    /*
     * Consolidate only OPD receipts for the selected branch.
     */
    def receipts = allReceipts.findAll { receipt ->

        String currentBranch =
                receipt.BranchName.text()?.trim()

        String billingHead =
                receipt.BillingHeadName.text()?.trim()

        return currentBranch == branchName &&
               billingHead.equalsIgnoreCase("IPD Billing")
    }

    if (receipts == null || receipts.size() == 0) {
        throw new IllegalArgumentException(
                "No OPD receipts found for branch ${branchName}"
        )
    }

    /*
     * Payment-mode totals.
     */
    BigDecimal cashTotal = 0.00G
    BigDecimal cardTotal = 0.00G

    BigDecimal chequeTotal = 0.00G
    BigDecimal neftTotal = 0.00G
    BigDecimal payTMTotal = 0.00G
    BigDecimal gpayTotal = 0.00G
    BigDecimal phonepeTotal = 0.00G
    BigDecimal othersTotal = 0.00G

    BigDecimal sourceTotalPaid = 0.00G

    receipts.each { receipt ->

        cashTotal += toDecimal(receipt.Cash.text())
        cardTotal += toDecimal(receipt.Card.text())

        chequeTotal += toDecimal(receipt.Cheque.text())
        neftTotal += toDecimal(receipt.NEFT.text())
        payTMTotal += toDecimal(receipt.PayTM.text())
        gpayTotal += toDecimal(receipt.Gpay.text())
        phonepeTotal += toDecimal(receipt.Phonepe.text())
        othersTotal += toDecimal(receipt.Others.text())

        sourceTotalPaid += toDecimal(receipt.TotalPaid.text())
    }

    cashTotal = scaleAmount(cashTotal)
    cardTotal = scaleAmount(cardTotal)

    chequeTotal = scaleAmount(chequeTotal)
    neftTotal = scaleAmount(neftTotal)
    payTMTotal = scaleAmount(payTMTotal)
    gpayTotal = scaleAmount(gpayTotal)
    phonepeTotal = scaleAmount(phonepeTotal)
    othersTotal = scaleAmount(othersTotal)

    sourceTotalPaid = scaleAmount(sourceTotalPaid)

    /*
     * Combine these payment modes into one SAP GL item:
     * Cheque + NEFT + PayTM + Gpay + Phonepe + Others.
     */
    BigDecimal otherPaymentTotal = scaleAmount(
            chequeTotal +
            neftTotal +
            payTMTotal +
            gpayTotal +
            phonepeTotal +
            othersTotal
    )

    /*
     * Total of all consolidated payment-mode amounts.
     */
    BigDecimal consolidatedTotal = scaleAmount(
            cashTotal +
            cardTotal +
            otherPaymentTotal
    )

    if (consolidatedTotal.compareTo(BigDecimal.ZERO) == 0) {
        throw new IllegalArgumentException(
                "Consolidated amount is zero. No journal entry can be created."
        )
    }

    /*
     * Validate payment-mode total against the source TotalPaid.
     */
    if (consolidatedTotal.compareTo(sourceTotalPaid) != 0) {

        message.setProperty(
                "AmountValidationStatus",
                "MISMATCH"
        )

        message.setProperty(
                "AmountValidationWarning",
                "Payment-mode total ${formatAmount(consolidatedTotal)} " +
                "does not match TotalPaid ${formatAmount(sourceTotalPaid)}"
        )

        throw new IllegalArgumentException(
                "Consolidation mismatch: Payment-mode total " +
                "${formatAmount(consolidatedTotal)} does not match " +
                "TotalPaid ${formatAmount(sourceTotalPaid)}"
        )
    }

    message.setProperty(
            "AmountValidationStatus",
            "MATCHED"
    )

    String receiptDateTime =
            receipts[0].ReceiptDate.text()?.trim()

    if (!receiptDateTime || receiptDateTime.length() < 10) {
        throw new IllegalArgumentException(
                "Valid ReceiptDate is missing"
        )
    }

    String documentDate =
            receiptDateTime.substring(0, 10)

    String documentReferenceDate =
            documentDate.replace("-", "")

    String creationDateTime =
            getCurrentUtcTimestamp()

    String uniqueId =
            "COLLECTION_${documentReferenceDate}_${System.currentTimeMillis()}"

    StringWriter writer = new StringWriter()
    MarkupBuilder xml = new MarkupBuilder(writer)

    xml.setDoubleQuotes(true)

    xml.mkp.xmlDeclaration(
            version: "1.0",
            encoding: "UTF-8"
    )

    xml."soapenv:Envelope"(
            "xmlns:soapenv":
                    "http://schemas.xmlsoap.org/soap/envelope/",
            "xmlns:sfin":
                    "http://sap.com/xi/SAPSCORE/SFIN"
    ) {

        "soapenv:Header"()

        "soapenv:Body" {

            "sfin:JournalEntryBulkCreateRequest" {

                // Bulk Message Header
                MessageHeader {
                    ID(uniqueId)
                    CreationDateTime(creationDateTime)
                    TestDataIndicator("false")
                    SenderBusinessSystemID("CPI_DEV")
                }

                JournalEntryCreateRequest {

                    // Journal Entry Message Header
                    MessageHeader {
                        ID("SUB_COLLECTION")
                        CreationDateTime(creationDateTime)
                    }

                    JournalEntry {

                        OriginalReferenceDocumentType("BKPFF")
                        OriginalReferenceDocument("Hospital Collection")
                        OriginalReferenceDocumentLogicalSystem("CPI_DEV")
                        BusinessTransactionType("RFBU")
                        AccountingDocumentType("SA")
                        DocumentReferenceID(documentReferenceDate)
                        DocumentHeaderText("Netram Collection")
                        CreatedByUser("CC0000000044")
                        CompanyCode("MX01")
                        DocumentDate(documentDate)
                        PostingDate(documentDate)
                        Reference1InDocumentHeader("NETRAM COLLECTION")
                        Reference2InDocumentHeader(documentReferenceDate)

                        int itemNumber = 1

                        /*
                         * Scenario 1: Cash exists.
                         */
                        if (cashTotal.compareTo(BigDecimal.ZERO) != 0) {

                            Item {
                                ReferenceDocumentItem(
                                        itemNumber.toString()
                                )

                                GLAccount("A2180003")

                                AmountInTransactionCurrency(
                                        currencyCode: "INR",
                                        formatAmount(cashTotal)
                                )

                                DebitCreditCode(
                                        getDebitCreditCode(cashTotal)
                                )

                                DocumentItemText("Cash Collection")

                                AccountAssignment {
                                    ProfitCenter("PC-SO01-OP")
                                }
                            }

                            itemNumber++
                        }

                        /*
                         * Scenario 2: Card exists.
                         */
                        if (cardTotal.compareTo(BigDecimal.ZERO) != 0) {

                            Item {
                                ReferenceDocumentItem(
                                        itemNumber.toString()
                                )

                                GLAccount("A2180004")

                                AmountInTransactionCurrency(
                                        currencyCode: "INR",
                                        formatAmount(cardTotal)
                                )

                                DebitCreditCode(
                                        getDebitCreditCode(cardTotal)
                                )

                                DocumentItemText("Card Collection")

                                AccountAssignment {
                                    ProfitCenter("PC-SO01-OP")
                                }
                            }

                            itemNumber++
                        }

                        /*
                         * Scenario 3:
                         * Cheque + NEFT + PayTM + Gpay +
                         * Phonepe + Others exists.
                         */
                        if (otherPaymentTotal.compareTo(BigDecimal.ZERO) != 0) {

                            Item {
                                ReferenceDocumentItem(
                                        itemNumber.toString()
                                )

                                GLAccount("A2180005")

                                AmountInTransactionCurrency(
                                        currencyCode: "INR",
                                        formatAmount(otherPaymentTotal)
                                )

                                DebitCreditCode(
                                        getDebitCreditCode(otherPaymentTotal)
                                )

                                DocumentItemText(
                                        "Cheque NEFT PayTM Gpay Phonepe Others"
                                )

                                AccountAssignment {
                                    ProfitCenter("PC-SO01-OP")
                                }
                            }

                            itemNumber++
                        }

                        /*
                         * Balancing Hospital OPD item.
                         *
                         * Positive payment total:
                         * Payment GLs are debit and OPD is credit.
                         *
                         * Negative payment total:
                         * Payment GLs are credit and OPD is debit.
                         */
                        BigDecimal balancingAmount =
                                scaleAmount(consolidatedTotal.negate())

                        Item {
                            ReferenceDocumentItem(
                                    itemNumber.toString()
                            )

                            GLAccount("A2210005")

                            AmountInTransactionCurrency(
                                    currencyCode: "INR",
                                    formatAmount(balancingAmount)
                            )

                            DebitCreditCode(
                                    getDebitCreditCode(balancingAmount)
                            )

                            DocumentItemText("Hospital IPD")

                            AccountAssignment {
                                ProfitCenter("PC-SO01-OP")
                            }
                        }
                    }
                }
            }
        }
    }

    message.setBody(writer.toString())

    message.setHeader(
            "Content-Type",
            "text/xml;charset=UTF-8"
    )

    /*
     * Store BranchName in the message header and property.
     */
    message.setHeader("BranchName", branchName)
    message.setProperty("BranchName", branchName)

    /*
     * Store all calculated totals for monitoring.
     */
    message.setProperty(
            "ConsolidatedReceiptCount",
            receipts.size().toString()
    )

    message.setProperty(
            "CashTotal",
            formatAmount(cashTotal)
    )

    message.setProperty(
            "CardTotal",
            formatAmount(cardTotal)
    )

    message.setProperty(
            "ChequeTotal",
            formatAmount(chequeTotal)
    )

    message.setProperty(
            "NEFTTotal",
            formatAmount(neftTotal)
    )

    message.setProperty(
            "PayTMTotal",
            formatAmount(payTMTotal)
    )

    message.setProperty(
            "GpayTotal",
            formatAmount(gpayTotal)
    )

    message.setProperty(
            "PhonepeTotal",
            formatAmount(phonepeTotal)
    )

    message.setProperty(
            "OthersTotal",
            formatAmount(othersTotal)
    )

    message.setProperty(
            "OtherPaymentTotal",
            formatAmount(otherPaymentTotal)
    )

    message.setProperty(
            "SourceTotalPaid",
            formatAmount(sourceTotalPaid)
    )

    message.setProperty(
            "ConsolidatedTotal",
            formatAmount(consolidatedTotal)
    )

    return message
}

/*
 * Safely convert an XML value to BigDecimal.
 */
BigDecimal toDecimal(String value) {

    String normalizedValue = value?.trim()

    if (!normalizedValue) {
        return 0.00G
    }

    try {
        return new BigDecimal(normalizedValue)
    } catch (Exception exception) {
        throw new IllegalArgumentException(
                "Invalid amount value: ${normalizedValue}"
        )
    }
}

/*
 * Return S for positive amounts and H for negative amounts.
 */
String getDebitCreditCode(BigDecimal amount) {

    return amount.signum() >= 0 ? "S" : "H"
}

/*
 * Format amounts with two decimal places.
 */
BigDecimal scaleAmount(BigDecimal amount) {

    return amount.setScale(
            2,
            RoundingMode.HALF_UP
    )
}

String formatAmount(BigDecimal amount) {

    return scaleAmount(amount).toPlainString()
}

/*
 * Generate the current UTC timestamp.
 */
String getCurrentUtcTimestamp() {

    SimpleDateFormat formatter =
            new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
            )

    formatter.setTimeZone(
            TimeZone.getTimeZone("UTC")
    )

    return formatter.format(new Date())
}