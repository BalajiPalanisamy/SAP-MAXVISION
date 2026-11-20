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
         * BranchName is taken from the first input record
         * and stored in the CPI message header.
         */
        String branchName = records[0].BranchName?.toString()?.trim() ?: ""
    
        if (!branchName) {
            throw new IllegalArgumentException(
                "BranchName is missing in the first input record"
            )
        }
    
        message.setHeader("BranchName", branchName)
    
        /*
         * CompanyCode and ProfitCenter can be populated through
         * Exchange Properties or message headers before this Groovy step.
         *
         * No branch-based mapping is performed here.
         */
        String companyCode =
            message.getProperty("CompanyCode")?.toString()?.trim()
            ?: message.getHeader("CompanyCode", String)?.trim()
            ?: ""
    
        String profitCenter =
            message.getProperty("ProfitCenter")?.toString()?.trim()
            ?: message.getHeader("ProfitCenter", String)?.trim()
            ?: ""
    
        BigDecimal otTotal = BigDecimal.ZERO
        BigDecimal iolTotal = BigDecimal.ZERO
        BigDecimal pharmacyTotal = BigDecimal.ZERO
    
        int recordCount = records.size()
    
        /*
         * High-performance single-pass consolidation.
         *
         * Only these exact StoreName values are considered:
         * OT
         * IOL
         * Pharmacy
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
    
            def stockValue

if (storeName == "PHARMACY") {
// Pharmacy: consolidate StockValue without GST
stockValue = record.StockValue
} else {
// OT and IOL: consolidate StockValueWithGST
stockValue = record.StockValueWithGST
}
    
            if (stockValue == null) {
                continue
            }
    
            BigDecimal amount
    
            if (stockValue instanceof BigDecimal) {
                amount = stockValue
            } else {
                String amountText = stockValue.toString().trim()
    
                if (amountText.isEmpty()) {
                    continue
                }
    
                amount = new BigDecimal(amountText)
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
         * StockDate can be supplied as an Exchange Property
         * or message header.
         *
         * Supported formats:
         * 2026-06-08T00:00:00
         * 2026-06-08
         * 08.06.2026
         *
         * Current date is used when StockDate is unavailable.
         */
        String stockDate =
            message.getProperty("StockDate")?.toString()?.trim()
            ?: message.getHeader("StockDate", String)?.trim()
            ?: ""
    
        Date accountingDate
    
        if (stockDate) {
    
            if (stockDate.length() >= 10 &&
                stockDate.charAt(4) == '-' &&
                stockDate.charAt(7) == '-') {
    
                SimpleDateFormat inputFormat =
                    new SimpleDateFormat("yyyy-MM-dd")
    
                inputFormat.setLenient(false)
    
                accountingDate = inputFormat.parse(
                    stockDate.substring(0, 10)
                )
    
            } else {
    
                SimpleDateFormat inputFormat =
                    new SimpleDateFormat("dd.MM.yyyy")
    
                inputFormat.setLenient(false)
    
                accountingDate = inputFormat.parse(stockDate)
            }
    
        } else {
            accountingDate = new Date()
        }
    
        SimpleDateFormat outputDateFormat =
            new SimpleDateFormat("dd.MM.yyyy")
    
        SimpleDateFormat referenceDateFormat =
            new SimpleDateFormat("ddMMyy")
    
        String formattedDate =
            outputDateFormat.format(accountingDate)
    
        String reference =
            "OP" + referenceDateFormat.format(accountingDate)
    
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
                    debitamount          : "0.00",
                    creditamount         : otAmount,
                    debitcreditindicator : "CR",
                    gltext               : "OT Stock",
                    costcenter           : "",
                    profitcenter         : profitCenter,
                    companycode2         : "",
                    profitcenter2        : "",
    "businessplace"         : ""
                ],
                [
                    reference            : reference,
                    companycode          : companyCode,
                    documentdate         : formattedDate,
                    postingdate          : formattedDate,
                    documenttype         : "SA",
                    referencedocument    : "IOL Stock",
                    headertext           : "IOL Stock",
                    glaccount            : "A2160007",
                    debitamount          : "0.00",
                    creditamount         : iolAmount,
                    debitcreditindicator : "CR",
                    gltext               : "IOL Stock",
                    costcenter           : "",
                    profitcenter         : profitCenter,
                    companycode2         : "",
                    profitcenter2        : "",
    "businessplace"         : ""
                ],
                [
                    reference            : reference,
                    companycode          : companyCode,
                    documentdate         : formattedDate,
                    postingdate          : formattedDate,
                    documenttype         : "SA",
                    referencedocument    : "Pharmacy Stock",
                    headertext           : "Pharmacy Stock",
                    glaccount            : "A2160008",
                    debitamount          : "0.00",
                    creditamount         :  pharmacyAmount,
                    debitcreditindicator : "CR",
                    gltext               : "Pharmacy Stock",
                    costcenter           : "",
                    profitcenter         : profitCenter,
                    companycode2         : "",
                    profitcenter2        : "",
    "businessplace"         : ""
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
                    debitamount          : offsetAmount,
                    creditamount         : "0.00",
                    debitcreditindicator : "DR",
                    gltext               : "Stock Offset GL",
                    costcenter           : "",
                    profitcenter         : profitCenter,
                    companycode2         : "",
                    profitcenter2        : "",
    "businessplace"         : ""
                ]
            ]
        ]
    
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