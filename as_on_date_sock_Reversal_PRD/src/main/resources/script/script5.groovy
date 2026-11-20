import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def Message processData(Message message) {

    String body = message.getBody(String)

    def inputJson = new JsonSlurper().parseText(body)

    def outputJson

    if (inputJson instanceof List) {
        outputJson = [
            Receipt: inputJson
        ]
    } else {
        outputJson = inputJson
    }

    message.setBody(JsonOutput.toJson(outputJson))
    message.setHeader("Content-Type", "application/json")

    return message
}