import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import groovy.xml.MarkupBuilder
import java.math.RoundingMode

def Message processData(Message message) {

    String body = message.getBody(String)

    if (body == null || body.trim().isEmpty()) {
        throw new IllegalArgumentException(
            "Input JSON body is empty."
        )
    }

    def records = new JsonSlurper().parseText(body)

    if (!(records instanceof List)) {
        throw new IllegalArgumentException(
            "Expected Netram payload as a JSON array."
        )
    }

    if (records.isEmpty()) {
        throw new IllegalArgumentException(
            "Netram payload JSON array is empty."
        )
    }

    /*
     * Read Branch Name from the first JSON record
     */
    String branchName =
        records[0].branchname?.toString()?.trim()

    if (branchName == null || branchName.isEmpty()) {
        throw new IllegalArgumentException(
            "branchname is missing in the first Netram record."
        )
    }

    /*
     * Validate that all records belong to the same branch
     */
    def differentBranchRecord = records.find { record ->

        String currentBranch =
            record.branchname?.toString()?.trim()

        return currentBranch == null ||
               currentBranch.isEmpty() ||
               !currentBranch.equalsIgnoreCase(branchName)
    }

    if (differentBranchRecord != null) {
        throw new IllegalArgumentException(
            "All records must belong to the same branch. " +
            "Expected branch: ${branchName}, but found: " +
            "${differentBranchRecord.branchname}"
        )
    }

    /*
     * Set Branch Name in Message Header
     */
    message.setHeader("BranchName", branchName)

    /*
     * Read Profit Center from Exchange Property
     *
     * This property must be populated by the previous
     * Branch + Department mapping Groovy.
     */
    String profitCenter =
        message.getProperty("ProfitCenter")
            ?.toString()
            ?.trim()

    if (profitCenter == null || profitCenter.isEmpty()) {
        throw new IllegalArgumentException(
            "Exchange Property 'ProfitCenter' is missing. " +
            "Execute the Branch and Department mapping Groovy " +
            "before this Groovy."
        )
    }

    BigDecimal totalCash = 0.00G
    BigDecimal totalCard = 0.00G
    BigDecimal totalNEFT = 0.00G
    BigDecimal totalCheque = 0.00G
    BigDecimal totalUPI = 0.00G
    BigDecimal totalCollection = 0.00G

    records.each { record ->

        totalCash += toDecimal(record.Cash)
        totalCard += toDecimal(record.Card)
        totalNEFT += toDecimal(record.NEFT)
        totalCheque += toDecimal(record.Cheque)
        totalUPI += toDecimal(record.UPI)
        totalCollection += toDecimal(record.TotalAmt)
    }

    /*
     * Consolidate UPI, NEFT and Cheque
     */
    BigDecimal totalOtherCollection =
        totalUPI + totalNEFT + totalCheque

    /*
     * Validate payment-mode total against TotalAmt
     */
    BigDecimal calculatedCollection =
        totalCash +
        totalCard +
        totalOtherCollection

    if (
        calculatedCollection
            .setScale(2, RoundingMode.HALF_UP)
            .compareTo(
                totalCollection.setScale(
                    2,
                    RoundingMode.HALF_UP
                )
            ) != 0
    ) {
        throw new IllegalStateException(
            "Payment mode total " +
            "${formatAmount(calculatedCollection)} " +
            "does not match TotalAmt total " +
            "${formatAmount(totalCollection)}"
        )
    }

    String documentDate = getDocumentDate(records)

    String dateReference =
        documentDate.replace("-", "")

    String timestamp = new Date().format(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        TimeZone.getTimeZone("UTC")
    )

    String messageId =
        "COLLECTION_" +
        dateReference +
        "_" +
        System.currentTimeMillis()

    StringWriter writer = new StringWriter()
    MarkupBuilder xml = new MarkupBuilder(writer)

    xml.'soapenv:Envelope'(
        'xmlns:soapenv':
            'http://schemas.xmlsoap.org/soap/envelope/',
        'xmlns:sfin':
            'http://sap.com/xi/SAPSCORE/SFIN'
    ) {

        'soapenv:Header'()

        'soapenv:Body' {

            'sfin:JournalEntryBulkCreateRequest' {

                /*
                 * Bulk Message Header
                 */
                MessageHeader {
                    ID(messageId)
                    CreationDateTime(timestamp)
                    TestDataIndicator('false')
                    SenderBusinessSystemID('CPI_DEV')
                }

                JournalEntryCreateRequest {

                    /*
                     * Journal Entry Message Header
                     */
                    MessageHeader {
                        ID('SUB_COLLECTION')
                        CreationDateTime(timestamp)
                    }

                    JournalEntry {

                        OriginalReferenceDocumentType('BKPFF')
                        OriginalReferenceDocument('COLLECTION')
                        OriginalReferenceDocumentLogicalSystem(
                            'CPI_DEV'
                        )

                        BusinessTransactionType('RFBU')
                        AccountingDocumentType('SA')

                        DocumentReferenceID(dateReference)
                        DocumentHeaderText('Netram Collection')

                        CreatedByUser('CC0000000044')
                        CompanyCode('MX01')
                        DocumentDate(documentDate)
                        PostingDate(documentDate)

                        Reference1InDocumentHeader(
                            'NETRAM COLLECTION'
                        )

                        Reference2InDocumentHeader(
                            dateReference
                        )

                        /*
                         * Cash Collection - Debit
                         */
                        if (
                            totalCash.compareTo(
                                BigDecimal.ZERO
                            ) != 0
                        ) {
                            Item {
                                ReferenceDocumentItem('1')
                                GLAccount('A2180003')

                                AmountInTransactionCurrency(
                                    currencyCode: 'INR',
                                    formatAmount(totalCash)
                                )

                                DebitCreditCode('S')
                                DocumentItemText(
                                    'Cash Collection'
                                )

                                AccountAssignment {
                                    ProfitCenter(profitCenter)
                                }
                            }
                        }

                        /*
                         * Card Collection - Debit
                         */
                        if (
                            totalCard.compareTo(
                                BigDecimal.ZERO
                            ) != 0
                        ) {
                            Item {
                                ReferenceDocumentItem('2')
                                GLAccount('A2180004')

                                AmountInTransactionCurrency(
                                    currencyCode: 'INR',
                                    formatAmount(totalCard)
                                )

                                DebitCreditCode('S')
                                DocumentItemText(
                                    'Card Collection'
                                )

                                AccountAssignment {
                                    ProfitCenter(profitCenter)
                                }
                            }
                        }

                        /*
                         * UPI + NEFT + Cheque Collection - Debit
                         */
                        if (
                            totalOtherCollection.compareTo(
                                BigDecimal.ZERO
                            ) != 0
                        ) {
                            Item {
                                ReferenceDocumentItem('3')
                                GLAccount('A2180005')

                                AmountInTransactionCurrency(
                                    currencyCode: 'INR',
                                    formatAmount(
                                        totalOtherCollection
                                    )
                                )

                                DebitCreditCode('S')

                                DocumentItemText(
                                    'UPI NEFT Cheque Collection'
                                )

                                AccountAssignment {
                                    ProfitCenter(profitCenter)
                                }
                            }
                        }

                        /*
                         * Total Collection - Credit
                         */
                        Item {
                            ReferenceDocumentItem('4')
                            GLAccount('A2180005')

                            AmountInTransactionCurrency(
                                currencyCode: 'INR',
                                formatAmount(
                                    totalCollection.negate()
                                )
                            )

                            DebitCreditCode('H')
                            DocumentItemText(
                                'Total Collection'
                            )

                            AccountAssignment {
                                ProfitCenter(profitCenter)
                            }
                        }
                    }
                }
            }
        }
    }

    message.setBody(writer.toString())

    /*
     * Store totals as Exchange Properties
     */
    message.setProperty(
        'TotalCash',
        formatAmount(totalCash)
    )

    message.setProperty(
        'TotalCard',
        formatAmount(totalCard)
    )

    message.setProperty(
        'TotalNEFT',
        formatAmount(totalNEFT)
    )

    message.setProperty(
        'TotalCheque',
        formatAmount(totalCheque)
    )

    message.setProperty(
        'TotalUPI',
        formatAmount(totalUPI)
    )

    message.setProperty(
        'TotalOtherCollection',
        formatAmount(totalOtherCollection)
    )

    message.setProperty(
        'TotalCollection',
        formatAmount(totalCollection)
    )

    /*
     * Retain useful values as properties
     */
    message.setProperty(
        'BranchName',
        branchName
    )

    message.setProperty(
        'JournalEntryMessageID',
        messageId
    )

    message.setHeader(
        'Content-Type',
        'text/xml;charset=UTF-8'
    )

    return message
}


BigDecimal toDecimal(def value) {

    if (
        value == null ||
        value.toString().trim().isEmpty()
    ) {
        return 0.00G
    }

    try {
        return new BigDecimal(
            value.toString().trim()
        )
    } catch (Exception exception) {
        throw new IllegalArgumentException(
            "Invalid numeric value: ${value}"
        )
    }
}


String formatAmount(BigDecimal value) {

    return value.setScale(
        2,
        RoundingMode.HALF_UP
    ).toPlainString()
}


String getDocumentDate(List records) {

    def paymentDate = records.find { record ->

        record.PaymentDate != null &&
        !record.PaymentDate
            .toString()
            .trim()
            .isEmpty()
    }?.PaymentDate

    if (paymentDate == null) {
        throw new IllegalArgumentException(
            "PaymentDate is missing in Netram payload."
        )
    }

    String documentDate =
        paymentDate.toString().trim()

    if (documentDate.length() < 10) {
        throw new IllegalArgumentException(
            "Invalid PaymentDate: ${documentDate}"
        )
    }

    String firstDocumentDate =
        documentDate.substring(0, 10)

    /*
     * Validate that all records have the same PaymentDate
     */
    def differentDateRecord = records.find { record ->

        String currentDate =
            record.PaymentDate
                ?.toString()
                ?.trim()

        return currentDate == null ||
               currentDate.length() < 10 ||
               currentDate.substring(0, 10) !=
                   firstDocumentDate
    }

    if (differentDateRecord != null) {
        throw new IllegalArgumentException(
            "All records must have the same PaymentDate. " +
            "Expected date: ${firstDocumentDate}, but found: " +
            "${differentDateRecord.PaymentDate}"
        )
    }

    return firstDocumentDate
}