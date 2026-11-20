import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput
import groovy.util.XmlSlurper
import java.text.SimpleDateFormat

def Message processData(Message message) {

    def body = message.getBody(String)

    if (!body || body.trim().isEmpty()) {
        throw new IllegalArgumentException("Input XML is empty")
    }

    def xml = new XmlSlurper().parseText(body)

    def receipt = xml.Receipt[0]

    if (!receipt) {
        throw new IllegalArgumentException("Receipt node not found")
    }


    // =========================
    // Read Input Values
    // =========================

    String branchName = receipt.BranchName.text().trim()
    String supplierName = receipt.SupplierID.text().trim()
    String deptName = receipt.DeptName.text().trim()

    String invoiceNo = receipt.CS_InvoiceNo.text().trim()

    String transactionDate =
            receipt.transactionDate.text().trim()

    BigDecimal gstPercentage =
            receipt.GSTPercentage.text().toBigDecimal()

    BigDecimal netValue =
            receipt.NetValue.text().toBigDecimal()


    // =========================
    // Store Headers
    // =========================

    message.setHeader(
            "BranchName",
            branchName
    )

    message.setHeader(
            "SupplierName",
            supplierName
    )


    // =========================
    // Date Formatting
    // =========================

    if (!transactionDate) {
        throw new IllegalArgumentException(
                "Transaction Date is missing"
        )
    }

    Date date = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss"
    ).parse(transactionDate)


    String documentDate =
            new SimpleDateFormat(
                    "dd.MM.yyyy"
            ).format(date)


    // =========================
    // Company Code from Exchange Property
    // =========================

    String companyCode =
            message.getProperty("CompanyCode")?.toString()?.trim()


    // =========================
    // Cost Center from Exchange Property
    // =========================

    String costCenter =
            message.getProperty("CostCenter")?.toString()?.trim()


    // =========================
    // Business Place from Exchange Property
    // =========================

    String businessPlace =
            message.getProperty("BusinessPlace")?.toString()?.trim()


    // =========================
    // GL Account Mapping
    // =========================

    String glAccount


    if (deptName.toUpperCase() == "IOL") {


        glAccount = [

                5  : "E4120003",
                12 : "E4120001",
                18 : "E4120002"

        ][gstPercentage.intValue()]


        if (!glAccount) {

            throw new IllegalArgumentException(
                    "GL Account mapping not found for IOL GST ${gstPercentage}"
            )
        }


    } else {


        throw new IllegalArgumentException(
                "GL Account mapping not available for Department ${deptName}"
        )

    }


    // =========================
    // Output JSON
    // =========================

    def output = [

            ApiType: "Consignment Invoice",

            Lines: [

                    [

                            InvoiceID: "1",

                            CompanyCode: "",

                            zTransaction: "1",

                            InvoicingParty: supplierName,

                            Reference: invoiceNo,

                            DocumentDate: documentDate,

                            PostingDate: documentDate,

                            DocumentType: "KR",

                            DocumentHeaderText: "Consignment Invoice",

                            Currency: "INR",

                            GrossInvoiceAmount: netValue.toString(),

                            BusinessPlace: "",

                            DateForDeterminingTaxRates: documentDate,

                            GLAccount: glAccount,

                            ItemText: "Consignment Invoice",

                            DebitCreditIndicator: "S",

                            AmountInDocumentCurrency: netValue.toString(),


                            Assignment: "",

                            CostCenter: "",

                            ProfitCenter: "",
                            SectionCode                : ""

                    ]

            ]

    ]


    message.setBody(
            JsonOutput.prettyPrint(
                    JsonOutput.toJson(output)
            )
    )


    message.setHeader(
            "Content-Type",
            "application/json"
    )


    return message

}