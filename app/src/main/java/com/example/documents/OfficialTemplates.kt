package com.example.documents

data class OfficialTemplate(
    val id: String,
    val titleHi: String,
    val titleEn: String,
    val category: String, // "अवकाश (Leaves)", "वेतन एवं सेवा (Salary & Service)", "प्रशासनिक (Administrative)", "स्थानांतरण (Transfer)"
    val defaultRecipientHi: String,
    val defaultRecipientEn: String,
    val defaultSubjectHi: String,
    val defaultSubjectEn: String,
    val defaultBodyHi: String,
    val defaultBodyEn: String
)

object OfficialTemplatesData {

    val templates: List<OfficialTemplate> = listOf(
        OfficialTemplate(
            id = "cl_application",
            titleHi = "आकस्मिक अवकाश (Casual Leave - CL) हेतु आवेदन",
            titleEn = "Casual Leave Application",
            category = "अवकाश (Leaves)",
            defaultRecipientHi = "सेवा में,\nप्रधानाध्यापक महोदय,\n[विद्यालय का नाम], [प्रखंड], [जिला]",
            defaultRecipientEn = "To,\nThe Headmaster,\n[School Name], [Block], [District]",
            defaultSubjectHi = "आकस्मिक अवकाश (CL) स्वीकृत करने के संबंध में।",
            defaultSubjectEn = "Application for grant of Casual Leave (CL).",
            defaultBodyHi = "महाशय,\nसविनय निवेदन है कि मुझे आवश्यक घरेलू कार्य आ जाने के कारण मैं दिनांक [प्रारंभ दिनांक] से [अंतिम दिनांक] तक (कुल [दिन संख्या] दिवस) विद्यालय में उपस्थित रहने में असमर्थ रहूँगा/रहूँगी।\nअतः श्रीमान से विनम्र प्रार्थना है कि उक्त अवधि का आकस्मिक अवकाश स्वीकृत करने की कृपा की जाए। इसके लिए मैं सदा आभारी रहूँगा/रहूँगी।",
            defaultBodyEn = "Respected Sir/Madam,\nI beg to state that due to urgent domestic work, I will be unable to attend school from [Start Date] to [End Date] (Total [Number of Days] days).\nTherefore, I kindly request you to grant me Casual Leave for the said period."
        ),
        OfficialTemplate(
            id = "medical_leave",
            titleHi = "चिकित्सा अवकाश (Medical Leave) हेतु आवेदन",
            titleEn = "Medical Leave Application",
            category = "अवकाश (Leaves)",
            defaultRecipientHi = "सेवा में,\nप्रखंड शिक्षा पदाधिकारी,\n[प्रखंड का नाम], [जिला], बिहार\n(द्वारा: प्रधानाध्यापक)",
            defaultRecipientEn = "To,\nThe Block Education Officer (BEO),\n[Block Name], [District], Bihar\n(Through: Headmaster)",
            defaultSubjectHi = "चिकित्सा अवकाश स्वीकृत करने एवं अनुपस्थिति नियमितीकरण के संबंध में।",
            defaultSubjectEn = "Application for grant of Medical Leave and regularization.",
            defaultBodyHi = "महाशय,\nसविनय निवेदन है कि अचानक अस्वस्थ हो जाने के कारण चिकित्सक द्वारा मुझे दिनांक [प्रारंभ दिनांक] से [अंतिम दिनांक] तक पूर्ण विश्राम की सलाह दी गई थी। संलग्न मेडिकल प्रमाण पत्र एवं फिटनेस प्रमाण पत्र के आलोक में मैं आज दिनांक [योगदान दिनांक] को विद्यालय में अपना योगदान समर्पित कर रहा/रही हूँ।\nअतः श्रीमान से प्रार्थना है कि उक्त अवधि का चिकित्सा अवकाश स्वीकृत करने की कृपा की जाए।",
            defaultBodyEn = "Respected Sir/Madam,\nI respectfully submit that due to sudden illness, the registered medical practitioner advised me complete bed rest from [Start Date] to [End Date]. Enclosing the medical fitness certificate, I am resuming my duties today.\nKindly sanction my Medical Leave for the said period."
        ),
        OfficialTemplate(
            id = "earned_leave",
            titleHi = "अर्जित अवकाश (Earned Leave - EL) हेतु आवेदन",
            titleEn = "Earned Leave Application",
            category = "अवकाश (Leaves)",
            defaultRecipientHi = "सेवा में,\nजिला कार्यक्रम पदाधिकारी (स्थापना),\nशिक्षा विभाग, [जिला], बिहार\n(द्वारा: प्रखंड शिक्षा पदाधिकारी)",
            defaultRecipientEn = "To,\nThe District Programme Officer (Establishment),\nEducation Department, [District], Bihar",
            defaultSubjectHi = "अर्जित अवकाश (Earned Leave) की स्वीकृति के संबंध में।",
            defaultSubjectEn = "Application for sanction of Earned Leave (EL).",
            defaultBodyHi = "महाशय,\nसविनय निवेदन है कि अत्यंत अनिवार्य पारिवारिक एवं सामाजिक दायित्वों के निर्वहन हेतु मुझे दिनांक [प्रारंभ दिनांक] से [अंतिम दिनांक] तक (कुल [दिन संख्या] दिन) अर्जित अवकाश की आवश्यकता है। मेरे अवकाश खाते में पर्याप्त अर्जित अवकाश शेष है।\nअतः नम्र निवेदन है कि उक्त अर्जित अवकाश स्वीकृत करने का आदेश निर्गत करने की कृपा की जाए।",
            defaultBodyEn = "Respected Sir/Madam,\nI beg to request sanction of [Number of days] days of Earned Leave from [Start Date] to [End Date] for urgent family responsibilities. Sufficient EL balance is available in my Service Book."
        ),
        OfficialTemplate(
            id = "special_cl",
            titleHi = "विशेष आकस्मिक अवकाश (Special CL) हेतु आवेदन",
            titleEn = "Special Casual Leave Application",
            category = "अवकाश (Leaves)",
            defaultRecipientHi = "सेवा में,\nप्रधानाध्यापक / प्रखंड शिक्षा पदाधिकारी,\n[प्रखंड], [जिला], बिहार",
            defaultRecipientEn = "To,\nThe Headmaster / BEO,\n[Block], [District], Bihar",
            defaultSubjectHi = "महिला शिक्षिका हेतु दो दिवसीय विशेष आकस्मिक अवकाश के संबंध में।",
            defaultSubjectEn = "Application for Special Casual Leave for Women Teachers.",
            defaultBodyHi = "महाशय,\nसविनय निवेदन है कि बिहार सरकार शिक्षा विभाग के संकल्प के आलोक में महिला कर्मियों को प्रत्येक माह दो दिवसीय विशेष आकस्मिक अवकाश देय है। अतः मुझे इस माह दिनांक [प्रारंभ दिनांक] एवं दिनांक [अंतिम दिनांक] को दो दिनों का विशेष आकस्मिक अवकाश प्रदान करने की कृपा की जाए।",
            defaultBodyEn = "Respected Sir,\nIn accordance with Bihar Government education rules granting 2 days of Special Casual Leave per month to women teachers, I request approval for special CL on [Dates]."
        ),
        OfficialTemplate(
            id = "maternity_leave",
            titleHi = "मातृत्व अवकाश (Maternity Leave) हेतु आवेदन",
            titleEn = "Maternity Leave Application",
            category = "अवकाश (Leaves)",
            defaultRecipientHi = "सेवा में,\nजिला कार्यक्रम पदाधिकारी (स्थापना),\nशिक्षा विभाग, [जिला], बिहार",
            defaultRecipientEn = "To,\nThe District Programme Officer (Establishment),\nEducation Department, [District], Bihar",
            defaultSubjectHi = "180 दिनों का सवैतनिक मातृत्व अवकाश स्वीकृत करने के संबंध में।",
            defaultSubjectEn = "Sanction of 180 days paid Maternity Leave.",
            defaultBodyHi = "महाशय,\nसविनय निवेदन है कि चिकित्सकीय परीक्षण अनुसार मेरी संभावित प्रसव तिथि [संभावित प्रसव तिथि] है। अतः बिहार सेवा संहिता के प्रावधानों के तहत मुझे दिनांक [प्रारंभ दिनांक] से 180 दिनों का सवैतनिक मातृत्व अवकाश प्रदान करने की कृपा की जाए। चिकित्सक का प्रमाण पत्र संलग्न है।",
            defaultBodyEn = "Respected Sir,\nAs per the medical advice, the expected date of delivery is [EDD]. In accordance with Bihar Service Rules, I request you to sanction 180 days of paid Maternity Leave commencing from [Start Date]."
        ),
        OfficialTemplate(
            id = "paternity_leave",
            titleHi = "पितृत्व अवकाश (Paternity Leave) हेतु आवेदन",
            titleEn = "Paternity Leave Application",
            category = "अवकाश (Leaves)",
            defaultRecipientHi = "सेवा में,\nप्रखंड शिक्षा पदाधिकारी / डीपीओ स्थापना,\n[प्रखंड], [जिला], बिहार",
            defaultRecipientEn = "To,\nThe BEO / DPO (Establishment),\n[Block], [District], Bihar",
            defaultSubjectHi = "15 दिनों का पितृत्व अवकाश (Paternity Leave) स्वीकृत करने हेतु।",
            defaultSubjectEn = "Application for 15 days Paternity Leave.",
            defaultBodyHi = "महाशय,\nसविनय निवेदन है कि मेरी धर्मपत्नी का प्रसव दिनांक [प्रसव तिथि] को हुआ है। नवजात शिशु एवं माता की उचित देखभाल हेतु मुझे नियमानुसार 15 दिनों के पितृत्व अवकाश की आवश्यकता है। अतः दिनांक [प्रारंभ दिनांक] से [अंतिम दिनांक] तक पितृत्व अवकाश स्वीकृत करने की कृपा की जाए।",
            defaultBodyEn = "Respected Sir,\nI request sanction of 15 days of Paternity Leave from [Start Date] to [End Date] to take care of my newly born child and wife."
        ),
        OfficialTemplate(
            id = "salary_pending",
            titleHi = "बकाया वेतन भुगतान (Salary Arrear / Pending Salary) हेतु आवेदन",
            titleEn = "Application for Pending Salary Clearance",
            category = "वेतन एवं सेवा (Salary & Service)",
            defaultRecipientHi = "सेवा में,\nजिला कार्यक्रम पदाधिकारी (स्थापना),\nशिक्षा विभाग, [जिला], बिहार\n(द्वारा: प्रखंड शिक्षा पदाधिकारी)",
            defaultRecipientEn = "To,\nThe District Programme Officer (Establishment),\nEducation Department, [District], Bihar",
            defaultSubjectHi = "माह [बकाया माह/वर्ष] का बकाया वेतन भुगतान करने के संबंध में।",
            defaultSubjectEn = "Request for payment of pending salary for [Month/Year].",
            defaultBodyHi = "महाशय,\nसविनय निवेदन है कि मैं [विद्यालय का नाम] में [पदनाम] के पद पर पदस्थापित हूँ। मेरा माह [माह/वर्ष] से [माह/वर्ष] तक का वेतन तकनीकी कारणों / अनुपस्थिति विवरणी त्रुटि / आवंटन अभाव के कारण अप्राप्त है। उक्त अवधि में मेरा कार्य संतोषप्रद रहा है एवं अनुपस्थिति विवरणी पूर्व में ही प्रेषित की जा चुकी है।\nअतः विनम्र प्रार्थना है कि बकाया वेतन भुगतान हेतु विपत्र पारित करने का आदेश दिया जाए।",
            defaultBodyEn = "Respected Sir,\nI respectfully state that my salary for the period [Month/Year] has remained unpaid due to administrative/allotment reasons. My service record during the period has been satisfactory. Kindly issue directions to release the pending salary arrears."
        ),
        OfficialTemplate(
            id = "salary_correction",
            titleHi = "वेतन विसंगति / वेतन सुधार (Salary Correction) हेतु आवेदन",
            titleEn = "Salary Discrepancy & Correction Request",
            category = "वेतन एवं सेवा (Salary & Service)",
            defaultRecipientHi = "सेवा में,\nजिला कार्यक्रम पदाधिकारी (स्थापना),\nशिक्षा विभाग, [जिला], बिहार",
            defaultRecipientEn = "To,\nThe District Programme Officer (Establishment),\nEducation Department, [District], Bihar",
            defaultSubjectHi = "वेतन पर्ची में मूल वेतन / DA / HRA की विसंगति सुधारने के संबंध में।",
            defaultSubjectEn = "Correction of Basic Pay / DA / HRA in monthly salary slip.",
            defaultBodyHi = "महाशय,\nसविनय निवेदन है कि ई-शिक्षाकोष पोर्टल / पे-स्लिप पर मेरा मूल वेतन नियमानुसार [सही मूल वेतन] के स्थान पर त्रुटिवश [गलत मूल वेतन] अंकित हो रहा है, जिससे प्रतिमाह आर्थिक क्षति हो रही है। सेवा पुस्तिका के पृष्ठ संख्या की प्रति संलग्न है।\nअतः श्रीमान से अनुरोध है कि वेतन विसंगति का त्वरित निराकरण करने की कृपा की जाए।",
            defaultBodyEn = "Respected Sir,\nI state that my Basic Pay is incorrectly reflected in the payroll system as [Incorrect Amount] instead of [Correct Amount]. Relevant copies of the Service Book are enclosed for correction."
        ),
        OfficialTemplate(
            id = "joining_report",
            titleHi = "विद्यालय योगदान प्रतिवेदन (Joining Report)",
            titleEn = "Teacher Joining Report",
            category = "प्रशासनिक (Administrative)",
            defaultRecipientHi = "सेवा में,\nप्रधानाध्यापक महोदय,\n[विद्यालय का नाम], [प्रखंड], [जिला]",
            defaultRecipientEn = "To,\nThe Headmaster,\n[School Name], [Block], [District]",
            defaultSubjectHi = "विद्यालय में पदभार ग्रहण / योगदान समर्पित करने के संबंध में।",
            defaultSubjectEn = "Joining Report for resumption of duties.",
            defaultBodyHi = "महाशय,\nकार्यालय आदेश ज्ञापांक [ज्ञापांक संख्या] दिनांक [दिनांक] के अनुपालन में मैं अधोहस्ताक्षरी आज दिनांक [योगदान दिनांक] को पूर्वाह्न में [विद्यालय का नाम] में [पदनाम] के पद पर अपना विधिवत योगदान समर्पित कर रहा/रही हूँ।\nअतः योगदान प्रतिवेदन स्वीकृत करते हुए सेवा पुस्तिका में प्रविष्टि करने की कृपा की जाए।",
            defaultBodyEn = "Respected Sir,\nIn compliance with Order Memo No. [Memo No] dated [Date], I hereby submit my formal joining report today forenoon for the post of [Designation]. Please accept my joining report."
        ),
        OfficialTemplate(
            id = "relieving_request",
            titleHi = "विरमन आदेश (Relieving Order) हेतु आवेदन",
            titleEn = "Relieving Order Request",
            category = "प्रशासनिक (Administrative)",
            defaultRecipientHi = "सेवा में,\nप्रधानाध्यापक महोदय,\n[विद्यालय का नाम], [प्रखंड], [जिला]",
            defaultRecipientEn = "To,\nThe Headmaster,\n[School Name], [Block], [District]",
            defaultSubjectHi = "अन्य विद्यालय में पदस्थापन / स्थानांतरण हेतु विरमित करने के संबंध में।",
            defaultSubjectEn = "Request for relieving order for joining new posting.",
            defaultBodyHi = "महाशय,\nशिक्षा विभाग, बिहार सरकार के स्थानांतरण आदेश ज्ञापांक [ज्ञापांक] दिनांक [दिनांक] के आलोक में मेरा स्थानांतरण [नवीन विद्यालय] में किया गया है। विद्यालय से संबंधित सभी प्रभार मैंने विधिवत हस्तगत करा दिए हैं।\nअतः मुझे आज अपराह्न से नए पदस्थापन हेतु विरमित करने का कष्ट करें।",
            defaultBodyEn = "Respected Sir,\nPursuant to the Department transfer order Memo No. [Memo No], I have been transferred to [New School Name]. Having handed over all charges, please relieve me today afternoon."
        ),
        OfficialTemplate(
            id = "transfer_application",
            titleHi = "स्थानांतरण (Transfer Application) हेतु आवेदन",
            titleEn = "Teacher Transfer Application",
            category = "स्थानांतरण (Transfer)",
            defaultRecipientHi = "सेवा में,\nजिला शिक्षा पदाधिकारी (DEO),\n[जिला], बिहार",
            defaultRecipientEn = "To,\nThe District Education Officer (DEO),\n[District], Bihar",
            defaultSubjectHi = "गंभीर पारिवारिक / स्वास्थ्य कारणों से ऐच्छिक स्थानांतरण के संबंध में।",
            defaultSubjectEn = "Application for transfer on medical / compassionate grounds.",
            defaultBodyHi = "महाशय,\nसविनय निवेदन है कि मैं वर्तमान में [वर्तमान विद्यालय] में कार्यरत हूँ। मेरे गृह स्थान से विद्यालय की दूरी लगभग [किलोमीटर] है तथा वृद्ध माता-पिता के स्वास्थ्य एवं पारिवारिक परिस्थितियों के कारण विद्यालय आने-जाने में अत्यधिक कठिनाई हो रही है।\nअतः नम्र निवेदन है कि मुझे [इच्छित प्रखंड/विद्यालय] में रिक्त पद के विरुद्ध स्थानांतरित करने की कृपा की जाए।",
            defaultBodyEn = "Respected Sir,\nI request you to consider my transfer application from [Current School] to [Desired School/Block] on health and compassionate family grounds."
        ),
        OfficialTemplate(
            id = "mutual_transfer",
            titleHi = "पारस्परिक स्थानांतरण (Mutual Transfer) हेतु संयुक्त आवेदन",
            titleEn = "Mutual Transfer Joint Application",
            category = "स्थानांतरण (Transfer)",
            defaultRecipientHi = "सेवा में,\nनिदेशक (प्राथमिक शिक्षा) / जिला शिक्षा पदाधिकारी,\nशिक्षा विभाग, बिहार",
            defaultRecipientEn = "To,\nThe Director (Primary Education) / DEO,\nEducation Department, Bihar",
            defaultSubjectHi = "समान संवर्ग एवं विषय में पारस्परिक स्थानांतरण (Mutual Transfer) की स्वीकृति हेतु।",
            defaultSubjectEn = "Joint application for mutual transfer in identical cadre.",
            defaultBodyHi = "महाशय,\nहम दोनों शिक्षक/शिक्षिका एक ही संवर्ग एवं विषय में कार्यरत हैं तथा आपसी सहमति से एक-दूसरे के पदस्थापित विद्यालय में पारस्परिक स्थानांतरण चाहते हैं। हम दोनों का संयुक्त अनापत्ति शपथ पत्र संलग्न है।\nअतः हमारे पारस्परिक स्थानांतरण की अनुशंसा/स्वीकृति प्रदान करने की कृपा की जाए।",
            defaultBodyEn = "Respected Sir,\nWe the undersigned teachers working in identical cadre and subject request mutual transfer with mutual consent. Relevant affidavits and no-objection documents are enclosed."
        ),
        OfficialTemplate(
            id = "noc_application",
            titleHi = "अनापत्ति प्रमाण पत्र (NOC) हेतु आवेदन",
            titleEn = "NOC (No Objection Certificate) Application",
            category = "प्रशासनिक (Administrative)",
            defaultRecipientHi = "सेवा में,\nजिला कार्यक्रम पदाधिकारी (स्थापना),\nशिक्षा विभाग, [जिला], बिहार",
            defaultRecipientEn = "To,\nThe District Programme Officer (Establishment),\nEducation Department, [District], Bihar",
            defaultSubjectHi = "उच्च शिक्षा / प्रतियोगिता परीक्षा में सम्मिलित होने हेतु अनापत्ति प्रमाण पत्र (NOC) निर्गत करने के संबंध में।",
            defaultSubjectEn = "Application for issuance of No Objection Certificate (NOC).",
            defaultBodyHi = "महाशय,\nसविनय निवेदन है कि मैंने [परीक्षा/आयोग का नाम, जैसे BPSC] द्वारा विज्ञापित [पद का नाम] हेतु आवेदन किया है। विभागीय नियमानुसार साक्षात्कार / अभिलेख सत्यापन हेतु अनापत्ति प्रमाण पत्र (NOC) आवश्यक है।\nअतः श्रीमान से प्रार्थना है कि मुझे अनापत्ति प्रमाण पत्र निर्गत करने की कृपा की जाए।",
            defaultBodyEn = "Respected Sir,\nI have appeared/applied for the examination conducted by [Commission/Authority] and require an official No Objection Certificate (NOC) for verification."
        ),
        OfficialTemplate(
            id = "lpc_request",
            titleHi = "अंतिम वेतन प्रमाण पत्र (LPC - Last Pay Certificate) हेतु आवेदन",
            titleEn = "Last Pay Certificate (LPC) Request",
            category = "वेतन एवं सेवा (Salary & Service)",
            defaultRecipientHi = "सेवा में,\nप्रखंड शिक्षा पदाधिकारी / निकासी एवं व्ययन पदाधिकारी (DDO),\n[प्रखंड], [जिला], बिहार",
            defaultRecipientEn = "To,\nThe Drawing and Disbursing Officer (DDO) / BEO,\n[Block], [District], Bihar",
            defaultSubjectHi = "नवीन पदस्थापन विद्यालय हेतु अंतिम वेतन प्रमाण पत्र (LPC) निर्गत करने के संबंध में।",
            defaultSubjectEn = "Request for issuance of Last Pay Certificate (LPC).",
            defaultBodyHi = "महाशय,\nसविनय निवेदन है कि मेरा स्थानांतरण [पुराने विद्यालय] से [नए विद्यालय] में हो गया है तथा मैंने दिनांक [योगदान दिनांक] को नवीन विद्यालय में योगदान कर लिया है। नवीन पदस्थापन स्थल पर नियमित वेतन भुगतान हेतु अंतिम वेतन प्रमाण पत्र (LPC) की आवश्यकता है।\nअतः मेरा LPC शीघ्र निर्गत करने की कृपा की जाए।",
            defaultBodyEn = "Respected Sir,\nFollowing my transfer and joining at [New School Name], kindly issue my Last Pay Certificate (LPC) to enable drawal of monthly salary."
        ),
        OfficialTemplate(
            id = "service_book_correction",
            titleHi = "सेवा पुस्तिका संशोधन / प्रविष्टि (Service Book Entry) हेतु आवेदन",
            titleEn = "Service Book Entry & Correction Application",
            category = "वेतन एवं सेवा (Salary & Service)",
            defaultRecipientHi = "सेवा में,\nप्रखंड शिक्षा पदाधिकारी,\n[प्रखंड का नाम], [जिला], बिहार",
            defaultRecipientEn = "To,\nThe Block Education Officer,\n[Block Name], [District], Bihar",
            defaultSubjectHi = "सेवा पुस्तिका में शैक्षणिक योग्यता / वेतन निर्धारण / स्थायीकरण की प्रविष्टि के संबंध में।",
            defaultSubjectEn = "Application for entry and verification in Service Book.",
            defaultBodyHi = "महाशय,\nसविनय निवेदन है कि मेरे द्वारा प्राप्त उच्चतर शैक्षणिक योग्यता / विभागीय प्रशिक्षण प्रमाण पत्र की मूल एवं स्वप्रमाणित प्रति संलग्न है। कृपया मेरी सेवा पुस्तिका में इसका विधिवत सत्यापन एवं प्रविष्टि करने का कष्ट करें।",
            defaultBodyEn = "Respected Sir,\nI have acquired additional recognized educational qualification / completed mandatory training. Please make necessary verification and entry in my official Service Book."
        ),
        OfficialTemplate(
            id = "training_permission",
            titleHi = "प्रशिक्षण / कार्यशाला (Training / Workshop Permission) हेतु आवेदन",
            titleEn = "Permission for Training / Workshop",
            category = "प्रशासनिक (Administrative)",
            defaultRecipientHi = "सेवा में,\nप्रधानाध्यापक महोदय,\n[विद्यालय का नाम], [प्रखंड], [जिला]",
            defaultRecipientEn = "To,\nThe Headmaster,\n[School Name], [Block], [District]",
            defaultSubjectHi = "SCERT / DIET द्वारा आयोजित प्रशिक्षण में प्रतिभागिता एवं कार्यमुक्ति के संबंध में।",
            defaultSubjectEn = "Permission and duty leave for attending SCERT/DIET training.",
            defaultBodyHi = "महाशय,\nSCERT / डायट [स्थान] द्वारा दिनांक [प्रारंभ दिनांक] से [अंतिम दिनांक] तक आयोजित प्रशिक्षण कार्यक्रम हेतु मेरा चयन किया गया है। पत्र संख्या [पत्र संख्या] संलग्न है।\nअतः मुझे उक्त प्रशिक्षण में भाग लेने हेतु कर्तव्य अवकाश (Official Duty) पर कार्यमुक्त करने की कृपा की जाए।",
            defaultBodyEn = "Respected Sir,\nI have been deputed for the teacher training program organized by SCERT/DIET from [Start Date] to [End Date]. Please relieve me on official duty."
        ),
        OfficialTemplate(
            id = "election_duty",
            titleHi = "निर्वाचन कर्तव्य (Election Duty) पारिश्रमिक / अवकाश आवेदन",
            titleEn = "Election Duty Remuneration & Leave Application",
            category = "प्रशासनिक (Administrative)",
            defaultRecipientHi = "सेवा में,\nप्रखंड विकास पदाधिकारी सह सहायक निर्वाची पदाधिकारी,\n[प्रखंड], [जिला], बिहार",
            defaultRecipientEn = "To,\nThe Returning Officer / BDO,\n[Block], [District], Bihar",
            defaultSubjectHi = "निर्वाचन कार्य संपादन पश्चात पारिश्रमिक भुगतान एवं उपस्थिति प्रमाण पत्र हेतु।",
            defaultSubjectEn = "Request for Election Duty Certificate and Remuneration.",
            defaultBodyHi = "महाशय,\nसविनय निवेदन है कि विगत निर्वाचन में मेरी प्रतिनियुक्ति मतदान दल [P1/P2/P3/PRO] के रूप में बूथ संख्या [बूथ संख्या] पर की गई थी। मतदान का कार्य शांतिपूर्ण ढंग से संपन्न करा दिया गया है।\nअतः मुझे निर्वाचन उपस्थिति प्रमाण पत्र एवं देय यात्रा/मानदेय भुगतान करने की कृपा की जाए।",
            defaultBodyEn = "Respected Sir,\nI successfully performed election duty at Polling Station No. [Booth No]. Kindly issue the duty certificate and disburse admissible remuneration."
        ),
        OfficialTemplate(
            id = "census_duty_payment",
            titleHi = "जनगणना / जातीय गणना (Census Duty) मानदेय भुगतान आवेदन",
            titleEn = "Census / Survey Duty Remuneration Request",
            category = "प्रशासनिक (Administrative)",
            defaultRecipientHi = "सेवा में,\nचार्ज अधिकारी सह प्रखंड विकास पदाधिकारी,\n[प्रखंड], [जिला], बिहार",
            defaultRecipientEn = "To,\nThe Charge Officer cum BDO,\n[Block], [District], Bihar",
            defaultSubjectHi = "गणना कार्य के बकाया मानदेय भुगतान के संबंध में।",
            defaultSubjectEn = "Application for disbursement of Census / Survey honorarium.",
            defaultBodyHi = "महाशय,\nसविनय निवेदन है कि प्रगणक / पर्यवेक्षक के रूप में मुझे आवंटित गणना ब्लॉक संख्या [ब्लॉक संख्या] का संपूर्ण कार्य समय-सीमा के भीतर पूर्ण कर प्रपत्र एवं पोर्टल पर जमा कर दिया गया है। परंतु अब तक मानदेय अप्राप्त है।\nअतः मानदेय की राशि बैंक खाते में अंतरित करने की कृपा की जाए।",
            defaultBodyEn = "Respected Sir,\nHaving successfully completed enumerator / supervisor duties for assigned Census Block No. [Block No], I request disbursement of pending honorarium."
        ),
        OfficialTemplate(
            id = "explanation_reply",
            titleHi = "स्पष्टीकरण का जवाब (Explanation Reply / Clarification)",
            titleEn = "Written Explanation / Clarification Reply",
            category = "प्रशासनिक (Administrative)",
            defaultRecipientHi = "सेवा में,\nप्रखंड शिक्षा पदाधिकारी / जिला कार्यक्रम पदाधिकारी,\n[प्रखंड], [जिला], बिहार",
            defaultRecipientEn = "To,\nThe Block Education Officer / DPO,\n[Block], [District], Bihar",
            defaultSubjectHi = "आपके पत्रांक [पत्रांक] दिनांक [दिनांक] के स्पष्टीकरण के उत्तर के संबंध में।",
            defaultSubjectEn = "Reply to letter/memo regarding explanation.",
            defaultBodyHi = "महाशय,\nउपर्युक्त विषयक आपके पत्रांक [पत्रांक] के संदर्भ में सादर स्पष्टीकरण प्रस्तुत है कि दिनांक [निरीक्षण दिनांक] को निरीक्षण दल के भ्रमण समय [उचित कारण, जैसे विभागीय प्रशिक्षण / चुनाव कार्य / अचानक स्वास्थ्य बिगड़ने] के कारण मैं विद्यालय में भौतिक रूप से अनुपस्थित था/थी, जिसकी पूर्व सूचना विद्यालय के उपस्थिति पंजी एवं प्रधानाध्यापक को दी गई थी।\nमेरा यह कृत्य किसी अनुशासनहीनता के तहत नहीं था। अतः मेरा यह स्पष्टीकरण स्वीकार करते हुए आरोप मुक्त करने की कृपा की जाए।",
            defaultBodyEn = "Respected Sir,\nIn reference to your memo dated [Date], I submit that my absence during the inspection was due to unavoidable official duty / sudden health emergency, of which prior intimation was given. Please accept this explanation."
        ),
        OfficialTemplate(
            id = "rti_application",
            titleHi = "सूचना का अधिकार (RTI Application - Form K)",
            titleEn = "RTI (Right to Information) Application",
            category = "प्रशासनिक (Administrative)",
            defaultRecipientHi = "सेवा में,\nलोक सूचना पदाधिकारी (PIO),\nकार्यालय जिला शिक्षा पदाधिकारी, [जिला], बिहार",
            defaultRecipientEn = "To,\nThe Public Information Officer (PIO),\nOffice of the DEO, [District], Bihar",
            defaultSubjectHi = "सूचना का अधिकार अधिनियम, 2005 के तहत सूचना उपलब्ध कराने हेतु।",
            defaultSubjectEn = "Application under Right to Information Act, 2005.",
            defaultBodyHi = "महाशय,\nसूचना का अधिकार अधिनियम, 2005 की धारा 6(1) के तहत मैं निम्नलिखित सूचनाएं प्रमाणित प्रति के साथ प्राप्त करना चाहता/चाहती हूँ:\n1. [बिंदु 1: वांछित सूचना का विवरण]\n2. [बिंदु 2: संबंधित पत्र/कार्यालय आदेश की प्रति]\nनिर्धारित आवेदन शुल्क ₹10 का भारतीय पोस्टल ऑर्डर संख्या [IPO No] संलग्न है।",
            defaultBodyEn = "Respected Sir,\nUnder Section 6(1) of RTI Act 2005, please furnish certified copies of the following information: [Details]. Prescribed fee of Rs. 10 is attached via Postal Order."
        ),
        OfficialTemplate(
            id = "app_to_beo",
            titleHi = "प्रखंड शिक्षा पदाधिकारी (BEO) को सामान्य आवेदन",
            titleEn = "Official Application to BEO",
            category = "प्रशासनिक (Administrative)",
            defaultRecipientHi = "सेवा में,\nप्रखंड शिक्षा पदाधिकारी,\n[प्रखंड का नाम], [जिला], बिहार",
            defaultRecipientEn = "To,\nThe Block Education Officer (BEO),\n[Block Name], [District], Bihar",
            defaultSubjectHi = "[आवेदन का विषय दर्ज करें]",
            defaultSubjectEn = "[Enter Application Subject]",
            defaultBodyHi = "महाशय,\nसविनय निवेदन है कि [यहाँ अपनी समस्या या अनुरोध का विस्तार से उल्लेख करें]।\nअतः श्रीमान से सादर अनुरोध है कि उपर्युक्त मामले में यथोचित प्रशासनिक कार्रवाई करने का कष्ट करें।",
            defaultBodyEn = "Respected Sir,\nI respectfully submit the following matter for your kind attention and necessary administrative action: [Details]."
        ),
        OfficialTemplate(
            id = "app_to_deo",
            titleHi = "जिला शिक्षा पदाधिकारी (DEO) को आवेदन",
            titleEn = "Official Application to DEO",
            category = "प्रशासनिक (Administrative)",
            defaultRecipientHi = "सेवा में,\nजिला शिक्षा पदाधिकारी (DEO),\nशिक्षा विभाग, [जिला], बिहार",
            defaultRecipientEn = "To,\nThe District Education Officer (DEO),\nEducation Department, [District], Bihar",
            defaultSubjectHi = "[विषय दर्ज करें]",
            defaultSubjectEn = "[Enter Subject]",
            defaultBodyHi = "महाशय,\nसविनय निवेदन है कि [समस्या/अनुरोध का संक्षिप्त विवरण]। इस संबंध में पूर्व में प्रखंड स्तर पर भी आवेदन समर्पित किया जा चुका है।\nअतः श्रीमान से विनम्र प्रार्थना है कि संज्ञान लेते हुए आवश्यक आदेश निर्गत करने की कृपा की जाए।",
            defaultBodyEn = "Respected Sir,\nI bring to your kind attention the following matter regarding [Issue], previously submitted at block level. Kindly intervene and issue appropriate directions."
        ),
        OfficialTemplate(
            id = "app_to_dpo_establishment",
            titleHi = "जिला कार्यक्रम पदाधिकारी (DPO स्थापना) को आवेदन",
            titleEn = "Application to DPO (Establishment)",
            category = "प्रशासनिक (Administrative)",
            defaultRecipientHi = "सेवा में,\nजिला कार्यक्रम पदाधिकारी (स्थापना),\nशिक्षा विभाग, [जिला], बिहार",
            defaultRecipientEn = "To,\nThe District Programme Officer (Establishment),\nEducation Department, [District], Bihar",
            defaultSubjectHi = "वेतन भुगतान / सेवा संपुष्टि / वेतन निर्धारण के संबंध में।",
            defaultSubjectEn = "Regarding Salary / Pay Fixation / Service Confirmation.",
            defaultBodyHi = "महाशय,\nसविनय निवेदन है कि [स्थापना शाखा से संबंधित विषय का विस्तृत ब्यौरा यहाँ लिखें]।\nअतः श्रीमान से प्रार्थना है कि संबंधित लिपिक को आवश्यक निर्देश देकर कार्य निष्पादन कराने की कृपा की जाए।",
            defaultBodyEn = "Respected Sir,\nI respectfully request your kind intervention regarding service/salary matter pending at Establishment section."
        ),
        OfficialTemplate(
            id = "app_to_headmaster",
            titleHi = "प्रधानाध्यापक को सामान्य आवेदन",
            titleEn = "General Application to Headmaster",
            category = "प्रशासनिक (Administrative)",
            defaultRecipientHi = "सेवा में,\nप्रधानाध्यापक महोदय,\n[विद्यालय का नाम], [प्रखंड], [जिला]",
            defaultRecipientEn = "To,\nThe Headmaster,\n[School Name], [Block], [District]",
            defaultSubjectHi = "[विषय यहाँ दर्ज करें]",
            defaultSubjectEn = "[Enter Subject]",
            defaultBodyHi = "महाशय,\nसविनय निवेदन है कि [आवेदन का मुख्य कारण यहाँ लिखें]।\nअतः श्रीमान से अनुरोध है कि इसे स्वीकृत करने की कृपा की जाए।",
            defaultBodyEn = "Respected Sir,\nI request you to kindly approve the following: [Details]."
        )
    )
}
