import com.sap.gateway.ip.core.customdev.util.Message
import groovy.util.XmlSlurper
import groovy.xml.MarkupBuilder

import java.text.SimpleDateFormat
import java.math.RoundingMode
import java.util.TimeZone

def Message processData(Message message) {

    String body = message.getBody(String)?.trim()

    if (!body) {
        throw new IllegalArgumentException("Input payload is empty")
    }

    /*
     * The input contains multiple <Receipt> elements without one common root.
     * Remove any XML declaration and temporarily wrap all records with <root>.
     */
    body = body.replaceFirst(/(?s)^\s*<\?xml[^>]*\?>\s*/, "")

    if (!body.startsWith("<root")) {
        body = "<root>${body}</root>"
    }

    def inputXml = new XmlSlurper(false, false).parseText(body)

    def receipts = inputXml.Receipt

    if (receipts == null || receipts.size() == 0) {
        throw new IllegalArgumentException("No Receipt records found in input payload")
    }

    BigDecimal cashTotal = 0.00G
    BigDecimal cardTotal = 0.00G
    BigDecimal otherPaymentTotal = 0.00G
    BigDecimal sourceTotalPaid = 0.00G

    receipts.each { receipt ->

        cashTotal += toDecimal(receipt.Cash.text())
        cardTotal += toDecimal(receipt.Card.text())

        otherPaymentTotal +=
                toDecimal(receipt.Cheque.text()) +
                toDecimal(receipt.NEFT.text()) +
                toDecimal(receipt.PayTM.text()) +
                toDecimal(receipt.Gpay.text()) +
                toDecimal(receipt.Phonepe.text()) +
                toDecimal(receipt.Others.text())

        sourceTotalPaid += toDecimal(receipt.TotalPaid.text())
    }

    cashTotal = cashTotal.setScale(2, RoundingMode.HALF_UP)
    cardTotal = cardTotal.setScale(2, RoundingMode.HALF_UP)
    otherPaymentTotal = otherPaymentTotal.setScale(2, RoundingMode.HALF_UP)
    sourceTotalPaid = sourceTotalPaid.setScale(2, RoundingMode.HALF_UP)

    // Credit amount is calculated from the consolidated payment-mode amounts.
    BigDecimal consolidatedTotal =
            cashTotal +
            cardTotal +
            otherPaymentTotal

    consolidatedTotal = consolidatedTotal.setScale(2, RoundingMode.HALF_UP)

    /*
     * Optional validation:
     * Ensure payment-mode total equals TotalPaid total.
     */
    if (consolidatedTotal.compareTo(sourceTotalPaid) != 0) {
        message.setProperty(
                "AmountValidationWarning",
                "Payment mode total ${formatAmount(consolidatedTotal)} " +
                "does not match TotalPaid ${formatAmount(sourceTotalPaid)}"
        )
    }

    String receiptDateTime = receipts[0].ReceiptDate.text()?.trim()

    if (!receiptDateTime) {
        throw new IllegalArgumentException(
                "ReceiptDate is missing in the first Receipt record"
        )
    }

    String documentDate = receiptDateTime.take(10)
    String documentReferenceDate = documentDate.replace("-", "")

    String creationDateTime = getCurrentUtcTimestamp()
    String uniqueId = "COLLECTION_${documentReferenceDate}_${System.currentTimeMillis()}"

    StringWriter writer = new StringWriter()
    MarkupBuilder xml = new MarkupBuilder(writer)

    xml.setDoubleQuotes(true)
    xml.mkp.xmlDeclaration(version: "1.0", encoding: "UTF-8")

    xml."soapenv:Envelope"(
            "xmlns:soapenv": "http://schemas.xmlsoap.org/soap/envelope/",
            "xmlns:sfin"   : "http://sap.com/xi/SAPSCORE/SFIN"
    ) {
        "soapenv:Header"()

        "soapenv:Body" {
            "sfin:JournalEntryBulkCreateRequest" {

                MessageHeader {
                    ID(uniqueId)
                    CreationDateTime(creationDateTime)
                    TestDataIndicator("false")
                    SenderBusinessSystemID("CPI_DEV")
                }

                JournalEntryCreateRequest {

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

                        // Cash Collection
                        if (cashTotal.compareTo(BigDecimal.ZERO) != 0) {
                            Item {
                                ReferenceDocumentItem(itemNumber.toString())
                                GLAccount("A2180003")
                                AmountInTransactionCurrency(
                                        currencyCode: "INR",
                                        formatAmount(cashTotal)
                                )
                                DebitCreditCode(
                                        cashTotal.signum() >= 0 ? "S" : "H"
                                )
                                DocumentItemText("Cash Collection")
                                AccountAssignment {
                                    ProfitCenter("PC-SO01-OP")
                                }
                            }

                            itemNumber++
                        }

                        // Card Collection
                        if (cardTotal.compareTo(BigDecimal.ZERO) != 0) {
                            Item {
                                ReferenceDocumentItem(itemNumber.toString())
                                GLAccount("A2180004")
                                AmountInTransactionCurrency(
                                        currencyCode: "INR",
                                        formatAmount(cardTotal)
                                )
                                DebitCreditCode(
                                        cardTotal.signum() >= 0 ? "S" : "H"
                                )
                                DocumentItemText("Card Collection")
                                AccountAssignment {
                                    ProfitCenter("PC-SO01-OP")
                                }
                            }

                            itemNumber++
                        }

                        // Cheque + NEFT + PayTM + Gpay + Phonepe + Others
                        if (otherPaymentTotal.compareTo(BigDecimal.ZERO) != 0) {
                            Item {
                                ReferenceDocumentItem(itemNumber.toString())
                                GLAccount("A2180005")
                                AmountInTransactionCurrency(
                                        currencyCode: "INR",
                                        formatAmount(otherPaymentTotal)
                                )
                                DebitCreditCode(
                                        otherPaymentTotal.signum() >= 0 ? "S" : "H"
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

                        // Hospital OPD Credit
                        if (consolidatedTotal.compareTo(BigDecimal.ZERO) != 0) {
                            BigDecimal creditAmount =
                                    consolidatedTotal.abs().negate()

                            Item {
                                ReferenceDocumentItem(itemNumber.toString())
                                GLAccount("A2210004")
                                AmountInTransactionCurrency(
                                        currencyCode: "INR",
                                        formatAmount(creditAmount)
                                )
                                DebitCreditCode("H")
                                DocumentItemText("Hospital OPD")
                                AccountAssignment {
                                    ProfitCenter("PC-SO01-OP")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    message.setBody(writer.toString())
    message.setHeader("Content-Type", "text/xml;charset=UTF-8")

    // Store consolidated amounts as exchange properties for monitoring.
    message.setProperty("CashTotal", formatAmount(cashTotal))
    message.setProperty("CardTotal", formatAmount(cardTotal))
    message.setProperty("OtherPaymentTotal", formatAmount(otherPaymentTotal))
    message.setProperty("ConsolidatedTotal", formatAmount(consolidatedTotal))

    return message
}

/*
 * Convert empty or invalid-looking numeric values safely to zero.
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
                "Invalid amount value received: ${normalizedValue}"
        )
    }
}

/*
 * Format every SAP amount with two decimal places.
 */
String formatAmount(BigDecimal amount) {
    return amount
            .setScale(2, RoundingMode.HALF_UP)
            .toPlainString()
}

/*
 * Generate UTC timestamp such as:
 * 2026-07-13T12:20:36.198Z
 */
String getCurrentUtcTimestamp() {

    SimpleDateFormat formatter =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

    formatter.setTimeZone(TimeZone.getTimeZone("UTC"))

    return formatter.format(new Date())
}