package com.example.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.database.daos.AbsenteeDao
import com.example.database.daos.ContentDao
import com.example.database.daos.SalaryDao
import com.example.database.daos.SchoolProfileDao
import com.example.database.daos.StaffDao
import com.example.database.daos.TeacherProfileDao
import com.example.database.entities.AdminAuditLogEntity
import com.example.database.entities.DocumentDraftEntity
import com.example.database.entities.EducationActivityEntity
import com.example.database.entities.HolidayEntity
import com.example.database.entities.MonthlyAbsenteeDetailEntity
import com.example.database.entities.MonthlyAbsenteeReportEntity
import com.example.database.entities.NoticeEntity
import com.example.database.entities.PersonalNoteEntity
import com.example.database.entities.SalaryRecordEntity
import com.example.database.entities.SchoolProfileEntity
import com.example.database.entities.StaffEntity
import com.example.database.entities.TeacherProfileEntity
import com.example.database.entities.TeacherTaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TeacherProfileEntity::class,
        SchoolProfileEntity::class,
        StaffEntity::class,
        MonthlyAbsenteeReportEntity::class,
        MonthlyAbsenteeDetailEntity::class,
        SalaryRecordEntity::class,
        DocumentDraftEntity::class,
        NoticeEntity::class,
        HolidayEntity::class,
        PersonalNoteEntity::class,
        TeacherTaskEntity::class,
        AdminAuditLogEntity::class,
        EducationActivityEntity::class,
        com.example.database.entities.QuickPortalEntity::class,
        com.example.database.entities.PromoBannerEntity::class,
        com.example.database.entities.SchoolTimingEntity::class,
        com.example.database.entities.VideoTutorialEntity::class,
        com.example.database.entities.DownloadResourceEntity::class,
        com.example.database.entities.WhatsAppLinkConfigEntity::class,
        com.example.database.entities.NewsArticleEntity::class,
        com.example.database.entities.FeatureBoxEntity::class,
        com.example.database.entities.TopThreeLinksEntity::class,
        com.example.database.entities.FinancialBannerEntity::class,
        com.example.database.entities.WhatsAppBannerEntity::class,
        com.example.database.entities.ComplaintOptionEntity::class,
        com.example.database.entities.HomeTopLinkEntity::class,
        com.example.database.entities.FinancialServiceEntity::class,
        com.example.database.entities.YouTubeFeedEntity::class,
        com.example.database.entities.NewsRssFeedEntity::class,
        com.example.database.entities.GoogleDriveConfigEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun teacherProfileDao(): TeacherProfileDao
    abstract fun schoolProfileDao(): SchoolProfileDao
    abstract fun staffDao(): StaffDao
    abstract fun absenteeDao(): AbsenteeDao
    abstract fun salaryDao(): SalaryDao
    abstract fun contentDao(): ContentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_8_9: Migration = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!hasColumn(db, "notices", "imageUrl")) {
                    db.execSQL("ALTER TABLE notices ADD COLUMN imageUrl TEXT NOT NULL DEFAULT ''")
                }
                if (!hasColumn(db, "download_resources", "cloudId")) {
                    db.execSQL("ALTER TABLE download_resources ADD COLUMN cloudId TEXT NOT NULL DEFAULT ''")
                }
            }

            private fun hasColumn(db: SupportSQLiteDatabase, table: String, column: String): Boolean {
                val cursor = db.query("PRAGMA table_info($table)")
                cursor.use {
                    val nameIdx = it.getColumnIndex("name")
                    while (it.moveToNext()) {
                        if (nameIdx >= 0 && it.getString(nameIdx) == column) {
                            return true
                        }
                    }
                }
                return false
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shikshak_sathi_bihar.db"
                )
                    .addMigrations(MIGRATION_8_9)
                    .addCallback(AppDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }

        private suspend fun populateInitialData(database: AppDatabase) {
            // Seed Teacher Profile default
            database.teacherProfileDao().saveTeacherProfile(
                TeacherProfileEntity()
            )

            // Seed School Profile default
            database.schoolProfileDao().saveSchoolProfile(
                SchoolProfileEntity()
            )

            // Seed Initial Staff Members
            val initialStaff = listOf(
                StaffEntity(
                    id = 1,
                    name = "सत्यम प्रकाश",
                    designation = "प्रधानाध्यापक",
                    employeeId = "BR-EDU-84721",
                    joiningDate = "15/07/2014",
                    basicPay = 44900.0,
                    daPercent = 53.0,
                    hraCategory = "8%",
                    contactNumber = "9876543210",
                    isActive = true
                ),
                StaffEntity(
                    id = 2,
                    name = "आनंद कुमार झा",
                    designation = "सहायक शिक्षक (गणित/विज्ञान)",
                    employeeId = "BR-EDU-84722",
                    joiningDate = "20/09/2019",
                    basicPay = 35400.0,
                    daPercent = 53.0,
                    hraCategory = "8%",
                    contactNumber = "9876543211",
                    isActive = true
                ),
                StaffEntity(
                    id = 3,
                    name = "सुनीता कुमारी",
                    designation = "सहायक शिक्षिका (सामाजिक विज्ञान)",
                    employeeId = "BR-EDU-84723",
                    joiningDate = "10/01/2021",
                    basicPay = 31100.0,
                    daPercent = 53.0,
                    hraCategory = "8%",
                    contactNumber = "9876543212",
                    isActive = true
                ),
                StaffEntity(
                    id = 4,
                    name = "राकेश कुमार वर्मा",
                    designation = "सहायक शिक्षक (भाषा - हिंदी/संस्कृत)",
                    employeeId = "BR-EDU-84724",
                    joiningDate = "05/12/2022",
                    basicPay = 29600.0,
                    daPercent = 53.0,
                    hraCategory = "8%",
                    contactNumber = "9876543213",
                    isActive = true
                ),
                StaffEntity(
                    id = 5,
                    name = "मो० इरफान अंसारी",
                    designation = "शारीरिक शिक्षक",
                    employeeId = "BR-EDU-84725",
                    joiningDate = "01/08/2023",
                    basicPay = 28000.0,
                    daPercent = 53.0,
                    hraCategory = "8%",
                    contactNumber = "9876543214",
                    isActive = true
                ),
                StaffEntity(
                    id = 6,
                    name = "रेखा देवी",
                    designation = "रसोईया सह सहायिका",
                    employeeId = "BR-EDU-S01",
                    joiningDate = "01/04/2016",
                    basicPay = 18000.0,
                    daPercent = 53.0,
                    hraCategory = "8%",
                    contactNumber = "9876543215",
                    isActive = true
                )
            )
            // Demo staff insertion removed as requested

            // Seed Initial Official Bihar Teacher Notices
            val initialNotices = listOf(
                NoticeEntity(
                    title = "शिक्षा विभाग: शिक्षकों के लिए महंगाई भत्ता (DA) 50% से बढ़ाकर 53% करने की अधिसूचना",
                    summary = "बिहार सरकार वित्त विभाग एवं शिक्षा विभाग द्वारा राज्य कर्मियों एवं शिक्षकों के महंगाई भत्ते में 3% की वृद्धि कर 53% कर दिया गया है।",
                    category = "वेतन/DA",
                    issueDate = "15/10/2026",
                    department = "वित्त विभाग, बिहार सरकार",
                    orderNumber = "सं०-14/वि०-05/2026-8942",
                    isFeatured = true
                ),
                NoticeEntity(
                    title = "माहवार अनुपस्थिति विवरणी (Absentee Report) प्रेषण के संबंध में आवश्यक दिशा-निर्देश",
                    summary = "सभी प्रधानाध्यापकों को निर्देशित किया जाता है कि माह की 25 तारीख तक ई-शिक्षाकोष एवं संबंधित प्रखंड शिक्षा पदाधिकारी कार्यालय में विवरणी समर्पित करें।",
                    category = "सरकारी आदेश",
                    issueDate = "05/10/2026",
                    department = "प्राथमिक शिक्षा निदेशालय, बिहार",
                    orderNumber = "ज्ञापांक-7/विविध-12/2026-3120",
                    isFeatured = true
                ),
                NoticeEntity(
                    title = "सत्र 2026-27 के लिए अवकाश तालिका (Holiday Calendar) का प्रकाशन",
                    summary = "बिहार राज्य के सभी राजकीय, राजकीयकृत, प्राथमिक, मध्य एवं माध्यमिक विद्यालयों के लिए अवकाश कैलेंडर का आधिकारिक विवरण।",
                    category = "अवकाश",
                    issueDate = "01/01/2026",
                    department = "शिक्षा विभाग, बिहार सरकार",
                    orderNumber = "अधिसूचना सं०-11/आ०-01/2026-105",
                    isFeatured = true
                ),
                NoticeEntity(
                    title = "सक्षमता उत्तीर्ण शिक्षकों के सेवा समंजन एवं वेतन निर्धारण निर्देश",
                    summary = "सक्षमता परीक्षा उत्तीर्ण विशिष्ट शिक्षकों के पदस्थापन एवं वेतन संरक्षण (Pay Protection) संबंधी महत्वपूर्ण स्पष्टीकरण।",
                    category = "BPSC/TRE",
                    issueDate = "22/09/2026",
                    department = "शिक्षा विभाग, बिहार सरकार",
                    orderNumber = "ज्ञापांक-9/नि०-03/2026-4512",
                    isFeatured = false
                )
            )
            database.contentDao().insertNotices(initialNotices)

            // Seed Initial Holidays
            val initialHolidays = listOf(
                HolidayEntity(name = "गणतंत्र दिवस", dateStr = "26/01/2026", dayOfWeek = "सोमवार", month = 1, type = "सरकारी अवकाश"),
                HolidayEntity(name = "सरस्वती पूजा (वसंत पंचमी)", dateStr = "23/01/2026", dayOfWeek = "शुक्रवार", month = 1, type = "त्योहार"),
                HolidayEntity(name = "महाशिवरात्रि", dateStr = "16/02/2026", dayOfWeek = "सोमवार", month = 2, type = "त्योहार"),
                HolidayEntity(name = "होली (धुलेंडी)", dateStr = "04/03/2026", dayOfWeek = "बुधवार", month = 3, type = "त्योहार", durationDays = 2),
                HolidayEntity(name = "बिहार दिवस", dateStr = "22/03/2026", dayOfWeek = "रविवार", month = 3, type = "सरकारी अवकाश"),
                HolidayEntity(name = "ईद-उल-फितर", dateStr = "21/03/2026", dayOfWeek = "शनिवार", month = 3, type = "त्योहार"),
                HolidayEntity(name = "अंबेडकर जयंती", dateStr = "14/04/2026", dayOfWeek = "मंगलवार", month = 4, type = "सरकारी अवकाश"),
                HolidayEntity(name = "ग्रीष्मावकाश (Summer Vacation)", dateStr = "01/05/2026", dayOfWeek = "शुक्रवार", month = 5, type = "विद्यालय अवकाश", durationDays = 25),
                HolidayEntity(name = "स्वतंत्रता दिवस", dateStr = "15/08/2026", dayOfWeek = "शनिवार", month = 8, type = "सरकारी अवकाश"),
                HolidayEntity(name = "श्रीकृष्ण जन्माष्टमी", dateStr = "04/09/2026", dayOfWeek = "शुक्रवार", month = 9, type = "त्योहार"),
                HolidayEntity(name = "गांधी जयंती", dateStr = "02/10/2026", dayOfWeek = "शुक्रवार", month = 10, type = "सरकारी अवकाश"),
                HolidayEntity(name = "दुर्गा पूजा (महानवमी/दशहरा)", dateStr = "19/10/2026", dayOfWeek = "सोमवार", month = 10, type = "त्योहार", durationDays = 4),
                HolidayEntity(name = "दीपावली", dateStr = "08/11/2026", dayOfWeek = "रविवार", month = 11, type = "त्योहार"),
                HolidayEntity(name = "छठ पूजा (सायंकालीन एवं प्रात:कालीन अर्घ्य)", dateStr = "15/11/2026", dayOfWeek = "रविवार", month = 11, type = "त्योहार", durationDays = 3),
                HolidayEntity(name = "क्रिसमस डे", dateStr = "25/12/2026", dayOfWeek = "शुक्रवार", month = 12, type = "त्योहार")
            )
            database.contentDao().insertHolidays(initialHolidays)

            // Seed Initial Bihar Educational Activities & Campaigns (मशाल, इको क्लब आदि)
            val initialActivities = listOf(
                EducationActivityEntity(
                    title = "मशाल (Mashal Campaign) - खेल एवं अनामांकित बच्चों की खोज",
                    description = "विद्यालय से बाहर (Out of School) एवं ड्रॉपआउट बच्चों की पहचान कर शत-प्रतिशत मुख्यधारा में जोड़ने तथा खेलकूद व सांस्कृतिक गतिविधियों को बढ़ावा देने का राज्यव्यापी अभियान।",
                    category = "अभियान",
                    linkUrl = "https://state.bihar.gov.in/educationbihar",
                    orderNumber = "ज्ञापांक-18/मशाल-अभियान/2026-114"
                ),
                EducationActivityEntity(
                    title = "इको क्लब (Eco Club) एवं पर्यावरण संरक्षण (Mission LiFE)",
                    description = "विद्यालय परिसर में पोषण वाटिका, औषधीय वाटिका का निर्माण, सिंगल यूज प्लास्टिक निषेध, जल संरक्षण एवं पर्यावरण चेतना हेतु विद्यार्थियों का क्लब गठन।",
                    category = "पर्यावरण",
                    linkUrl = "https://state.bihar.gov.in/educationbihar",
                    orderNumber = "सं०-07/पर्यावरण-इको/2026-382"
                ),
                EducationActivityEntity(
                    title = "सुरक्षित शनिवार (Surakshit Shanivar) - आपदा प्रबंधन",
                    description = "प्रत्येक शनिवार को चेतना सत्र के उपरांत बच्चों को बाढ़, भूकंप, अगलगी, आकाशीय बिजली (वज्रपात), सड़क सुरक्षा एवं बाल सुरक्षा से बचाव हेतु व्यावहारिक मॉक ड्रिल प्रशिक्षण।",
                    category = "सुरक्षा",
                    linkUrl = "https://state.bihar.gov.in/educationbihar",
                    orderNumber = "ज्ञापांक-09/आपदा-प्रबंधन/2026-890"
                ),
                EducationActivityEntity(
                    title = "खेलो बिहार (Khelo Bihar) - खेल महाकुंभ",
                    description = "प्रखंड, जिला एवं राज्य स्तर पर विद्यालयी छात्र-छात्राओं के लिए एथलेटिक्स, कबड्डी, खो-खो, फुटबॉल एवं ताइक्वांडो प्रतियोगिताओं का आयोजन।",
                    category = "खेलकूद",
                    linkUrl = "https://state.bihar.gov.in/educationbihar",
                    orderNumber = "सं०-12/खेल-युवा/2026-554"
                ),
                EducationActivityEntity(
                    title = "तरंग मेधा उत्सव - प्रतिभा संवर्धन प्रतियोगिता",
                    description = "विद्यालय एवं प्रखंड स्तर पर सुलेख, चित्रकला, निबंध, सामान्य ज्ञान क्विज, आशु भाषण एवं विज्ञान प्रदर्शनी का वार्षिक आयोजन।",
                    category = "शैक्षणिक",
                    linkUrl = "https://state.bihar.gov.in/educationbihar",
                    orderNumber = "ज्ञापांक-05/तरंग-उत्सव/2026-720"
                ),
                EducationActivityEntity(
                    title = "मिशन दक्ष (Mission Daksh) - उपचारात्मक शिक्षण",
                    description = "कक्षा 3 से 8 के हिंदी, अंग्रेजी एवं गणित में पिछड़ रहे कमजोर बच्चों के लिए विशेष कक्षाएं एवं मासिक उपचारात्मक मूल्यांकन।",
                    category = "शैक्षणिक",
                    linkUrl = "https://state.bihar.gov.in/educationbihar",
                    orderNumber = "ज्ञापांक-14/दक्ष-अकादमिक/2026-902"
                )
            )
            database.contentDao().insertAllActivities(initialActivities)

            // Seed Initial Quick Portals & Links
            val initialPortals = listOf(
                com.example.database.entities.QuickPortalEntity(
                    title = "ई-शिक्षाकोष पोर्टल (e-Shikshakosh)",
                    subtitle = "उपस्थिति, शिक्षक प्रोफाइल व छात्र नामांकन",
                    url = "https://eshikshakosh.bihar.gov.in",
                    badge = "हाजिरी व छात्र",
                    colorHex = "#0D47A1"
                ),
                com.example.database.entities.QuickPortalEntity(
                    title = "मेधासॉफ्ट पोर्टल (Medhasoft)",
                    subtitle = "छात्रवृत्ति, पोशाक व साइकिल डीबीटी योजना",
                    url = "https://medhasoft.bih.nic.in",
                    badge = "डीबीटी योजना",
                    colorHex = "#2E7D32"
                ),
                com.example.database.entities.QuickPortalEntity(
                    title = "सीएफएमएस बिहार (CFMS Bihar)",
                    subtitle = "वेतन विपत्र, ई-पे स्लिप व पीएफ स्टेटमेंट",
                    url = "https://e-nidhi.bihar.gov.in",
                    badge = "वेतन व बिल",
                    colorHex = "#C2185B"
                ),
                com.example.database.entities.QuickPortalEntity(
                    title = "दीक्षा पोर्टल (DIKSHA Bihar)",
                    subtitle = "शिक्षक प्रशिक्षण, निष्ठा कोर्स व डिजिटल पाठ्यपुस्तकें",
                    url = "https://diksha.gov.in",
                    badge = "प्रशिक्षण",
                    colorHex = "#E65100"
                ),
                com.example.database.entities.QuickPortalEntity(
                    title = "यू-डायस प्लस (UDISE+)",
                    subtitle = "वार्षिक विद्यालय सांख्यिकी एवं अधोसंरचना डेटा",
                    url = "https://udiseplus.gov.in",
                    badge = "विद्यालय डेटा",
                    colorHex = "#00695C"
                ),
                com.example.database.entities.QuickPortalEntity(
                    title = "मानव संपदा / HRMS बिहार",
                    subtitle = "ऑनलाइन अवकाश आवेदन, सेवा पुस्तिका व स्थानांतरण",
                    url = "https://state.bihar.gov.in/educationbihar",
                    badge = "सेवा व अवकाश",
                    colorHex = "#4A148C"
                )
            )
            database.contentDao().insertAllPortals(initialPortals)

            // Seed Initial School Timings (09:00 AM to 04:30 PM)
            database.contentDao().saveSchoolTiming(
                com.example.database.entities.SchoolTimingEntity(
                    id = 1,
                    openingTime = "09:00 AM",
                    closingTime = "04:30 PM",
                    isManuallyClosed = false,
                    closureReason = ""
                )
            )

            // Seed Initial 3 Auto-Sliding Promotional Banners (Personal Loan, Credit Card, Promo)
            val initialBanners = listOf(
                com.example.database.entities.PromoBannerEntity(
                    id = 1,
                    bannerId = "banner_loan",
                    title = "विशेष शिक्षक पर्सनल लोन (Personal Loan)",
                    description = "बिहार सरकार शिक्षकों हेतु न्यूनतम ब्याज दर, तत्काल स्वीकृति एवं शून्य प्रोसेसिंग शुल्क",
                    category = "पर्सनल लोन",
                    destinationUrl = "https://state.bihar.gov.in/educationbihar",
                    buttonText = "लोन पात्रता जांचें",
                    orderIndex = 1,
                    isActive = true
                ),
                com.example.database.entities.PromoBannerEntity(
                    id = 2,
                    bannerId = "banner_card",
                    title = "लाइफटाइम फ्री शिक्षक क्रेडिट कार्ड (Credit Card)",
                    description = "रिवॉर्ड पॉइंट्स, ईंधन सरचार्ज छूट एवं विशेष शॉपिंग ऑफर्स के साथ निशुल्क क्रेडिट कार्ड",
                    category = "क्रेडिट कार्ड",
                    destinationUrl = "https://state.bihar.gov.in/educationbihar",
                    buttonText = "कार्ड हेतु आवेदन",
                    orderIndex = 2,
                    isActive = true
                ),
                com.example.database.entities.PromoBannerEntity(
                    id = 3,
                    bannerId = "banner_promo",
                    title = "शिक्षक कल्याण व डिजिटल सक्षमता अभियान",
                    description = "ऑनलाइन पाठ्य सामग्री, शैक्षिक उपकरण एवं डिजिटल नवाचार से जुड़ें",
                    category = "प्रमोशनल",
                    destinationUrl = "https://diksha.gov.in",
                    buttonText = "अधिक जानें",
                    orderIndex = 3,
                    isActive = true
                )
            )
            database.contentDao().insertAllBanners(initialBanners)

            // Seed WhatsApp Links
            database.contentDao().saveWhatsAppData(
                com.example.database.entities.WhatsAppLinkConfigEntity(
                    id = 1,
                    channelUrl = "https://whatsapp.com/channel/0029Va4yZ1m2k345",
                    channelTitle = "शिक्षा विभाग बिहार - शिक्षक साथी आधिकारिक चैनल",
                    districtGroupsUrl = "https://state.bihar.gov.in/educationbihar",
                    districtGroupsTitle = "बिहार के 38 जिलों एवं प्रखंडों के शिक्षक व्हाट्सएप ग्रुप"
                )
            )

            // Seed Initial Video Tutorials (Real Educational Videos, No Demo Rick Astley Placeholders)
            val initialVideos = listOf(
                com.example.database.entities.VideoTutorialEntity(
                    id = 1,
                    title = "ई-शिक्षाकोष पोर्टल पर दैनिक उपस्थिति एवं सेल्फी दर्ज करने की संपूर्ण विधि",
                    category = "ई-शिक्षाकोष",
                    youtubeUrl = "https://www.youtube.com/watch?v=nigmHzbhDt8",
                    youtubeId = "nigmHzbhDt8",
                    thumbnailUrl = "https://i.ytimg.com/vi/nigmHzbhDt8/hqdefault.jpg",
                    duration = "08:45",
                    description = "शिक्षक उपस्थिति ऐप में लोकेशन, नेटवर्क समस्या समाधान व दैनिक रिपोर्टिंग ट्यूटोरियल।",
                    isPinned = true,
                    isFeaturedOnHome = true
                ),
                com.example.database.entities.VideoTutorialEntity(
                    id = 2,
                    title = "7वें वेतनमान में DA 53% एवं मकान किराया भत्ता (HRA) गणना कैसे करें",
                    category = "वेतन नियम",
                    youtubeUrl = "https://www.youtube.com/watch?v=0hYyL8sRkY8",
                    youtubeId = "0hYyL8sRkY8",
                    thumbnailUrl = "https://i.ytimg.com/vi/0hYyL8sRkY8/hqdefault.jpg",
                    duration = "12:20",
                    description = "पे-मैट्रिक्स लेवल अनुसार मूल वेतन, महंगाई भत्ता, एनपीएस कटौती व शुद्ध वेतन का विस्तृत विवरण।",
                    isPinned = false,
                    isFeaturedOnHome = true
                ),
                com.example.database.entities.VideoTutorialEntity(
                    id = 3,
                    title = "मेधासॉफ्ट डीबीटी पोर्टल पर छात्रों का बैंक खाता एवं आधार सीडिंग गाइड",
                    category = "डीबीटी योजना",
                    youtubeUrl = "https://www.youtube.com/watch?v=eG_4k51t53g",
                    youtubeId = "eG_4k51t53g",
                    thumbnailUrl = "https://i.ytimg.com/vi/eG_4k51t53g/hqdefault.jpg",
                    duration = "09:30",
                    description = "पोशाक, छात्रवृत्ति व साइकिल योजना राशि अंतरण हेतु त्रुटिरहित प्रविष्टि ट्यूटोरियल।",
                    isPinned = false,
                    isFeaturedOnHome = true
                )
            )
            // Initial videos seeding removed - only admin uploaded / cloud videos used

            // Seed Initial Downloads
            val initialDownloads = listOf<com.example.database.entities.DownloadResourceEntity>()
            // Initial downloads seeding removed - only Google Drive and admin uploaded documents used

            // Seed Initial News Articles
            val initialNews = listOf(
                com.example.database.entities.NewsArticleEntity(
                    id = 1,
                    title = "शिक्षा विभाग: विद्यालयों में ई-शिक्षाकोष डिजिटल मॉनिटरिंग और उपस्थिति सुदृढ़ीकरण के निर्देश",
                    summary = "राज्य के सभी प्राथमिक, मध्य एवं उच्च माध्यमिक विद्यालयों में दैनिक उपस्थिति एवं मध्याह्न भोजन रिपोर्टिंग अनिवार्य।",
                    content = "शिक्षा विभाग बिहार सरकार द्वारा जारी नवीनतम दिशानिर्देश के अनुसार सभी विद्यालयों में समय-सारणी का कठोरता से अनुपालन सुनिश्चित किया जाएगा।",
                    category = "विभागीय",
                    publishedAt = "04/11/2026",
                    source = "शिक्षा विभाग बिहार",
                    isPinned = true,
                    isBreaking = true
                ),
                com.example.database.entities.NewsArticleEntity(
                    id = 2,
                    title = "राज्य कर्मियों एवं शिक्षकों के महंगाई भत्ते (DA) में 3% की वृद्धि की अधिसूचना जारी",
                    summary = "अब कुल महंगाई भत्ता 50% से बढ़कर 53% हुआ, 1 जुलाई 2026 से एरियर राशि का होगा एकमुश्त भुगतान।",
                    content = "वित्त विभाग द्वारा अनुमोदित निर्णय के आलोक में शिक्षकों के मासिक वेतन विपत्र में संशोधित डीए दरें लागू कर दी गई हैं।",
                    category = "वेतन व भत्ते",
                    publishedAt = "02/11/2026",
                    source = "वित्त विभाग, बिहार सरकार",
                    isPinned = true,
                    isBreaking = false
                )
            )
            database.contentDao().insertAllNews(initialNews)

            // Seed Top 3 Official Links
            database.contentDao().saveTopThreeLinks(
                com.example.database.entities.TopThreeLinksEntity(
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
            )

            // Seed Financial Services Banner
            database.contentDao().saveFinancialBanner(
                com.example.database.entities.FinancialBannerEntity(
                    id = 1,
                    heading = "शिक्षकों के लिए विशेष वित्तीय सुविधाएं",
                    supportingText = "शिक्षकों के लिए कम ब्याज दरों पर विभिन्न बैंकों एवं वित्तीय संस्थानों के विकल्प उपलब्ध।",
                    disclaimer = "*शर्तें लागू। अनुमोदन एवं ब्याज दरें बैंकों/वित्तीय संस्थानों के नियमानुसार।",
                    buttonText = "विवरण देखें / आवेदन",
                    destinationType = "IN_APP",
                    destinationUrl = "",
                    isVisible = true
                )
            )

            // Seed WhatsApp Channel Banner
            database.contentDao().saveWhatsAppBanner(
                com.example.database.entities.WhatsAppBannerEntity(
                    id = 1,
                    bannerText = "बिहार के शिक्षकों से जुड़ी महत्वपूर्ण सूचनाएं, अपडेट एवं जानकारियों के लिए हमारे WhatsApp Channel से जुड़ें।",
                    buttonText = "JOIN WHATSAPP CHANNEL",
                    channelUrl = "https://whatsapp.com/channel/0029Va4yZ1m2k345",
                    isVisible = true
                )
            )

            // Seed 14 Main Feature Boxes (मुख्य शिक्षक सुविधाएं)
            val initialFeatureBoxes = listOf(
                com.example.database.entities.FeatureBoxEntity("leave_holidays", "अवकाश", "अवकाश कैलेंडर व नियम", "CalendarMonth", "holiday_calendar", 1),
                com.example.database.entities.FeatureBoxEntity("important_links", "सक्रिय डिजिटल सेवा", "ई-शिक्षाकोष, मेधासॉफ्ट व पोर्टल", "Link", "important_links", 2),
                com.example.database.entities.FeatureBoxEntity("letters_orders", "पत्र एवं आदेश", "शासनादेश व विभागीय पत्र", "Description", "latest_orders", 3),
                com.example.database.entities.FeatureBoxEntity("news_updates", "न्यूज़ एवं अपडेट्स", "नवीनतम विभागीय समाचार", "Newspaper", "news_tab", 4),
                com.example.database.entities.FeatureBoxEntity("current_activities", "वर्तमान गतिविधियां", "मशाल, खेलकूद व प्रोजेक्ट्स", "Campaign", "activities", 5),
                com.example.database.entities.FeatureBoxEntity("videos", "वीडियो", "शैक्षणिक ट्यूटोरियल व गाइड", "VideoLibrary", "video_library", 6),
                com.example.database.entities.FeatureBoxEntity("salary", "वेतन", "वेतन व एरियर कैलकुलेटर", "AccountBalance", "salary", 7),
                com.example.database.entities.FeatureBoxEntity("downloads", "डाउनलोड", "प्रपत्र, प्रारूप व PDF", "Download", "downloads", 8),
                com.example.database.entities.FeatureBoxEntity("application_formats", "आवेदन प्रारूप", "अवकाश, विरमन व अन्य पत्र", "EditDocument", "documents", 9),
                com.example.database.entities.FeatureBoxEntity("complaints", "शिकायत", "ऑनलाइन परिवाद व सहायता", "SupportAgent", "complaints", 10),
                com.example.database.entities.FeatureBoxEntity("loan", "लोन", "विशेष शिक्षक पर्सनल लोन", "MonetizationOn", "loan", 11),
                com.example.database.entities.FeatureBoxEntity("credit_card", "क्रेडिट कार्ड", "लाइफटाइम फ्री व रिवार्ड कार्ड", "CreditCard", "credit_card", 12),
                com.example.database.entities.FeatureBoxEntity("investment", "निवेश", "NPS, GPF व बचत योजनाएं", "TrendingUp", "investment", 13),
                com.example.database.entities.FeatureBoxEntity("whatsapp_channel", "WhatsApp Channel", "ताज़ा अपडेट्स व ग्रुप्स", "Chat", "whatsapp_channel", 14)
            )
            database.contentDao().insertAllFeatureBoxes(initialFeatureBoxes)

            // Seed Initial Downloads removed as requested - downloads section keeps only Google Drive and admin documents
            // database.contentDao().insertAllDownloads(com.example.database.entities.DEFAULT_INITIAL_DOWNLOADS)

            // Seed Home Top Links
            database.contentDao().insertAllHomeTopLinks(com.example.database.entities.DEFAULT_HOME_TOP_LINKS)

            // Seed Complaint Options
            database.contentDao().insertAllComplaintOptions(com.example.database.entities.DEFAULT_COMPLAINT_OPTIONS)

            // Seed Financial Services
            database.contentDao().insertAllFinancialServices(com.example.database.entities.DEFAULT_FINANCIAL_SERVICES)

            // Seed YouTube RSS Feeds
            database.contentDao().insertAllYouTubeFeeds(com.example.database.entities.DEFAULT_YOUTUBE_FEEDS)

            // Seed News RSS Feeds
            database.contentDao().insertAllNewsRssFeeds(com.example.database.entities.DEFAULT_NEWS_RSS_FEEDS)

            // Seed Google Drive Config
            database.contentDao().saveGoogleDriveConfig(com.example.database.entities.DEFAULT_GOOGLE_DRIVE_CONFIG)
        }
    }
}

