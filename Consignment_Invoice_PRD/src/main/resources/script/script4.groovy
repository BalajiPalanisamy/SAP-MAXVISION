import com.sap.gateway.ip.core.customdev.util.Message
import groovy.xml.StreamingMarkupBuilder
import groovy.util.XmlSlurper

def Message processData(Message message) {

    String profitCenter = message.getProperty("ProfitCenter")?.toString()?.trim()

    if (!profitCenter) {
        throw new IllegalArgumentException(
            "Exchange Property 'ProfitCenter' is missing."
        )
    }

    def xml = new XmlSlurper(false, true).parseText(message.getBody(String))

    xml.depthFirst().findAll {
        it.name() == 'ProfitCenter'
    }.each {
        it.replaceBody(profitCenter)
    }

    def builder = new StreamingMarkupBuilder()
    message.setBody(builder.bind { mkp.yield xml }.toString())

    return message
}