import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

def Message processData(Message message) {

    // =====================================================
    // Fetch Values from CPI Exchange Properties
    // =====================================================
    String companyCode = message.getProperty("CompanyCode")
        ?.toString()?.trim()

    String profitCenter = message.getProperty("ProfitCenter")
        ?.toString()?.trim()

    String costCenter = message.getProperty("CostCenter")
        ?.toString()?.trim()

    String businessPlace = message.getProperty("BusinessPlace")
        ?.toString()?.trim()

    // =====================================================
    // Validate Exchange Properties
    // =====================================================
    if (!companyCode) {
        throw new IllegalArgumentException(
            "Exchange Property 'CompanyCode' is missing or empty"
        )
    }

    if (!profitCenter) {
        throw new IllegalArgumentException(
            "Exchange Property 'ProfitCenter' is missing or empty"
        )
    }

    if (!costCenter) {
        throw new IllegalArgumentException(
            "Exchange Property 'CostCenter' is missing or empty"
        )
    }

    if (!businessPlace) {
        throw new IllegalArgumentException(
            "Exchange Property 'BusinessPlace' is missing or empty"
        )
    }

    // =====================================================
    // Read Payload
    // =====================================================
    String body = message.getBody(String)

    if (!body?.trim()) {
        throw new IllegalArgumentException(
            "Message body is empty"
        )
    }

    def json

    try {
        json = new JsonSlurper().parseText(body)
    } catch (Exception e) {
        throw new IllegalArgumentException(
            "Invalid JSON payload: ${e.message}",
            e
        )
    }

    // =====================================================
    // Validate JSON Structure
    // =====================================================
    if (!(json instanceof Map)) {
        throw new IllegalArgumentException(
            "Expected a JSON object containing the 'Lines' array"
        )
    }

    if (!(json.Lines instanceof List)) {
        throw new IllegalArgumentException(
            "Field 'Lines' is missing or is not a JSON array"
        )
    }

    // =====================================================
    // Update Every Accounting Line
    // =====================================================
    json.Lines.eachWithIndex { line, index ->

        if (!(line instanceof Map)) {
            throw new IllegalArgumentException(
                "Invalid line item at Lines index ${index}"
            )
        }

        // Replace values using Exchange Properties
        line.companycode = companyCode
        line.profitcenter = profitCenter
        line.costcenter = costCenter
        line.businessplace = businessPlace

        // Remove possible uppercase duplicate fields
        line.remove("CompanyCode")
        line.remove("ProfitCenter")
        line.remove("CostCenter")
        line.remove("BusinessPlace")
    }

    // =====================================================
    // Set Updated JSON as Message Body
    // =====================================================
    message.setBody(JsonOutput.toJson(json))
    message.setHeader("Content-Type", "application/json")

    return message
}