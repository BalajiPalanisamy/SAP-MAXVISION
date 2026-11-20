import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {

    def businessPlaceMap = [
    // Telangana / Hyderabad
    "Somajiguda"                   : "MVTS",
    "ASRao Nagar"                  : "MVTS",
    "Attapur"                      : "MVTS",
    "Begumpet"                     : "MVTS",
    "Dilsukhnagar"                 : "MVTS",
    "Kompally"                     : "MVTS",
    "Kothapet"                     : "MVTS",
    "Kukatpally"                   : "MVTS",
    "Madeenaguda"                  : "MVTS",
    "Madhapur"                     : "MVTS",
    "Manikonda"                    : "MVTS",
    "Santhosh Nagar"               : "MVTS",
    "Vanasthalipuram"              : "MVTS",
    "Tanvi"                        : "MVTS",
    "Karimnagar"                   : "MVTS",
    "Khammam"                      : "MVTS",
    "Warangal"                     : "MVTS",
    "Asifabad VC"                  : "MVTS",
    "Bhupalpally VC"               : "MVTS",
    "Janagoan VC"                  : "MVTS",
    "Kothakonda VC"                : "MVTS",
    "Mulknoor VC"                  : "MVTS",
    "Mulugu VC"                    : "MVTS",
    "Narsampet VC"                 : "MVTS",
    "Parakala VC"                  : "MVTS",
    "Kukatpally Optical Warehouse" : "MVTS",
    "Vemulawada VC"                : "MVTS",
    "Sulthanabad VC"               : "MVTS",
    "Choppadandi VC"               : "MVTS",
    "Ghanpur VC"                   : "MVTS",
    "Madhira VC"                   : "MVTS",
    "Yellandu VC"                  : "MVTS",
    "Palakurthy VC"                : "MVTS",
    "Mahabubabad VC"               : "MVTS",
    "Uppal"                        : "MVTS",
    "Nizamabad"                    : "MVTS",
    "Warangal Opticals 2"          : "MVTS",
    "Regional Office - Hyderabad"  : "MVTS",
    "Kalwakurthy VC"               : "MVTS",
    "Rasoolpura"                   : "MVTS",
    "Karmanghat"                   : "MVTS",
    "RC PURAM"                     : "MVTS",

    // Andhra Pradesh
    "Bhimavaram"                   : "MVAP",
    "Kakinada"                     : "MVAP",
    "Rajahmundry"                  : "MVAP",
    "Vizianagaram"                 : "MVAP",
    "Vijayawada"                   : "MVAP",
    "Vizag"                        : "MVAP",
    "Guntur"                       : "MVAP",
    "Macherla"                     : "MVAP",
    "Narsaraopet"                  : "MVAP",
    "Ongole"                       : "MVAP",
    "Gajuwaka"                     : "MVAP",
    "Tirupati"                     : "MVAP",
    "MVP Colony"                   : "MVAP",
    "Madhurawada VC"               : "MVAP",
    "Z. P. Junction VC"            : "MVAP",
    "Anakapalli VC"                : "MVAP",

    // Tamil Nadu
    "Abhiramapuram VC"             : "MVTN",
    "Adyar VC"                     : "MVTN",
    "Ambattur VC"                  : "MVTN",
    "Aminjikarai VC"               : "MVTN",
    "Annanagar"                    : "MVTN",
    "Mogappair VC"                 : "MVTN",
    "OMR VC"                       : "MVTN",
    "Royapet VC"                   : "MVTN",
    "Vadapalani VC"                : "MVTN",
    "Jaya Eye Care"                : "MVTN",
    "Harur VC"                     : "MVTN",
    "Kumbakonam"                   : "MVTN",
    "Madurai"                      : "MVTN",
    "Perambalur"                   : "MVTN",
    "Salem"                        : "MVTN",
    "Thanjavur"                    : "MVTN",
    "Trichy"                       : "MVTN",
    "Valapadi VC"                  : "MVTN",
    "Madurai VC"                   : "MVTN",
    "Trichy VC"                    : "MVTN",
    "TTK Road"                     : "MVTN",
    "Leela Eye Clinic"             : "MVTN",

    // Kerala
    "Gurvayur"                     : "MVKL",
    "Kunnamkulam"                  : "MVKL",
    "Thrissur"                     : "MVKL",
    "Wadakanchery"                 : "MVKL",
    "Malabar"                      : "MVKL",
    "Angamaly"                     : "MVKL",

    // Gujarat
    "Jamnagar"                     : "MVGJ",
    "Morbi"                        : "MVGJ",
    "Pedak Road"                   : "MVGJ",
    "Rajkot"                       : "MVGJ",
    "Saurashtra"                   : "MVGJ",
    "University Road"              : "MVGJ",
    "SRG Addor"                    : "MVGJ",
    "SRG Spectra"                  : "MVGJ",
    "SRG Shivalik"                 : "MVGJ",

    // Gujarat
    "Parle Point"                  : "MVGJ",
    "Parvat Patia"                 : "MVGJ",

    // Maharashtra
    "Bandra"                       : "MVMH",
    "Kandivali"                    : "MVMH",
    "Juhu"                         : "MVMH",
    "Powai VC"                     : "MVMH",

    // Telangana
    "Trust"                        : "MVTS",
    "Netram To Eh Note Transfer"   : "MVTS",
    "Thirumalagiri VC"             : "MVTS",
    "PALAKURTHI"                   : "MVTS",
    "Station Ghanpur VC"           : "MVTS",
    "HUZURABAD VC"                 : "MVTS",
    "CHOPPADANDI"                  : "MVTS",
    "VEMULAWADA"                   : "MVTS",
    "SULTHANABAD"                  : "MVTS",
    "Husnabad VC"                  : "MVTS",
    "Madhira"                      : "MVTS",
    "Huzurnagar VC"                : "MVTS",
    "YELLANDU"                     : "MVTS",

    // Kerala
    "Atholi VC"                    : "MVKL",
    "Thorour VC"                   : "MVKL",

    // Tamil Nadu
    "Valapadi"                     : "MVTN",
    "Maruti Hospital VC"           : "MVTN"
]


    // Section Code mapping based on Business Place
    def sectionCodeMap = [
        "MVTS" : "1840",
        "MVAP" : "1811",
        "MVGJ" : "1821",
        "MVTN" : "1841",
        "MVMH" : "1829",
        "MVKL" : "1827"
    ]


    // Read BranchName from Header
    def branchName = message.getHeader("BranchName", String)

    if (!branchName) {
        throw new Exception("BranchName header is missing")
    }

    branchName = branchName.trim()
                           .replaceAll("\\s+", " ")


    def businessPlace = businessPlaceMap.find { key, value ->
        key.equalsIgnoreCase(branchName)
    }?.value

    if (!businessPlace) {
        throw new Exception(
            "Business Place mapping not found for BranchName: " + branchName
        )
    }


    // Get Section Code based on Business Place
    def sectionCode = sectionCodeMap[businessPlace]

    if (!sectionCode) {
        throw new Exception(
            "Section Code mapping not found for BusinessPlace: " + businessPlace
        )
    }


    // Existing Output Property
    message.setProperty("BusinessPlace", businessPlace)

    // New Output Property
    message.setProperty("SectionCode", sectionCode)

    return message
}