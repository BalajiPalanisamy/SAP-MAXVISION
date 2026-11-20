import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {

    def writer = new StringWriter()
    def xml = new groovy.xml.MarkupBuilder(writer)

    xml.Branches {
        (1..119).each { id ->
            Branch {
                BranchId(id)
            }
        }
    }

    message.setBody(writer.toString())

    return message
}