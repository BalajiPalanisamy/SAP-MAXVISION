import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {

    // Input can be supplied as Message Headers or Exchange Properties.
    String branchInput = message.getHeader('BranchName', String)
    if (!branchInput) { branchInput = message.getProperty('BranchName') as String }

    String departmentInput = message.getHeader('Department', String)
    if (!departmentInput) { departmentInput = message.getProperty('Department') as String }

    String branchKey = normalize(branchInput)
    String departmentKey = normalize(departmentInput)

    // Department names and accepted aliases.
    Map<String, String> departmentSuffix = [
        'ADMIN'          : 'AD',
        'ADMINISTRATION' : 'AD',
        'EYECARE'        : 'EC',
        'OT'        : 'EC',
        'IOL'        : 'EC',
        'EYE'            : 'EC',
        'FINANCE'        : 'FI',
        'HR'             : 'HR',
        'HUMANRESOURCES' : 'HR',
        'IT'             : 'IT',
        'INFORMATIONTECHNOLOGY': 'IT',
        'MARKETING'      : 'MK',
        'MARKETIN'       : 'MK',
        'OPTICAL'        : 'OP',
        'PHARMACY'       : 'PH'
    ]

    // Branch mappings generated directly from the uploaded Excel.
    // Value format: [canonical branch name, PC base, CC base].
    Map<String, List<String>> branchMap = [
        'SOMAJIGUDA': ['Somajiguda', 'PC-SO01', 'CC-SO01'],
        'ASRAONAGAR': ['ASRao Nagar', 'PC-AS05', 'CC-AS05'],
        'ATTAPUR': ['Attapur', 'PC-AT12', 'CC-AT12'],
        'BEGUMPET': ['Begumpet', 'PC-BE02', 'CC-BE02'],
        'DILSUKHNAGAR': ['Dilsukhnagar', 'PC-DI06', 'CC-DI06'],
        'KOMPALLY': ['Kompally', 'PC-KO10', 'CC-KO10'],
        'KOTHAPET': ['Kothapet', 'PC-KO09', 'CC-KO09'],
        'KUKATPALLY': ['Kukatpally', 'PC-KU04', 'CC-KU04'],
        'MADHAPUR': ['Madhapur', 'PC-MA03', 'CC-MA03'],
        'MANIKONDA': ['Manikonda', 'PC-MA11', 'CC-MA11'],
        'SANTHOSHNAGAR': ['Santhosh Nagar', 'PC-SA07', 'CC-SA07'],
        'VANASTHALIPURAM': ['Vanasthalipuram', 'PC-VA72', 'CC-VA72'],
        'TANVI': ['Tanvi', 'PC-TA58', 'CC-TA58'],
        'KARIMNAGAR': ['Karimnagar', 'PC-KA42', 'CC-KA42'],
        'KHAMMAM': ['Khammam', 'PC-KH43', 'CC-KH43'],
        'WARANGAL': ['Warangal', 'PC-WA41', 'CC-WA41'],
        'ASIFABADVC': ['Asifabad VC', 'PC-AS88', 'CC-AS88'],
        'BHUPALPALLYVC': ['Bhupalpally VC', 'PC-BH92', 'CC-BH92'],
        'KOTHAKONDAVC': ['Kothakonda VC', 'PC-KO09', 'CC-KO09'],
        'MULKNOORVC': ['Mulknoor VC', 'PC-MU85', 'CC-MU85'],
        'MULUGUVC': ['Mulugu VC', 'PC-MU87', 'CC-MU87'],
        'NARSAMPETVC': ['Narsampet VC', 'PC-NA79', 'CC-NA79'],
        'PARAKALAVC': ['Parakala VC', 'PC-PA89', 'CC-PA89'],
        'BHIMAVARAM': ['Bhimavaram', 'PC-BH23', 'CC-BH23'],
        'KAKINADA': ['Kakinada', 'PC-KA22', 'CC-KA22'],
        'RAJAHMUNDRY': ['Rajahmundry', 'PC-RA20', 'CC-RA20'],
        'VIJAYAWADA': ['Vijayawada', 'PC-VI18', 'CC-VI18'],
        'VIZAG': ['Vizag', 'PC-VI19', 'CC-VI19'],
        'GUNTUR': ['Guntur', 'PC-GU81', 'CC-GU81'],
        'MACHERLA': ['Macherla', 'PC-MA80', 'CC-MA80'],
        'NARSARAOPET': ['Narsaraopet', 'PC-NA79', 'CC-NA79'],
        'ABHIRAMAPURAMVC': ['Abhiramapuram VC', 'PC-AB48', 'CC-AB48'],
        'ADYARVC': ['Adyar VC', 'PC-AD49', 'CC-AD49'],
        'AMBATTURVC': ['Ambattur VC', 'PC-AM50', 'CC-AM50'],
        'AMINJIKARAIVC': ['Aminjikarai VC', 'PC-AM51', 'CC-AM51'],
        'ANNANAGAR': ['Annanagar', 'PC-AN35', 'CC-AN35'],
        'MOGAPPAIRVC': ['Mogappair VC', 'PC-MO53', 'CC-MO53'],
        'OMRVC': ['OMR VC', 'PC-OM39', 'CC-OM39'],
        'ROYAPETVC': ['Royapet VC', 'PC-RO55', 'CC-RO55'],
        'VADAPALANIVC': ['Vadapalani VC', 'PC-VA54', 'CC-VA54'],
        'JAYAEYECARE': ['Jaya Eye Care', 'PC-JA36', 'CC-JA36'],
        'HARURVC': ['Harur VC', 'PC-HA52', 'CC-HA52'],
        'KUMBAKONAM': ['Kumbakonam', 'PC-KU34', 'CC-KU34'],
        'MADURAI': ['Madurai', 'PC-MA33', 'CC-MA33'],
        'PERAMBALUR': ['Perambalur', 'PC-PE30', 'CC-PE30'],
        'SALEM': ['Salem', 'PC-SA31', 'CC-SA31'],
        'TRICHY': ['Trichy', 'PC-TR29', 'CC-TR29'],
        'GURVAYUR': ['Gurvayur', 'PC-GU63', 'CC-GU63'],
        'KUNNAMKULAM': ['Kunnamkulam', 'PC-KU65', 'CC-KU65'],
        'THRISSUR': ['Thrissur', 'PC-TH62', 'CC-TH62'],
        'MALABAR': ['Malabar', 'PC-MA59', 'CC-MA59'],
        'JAMNAGAR': ['Jamnagar', 'PC-JA77', 'CC-JA77'],
        'MORBI': ['Morbi', 'PC-MO78', 'CC-MO78'],
        'PEDAKROAD': ['Pedak Road', 'PC-PE74', 'CC-PE74'],
        'SAURASHTRA': ['Saurashtra', 'PC-SA76', 'CC-SA76'],
        'UNIVERSITYROAD': ['University Road', 'PC-UN75', 'CC-UN75'],
        'PARLEPOINT': ['Parle Point', 'PC-PA61', 'CC-PA61'],
        'PARVATPATIA': ['Parvat Patia', 'PC-PA60', 'CC-PA60'],
        'BANDRA': ['Bandra', 'PC-BA67', 'CC-BA67'],
        'KANDIVALI': ['Kandivali', 'PC-KA68', 'CC-KA68'],
        'TRUST': ['Trust', 'PC-TR69', 'CC-TR69'],
        'KUKATPALLYOPTICALWAREHOUSE': ['Kukatpally Optical Warehouse', 'PC-KU04', 'CC-KU04'],
        'MADHIRAVC': ['Madhira VC', 'PC-MA03', 'CC-MA03'],
        'UPPAL': ['Uppal', 'PC-UP14', 'CC-UP14'],
        'NIZAMABAD': ['Nizamabad', 'PC-NI44', 'CC-NI44'],
        'JUHU': ['Juhu', 'PC-JU70', 'CC-JU70'],
        'WARANGALOPTICALS2': ['Warangal Opticals 2', 'PC-WA41', 'CC-WA41'],
        'MADURAIVC': ['Madurai VC', 'PC-MA33', 'CC-MA33'],
        'TRICHYVC': ['Trichy VC', 'PC-TR29', 'CC-TR29'],
        'ONGOLE': ['Ongole', 'PC-ON82', 'CC-ON82'],
        'ANGAMALY': ['Angamaly', 'PC-AN66', 'CC-AN66'],
        'GAJUWAKA': ['Gajuwaka', 'PC-GA25', 'CC-GA25'],
        'TIRUPATI': ['Tirupati', 'PC-TI26', 'CC-TI26'],
        'MVPCOLONY': ['MVP Colony', 'PC-MV24', 'CC-MV24'],
        'MADHURAWADAVC': ['Madhurawada VC', 'PC-MA03', 'CC-MA03'],
        'POWAIVC': ['Powai VC', 'PC-PO71', 'CC-PO71'],
        'MADHIRA': ['Madhira', 'PC-MA03', 'CC-MA03'],
        'SRGADDOR': ['SRG Addor', 'PC-SR56', 'CC-SR56'],
        'SRGSPECTRA': ['SRG Spectra', 'PC-SR56', 'CC-SR56'],
        'SRGSHIVALIK': ['SRG Shivalik', 'PC-SR56', 'CC-SR56'],
        'TTKROAD': ['TTK Road', 'PC-TT37', 'CC-TT37'],
        'MADEENAGUDA': ['Madeenaguda', 'PC-CH08', 'CC-CH08']
    ]

    // These branches exist in the Excel but PC/CC values are not maintained.
    Set<String> unmappedBranches = [
     
        'JANAGOANVC',
        'VIZIANAGARAM',
        'THANJAVUR',
        'WADAKANCHERY',
        'RAJKOT',
        'VEMULAWADAVC',
        'SULTHANABADVC',
        'CHOPPADANDIVC',
        'GHANPURVC',
        'YELLANDUVC',
        'PALAKURTHYVC',
        'MAHABUBABADVC',
        'NETRAMTOEHNOTETRANSFER',
        'VALAPADIVC',
        'ATHOLIVC',
        'VALAPADI',
        'REGIONALOFFICEHYDERABAD',
        'ZPJUNCTIONVC',
        'ANAKAPALLIVC',
        'MARUTIHOSPITALVC',
        'THIRUMALAGIRIVC',
        'THOROURVC',
        'PALAKURTHI',
        'STATIONGHANPURVC',
        'HUZURABADVC',
        'CHOPPADANDI',
        'VEMULAWADA',
        'SULTHANABAD',
        'HUSNABADVC',
        'HUZURNAGARVC',
        'YELLANDU',
        'KALWAKURTHYVC',
        'RASOOLPURA',
        'KARMANGHAT',
        'RCPURAM',
        'LEELAEYECLINIC'
    ] as Set<String>

    message.setProperty('ProfitCenter', '')
    message.setProperty('CostCenter', '')
    message.setProperty('MappingFound', 'false')
    message.setProperty('MappingError', '')

    if (!branchKey || !departmentKey) {
        message.setProperty('MappingError', 'BranchName and Department are mandatory')
        return message
    }

    String suffix = departmentSuffix[departmentKey]
    if (!suffix) {
        message.setProperty('MappingError', 'Invalid Department: ' + departmentInput)
        return message
    }

    if (unmappedBranches.contains(branchKey)) {
        message.setProperty('MappingError', 'PC/CC not maintained in Excel for Branch: ' + branchInput)
        return message
    }

    List<String> branchData = branchMap[branchKey]
    if (!branchData) {
        message.setProperty('MappingError', 'Branch not found: ' + branchInput)
        return message
    }

    String canonicalBranchName = branchData[0]
    String profitCenter = branchData[1] + '-' + suffix
    String costCenter = branchData[2] + '-' + suffix

    message.setProperty('ResolvedBranchName', canonicalBranchName)
    message.setProperty('ProfitCenter', profitCenter)
    message.setProperty('CostCenter', costCenter)
    message.setProperty('MappingFound', 'true')

    return message
}

String normalize(String value) {
    return value == null ? '' : value.trim().toUpperCase().replaceAll('[^A-Z0-9]', '')
}
