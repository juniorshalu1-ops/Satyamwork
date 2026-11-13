package com.example.firebase

import android.content.SharedPreferences
import android.util.Log
import com.example.core.SoundNotificationHelper
import com.example.core.YouTubeUrlHelper
import com.example.database.AppRepository
import com.example.database.entities.ComplaintOptionEntity
import com.example.database.entities.DEFAULT_FINANCIAL_BANNER
import com.example.database.entities.DEFAULT_GOOGLE_DRIVE_CONFIG
import com.example.database.entities.DEFAULT_MAIN_DRIVE_FOLDER_URL
import com.example.database.entities.DEFAULT_TOP_THREE_LINKS
import com.example.database.entities.DEFAULT_WHATSAPP_BANNER
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
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
import android.content.Context

internal enum class CloudSection(val versionKey: String) {
    NOTICES("notices_v"),
    HOLIDAYS("holidays_v"),
    ACTIVITIES("activities_v"),
    PORTALS("portals_v"),
    BANNERS("banners_v"),
    SCHOOL_TIMING("school_timing_v"),
    WHATSAPP_CONFIG("whatsapp_config_v"),
    VIDEOS("videos_v"),
    DOWNLOADS("downloads_v"),
    GOOGLE_DRIVE("google_drive_v"),
    NEWS("news_v"),
    FEATURE_BOXES("feature_boxes_v"),
    TOP_THREE_LINKS("top_three_links_v"),
    FINANCIAL_BANNER("financial_banner_v"),
    WHATSAPP_BANNER("whatsapp_banner_v"),
    SALARY_RULES("salary_rules_v"),
    HOME_TOP_LINKS("home_top_links_v"),
    COMPLAINT_OPTIONS("complaint_options_v"),
    FINANCIAL_SERVICES("financial_services_v"),
    YOUTUBE_FEEDS("youtube_feeds_v"),
    NEWS_RSS_FEEDS("news_rss_feeds_v");

    companion object {
        const val ALERT_VERSION_KEY = "alert_v"
        const val BROADCAST_SEQ_KEY = "broadcastSeq"
    }
}

internal object CloudSyncEngine {

    private const val TAG = "CloudSyncEngine"
    const val META_COLLECTION = "app_meta"
    const val SYNC_VERSIONS_DOC = "sync_versions"
    const val KEY_LAST_ALERT_ID = "key_last_alert_id"
    private const val KEY_LEGACY_INITIAL_SYNC_DONE = "key_legacy_initial_sync_done_v1"
    private const val PREF_VER_PREFIX = "ver_"

    suspend fun handleSyncVersionsSnapshot(
        context: Context,
        firestore: FirebaseFirestore,
        doc: DocumentSnapshot?,
        repository: AppRepository,
        sharedPrefs: SharedPreferences,
        currentSalaryRules: MutableStateFlow<Triple<Double, Double, Double>>,
        onBroadcastSeqObserved: (Long) -> Unit,
        onConnected: () -> Unit
    ) {
        if (doc != null && !doc.metadata.isFromCache) {
            onConnected()
        }

        val legacyDone = sharedPrefs.getBoolean(KEY_LEGACY_INITIAL_SYNC_DONE, false)

        if (doc == null || !doc.exists()) {
            // Backward-compatible transition: if app_meta/sync_versions is not yet created in Firestore,
            // run a single one-time .get() sync on fresh install so existing data still loads without
            // keeping 21 persistent real-time listeners open.
            if (!legacyDone) {
                Log.d(TAG, "sync_versions missing; running one-time legacy initial fetch")
                var allSucceeded = true
                for (section in CloudSection.entries) {
                    val ok = fetchAndApplySection(firestore, section, repository, Source.DEFAULT, currentSalaryRules)
                    if (!ok) allSucceeded = false
                }
                checkAndHandleLatestAlert(context, firestore, sharedPrefs, Source.DEFAULT)
                if (allSucceeded) {
                    sharedPrefs.edit().putBoolean(KEY_LEGACY_INITIAL_SYNC_DONE, true).apply()
                }
            }
            return
        }

        val seq = doc.getLong(CloudSection.BROADCAST_SEQ_KEY)
        if (seq != null && seq > 0L) {
            onBroadcastSeqObserved(seq)
        }

        // 1. Check Admin Broadcast Alert Version
        val serverAlertVer = doc.getLong(CloudSection.ALERT_VERSION_KEY) ?: 0L
        val localAlertVer = sharedPrefs.getLong(PREF_VER_PREFIX + CloudSection.ALERT_VERSION_KEY, -1L)
        if (!sharedPrefs.contains(KEY_LAST_ALERT_ID) || (serverAlertVer > 0L && serverAlertVer > localAlertVer)) {
            val source = if (serverAlertVer > localAlertVer && localAlertVer >= 0L) Source.SERVER else Source.DEFAULT
            val alertOk = checkAndHandleLatestAlert(context, firestore, sharedPrefs, source)
            if (alertOk) {
                sharedPrefs.edit().putLong(PREF_VER_PREFIX + CloudSection.ALERT_VERSION_KEY, serverAlertVer).apply()
            }
        }

        // 2. Check each section selectively — download ONLY sections whose version bumped
        val editor = sharedPrefs.edit()
        var syncedCount = 0
        for (section in CloudSection.entries) {
            val serverVer = doc.getLong(section.versionKey) ?: 0L
            val localVer = sharedPrefs.getLong(PREF_VER_PREFIX + section.versionKey, -1L)

            val shouldFetchUpdated = serverVer > 0L && serverVer > localVer
            val shouldFetchLegacyOnce = !legacyDone && localVer == -1L && serverVer == 0L

            if (shouldFetchUpdated || shouldFetchLegacyOnce) {
                val source = if (shouldFetchUpdated && localVer >= 0L) Source.SERVER else Source.DEFAULT
                val ok = fetchAndApplySection(
                    firestore = firestore,
                    section = section,
                    repository = repository,
                    preferredSource = source,
                    currentSalaryRules = currentSalaryRules,
                    allowEmptySync = serverVer > 0L
                )
                if (ok) {
                    editor.putLong(PREF_VER_PREFIX + section.versionKey, serverVer)
                    syncedCount++
                }
            }
        }
        if (!legacyDone) {
            editor.putBoolean(KEY_LEGACY_INITIAL_SYNC_DONE, true)
        }
        editor.apply()
        if (syncedCount > 0) {
            Log.d(TAG, "Smart Sync updated $syncedCount changed sections cleanly.")
        }
    }

    private suspend fun checkAndHandleLatestAlert(
        context: Context,
        firestore: FirebaseFirestore,
        sharedPrefs: SharedPreferences,
        preferredSource: Source
    ): Boolean {
        return try {
            val ref = firestore.collection("admin_broadcasts").document("latest_alert")
            val snapshot = getDocumentWithFallback(ref, preferredSource)
            if (snapshot != null && snapshot.exists()) {
                val alertId = snapshot.getString("alertId") ?: return true
                val title = snapshot.getString("title") ?: "नया प्रशासनिक अपडेट"
                val section = snapshot.getString("section") ?: "सामान्य"
                val details = snapshot.getString("details") ?: ""
                val contentType = snapshot.getString("contentType") ?: ""
                val contentId = snapshot.getString("contentId") ?: ""
                val targetSection = snapshot.getString("targetSection") ?: ""
                val targetUrl = snapshot.getString("targetUrl") ?: ""

                // Newly installed users record existing alertId silently without old notifications
                if (!sharedPrefs.contains(KEY_LAST_ALERT_ID)) {
                    sharedPrefs.edit().putString(KEY_LAST_ALERT_ID, alertId).apply()
                    return true
                }

                val lastAlertId = sharedPrefs.getString(KEY_LAST_ALERT_ID, "")
                if (alertId != lastAlertId) {
                    sharedPrefs.edit().putString(KEY_LAST_ALERT_ID, alertId).apply()
                    SoundNotificationHelper.triggerSpecialAdminNotification(
                        context = context,
                        title = title,
                        section = section,
                        details = details,
                        targetSection = targetSection,
                        contentType = contentType,
                        contentId = contentId,
                        targetUrl = targetUrl
                    )
                }
            }
            true
        } catch (t: Throwable) {
            Log.w(TAG, "checkAndHandleLatestAlert error", t)
            false
        }
    }

    suspend fun fetchLatestBroadcastHistory(
        firestore: FirebaseFirestore,
        preferredSource: Source = Source.DEFAULT
    ): List<AdminLoudAlert> {
        return try {
            val query = firestore.collection("admin_broadcasts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(11)
            val snapshots = try {
                query.get(preferredSource).await()
            } catch (_: Exception) {
                query.get(Source.DEFAULT).await()
            }
            snapshots.documents
                .filter { it.id != "latest_alert" }
                .take(10)
                .mapNotNull { doc ->
                    val title = doc.getString("title") ?: return@mapNotNull null
                    AdminLoudAlert(
                        alertId = doc.getString("alertId") ?: doc.id,
                        title = title,
                        section = doc.getString("section") ?: "सामान्य",
                        details = doc.getString("details") ?: "",
                        timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: System.currentTimeMillis()
                    )
                }
        } catch (t: Throwable) {
            Log.w(TAG, "fetchLatestBroadcastHistory error", t)
            emptyList()
        }
    }

    suspend fun fetchAndApplySection(
        firestore: FirebaseFirestore,
        section: CloudSection,
        repository: AppRepository,
        preferredSource: Source,
        currentSalaryRules: MutableStateFlow<Triple<Double, Double, Double>>,
        allowEmptySync: Boolean = false
    ): Boolean {
        return try {
            when (section) {
                CloudSection.NOTICES -> {
                    val snap = getQueryWithFallback(firestore.collection("notices"), preferredSource)
                    val list = snap.documents.mapIndexedNotNull { idx, doc ->
                        val title = doc.getString("title") ?: return@mapIndexedNotNull null
                        NoticeEntity(
                            id = (idx + 1).toLong(),
                            title = title,
                            summary = doc.getString("summary") ?: "",
                            category = doc.getString("category") ?: "सरकारी आदेश",
                            issueDate = doc.getString("issueDate") ?: "",
                            department = doc.getString("department") ?: "शिक्षा विभाग, बिहार सरकार",
                            orderNumber = doc.getString("orderNumber") ?: "",
                            officialUrl = doc.getString("officialUrl") ?: "https://state.bihar.gov.in/educationbihar",
                            pdfUrl = doc.getString("pdfUrl") ?: "",
                            imageUrl = doc.getString("imageUrl") ?: "",
                            isFeatured = doc.getBoolean("isFeatured") ?: false
                        )
                    }
                    repository.syncNoticesFromCloud(list)
                }

                CloudSection.HOLIDAYS -> {
                    val snap = getQueryWithFallback(firestore.collection("holiday_calendars"), preferredSource)
                    val list = snap.documents.mapIndexedNotNull { idx, doc ->
                        val name = doc.getString("name") ?: return@mapIndexedNotNull null
                        HolidayEntity(
                            id = (idx + 1).toLong(),
                            name = name,
                            dateStr = doc.getString("dateStr") ?: "",
                            dayOfWeek = doc.getString("dayOfWeek") ?: "",
                            month = doc.getLong("month")?.toInt() ?: 1,
                            type = doc.getString("type") ?: "त्योहार",
                            durationDays = doc.getLong("durationDays")?.toInt() ?: 1,
                            description = doc.getString("description") ?: ""
                        )
                    }
                    repository.syncHolidaysFromCloud(list)
                }

                CloudSection.ACTIVITIES -> {
                    val snap = getQueryWithFallback(firestore.collection("activities"), preferredSource)
                    val list = snap.documents.mapIndexedNotNull { idx, doc ->
                        val title = doc.getString("title") ?: return@mapIndexedNotNull null
                        EducationActivityEntity(
                            id = (idx + 1).toLong(),
                            title = title,
                            description = doc.getString("description") ?: "",
                            category = doc.getString("category") ?: "अभियान",
                            linkUrl = doc.getString("linkUrl") ?: "https://state.bihar.gov.in/educationbihar",
                            orderNumber = doc.getString("orderNumber") ?: ""
                        )
                    }
                    repository.syncActivitiesFromCloud(list)
                }

                CloudSection.PORTALS -> {
                    val snap = getQueryWithFallback(firestore.collection("quick_portals"), preferredSource)
                    val list = snap.documents.mapIndexedNotNull { idx, doc ->
                        val title = doc.getString("title") ?: return@mapIndexedNotNull null
                        QuickPortalEntity(
                            id = (idx + 1).toLong(),
                            title = title,
                            subtitle = doc.getString("subtitle") ?: "",
                            url = doc.getString("url") ?: "",
                            badge = doc.getString("badge") ?: "पोर्टल",
                            colorHex = doc.getString("colorHex") ?: "#0D47A1"
                        )
                    }
                    repository.syncPortalsFromCloud(list)
                }

                CloudSection.BANNERS -> {
                    val snap = getQueryWithFallback(firestore.collection("promo_banners"), preferredSource)
                    val list = snap.documents.mapIndexedNotNull { idx, doc ->
                        val title = doc.getString("title") ?: return@mapIndexedNotNull null
                        PromoBannerEntity(
                            id = (idx + 1).toLong(),
                            bannerId = doc.getString("bannerId") ?: "banner_${idx + 1}",
                            title = title,
                            description = doc.getString("description") ?: "",
                            category = doc.getString("category") ?: "प्रमोशनल",
                            imageUrl = doc.getString("imageUrl") ?: "",
                            destinationUrl = doc.getString("destinationUrl") ?: "",
                            buttonText = doc.getString("buttonText") ?: "आवेदन करें",
                            orderIndex = doc.getLong("orderIndex")?.toInt() ?: (idx + 1),
                            isActive = doc.getBoolean("isActive") ?: true
                        )
                    }
                    if (list.isNotEmpty() || allowEmptySync) {
                        repository.syncBannersFromCloud(list)
                    }
                }

                CloudSection.SCHOOL_TIMING -> {
                    val doc = getDocumentWithFallback(firestore.collection("school_config").document("timings"), preferredSource)
                    if (doc != null && doc.exists()) {
                        repository.saveSchoolTiming(
                            SchoolTimingEntity(
                                id = 1,
                                openingTime = doc.getString("openingTime") ?: "09:00 AM",
                                closingTime = doc.getString("closingTime") ?: "04:30 PM",
                                isManuallyClosed = doc.getBoolean("isManuallyClosed") ?: false,
                                closureReason = doc.getString("closureReason") ?: ""
                            )
                        )
                    }
                }

                CloudSection.WHATSAPP_CONFIG -> {
                    val doc = getDocumentWithFallback(firestore.collection("whatsapp_config").document("main"), preferredSource)
                    if (doc != null && doc.exists()) {
                        repository.saveWhatsAppData(
                            WhatsAppLinkConfigEntity(
                                id = 1,
                                channelUrl = doc.getString("channelUrl") ?: "https://whatsapp.com/channel/0029Va4yZ1m2k345",
                                channelTitle = doc.getString("channelTitle") ?: "शिक्षा विभाग बिहार - शिक्षक साथी आधिकारिक चैनल",
                                districtGroupsUrl = doc.getString("districtGroupsUrl") ?: "https://state.bihar.gov.in/educationbihar",
                                districtGroupsTitle = doc.getString("districtGroupsTitle") ?: "बिहार के 38 जिलों एवं प्रखंडों के शिक्षक व्हाट्सएप ग्रुप"
                            )
                        )
                    }
                }

                CloudSection.VIDEOS -> {
                    val query = firestore.collection("video_tutorials")
                        .orderBy("publishedAt", Query.Direction.DESCENDING)
                        .limit(50)
                    val snap = getQueryWithFallback(query, preferredSource)
                    val list = snap.documents.mapIndexedNotNull { idx, doc ->
                        val title = doc.getString("title") ?: return@mapIndexedNotNull null
                        val yId = doc.getString("youtubeId") ?: ""
                        val thumb = doc.getString("thumbnailUrl") ?: ""
                        VideoTutorialEntity(
                            id = doc.getLong("id") ?: 0L,
                            title = title,
                            category = doc.getString("category") ?: "सामान्य",
                            youtubeUrl = doc.getString("youtubeUrl") ?: "",
                            youtubeId = yId,
                            thumbnailUrl = if (thumb.isNotBlank()) thumb else YouTubeUrlHelper.getThumbnailUrl(yId),
                            duration = doc.getString("duration") ?: "10:00",
                            description = doc.getString("description") ?: "",
                            isPinned = doc.getBoolean("isPinned") ?: false,
                            isFeaturedOnHome = doc.getBoolean("isFeaturedOnHome") ?: true,
                            displayOrder = doc.getLong("displayOrder")?.toInt() ?: idx
                        )
                    }
                    if (list.isNotEmpty() || allowEmptySync) {
                        repository.syncVideosFromCloud(list)
                    }
                }

                CloudSection.DOWNLOADS -> {
                    val snap = getQueryWithFallback(firestore.collection("download_resources"), preferredSource)
                    val list = snap.documents.mapNotNull { doc ->
                        val title = doc.getString("title") ?: return@mapNotNull null
                        val cid = (doc.getString("cloudId") ?: "").ifBlank { doc.id }
                        DownloadResourceEntity(
                            id = 0L,
                            cloudId = cid,
                            title = title,
                            category = doc.getString("category") ?: "प्रपत्र",
                            fileType = doc.getString("fileType") ?: "PDF",
                            fileSize = doc.getString("fileSize") ?: "1.0 MB",
                            pdfUrl = doc.getString("pdfUrl") ?: "",
                            publicationDate = doc.getString("publicationDate") ?: "",
                            orderNumber = doc.getString("orderNumber") ?: "",
                            description = doc.getString("description") ?: "",
                            isVisible = doc.getBoolean("isVisible") ?: true
                        )
                    }
                    repository.syncDownloadsFromCloud(com.example.core.DownloadSyncHelper.deduplicate(list))
                }

                CloudSection.GOOGLE_DRIVE -> {
                    val doc = getDocumentWithFallback(firestore.collection("app_configs").document("google_drive"), preferredSource)
                    if (doc != null && doc.exists()) {
                        val enabled = doc.getBoolean("isEnabled") ?: true
                        val folder = doc.getString("folderUrl") ?: DEFAULT_MAIN_DRIVE_FOLDER_URL
                        val current = repository.getGoogleDriveConfigOnce() ?: DEFAULT_GOOGLE_DRIVE_CONFIG
                        repository.saveGoogleDriveConfig(current.copy(isEnabled = enabled, folderUrl = folder))
                    }
                }

                CloudSection.NEWS -> {
                    val query = firestore.collection("news_articles")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(50)
                    val snap = getQueryWithFallback(query, preferredSource)
                    val list = snap.documents.mapIndexedNotNull { idx, doc ->
                        val title = doc.getString("title") ?: return@mapIndexedNotNull null
                        NewsArticleEntity(
                            id = (idx + 1).toLong(),
                            title = title,
                            summary = doc.getString("summary") ?: "",
                            content = doc.getString("content") ?: "",
                            category = doc.getString("category") ?: "शिक्षा समाचार",
                            publishedAt = doc.getString("publishedAt") ?: "",
                            source = doc.getString("source") ?: "शिक्षा विभाग बिहार",
                            sourceUrl = doc.getString("sourceUrl") ?: "https://state.bihar.gov.in/educationbihar",
                            imageUrl = doc.getString("imageUrl") ?: "",
                            isPinned = doc.getBoolean("isPinned") ?: false,
                            pinOrder = doc.getLong("pinOrder")?.toInt() ?: idx,
                            isBreaking = doc.getBoolean("isBreaking") ?: false,
                            isVisible = doc.getBoolean("isVisible") ?: true
                        )
                    }
                    if (list.isNotEmpty() || allowEmptySync) {
                        repository.syncNewsFromCloud(list)
                    }
                }

                CloudSection.FEATURE_BOXES -> {
                    val snap = getQueryWithFallback(firestore.collection("feature_boxes"), preferredSource)
                    val list = snap.documents.mapNotNull { doc ->
                        val title = doc.getString("title") ?: return@mapNotNull null
                        FeatureBoxEntity(
                            id = doc.id,
                            title = title,
                            subtitle = doc.getString("subtitle") ?: "",
                            iconType = doc.getString("iconType") ?: "",
                            destinationRoute = doc.getString("destinationRoute") ?: "",
                            displayOrder = (doc.getLong("displayOrder") ?: 0L).toInt(),
                            isVisible = doc.getBoolean("isVisible") ?: true,
                            isExternal = doc.getBoolean("isExternal") ?: false,
                            externalUrl = doc.getString("externalUrl") ?: ""
                        )
                    }
                    if (list.isNotEmpty() || allowEmptySync) {
                        repository.syncFeatureBoxesFromCloud(list.sortedBy { it.displayOrder })
                    }
                }

                CloudSection.TOP_THREE_LINKS -> {
                    val doc = getDocumentWithFallback(firestore.collection("app_config").document("top_three_links"), preferredSource)
                    if (doc != null && doc.exists()) {
                        repository.saveTopThreeLinks(
                            TopThreeLinksEntity(
                                id = 1,
                                link1Title = doc.getString("link1Title") ?: DEFAULT_TOP_THREE_LINKS.link1Title,
                                link1Url = doc.getString("link1Url") ?: DEFAULT_TOP_THREE_LINKS.link1Url,
                                link1Visible = doc.getBoolean("link1Visible") ?: true,
                                link2Title = doc.getString("link2Title") ?: DEFAULT_TOP_THREE_LINKS.link2Title,
                                link2Url = doc.getString("link2Url") ?: DEFAULT_TOP_THREE_LINKS.link2Url,
                                link2Visible = doc.getBoolean("link2Visible") ?: true,
                                link3Title = doc.getString("link3Title") ?: DEFAULT_TOP_THREE_LINKS.link3Title,
                                link3Url = doc.getString("link3Url") ?: DEFAULT_TOP_THREE_LINKS.link3Url,
                                link3Visible = doc.getBoolean("link3Visible") ?: true
                            )
                        )
                    }
                }

                CloudSection.FINANCIAL_BANNER -> {
                    val doc = getDocumentWithFallback(firestore.collection("app_config").document("financial_banner"), preferredSource)
                    if (doc != null && doc.exists()) {
                        repository.saveFinancialBanner(
                            FinancialBannerEntity(
                                id = 1,
                                heading = doc.getString("heading") ?: DEFAULT_FINANCIAL_BANNER.heading,
                                supportingText = doc.getString("supportingText") ?: DEFAULT_FINANCIAL_BANNER.supportingText,
                                disclaimer = doc.getString("disclaimer") ?: DEFAULT_FINANCIAL_BANNER.disclaimer,
                                buttonText = doc.getString("buttonText") ?: DEFAULT_FINANCIAL_BANNER.buttonText,
                                destinationType = doc.getString("destinationType") ?: "IN_APP",
                                destinationUrl = doc.getString("destinationUrl") ?: "",
                                imageUrl = doc.getString("imageUrl") ?: "",
                                isVisible = doc.getBoolean("isVisible") ?: true
                            )
                        )
                    }
                }

                CloudSection.WHATSAPP_BANNER -> {
                    val doc = getDocumentWithFallback(firestore.collection("app_config").document("whatsapp_banner"), preferredSource)
                    if (doc != null && doc.exists()) {
                        repository.saveWhatsAppBanner(
                            WhatsAppBannerEntity(
                                id = 1,
                                bannerText = doc.getString("bannerText") ?: DEFAULT_WHATSAPP_BANNER.bannerText,
                                buttonText = doc.getString("buttonText") ?: DEFAULT_WHATSAPP_BANNER.buttonText,
                                channelUrl = doc.getString("channelUrl") ?: DEFAULT_WHATSAPP_BANNER.channelUrl,
                                imageUrl = doc.getString("imageUrl") ?: "",
                                isVisible = doc.getBoolean("isVisible") ?: true
                            )
                        )
                    }
                }

                CloudSection.SALARY_RULES -> {
                    val doc = getDocumentWithFallback(firestore.collection("salary_rules").document("current"), preferredSource)
                    if (doc != null && doc.exists()) {
                        val da = doc.getDouble("daPercent") ?: doc.getDouble("currentDa") ?: 53.0
                        val rural = doc.getDouble("ruralHra") ?: doc.getDouble("hraRural") ?: 4.0
                        val urban = doc.getDouble("urbanHra") ?: doc.getDouble("hraUrban") ?: 8.0
                        currentSalaryRules.value = Triple(da, rural, urban)
                        repository.updateGlobalSalaryRules(da, rural, urban)
                    }
                }

                CloudSection.HOME_TOP_LINKS -> {
                    val snap = getQueryWithFallback(firestore.collection("home_top_links"), preferredSource)
                    val list = snap.documents.mapIndexedNotNull { idx, doc ->
                        val title = doc.getString("title") ?: return@mapIndexedNotNull null
                        HomeTopLinkEntity(
                            id = doc.getLong("id") ?: (idx + 1).toLong(),
                            title = title,
                            url = doc.getString("url") ?: "",
                            isVisible = doc.getBoolean("isVisible") ?: true,
                            displayOrder = (doc.getLong("displayOrder") ?: (idx + 1).toLong()).toInt()
                        )
                    }
                    if (list.isNotEmpty() || allowEmptySync) {
                        repository.syncHomeTopLinksFromCloud(list.sortedBy { it.displayOrder })
                    }
                }

                CloudSection.COMPLAINT_OPTIONS -> {
                    val snap = getQueryWithFallback(firestore.collection("complaint_options"), preferredSource)
                    val list = snap.documents.mapIndexedNotNull { idx, doc ->
                        val title = doc.getString("title") ?: return@mapIndexedNotNull null
                        ComplaintOptionEntity(
                            id = doc.getLong("id") ?: (idx + 1).toLong(),
                            title = title,
                            description = doc.getString("description") ?: "",
                            phone = doc.getString("phone") ?: "",
                            email = doc.getString("email") ?: "",
                            linkUrl = doc.getString("portalUrl") ?: doc.getString("linkUrl") ?: "",
                            isVisible = doc.getBoolean("isVisible") ?: true,
                            displayOrder = (doc.getLong("displayOrder") ?: (idx + 1).toLong()).toInt()
                        )
                    }
                    if (list.isNotEmpty() || allowEmptySync) {
                        repository.syncComplaintOptionsFromCloud(list.sortedBy { it.displayOrder })
                    }
                }

                CloudSection.FINANCIAL_SERVICES -> {
                    val snap = getQueryWithFallback(firestore.collection("financial_services"), preferredSource)
                    val list = snap.documents.mapIndexedNotNull { idx, doc ->
                        val category = doc.getString("category") ?: return@mapIndexedNotNull null
                        val title = doc.getString("title") ?: return@mapIndexedNotNull null
                        FinancialServiceEntity(
                            id = doc.getLong("id") ?: (idx + 1).toLong(),
                            category = category,
                            title = title,
                            description = doc.getString("description") ?: "",
                            applyUrl = doc.getString("applyUrl") ?: "",
                            isGoogleForm = doc.getBoolean("isGoogleForm") ?: false,
                            buttonText = doc.getString("buttonText") ?: "आवेदन करें",
                            isVisible = doc.getBoolean("isVisible") ?: true,
                            displayOrder = (doc.getLong("displayOrder") ?: (idx + 1).toLong()).toInt()
                        )
                    }
                    if (list.isNotEmpty() || allowEmptySync) {
                        repository.syncFinancialServicesFromCloud(list.sortedBy { it.displayOrder })
                    }
                }

                CloudSection.YOUTUBE_FEEDS -> {
                    val snap = getQueryWithFallback(firestore.collection("youtube_feeds"), preferredSource)
                    val list = snap.documents.mapIndexedNotNull { idx, doc ->
                        val channelTitle = doc.getString("channelTitle") ?: return@mapIndexedNotNull null
                        val feedUrl = doc.getString("feedUrl") ?: return@mapIndexedNotNull null
                        YouTubeFeedEntity(
                            id = doc.getLong("id") ?: (idx + 1).toLong(),
                            channelTitle = channelTitle,
                            feedUrl = feedUrl,
                            channelId = doc.getString("channelId") ?: "",
                            isVisible = doc.getBoolean("isVisible") ?: true
                        )
                    }
                    if (list.isNotEmpty() || allowEmptySync) {
                        repository.syncYouTubeFeedsFromCloud(list.sortedBy { it.id })
                    }
                }

                CloudSection.NEWS_RSS_FEEDS -> {
                    val snap = getQueryWithFallback(firestore.collection("news_rss_feeds"), preferredSource)
                    val list = snap.documents.mapIndexedNotNull { idx, doc ->
                        val title = doc.getString("title") ?: return@mapIndexedNotNull null
                        val feedUrl = doc.getString("feedUrl") ?: return@mapIndexedNotNull null
                        NewsRssFeedEntity(
                            id = doc.getLong("id") ?: (idx + 1).toLong(),
                            title = title,
                            feedUrl = feedUrl,
                            isVisible = doc.getBoolean("isVisible") ?: true
                        )
                    }
                    if (list.isNotEmpty() || allowEmptySync) {
                        repository.syncNewsRssFeedsFromCloud(list.sortedBy { it.id })
                    }
                }
            }
            true
        } catch (t: Throwable) {
            Log.w(TAG, "Error fetching/applying section ${section.name}", t)
            false
        }
    }

    private suspend fun getQueryWithFallback(
        query: Query,
        preferredSource: Source
    ) = try {
        query.get(preferredSource).await()
    } catch (_: Exception) {
        query.get(Source.DEFAULT).await()
    }

    private suspend fun getDocumentWithFallback(
        docRef: com.google.firebase.firestore.DocumentReference,
        preferredSource: Source
    ): DocumentSnapshot? = try {
        docRef.get(preferredSource).await()
    } catch (_: Exception) {
        try {
            docRef.get(Source.DEFAULT).await()
        } catch (_: Exception) {
            null
        }
    }
}
