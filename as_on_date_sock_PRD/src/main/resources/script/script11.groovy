import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {

   // Read BranchName only from Message Header
String branchName = message.getHeader("BranchName", String)

if (!branchName?.trim()) {
    throw new IllegalArgumentException("Message Header 'BranchName' is missing.")
}

// Keep input exactly as received (only remove leading/trailing spaces)
String branchKey = branchName.trim()


// Branch -> Company Code Mapping
Map<String, String> companyCodeMap = [
'Somajiguda' : 'MX01',
'ASRao Nagar' : 'MX01',
'Attapur' : 'MX01',
'Begumpet' : 'MX01',
'Dilsukhnagar' : 'MX01',
'Kompally' : 'MX01',
'Kothapet' : 'MX01',
'Kukatpally' : 'MX01',
'Madeenaguda' : 'MX01',
'Madhapur' : 'MX01',
'Manikonda' : 'MX01',
'Santhosh Nagar' : 'MX01',
'Vanasthalipuram' : 'SM01',
'Tanvi' : 'TA01',
'Karimnagar' : 'MX01',
'Khammam' : 'MX01',
'Warangal' : 'MX01',
'Asifabad VC' : 'SH01',
'Bhupalpally VC' : 'SH01',
'Janagoan VC' : 'SH01',
'Kothakonda VC' : 'SH01',
'Mulknoor VC' : 'SH01',
'Mulugu VC' : 'SH01',
'Narsampet VC' : 'SH01',
'Parakala VC' : 'SH01',
'Bhimavaram' : 'MX01',
'Kakinada' : 'MX01',
'Rajahmundry' : 'MX01',
'Vizianagaram' : 'MX01',
'Vijayawada' : 'MX01',
'Vizag' : 'MX01',
'Guntur' : 'RR01',
'Macherla' : 'RR01',
'Narsaraopet' : 'RR01',
'Abhiramapuram VC' : 'MX01',
'Adyar VC' : 'MX01',
'Ambattur VC' : 'MX01',
'Aminjikarai VC' : 'MX01',
'Annanagar' : 'MX01',
'Mogappair VC' : 'MX01',
'OMR VC' : 'MX01',
'Royapet VC' : 'MX01',
'Vadapalani VC' : 'MX01',
'Jaya Eye Care' : 'MX01',
'Harur VC' : 'MX01',
'Kumbakonam' : 'MX01',
'Madurai' : 'MX01',
'Perambalur' : 'MX01',
'Salem' : 'MX01',
'Thanjavur' : 'MX01',
'Trichy' : 'MX01',
'Gurvayur' : 'RM01',
'Kunnamkulam' : 'RM01',
'Thrissur' : 'RM01',
'Wadakanchery' : 'RM01',
'Malabar' : 'MM01',
'Jamnagar' : 'NE01',
'Morbi' : 'NE01',
'Pedak Road' : 'NE01',
'Rajkot' : 'NE01',
'Saurashtra' : 'NE01',
'University Road' : 'NE01',
'Parle Point' : 'RK01',
'Parvat Patia' : 'RK01',
'Bandra' : 'OM01',
'Kandivali' : 'OM01',
'Trust' : 'OM01',
'Kukatpally Optical Warehouse' : 'MX01',
'Vemulawada VC' : 'SH01',
'Sulthanabad VC' : 'SH01',
'Choppadandi VC' : 'SH01',
'Ghanpur VC' : 'SH01',
'Madhira VC' : 'SH01',
'Yellandu VC' : 'SH01',
'Palakurthy VC' : 'SH01',
'Mahabubabad VC' : 'SH01',
'Uppal' : 'MX01',
'Netram To Eh Note Transfer' : 'MX01',
'Nizamabad' : 'MX01',
'Juhu' : 'OM01',
'Warangal Opticals 2' : 'MX01',
'Valapadi VC' : 'MX01',
'Madurai VC' : 'MX01',
'Trichy VC' : 'MX01',
'Atholi VC' : 'RM01',
'Valapadi' : 'MX01',
'Ongole' : 'RR01',
'Angamaly' : 'RM01',
'Gajuwaka' : 'MX01',
'Tirupati' : 'MX01',
'MVP Colony' : 'MX01',
'Regional Office - Hyderabad' : 'MX01',
'Madhurawada VC' : 'MX01',
'Z. P. Junction VC' : 'MX01',
'Anakapalli VC' : 'MX01',
'Powai VC' : 'OM01',
'Maruti Hospital VC' : 'MX01',
'Thirumalagiri VC' : 'MX01',
'Thorour VC' : 'MX01',
'PALAKURTHI' : 'SH01',
'Station Ghanpur VC' : 'SH01',
'HUZURABAD VC' : 'SH01',
'CHOPPADANDI' : 'SH01',
'VEMULAWADA' : 'SH01',
'SULTHANABAD' : 'SH01',
'Husnabad VC' : 'SH01',
'Madhira' : 'SH01',
'Huzurnagar VC' : 'SH01',
'YELLANDU' : 'SH01',
'Kalwakurthy VC' : 'SH01',
'Rasoolpura' : 'MX01',
'Karmanghat' : 'MX01',
'SRG Addor' : 'MX01',
'SRG Spectra' : 'MX01',
'SRG Shivalik' : 'MX01',
'TTK Road' : 'MX01',
'RC PURAM' : 'MX01',
'Leela Eye Clinic' : 'MX01'
]
  String companyCode = companyCodeMap.get(branchKey)

if (companyCode == null) {
    throw new IllegalArgumentException(
        "Company Code mapping not found for BranchName: ${branchName}"
    )
}


message.setProperty("CompanyCode", companyCode)
message.setHeader("CompanyCode", companyCode)

return message
}