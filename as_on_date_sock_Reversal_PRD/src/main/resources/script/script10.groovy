import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import java.math.RoundingMode
import java.text.SimpleDateFormat

def Message processData(Message message) {

    String body = message.getBody(String)

    if (body == null || body.trim().isEmpty()) {
        throw new IllegalArgumentException("Input JSON payload is empty")
    }

    def records = new JsonSlurper().parseText(body)

    if (!(records instanceof List) || records.isEmpty()) {
        throw new IllegalArgumentException(
            "Input payload must be a non-empty JSON array"
        )
    }

    /*
     * Values must be maintained in CPI Exchange Properties.
     */
    String companyCode = message.getProperty("CompanyCode")
        ?.toString()
        ?.trim()

    String profitCenter = message.getProperty("ProfitCenter")
        ?.toString()
        ?.trim()

    if (!companyCode) {
        throw new IllegalArgumentException(
            "Exchange Property 'CompanyCode' is missing"
        )
    }

    if (!profitCenter) {
        throw new IllegalArgumentException(
            "Exchange Property 'ProfitCenter' is missing"
        )
    }

    BigDecimal otTotal = BigDecimal.ZERO
    BigDecimal iolTotal = BigDecimal.ZERO
    BigDecimal pharmacyTotal = BigDecimal.ZERO

    String branchName = records[0].BranchName
        ?.toString()
        ?.trim() ?: ""

    int recordCount = records.size()

    /*
     * Single-pass consolidation for better performance.
     * Only OT, IOL and Pharmacy are considered.
     */
    for (int i = 0; i < recordCount; i++) {

        def record = records[i]

        String storeName = record.StoreName
            ?.toString()
            ?.trim()
            ?.toUpperCase()

        if (!storeName) {
            continue
        }

        BigDecimal amount = BigDecimal.ZERO
        def stockValue = record.StockValueWithGST

        if (stockValue != null) {

            if (stockValue instanceof BigDecimal) {
                amount = stockValue
            } else {
                String amountText = stockValue.toString().trim()

                if (!amountText.isEmpty()) {
                    amount = new BigDecimal(amountText)
                }
            }
        }

        switch (storeName) {

            case "OT":
                otTotal = otTotal.add(amount)
                break

            case "IOL":
                iolTotal = iolTotal.add(amount)
                break

            case "PHARMACY":
                pharmacyTotal = pharmacyTotal.add(amount)
                break

            default:
                // Ignore all other StoreName values
                break
        }
    }

    /*
     * Maintain StockDate as CPI Exchange Property.
     *
     * Supported formats:
     * 2026-06-08T00:00:00
     * 2026-06-08
     * 08.06.2026
     *
     * Current date is used when StockDate is empty.
     */
    String stockDate = message.getProperty("StockDate")
        ?.toString()
        ?.trim()

    Date accountingDate

    if (stockDate) {

        if (stockDate.length() >= 10 &&
            stockDate.charAt(4) == '-' &&
            stockDate.charAt(7) == '-') {

            accountingDate = new SimpleDateFormat("yyyy-MM-dd")
                .parse(stockDate.substring(0, 10))

        } else {

            accountingDate = new SimpleDateFormat("dd.MM.yyyy")
                .parse(stockDate)
        }

    } else {
        accountingDate = new Date()
    }

    String formattedDate = new SimpleDateFormat("dd.MM.yyyy")
        .format(accountingDate)

    String reference = "OP" +
        new SimpleDateFormat("ddMMyy").format(accountingDate)

    String otAmount = formatAmount(otTotal)
    String iolAmount = formatAmount(iolTotal)
    String pharmacyAmount = formatAmount(pharmacyTotal)

    BigDecimal totalOffset = otTotal
        .add(iolTotal)
        .add(pharmacyTotal)

    String offsetAmount = formatAmount(totalOffset)

    def output = [
        ApiType: "AsOnDateStock",
        Lines  : [
            [
                reference            : reference,
                companycode          : companyCode,
                documentdate         : formattedDate,
                postingdate          : formattedDate,
                documenttype         : "SA",
                referencedocument    : "OT Stock",
                headertext           : "OT Stock",
                glaccount            : "A2160006",
                debitamount          : otAmount,
                creditamount         : "0.00",
                debitcreditindicator : "DR",
                gltext               : "OT Stock",
                costcenter           : "",
                profitcenter         : profitCenter,
                companycode2         : "",
                profitcenter2        : ""
            ],
            [
                reference            : reference,
                companycode          : companyCode,
                documentdate         : formattedDate,
                postingdate          : formattedDate,
                documenttype         : "SA",
                referencedocument    : "IOL Stock",
                headertext           : "IOL Stock",
                glaccount            : "A2160008",
                debitamount          : iolAmount,
                creditamount         : "0.00",
                debitcreditindicator : "DR",
                gltext               : "IOL Stock",
                costcenter           : "",
                profitcenter         : profitCenter,
                companycode2         : "",
                profitcenter2        : ""
            ],
            [
                reference            : reference,
                companycode          : companyCode,
                documentdate         : formattedDate,
                postingdate          : formattedDate,
                documenttype         : "SA",
                referencedocument    : "Pharmacy Stock",
                headertext           : "Pharmacy Stock",
                glaccount            : "A2160007",
                debitamount          : pharmacyAmount,
                creditamount         : "0.00",
                debitcreditindicator : "DR",
                gltext               : "Pharmacy Stock",
                costcenter           : "",
                profitcenter         : profitCenter,
                companycode2         : "",
                profitcenter2        : ""
            ],
            [
                reference            : reference,
                companycode          : companyCode,
                documentdate         : formattedDate,
                postingdate          : formattedDate,
                documenttype         : "SA",
                referencedocument    : "Stock Offset GL",
                headertext           : "Stock Offset GL",
                glaccount            : "U9120007",
                debitamount          : "0.00",
                creditamount         : offsetAmount,
                debitcreditindicator : "CR",
                gltext               : "Stock Offset GL",
                costcenter           : "",
                profitcenter         : profitCenter,
                companycode2         : "",
                profitcenter2        : ""
            ]
        ]
    ]

    message.setHeader("BranchName", branchName)
    message.setHeader("RecordCount", recordCount)
    message.setHeader("Content-Type", "application/json")

    message.setProperty("OTStockTotal", otAmount)
    message.setProperty("IOLStockTotal", iolAmount)
    message.setProperty("PharmacyStockTotal", pharmacyAmount)
    message.setProperty("TotalStockOffset", offsetAmount)

    message.setBody(JsonOutput.toJson(output))

    return message
}

def String formatAmount(BigDecimal amount) {

    return amount
        .setScale(2, RoundingMode.HALF_UP)
        .toPlainString()
}