package com.example.firebase

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.core.SoundNotificationHelper
import com.example.core.YouTubeUrlHelper
import com.example.database.AppRepository
import com.example.database.entities.ComplaintOptionEntity
import com.example.database.entities.DEFAULT_COMPLAINT_OPTIONS
import com.example.database.entities.DEFAULT_FEATURE_BOXES
import com.example.database.entities.DEFAULT_FINANCIAL_BANNER
import com.example.database.entities.DEFAULT_FINANCIAL_SERVICES
import com.example.database.entities.DEFAULT_HOME_TOP_LINKS
import com.example.database.entities.DEFAULT_MAIN_DRIVE_FOLDER_URL
import com.example.database.entities.DEFAULT_NEWS_RSS_FEEDS
import com.example.database.entities.DEFAULT_TOP_THREE_LINKS
import com.example.database.entities.DEFAULT_WHATSAPP_BANNER
import com.example.database.entities.DEFAULT_YOUTUBE_FEEDS
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
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

internal object AdminCloudPublisher {

    private const val TAG = "AdminCloudPublisher"
    private const val PREF_PUSH_SIG_PREFIX = "admin_push_sig_"
    private const val KEY_ADMIN_BROADCAST_SEQ = "admin_broadcast_seq"
    private const val MAX_BROADCAST_HISTORY = 10

    private fun versionsDoc(firestore: FirebaseFirestore): DocumentReference =
        firestore.collection(CloudSyncEngine.META_COLLECTION).document(CloudSyncEngine.SYNC_VERSIONS_DOC)

    private suspend fun commitSingleDocWithVersionBump(
        firestore: FirebaseFirestore,
        targetDoc: DocumentReference,
        data: Map<String, Any>,
        versionKey: String
    ) {
        val batch = firestore.batch()
        batch.set(targetDoc, data)
        batch.set(
            versionsDoc(firestore),
            mapOf(
                versionKey to FieldValue.increment(1),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        )
        batch.commit().await()
    }

    private suspend fun bumpVersionKeys(firestore: FirebaseFirestore, keys: Collection<String>) {
        if (keys.isEmpty()) return
        val updates = hashMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
        for (key in keys) {
            updates[key] = FieldValue.increment(1)
        }
        versionsDoc(firestore).set(updates, SetOptions.merge()).await()
    }

    /**
     * Broadcasts an admin alert to all user devices via admin_broadcasts/latest_alert
     * and stores history in a fixed 10-slot circular buffer (history_0 .. history_9),
     * overwriting the oldest slot automatically without extra delete or read queries.
     */
    suspend fun broadcastAdminUpload(
        context: Context,
        firestore: FirebaseFirestore,
        sharedPrefs: SharedPreferences,
        knownBroadcastSeq: Long,
        onSeqUpdated: (Long) -> Unit,
        onHistoryPrepended: (AdminLoudAlert) -> Unit,
        title: String,
        section: String,
        details: String,
        contentType: String = "",
        contentId: String = "",
        targetSection: String = "",
        targetUrl: String = ""
    ): Boolean {
        return try {
            val baseSeq = when {
                knownBroadcastSeq > 0L -> knownBroadcastSeq
                else -> {
                    val localSeq = sharedPrefs.getLong(KEY_ADMIN_BROADCAST_SEQ, 0L)
                    if (localSeq > 0L) {
                        localSeq
                    } else {
                        try {
                            versionsDoc(firestore).get().await().getLong(CloudSection.BROADCAST_SEQ_KEY) ?: 0L
                        } catch (_: Exception) {
                            0L
                        }
                    }
                }
            }
            val nextSeq = baseSeq + 1L
            val slotIndex = ((nextSeq - 1L) % MAX_BROADCAST_HISTORY).toInt()
            val historyDocId = "history_$slotIndex"

            val alertId = UUID.randomUUID().toString()
            val nowTs = Timestamp.now()

            val latestAlertData = hashMapOf(
                "alertId" to alertId,
                "title" to title,
                "section" to section,
                "details" to details,
                "contentType" to contentType,
                "contentId" to contentId,
                "targetSection" to targetSection,
                "targetUrl" to targetUrl,
                "adminEmail" to "satyamprakash100@gmail.com",
                "timestamp" to nowTs,
                "loudAlert" to false,
                "isHistory" to false
            )

            val historySlotData = hashMapOf(
                "alertId" to alertId,
                "title" to title,
                "section" to section,
                "details" to details,
                "contentType" to contentType,
                "contentId" to contentId,
                "targetSection" to targetSection,
                "targetUrl" to targetUrl,
                "adminEmail" to "satyamprakash100@gmail.com",
                "timestamp" to nowTs,
                "loudAlert" to false,
                "isHistory" to true,
                "slotIndex" to slotIndex,
                "seq" to nextSeq
            )

            // Pre-record alertId on broadcaster device so single version listener doesn't re-sound
            sharedPrefs.edit()
                .putString(CloudSyncEngine.KEY_LAST_ALERT_ID, alertId)
                .putLong(KEY_ADMIN_BROADCAST_SEQ, nextSeq)
                .apply()

            val batch = firestore.batch()
            batch.set(firestore.collection("admin_broadcasts").document("latest_alert"), latestAlertData)
            batch.set(firestore.collection("admin_broadcasts").document(historyDocId), historySlotData)
            batch.set(
                versionsDoc(firestore),
                mapOf(
                    CloudSection.ALERT_VERSION_KEY to FieldValue.increment(1),
                    CloudSection.BROADCAST_SEQ_KEY to nextSeq,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            batch.commit().await()

            onSeqUpdated(nextSeq)
            onHistoryPrepended(
                AdminLoudAlert(
                    alertId = alertId,
                    title = title,
                    section = section,
                    details = details,
                    timestamp = nowTs.toDate().time
                )
            )
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
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error broadcasting admin upload", e)
            false
        }
    }

    // --- Notice ---
    suspend fun publishNoticeToCloud(
        firestore: FirebaseFirestore,
        notice: NoticeEntity,
        notifyFn: suspend (String, String, String) -> Boolean,
        sendNotification: Boolean,
        notifyTypedFn: (suspend (String, String, String, String, String, String, String) -> Boolean)? = null
    ): Boolean = try {
        val docData = hashMapOf(
            "title" to notice.title,
            "summary" to notice.summary,
            "category" to notice.category,
            "issueDate" to notice.issueDate,
            "department" to notice.department,
            "orderNumber" to notice.orderNumber,
            "officialUrl" to notice.officialUrl,
            "pdfUrl" to notice.pdfUrl,
            "imageUrl" to notice.imageUrl,
            "isFeatured" to notice.isFeatured,
            "isPublished" to true,
            "timestamp" to Timestamp.now()
        )
        val docRef = firestore.collection("notices").document(getNoticeDocId(notice))
        commitSingleDocWithVersionBump(firestore, docRef, docData, CloudSection.NOTICES.versionKey)
        if (sendNotification) {
            val noticeTitle = "नया विभागीय आदेश: ${notice.title}"
            val noticeSection = "विभागीय आदेश व सूचनाएं"
            val cid = notice.orderNumber.ifBlank { notice.title }
            val url = notice.pdfUrl.ifBlank { notice.officialUrl }
            if (notifyTypedFn != null) {
                notifyTypedFn(noticeTitle, noticeSection, notice.summary, "notice", cid, "notices", url)
            } else {
                notifyFn(noticeTitle, noticeSection, notice.summary)
            }
        }
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error publishing notice", e)
        false
    }

    suspend fun deleteNoticeFromCloud(firestore: FirebaseFirestore, notice: NoticeEntity): Boolean = try {
        val batch = firestore.batch()
        batch.delete(firestore.collection("notices").document(getNoticeDocId(notice)))
        if (notice.title.isNotBlank()) {
            val matches = firestore.collection("notices").whereEqualTo("title", notice.title).get().await()
            matches.documents.forEach { batch.delete(it.reference) }
        }
        if (notice.orderNumber.isNotBlank()) {
            val matches = firestore.collection("notices").whereEqualTo("orderNumber", notice.orderNumber).get().await()
            matches.documents.forEach { batch.delete(it.reference) }
        }
        batch.set(
            versionsDoc(firestore),
            mapOf(CloudSection.NOTICES.versionKey to FieldValue.increment(1), "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )
        batch.commit().await()
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error deleting notice", e)
        false
    }

    // --- Holiday ---
    suspend fun publishHolidayToCloud(
        firestore: FirebaseFirestore,
        holiday: HolidayEntity,
        notifyFn: suspend (String, String, String) -> Boolean,
        sendNotification: Boolean,
        notifyTypedFn: (suspend (String, String, String, String, String, String, String) -> Boolean)? = null
    ): Boolean = try {
        val docData = hashMapOf(
            "name" to holiday.name,
            "dateStr" to holiday.dateStr,
            "dayOfWeek" to holiday.dayOfWeek,
            "month" to holiday.month,
            "type" to holiday.type,
            "durationDays" to holiday.durationDays,
            "description" to holiday.description,
            "timestamp" to Timestamp.now()
        )
        val docRef = firestore.collection("holiday_calendars").document(getHolidayDocId(holiday))
        commitSingleDocWithVersionBump(firestore, docRef, docData, CloudSection.HOLIDAYS.versionKey)
        if (sendNotification) {
            val hTitle = "अवकाश तालिका अपडेट: ${holiday.name} (${holiday.dateStr})"
            val hSection = "अवकाश कैलेंडर"
            val hDetails = "${holiday.type} - ${holiday.durationDays} दिन"
            val cid = com.example.core.DeepLinkHelper.buildHolidayContentId(holiday)
            if (notifyTypedFn != null) {
                notifyTypedFn(hTitle, hSection, hDetails, "holiday", cid, "avakash", "")
            } else {
                notifyFn(hTitle, hSection, hDetails)
            }
        }
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error publishing holiday", e)
        false
    }

    suspend fun deleteHolidayFromCloud(firestore: FirebaseFirestore, holiday: HolidayEntity): Boolean = try {
        val batch = firestore.batch()
        batch.delete(firestore.collection("holiday_calendars").document(getHolidayDocId(holiday)))
        val matches = firestore.collection("holiday_calendars").whereEqualTo("name", holiday.name).get().await()
        matches.documents.forEach { batch.delete(it.reference) }
        batch.set(
            versionsDoc(firestore),
            mapOf(CloudSection.HOLIDAYS.versionKey to FieldValue.increment(1), "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )
        batch.commit().await()
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error deleting holiday", e)
        false
    }

    // --- Activities ---
    suspend fun publishActivityToCloud(
        firestore: FirebaseFirestore,
        activity: EducationActivityEntity,
        notifyFn: suspend (String, String, String) -> Boolean,
        sendNotification: Boolean
    ): Boolean = try {
        val docData = hashMapOf(
            "title" to activity.title,
            "description" to activity.description,
            "category" to activity.category,
            "linkUrl" to activity.linkUrl,
            "orderNumber" to activity.orderNumber,
            "timestamp" to Timestamp.now()
        )
        val docRef = firestore.collection("activities").document(getActivityDocId(activity))
        commitSingleDocWithVersionBump(firestore, docRef, docData, CloudSection.ACTIVITIES.versionKey)
        if (sendNotification) {
            notifyFn("नया कार्यक्रम/अभियान: ${activity.title}", "विद्यालय गतिविधियां (${activity.category})", activity.description)
        }
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error publishing activity", e)
        false
    }

    suspend fun deleteActivityFromCloud(firestore: FirebaseFirestore, activity: EducationActivityEntity): Boolean = try {
        val batch = firestore.batch()
        batch.delete(firestore.collection("activities").document(getActivityDocId(activity)))
        val matches = firestore.collection("activities").whereEqualTo("title", activity.title).get().await()
        matches.documents.forEach { batch.delete(it.reference) }
        batch.set(
            versionsDoc(firestore),
            mapOf(CloudSection.ACTIVITIES.versionKey to FieldValue.increment(1), "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )
        batch.commit().await()
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error deleting activity", e)
        false
    }

    // --- Quick Portals ---
    suspend fun publishPortalToCloud(
        firestore: FirebaseFirestore,
        portal: QuickPortalEntity,
        notifyFn: suspend (String, String, String) -> Boolean,
        sendNotification: Boolean
    ): Boolean = try {
        val docData = hashMapOf(
            "title" to portal.title,
            "subtitle" to portal.subtitle,
            "url" to portal.url,
            "badge" to portal.badge,
            "colorHex" to portal.colorHex,
            "timestamp" to Timestamp.now()
        )
        val docRef = firestore.collection("quick_portals").document(getPortalDocId(portal))
        commitSingleDocWithVersionBump(firestore, docRef, docData, CloudSection.PORTALS.versionKey)
        if (sendNotification) {
            notifyFn("नया डिजिटल पोर्टल लिंक: ${portal.title}", "महत्वपूर्ण पोर्टल व लिंक्स", portal.url)
        }
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error publishing portal", e)
        false
    }

    suspend fun deletePortalFromCloud(firestore: FirebaseFirestore, portal: QuickPortalEntity): Boolean = try {
        val batch = firestore.batch()
        batch.delete(firestore.collection("quick_portals").document(getPortalDocId(portal)))
        val matches = firestore.collection("quick_portals").whereEqualTo("title", portal.title).get().await()
        matches.documents.forEach { batch.delete(it.reference) }
        batch.set(
            versionsDoc(firestore),
            mapOf(CloudSection.PORTALS.versionKey to FieldValue.increment(1), "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )
        batch.commit().await()
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error deleting portal", e)
        false
    }

    // --- Salary Rules ---
    suspend fun publishSalaryRuleToCloud(
        firestore: FirebaseFirestore,
        daPercent: Double,
        ruralHra: Double,
        urbanHra: Double,
        notifyFn: suspend (String, String, String) -> Boolean,
        sendNotification: Boolean
    ): Boolean = try {
        val ruleData = hashMapOf(
            "daPercent" to daPercent,
            "ruralHra" to ruralHra,
            "urbanHra" to urbanHra,
            "effectiveDate" to "01/11/2026",
            "publishedAt" to Timestamp.now()
        )
        val docRef = firestore.collection("salary_rules").document("current")
        commitSingleDocWithVersionBump(firestore, docRef, ruleData, CloudSection.SALARY_RULES.versionKey)
        if (sendNotification) {
            notifyFn(
                "नया वेतन नियम लागू: DA $daPercent%, HRA $ruralHra% / $urbanHra%",
                "वेतन व भत्ते",
                "राज्य कर्मियों एवं शिक्षकों हेतु संशोधित महंगाई भत्ता एवं मकान किराया भत्ता दरें।"
            )
        }
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error publishing salary rule", e)
        false
    }

    // --- Promo Banners ---
    suspend fun publishBannerToCloud(
        firestore: FirebaseFirestore,
        banner: PromoBannerEntity,
        notifyFn: suspend (String, String, String) -> Boolean,
        sendNotification: Boolean
    ): Boolean = try {
        val docId = if (banner.bannerId.isNotBlank()) banner.bannerId else "banner_${System.currentTimeMillis()}"
        val data = hashMapOf(
            "bannerId" to docId,
            "title" to banner.title,
            "description" to banner.description,
            "category" to banner.category,
            "imageUrl" to banner.imageUrl,
            "destinationUrl" to banner.destinationUrl,
            "buttonText" to banner.buttonText,
            "orderIndex" to banner.orderIndex,
            "isActive" to banner.isActive,
            "publishedAt" to Timestamp.now()
        )
        val docRef = firestore.collection("promo_banners").document(docId)
        commitSingleDocWithVersionBump(firestore, docRef, data, CloudSection.BANNERS.versionKey)
        if (sendNotification) {
            notifyFn("नया प्रमोशनल बैनर अपडेट: ${banner.title}", "बैनर व प्रचार", banner.description)
        }
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error publishing banner", e)
        false
    }

    suspend fun deleteBannerFromCloud(firestore: FirebaseFirestore, bannerId: String): Boolean = try {
        val batch = firestore.batch()
        batch.delete(firestore.collection("promo_banners").document(bannerId))
        batch.set(
            versionsDoc(firestore),
            mapOf(CloudSection.BANNERS.versionKey to FieldValue.increment(1), "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )
        batch.commit().await()
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error deleting banner", e)
        false
    }

    // --- School Timing & WhatsApp Config ---
    suspend fun publishSchoolTimingToCloud(
        firestore: FirebaseFirestore,
        timing: SchoolTimingEntity,
        notifyFn: suspend (String, String, String) -> Boolean,
        sendNotification: Boolean
    ): Boolean = try {
        val data = hashMapOf(
            "openingTime" to timing.openingTime,
            "closingTime" to timing.closingTime,
            "isManuallyClosed" to timing.isManuallyClosed,
            "closureReason" to timing.closureReason,
            "updatedAt" to Timestamp.now()
        )
        val docRef = firestore.collection("school_config").document("timings")
        commitSingleDocWithVersionBump(firestore, docRef, data, CloudSection.SCHOOL_TIMING.versionKey)
        if (sendNotification) {
            notifyFn(
                if (timing.isManuallyClosed) "📢 विद्यालय अवकाश / बंदी सूचना: ${timing.closureReason}"
                else "विद्यालय समय-सारणी अद्यतन: ${timing.openingTime} से ${timing.closingTime}",
                "विद्यालय समय-सारणी",
                if (timing.isManuallyClosed) "प्रशासनिक आदेश अनुसार विद्यालय बंद रहेगा।" else "नियमित विद्यालय संचालन समय।"
            )
        }
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error publishing school timings", e)
        false
    }

    suspend fun publishWhatsAppConfigToCloud(
        firestore: FirebaseFirestore,
        config: WhatsAppLinkConfigEntity,
        notifyFn: suspend (String, String, String) -> Boolean,
        sendNotification: Boolean
    ): Boolean = try {
        val data = hashMapOf(
            "channelUrl" to config.channelUrl,
            "channelTitle" to config.channelTitle,
            "districtGroupsUrl" to config.districtGroupsUrl,
            "districtGroupsTitle" to config.districtGroupsTitle,
            "updatedAt" to Timestamp.now()
        )
        val docRef = firestore.collection("whatsapp_config").document("main")
        commitSingleDocWithVersionBump(firestore, docRef, data, CloudSection.WHATSAPP_CONFIG.versionKey)
        if (sendNotification) {
            notifyFn("व्हाट्सएप चैनल एवं समूह लिंक अपडेट", "व्हाट्सएप कम्युनिटी", config.channelTitle)
        }
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error publishing whatsapp config", e)
        false
    }

    // --- Videos ---
    suspend fun publishVideoToCloud(
        firestore: FirebaseFirestore,
        video: VideoTutorialEntity,
        notifyFn: suspend (String, String, String) -> Boolean,
        sendNotification: Boolean,
        notifyTypedFn: (suspend (String, String, String, String, String, String, String) -> Boolean)? = null
    ): Boolean = try {
        val docId = getVideoDocId(video)
        val thumb = if (video.thumbnailUrl.isNotBlank()) video.thumbnailUrl else YouTubeUrlHelper.getThumbnailUrl(video.youtubeId)
        val data = hashMapOf(
            "id" to video.id,
            "title" to video.title,
            "category" to video.category,
            "youtubeUrl" to video.youtubeUrl,
            "youtubeId" to video.youtubeId,
            "thumbnailUrl" to thumb,
            "duration" to video.duration,
            "description" to video.description,
            "isPinned" to video.isPinned,
            "isFeaturedOnHome" to video.isFeaturedOnHome,
            "displayOrder" to video.displayOrder,
            "publishedAt" to Timestamp.now()
        )
        val docRef = firestore.collection("video_tutorials").document(docId)
        commitSingleDocWithVersionBump(firestore, docRef, data, CloudSection.VIDEOS.versionKey)
        if (sendNotification) {
            val details = video.description.ifBlank { "नया शिक्षक वीडियो ट्यूटोरियल" }
            val cid = video.youtubeId.ifBlank { video.youtubeUrl.ifBlank { video.title } }
            if (notifyTypedFn != null) {
                notifyTypedFn(video.title, "वीडियो लाइब्रेरी", details, "video", cid, "videos", video.youtubeUrl)
            } else {
                notifyFn(video.title, "वीडियो लाइब्रेरी", details)
            }
        }
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error publishing video", e)
        false
    }

    suspend fun deleteVideoFromCloud(firestore: FirebaseFirestore, video: VideoTutorialEntity): Boolean = try {
        val batch = firestore.batch()
        if (video.youtubeId.isNotBlank()) {
            batch.delete(firestore.collection("video_tutorials").document("vid_${video.youtubeId}"))
        }
        val matches = firestore.collection("video_tutorials").whereEqualTo("title", video.title).get().await()
        matches.documents.forEach { batch.delete(it.reference) }
        batch.set(
            versionsDoc(firestore),
            mapOf(CloudSection.VIDEOS.versionKey to FieldValue.increment(1), "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )
        batch.commit().await()
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error deleting video", e)
        false
    }

    // --- Downloads & Google Drive ---
    suspend fun publishDownloadToCloud(
        firestore: FirebaseFirestore,
        download: DownloadResourceEntity,
        notifyFn: suspend (String, String, String) -> Boolean,
        sendNotification: Boolean,
        notifyTypedFn: (suspend (String, String, String, String, String, String, String) -> Boolean)? = null
    ): Boolean = try {
        val resolvedCid = download.cloudId.ifBlank {
            com.example.core.DownloadSyncHelper.resolveCanonicalDocId(download)
        }
        val canonicalDownload = if (download.cloudId.isBlank()) download.copy(cloudId = resolvedCid) else download
        val docRef = firestore.collection("download_resources").document(getDownloadDocId(canonicalDownload))
        val data = hashMapOf(
            "cloudId" to resolvedCid,
            "title" to canonicalDownload.title,
            "category" to canonicalDownload.category,
            "fileSize" to canonicalDownload.fileSize,
            "pdfUrl" to canonicalDownload.pdfUrl,
            "publicationDate" to canonicalDownload.publicationDate,
            "orderNumber" to canonicalDownload.orderNumber,
            "description" to canonicalDownload.description,
            "fileType" to canonicalDownload.fileType,
            "isVisible" to canonicalDownload.isVisible,
            "publishedAt" to Timestamp.now()
        )
        commitSingleDocWithVersionBump(firestore, docRef, data, CloudSection.DOWNLOADS.versionKey)
        if (sendNotification) {
            val details = canonicalDownload.description.ifBlank { "नया प्रपत्र डाउनलोड उपलब्ध" }
            if (notifyTypedFn != null) {
                notifyTypedFn(canonicalDownload.title, "डाउनलोड सेक्शन", details, "download", resolvedCid, "downloads", canonicalDownload.pdfUrl)
            } else {
                notifyFn(canonicalDownload.title, "डाउनलोड सेक्शन", details)
            }
        }
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error publishing download", e)
        false
    }

    suspend fun deleteDownloadFromCloud(firestore: FirebaseFirestore, download: DownloadResourceEntity): Boolean = try {
        val batch = firestore.batch()
        val canonicalId = getDownloadDocId(download)
        val deletedIds = mutableSetOf<String>()
        fun addDelete(docRef: com.google.firebase.firestore.DocumentReference) {
            if (deletedIds.add(docRef.id)) {
                batch.delete(docRef)
            }
        }
        addDelete(firestore.collection("download_resources").document(canonicalId))
        addDelete(firestore.collection("download_resources").document(com.example.core.DownloadSyncHelper.legacyDocId(download)))

        if (download.cloudId.isNotBlank()) {
            val cidMatches = firestore.collection("download_resources").whereEqualTo("cloudId", download.cloudId).get().await()
            cidMatches.documents.forEach { addDelete(it.reference) }
        }
        if (download.pdfUrl.isNotBlank()) {
            val urlMatches = firestore.collection("download_resources").whereEqualTo("pdfUrl", download.pdfUrl).get().await()
            urlMatches.documents.forEach { addDelete(it.reference) }
        }
        if (download.title.isNotBlank()) {
            val titleMatches = firestore.collection("download_resources").whereEqualTo("title", download.title).get().await()
            titleMatches.documents.forEach { addDelete(it.reference) }
        }
        batch.set(
            versionsDoc(firestore),
            mapOf(CloudSection.DOWNLOADS.versionKey to FieldValue.increment(1), "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )
        batch.commit().await()
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error deleting download", e)
        false
    }

    suspend fun publishGoogleDriveConfigToCloud(
        firestore: FirebaseFirestore,
        isEnabled: Boolean,
        folderUrl: String?,
        notifyFn: suspend (String, String, String) -> Boolean,
        sendNotification: Boolean
    ): Boolean = try {
        val data = hashMapOf(
            "isEnabled" to isEnabled,
            "folderUrl" to (folderUrl ?: DEFAULT_MAIN_DRIVE_FOLDER_URL),
            "updatedAt" to Timestamp.now()
        )
        val docRef = firestore.collection("app_configs").document("google_drive")
        commitSingleDocWithVersionBump(firestore, docRef, data, CloudSection.GOOGLE_DRIVE.versionKey)
        if (sendNotification) {
            notifyFn(
                if (isEnabled) "शिक्षक गूगल ड्राइव लिंक सक्रिय" else "गूगल ड्राइव लिंक अपडेट",
                "डाउनलोड सेक्शन / Drive",
                "आधिकारिक प्रपत्र एवं ड्राइव फोल्डर लिंक।"
            )
        }
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error publishing google drive config", e)
        false
    }

    // --- News ---
    suspend fun publishNewsToCloud(
        firestore: FirebaseFirestore,
        news: NewsArticleEntity,
        notifyFn: suspend (String, String, String) -> Boolean,
        sendNotification: Boolean,
        notifyTypedFn: (suspend (String, String, String, String, String, String, String) -> Boolean)? = null
    ): Boolean = try {
        val docRef = firestore.collection("news_articles").document(getNewsDocId(news))
        val data = hashMapOf(
            "title" to news.title,
            "summary" to news.summary,
            "content" to news.content,
            "category" to news.category,
            "publishedAt" to news.publishedAt,
            "source" to news.source,
            "sourceUrl" to news.sourceUrl,
            "imageUrl" to news.imageUrl,
            "isPinned" to news.isPinned,
            "pinOrder" to news.pinOrder,
            "isBreaking" to news.isBreaking,
            "isVisible" to news.isVisible,
            "timestamp" to Timestamp.now()
        )
        commitSingleDocWithVersionBump(firestore, docRef, data, CloudSection.NEWS.versionKey)
        if (sendNotification) {
            val details = news.summary.ifBlank { news.content.take(100) }
            val cid = news.title
            if (notifyTypedFn != null) {
                notifyTypedFn(news.title, "शिक्षा समाचार", details, "news", cid, "news", news.sourceUrl)
            } else {
                notifyFn(news.title, "शिक्षा समाचार", details)
            }
        }
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error publishing news", e)
        false
    }

    suspend fun deleteNewsFromCloud(firestore: FirebaseFirestore, news: NewsArticleEntity): Boolean = try {
        val batch = firestore.batch()
        batch.delete(firestore.collection("news_articles").document(getNewsDocId(news)))
        val matches = firestore.collection("news_articles").whereEqualTo("title", news.title).get().await()
        matches.documents.forEach { batch.delete(it.reference) }
        batch.set(
            versionsDoc(firestore),
            mapOf(CloudSection.NEWS.versionKey to FieldValue.increment(1), "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )
        batch.commit().await()
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error deleting news", e)
        false
    }

    // --- Feature Boxes & Config Cards ---
    suspend fun publishFeatureBoxesToCloud(
        firestore: FirebaseFirestore,
        boxes: List<FeatureBoxEntity>,
        notifyFn: suspend (String, String, String) -> Boolean,
        sendNotification: Boolean
    ): Boolean = try {
        val batch = firestore.batch()
        for (box in boxes) {
            val docRef = firestore.collection("feature_boxes").document(box.id)
            batch.set(
                docRef,
                hashMapOf(
                    "id" to box.id,
                    "title" to box.title,
                    "subtitle" to box.subtitle,
                    "iconType" to box.iconType,
                    "destinationRoute" to box.destinationRoute,
                    "displayOrder" to box.displayOrder,
                    "isVisible" to box.isVisible,
                    "isExternal" to box.isExternal,
                    "externalUrl" to box.externalUrl,
                    "timestamp" to Timestamp.now()
                )
            )
        }
        batch.set(
            versionsDoc(firestore),
            mapOf(CloudSection.FEATURE_BOXES.versionKey to FieldValue.increment(1), "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )
        batch.commit().await()
        if (sendNotification) {
            notifyFn("मुख्य शिक्षक सुविधाएं अपडेट", "मुख्य सुविधाएं", "नई सुविधाएं एवं क्रम अद्यतन किया गया।")
        }
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error publishing feature boxes", e)
        false
    }

    suspend fun deleteFeatureBoxFromCloud(firestore: FirebaseFirestore, box: FeatureBoxEntity): Boolean = try {
        val batch = firestore.batch()
        batch.delete(firestore.collection("feature_boxes").document(box.id))
        batch.set(
            versionsDoc(firestore),
            mapOf(CloudSection.FEATURE_BOXES.versionKey to FieldValue.increment(1), "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )
        batch.commit().await()
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error deleting feature box", e)
        false
    }

    suspend fun publishTopThreeLinksToCloud(
        firestore: FirebaseFirestore,
        links: TopThreeLinksEntity,
        notifyFn: suspend (String, String, String) -> Boolean,
        sendNotification: Boolean
    ): Boolean = try {
        val data = hashMapOf(
            "link1Title" to links.link1Title,
            "link1Url" to links.link1Url,
            "link1Visible" to links.link1Visible,
            "link2Title" to links.link2Title,
            "link2Url" to links.link2Url,
            "link2Visible" to links.link2Visible,
            "link3Title" to links.link3Title,
            "link3Url" to links.link3Url,
            "link3Visible" to links.link3Visible,
            "timestamp" to Timestamp.now()
        )
        commitSingleDocWithVersionBump(
            firestore,
            firestore.collection("app_config").document("top_three_links"),
            data,
            CloudSection.TOP_THREE_LINKS.versionKey
        )
        if (sendNotification) {
            notifyFn("शीर्ष 3 आधिकारिक पोर्टल लिंक अपडेट", "होम स्क्रीन लिंक्स", "${links.link1Title}, ${links.link2Title}, ${links.link3Title}")
        }
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error publishing top three links", e)
        false
    }

    suspend fun publishFinancialBannerToCloud(
        firestore: FirebaseFirestore,
        banner: FinancialBannerEntity,
        notifyFn: suspend (String, String, String) -> Boolean,
        sendNotification: Boolean
    ): Boolean = try {
        val data = hashMapOf(
            "heading" to banner.heading,
            "supportingText" to banner.supportingText,
            "disclaimer" to banner.disclaimer,
            "buttonText" to banner.buttonText,
            "destinationType" to banner.destinationType,
            "destinationUrl" to banner.destinationUrl,
            "imageUrl" to banner.imageUrl,
            "isVisible" to banner.isVisible,
            "timestamp" to Timestamp.now()
        )
        commitSingleDocWithVersionBump(
            firestore,
            firestore.collection("app_config").document("financial_banner"),
            data,
            CloudSection.FINANCIAL_BANNER.versionKey
        )
        if (sendNotification) {
            notifyFn(banner.heading, "वित्तीय सुविधाएं बैनर", banner.supportingText)
        }
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error publishing financial banner", e)
        false
    }

    suspend fun publishWhatsAppBannerToCloud(
        firestore: FirebaseFirestore,
        banner: WhatsAppBannerEntity,
        notifyFn: suspend (String, String, String) -> Boolean,
        sendNotification: Boolean
    ): Boolean = try {
        val data = hashMapOf(
            "bannerText" to banner.bannerText,
            "buttonText" to banner.buttonText,
            "channelUrl" to banner.channelUrl,
            "imageUrl" to banner.imageUrl,
            "isVisible" to banner.isVisible,
            "timestamp" to Timestamp.now()
        )
        commitSingleDocWithVersionBump(
            firestore,
            firestore.collection("app_config").document("whatsapp_banner"),
            data,
            CloudSection.WHATSAPP_BANNER.versionKey
        )
        if (sendNotification) {
            notifyFn("व्हाट्सएप चैनल अपडेट", "व्हाट्सएप बैनर", banner.bannerText)
        }
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error publishing whatsapp banner", e)
        false
    }

    // --- Home Top Links / Complaints / Financial Services / Feeds ---
    suspend fun publishHomeTopLinkToCloud(
        firestore: FirebaseFirestore,
        link: HomeTopLinkEntity,
        notifyFn: suspend (String, String, String) -> Boolean,
        sendNotification: Boolean
    ): Boolean = try {
        val docRef = firestore.collection("home_top_links").document(getHomeTopLinkDocId(link))
        val data = hashMapOf(
            "id" to link.id,
            "title" to link.title,
            "url" to link.url,
            "isVisible" to link.isVisible,
            "displayOrder" to link.displayOrder,
            "lastUpdated" to System.currentTimeMillis(),
            "timestamp" to Timestamp.now()
        )
        commitSingleDocWithVersionBump(firestore, docRef, data, CloudSection.HOME_TOP_LINKS.versionKey)
        if (sendNotification) {
            notifyFn(link.title, "होम पेज लिंक", link.url)
        }
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error publishing home top link", e)
        false
    }

    suspend fun deleteHomeTopLinkFromCloud(firestore: FirebaseFirestore, link: HomeTopLinkEntity): Boolean = try {
        val batch = firestore.batch()
        batch.delete(firestore.collection("home_top_links").document(getHomeTopLinkDocId(link)))
        val matches = firestore.collection("home_top_links").whereEqualTo("title", link.title).get().await()
        matches.documents.forEach { batch.delete(it.reference) }
        batch.set(
            versionsDoc(firestore),
            mapOf(CloudSection.HOME_TOP_LINKS.versionKey to FieldValue.increment(1), "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )
        batch.commit().await()
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error deleting home top link", e)
        false
    }

    suspend fun publishComplaintOptionToCloud(
        firestore: FirebaseFirestore,
        option: ComplaintOptionEntity,
        notifyFn: suspend (String, String, String) -> Boolean,
        sendNotification: Boolean
    ): Boolean = try {
        val docRef = firestore.collection("complaint_options").document(getComplaintDocId(option))
        val data = hashMapOf(
            "id" to option.id,
            "title" to option.title,
            "description" to option.description,
            "phone" to option.phone,
            "email" to option.email,
            "linkUrl" to option.linkUrl,
            "isVisible" to option.isVisible,
            "displayOrder" to option.displayOrder,
            "lastUpdated" to System.currentTimeMillis(),
            "timestamp" to Timestamp.now()
        )
        commitSingleDocWithVersionBump(firestore, docRef, data, CloudSection.COMPLAINT_OPTIONS.versionKey)
        if (sendNotification) {
            notifyFn(option.title, "शिकायत निवारण", option.phone.ifBlank { option.email })
        }
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error publishing complaint option", e)
        false
    }

    suspend fun deleteComplaintOptionFromCloud(firestore: FirebaseFirestore, option: ComplaintOptionEntity): Boolean = try {
        val batch = firestore.batch()
        batch.delete(firestore.collection("complaint_options").document(getComplaintDocId(option)))
        val matches = firestore.collection("complaint_options").whereEqualTo("title", option.title).get().await()
        matches.documents.forEach { batch.delete(it.reference) }
        batch.set(
            versionsDoc(firestore),
            mapOf(CloudSection.COMPLAINT_OPTIONS.versionKey to FieldValue.increment(1), "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )
        batch.commit().await()
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error deleting complaint option", e)
        false
    }

    suspend fun publishFinancialServiceToCloud(
        firestore: FirebaseFirestore,
        service: FinancialServiceEntity,
        notifyFn: suspend (String, String, String) -> Boolean,
        sendNotification: Boolean
    ): Boolean = try {
        val docRef = firestore.collection("financial_services").document(getFinancialServiceDocId(service))
        val data = hashMapOf(
            "id" to service.id,
            "category" to service.category,
            "title" to service.title,
            "description" to service.description,
            "applyUrl" to service.applyUrl,
            "isGoogleForm" to service.isGoogleForm,
            "buttonText" to service.buttonText,
            "isVisible" to service.isVisible,
            "displayOrder" to service.displayOrder,
            "lastUpdated" to System.currentTimeMillis(),
            "timestamp" to Timestamp.now()
        )
        commitSingleDocWithVersionBump(firestore, docRef, data, CloudSection.FINANCIAL_SERVICES.versionKey)
        if (sendNotification) {
            val sectionName = when (service.category) {
                "LOAN" -> "शिक्षक लोन सुविधा"
                "CREDIT_CARD", "CARD" -> "क्रेडिट कार्ड सेवा"
                else -> "निवेश व पेंशन योजना"
            }
            notifyFn(service.title, sectionName, service.description)
        }
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error publishing financial service", e)
        false
    }

    suspend fun deleteFinancialServiceFromCloud(firestore: FirebaseFirestore, service: FinancialServiceEntity): Boolean = try {
        val batch = firestore.batch()
        batch.delete(firestore.collection("financial_services").document(getFinancialServiceDocId(service)))
        val matches = firestore.collection("financial_services").whereEqualTo("title", service.title).get().await()
        matches.documents.forEach { batch.delete(it.reference) }
        batch.set(
            versionsDoc(firestore),
            mapOf(CloudSection.FINANCIAL_SERVICES.versionKey to FieldValue.increment(1), "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )
        batch.commit().await()
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error deleting financial service", e)
        false
    }

    suspend fun publishYouTubeFeedToCloud(
        firestore: FirebaseFirestore,
        feed: YouTubeFeedEntity,
        notifyFn: suspend (String, String, String) -> Boolean,
        isNew: Boolean
    ): Boolean = try {
        val docRef = firestore.collection("youtube_feeds").document(getYouTubeFeedDocId(feed))
        val data = hashMapOf(
            "id" to feed.id,
            "channelTitle" to feed.channelTitle,
            "feedUrl" to feed.feedUrl,
            "channelId" to feed.channelId,
            "isVisible" to feed.isVisible,
            "lastUpdated" to System.currentTimeMillis(),
            "timestamp" to Timestamp.now()
        )
        commitSingleDocWithVersionBump(firestore, docRef, data, CloudSection.YOUTUBE_FEEDS.versionKey)
        if (isNew) {
            notifyFn(feed.channelTitle, "यूट्यूब वीडियो चैनल", "नया चैनल RSS अपडेट")
        }
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error publishing youtube feed", e)
        false
    }

    suspend fun deleteYouTubeFeedFromCloud(firestore: FirebaseFirestore, feed: YouTubeFeedEntity): Boolean = try {
        val batch = firestore.batch()
        batch.delete(firestore.collection("youtube_feeds").document(getYouTubeFeedDocId(feed)))
        val matches = firestore.collection("youtube_feeds").whereEqualTo("feedUrl", feed.feedUrl).get().await()
        matches.documents.forEach { batch.delete(it.reference) }
        batch.set(
            versionsDoc(firestore),
            mapOf(CloudSection.YOUTUBE_FEEDS.versionKey to FieldValue.increment(1), "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )
        batch.commit().await()
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error deleting youtube feed", e)
        false
    }

    suspend fun publishNewsRssFeedToCloud(
        firestore: FirebaseFirestore,
        feed: NewsRssFeedEntity,
        notifyFn: suspend (String, String, String) -> Boolean,
        isNew: Boolean
    ): Boolean = try {
        val docRef = firestore.collection("news_rss_feeds").document(getNewsRssDocId(feed))
        val data = hashMapOf(
            "id" to feed.id,
            "title" to feed.title,
            "feedUrl" to feed.feedUrl,
            "isVisible" to feed.isVisible,
            "lastUpdated" to System.currentTimeMillis(),
            "timestamp" to Timestamp.now()
        )
        commitSingleDocWithVersionBump(firestore, docRef, data, CloudSection.NEWS_RSS_FEEDS.versionKey)
        if (isNew) {
            notifyFn(feed.title, "लाइव समाचार RSS", feed.feedUrl)
        }
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error publishing news rss feed", e)
        false
    }

    suspend fun deleteNewsRssFeedFromCloud(firestore: FirebaseFirestore, feed: NewsRssFeedEntity): Boolean = try {
        val batch = firestore.batch()
        batch.delete(firestore.collection("news_rss_feeds").document(getNewsRssDocId(feed)))
        val matches = firestore.collection("news_rss_feeds").whereEqualTo("feedUrl", feed.feedUrl).get().await()
        matches.documents.forEach { batch.delete(it.reference) }
        batch.set(
            versionsDoc(firestore),
            mapOf(CloudSection.NEWS_RSS_FEEDS.versionKey to FieldValue.increment(1), "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )
        batch.commit().await()
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error deleting news rss feed", e)
        false
    }

    /**
     * Diff-based Batch Sync for Admin Panel:
     * Computes a signature per collection and writes ONLY sections whose local data changed,
     * committing all writes in batched requests (400 ops/batch) + bumping changed keys in sync_versions.
     */
    suspend fun syncAllLocalDataToCloud(
        firestore: FirebaseFirestore,
        repository: AppRepository,
        sharedPrefs: SharedPreferences,
        forceFull: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val ops = mutableListOf<Pair<DocumentReference, Map<String, Any>>>()
            val changedSections = mutableSetOf<String>()
            val sigEdits = mutableMapOf<String, Int>()
            val now = Timestamp.now()

            fun checkAndQueue(section: CloudSection, signature: Int, builder: () -> List<Pair<DocumentReference, Map<String, Any>>>) {
                val prefKey = PREF_PUSH_SIG_PREFIX + section.versionKey
                val prevSig = sharedPrefs.getInt(prefKey, Int.MIN_VALUE)
                if (forceFull || prevSig != signature) {
                    val items = builder()
                    if (items.isNotEmpty()) {
                        ops.addAll(items)
                        changedSections.add(section.versionKey)
                    }
                    sigEdits[prefKey] = signature
                }
            }

            val notices = repository.allNoticesOnce()
            checkAndQueue(CloudSection.NOTICES, notices.map { listOf(it.title, it.summary, it.category, it.issueDate, it.orderNumber, it.officialUrl, it.pdfUrl, it.imageUrl, it.isFeatured) }.hashCode()) {
                notices.map { n ->
                    firestore.collection("notices").document(getNoticeDocId(n)) to hashMapOf(
                        "title" to n.title, "summary" to n.summary, "category" to n.category,
                        "issueDate" to n.issueDate, "department" to n.department, "orderNumber" to n.orderNumber,
                        "officialUrl" to n.officialUrl, "pdfUrl" to n.pdfUrl, "imageUrl" to n.imageUrl,
                        "isFeatured" to n.isFeatured, "isPublished" to true, "timestamp" to now
                    )
                }
            }

            val holidays = repository.allHolidaysOnce()
            checkAndQueue(CloudSection.HOLIDAYS, holidays.map { listOf(it.name, it.dateStr, it.dayOfWeek, it.month, it.type, it.durationDays, it.description) }.hashCode()) {
                holidays.map { h ->
                    firestore.collection("holiday_calendars").document(getHolidayDocId(h)) to hashMapOf(
                        "name" to h.name, "dateStr" to h.dateStr, "dayOfWeek" to h.dayOfWeek,
                        "month" to h.month, "type" to h.type, "durationDays" to h.durationDays,
                        "description" to h.description, "timestamp" to now
                    )
                }
            }

            val activities = repository.allActivitiesOnce()
            checkAndQueue(CloudSection.ACTIVITIES, activities.map { listOf(it.title, it.description, it.category, it.linkUrl, it.orderNumber) }.hashCode()) {
                activities.map { a ->
                    firestore.collection("activities").document(getActivityDocId(a)) to hashMapOf(
                        "title" to a.title, "description" to a.description, "category" to a.category,
                        "linkUrl" to a.linkUrl, "orderNumber" to a.orderNumber, "timestamp" to now
                    )
                }
            }

            val portals = repository.allPortalsOnce()
            checkAndQueue(CloudSection.PORTALS, portals.map { listOf(it.title, it.subtitle, it.url, it.badge, it.colorHex) }.hashCode()) {
                portals.map { p ->
                    firestore.collection("quick_portals").document(getPortalDocId(p)) to hashMapOf(
                        "title" to p.title, "subtitle" to p.subtitle, "url" to p.url,
                        "badge" to p.badge, "colorHex" to p.colorHex, "timestamp" to now
                    )
                }
            }

            val rawDownloads = repository.allDownloadsOnce()
            val dedupedDownloads = com.example.core.DownloadSyncHelper.deduplicate(rawDownloads).map { d ->
                val cid = d.cloudId.ifBlank { com.example.core.DownloadSyncHelper.resolveCanonicalDocId(d) }
                if (d.cloudId.isBlank() && d.id > 0L) {
                    val updatedLocal = d.copy(cloudId = cid)
                    repository.updateDownload(updatedLocal)
                    updatedLocal
                } else {
                    d.copy(cloudId = cid)
                }
            }
            checkAndQueue(CloudSection.DOWNLOADS, dedupedDownloads.map { listOf(it.cloudId, it.title, it.category, it.fileSize, it.pdfUrl, it.publicationDate, it.orderNumber, it.fileType, it.isVisible) }.hashCode()) {
                dedupedDownloads.map { d ->
                    firestore.collection("download_resources").document(getDownloadDocId(d)) to hashMapOf(
                        "cloudId" to d.cloudId,
                        "title" to d.title, "category" to d.category, "fileSize" to d.fileSize,
                        "pdfUrl" to d.pdfUrl, "publicationDate" to d.publicationDate, "orderNumber" to d.orderNumber,
                        "description" to d.description, "fileType" to d.fileType, "isVisible" to d.isVisible, "publishedAt" to now
                    )
                }
            }

            val videos = repository.allVideosOnce().filter { it.isPinned || it.isFeaturedOnHome }
            checkAndQueue(CloudSection.VIDEOS, videos.map { listOf(it.title, it.youtubeUrl, it.youtubeId, it.isPinned, it.isFeaturedOnHome, it.displayOrder) }.hashCode()) {
                videos.map { v ->
                    val thumb = if (v.thumbnailUrl.isNotBlank()) v.thumbnailUrl else YouTubeUrlHelper.getThumbnailUrl(v.youtubeId)
                    firestore.collection("video_tutorials").document(getVideoDocId(v)) to hashMapOf(
                        "id" to v.id, "title" to v.title, "category" to v.category,
                        "youtubeUrl" to v.youtubeUrl, "youtubeId" to v.youtubeId, "thumbnailUrl" to thumb,
                        "duration" to v.duration, "description" to v.description,
                        "isPinned" to v.isPinned, "isFeaturedOnHome" to v.isFeaturedOnHome,
                        "displayOrder" to v.displayOrder, "publishedAt" to now
                    )
                }
            }

            val news = repository.allNewsOnce().filter { it.isPinned || it.isBreaking || it.source.contains("Admin") || it.source.contains("एडमिन") }
            checkAndQueue(CloudSection.NEWS, news.map { listOf(it.title, it.summary, it.isPinned, it.pinOrder, it.isBreaking, it.isVisible) }.hashCode()) {
                news.map { n ->
                    firestore.collection("news_articles").document(getNewsDocId(n)) to hashMapOf(
                        "title" to n.title, "summary" to n.summary, "content" to n.content,
                        "category" to n.category, "publishedAt" to n.publishedAt, "source" to n.source,
                        "sourceUrl" to n.sourceUrl, "imageUrl" to n.imageUrl,
                        "isPinned" to n.isPinned, "pinOrder" to n.pinOrder, "isBreaking" to n.isBreaking,
                        "isVisible" to n.isVisible, "timestamp" to now
                    )
                }
            }

            val topLinks = repository.getAllHomeTopLinksOnce()
            checkAndQueue(CloudSection.HOME_TOP_LINKS, topLinks.map { listOf(it.id, it.title, it.url, it.isVisible, it.displayOrder) }.hashCode()) {
                topLinks.map { link ->
                    firestore.collection("home_top_links").document(getHomeTopLinkDocId(link)) to hashMapOf(
                        "id" to link.id, "title" to link.title, "url" to link.url,
                        "isVisible" to link.isVisible, "displayOrder" to link.displayOrder,
                        "lastUpdated" to System.currentTimeMillis(), "timestamp" to now
                    )
                }
            }

            val complaints = repository.getAllComplaintOptionsOnce()
            checkAndQueue(CloudSection.COMPLAINT_OPTIONS, complaints.map { listOf(it.id, it.title, it.phone, it.email, it.linkUrl, it.isVisible, it.displayOrder) }.hashCode()) {
                complaints.map { c ->
                    firestore.collection("complaint_options").document(getComplaintDocId(c)) to hashMapOf(
                        "id" to c.id, "title" to c.title, "description" to c.description,
                        "phone" to c.phone, "email" to c.email, "linkUrl" to c.linkUrl,
                        "isVisible" to c.isVisible, "displayOrder" to c.displayOrder,
                        "lastUpdated" to System.currentTimeMillis(), "timestamp" to now
                    )
                }
            }

            val financial = repository.getAllFinancialServicesOnce()
            checkAndQueue(CloudSection.FINANCIAL_SERVICES, financial.map { listOf(it.id, it.category, it.title, it.applyUrl, it.isVisible, it.displayOrder) }.hashCode()) {
                financial.map { f ->
                    firestore.collection("financial_services").document(getFinancialServiceDocId(f)) to hashMapOf(
                        "id" to f.id, "category" to f.category, "title" to f.title,
                        "description" to f.description, "applyUrl" to f.applyUrl, "isGoogleForm" to f.isGoogleForm,
                        "buttonText" to f.buttonText, "isVisible" to f.isVisible, "displayOrder" to f.displayOrder,
                        "lastUpdated" to System.currentTimeMillis(), "timestamp" to now
                    )
                }
            }

            val boxes = repository.getAllFeatureBoxesOnce()
            if (boxes.isNotEmpty()) {
                checkAndQueue(CloudSection.FEATURE_BOXES, boxes.map { listOf(it.id, it.title, it.subtitle, it.displayOrder, it.isVisible, it.externalUrl) }.hashCode()) {
                    boxes.map { b ->
                        firestore.collection("feature_boxes").document(b.id) to hashMapOf(
                            "id" to b.id, "title" to b.title, "subtitle" to b.subtitle,
                            "iconType" to b.iconType, "destinationRoute" to b.destinationRoute,
                            "displayOrder" to b.displayOrder, "isVisible" to b.isVisible,
                            "isExternal" to b.isExternal, "externalUrl" to b.externalUrl, "timestamp" to now
                        )
                    }
                }
            }

            repository.getSchoolTimingOnce()?.let { t ->
                checkAndQueue(CloudSection.SCHOOL_TIMING, listOf(t.openingTime, t.closingTime, t.isManuallyClosed, t.closureReason).hashCode()) {
                    listOf(
                        firestore.collection("school_config").document("timings") to hashMapOf(
                            "openingTime" to t.openingTime, "closingTime" to t.closingTime,
                            "isManuallyClosed" to t.isManuallyClosed, "closureReason" to t.closureReason, "updatedAt" to now
                        )
                    )
                }
            }

            repository.getTopThreeLinksOnce()?.let { l ->
                checkAndQueue(CloudSection.TOP_THREE_LINKS, listOf(l.link1Title, l.link1Url, l.link1Visible, l.link2Title, l.link2Url, l.link2Visible, l.link3Title, l.link3Url, l.link3Visible).hashCode()) {
                    listOf(
                        firestore.collection("app_config").document("top_three_links") to hashMapOf(
                            "link1Title" to l.link1Title, "link1Url" to l.link1Url, "link1Visible" to l.link1Visible,
                            "link2Title" to l.link2Title, "link2Url" to l.link2Url, "link2Visible" to l.link2Visible,
                            "link3Title" to l.link3Title, "link3Url" to l.link3Url, "link3Visible" to l.link3Visible,
                            "timestamp" to now
                        )
                    )
                }
            }

            repository.getFinancialBannerOnce()?.let { fb ->
                checkAndQueue(CloudSection.FINANCIAL_BANNER, listOf(fb.heading, fb.supportingText, fb.buttonText, fb.destinationUrl, fb.isVisible).hashCode()) {
                    listOf(
                        firestore.collection("app_config").document("financial_banner") to hashMapOf(
                            "heading" to fb.heading, "supportingText" to fb.supportingText, "disclaimer" to fb.disclaimer,
                            "buttonText" to fb.buttonText, "destinationType" to fb.destinationType,
                            "destinationUrl" to fb.destinationUrl, "imageUrl" to fb.imageUrl, "isVisible" to fb.isVisible, "timestamp" to now
                        )
                    )
                }
            }

            repository.getWhatsAppBannerOnce()?.let { wb ->
                checkAndQueue(CloudSection.WHATSAPP_BANNER, listOf(wb.bannerText, wb.buttonText, wb.channelUrl, wb.isVisible).hashCode()) {
                    listOf(
                        firestore.collection("app_config").document("whatsapp_banner") to hashMapOf(
                            "bannerText" to wb.bannerText, "buttonText" to wb.buttonText, "channelUrl" to wb.channelUrl,
                            "imageUrl" to wb.imageUrl, "isVisible" to wb.isVisible, "timestamp" to now
                        )
                    )
                }
            }

            val currentDa = repository.getCurrentDaPercent()
            checkAndQueue(CloudSection.SALARY_RULES, currentDa.hashCode()) {
                listOf(
                    firestore.collection("salary_rules").document("current") to hashMapOf(
                        "daPercent" to currentDa, "ruralHra" to 8.0, "urbanHra" to 16.0,
                        "effectiveDate" to "01/11/2026", "publishedAt" to now
                    )
                )
            }

            // Execute chunked batches ONLY for modified collections
            if (ops.isNotEmpty()) {
                val chunks = ops.chunked(400)
                for (chunk in chunks) {
                    val batch: WriteBatch = firestore.batch()
                    for ((ref, data) in chunk) {
                        batch.set(ref, data)
                    }
                    batch.commit().await()
                }
                bumpVersionKeys(firestore, changedSections)
                val editor = sharedPrefs.edit()
                sigEdits.forEach { (k, v) -> editor.putInt(k, v) }
                editor.apply()
                Log.d(TAG, "Smart Admin Sync pushed ${ops.size} docs across ${changedSections.size} changed sections.")
            } else {
                // Ensure sync_versions doc exists even when no local diff detected
                val verSnap = versionsDoc(firestore).get().await()
                if (!verSnap.exists()) {
                    val initVersions = hashMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
                    CloudSection.entries.forEach { initVersions[it.versionKey] = 1L }
                    versionsDoc(firestore).set(initVersions, SetOptions.merge()).await()
                }
                Log.d(TAG, "Smart Admin Sync: 0 changes detected, 0 redundant writes performed.")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error in smart syncAllLocalDataToCloud", e)
            false
        }
    }

    /**
     * Explicit Admin-only one-time cloud bootstrap (NEVER called on teacher startup).
     */
    suspend fun bootstrapCloudCollectionsIfEmpty(
        firestore: FirebaseFirestore,
        repository: AppRepository
    ) = withContext(Dispatchers.IO) {
        try {
            val batch = firestore.batch()
            var opsCount = 0
            val now = Timestamp.now()

            if (!firestore.collection("school_config").document("timings").get().await().exists()) {
                val localTiming = repository.getSchoolTimingOnce() ?: SchoolTimingEntity()
                batch.set(
                    firestore.collection("school_config").document("timings"),
                    hashMapOf(
                        "openingTime" to localTiming.openingTime, "closingTime" to localTiming.closingTime,
                        "isManuallyClosed" to localTiming.isManuallyClosed, "closureReason" to localTiming.closureReason,
                        "updatedAt" to now
                    )
                )
                opsCount++
            }

            if (!firestore.collection("salary_rules").document("current").get().await().exists()) {
                val currentDa = repository.getCurrentDaPercent()
                batch.set(
                    firestore.collection("salary_rules").document("current"),
                    hashMapOf(
                        "daPercent" to currentDa, "ruralHra" to 8.0, "urbanHra" to 16.0,
                        "effectiveDate" to "01/11/2026", "publishedAt" to now
                    )
                )
                opsCount++
            }

            if (firestore.collection("feature_boxes").limit(1).get().await().isEmpty) {
                val localBoxes = repository.getAllFeatureBoxesOnce().ifEmpty { DEFAULT_FEATURE_BOXES }
                for (box in localBoxes) {
                    batch.set(
                        firestore.collection("feature_boxes").document(box.id),
                        hashMapOf(
                            "id" to box.id, "title" to box.title, "subtitle" to box.subtitle,
                            "iconType" to box.iconType, "destinationRoute" to box.destinationRoute,
                            "displayOrder" to box.displayOrder, "isVisible" to box.isVisible,
                            "isExternal" to box.isExternal, "externalUrl" to box.externalUrl, "timestamp" to now
                        )
                    )
                    opsCount++
                }
            }

            if (firestore.collection("home_top_links").limit(1).get().await().isEmpty) {
                val localLinks = repository.getAllHomeTopLinksOnce().ifEmpty { DEFAULT_HOME_TOP_LINKS }
                for (link in localLinks) {
                    batch.set(
                        firestore.collection("home_top_links").document(getHomeTopLinkDocId(link)),
                        hashMapOf(
                            "id" to link.id, "title" to link.title, "url" to link.url,
                            "isVisible" to link.isVisible, "displayOrder" to link.displayOrder,
                            "lastUpdated" to System.currentTimeMillis(), "timestamp" to now
                        )
                    )
                    opsCount++
                }
            }

            if (firestore.collection("complaint_options").limit(1).get().await().isEmpty) {
                val localComps = repository.getAllComplaintOptionsOnce().ifEmpty { DEFAULT_COMPLAINT_OPTIONS }
                for (c in localComps) {
                    batch.set(
                        firestore.collection("complaint_options").document(getComplaintDocId(c)),
                        hashMapOf(
                            "id" to c.id, "title" to c.title, "description" to c.description,
                            "phone" to c.phone, "email" to c.email, "linkUrl" to c.linkUrl,
                            "isVisible" to c.isVisible, "displayOrder" to c.displayOrder,
                            "lastUpdated" to System.currentTimeMillis(), "timestamp" to now
                        )
                    )
                    opsCount++
                }
            }

            if (firestore.collection("financial_services").limit(1).get().await().isEmpty) {
                val localFin = repository.getAllFinancialServicesOnce().ifEmpty { DEFAULT_FINANCIAL_SERVICES }
                for (s in localFin) {
                    batch.set(
                        firestore.collection("financial_services").document(getFinancialServiceDocId(s)),
                        hashMapOf(
                            "id" to s.id, "category" to s.category, "title" to s.title,
                            "description" to s.description, "applyUrl" to s.applyUrl, "isGoogleForm" to s.isGoogleForm,
                            "buttonText" to s.buttonText, "isVisible" to s.isVisible, "displayOrder" to s.displayOrder,
                            "lastUpdated" to System.currentTimeMillis(), "timestamp" to now
                        )
                    )
                    opsCount++
                }
            }

            if (firestore.collection("youtube_feeds").limit(1).get().await().isEmpty) {
                val feeds = repository.getAllYouTubeFeedsOnce().ifEmpty { DEFAULT_YOUTUBE_FEEDS }
                for (f in feeds) {
                    batch.set(
                        firestore.collection("youtube_feeds").document(getYouTubeFeedDocId(f)),
                        hashMapOf(
                            "id" to f.id, "channelTitle" to f.channelTitle, "channelId" to f.channelId,
                            "feedUrl" to f.feedUrl, "isVisible" to f.isVisible,
                            "lastUpdated" to System.currentTimeMillis(), "timestamp" to now
                        )
                    )
                    opsCount++
                }
            }

            if (firestore.collection("news_rss_feeds").limit(1).get().await().isEmpty) {
                val rss = repository.getAllNewsRssFeedsOnce().ifEmpty { DEFAULT_NEWS_RSS_FEEDS }
                for (f in rss) {
                    batch.set(
                        firestore.collection("news_rss_feeds").document(getNewsRssDocId(f)),
                        hashMapOf(
                            "id" to f.id, "title" to f.title, "feedUrl" to f.feedUrl,
                            "isVisible" to f.isVisible, "lastUpdated" to System.currentTimeMillis(), "timestamp" to now
                        )
                    )
                    opsCount++
                }
            }

            if (!firestore.collection("app_config").document("top_three_links").get().await().exists()) {
                val l = repository.getTopThreeLinksOnce() ?: DEFAULT_TOP_THREE_LINKS
                batch.set(
                    firestore.collection("app_config").document("top_three_links"),
                    hashMapOf(
                        "link1Title" to l.link1Title, "link1Url" to l.link1Url, "link1Visible" to l.link1Visible,
                        "link2Title" to l.link2Title, "link2Url" to l.link2Url, "link2Visible" to l.link2Visible,
                        "link3Title" to l.link3Title, "link3Url" to l.link3Url, "link3Visible" to l.link3Visible,
                        "timestamp" to now
                    )
                )
                opsCount++
            }

            if (!firestore.collection("app_config").document("financial_banner").get().await().exists()) {
                val fb = repository.getFinancialBannerOnce() ?: DEFAULT_FINANCIAL_BANNER
                batch.set(
                    firestore.collection("app_config").document("financial_banner"),
                    hashMapOf(
                        "heading" to fb.heading, "supportingText" to fb.supportingText, "disclaimer" to fb.disclaimer,
                        "buttonText" to fb.buttonText, "destinationType" to fb.destinationType,
                        "destinationUrl" to fb.destinationUrl, "imageUrl" to fb.imageUrl, "isVisible" to fb.isVisible, "timestamp" to now
                    )
                )
                opsCount++
            }

            if (!firestore.collection("app_config").document("whatsapp_banner").get().await().exists()) {
                val wb = repository.getWhatsAppBannerOnce() ?: DEFAULT_WHATSAPP_BANNER
                batch.set(
                    firestore.collection("app_config").document("whatsapp_banner"),
                    hashMapOf(
                        "bannerText" to wb.bannerText, "buttonText" to wb.buttonText,
                        "channelUrl" to wb.channelUrl, "isVisible" to wb.isVisible, "timestamp" to now
                    )
                )
                opsCount++
            }

            if (!firestore.collection("whatsapp_config").document("main").get().await().exists()) {
                val wa = repository.getWhatsAppDataOnce() ?: WhatsAppLinkConfigEntity(id = 1)
                batch.set(
                    firestore.collection("whatsapp_config").document("main"),
                    hashMapOf(
                        "channelUrl" to wa.channelUrl, "channelTitle" to wa.channelTitle,
                        "districtGroupsUrl" to wa.districtGroupsUrl, "districtGroupsTitle" to wa.districtGroupsTitle,
                        "updatedAt" to now
                    )
                )
                opsCount++
            }

            if (opsCount > 0) {
                val verUpdates = hashMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
                CloudSection.entries.forEach { verUpdates[it.versionKey] = FieldValue.increment(1) }
                batch.set(versionsDoc(firestore), verUpdates, SetOptions.merge())
                batch.commit().await()
                Log.d(TAG, "Admin bootstrapped $opsCount cloud documents in 1 batch.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error in Admin bootstrapCloudCollectionsIfEmpty", e)
        }
    }

    private fun getNoticeDocId(notice: NoticeEntity): String =
        if (notice.orderNumber.isNotBlank()) {
            "ord_" + notice.orderNumber.trim().replace("/", "_").replace(" ", "_").replace(".", "_")
        } else {
            "not_" + notice.title.trim().take(30).replace("/", "_").replace(" ", "_").replace(".", "_")
        }

    private fun getHolidayDocId(holiday: HolidayEntity): String =
        "hol_" + holiday.dateStr.trim().replace("/", "_") + "_" + holiday.name.trim().take(20).replace(" ", "_").replace("/", "_")

    private fun getActivityDocId(activity: EducationActivityEntity): String =
        if (activity.orderNumber.isNotBlank()) {
            "act_" + activity.orderNumber.trim().replace("/", "_").replace(" ", "_").replace(".", "_")
        } else {
            "act_" + activity.title.trim().take(30).replace("/", "_").replace(" ", "_").replace(".", "_")
        }

    private fun getPortalDocId(portal: QuickPortalEntity): String =
        "port_" + portal.title.trim().take(30).replace("/", "_").replace(" ", "_").replace(".", "_")

    private fun getVideoDocId(video: VideoTutorialEntity): String =
        if (video.youtubeId.isNotBlank()) "vid_${video.youtubeId}"
        else "vid_${if (video.id > 0) video.id else System.currentTimeMillis()}"

    private fun getDownloadDocId(download: DownloadResourceEntity): String =
        com.example.core.DownloadSyncHelper.resolveCanonicalDocId(download)

    private fun getNewsDocId(news: NewsArticleEntity): String {
        val basis = (news.title.trim() + "|" + news.sourceUrl.trim()).lowercase()
        val slug = news.title.take(28).trim().replace(Regex("[^a-zA-Z0-9_]"), "_")
        val shortHash = (kotlin.math.abs(basis.hashCode())).toString(16)
        return "news_${slug}_$shortHash"
    }

    private fun getHomeTopLinkDocId(link: HomeTopLinkEntity): String =
        if (link.id > 0) "top_${link.id}"
        else "top_" + link.title.trim().take(30).replace("/", "_").replace(" ", "_")

    private fun getComplaintDocId(option: ComplaintOptionEntity): String =
        if (option.id > 0) "comp_${option.id}"
        else "comp_" + option.title.trim().take(30).replace("/", "_").replace(" ", "_")

    private fun getFinancialServiceDocId(service: FinancialServiceEntity): String =
        if (service.id > 0) "fin_${service.category.lowercase()}_${service.id}"
        else "fin_${service.category.lowercase()}_" + service.title.trim().take(30).replace("/", "_").replace(" ", "_")

    private fun getYouTubeFeedDocId(feed: YouTubeFeedEntity): String =
        if (feed.id > 0) "yt_${feed.id}"
        else "yt_" + feed.channelTitle.trim().take(30).replace("/", "_").replace(" ", "_")

    private fun getNewsRssDocId(feed: NewsRssFeedEntity): String =
        if (feed.id > 0) "news_rss_${feed.id}"
        else "news_rss_" + feed.title.trim().take(30).replace("/", "_").replace(" ", "_")
}
