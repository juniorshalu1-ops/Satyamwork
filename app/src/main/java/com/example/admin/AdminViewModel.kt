package com.example.admin

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cloudflare.R2Config
import com.example.cloudflare.R2FileItem
import com.example.cloudflare.R2HealthStatus
import com.example.cloudflare.R2HttpException
import com.example.cloudflare.R2StorageRepository
import com.example.cloudflare.R2UploadResult
import com.example.database.AppDatabase
import com.example.database.AppRepository
import com.example.database.entities.AdminAuditLogEntity
import com.example.database.entities.EducationActivityEntity
import com.example.database.entities.HolidayEntity
import com.example.database.entities.NoticeEntity
import com.example.database.entities.QuickPortalEntity
import com.example.firebase.FirebaseManager
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

data class AdminSession(
    val isAuthenticated: Boolean = false,
    val username: String = "",
    val email: String = "",
    val role: String = "",
    val isMustChangePassword: Boolean = false
)

class AdminViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val SUPER_ADMIN_EMAIL = "satyamprakash100@gmail.com"
    }

    private val repository: AppRepository
    private val firebaseManager: FirebaseManager

    private val _session = MutableStateFlow(AdminSession())
    val session: StateFlow<AdminSession> = _session.asStateFlow()

    init {
        com.example.core.RssSyncCache.init(application)
        val db = AppDatabase.getDatabase(application, viewModelScope)
        repository = AppRepository(db)
        firebaseManager = FirebaseManager.getInstance(application)

        // Restore verified Firebase Google Admin session if still signed in as authorized admin
        val currentUser = runCatching { Firebase.auth.currentUser }.getOrNull()
        val currentEmail = currentUser?.email?.trim().orEmpty()
        if (currentEmail.equals(SUPER_ADMIN_EMAIL, ignoreCase = true)) {
            _session.value = AdminSession(
                isAuthenticated = true,
                username = "विभागीय मुख्य व्यवस्थापक",
                email = currentEmail,
                role = "SUPER_ADMIN",
                isMustChangePassword = false
            )
        }
    }

    private val _activeAdminSubSection = MutableStateFlow<String?>(null)
    val activeAdminSubSection: StateFlow<String?> = _activeAdminSubSection.asStateFlow()

    var savedDashboardGridIndex: Int = 0
    var savedDashboardGridOffset: Int = 0

    fun setActiveAdminSubSection(sectionId: String?) {
        _activeAdminSubSection.value = sectionId
    }

    // 1. Notices Flow
    val notices: StateFlow<List<NoticeEntity>> = repository.allNotices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2. Holidays Flow
    val holidays: StateFlow<List<HolidayEntity>> = repository.allHolidays
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. Activities Flow (मशाल, इको क्लब, आदि)
    val activities: StateFlow<List<EducationActivityEntity>> = repository.allActivities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 4. Quick Portals Flow (ई-शिक्षाकोष, मेधासॉफ्ट, आदि)
    val portals: StateFlow<List<QuickPortalEntity>> = repository.allPortals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 5. Audit Logs Flow
    val auditLogs: StateFlow<List<AdminAuditLogEntity>> = repository.auditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 6. Promo Banners Flow
    val promoBanners: StateFlow<List<com.example.database.entities.PromoBannerEntity>> = repository.allBanners
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 7. School Timing Flow
    val schoolTiming: StateFlow<com.example.database.entities.SchoolTimingEntity?> = repository.schoolTiming
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 8. WhatsApp Config Flow
    val whatsAppConfig: StateFlow<com.example.database.entities.WhatsAppLinkConfigEntity?> = repository.whatsAppData
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 9. Video Tutorials Flow
    val videos: StateFlow<List<com.example.database.entities.VideoTutorialEntity>> = repository.allVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 10. Downloads Flow
    val downloads: StateFlow<List<com.example.database.entities.DownloadResourceEntity>> = repository.allDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val googleDriveConfig: StateFlow<com.example.database.entities.GoogleDriveConfigEntity?> = repository.googleDriveConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 11. News Articles Flow
    val newsArticles: StateFlow<List<com.example.database.entities.NewsArticleEntity>> = repository.allNews
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 12. Feature Boxes Flow
    val featureBoxes: StateFlow<List<com.example.database.entities.FeatureBoxEntity>> = repository.allFeatureBoxes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 13. Top 3 Links Flow
    val topThreeLinks: StateFlow<com.example.database.entities.TopThreeLinksEntity?> = repository.topThreeLinks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 14. Financial Banner Flow
    val financialBanner: StateFlow<com.example.database.entities.FinancialBannerEntity?> = repository.financialBanner
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 15. WhatsApp Banner Flow
    val whatsAppBanner: StateFlow<com.example.database.entities.WhatsAppBannerEntity?> = repository.whatsAppBanner
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 16. Dynamic Home Top Links Flow
    val homeTopLinks: StateFlow<List<com.example.database.entities.HomeTopLinkEntity>> = repository.allHomeTopLinks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 17. Complaint Options Flow
    val complaintOptions: StateFlow<List<com.example.database.entities.ComplaintOptionEntity>> = repository.allComplaintOptions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 18. Financial Services Flows
    val loanServices: StateFlow<List<com.example.database.entities.FinancialServiceEntity>> = repository.getAllFinancialServicesByCategory("LOAN")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val creditCardServices: StateFlow<List<com.example.database.entities.FinancialServiceEntity>> = repository.getAllFinancialServicesByCategory("CREDIT_CARD")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val investmentServices: StateFlow<List<com.example.database.entities.FinancialServiceEntity>> = repository.getAllFinancialServicesByCategory("INVESTMENT")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 19. YouTube RSS Feeds Flow
    val youtubeFeeds: StateFlow<List<com.example.database.entities.YouTubeFeedEntity>> = repository.allYouTubeFeeds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 20. News RSS Feeds Flow (The Magadh Times, etc.)
    val newsRssFeeds: StateFlow<List<com.example.database.entities.NewsRssFeedEntity>> = repository.allNewsRssFeeds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Admin Feature Flags & Remote Config
    val isAiEnabled = MutableStateFlow(true)
    val dailyAiLimitPerDevice = MutableStateFlow(10)
    val currentPublishedDa = MutableStateFlow(53.0)
    val currentHraUrban = MutableStateFlow(16.0)
    val currentHraRural = MutableStateFlow(8.0)
    val maintenanceMode = MutableStateFlow(false)

    fun isOnline(): Boolean {
        return firebaseManager.isNetworkCurrentlyConnected()
    }

    /**
     * Google Sign-In + Firebase Auth verification strictly restricted to satyamprakash100@gmail.com.
     * Obtains a real Firebase Auth session so Cloudflare Worker APIs can verify the Admin ID Token.
     */
    suspend fun signInAdminWithGoogleIdToken(googleIdToken: String): Result<AdminSession> {
        if (!isOnline()) {
            return Result.failure(IllegalStateException("Google Sign-In हेतु इंटरनेट कनेक्शन आवश्यक है।"))
        }
        return try {
            val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
            val authResult = Firebase.auth.signInWithCredential(credential).await()
            val signedInEmail = authResult.user?.email?.trim().orEmpty()

            if (!signedInEmail.equals(SUPER_ADMIN_EMAIL, ignoreCase = true)) {
                runCatching { Firebase.auth.signOut() }
                return Result.failure(
                    SecurityException("अस्वीकृत: केवल $SUPER_ADMIN_EMAIL ही व्यवस्थापक के रूप में अधिकृत है।")
                )
            }

            val adminSession = AdminSession(
                isAuthenticated = true,
                username = "विभागीय मुख्य व्यवस्थापक",
                email = signedInEmail,
                role = "SUPER_ADMIN",
                isMustChangePassword = false
            )
            _session.value = adminSession
            repository.logAdminAction(
                signedInEmail,
                "GOOGLE_LOGIN",
                "Firebase Google Sign-In authenticated Super Admin ($signedInEmail)"
            )
            Result.success(adminSession)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAdminIdToken(forceRefresh: Boolean = false): String? {
        val user = runCatching { Firebase.auth.currentUser }.getOrNull() ?: return null
        if (!user.email.orEmpty().trim().equals(SUPER_ADMIN_EMAIL, ignoreCase = true)) {
            return null
        }
        return runCatching { user.getIdToken(forceRefresh).await()?.token }.getOrNull()
    }

    fun logout() {
        val user = _session.value.email.ifBlank { _session.value.username }
        _session.value = AdminSession(isAuthenticated = false)
        runCatching { Firebase.auth.signOut() }
        viewModelScope.launch {
            repository.logAdminAction(user, "LOGOUT", "Admin logged out")
        }
    }

    // ==========================================
    // CLOUDFLARE R2 STORAGE & CDN OPERATIONS
    // ==========================================
    private val r2Mutex = Mutex()

    private val _r2WorkerUrl = MutableStateFlow(R2Config.getWorkerBaseUrl(getApplication()))
    val r2WorkerUrl: StateFlow<String> = _r2WorkerUrl.asStateFlow()

    private val _r2Files = MutableStateFlow<List<R2FileItem>>(emptyList())
    val r2Files: StateFlow<List<R2FileItem>> = _r2Files.asStateFlow()

    private val _isLoadingR2 = MutableStateFlow(false)
    val isLoadingR2: StateFlow<Boolean> = _isLoadingR2.asStateFlow()

    private val _r2HealthStatus = MutableStateFlow<R2HealthStatus?>(null)
    val r2HealthStatus: StateFlow<R2HealthStatus?> = _r2HealthStatus.asStateFlow()

    private val _r2StatusMessage = MutableStateFlow<String?>(null)
    val r2StatusMessage: StateFlow<String?> = _r2StatusMessage.asStateFlow()

    private val _r2ErrorMessage = MutableStateFlow<String?>(null)
    val r2ErrorMessage: StateFlow<String?> = _r2ErrorMessage.asStateFlow()

    private val _r2AuthRequired = MutableStateFlow(false)
    val r2AuthRequired: StateFlow<Boolean> = _r2AuthRequired.asStateFlow()

    private suspend fun <T> executeWithAdminToken(
        block: suspend (token: String) -> Result<T>
    ): Result<T> {
        if (!isOnline()) {
            return Result.failure(IllegalStateException("इंटरनेट कनेक्शन उपलब्ध नहीं है।"))
        }
        val firstToken = getAdminIdToken(forceRefresh = false) ?: getAdminIdToken(forceRefresh = true)
        if (firstToken.isNullOrBlank()) {
            _r2AuthRequired.value = true
            return Result.failure(
                IllegalStateException("Google एडमिन सत्र समाप्त या अनुपलब्ध है। कृपया Google से पुनः साइन-इन करें।")
            )
        }
        val firstResult = block(firstToken)
        val err = firstResult.exceptionOrNull()
        val finalResult = if (err is R2HttpException && err.statusCode == 401) {
            val freshToken = getAdminIdToken(forceRefresh = true)
            if (!freshToken.isNullOrBlank()) {
                block(freshToken)
            } else {
                firstResult
            }
        } else {
            firstResult
        }

        val finalErr = finalResult.exceptionOrNull()
        if (finalErr is R2HttpException && (finalErr.statusCode == 401 || finalErr.statusCode == 403)) {
            _r2AuthRequired.value = true
        } else if (finalResult.isSuccess) {
            _r2AuthRequired.value = false
        }
        return finalResult
    }

    fun saveR2WorkerUrl(rawUrl: String): String {
        val saved = R2Config.saveWorkerBaseUrl(getApplication(), rawUrl)
        _r2WorkerUrl.value = saved
        _r2ErrorMessage.value = null
        _r2StatusMessage.value = "Worker URL अपडेट किया गया: $saved"
        refreshR2Dashboard()
        return saved
    }

    fun resetR2WorkerUrl(): String {
        R2Config.resetWorkerBaseUrl(getApplication())
        val defaultUrl = R2Config.getWorkerBaseUrl(getApplication())
        _r2WorkerUrl.value = defaultUrl
        _r2ErrorMessage.value = null
        _r2StatusMessage.value = "डिफ़ॉल्ट Worker URL बहाल किया गया: $defaultUrl"
        refreshR2Dashboard()
        return defaultUrl
    }

    fun refreshR2Dashboard(prefix: String = "") {
        viewModelScope.launch {
            r2Mutex.withLock {
                _isLoadingR2.value = true
                _r2ErrorMessage.value = null
                _r2StatusMessage.value = "Cloudflare Worker एवं R2 बकेट सिंक हो रहा है..."
                try {
                    val healthRes = R2StorageRepository.checkHealth(getApplication())
                    healthRes.onSuccess { status ->
                        _r2HealthStatus.value = status
                    }.onFailure { err ->
                        _r2HealthStatus.value = null
                        _r2ErrorMessage.value = "Worker कनेक्शन त्रुटि: ${err.message}"
                        _r2StatusMessage.value = null
                    }
                    if (healthRes.isFailure) return@withLock

                    loadR2FilesInternal(prefix)
                } finally {
                    _isLoadingR2.value = false
                }
            }
        }
    }

    fun checkR2Health() {
        viewModelScope.launch {
            r2Mutex.withLock {
                _isLoadingR2.value = true
                _r2ErrorMessage.value = null
                _r2StatusMessage.value = "Cloudflare Worker कनेक्शन जांचा जा रहा है..."
                try {
                    R2StorageRepository.checkHealth(getApplication())
                        .onSuccess { status ->
                            _r2HealthStatus.value = status
                            _r2StatusMessage.value = if (status.ok) {
                                "कनेक्टेड: R2 Worker सक्रिय (${status.projectId})"
                            } else {
                                "Worker प्रतिक्रिया असामान्य"
                            }
                        }
                        .onFailure { err ->
                            _r2HealthStatus.value = null
                            _r2ErrorMessage.value = "कनेक्शन त्रुटि: ${err.message}"
                            _r2StatusMessage.value = null
                        }
                } finally {
                    _isLoadingR2.value = false
                }
            }
        }
    }

    fun loadR2Files(prefix: String = "") {
        viewModelScope.launch {
            r2Mutex.withLock {
                _isLoadingR2.value = true
                _r2ErrorMessage.value = null
                try {
                    loadR2FilesInternal(prefix)
                } finally {
                    _isLoadingR2.value = false
                }
            }
        }
    }

    private suspend fun loadR2FilesInternal(prefix: String = "") {
        executeWithAdminToken { token ->
            R2StorageRepository.listFiles(getApplication(), token, prefix)
        }.onSuccess { files ->
            _r2Files.value = files
            _r2ErrorMessage.value = null
            _r2StatusMessage.value = "${files.size} फाइलें Cloudflare R2 CDN में सिंक हैं"
        }.onFailure { err ->
            _r2ErrorMessage.value = "R2 बकेट सिंक त्रुटि: ${err.message}"
            _r2StatusMessage.value = null
        }
    }

    fun deleteR2FileDirectly(item: R2FileItem) {
        viewModelScope.launch {
            r2Mutex.withLock {
                _isLoadingR2.value = true
                _r2ErrorMessage.value = null
                try {
                    val target = item.key.ifBlank { item.url }
                    executeWithAdminToken { token ->
                        R2StorageRepository.deleteByUrlOrKey(getApplication(), target, token)
                    }.onSuccess { deleted ->
                        if (deleted) {
                            _r2Files.value = _r2Files.value.filterNot { it.key == item.key }
                            _r2StatusMessage.value = "फाइल R2 से हटाई गई: ${item.key}"
                            repository.logAdminAction(_session.value.email, "R2_DELETE", "Deleted R2 object: ${item.key}")
                        } else {
                            _r2ErrorMessage.value = "R2 फाइल हटाई नहीं जा सकी (${item.key})"
                        }
                    }.onFailure { err ->
                        _r2ErrorMessage.value = "R2 डिलीट त्रुटि: ${err.message}"
                    }
                } finally {
                    _isLoadingR2.value = false
                }
            }
        }
    }

    fun publishLibraryManifestToR2() {
        viewModelScope.launch {
            r2Mutex.withLock {
                _isLoadingR2.value = true
                _r2ErrorMessage.value = null
                _r2StatusMessage.value = "Static JSON Catalog Manifest R2 CDN पर प्रकाशित हो रहा है..."
                try {
                    val allDownloads = repository.allDownloadsOnce().filter { it.isVisible }
                    val allNotices = repository.allNoticesOnce()
                    val allBanners = repository.allBannersOnce().filter { it.isActive }
                    val allPortals = repository.allPortalsOnce()

                    val payload = JSONObject().apply {
                        put("generatedAt", System.currentTimeMillis())
                        put("noticesCount", allNotices.size)
                        put("downloadsCount", allDownloads.size)
                        put("bannersCount", allBanners.size)
                        put("portalsCount", allPortals.size)

                        val dlArr = JSONArray()
                        allDownloads.forEach { d ->
                            dlArr.put(
                                JSONObject().apply {
                                    put("title", d.title)
                                    put("category", d.category)
                                    put("fileType", d.fileType)
                                    put("fileSize", d.fileSize)
                                    put("url", d.pdfUrl)
                                    put("timestamp", d.timestamp)
                                }
                            )
                        }
                        put("downloads", dlArr)

                        val noticeArr = JSONArray()
                        allNotices.take(100).forEach { n ->
                            noticeArr.put(
                                JSONObject().apply {
                                    put("title", n.title)
                                    put("category", n.category)
                                    put("orderNumber", n.orderNumber)
                                    put("pdfUrl", n.pdfUrl)
                                    put("imageUrl", n.imageUrl)
                                    put("issueDate", n.issueDate)
                                }
                            )
                        }
                        put("notices", noticeArr)

                        val bannerArr = JSONArray()
                        allBanners.forEach { b ->
                            bannerArr.put(
                                JSONObject().apply {
                                    put("bannerId", b.bannerId)
                                    put("title", b.title)
                                    put("imageUrl", b.imageUrl)
                                    put("destinationUrl", b.destinationUrl)
                                    put("orderIndex", b.orderIndex)
                                }
                            )
                        }
                        put("banners", bannerArr)
                    }

                    executeWithAdminToken { token ->
                        R2StorageRepository.publishManifest(getApplication(), "library_catalog", payload, token)
                    }.onSuccess { url ->
                        _r2ErrorMessage.value = null
                        _r2StatusMessage.value = "Static JSON Manifest R2 CDN पर प्रकाशित: $url"
                        repository.logAdminAction(_session.value.email, "R2_MANIFEST", "Published manifest: $url")
                        loadR2FilesInternal()
                    }.onFailure { err ->
                        _r2ErrorMessage.value = "Manifest त्रुटि: ${err.message}"
                        _r2StatusMessage.value = null
                    }
                } finally {
                    _isLoadingR2.value = false
                }
            }
        }
    }

    suspend fun uploadFileToR2(
        uri: Uri,
        folder: String,
        title: String,
        replaceUrl: String = "",
        onProgress: (Float) -> Unit = {}
    ): Result<R2UploadResult> {
        val result = executeWithAdminToken { token ->
            R2StorageRepository.uploadUri(
                context = getApplication(),
                uri = uri,
                folder = folder,
                title = title,
                idToken = token,
                replaceUrl = replaceUrl,
                onProgress = onProgress
            )
        }
        result.onSuccess { uploaded ->
            repository.logAdminAction(
                _session.value.email.ifBlank { SUPER_ADMIN_EMAIL },
                "R2_UPLOAD",
                "Uploaded to R2 ($folder): ${uploaded.key} (${uploaded.sizeFormatted})"
            )
        }
        return result
    }

    private fun deleteR2FileIfReplacedOrRemoved(oldUrl: String, newUrl: String = "") {
        val oldTrimmed = oldUrl.trim()
        val newTrimmed = newUrl.trim()
        if (oldTrimmed.isBlank() || oldTrimmed == newTrimmed) return
        if (!R2Config.isR2ManagedUrl(getApplication(), oldTrimmed)) return
        viewModelScope.launch {
            executeWithAdminToken { token ->
                R2StorageRepository.deleteByUrl(getApplication(), oldTrimmed, token)
            }
        }
    }

    private fun reportCloudAction(actionLabel: String, success: Boolean) {
        syncStatusMessage.value = if (success) {
            "क्लाउड अपडेट सफल: $actionLabel"
        } else {
            "चेतावनी: '$actionLabel' क्लाउड सर्वर पर सेव नहीं हो सका (इंटरनेट या अधिकृत Google लॉगिन जांचें)।"
        }
    }

    // ==========================================
    // SECTION 1: NOTICES & ORDERS (विभागीय आदेश)
    // ==========================================
    fun createNotice(
        title: String,
        summary: String,
        category: String,
        orderNumber: String,
        isFeatured: Boolean,
        sendNotification: Boolean = false,
        pdfUrl: String = "",
        imageUrl: String = ""
    ) {
        viewModelScope.launch {
            val notice = NoticeEntity(
                title = title,
                summary = summary,
                category = category,
                issueDate = com.example.core.DateUtils.getCurrentDateFormatted(),
                department = "शिक्षा विभाग, बिहार सरकार",
                orderNumber = orderNumber,
                pdfUrl = pdfUrl,
                imageUrl = imageUrl,
                isFeatured = isFeatured
            )
            val newId = repository.insertNotice(notice)
            val withId = notice.copy(id = newId)
            val cloudOk = firebaseManager.publishNoticeToCloud(withId, isNew = true, sendNotification = sendNotification)
            reportCloudAction("आदेश प्रकाशन ($title)", cloudOk)
            repository.logAdminAction(_session.value.username, "CREATE_NOTICE", "Published notice (notify: $sendNotification, cloudOk: $cloudOk): $title")
        }
    }

    fun updateNotice(
        notice: NoticeEntity,
        newTitle: String,
        newSummary: String,
        newCategory: String,
        newOrderNumber: String,
        isFeatured: Boolean,
        sendNotification: Boolean = false,
        newPdfUrl: String = notice.pdfUrl,
        newImageUrl: String = notice.imageUrl
    ) {
        viewModelScope.launch {
            val updated = notice.copy(
                title = newTitle,
                summary = newSummary,
                category = newCategory,
                orderNumber = newOrderNumber,
                pdfUrl = newPdfUrl,
                imageUrl = newImageUrl,
                isFeatured = isFeatured,
                timestamp = System.currentTimeMillis()
            )
            repository.updateNotice(updated)
            val cloudOk = firebaseManager.publishNoticeToCloud(updated, isNew = false, sendNotification = sendNotification)
            reportCloudAction("आदेश अपडेट ($newTitle)", cloudOk)
            deleteR2FileIfReplacedOrRemoved(notice.pdfUrl, newPdfUrl)
            deleteR2FileIfReplacedOrRemoved(notice.imageUrl, newImageUrl)
            repository.logAdminAction(_session.value.username, "UPDATE_NOTICE", "Updated notice (notify: $sendNotification, cloudOk: $cloudOk): $newTitle")
        }
    }

    fun deleteNotice(notice: NoticeEntity) {
        viewModelScope.launch {
            repository.deleteNotice(notice)
            val cloudOk = firebaseManager.deleteNoticeFromCloud(notice)
            reportCloudAction("आदेश डिलीट (${notice.title})", cloudOk)
            deleteR2FileIfReplacedOrRemoved(notice.pdfUrl)
            deleteR2FileIfReplacedOrRemoved(notice.imageUrl)
            repository.logAdminAction(_session.value.username, "DELETE_NOTICE", "Deleted notice id: ${notice.id} (cloudOk: $cloudOk)")
        }
    }

    // ==========================================
    // SECTION 2: HOLIDAYS (अवकाश तालिका)
    // ==========================================
    fun addHoliday(name: String, dateStr: String, type: String, month: Int, durationDays: Int = 1, sendNotification: Boolean = false) {
        viewModelScope.launch {
            val holiday = HolidayEntity(
                name = name,
                dateStr = dateStr,
                dayOfWeek = "निर्धारित",
                month = month,
                type = type,
                durationDays = durationDays
            )
            val newId = repository.insertHoliday(holiday)
            val withId = holiday.copy(id = newId)
            val cloudOk = firebaseManager.publishHolidayToCloud(withId, isNew = true, sendNotification = sendNotification)
            reportCloudAction("अवकाश ($name)", cloudOk)
            repository.logAdminAction(_session.value.username, "ADD_HOLIDAY", "Added holiday (notify: $sendNotification): $name")
        }
    }

    fun updateHoliday(holiday: HolidayEntity, newName: String, newDateStr: String, newType: String, newMonth: Int, newDays: Int, sendNotification: Boolean = false) {
        viewModelScope.launch {
            val updated = holiday.copy(
                name = newName,
                dateStr = newDateStr,
                type = newType,
                month = newMonth,
                durationDays = newDays
            )
            repository.updateHoliday(updated)
            val cloudOk = firebaseManager.publishHolidayToCloud(updated, isNew = false, sendNotification = sendNotification)
            reportCloudAction("अवकाश अपडेट ($newName)", cloudOk)
            repository.logAdminAction(_session.value.username, "UPDATE_HOLIDAY", "Updated holiday (notify: $sendNotification): $newName")
        }
    }

    fun deleteHoliday(holiday: HolidayEntity) {
        viewModelScope.launch {
            repository.deleteHoliday(holiday)
            val cloudOk = firebaseManager.deleteHolidayFromCloud(holiday)
            reportCloudAction("अवकाश डिलीट (${holiday.name})", cloudOk)
            repository.logAdminAction(_session.value.username, "DELETE_HOLIDAY", "Deleted holiday: ${holiday.name}")
        }
    }

    // ==========================================
    // SECTION 3: ACTIVITIES (मशाल, इको क्लब आदि)
    // ==========================================
    fun addActivity(title: String, category: String, description: String, linkUrl: String, orderNumber: String, sendNotification: Boolean = false) {
        viewModelScope.launch {
            val act = EducationActivityEntity(
                title = title,
                category = category,
                description = description,
                linkUrl = linkUrl,
                orderNumber = orderNumber
            )
            val newId = repository.insertActivity(act)
            val withId = act.copy(id = newId)
            val cloudOk = firebaseManager.publishActivityToCloud(withId, isNew = true, sendNotification = sendNotification)
            reportCloudAction("गतिविधि ($title)", cloudOk)
            repository.logAdminAction(_session.value.username, "ADD_ACTIVITY", "Added activity (notify: $sendNotification): $title")
        }
    }

    fun updateActivity(activity: EducationActivityEntity, newTitle: String, newCategory: String, newDesc: String, newLink: String, newOrder: String, sendNotification: Boolean = false) {
        viewModelScope.launch {
            val updated = activity.copy(
                title = newTitle,
                category = newCategory,
                description = newDesc,
                linkUrl = newLink,
                orderNumber = newOrder
            )
            repository.updateActivity(updated)
            val cloudOk = firebaseManager.publishActivityToCloud(updated, isNew = false, sendNotification = sendNotification)
            reportCloudAction("गतिविधि अपडेट ($newTitle)", cloudOk)
            repository.logAdminAction(_session.value.username, "UPDATE_ACTIVITY", "Updated activity (notify: $sendNotification): $newTitle")
        }
    }

    fun deleteActivity(activity: EducationActivityEntity) {
        viewModelScope.launch {
            repository.deleteActivity(activity)
            val cloudOk = firebaseManager.deleteActivityFromCloud(activity)
            reportCloudAction("गतिविधि डिलीट (${activity.title})", cloudOk)
            repository.logAdminAction(_session.value.username, "DELETE_ACTIVITY", "Deleted activity: ${activity.title}")
        }
    }

    // ==========================================
    // SECTION 4: QUICK PORTALS & LINKS
    // ==========================================
    fun addPortal(title: String, subtitle: String, url: String, badge: String, colorHex: String, sendNotification: Boolean = false) {
        viewModelScope.launch {
            val portal = QuickPortalEntity(
                title = title,
                subtitle = subtitle,
                url = url,
                badge = badge,
                colorHex = colorHex
            )
            val newId = repository.insertPortal(portal)
            val withId = portal.copy(id = newId)
            val cloudOk = firebaseManager.publishPortalToCloud(withId, isNew = true, sendNotification = sendNotification)
            reportCloudAction("पोर्टल ($title)", cloudOk)
            repository.logAdminAction(_session.value.username, "ADD_PORTAL", "Added portal link (notify: $sendNotification): $title")
        }
    }

    fun updatePortal(portal: QuickPortalEntity, newTitle: String, newSub: String, newUrl: String, newBadge: String, newColorHex: String, sendNotification: Boolean = false) {
        viewModelScope.launch {
            val updated = portal.copy(
                title = newTitle,
                subtitle = newSub,
                url = newUrl,
                badge = newBadge,
                colorHex = newColorHex
            )
            repository.updatePortal(updated)
            val cloudOk = firebaseManager.publishPortalToCloud(updated, isNew = false, sendNotification = sendNotification)
            reportCloudAction("पोर्टल अपडेट ($newTitle)", cloudOk)
            repository.logAdminAction(_session.value.username, "UPDATE_PORTAL", "Updated portal link (notify: $sendNotification): $newTitle")
        }
    }

    fun deletePortal(portal: QuickPortalEntity) {
        viewModelScope.launch {
            repository.deletePortal(portal)
            val cloudOk = firebaseManager.deletePortalFromCloud(portal)
            reportCloudAction("पोर्टल डिलीट (${portal.title})", cloudOk)
            repository.logAdminAction(_session.value.username, "DELETE_PORTAL", "Deleted portal: ${portal.title}")
        }
    }

    // ==========================================
    // SECTION 4B: PROMOTIONAL BANNERS
    // ==========================================
    fun savePromoBanner(banner: com.example.database.entities.PromoBannerEntity, sendNotification: Boolean = false) {
        viewModelScope.launch {
            val previousImageUrl = promoBanners.value.find { it.id == banner.id }?.imageUrl.orEmpty()
            val isNew = (banner.id == 0L)
            val withId = if (isNew) {
                val newId = repository.insertBanner(banner)
                banner.copy(id = newId)
            } else {
                repository.insertBanner(banner)
                banner
            }
            val cloudOk = firebaseManager.publishBannerToCloud(withId, isNew = isNew, sendNotification = sendNotification)
            reportCloudAction("बैनर (${banner.title})", cloudOk)
            if (!isNew) {
                deleteR2FileIfReplacedOrRemoved(previousImageUrl, banner.imageUrl)
            }
            repository.logAdminAction(_session.value.username, "SAVE_BANNER", "Updated banner (notify: $sendNotification, cloudOk: $cloudOk): ${banner.title}")
        }
    }

    fun deletePromoBanner(banner: com.example.database.entities.PromoBannerEntity) {
        viewModelScope.launch {
            repository.deleteBanner(banner)
            val docId = if (banner.bannerId.isNotBlank()) banner.bannerId else banner.id.toString()
            val cloudOk = firebaseManager.deleteBannerFromCloud(docId, banner.title)
            reportCloudAction("बैनर डिलीट (${banner.title})", cloudOk)
            deleteR2FileIfReplacedOrRemoved(banner.imageUrl)
            repository.logAdminAction(_session.value.username, "DELETE_BANNER", "Deleted banner: ${banner.title} (cloudOk: $cloudOk)")
        }
    }

    // ==========================================
    // SECTION 4C: SCHOOL TIMINGS & CLOSURES
    // ==========================================
    fun updateSchoolTimings(opening: String, closing: String, isClosed: Boolean, reason: String, sendNotification: Boolean = false) {
        viewModelScope.launch {
            val timing = com.example.database.entities.SchoolTimingEntity(
                id = 1,
                openingTime = opening,
                closingTime = closing,
                isManuallyClosed = isClosed,
                closureReason = reason,
                lastUpdated = System.currentTimeMillis()
            )
            repository.saveSchoolTiming(timing)
            val cloudOk = firebaseManager.publishSchoolTimingToCloud(timing, sendNotification = sendNotification)
            reportCloudAction("विद्यालय समय", cloudOk)
            repository.logAdminAction(_session.value.username, "UPDATE_TIMINGS", "Updated school timings (notify: $sendNotification): $opening - $closing, closed: $isClosed ($reason)")
        }
    }

    // ==========================================
    // SECTION 4D: WHATSAPP LINKS CONFIG
    // ==========================================
    fun updateWhatsAppLinks(channelUrl: String, channelTitle: String, districtGroupsUrl: String, districtGroupsTitle: String, sendNotification: Boolean = false) {
        viewModelScope.launch {
            val config = com.example.database.entities.WhatsAppLinkConfigEntity(
                id = 1,
                channelUrl = channelUrl,
                channelTitle = channelTitle,
                districtGroupsUrl = districtGroupsUrl,
                districtGroupsTitle = districtGroupsTitle,
                lastUpdated = System.currentTimeMillis()
            )
            repository.saveWhatsAppData(config)
            val cloudOk = firebaseManager.publishWhatsAppConfigToCloud(config, sendNotification = sendNotification)
            reportCloudAction("व्हाट्सएप लिंक", cloudOk)
            repository.logAdminAction(_session.value.username, "UPDATE_WHATSAPP", "Updated WhatsApp links (notify: $sendNotification)")
        }
    }

    // ==========================================
    // SECTION 4E: VIDEOS, DOWNLOADS & NEWS
    // ==========================================
    fun saveVideoTutorial(video: com.example.database.entities.VideoTutorialEntity, sendNotification: Boolean = false) {
        viewModelScope.launch {
            val isNew = (video.id == 0L)
            val cloudOk = if (isNew) {
                val newId = repository.insertVideo(video)
                val withId = video.copy(id = newId)
                repository.logAdminAction(_session.value.username, "ADD_VIDEO", "Added video (notify: $sendNotification): ${video.title}")
                firebaseManager.publishVideoToCloud(withId, isNew = true, sendNotification = sendNotification)
            } else {
                repository.updateVideo(video)
                repository.logAdminAction(_session.value.username, "UPDATE_VIDEO", "Updated video (notify: $sendNotification): ${video.title}")
                firebaseManager.publishVideoToCloud(video, isNew = false, sendNotification = sendNotification)
            }
            reportCloudAction("वीडियो (${video.title})", cloudOk)
        }
    }

    fun deleteVideoTutorial(video: com.example.database.entities.VideoTutorialEntity) {
        viewModelScope.launch {
            repository.deleteVideo(video)
            val cloudOk = firebaseManager.deleteVideoFromCloud(video)
            reportCloudAction("वीडियो डिलीट (${video.title})", cloudOk)
            repository.logAdminAction(_session.value.username, "DELETE_VIDEO", "Deleted video: ${video.title}")
        }
    }

    fun togglePinVideo(video: com.example.database.entities.VideoTutorialEntity) {
        viewModelScope.launch {
            val updated = video.copy(isPinned = !video.isPinned)
            repository.updateVideo(updated)
            val cloudOk = firebaseManager.publishVideoToCloud(updated, isNew = false)
            reportCloudAction("वीडियो पिन (${video.title})", cloudOk)
            repository.logAdminAction(_session.value.username, "PIN_VIDEO", "Toggled pin for: ${video.title}")
        }
    }

    fun toggleFeatureOnHomeVideo(video: com.example.database.entities.VideoTutorialEntity) {
        viewModelScope.launch {
            val updated = video.copy(isFeaturedOnHome = !video.isFeaturedOnHome)
            repository.updateVideo(updated)
            val cloudOk = firebaseManager.publishVideoToCloud(updated, isNew = false)
            reportCloudAction("होम वीडियो (${video.title})", cloudOk)
            repository.logAdminAction(_session.value.username, "FEATURE_VIDEO_HOME", "Toggled featured on home for: ${video.title}")
        }
    }

    fun moveVideoOrder(video: com.example.database.entities.VideoTutorialEntity, moveUp: Boolean) {
        viewModelScope.launch {
            val currentList = repository.allVideosOnce()
            val index = currentList.indexOfFirst { it.id == video.id }
            if (index == -1) return@launch
            val targetIndex = if (moveUp) index - 1 else index + 1
            if (targetIndex in currentList.indices) {
                val targetVideo = currentList[targetIndex]
                val currentOrder = video.displayOrder
                val targetOrder = targetVideo.displayOrder
                val newCurrentOrder = if (currentOrder == targetOrder) (if (moveUp) targetOrder - 1 else targetOrder + 1) else targetOrder
                val updatedCurrent = video.copy(displayOrder = newCurrentOrder)
                val updatedTarget = targetVideo.copy(displayOrder = currentOrder)

                repository.updateVideo(updatedCurrent)
                repository.updateVideo(updatedTarget)
                firebaseManager.publishVideoToCloud(updatedCurrent, isNew = false)
                firebaseManager.publishVideoToCloud(updatedTarget, isNew = false)
            }
        }
    }


    // ==========================================
    // SECTION 4F: UNLIMITED MAIN FEATURE SERVICES MANAGEMENT (Requirements 4 & 5)
    // ==========================================
    fun addNewFeatureBox(title: String, subtitle: String, url: String, iconType: String = "Widgets", isVisible: Boolean = true, sendNotification: Boolean = false) {
        viewModelScope.launch {
            val all = repository.getAllFeatureBoxesOnce()
            val nextOrder = (all.maxOfOrNull { it.displayOrder } ?: 14) + 1
            val newId = "service_${System.currentTimeMillis()}"
            val newBox = com.example.database.entities.FeatureBoxEntity(
                id = newId,
                title = title.trim(),
                subtitle = subtitle.trim(),
                iconType = iconType,
                destinationRoute = "external",
                displayOrder = nextOrder,
                isVisible = isVisible,
                isExternal = url.isNotBlank(),
                externalUrl = url.trim()
            )
            repository.insertFeatureBox(newBox)
            val updatedList = repository.getAllFeatureBoxesOnce()
            val cloudOk = firebaseManager.publishFeatureBoxesToCloud(updatedList, isNew = true, sendNotification = sendNotification)
            reportCloudAction("नई सेवा ($title)", cloudOk)
            repository.logAdminAction(_session.value.username, "ADD_FEATURE_BOX", "Added new service #$nextOrder (notify: $sendNotification): $title")
        }
    }

    fun deleteFeatureBox(box: com.example.database.entities.FeatureBoxEntity) {
        viewModelScope.launch {
            repository.deleteFeatureBox(box)
            val deletedOk = firebaseManager.deleteFeatureBoxFromCloud(box)
            val updatedList = repository.getAllFeatureBoxesOnce()
            val publishedOk = firebaseManager.publishFeatureBoxesToCloud(updatedList, isNew = false)
            reportCloudAction("सेवा डिलीट (${box.title})", deletedOk && publishedOk)
            repository.logAdminAction(_session.value.username, "DELETE_FEATURE_BOX", "Deleted service: ${box.title}")
        }
    }

    fun updateFeatureBox(box: com.example.database.entities.FeatureBoxEntity, newTitle: String, newSubtitle: String, newUrl: String, isVisible: Boolean, sendNotification: Boolean = false) {
        viewModelScope.launch {
            val updated = box.copy(
                title = newTitle,
                subtitle = newSubtitle,
                externalUrl = newUrl,
                isVisible = isVisible
            )
            repository.updateFeatureBox(updated)
            val all = repository.getAllFeatureBoxesOnce()
            val mapped = all.map { if (it.id == updated.id) updated else it }
            val cloudOk = firebaseManager.publishFeatureBoxesToCloud(mapped, isNew = false, sendNotification = sendNotification)
            reportCloudAction("सेवा अपडेट ($newTitle)", cloudOk)
            repository.logAdminAction(_session.value.username, "UPDATE_FEATURE_BOX", "Updated feature box (notify: $sendNotification): $newTitle (visible: $isVisible)")
        }
    }

    fun moveFeatureBox(box: com.example.database.entities.FeatureBoxEntity, direction: Int) {
        viewModelScope.launch {
            val list = repository.getAllFeatureBoxesOnce().sortedBy { it.displayOrder }.toMutableList()
            val currentIndex = list.indexOfFirst { it.id == box.id }
            val targetIndex = currentIndex + direction
            if (currentIndex != -1 && targetIndex in list.indices) {
                val currentItem = list[currentIndex]
                val targetItem = list[targetIndex]
                val currentOrder = currentItem.displayOrder
                val targetOrder = targetItem.displayOrder
                val updatedCurrent = currentItem.copy(displayOrder = targetOrder)
                val updatedTarget = targetItem.copy(displayOrder = currentOrder)
                repository.updateFeatureBox(updatedCurrent)
                repository.updateFeatureBox(updatedTarget)
                list[currentIndex] = updatedTarget
                list[targetIndex] = updatedCurrent
                val cloudOk = firebaseManager.publishFeatureBoxesToCloud(list, isNew = false)
                reportCloudAction("सेवा क्रम (${box.title})", cloudOk)
                repository.logAdminAction(_session.value.username, "REORDER_FEATURE_BOX", "Moved ${box.title} ${if (direction < 0) "UP" else "DOWN"}")
            }
        }
    }

    // ==========================================
    // SECTION 4G: TOP 3 LINKS, FINANCIAL & WHATSAPP BANNERS
    // ==========================================
    fun saveTopThreeLinks(eshikshakoshUrl: String, hrmsUrl: String, udiseUrl: String, isVisible: Boolean, sendNotification: Boolean = false) {
        viewModelScope.launch {
            val entity = com.example.database.entities.TopThreeLinksEntity(
                id = 1,
                link1Title = "eShikshakosh",
                link1Url = eshikshakoshUrl,
                link1Visible = isVisible,
                link2Title = "HRMS Bihar",
                link2Url = hrmsUrl,
                link2Visible = isVisible,
                link3Title = "UDISE+",
                link3Url = udiseUrl,
                link3Visible = isVisible,
                lastUpdated = System.currentTimeMillis()
            )
            repository.saveTopThreeLinks(entity)
            val cloudOk = firebaseManager.publishTopThreeLinksToCloud(entity, sendNotification = sendNotification)
            reportCloudAction("मुख्य 3 पोर्टल लिंक", cloudOk)
            repository.logAdminAction(_session.value.username, "SAVE_TOP_LINKS", "Updated top 3 official links (notify: $sendNotification)")
        }
    }

    fun saveFinancialBanner(heading: String, supportingText: String, buttonText: String, destinationUrl: String, isVisible: Boolean, sendNotification: Boolean = false, imageUrl: String = "") {
        viewModelScope.launch {
            val previousImageUrl = financialBanner.value?.imageUrl.orEmpty()
            val entity = com.example.database.entities.FinancialBannerEntity(
                id = 1,
                heading = heading,
                supportingText = supportingText,
                buttonText = buttonText,
                destinationUrl = destinationUrl,
                imageUrl = imageUrl,
                isVisible = isVisible,
                lastUpdated = System.currentTimeMillis()
            )
            repository.saveFinancialBanner(entity)
            val cloudOk = firebaseManager.publishFinancialBannerToCloud(entity, sendNotification = sendNotification)
            reportCloudAction("वित्तीय बैनर", cloudOk)
            deleteR2FileIfReplacedOrRemoved(previousImageUrl, imageUrl)
            repository.logAdminAction(_session.value.username, "SAVE_FINANCIAL_BANNER", "Updated financial banner (notify: $sendNotification): $heading")
        }
    }

    fun saveWhatsAppBanner(bannerText: String, buttonText: String, channelUrl: String, isVisible: Boolean, sendNotification: Boolean = false, imageUrl: String = "") {
        viewModelScope.launch {
            val previousImageUrl = whatsAppBanner.value?.imageUrl.orEmpty()
            val entity = com.example.database.entities.WhatsAppBannerEntity(
                id = 1,
                bannerText = bannerText,
                buttonText = buttonText,
                channelUrl = channelUrl,
                imageUrl = imageUrl,
                isVisible = isVisible,
                lastUpdated = System.currentTimeMillis()
            )
            repository.saveWhatsAppBanner(entity)
            val cloudOk = firebaseManager.publishWhatsAppBannerToCloud(entity, sendNotification = sendNotification)
            reportCloudAction("व्हाट्सएप बैनर", cloudOk)
            deleteR2FileIfReplacedOrRemoved(previousImageUrl, imageUrl)
            repository.logAdminAction(_session.value.username, "SAVE_WHATSAPP_BANNER", "Updated WhatsApp banner (notify: $sendNotification): $channelUrl")
        }
    }

    // ==========================================
    // SECTION 5: SPECIAL ADMIN BROADCAST NOTIFICATION (Max 10 History)
    // ==========================================
    val broadcastHistory: StateFlow<List<com.example.firebase.AdminLoudAlert>> = firebaseManager.broadcastHistory
    val isLoadingBroadcastHistory = MutableStateFlow(false)

    fun loadBroadcastHistory(forceServer: Boolean = false) {
        viewModelScope.launch {
            isLoadingBroadcastHistory.value = true
            try {
                firebaseManager.fetchLatestBroadcastHistory(forceServer = forceServer)
            } finally {
                isLoadingBroadcastHistory.value = false
            }
        }
    }

    fun sendBroadcastNotification(title: String, section: String, details: String) {
        viewModelScope.launch {
            val cloudOk = firebaseManager.broadcastAdminUpload(title, section, details)
            reportCloudAction("विशेष घोषणा ($title)", cloudOk)
            repository.logAdminAction(_session.value.username, "SPECIAL_BROADCAST", "Sent special lockscreen notification (cloudOk: $cloudOk): $title")
        }
    }

    fun sendLoudBroadcastAlert(title: String, section: String, details: String) = sendBroadcastNotification(title, section, details)

    // ==========================================
    // SECTION 6: SALARY RULES
    // ==========================================
    fun publishSalaryRule(newDa: Double, ruralHra: Double, urbanHra: Double) {
        currentPublishedDa.value = newDa
        currentHraRural.value = ruralHra
        currentHraUrban.value = urbanHra
        viewModelScope.launch {
            repository.updateGlobalSalaryRules(newDa, ruralHra, urbanHra)
            val cloudOk = firebaseManager.publishSalaryRuleToCloud(newDa, ruralHra, urbanHra)
            reportCloudAction("वेतन नियम (DA $newDa%)", cloudOk)
            repository.logAdminAction(_session.value.username, "UPDATE_SALARY_RULE", "Published new DA: $newDa%, Rural HRA: $ruralHra%, Urban: $urbanHra%")
        }
    }

    // ==========================================
    // SECTION 7: FULL CLOUD SYNC & PUSH (डेटा क्लाउड सिंक)
    // ==========================================
    val isSyncing = MutableStateFlow(false)
    val syncStatusMessage = MutableStateFlow<String?>(null)

    fun syncAllDataToCloud(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            isSyncing.value = true
            if (!isOnline()) {
                isSyncing.value = false
                syncStatusMessage.value = "चेतावनी: सिंक विफल — कृपया इंटरनेट कनेक्शन जांचें।"
                onComplete(false)
                return@launch
            }
            val token = getAdminIdToken(forceRefresh = false) ?: getAdminIdToken(forceRefresh = true)
            if (token.isNullOrBlank()) {
                isSyncing.value = false
                syncStatusMessage.value = "चेतावनी: कृपया अधिकृत Admin Google खाता ($SUPER_ADMIN_EMAIL) से पुनः लॉगिन करें।"
                onComplete(false)
                return@launch
            }
            syncStatusMessage.value = "क्लाउड सर्वर पर सभी डेटा सिंक हो रहा है..."
            val success = firebaseManager.syncAllLocalDataToCloud(repository, forceFull = true)
            isSyncing.value = false
            syncStatusMessage.value = if (success) {
                "सभी डेटा क्लाउड सर्वर पर सफलतापूर्वक सिंक हो गया और सभी उपयोगकर्ताओं को अपडेट भेजा गया!"
            } else {
                "चेतावनी: क्लाउड सिंक असफल रहा — कृपया अधिकृत Admin खाता व इंटरनेट जांचें।"
            }
            repository.logAdminAction(_session.value.username, "MANUAL_SYNC", "Triggered full cloud push sync. Result: $success")
            onComplete(success)
        }
    }

    // ==========================================
    // SECTION 8: HOME TOP LINKS CRUD
    // ==========================================
    fun saveHomeTopLink(link: com.example.database.entities.HomeTopLinkEntity, sendNotification: Boolean = false) {
        viewModelScope.launch {
            val entityToPublish = if (link.id == 0L) {
                val newId = repository.insertHomeTopLink(link)
                val withId = link.copy(id = newId)
                repository.logAdminAction(_session.value.username, "ADD_TOP_LINK", "Added top link (notify: $sendNotification): ${link.title}")
                withId
            } else {
                repository.updateHomeTopLink(link)
                repository.logAdminAction(_session.value.username, "UPDATE_TOP_LINK", "Updated top link (notify: $sendNotification): ${link.title}")
                link
            }
            val cloudOk = firebaseManager.publishHomeTopLinkToCloud(entityToPublish, sendNotification = sendNotification)
            reportCloudAction("होम लिंक (${link.title})", cloudOk)
        }
    }

    fun deleteHomeTopLink(link: com.example.database.entities.HomeTopLinkEntity) {
        viewModelScope.launch {
            repository.deleteHomeTopLink(link)
            val cloudOk = firebaseManager.deleteHomeTopLinkFromCloud(link)
            reportCloudAction("होम लिंक डिलीट (${link.title})", cloudOk)
            repository.logAdminAction(_session.value.username, "DELETE_TOP_LINK", "Deleted top link: ${link.title}")
        }
    }

    fun toggleHomeTopLinkVisibility(link: com.example.database.entities.HomeTopLinkEntity) {
        viewModelScope.launch {
            val updated = link.copy(isVisible = !link.isVisible)
            repository.updateHomeTopLink(updated)
            val cloudOk = firebaseManager.publishHomeTopLinkToCloud(updated)
            reportCloudAction("होम लिंक दृश्यता (${link.title})", cloudOk)
        }
    }

    // ==========================================
    // SECTION 9: COMPLAINT OPTIONS CRUD
    // ==========================================
    fun saveComplaintOption(option: com.example.database.entities.ComplaintOptionEntity, sendNotification: Boolean = false) {
        viewModelScope.launch {
            val entityToPublish = if (option.id == 0L) {
                val newId = repository.insertComplaintOption(option)
                val withId = option.copy(id = newId)
                repository.logAdminAction(_session.value.username, "ADD_COMPLAINT_OPTION", "Added complaint option (notify: $sendNotification): ${option.title}")
                withId
            } else {
                repository.updateComplaintOption(option)
                repository.logAdminAction(_session.value.username, "UPDATE_COMPLAINT_OPTION", "Updated complaint option (notify: $sendNotification): ${option.title}")
                option
            }
            val cloudOk = firebaseManager.publishComplaintOptionToCloud(entityToPublish, sendNotification = sendNotification)
            reportCloudAction("शिकायत विकल्प (${option.title})", cloudOk)
        }
    }

    fun deleteComplaintOption(option: com.example.database.entities.ComplaintOptionEntity) {
        viewModelScope.launch {
            repository.deleteComplaintOption(option)
            val cloudOk = firebaseManager.deleteComplaintOptionFromCloud(option)
            reportCloudAction("शिकायत डिलीट (${option.title})", cloudOk)
            repository.logAdminAction(_session.value.username, "DELETE_COMPLAINT_OPTION", "Deleted complaint option: ${option.title}")
        }
    }

    fun toggleComplaintVisibility(option: com.example.database.entities.ComplaintOptionEntity) {
        viewModelScope.launch {
            val updated = option.copy(isVisible = !option.isVisible)
            repository.updateComplaintOption(updated)
            val cloudOk = firebaseManager.publishComplaintOptionToCloud(updated)
            reportCloudAction("शिकायत दृश्यता (${option.title})", cloudOk)
        }
    }

    // ==========================================
    // SECTION 10: FINANCIAL SERVICES CRUD (LOAN, CARD, INVEST)
    // ==========================================
    fun saveFinancialService(service: com.example.database.entities.FinancialServiceEntity, sendNotification: Boolean = false) {
        viewModelScope.launch {
            val entityToPublish = if (service.id == 0L) {
                val newId = repository.insertFinancialService(service)
                val withId = service.copy(id = newId)
                repository.logAdminAction(_session.value.username, "ADD_FINANCIAL_SERVICE", "Added ${service.category} (notify: $sendNotification): ${service.title}")
                withId
            } else {
                repository.updateFinancialService(service)
                repository.logAdminAction(_session.value.username, "UPDATE_FINANCIAL_SERVICE", "Updated ${service.category} (notify: $sendNotification): ${service.title}")
                service
            }
            val cloudOk = firebaseManager.publishFinancialServiceToCloud(entityToPublish, isNew = (service.id == 0L), sendNotification = sendNotification)
            reportCloudAction("वित्तीय सेवा (${service.title})", cloudOk)
        }
    }

    fun deleteFinancialService(service: com.example.database.entities.FinancialServiceEntity) {
        viewModelScope.launch {
            repository.deleteFinancialService(service)
            val cloudOk = firebaseManager.deleteFinancialServiceFromCloud(service)
            reportCloudAction("वित्तीय सेवा डिलीट (${service.title})", cloudOk)
            repository.logAdminAction(_session.value.username, "DELETE_FINANCIAL_SERVICE", "Deleted ${service.category}: ${service.title}")
        }
    }

    fun toggleFinancialServiceVisibility(service: com.example.database.entities.FinancialServiceEntity) {
        viewModelScope.launch {
            val updated = service.copy(isVisible = !service.isVisible)
            repository.updateFinancialService(updated)
            val cloudOk = firebaseManager.publishFinancialServiceToCloud(updated)
            reportCloudAction("वित्तीय सेवा दृश्यता (${service.title})", cloudOk)
        }
    }

    // ==========================================
    // SECTION 11: YOUTUBE RSS FEEDS CRUD & REFRESH
    // ==========================================
    fun saveYouTubeFeed(feed: com.example.database.entities.YouTubeFeedEntity) {
        viewModelScope.launch {
            val entityToPublish = if (feed.id == 0L) {
                val newId = repository.insertYouTubeFeed(feed)
                val withId = feed.copy(id = newId)
                repository.logAdminAction(_session.value.username, "ADD_YOUTUBE_FEED", "Added RSS Feed: ${feed.channelTitle}")
                withId
            } else {
                repository.updateYouTubeFeed(feed)
                repository.logAdminAction(_session.value.username, "UPDATE_YOUTUBE_FEED", "Updated RSS Feed: ${feed.channelTitle}")
                feed
            }
            val cloudOk = firebaseManager.publishYouTubeFeedToCloud(entityToPublish)
            reportCloudAction("YouTube RSS (${feed.channelTitle})", cloudOk)
            repository.refreshYouTubeRssVideos(forceRefresh = true)
        }
    }

    fun deleteYouTubeFeed(feed: com.example.database.entities.YouTubeFeedEntity) {
        viewModelScope.launch {
            repository.deleteYouTubeFeed(feed)
            val cloudOk = firebaseManager.deleteYouTubeFeedFromCloud(feed)
            reportCloudAction("YouTube RSS डिलीट (${feed.channelTitle})", cloudOk)
            repository.logAdminAction(_session.value.username, "DELETE_YOUTUBE_FEED", "Deleted RSS Feed: ${feed.channelTitle}")
        }
    }

    fun toggleYouTubeFeedVisibility(feed: com.example.database.entities.YouTubeFeedEntity) {
        viewModelScope.launch {
            val updated = feed.copy(isVisible = !feed.isVisible)
            repository.updateYouTubeFeed(updated)
            val cloudOk = firebaseManager.publishYouTubeFeedToCloud(updated)
            reportCloudAction("YouTube RSS दृश्यता (${feed.channelTitle})", cloudOk)
            repository.refreshYouTubeRssVideos(forceRefresh = true)
        }
    }

    fun refreshYouTubeFeeds(onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val count = repository.refreshYouTubeRssVideos(forceRefresh = true)
            if (count > 0) {
                firebaseManager.broadcastAdminUpload(
                    title = "यूट्यूब वीडियो अपडेट",
                    section = "वीडियो लाइब्रेरी",
                    details = "$count नए वीडियो सिंक हुए",
                    contentType = "video",
                    contentId = "",
                    targetSection = "videos",
                    targetUrl = ""
                )
            }
            onComplete(count)
        }
    }

    // ==========================================
    // SECTION 12: DOWNLOADS CRUD (PDF & JPG) & GOOGLE DRIVE
    // ==========================================
    fun saveDownloadResource(download: com.example.database.entities.DownloadResourceEntity, sendNotification: Boolean = false) {
        viewModelScope.launch {
            val previousUrl = downloads.value.find { it.id == download.id }?.pdfUrl.orEmpty()
            val canonicalCloudId = download.cloudId.ifBlank {
                com.example.core.DownloadSyncHelper.generateCloudId()
            }
            val canonicalEntity = download.copy(cloudId = canonicalCloudId)
            val isNew = (canonicalEntity.id == 0L)
            val cloudOk = if (isNew) {
                val newId = repository.insertDownload(canonicalEntity)
                val withId = canonicalEntity.copy(id = newId)
                repository.logAdminAction(_session.value.username, "ADD_DOWNLOAD", "Added download resource (notify: $sendNotification): ${canonicalEntity.title}")
                firebaseManager.publishDownloadToCloud(withId, isNew = true, sendNotification = sendNotification)
            } else {
                repository.updateDownload(canonicalEntity)
                deleteR2FileIfReplacedOrRemoved(previousUrl, canonicalEntity.pdfUrl)
                repository.logAdminAction(_session.value.username, "UPDATE_DOWNLOAD", "Updated download resource (notify: $sendNotification): ${canonicalEntity.title}")
                firebaseManager.publishDownloadToCloud(canonicalEntity, isNew = false, sendNotification = sendNotification)
            }
            reportCloudAction("डाउनलोड फाइल (${canonicalEntity.title})", cloudOk)
        }
    }

    fun deleteDownloadResource(download: com.example.database.entities.DownloadResourceEntity) {
        viewModelScope.launch {
            repository.deleteDownload(download)
            val cloudOk = firebaseManager.deleteDownloadFromCloud(download)
            reportCloudAction("डाउनलोड डिलीट (${download.title})", cloudOk)
            deleteR2FileIfReplacedOrRemoved(download.pdfUrl)
            repository.logAdminAction(_session.value.username, "DELETE_DOWNLOAD", "Deleted download: ${download.title}")
        }
    }

    fun toggleDownloadVisibility(download: com.example.database.entities.DownloadResourceEntity) {
        viewModelScope.launch {
            val updated = download.copy(isVisible = !download.isVisible)
            repository.updateDownload(updated)
            val cloudOk = firebaseManager.publishDownloadToCloud(updated, isNew = false)
            reportCloudAction("डाउनलोड दृश्यता (${download.title})", cloudOk)
        }
    }

    fun toggleGoogleDriveEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            val current = repository.getGoogleDriveConfigOnce() ?: com.example.database.entities.DEFAULT_GOOGLE_DRIVE_CONFIG
            val updated = current.copy(isEnabled = isEnabled)
            repository.saveGoogleDriveConfig(updated)
            val cloudOk = firebaseManager.publishGoogleDriveConfigToCloud(isEnabled, updated.folderUrl)
            reportCloudAction("Google Drive सेटिंग", cloudOk)
            repository.logAdminAction(_session.value.username, "TOGGLE_DRIVE", "Google Drive set to: $isEnabled")
        }
    }

    // ==========================================
    // SECTION 13: NEWS ARTICLES CRUD & PINNING
    // ==========================================
    fun saveNewsArticle(news: com.example.database.entities.NewsArticleEntity, sendNotification: Boolean = false) {
        viewModelScope.launch {
            val previousImageUrl = newsArticles.value.find { it.id == news.id }?.imageUrl.orEmpty()
            val isNew = (news.id == 0L)
            val cloudOk = if (isNew) {
                val newId = repository.insertNews(news)
                val withId = news.copy(id = newId)
                repository.logAdminAction(_session.value.username, "ADD_NEWS", "Added news (notify: $sendNotification): ${news.title}")
                firebaseManager.publishNewsToCloud(withId, isNew = true, sendNotification = sendNotification)
            } else {
                repository.updateNews(news)
                deleteR2FileIfReplacedOrRemoved(previousImageUrl, news.imageUrl)
                repository.logAdminAction(_session.value.username, "UPDATE_NEWS", "Updated news (notify: $sendNotification): ${news.title}")
                firebaseManager.publishNewsToCloud(news, isNew = false, sendNotification = sendNotification)
            }
            reportCloudAction("समाचार (${news.title})", cloudOk)
        }
    }

    fun deleteNewsArticle(news: com.example.database.entities.NewsArticleEntity) {
        viewModelScope.launch {
            repository.deleteNews(news)
            val cloudOk = firebaseManager.deleteNewsFromCloud(news)
            reportCloudAction("समाचार डिलीट (${news.title})", cloudOk)
            deleteR2FileIfReplacedOrRemoved(news.imageUrl)
            repository.logAdminAction(_session.value.username, "DELETE_NEWS", "Deleted news: ${news.title}")
        }
    }

    fun toggleNewsVisibility(news: com.example.database.entities.NewsArticleEntity) {
        viewModelScope.launch {
            val updated = news.copy(isVisible = !news.isVisible)
            repository.updateNews(updated)
            val cloudOk = firebaseManager.publishNewsToCloud(updated, isNew = false)
            reportCloudAction("समाचार दृश्यता (${news.title})", cloudOk)
        }
    }

    fun togglePinNewsArticle(news: com.example.database.entities.NewsArticleEntity, onLimitReached: () -> Unit = {}) {
        viewModelScope.launch {
            if (!news.isPinned) {
                val currentPinned = repository.allNewsOnce().filter { it.isPinned }
                if (currentPinned.size >= 5) {
                    onLimitReached()
                    return@launch
                }
                val updated = news.copy(isPinned = true, pinOrder = currentPinned.size + 1)
                repository.updateNews(updated)
                val cloudOk = firebaseManager.publishNewsToCloud(updated, isNew = false)
                reportCloudAction("समाचार पिन (${news.title})", cloudOk)
                repository.logAdminAction(_session.value.username, "PIN_NEWS", "Pinned news: ${news.title}")
            } else {
                val updated = news.copy(isPinned = false, pinOrder = 0)
                repository.updateNews(updated)
                val cloudOk = firebaseManager.publishNewsToCloud(updated, isNew = false)
                reportCloudAction("समाचार अनपिन (${news.title})", cloudOk)
                repository.logAdminAction(_session.value.username, "UNPIN_NEWS", "Unpinned news: ${news.title}")
            }
        }
    }

    fun movePinnedNewsOrder(news: com.example.database.entities.NewsArticleEntity, moveUp: Boolean) {
        viewModelScope.launch {
            val currentPinned = repository.allNewsOnce().filter { it.isPinned }.sortedBy { it.pinOrder }
            val index = currentPinned.indexOfFirst { it.id == news.id }
            if (index == -1) return@launch
            val targetIndex = if (moveUp) index - 1 else index + 1
            if (targetIndex in currentPinned.indices) {
                val targetNews = currentPinned[targetIndex]
                val currentOrder = news.pinOrder
                val targetOrder = targetNews.pinOrder

                val updatedCurrent = news.copy(pinOrder = targetOrder)
                val updatedTarget = targetNews.copy(pinOrder = currentOrder)

                repository.updateNews(updatedCurrent)
                repository.updateNews(updatedTarget)
                firebaseManager.publishNewsToCloud(updatedCurrent, isNew = false)
                firebaseManager.publishNewsToCloud(updatedTarget, isNew = false)
            }
        }
    }

    // ==========================================
    // SECTION 14: NEWS RSS FEEDS CRUD & REFRESH (The Magadh Times, etc.)
    // ==========================================
    fun saveNewsRssFeed(feed: com.example.database.entities.NewsRssFeedEntity) {
        viewModelScope.launch {
            val entityToPublish = if (feed.id == 0L) {
                val newId = repository.insertNewsRssFeed(feed)
                val withId = feed.copy(id = newId)
                repository.logAdminAction(_session.value.username, "ADD_NEWS_RSS", "Added News RSS Feed: ${feed.title}")
                withId
            } else {
                repository.updateNewsRssFeed(feed)
                repository.logAdminAction(_session.value.username, "UPDATE_NEWS_RSS", "Updated News RSS Feed: ${feed.title}")
                feed
            }
            val cloudOk = firebaseManager.publishNewsRssFeedToCloud(entityToPublish)
            reportCloudAction("News RSS (${feed.title})", cloudOk)
            repository.refreshNewsRssArticles(forceRefresh = true)
        }
    }

    fun deleteNewsRssFeed(feed: com.example.database.entities.NewsRssFeedEntity) {
        viewModelScope.launch {
            repository.deleteNewsRssFeed(feed)
            val cloudOk = firebaseManager.deleteNewsRssFeedFromCloud(feed)
            reportCloudAction("News RSS डिलीट (${feed.title})", cloudOk)
            repository.logAdminAction(_session.value.username, "DELETE_NEWS_RSS", "Deleted News RSS Feed: ${feed.title}")
        }
    }

    fun toggleNewsRssFeedVisibility(feed: com.example.database.entities.NewsRssFeedEntity) {
        viewModelScope.launch {
            val updated = feed.copy(isVisible = !feed.isVisible)
            repository.updateNewsRssFeed(updated)
            val cloudOk = firebaseManager.publishNewsRssFeedToCloud(updated)
            reportCloudAction("News RSS दृश्यता (${feed.title})", cloudOk)
            repository.refreshNewsRssArticles(forceRefresh = true)
        }
    }

    fun refreshNewsRssFeeds(onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val count = repository.refreshNewsRssArticles(forceRefresh = true)
            if (count > 0) {
                firebaseManager.broadcastAdminUpload(
                    title = "ताज़ा शिक्षा समाचार",
                    section = "लाइव समाचार बुलेटिन",
                    details = "$count नए समाचार सिंक हुए",
                    contentType = "news",
                    contentId = "",
                    targetSection = "news",
                    targetUrl = ""
                )
            }
            onComplete(count)
        }
    }
}
