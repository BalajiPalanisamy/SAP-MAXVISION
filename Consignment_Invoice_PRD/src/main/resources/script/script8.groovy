import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def Message processData(Message message) {

String body = message.getBody(String)

if (!body || body.trim().isEmpty()) {
    throw new IllegalArgumentException("Input JSON payload is empty")
}

def json

try {
    json = new JsonSlurper().parseText(body)
} catch (Exception e) {
    throw new IllegalArgumentException(
        "Invalid JSON payload: " + e.getMessage()
    )
}

String companyCode =
    message.getProperty("CompanyCode")?.toString()?.trim()

String costCenter =
    message.getProperty("CostCenter")?.toString()?.trim()

/*
String businessPartner =
    message.getProperty("BusinessPartner")?.toString()?.trim()
*/

String businessPlace =
    message.getProperty("BusinessPlace")?.toString()?.trim()

String sectionCode =
    message.getProperty("SectionCode")?.toString()?.trim()


if (!companyCode) {
    throw new IllegalArgumentException(
        "Exchange Property CompanyCode is missing"
    )
}

if (!costCenter) {
    throw new IllegalArgumentException(
        "Exchange Property CostCenter is missing"
    )
}

/*
if (!businessPartner) {
    throw new IllegalArgumentException(
        "Exchange Property BusinessPartner is missing"
    )
}
*/


if (!(json.Lines instanceof List) || json.Lines.isEmpty()) {
    throw new IllegalArgumentException(
        "Lines array is missing or empty"
    )
}


json.Lines.each { line ->

    line.CompanyCode = companyCode
    line.CostCenter = costCenter

    // line.InvoicingParty = businessPartner

    line.BusinessPlace = businessPlace
    line.SectionCode = sectionCode

}


message.setBody(
    JsonOutput.prettyPrint(
        JsonOutput.toJson(json)
    )
)

return message

}