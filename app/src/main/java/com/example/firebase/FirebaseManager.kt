package com.example.firebase

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.R
import com.example.core.SoundNotificationHelper
import com.example.database.AppRepository
import com.example.database.entities.ComplaintOptionEntity
import com.example.database.entities.DownloadResourceEntity
import com.example.database.entities.EducationActivityEntity
import com.example.database.entities.FeatureBoxEntity
import com.example.database.entities.FinancialBannerEntity
import com.example.database.entities.FinancialServiceEntity
import com.example.database.entities.HolidayEntity
import com.example.database.entities.HomeTopLinkEntity
import com.example.database.entities.NewsArticleEntity
import com.example.database.entities.NewsRssFeedEntity
import com.example.database.entities.NoticeEntity
import com.example.database.entities.PromoBannerEntity
import com.example.database.entities.QuickPortalEntity
import com.example.database.entities.SchoolTimingEntity
import com.example.database.entities.TopThreeLinksEntity
import com.example.database.entities.VideoTutorialEntity
import com.example.database.entities.WhatsAppBannerEntity
import com.example.database.entities.WhatsAppLinkConfigEntity
import com.example.database.entities.YouTubeFeedEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class AdminLoudAlert(
    val alertId: String = "",
    val title: String = "",
    val section: String = "",
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

class FirebaseManager private constructor(private val context: Context) {

    private val TAG = "FirebaseManager"
    private val PREFS_NAME = "shikshak_sathi_cloud_prefs"
    private val sharedPrefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    val firestore: FirebaseFirestore by lazy {
        try {
            FirebaseApp.initializeApp(context)
        } catch (e: Throwable) {
            Log.w(TAG, "FirebaseApp.initializeApp exception", e)
        }
        val databaseId = try {
            context.getString(R.string.firestore_database_id)
        } catch (_: Throwable) {
            ""
        }
        val db = if (databaseId.isNotBlank() && databaseId != "DEFAULT") {
            val app = try {
                FirebaseApp.getInstance()
            } catch (_: Throwable) {
                FirebaseApp.initializeApp(context)
                    ?: throw IllegalStateException("FirebaseApp initialization failed for database $databaseId")
            }
            FirebaseFirestore.getInstance(app, databaseId)
        } else {
            FirebaseFirestore.getInstance()
        }
        try {
            db.firestoreSettings = firestoreSettings {
                setLocalCacheSettings(
                    persistentCacheSettings {
                        setSizeBytes(100L * 1024L * 1024L)
                    }
                )
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Firestore settings already initialized", e)
        }
        db
    }

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isCloudConnected = MutableStateFlow(isNetworkCurrentlyConnected())
    val isCloudConnected: StateFlow<Boolean> = _isCloudConnected.asStateFlow()

    private val _activeLoudAlert = MutableStateFlow<AdminLoudAlert?>(null)
    val activeLoudAlert: StateFlow<AdminLoudAlert?> = _activeLoudAlert.asStateFlow()

    // Cached 10 latest broadcast records for Admin Panel
    private val _broadcastHistory = MutableStateFlow<List<AdminLoudAlert>>(emptyList())
    val broadcastHistory: StateFlow<List<AdminLoudAlert>> = _broadcastHistory.asStateFlow()

    val currentSalaryRules = MutableStateFlow(Triple(53.0, 4.0, 8.0))

    @Volatile
    private var isCloudSyncStarted = false

    @Volatile
    private var versionListenerRegistration: ListenerRegistration? = null

    @Volatile
    private var currentBroadcastSeq: Long = 0L

    @Volatile
    private var lastManualRefreshMs: Long = 0L

    private val syncMutex = Mutex()
    private val listenerLock = Any()

    init {
        setupNetworkMonitoring()
        SoundNotificationHelper.initializeChannel(context)
    }

    fun dismissLoudAlert() {
        SoundNotificationHelper.stopLoudAlarm()
        _activeLoudAlert.value = null
    }

    fun isNetworkCurrentlyConnected(): Boolean {
        return try {
            val cm = connectivityManager ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    private fun setupNetworkMonitoring() {
        try {
            _isCloudConnected.value = isNetworkCurrentlyConnected()
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isCloudConnected.value = isNetworkCurrentlyConnected()
                }

                override fun onLost(network: Network) {
                    _isCloudConnected.value = false
                }

                override fun onUnavailable() {
                    _isCloudConnected.value = false
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    _isCloudConnected.value = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "NetworkCallback registration exception", e)
            _isCloudConnected.value = isNetworkCurrentlyConnected()
        }
    }

    /**
     * Single-Listener Smart Sync (Version Gatekeeper):
     * - Guarded against duplicate registration across ViewModels, refreshes, and Activity recreations.
     * - Zero startup writes (no heartbeat ping, no user-side collection bootstrap).
     * - Attaches ONLY 1 lightweight MetadataChanges.EXCLUDE listener on app_meta/sync_versions.
     * - Downloads ONLY collections whose version incremented on server.
     */
    fun startCloudSync(repository: AppRepository, scope: CoroutineScope) {
        synchronized(listenerLock) {
            if (isCloudSyncStarted && versionListenerRegistration != null) {
                return
            }
            isCloudSyncStarted = true
            versionListenerRegistration?.remove()
            try {
                versionListenerRegistration = firestore
                    .collection(CloudSyncEngine.META_COLLECTION)
                    .document(CloudSyncEngine.SYNC_VERSIONS_DOC)
                    .addSnapshotListener(MetadataChanges.EXCLUDE) { doc, e ->
                        if (e != null) {
                            FirestoreErrorHandler.handleFirestoreError(
                                e,
                                OperationType.GET,
                                "${CloudSyncEngine.META_COLLECTION}/${CloudSyncEngine.SYNC_VERSIONS_DOC}"
                            )
                            if (!isNetworkCurrentlyConnected()) _isCloudConnected.value = false
                            return@addSnapshotListener
                        }
                        scope.launch(Dispatchers.IO) {
                            syncMutex.withLock {
                                CloudSyncEngine.handleSyncVersionsSnapshot(
                                    context = context,
                                    firestore = firestore,
                                    doc = doc,
                                    repository = repository,
                                    sharedPrefs = sharedPrefs,
                                    currentSalaryRules = currentSalaryRules,
                                    onBroadcastSeqObserved = { seq -> currentBroadcastSeq = seq },
                                    onConnected = { _isCloudConnected.value = true }
                                )
                            }
                        }
                    }
            } catch (t: Throwable) {
                isCloudSyncStarted = false
                Log.w(TAG, "Failed to attach single sync_versions listener", t)
            }
        }
    }

    /**
     * Manual refresh (1 read of sync_versions with 15s cooldown — never re-attaches listeners).
     */
    suspend fun refreshCloudSyncNow(repository: AppRepository, force: Boolean = false) {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            if (!force && now - lastManualRefreshMs < 15_000L) {
                return@withContext
            }
            lastManualRefreshMs = now
            syncChangedCollectionsOnce(repository)
        }
    }

    /**
     * One-shot version check & selective sync (used by manual refresh and BootSyncReceiver).
     */
    suspend fun syncChangedCollectionsOnce(repository: AppRepository) {
        withContext(Dispatchers.IO) {
            try {
                val docRef = firestore
                    .collection(CloudSyncEngine.META_COLLECTION)
                    .document(CloudSyncEngine.SYNC_VERSIONS_DOC)
                val doc = try {
                    docRef.get(Source.SERVER).await()
                } catch (_: Exception) {
                    docRef.get(Source.DEFAULT).await()
                }
                syncMutex.withLock {
                    CloudSyncEngine.handleSyncVersionsSnapshot(
                        context = context,
                        firestore = firestore,
                        doc = doc,
                        repository = repository,
                        sharedPrefs = sharedPrefs,
                        currentSalaryRules = currentSalaryRules,
                        onBroadcastSeqObserved = { seq -> currentBroadcastSeq = seq },
                        onConnected = { _isCloudConnected.value = true }
                    )
                }
            } catch (t: Throwable) {
                if (t is FirebaseFirestoreException) {
                    FirestoreErrorHandler.handleFirestoreError(
                        t,
                        OperationType.GET,
                        "${CloudSyncEngine.META_COLLECTION}/${CloudSyncEngine.SYNC_VERSIONS_DOC}"
                    )
                } else {
                    Log.w(TAG, "syncChangedCollectionsOnce failed", t)
                }
                if (!isNetworkCurrentlyConnected()) _isCloudConnected.value = false
            }
        }
    }

    /**
     * Fetches ONLY the 10 latest broadcast history records for Admin Panel.
     */
    suspend fun fetchLatestBroadcastHistory(forceServer: Boolean = false): List<AdminLoudAlert> {
        return withContext(Dispatchers.IO) {
            val source = if (forceServer) Source.SERVER else Source.DEFAULT
            val list = CloudSyncEngine.fetchLatestBroadcastHistory(firestore, source)
            if (list.isNotEmpty() || forceServer) {
                _broadcastHistory.value = list
            }
            list
        }
    }

    suspend fun bootstrapCloudCollectionsIfEmpty(repository: AppRepository) {
        AdminCloudPublisher.bootstrapCloudCollectionsIfEmpty(firestore, repository)
    }

    suspend fun syncAllLocalDataToCloud(repository: AppRepository, forceFull: Boolean = false): Boolean {
        if (!isNetworkCurrentlyConnected()) {
            _isCloudConnected.value = false
            return false
        }
        val ok = AdminCloudPublisher.syncAllLocalDataToCloud(firestore, repository, sharedPrefs, forceFull)
        if (ok) _isCloudConnected.value = true
        return ok
    }

    suspend fun broadcastAdminUpload(title: String, section: String, details: String): Boolean =
        broadcastAdminUpload(
            title = title,
            section = section,
            details = details,
            contentType = "",
            contentId = "",
            targetSection = "",
            targetUrl = ""
        )

    suspend fun broadcastAdminUpload(
        title: String,
        section: String,
        details: String,
        contentType: String,
        contentId: String,
        targetSection: String,
        targetUrl: String
    ): Boolean {
        val ok = AdminCloudPublisher.broadcastAdminUpload(
            context = context,
            firestore = firestore,
            sharedPrefs = sharedPrefs,
            knownBroadcastSeq = currentBroadcastSeq,
            onSeqUpdated = { newSeq -> currentBroadcastSeq = newSeq },
            onHistoryPrepended = { item ->
                _broadcastHistory.value = (listOf(item) + _broadcastHistory.value)
                    .distinctBy { it.alertId }
                    .sortedByDescending { it.timestamp }
                    .take(10)
            },
            title = title,
            section = section,
            details = details,
            contentType = contentType,
            contentId = contentId,
            targetSection = targetSection,
            targetUrl = targetUrl
        )
        if (ok) _isCloudConnected.value = isNetworkCurrentlyConnected()
        return ok
    }

    // --- Notice Cloud Operations ---
    suspend fun publishNoticeToCloud(notice: NoticeEntity, isNew: Boolean = false, sendNotification: Boolean = false): Boolean {
        val ok = AdminCloudPublisher.publishNoticeToCloud(
            firestore = firestore,
            notice = notice,
            notifyFn = ::broadcastAdminUpload,
            sendNotification = sendNotification,
            notifyTypedFn = { t, s, d, ct, cid, ts, tu -> broadcastAdminUpload(t, s, d, ct, cid, ts, tu) }
        )
        if (ok) _isCloudConnected.value = isNetworkCurrentlyConnected()
        return ok
    }

    suspend fun updateNoticeInCloud(notice: NoticeEntity, sendNotification: Boolean = false): Boolean =
        publishNoticeToCloud(notice, isNew = false, sendNotification = sendNotification)

    suspend fun deleteNoticeFromCloud(notice: NoticeEntity): Boolean =
        AdminCloudPublisher.deleteNoticeFromCloud(firestore, notice)

    // --- Holiday Cloud Operations ---
    suspend fun publishHolidayToCloud(holiday: HolidayEntity, isNew: Boolean = false, sendNotification: Boolean = false): Boolean {
        val ok = AdminCloudPublisher.publishHolidayToCloud(
            firestore = firestore,
            holiday = holiday,
            notifyFn = ::broadcastAdminUpload,
            sendNotification = sendNotification,
            notifyTypedFn = { t, s, d, ct, cid, ts, tu -> broadcastAdminUpload(t, s, d, ct, cid, ts, tu) }
        )
        if (ok) _isCloudConnected.value = isNetworkCurrentlyConnected()
        return ok
    }

    suspend fun deleteHolidayFromCloud(holiday: HolidayEntity): Boolean =
        AdminCloudPublisher.deleteHolidayFromCloud(firestore, holiday)

    // --- Activities Cloud Operations ---
    suspend fun publishActivityToCloud(activity: EducationActivityEntity, isNew: Boolean = false, sendNotification: Boolean = false): Boolean {
        val ok = AdminCloudPublisher.publishActivityToCloud(firestore, activity, ::broadcastAdminUpload, sendNotification)
        if (ok) _isCloudConnected.value = isNetworkCurrentlyConnected()
        return ok
    }

    suspend fun deleteActivityFromCloud(activity: EducationActivityEntity): Boolean =
        AdminCloudPublisher.deleteActivityFromCloud(firestore, activity)

    // --- Quick Portals Cloud Operations ---
    suspend fun publishPortalToCloud(portal: QuickPortalEntity, isNew: Boolean = false, sendNotification: Boolean = false): Boolean {
        val ok = AdminCloudPublisher.publishPortalToCloud(firestore, portal, ::broadcastAdminUpload, sendNotification)
        if (ok) _isCloudConnected.value = isNetworkCurrentlyConnected()
        return ok
    }

    suspend fun deletePortalFromCloud(portal: QuickPortalEntity): Boolean =
        AdminCloudPublisher.deletePortalFromCloud(firestore, portal)

    // --- Salary Rules ---
    suspend fun publishSalaryRuleToCloud(daPercent: Double, ruralHra: Double, urbanHra: Double, isNew: Boolean = false, sendNotification: Boolean = false): Boolean {
        val ok = AdminCloudPublisher.publishSalaryRuleToCloud(firestore, daPercent, ruralHra, urbanHra, ::broadcastAdminUpload, sendNotification)
        if (ok) _isCloudConnected.value = isNetworkCurrentlyConnected()
        return ok
    }

    // --- Promo Banners ---
    suspend fun publishBannerToCloud(banner: PromoBannerEntity, isNew: Boolean = false, sendNotification: Boolean = false): Boolean =
        AdminCloudPublisher.publishBannerToCloud(firestore, banner, ::broadcastAdminUpload, sendNotification)

    suspend fun deleteBannerFromCloud(bannerId: String, title: String = ""): Boolean =
        AdminCloudPublisher.deleteBannerFromCloud(firestore, bannerId, title)

    // --- School Timings & WhatsApp Config ---
    suspend fun publishSchoolTimingToCloud(timing: SchoolTimingEntity, isNew: Boolean = false, sendNotification: Boolean = false): Boolean =
        AdminCloudPublisher.publishSchoolTimingToCloud(firestore, timing, ::broadcastAdminUpload, sendNotification)

    suspend fun publishWhatsAppConfigToCloud(config: WhatsAppLinkConfigEntity, isNew: Boolean = false, sendNotification: Boolean = false): Boolean =
        AdminCloudPublisher.publishWhatsAppConfigToCloud(firestore, config, ::broadcastAdminUpload, sendNotification)

    // --- Video Tutorials ---
    suspend fun publishVideoToCloud(video: VideoTutorialEntity, isNew: Boolean = false, sendNotification: Boolean = false): Boolean =
        AdminCloudPublisher.publishVideoToCloud(
            firestore = firestore,
            video = video,
            notifyFn = ::broadcastAdminUpload,
            sendNotification = sendNotification,
            notifyTypedFn = { t, s, d, ct, cid, ts, tu -> broadcastAdminUpload(t, s, d, ct, cid, ts, tu) }
        )

    suspend fun deleteVideoFromCloud(video: VideoTutorialEntity): Boolean =
        AdminCloudPublisher.deleteVideoFromCloud(firestore, video)

    // --- Downloads & Google Drive ---
    suspend fun publishDownloadToCloud(download: DownloadResourceEntity, isNew: Boolean = false, sendNotification: Boolean = false): Boolean =
        AdminCloudPublisher.publishDownloadToCloud(
            firestore = firestore,
            download = download,
            notifyFn = ::broadcastAdminUpload,
            sendNotification = sendNotification,
            notifyTypedFn = { t, s, d, ct, cid, ts, tu -> broadcastAdminUpload(t, s, d, ct, cid, ts, tu) }
        )

    suspend fun deleteDownloadFromCloud(download: DownloadResourceEntity): Boolean =
        AdminCloudPublisher.deleteDownloadFromCloud(firestore, download)

    suspend fun publishGoogleDriveConfigToCloud(isEnabled: Boolean, folderUrl: String? = null, isNew: Boolean = false, sendNotification: Boolean = false): Boolean =
        AdminCloudPublisher.publishGoogleDriveConfigToCloud(firestore, isEnabled, folderUrl, ::broadcastAdminUpload, sendNotification)

    // --- News Articles ---
    suspend fun publishNewsToCloud(news: NewsArticleEntity, isNew: Boolean = false, sendNotification: Boolean = false): Boolean =
        AdminCloudPublisher.publishNewsToCloud(
            firestore = firestore,
            news = news,
            notifyFn = ::broadcastAdminUpload,
            sendNotification = sendNotification,
            notifyTypedFn = { t, s, d, ct, cid, ts, tu -> broadcastAdminUpload(t, s, d, ct, cid, ts, tu) }
        )

    suspend fun deleteNewsFromCloud(news: NewsArticleEntity): Boolean =
        AdminCloudPublisher.deleteNewsFromCloud(firestore, news)

    // --- Feature Boxes & App Config Cards ---
    suspend fun publishFeatureBoxesToCloud(boxes: List<FeatureBoxEntity>, isNew: Boolean = false, sendNotification: Boolean = false): Boolean =
        AdminCloudPublisher.publishFeatureBoxesToCloud(firestore, boxes, ::broadcastAdminUpload, sendNotification)

    suspend fun deleteFeatureBoxFromCloud(box: FeatureBoxEntity): Boolean =
        AdminCloudPublisher.deleteFeatureBoxFromCloud(firestore, box)

    suspend fun publishTopThreeLinksToCloud(links: TopThreeLinksEntity, sendNotification: Boolean = false): Boolean =
        AdminCloudPublisher.publishTopThreeLinksToCloud(firestore, links, ::broadcastAdminUpload, sendNotification)

    suspend fun publishFinancialBannerToCloud(banner: FinancialBannerEntity, sendNotification: Boolean = false): Boolean =
        AdminCloudPublisher.publishFinancialBannerToCloud(firestore, banner, ::broadcastAdminUpload, sendNotification)

    suspend fun publishWhatsAppBannerToCloud(banner: WhatsAppBannerEntity, sendNotification: Boolean = false): Boolean =
        AdminCloudPublisher.publishWhatsAppBannerToCloud(firestore, banner, ::broadcastAdminUpload, sendNotification)

    // --- Home Top Links / Complaints / Financial Services / Feeds ---
    suspend fun publishHomeTopLinkToCloud(link: HomeTopLinkEntity, isNew: Boolean = false, sendNotification: Boolean = false): Boolean =
        AdminCloudPublisher.publishHomeTopLinkToCloud(firestore, link, ::broadcastAdminUpload, sendNotification)

    suspend fun deleteHomeTopLinkFromCloud(link: HomeTopLinkEntity): Boolean =
        AdminCloudPublisher.deleteHomeTopLinkFromCloud(firestore, link)

    suspend fun publishComplaintOptionToCloud(option: ComplaintOptionEntity, isNew: Boolean = false, sendNotification: Boolean = false): Boolean =
        AdminCloudPublisher.publishComplaintOptionToCloud(firestore, option, ::broadcastAdminUpload, sendNotification)

    suspend fun deleteComplaintOptionFromCloud(option: ComplaintOptionEntity): Boolean =
        AdminCloudPublisher.deleteComplaintOptionFromCloud(firestore, option)

    suspend fun publishFinancialServiceToCloud(service: FinancialServiceEntity, isNew: Boolean = false, sendNotification: Boolean = false): Boolean =
        AdminCloudPublisher.publishFinancialServiceToCloud(firestore, service, ::broadcastAdminUpload, sendNotification)

    suspend fun deleteFinancialServiceFromCloud(service: FinancialServiceEntity): Boolean =
        AdminCloudPublisher.deleteFinancialServiceFromCloud(firestore, service)

    suspend fun publishYouTubeFeedToCloud(feed: YouTubeFeedEntity, isNew: Boolean = false): Boolean =
        AdminCloudPublisher.publishYouTubeFeedToCloud(firestore, feed, ::broadcastAdminUpload, isNew)

    suspend fun deleteYouTubeFeedFromCloud(feed: YouTubeFeedEntity): Boolean =
        AdminCloudPublisher.deleteYouTubeFeedFromCloud(firestore, feed)

    suspend fun publishNewsRssFeedToCloud(feed: NewsRssFeedEntity, isNew: Boolean = false): Boolean =
        AdminCloudPublisher.publishNewsRssFeedToCloud(firestore, feed, ::broadcastAdminUpload, isNew)

    suspend fun deleteNewsRssFeedFromCloud(feed: NewsRssFeedEntity): Boolean =
        AdminCloudPublisher.deleteNewsRssFeedFromCloud(firestore, feed)

    // --- Admin Audit Log Cloud Sync ---
    suspend fun logAdminActionToCloud(username: String, action: String, details: String): Boolean {
        val logId = "log_${System.currentTimeMillis()}"
        return try {
            val logData = hashMapOf<String, Any>(
                "username" to username,
                "action" to action,
                "details" to details,
                "timestamp" to FieldValue.serverTimestamp()
            )
            firestore.collection("admin_audit_logs").document(logId).set(logData).await()
            true
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.CREATE, "admin_audit_logs/$logId")
            false
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: FirebaseManager? = null

        fun getInstance(context: Context): FirebaseManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
