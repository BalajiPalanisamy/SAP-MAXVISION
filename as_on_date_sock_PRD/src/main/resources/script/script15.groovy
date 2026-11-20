import com.sap.gateway.ip.core.customdev.util.Message

 

def Message processData(Message message) {
    def yesterdayDate = (new Date() - 1).format("yyyy-MM-dd")
    message.setProperty("YesterdayDate", yesterdayDate)
    return message
}