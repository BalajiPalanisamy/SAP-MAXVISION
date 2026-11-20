import com.sap.gateway.ip.core.customdev.util.Message
import groovy.util.XmlSlurper
import java.net.URLEncoder

Message processData(Message message) {

    String body = message.getBody(String)

    if (!body?.trim()) {
        throw new IllegalArgumentException("Input XML is empty.")
    }

    def receipt = new XmlSlurper(false, false)
            .parseText(body)
            .Receipt

    if (!receipt || receipt.size() == 0) {
        throw new IllegalArgumentException("Receipt node not found.")
    }

    String branchName = receipt.branchname.text()?.trim()
    String supplierName = receipt.SupplierName.text()?.trim()

    if (!branchName) {
        throw new IllegalArgumentException("branchname is missing.")
    }

    if (!supplierName) {
        throw new IllegalArgumentException("SupplierName is missing.")
    }

    message.setProperty("OriginalPayload", body)
    message.setProperty("BranchName", branchName)
    message.setProperty("PurchaseDate", receipt.PurchaseDate.text()?.trim())
    message.setProperty("SupplierName", supplierName)
    message.setProperty("SupplierInvoiceNo", receipt.SupplierInvoiceNo.text()?.trim())
    message.setProperty("PurchaseType", receipt.PurchaseType.text()?.trim())
    message.setProperty("DeptName", receipt.DeptName.text()?.trim())
    message.setProperty("GSTRate", receipt.GSTRate.text()?.trim())
    message.setProperty("AmountwithGST", receipt.AmountwithGST.text()?.trim())

    String escapedSupplierName = supplierName.replace("'", "''")

    String encodedFilter = URLEncoder.encode(
            "OrganizationBPName1 eq '${escapedSupplierName}'",
            "UTF-8"
    ).replace("+", "%20")

    String query =
            "\$filter=${encodedFilter}" +
            "&\$select=BusinessPartner" +
            "&\$format=json"

    message.setProperty("BPQuery", query)

    return message
}