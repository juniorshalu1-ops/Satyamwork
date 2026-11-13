package com.example.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "document_drafts")
data class DocumentDraftEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val templateId: String,
    val title: String,
    val recipient: String,
    val subject: String,
    val body: String,
    val applicantName: String,
    val designation: String,
    val schoolName: String,
    val district: String,
    val dateStr: String,
    val language: String = "hi", // "hi" or "en"
    val isAiGenerated: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "notices",
    indices = [Index(value = ["orderNumber"]), Index(value = ["category"])]
)
data class NoticeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val summary: String,
    val category: String, // e.g. "वेतन/DA", "अवकाश", "स्थानांतरण", "BPSC/TRE", "प्रशिक्षण", "सरकारी आदेश"
    val issueDate: String,
    val department: String = "शिक्षा विभाग, बिहार सरकार",
    val orderNumber: String = "",
    val officialUrl: String = "https://state.bihar.gov.in/educationbihar",
    val pdfUrl: String = "",
    val imageUrl: String = "",
    val isFeatured: Boolean = false,
    val isBookmarked: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "holidays")
data class HolidayEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dateStr: String, // "dd/MM/yyyy"
    val dayOfWeek: String,
    val month: Int,
    val year: Int = 2026,
    val type: String, // "सरकारी अवकाश", "विद्यालय अवकाश", "त्योहार", "गर्मी की छुट्टी"
    val durationDays: Int = 1,
    val description: String = ""
)

@Entity(tableName = "personal_notes")
data class PersonalNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val tag: String = "सामान्य",
    val isPinned: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "teacher_tasks")
data class TeacherTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val dueDate: String = "",
    val isCompleted: Boolean = false,
    val priority: String = "मध्यम", // "उच्च", "मध्यम", "सामान्य"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "admin_audit_logs")
data class AdminAuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val adminUser: String,
    val action: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "education_activities")
data class EducationActivityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val category: String, // "अभियान", "पर्यावरण", "सुरक्षा", "खेलकूद", "शैक्षणिक"
    val linkUrl: String,
    val orderNumber: String = "",
    val isActive: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "quick_portals")
data class QuickPortalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val subtitle: String = "",
    val url: String,
    val badge: String = "पोर्टल",
    val colorHex: String = "#0D47A1",
    val isActive: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "promo_banners")
data class PromoBannerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bannerId: String = "", // "banner_loan", "banner_card", "banner_promo"
    val title: String,
    val description: String,
    val category: String = "सामान्य", // "पर्सनल लोन", "क्रेडिट कार्ड", "प्रमोशनल"
    val imageUrl: String = "",
    val destinationUrl: String = "",
    val buttonText: String = "आवेदन करें",
    val orderIndex: Int = 0,
    val isActive: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "school_timings")
data class SchoolTimingEntity(
    @PrimaryKey
    val id: Long = 1,
    val openingTime: String = "09:00 AM",
    val closingTime: String = "04:30 PM",
    val isManuallyClosed: Boolean = false,
    val closureReason: String = "",
    val morningAssemblyTime: String = "09:15 AM",
    val lunchTime: String = "01:00 PM - 01:45 PM",
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "video_tutorials")
data class VideoTutorialEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String = "सामान्य", // "ई-शिक्षाकोष", "वेतन नियम", "प्रशिक्षण", "दीक्षा पोर्टल"
    val youtubeUrl: String,
    val youtubeId: String = "",
    val thumbnailUrl: String = "",
    val duration: String = "10:00",
    val description: String = "",
    val isPinned: Boolean = false,
    val isFeaturedOnHome: Boolean = false,
    val displayOrder: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "google_drive_config")
data class GoogleDriveConfigEntity(
    @PrimaryKey
    val id: Long = 1,
    val isEnabled: Boolean = true,
    val folderUrl: String = DEFAULT_MAIN_DRIVE_FOLDER_URL,
    val adminEmail: String = "juniorshalu100@gmail.com",
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "download_resources")
data class DownloadResourceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cloudId: String = "",
    val title: String,
    val category: String = "प्रपत्र", // "छुट्टी प्रपत्र", "वेतन सत्यापन", "विभागीय नियमावली", "परीक्षा"
    val fileType: String = "PDF", // "PDF" or "JPG"
    val fileSize: String = "1.2 MB",
    val pdfUrl: String = "",
    val publicationDate: String = "",
    val orderNumber: String = "",
    val description: String = "",
    val localFilePath: String = "",
    val isVisible: Boolean = true,
    val downloadCount: Int = 0,
    val displayOrder: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "whatsapp_links")
data class WhatsAppLinkConfigEntity(
    @PrimaryKey
    val id: Long = 1,
    val channelUrl: String = "https://whatsapp.com/channel/0029Va4yZ1m2k345",
    val channelTitle: String = "शिक्षा विभाग बिहार - शिक्षक साथी आधिकारिक चैनल",
    val districtGroupsUrl: String = "https://state.bihar.gov.in/educationbihar",
    val districtGroupsTitle: String = "बिहार के 38 जिलों एवं प्रखंडों के शिक्षक व्हाट्सएप ग्रुप",
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "news_articles",
    indices = [Index(value = ["category"])]
)
data class NewsArticleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val summary: String,
    val content: String = "",
    val category: String = "शिक्षा समाचार", // "विभागीय", "वेतन", "नियुक्ति", "स्थानांतरण", "अवकाश"
    val publishedAt: String,
    val source: String = "शिक्षा विभाग, बिहार",
    val sourceUrl: String = "https://state.bihar.gov.in/educationbihar",
    val imageUrl: String = "",
    val isPinned: Boolean = false,
    val pinOrder: Int = 0,
    val isBreaking: Boolean = false,
    val isVisible: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "complaint_options")
data class ComplaintOptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val phone: String = "",
    val email: String = "",
    val linkUrl: String = "",
    val isVisible: Boolean = true,
    val displayOrder: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "home_top_links")
data class HomeTopLinkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val isVisible: Boolean = true,
    val displayOrder: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "financial_services")
data class FinancialServiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String, // "LOAN", "CREDIT_CARD", "INVESTMENT"
    val title: String,
    val description: String,
    val applyUrl: String,
    val isGoogleForm: Boolean = false,
    val buttonText: String = "आवेदन करें",
    val isVisible: Boolean = true,
    val displayOrder: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "youtube_feeds")
data class YouTubeFeedEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val channelTitle: String,
    val feedUrl: String,
    val channelId: String = "",
    val isVisible: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "news_rss_feeds")
data class NewsRssFeedEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val feedUrl: String,
    val isVisible: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "feature_boxes")
data class FeatureBoxEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subtitle: String = "",
    val iconType: String = "",
    val destinationRoute: String = "",
    val displayOrder: Int = 0,
    val isVisible: Boolean = true,
    val isExternal: Boolean = false,
    val externalUrl: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "top_three_links")
data class TopThreeLinksEntity(
    @PrimaryKey val id: Long = 1,
    val link1Title: String = "eShikshakosh",
    val link1Url: String = "https://eshikshakosh.bihar.gov.in",
    val link1Visible: Boolean = true,
    val link2Title: String = "HRMS Bihar",
    val link2Url: String = "https://hrms.bihar.gov.in",
    val link2Visible: Boolean = true,
    val link3Title: String = "UDISE+",
    val link3Url: String = "https://udiseplus.gov.in",
    val link3Visible: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "financial_banner")
data class FinancialBannerEntity(
    @PrimaryKey val id: Long = 1,
    val heading: String = "शिक्षकों के लिए विशेष वित्तीय सुविधाएं",
    val supportingText: String = "शिक्षकों के लिए कम ब्याज दरों पर विभिन्न बैंकों एवं वित्तीय संस्थानों के विकल्प उपलब्ध।",
    val disclaimer: String = "*शर्तें लागू। अनुमोदन एवं ब्याज दरें बैंकों/वित्तीय संस्थानों के नियमानुसार।",
    val buttonText: String = "विवरण देखें / आवेदन",
    val destinationType: String = "IN_APP", // "EXTERNAL_URL", "IN_APP", "GOOGLE_FORM", "APP_LINK"
    val destinationUrl: String = "",
    val imageUrl: String = "",
    val isVisible: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "whatsapp_banner")
data class WhatsAppBannerEntity(
    @PrimaryKey val id: Long = 1,
    val bannerText: String = "बिहार के शिक्षकों से जुड़ी महत्वपूर्ण सूचनाएं, अपडेट एवं जानकारियों के लिए हमारे WhatsApp Channel से जुड़ें।",
    val buttonText: String = "JOIN WHATSAPP CHANNEL",
    val channelUrl: String = "https://whatsapp.com/channel/0029Va4yZ1m2k345",
    val imageUrl: String = "",
    val isVisible: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
)

val DEFAULT_FEATURE_BOXES = listOf(
    FeatureBoxEntity("leave_holidays", "अवकाश", "अवकाश कैलेंडर व नियम", "CalendarMonth", "holiday_calendar", 1),
    FeatureBoxEntity("important_links", "सक्रिय डिजिटल सेवा", "ई-शिक्षाकोष, मेधासॉफ्ट व पोर्टल", "Link", "important_links", 2),
    FeatureBoxEntity("letters_orders", "पत्र एवं आदेश", "शासनादेश व विभागीय पत्र", "Description", "latest_orders", 3),
    FeatureBoxEntity("news_updates", "न्यूज़ एवं अपडेट्स", "नवीनतम विभागीय समाचार", "Newspaper", "news_tab", 4),
    FeatureBoxEntity("current_activities", "वर्तमान गतिविधियां", "मशाल, खेलकूद व प्रोजेक्ट्स", "Campaign", "activities", 5),
    FeatureBoxEntity("videos", "वीडियो", "शैक्षणिक ट्यूटोरियल व गाइड", "VideoLibrary", "video_library", 6),
    FeatureBoxEntity("salary", "वेतन", "वेतन व एरियर कैलकुलेटर", "AccountBalance", "salary", 7),
    FeatureBoxEntity("downloads", "डाउनलोड", "प्रपत्र, प्रारूप व PDF", "Download", "downloads", 8),
    FeatureBoxEntity("application_formats", "आवेदन प्रारूप", "अवकाश, विरमन व अन्य पत्र", "Edit", "documents", 9),
    FeatureBoxEntity("complaints", "शिकायत", "ऑनलाइन परिवाद व सहायता", "SupportAgent", "complaints", 10),
    FeatureBoxEntity("loan", "लोन", "विशेष शिक्षक पर्सनल लोन", "MonetizationOn", "loan", 11),
    FeatureBoxEntity("credit_card", "क्रेडिट कार्ड", "लाइफटाइम फ्री व रिवार्ड कार्ड", "CreditCard", "credit_card", 12),
    FeatureBoxEntity("investment", "निवेश", "NPS, GPF व बचत योजनाएं", "TrendingUp", "investment", 13),
    FeatureBoxEntity("whatsapp_channel", "WhatsApp Channel", "ताज़ा अपडेट्स व ग्रुप्स", "Chat", "whatsapp_channel", 14)
)

val DEFAULT_TOP_THREE_LINKS = TopThreeLinksEntity(
    id = 1,
    link1Title = "eShikshakosh",
    link1Url = "https://eshikshakosh.bihar.gov.in",
    link1Visible = true,
    link2Title = "HRMS Bihar",
    link2Url = "https://hrms.bihar.gov.in",
    link2Visible = true,
    link3Title = "UDISE+",
    link3Url = "https://udiseplus.gov.in",
    link3Visible = true
)

val DEFAULT_FINANCIAL_BANNER = FinancialBannerEntity(
    id = 1,
    heading = "शिक्षकों के लिए विशेष वित्तीय सुविधाएं",
    supportingText = "शिक्षकों के लिए कम ब्याज दरों पर विभिन्न बैंकों एवं वित्तीय संस्थानों के विकल्प उपलब्ध।",
    disclaimer = "*शर्तें लागू। अनुमोदन एवं ब्याज दरें बैंकों/वित्तीय संस्थानों के नियमानुसार।",
    buttonText = "विवरण देखें / आवेदन",
    destinationType = "IN_APP",
    destinationUrl = "",
    isVisible = true
)

val DEFAULT_WHATSAPP_BANNER = WhatsAppBannerEntity(
    id = 1,
    bannerText = "बिहार के शिक्षकों से जुड़ी महत्वपूर्ण सूचनाएं, अपडेट एवं जानकारियों के लिए हमारे WhatsApp Channel से जुड़ें।",
    buttonText = "JOIN WHATSAPP CHANNEL",
    channelUrl = "https://whatsapp.com/channel/0029Va4yZ1m2k345",
    isVisible = true
)

val DEFAULT_HOME_TOP_LINKS = listOf(
    HomeTopLinkEntity(id = 1, title = "eShikshakosh", url = "https://eshikshakosh.bihar.gov.in", isVisible = true, displayOrder = 1),
    HomeTopLinkEntity(id = 2, title = "HRMS Bihar", url = "https://hrms.bihar.gov.in", isVisible = true, displayOrder = 2),
    HomeTopLinkEntity(id = 3, title = "UDISE+", url = "https://udiseplus.gov.in", isVisible = true, displayOrder = 3),
    HomeTopLinkEntity(id = 4, title = "Medhasoft", url = "https://medhasoft.bih.nic.in", isVisible = true, displayOrder = 4),
    HomeTopLinkEntity(id = 5, title = "CFMS Bihar", url = "https://e-nidhi.bihar.gov.in", isVisible = true, displayOrder = 5),
    HomeTopLinkEntity(id = 6, title = "DIKSHA Portal", url = "https://diksha.gov.in", isVisible = true, displayOrder = 6)
)

val DEFAULT_COMPLAINT_OPTIONS = listOf(
    ComplaintOptionEntity(
        id = 1,
        title = "बिहार लोक शिकायत निवारण अधिकार (BPGORS)",
        description = "वेतन भुगतान, सेवा संपुष्टि, मातृत्व अवकाश अथवा विभागीय समस्याओं का कानूनी समयबद्ध समाधान।",
        phone = "18003456284",
        email = "lokshikayat-bih@nic.in",
        linkUrl = "https://lokshikayat.bihar.gov.in",
        isVisible = true,
        displayOrder = 1
    ),
    ComplaintOptionEntity(
        id = 2,
        title = "शिक्षा विभाग केंद्रीय हेल्पलाइन",
        description = "विभागीय टोल-फ्री नंबर पर सीधे अपनी शिकायत दर्ज कराएं एवं संदर्भ संख्या प्राप्त करें।",
        phone = "18003456789",
        email = "grievance.edu-bih@gov.in",
        linkUrl = "https://state.bihar.gov.in/educationbihar",
        isVisible = true,
        displayOrder = 2
    ),
    ComplaintOptionEntity(
        id = 3,
        title = "ई-शिक्षाकोष तकनीकी सहायता प्रकोष्ठ",
        description = "ऑनलाइन हाजिरी, नेटवर्क एरर, लोकेशन समस्या एवं मार्क इन/आउट तकनीकी निवारण।",
        phone = "06122233445",
        email = "eshikshakosh-support@bihar.gov.in",
        linkUrl = "https://eshikshakosh.bihar.gov.in",
        isVisible = true,
        displayOrder = 3
    ),
    ComplaintOptionEntity(
        id = 4,
        title = "प्राथमिक शिक्षा निदेशालय परिवाद कोषांग",
        description = "प्राथमिक एवं मध्य विद्यालय शिक्षकों के व्यक्तिगत परिवाद, एरियर व सेवा पुस्तिका मामले।",
        phone = "06122215678",
        email = "dir-pri.edu@bihar.gov.in",
        linkUrl = "https://state.bihar.gov.in/educationbihar",
        isVisible = true,
        displayOrder = 4
    ),
    ComplaintOptionEntity(
        id = 5,
        title = "माध्यमिक शिक्षा निदेशालय सहायता",
        description = "माध्यमिक एवं उच्च माध्यमिक शिक्षक परिवाद, पदस्थापन एवं वेतन निर्धारण प्रकोष्ठ।",
        phone = "06122215679",
        email = "dir-sec.edu@bihar.gov.in",
        linkUrl = "https://state.bihar.gov.in/educationbihar",
        isVisible = true,
        displayOrder = 5
    ),
    ComplaintOptionEntity(
        id = 6,
        title = "महिला शिक्षक विशेष सहायता प्रकोष्ठ",
        description = "महिला शिक्षकों की सुरक्षा, मातृत्व अवकाश एवं आपातकालीन सहायता प्रकोष्ठ।",
        phone = "112",
        email = "womenhelp.edu@bihar.gov.in",
        linkUrl = "https://state.bihar.gov.in/educationbihar",
        isVisible = true,
        displayOrder = 6
    )
)

val DEFAULT_FINANCIAL_SERVICES = listOf(
    FinancialServiceEntity(
        id = 1,
        category = "LOAN",
        title = "स्टेट बैंक ऑफ इंडिया (SBI) शिक्षक पर्सनल लोन",
        description = "न्यूनतम ब्याज दर, शून्य हिडन चार्ज, तत्काल स्वीकृति एवं आसान ईएमआई विकल्प।",
        applyUrl = "https://sbi.co.in/web/personal-banking/loans/personal-loans",
        isGoogleForm = false,
        buttonText = "SBI लोन विवरण",
        isVisible = true,
        displayOrder = 1
    ),
    FinancialServiceEntity(
        id = 2,
        category = "LOAN",
        title = "पंजाब नेशनल बैंक (PNB) शिक्षक वेतन खाता लोन",
        description = "शिक्षकों हेतु विशेष रियायती दर एवं आसान मासिक किस्तों पर ऋण सुविधा।",
        applyUrl = "https://pnbindia.in",
        isGoogleForm = false,
        buttonText = "PNB लोन आवेदन",
        isVisible = true,
        displayOrder = 2
    ),
    FinancialServiceEntity(
        id = 3,
        category = "LOAN",
        title = "विशेष शिक्षक लोन सहायता गूगल फॉर्म (Google Form)",
        description = "डोरस्टेप बैंकिंग एवं कम ब्याज दर सहायता हेतु अपना विवरण दर्ज करें।",
        applyUrl = "https://docs.google.com/forms",
        isGoogleForm = true,
        buttonText = "गूगल फॉर्म भरें",
        isVisible = true,
        displayOrder = 3
    ),
    FinancialServiceEntity(
        id = 4,
        category = "CREDIT_CARD",
        title = "SBI कार्ड सिम्पलीक्लिक (SimplyCLICK) लाइफटाइम फ्री",
        description = "शिक्षकों के लिए शून्य वार्षिक शुल्क एवं ऑनलाइन खरीदारी पर 10X रिवॉर्ड पॉइंट्स।",
        applyUrl = "https://www.sbicard.com",
        isGoogleForm = false,
        buttonText = "SBI कार्ड आवेदन",
        isVisible = true,
        displayOrder = 1
    ),
    FinancialServiceEntity(
        id = 5,
        category = "CREDIT_CARD",
        title = "HDFC बैंक शिक्षक प्रिविलेज क्रेडिट कार्ड",
        description = "कैशबैक, एयरपोर्ट लाउंज एक्सेस एवं पेट्रोल पंप पर ईंधन सरचार्ज छूट।",
        applyUrl = "https://www.hdfcbank.com",
        isGoogleForm = false,
        buttonText = "HDFC कार्ड आवेदन",
        isVisible = true,
        displayOrder = 2
    ),
    FinancialServiceEntity(
        id = 6,
        category = "CREDIT_CARD",
        title = "क्रेडिट कार्ड परामर्श गूगल फॉर्म (Google Form)",
        description = "सर्वश्रेष्ठ लाइफटाइम फ्री कार्ड चयन हेतु फॉर्म भरें, बैंक प्रतिनिधि संपर्क करेंगे।",
        applyUrl = "https://docs.google.com/forms",
        isGoogleForm = true,
        buttonText = "गूगल फॉर्म भरें",
        isVisible = true,
        displayOrder = 3
    ),
    FinancialServiceEntity(
        id = 7,
        category = "INVESTMENT",
        title = "NPS (राष्ट्रीय पेंशन प्रणाली) टियर-1 एवं टियर-2",
        description = "अतिरिक्त ₹50,000 कर छूट (धारा 80CCD 1B) एवं सुरक्षित दीर्घकालिक निवेश।",
        applyUrl = "https://enps.nsdl.com",
        isGoogleForm = false,
        buttonText = "NPS पोर्टल खोलें",
        isVisible = true,
        displayOrder = 1
    ),
    FinancialServiceEntity(
        id = 8,
        category = "INVESTMENT",
        title = "डाकघर पीपीएफ (PPF) एवं सुकन्या समृद्धि योजना",
        description = "भारत सरकार द्वारा गारंटीकृत 7.1% से 8.2% ब्याज एवं ट्रिपल टैक्स छूट।",
        applyUrl = "https://www.indiapost.gov.in",
        isGoogleForm = false,
        buttonText = "डाकघर योजनाएं",
        isVisible = true,
        displayOrder = 2
    ),
    FinancialServiceEntity(
        id = 9,
        category = "INVESTMENT",
        title = "म्यूचुअल फंड व बीमा योजना परामर्श गूगल फॉर्म",
        description = "विशेष शिक्षक सुरक्षित निवेश पोर्टफोलियो मार्गदर्शन हेतु गूगल फॉर्म भरें।",
        applyUrl = "https://docs.google.com/forms",
        isGoogleForm = true,
        buttonText = "गूगल फॉर्म भरें",
        isVisible = true,
        displayOrder = 3
    )
)

const val DEFAULT_MAIN_DRIVE_FOLDER_URL = "https://drive.google.com/drive/folders/10g2H4sSk6tjCu2KgEBT9_MiAvd7CM-2M"

val DEFAULT_YOUTUBE_FEEDS = listOf(
    YouTubeFeedEntity(
        id = 1,
        channelTitle = "शिक्षक साथी बिहार (@shikshaksathibihar)",
        feedUrl = "https://www.youtube.com/feeds/videos.xml?channel_id=UCm1BZKUQgC2W5ixkKqfQ9Aw",
        channelId = "UCm1BZKUQgC2W5ixkKqfQ9Aw",
        isVisible = true
    )
)

val DEFAULT_NEWS_RSS_FEEDS = listOf(
    NewsRssFeedEntity(
        id = 1,
        title = "द मगध टाइम्स (The Magadh Times)",
        feedUrl = "https://themagadhtimes.blogspot.com/feeds/posts/default?alt=rss",
        isVisible = true
    )
)

val DEFAULT_INITIAL_DOWNLOADS = emptyList<DownloadResourceEntity>()

val DEFAULT_GOOGLE_DRIVE_CONFIG = GoogleDriveConfigEntity(
    id = 1,
    isEnabled = true,
    folderUrl = DEFAULT_MAIN_DRIVE_FOLDER_URL,
    adminEmail = "juniorshalu100@gmail.com"
)



