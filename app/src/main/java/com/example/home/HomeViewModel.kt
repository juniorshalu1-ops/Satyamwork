package com.example.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.DateUtils
import com.example.database.AppDatabase
import com.example.database.AppRepository
import com.example.database.entities.NoticeEntity
import com.example.database.entities.SchoolProfileEntity
import com.example.database.entities.TeacherProfileEntity
import com.example.firebase.FirebaseManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class HomeUiState(
    val greetingHi: String = "",
    val greetingEn: String = "",
    val currentDateHi: String = "",
    val currentDateEn: String = "",
    val featuredNotices: List<NoticeEntity> = emptyList(),
    val latestNotices: List<NoticeEntity> = emptyList(),
    val teacherProfile: TeacherProfileEntity? = null,
    val schoolProfile: SchoolProfileEntity? = null
)

data class TodaySchoolStatus(
    val isOpen: Boolean = true,
    val statusBadge: String = "विद्यालय खुला है",
    val statusTitle: String = "आज विद्यालय खुला है",
    val statusSubtitle: String = "सामान्य कार्य दिवस",
    val openingTime: String = "09:00 AM",
    val closingTime: String = "04:30 PM",
    val remainingTimeText: String = "",
    val holidayName: String = "",
    val holidayType: String = "",
    val nextHolidayInfo: String = "",
    val dateTagged: String = ""
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        fun getFormattedCurrentDateOnly(): String {
            val cal = Calendar.getInstance()
            val days = arrayOf("रविवार", "सोमवार", "मंगलवार", "बुधवार", "गुरुवार", "शुक्रवार", "शनिवार")
            val months = arrayOf("जनवरी", "फरवरी", "मार्च", "अप्रैल", "मई", "जून", "जुलाई", "अगस्त", "सितंबर", "अक्टूबर", "नवंबर", "दिसंबर")
            val dayName = days[cal.get(Calendar.DAY_OF_WEEK) - 1]
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val monthName = months[cal.get(Calendar.MONTH)]
            val year = cal.get(Calendar.YEAR)
            return "%s, %02d %s %04d".format(dayName, day, monthName, year)
        }

        fun getFormattedCurrentDateTime(): String {
            val cal = Calendar.getInstance()
            val days = arrayOf("रविवार", "सोमवार", "मंगलवार", "बुधवार", "गुरुवार", "शुक्रवार", "शनिवार")
            val months = arrayOf("जनवरी", "फरवरी", "मार्च", "अप्रैल", "मई", "जून", "जुलाई", "अगस्त", "सितंबर", "अक्टूबर", "नवंबर", "दिसंबर")
            val dayName = days[cal.get(Calendar.DAY_OF_WEEK) - 1]
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val monthName = months[cal.get(Calendar.MONTH)]
            val year = cal.get(Calendar.YEAR)
            var hour = cal.get(Calendar.HOUR)
            if (hour == 0) hour = 12
            val minute = cal.get(Calendar.MINUTE)
            val second = cal.get(Calendar.SECOND)
            val amPm = if (cal.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
            return "%s, %02d %s %04d | %02d:%02d:%02d %s".format(dayName, day, monthName, year, hour, minute, second, amPm)
        }
    }

    val repository: AppRepository
    val teacherProfile: StateFlow<TeacherProfileEntity?>
    val schoolProfile: StateFlow<SchoolProfileEntity?>
    val featuredNotices: StateFlow<List<NoticeEntity>>
    val latestNotices: StateFlow<List<NoticeEntity>>
    val portals: StateFlow<List<com.example.database.entities.QuickPortalEntity>>
    val promoBanners: StateFlow<List<com.example.database.entities.PromoBannerEntity>>
    val activeActivities: StateFlow<List<com.example.database.entities.EducationActivityEntity>>
    val whatsAppData: StateFlow<com.example.database.entities.WhatsAppLinkConfigEntity?>
    val schoolTiming: StateFlow<com.example.database.entities.SchoolTimingEntity?>
    val downloadResources: StateFlow<List<com.example.database.entities.DownloadResourceEntity>>
    val newsArticles: StateFlow<List<com.example.database.entities.NewsArticleEntity>>
    val videoTutorials: StateFlow<List<com.example.database.entities.VideoTutorialEntity>>
    val featuredHomeVideos: StateFlow<List<com.example.database.entities.VideoTutorialEntity>>
    val googleDriveConfig: StateFlow<com.example.database.entities.GoogleDriveConfigEntity?>
    val featureBoxes: StateFlow<List<com.example.database.entities.FeatureBoxEntity>>
    val topThreeLinks: StateFlow<com.example.database.entities.TopThreeLinksEntity?>
    val homeTopLinks: StateFlow<List<com.example.database.entities.HomeTopLinkEntity>>
    val complaintOptions: StateFlow<List<com.example.database.entities.ComplaintOptionEntity>>
    val loanServices: StateFlow<List<com.example.database.entities.FinancialServiceEntity>>
    val creditCardServices: StateFlow<List<com.example.database.entities.FinancialServiceEntity>>
    val investmentServices: StateFlow<List<com.example.database.entities.FinancialServiceEntity>>
    val financialBanner: StateFlow<com.example.database.entities.FinancialBannerEntity?>
    val whatsAppBanner: StateFlow<com.example.database.entities.WhatsAppBannerEntity?>
    val newsRssFeeds: StateFlow<List<com.example.database.entities.NewsRssFeedEntity>>
    val notices: StateFlow<List<NoticeEntity>> get() = latestNotices
    val activeLoudAlert: StateFlow<com.example.firebase.AdminLoudAlert?>
    val isCloudOnline: StateFlow<Boolean>

    private val _greetingHi = MutableStateFlow("सुप्रभात (Good Morning)")
    val greetingHi: StateFlow<String> = _greetingHi.asStateFlow()

    private val _greetingEn = MutableStateFlow("Good Morning")
    val greetingEn: StateFlow<String> = _greetingEn.asStateFlow()

    private val _currentDateTime = MutableStateFlow(getFormattedCurrentDateTime())
    val currentDateTime: StateFlow<String> = _currentDateTime.asStateFlow()

    private val _currentDateOnly = MutableStateFlow(getFormattedCurrentDateOnly())
    val currentDateOnly: StateFlow<String> = _currentDateOnly.asStateFlow()

    private val _isManualRefreshing = MutableStateFlow(false)
    val isManualRefreshing: StateFlow<Boolean> = _isManualRefreshing.asStateFlow()

    private val _schoolStatus = MutableStateFlow(TodaySchoolStatus())
    val schoolStatus: StateFlow<TodaySchoolStatus> = _schoolStatus.asStateFlow()

    private val _pendingDeepLink = MutableStateFlow<com.example.core.DeepLinkTarget?>(null)
    val pendingDeepLink: StateFlow<com.example.core.DeepLinkTarget?> = _pendingDeepLink.asStateFlow()

    fun dispatchDeepLink(target: com.example.core.DeepLinkTarget?) {
        if (target != null) {
            _pendingDeepLink.value = target
        }
    }

    fun consumeDeepLink(eventId: Long) {
        val current = _pendingDeepLink.value
        if (current != null && (eventId == 0L || current.eventId == eventId)) {
            _pendingDeepLink.value = null
        }
    }

    init {
        com.example.core.RssSyncCache.init(application)
        val db = AppDatabase.getDatabase(application, viewModelScope)
        repository = AppRepository(db)

        val firebaseManager = FirebaseManager.getInstance(application)
        isCloudOnline = firebaseManager.isCloudConnected
        firebaseManager.startCloudSync(repository, viewModelScope)

        teacherProfile = repository.teacherProfile
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        schoolProfile = repository.schoolProfile
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        featuredNotices = repository.featuredNotices
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        latestNotices = repository.allNotices
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        portals = repository.allPortals
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        promoBanners = repository.allBanners
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        activeActivities = repository.allActivities
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        whatsAppData = repository.whatsAppData
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        schoolTiming = repository.schoolTiming
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        downloadResources = repository.allDownloads
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        newsArticles = repository.allNews
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        videoTutorials = repository.allVideos
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        featuredHomeVideos = repository.featuredHomeVideos
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        googleDriveConfig = repository.googleDriveConfig
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        featureBoxes = repository.allFeatureBoxes
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        topThreeLinks = repository.topThreeLinks
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        homeTopLinks = repository.activeHomeTopLinks
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        complaintOptions = repository.activeComplaintOptions
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        loanServices = repository.getActiveFinancialServicesByCategory("LOAN")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        creditCardServices = repository.getActiveFinancialServicesByCategory("CREDIT_CARD")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        investmentServices = repository.getActiveFinancialServicesByCategory("INVESTMENT")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        financialBanner = repository.financialBanner
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        whatsAppBanner = repository.whatsAppBanner
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        newsRssFeeds = repository.activeNewsRssFeeds
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        activeLoudAlert = firebaseManager.activeLoudAlert

        // Ensure initial feature boxes, top links, complaint options, and financial services are seeded
        viewModelScope.launch {
            try {
                if (repository.getAllFeatureBoxesOnce().isEmpty()) {
                    repository.syncFeatureBoxesFromCloud(com.example.database.entities.DEFAULT_FEATURE_BOXES)
                }
                if (repository.getAllHomeTopLinksOnce().isEmpty()) {
                    com.example.database.entities.DEFAULT_HOME_TOP_LINKS.forEach { repository.insertHomeTopLink(it) }
                }
                if (repository.getAllComplaintOptionsOnce().isEmpty()) {
                    com.example.database.entities.DEFAULT_COMPLAINT_OPTIONS.forEach { repository.insertComplaintOption(it) }
                }
                if (repository.getAllFinancialServicesOnce().isEmpty()) {
                    com.example.database.entities.DEFAULT_FINANCIAL_SERVICES.forEach { repository.insertFinancialService(it) }
                }
                repository.removeInvalidPlaceholderFeeds()
                if (repository.getAllYouTubeFeedsOnce().isEmpty()) {
                    com.example.database.entities.DEFAULT_YOUTUBE_FEEDS.forEach { repository.insertYouTubeFeed(it) }
                }
                if (repository.getAllNewsRssFeedsOnce().isEmpty()) {
                    com.example.database.entities.DEFAULT_NEWS_RSS_FEEDS.forEach { repository.insertNewsRssFeed(it) }
                }
                if (repository.getTopThreeLinksOnce() == null) {
                    repository.saveTopThreeLinks(com.example.database.entities.DEFAULT_TOP_THREE_LINKS)
                }
                if (repository.getFinancialBannerOnce() == null) {
                    repository.saveFinancialBanner(com.example.database.entities.DEFAULT_FINANCIAL_BANNER)
                }
                if (repository.getWhatsAppBannerOnce() == null) {
                    repository.saveWhatsAppBanner(com.example.database.entities.DEFAULT_WHATSAPP_BANNER)
                }
                // Try initial YouTube and News RSS refresh only when 4-hour cooldown has elapsed
                repository.refreshYouTubeRssVideos(forceRefresh = false)
                repository.refreshNewsRssArticles(forceRefresh = false)
            } catch (_: Exception) {}
        }

        calculateGreeting()
        computeSchoolStatus()

        // React immediately to school timing updates from Admin or Cloud
        viewModelScope.launch {
            repository.schoolTiming.collect {
                computeSchoolStatus()
            }
        }

        // 1-Second Clock Tick: updates live UI date & time smoothly and computes remaining school time
        viewModelScope.launch {
            var tickCount = 0
            while (true) {
                try {
                    _currentDateTime.value = getFormattedCurrentDateTime()
                    tickCount++
                    if (tickCount % 60 == 0) {
                        computeSchoolStatus()
                    }
                } catch (t: Throwable) {
                    // Safe guard
                }
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    fun manualRefresh() {
        viewModelScope.launch {
            _isManualRefreshing.value = true
            val firebaseManager = FirebaseManager.getInstance(getApplication())
            firebaseManager.refreshCloudSyncNow(repository)
            computeSchoolStatus()
            calculateGreeting()
            kotlinx.coroutines.delay(800)
            _isManualRefreshing.value = false
        }
    }

    fun dismissLoudAlert() {
        com.example.firebase.FirebaseManager.getInstance(getApplication()).dismissLoudAlert()
    }

    fun triggerCloudSyncNow() {
        manualRefresh()
    }

    fun saveTeacherProfileLocally(
        name: String,
        schoolName: String,
        udiseCode: String,
        designation: String
    ) {
        viewModelScope.launch {
            val existing = repository.getTeacherProfileOnce() ?: TeacherProfileEntity()
            val updated = existing.copy(
                name = name.trim().ifBlank { existing.name },
                schoolName = schoolName.trim().ifBlank { existing.schoolName },
                udiseCode = udiseCode.trim().ifBlank { existing.udiseCode },
                designation = designation.trim().ifBlank { existing.designation }
            )
            repository.saveTeacherProfile(updated)
        }
    }

    private fun computeSchoolStatus() {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val month = cal.get(Calendar.MONTH) + 1
            val year = cal.get(Calendar.YEAR)
            val todayStr = "%02d/%02d/%04d".format(day, month, year)
            val currentHour = cal.get(Calendar.HOUR_OF_DAY)
            val currentMinute = cal.get(Calendar.MINUTE)
            val currentMinsOfDay = currentHour * 60 + currentMinute

            val timing = repository.getSchoolTimingOnce() ?: com.example.database.entities.SchoolTimingEntity()
            val openTimeStr = timing.openingTime.ifBlank { "09:00 AM" }
            val closeTimeStr = timing.closingTime.ifBlank { "04:30 PM" }

            val holidays = repository.allHolidaysOnce()
            val matchingHoliday = holidays.find { it.dateStr == todayStr }

            val nextHoliday = holidays.firstOrNull { h ->
                val parts = h.dateStr.split("/")
                if (parts.size == 3) {
                    val hDay = parts[0].toIntOrNull() ?: 0
                    val hMonth = parts[1].toIntOrNull() ?: 0
                    val hYear = parts[2].toIntOrNull() ?: 0
                    (hYear > year) || (hYear == year && hMonth > month) || (hYear == year && hMonth == month && hDay > day)
                } else false
            }
            val nextInfo = if (nextHoliday != null) "अगला अवकाश: ${nextHoliday.name} (${nextHoliday.dateStr})" else "अगला अवकाश शीघ्र घोषित होगा"

            // Calculate remaining minutes based on dynamic timing string
            val openMinsOfDay = parseTimeToMinutes(openTimeStr, 9 * 60) // Default 09:00 AM
            val closeMinsOfDay = parseTimeToMinutes(closeTimeStr, 16 * 60 + 30) // Default 04:30 PM

            if (dayOfWeek == Calendar.SUNDAY) {
                _schoolStatus.value = TodaySchoolStatus(
                    isOpen = false,
                    statusBadge = "विद्यालय बंद है (CLOSED)",
                    statusTitle = "आज रविवार: विद्यालय में साप्ताहिक अवकाश है",
                    statusSubtitle = "कल सोमवार को विद्यालय नियत समय पर संचालित होगा",
                    openingTime = openTimeStr,
                    closingTime = closeTimeStr,
                    remainingTimeText = "साप्ताहिक अवकाश",
                    holidayName = "रविवार साप्ताहिक अवकाश",
                    holidayType = "साप्ताहिक अवकाश",
                    nextHolidayInfo = nextInfo,
                    dateTagged = todayStr
                )
            } else if (timing.isManuallyClosed) {
                _schoolStatus.value = TodaySchoolStatus(
                    isOpen = false,
                    statusBadge = "विद्यालय बंद है (CLOSED)",
                    statusTitle = "प्रशासनिक आदेश अनुसार विद्यालय बंद है",
                    statusSubtitle = timing.closureReason.ifBlank { "अपरिहार्य कारणों से विद्यालय स्थगित" },
                    openingTime = openTimeStr,
                    closingTime = closeTimeStr,
                    remainingTimeText = "प्रशासनिक अवकाश",
                    holidayName = timing.closureReason.ifBlank { "विशेष प्रशासनिक अवकाश" },
                    holidayType = "विशेष अवकाश",
                    nextHolidayInfo = nextInfo,
                    dateTagged = todayStr
                )
            } else if (matchingHoliday != null) {
                _schoolStatus.value = TodaySchoolStatus(
                    isOpen = false,
                    statusBadge = "विद्यालय बंद है (CLOSED)",
                    statusTitle = "आज विद्यालय में अवकाश है: ${matchingHoliday.name}",
                    statusSubtitle = "अवकाश प्रकार: ${matchingHoliday.type} • अवकाश तिथि: ${matchingHoliday.dateStr}",
                    openingTime = openTimeStr,
                    closingTime = closeTimeStr,
                    remainingTimeText = "आज अवकाश है",
                    holidayName = matchingHoliday.name,
                    holidayType = matchingHoliday.type,
                    nextHolidayInfo = nextInfo,
                    dateTagged = todayStr
                )
            } else {
                // Regular Working Day
                val remainingMins = closeMinsOfDay - currentMinsOfDay
                val (isOpenNow, countdownText) = when {
                    currentMinsOfDay < openMinsOfDay -> {
                        val waitMins = openMinsOfDay - currentMinsOfDay
                        Pair(true, "खुलने में ${waitMins / 60} घंटे ${waitMins % 60} मिनट शेष")
                    }
                    currentMinsOfDay in openMinsOfDay..closeMinsOfDay -> {
                        val hrs = remainingMins / 60
                        val mins = remainingMins % 60
                        Pair(true, "बंद होने में ${hrs} घंटे ${mins} मिनट शेष")
                    }
                    else -> {
                        Pair(false, "आज का कार्य दिवस समाप्त (${closeTimeStr})")
                    }
                }

                _schoolStatus.value = TodaySchoolStatus(
                    isOpen = isOpenNow,
                    statusBadge = if (isOpenNow) "विद्यालय खुला है (OPEN)" else "विद्यालय बंद है (CLOSED)",
                    statusTitle = if (isOpenNow) "आज विद्यालय खुला है (सामान्य कार्य दिवस)" else "विद्यालय समय समाप्त हो चुका है",
                    statusSubtitle = "संचालन समय: $openTimeStr से $closeTimeStr",
                    openingTime = openTimeStr,
                    closingTime = closeTimeStr,
                    remainingTimeText = countdownText,
                    holidayName = "",
                    holidayType = "सामान्य कार्य दिवस",
                    nextHolidayInfo = nextInfo,
                    dateTagged = todayStr
                )
            }
        }
    }

    private fun parseTimeToMinutes(timeStr: String, defaultMins: Int): Int {
        return try {
            val clean = timeStr.trim().uppercase()
            val isPm = clean.contains("PM")
            val isAm = clean.contains("AM")
            val timePart = clean.replace("AM", "").replace("PM", "").trim()
            val parts = timePart.split(":")
            var hour = parts[0].trim().toInt()
            val min = if (parts.size > 1) parts[1].trim().toInt() else 0
            if (isPm && hour < 12) hour += 12
            if (isAm && hour == 12) hour = 0
            hour * 60 + min
        } catch (_: Exception) {
            defaultMins
        }
    }

    fun calculateGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 4..11 -> {
                _greetingHi.value = "सुप्रभात (Good Morning)"
                _greetingEn.value = "Good Morning"
            }
            in 12..16 -> {
                _greetingHi.value = "शुभ दोपहर (Good Afternoon)"
                _greetingEn.value = "Good Afternoon"
            }
            else -> {
                _greetingHi.value = "शुभ संध्या (Good Evening)"
                _greetingEn.value = "Good Evening"
            }
        }
    }

    fun toggleBookmark(notice: NoticeEntity) {
        viewModelScope.launch {
            repository.toggleBookmarkNotice(notice)
        }
    }

    fun toggleNoticeBookmark(noticeId: Long) {
        viewModelScope.launch {
            latestNotices.value.find { it.id == noticeId }?.let {
                repository.toggleBookmarkNotice(it)
            }
        }
    }

    fun refreshExternalFeeds(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                repository.refreshYouTubeRssVideos(forceRefresh = true)
                repository.refreshNewsRssArticles(forceRefresh = true)
            } catch (_: Exception) {}
            onComplete?.invoke()
        }
    }
}
