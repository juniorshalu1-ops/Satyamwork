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
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.MessageDigest
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
            val alertId = UUID.randomUUID().toString()
            val localFallbackSeq = maxOf(knownBroadcastSeq, sharedPrefs.getLong(KEY_ADMIN_BROADCAST_SEQ, 0L))

            // Pre-record alertId on broadcaster device so single version listener doesn't re-sound
            sharedPrefs.edit()
                .putString(CloudSyncEngine.KEY_LAST_ALERT_ID, alertId)
                .apply()

            val nextSeq = firestore.runTransaction { tx ->
                val vRef = versionsDoc(firestore)
                val vSnap = tx.get(vRef)
                val remoteSeq = vSnap.getLong(CloudSection.BROADCAST_SEQ_KEY) ?: 0L
                val remoteAlertV = vSnap.getLong(CloudSection.ALERT_VERSION_KEY) ?: 0L
                val computedNextSeq = maxOf(localFallbackSeq, remoteSeq) + 1L
                val slotIndex = ((computedNextSeq - 1L) % MAX_BROADCAST_HISTORY).toInt()
                val historyDocId = "history_$slotIndex"

                val latestAlertData = hashMapOf<String, Any>(
                    "alertId" to alertId,
                    "title" to title,
                    "section" to section,
                    "details" to details,
                    "contentType" to contentType,
                    "contentId" to contentId,
                    "targetSection" to targetSection,
                    "targetUrl" to targetUrl,
                    "adminEmail" to "satyamprakash100@gmail.com",
                    "timestamp" to FieldValue.serverTimestamp(),
                    "loudAlert" to false,
                    "isHistory" to false
                )

                val historySlotData = hashMapOf<String, Any>(
                    "alertId" to alertId,
                    "title" to title,
                    "section" to section,
                    "details" to details,
                    "contentType" to contentType,
                    "contentId" to contentId,
                    "targetSection" to targetSection,
                    "targetUrl" to targetUrl,
                    "adminEmail" to "satyamprakash100@gmail.com",
                    "timestamp" to FieldValue.serverTimestamp(),
                    "loudAlert" to false,
                    "isHistory" to true,
                    "slotIndex" to slotIndex,
                    "seq" to computedNextSeq
                )

                tx.set(firestore.collection("admin_broadcasts").document("latest_alert"), latestAlertData)
                tx.set(firestore.collection("admin_broadcasts").document(historyDocId), historySlotData)
                tx.set(
                    vRef,
                    mapOf(
                        CloudSection.ALERT_VERSION_KEY to (remoteAlertV + 1L),
                        CloudSection.BROADCAST_SEQ_KEY to computedNextSeq,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                computedNextSeq
            }.await()

            sharedPrefs.edit()
                .putString(CloudSyncEngine.KEY_LAST_ALERT_ID, alertId)
                .putLong(KEY_ADMIN_BROADCAST_SEQ, nextSeq)
                .apply()

            onSeqUpdated(nextSeq)
            onHistoryPrepended(
                AdminLoudAlert(
                    alertId = alertId,
                    title = title,
                    section = section,
                    details = details,
                    timestamp = System.currentTimeMillis()
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
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "admin_broadcasts/latest_alert")
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
        val docData = hashMapOf<String, Any>(
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
            "timestamp" to FieldValue.serverTimestamp()
        )
        val docId = getNoticeDocId(notice)
        val docRef = firestore.collection("notices").document(docId)
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
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "notices/${getNoticeDocId(notice)}")
        false
    }

    suspend fun deleteNoticeFromCloud(firestore: FirebaseFirestore, notice: NoticeEntity): Boolean = try {
        val batch = firestore.batch()
        val deletedIds = mutableSetOf<String>()
        fun addDelete(ref: DocumentReference) {
            if (isSafeDocId(ref.id) && deletedIds.add(ref.id)) batch.delete(ref)
        }
        addDelete(firestore.collection("notices").document(getNoticeDocId(notice)))
        if (notice.title.isNotBlank()) {
            val matches = firestore.collection("notices").whereEqualTo("title", notice.title).get().await()
            matches.documents.forEach { addDelete(it.reference) }
        }
        if (notice.orderNumber.isNotBlank()) {
            val matches = firestore.collection("notices").whereEqualTo("orderNumber", notice.orderNumber).get().await()
            matches.documents.forEach { addDelete(it.reference) }
        }
        batch.set(
            versionsDoc(firestore),
            mapOf(CloudSection.NOTICES.versionKey to FieldValue.increment(1), "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )
        batch.commit().await()
        true
    } catch (e: Exception) {
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.DELETE, "notices/${getNoticeDocId(notice)}")
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
        val docData = hashMapOf<String, Any>(
            "name" to holiday.name,
            "dateStr" to holiday.dateStr,
            "dayOfWeek" to holiday.dayOfWeek,
            "month" to holiday.month,
            "type" to holiday.type,
            "durationDays" to holiday.durationDays,
            "description" to holiday.description,
            "timestamp" to FieldValue.serverTimestamp()
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
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "holiday_calendars/${getHolidayDocId(holiday)}")
        false
    }

    suspend fun deleteHolidayFromCloud(firestore: FirebaseFirestore, holiday: HolidayEntity): Boolean = try {
        val batch = firestore.batch()
        val deletedIds = mutableSetOf<String>()
        fun addDelete(ref: DocumentReference) {
            if (isSafeDocId(ref.id) && deletedIds.add(ref.id)) batch.delete(ref)
        }
        addDelete(firestore.collection("holiday_calendars").document(getHolidayDocId(holiday)))
        val matches = firestore.collection("holiday_calendars").whereEqualTo("name", holiday.name).get().await()
        matches.documents.forEach { addDelete(it.reference) }
        batch.set(
            versionsDoc(firestore),
            mapOf(CloudSection.HOLIDAYS.versionKey to FieldValue.increment(1), "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )
        batch.commit().await()
        true
    } catch (e: Exception) {
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.DELETE, "holiday_calendars/${getHolidayDocId(holiday)}")
        false
    }

    // --- Activities ---
    suspend fun publishActivityToCloud(
        firestore: FirebaseFirestore,
        activity: EducationActivityEntity,
        notifyFn: suspend (String, String, String) -> Boolean,
        sendNotification: Boolean
    ): Boolean = try {
        val docData = hashMapOf<String, Any>(
            "title" to activity.title,
            "description" to activity.description,
            "category" to activity.category,
            "linkUrl" to activity.linkUrl,
            "orderNumber" to activity.orderNumber,
            "timestamp" to FieldValue.serverTimestamp()
        )
        val docRef = firestore.collection("activities").document(getActivityDocId(activity))
        commitSingleDocWithVersionBump(firestore, docRef, docData, CloudSection.ACTIVITIES.versionKey)
        if (sendNotification) {
            notifyFn("नया कार्यक्रम/अभियान: ${activity.title}", "विद्यालय गतिविधियां (${activity.category})", activity.description)
        }
        true
    } catch (e: Exception) {
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "activities/${getActivityDocId(activity)}")
        false
    }

    suspend fun deleteActivityFromCloud(firestore: FirebaseFirestore, activity: EducationActivityEntity): Boolean = try {
        val batch = firestore.batch()
        val deletedIds = mutableSetOf<String>()
        fun addDelete(ref: DocumentReference) {
            if (isSafeDocId(ref.id) && deletedIds.add(ref.id)) batch.delete(ref)
        }
        addDelete(firestore.collection("activities").document(getActivityDocId(activity)))
        val matches = firestore.collection("activities").whereEqualTo("title", activity.title).get().await()
        matches.documents.forEach { addDelete(it.reference) }
        batch.set(
            versionsDoc(firestore),
            mapOf(CloudSection.ACTIVITIES.versionKey to FieldValue.increment(1), "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )
        batch.commit().await()
        true
    } catch (e: Exception) {
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.DELETE, "activities/${getActivityDocId(activity)}")
        false
    }

    // --- Quick Portals ---
    suspend fun publishPortalToCloud(
        firestore: FirebaseFirestore,
        portal: QuickPortalEntity,
        notifyFn: suspend (String, String, String) -> Boolean,
        sendNotification: Boolean
    ): Boolean = try {
        val docData = hashMapOf<String, Any>(
            "title" to portal.title,
            "subtitle" to portal.subtitle,
            "url" to portal.url,
            "badge" to portal.badge,
            "colorHex" to portal.colorHex,
            "timestamp" to FieldValue.serverTimestamp()
        )
        val docRef = firestore.collection("quick_portals").document(getPortalDocId(portal))
        commitSingleDocWithVersionBump(firestore, docRef, docData, CloudSection.PORTALS.versionKey)
        if (sendNotification) {
            notifyFn("नया डिजिटल पोर्टल लिंक: ${portal.title}", "महत्वपूर्ण पोर्टल व लिंक्स", portal.url)
        }
        true
    } catch (e: Exception) {
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "quick_portals/${getPortalDocId(portal)}")
        false
    }

    suspend fun deletePortalFromCloud(firestore: FirebaseFirestore, portal: QuickPortalEntity): Boolean = try {
        val batch = firestore.batch()
        val deletedIds = mutableSetOf<String>()
        fun addDelete(ref: DocumentReference) {
            if (isSafeDocId(ref.id) && deletedIds.add(ref.id)) batch.delete(ref)
        }
        addDelete(firestore.collection("quick_portals").document(getPortalDocId(portal)))
        val matches = firestore.collection("quick_portals").whereEqualTo("title", portal.title).get().await()
        matches.documents.forEach { addDelete(it.reference) }
        batch.set(
            versionsDoc(firestore),
            mapOf(CloudSection.PORTALS.versionKey to FieldValue.increment(1), "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )
        batch.commit().await()
        true
    } catch (e: Exception) {
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.DELETE, "quick_portals/${getPortalDocId(portal)}")
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
        val ruleData = hashMapOf<String, Any>(
            "daPercent" to daPercent,
            "ruralHra" to ruralHra,
            "urbanHra" to urbanHra,
            "effectiveDate" to "01/11/2026",
            "publishedAt" to FieldValue.serverTimestamp()
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
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "salary_rules/current")
        false
    }

    // --- Promo Banners ---
    suspend fun publishBannerToCloud(
        firestore: FirebaseFirestore,
        banner: PromoBannerEntity,
        notifyFn: suspend (String, String, String) -> Boolean,
        sendNotification: Boolean
    ): Boolean = try {
        val docId = resolveBannerDocId(banner)
        val data = hashMapOf<String, Any>(
            "bannerId" to docId,
            "title" to banner.title,
            "description" to banner.description,
            "category" to banner.category,
            "imageUrl" to banner.imageUrl,
            "destinationUrl" to banner.destinationUrl,
            "buttonText" to banner.buttonText,
            "orderIndex" to banner.orderIndex,
            "isActive" to banner.isActive,
            "publishedAt" to FieldValue.serverTimestamp()
        )
        val docRef = firestore.collection("promo_banners").document(docId)
        commitSingleDocWithVersionBump(firestore, docRef, data, CloudSection.BANNERS.versionKey)
        if (sendNotification) {
            notifyFn("नया प्रमोशनल बैनर अपडेट: ${banner.title}", "बैनर व प्रचार", banner.description)
        }
        true
    } catch (e: Exception) {
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "promo_banners/${resolveBannerDocId(banner)}")
        false
    }

    suspend fun deleteBannerFromCloud(
        firestore: FirebaseFirestore,
        bannerId: String,
        title: String = ""
    ): Boolean = try {
        val batch = firestore.batch()
        val deletedIds = mutableSetOf<String>()
        fun addDelete(ref: DocumentReference) {
            if (isSafeDocId(ref.id) && deletedIds.add(ref.id)) batch.delete(ref)
        }
        if (bannerId.isNotBlank()) {
            if (isSafeDocId(bannerId)) {
                addDelete(firestore.collection("promo_banners").document(bannerId))
            }
            val idMatches = firestore.collection("promo_banners").whereEqualTo("bannerId", bannerId).get().await()
            idMatches.documents.forEach { addDelete(it.reference) }
        }
        if (title.isNotBlank()) {
            val titleMatches = firestore.collection("promo_banners").whereEqualTo("title", title).get().await()
            titleMatches.documents.forEach { addDelete(it.reference) }
        }
        batch.set(
            versionsDoc(firestore),
            mapOf(CloudSection.BANNERS.versionKey to FieldValue.increment(1), "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )
        batch.commit().await()
        true
    } catch (e: Exception) {
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.DELETE, "promo_banners/$bannerId")
        false
    }

    // --- School Timing & WhatsApp Config ---
    suspend fun publishSchoolTimingToCloud(
        firestore: FirebaseFirestore,
        timing: SchoolTimingEntity,
        notifyFn: suspend (String, String, String) -> Boolean,
        sendNotification: Boolean
    ): Boolean = try {
        val data = hashMapOf<String, Any>(
            "openingTime" to timing.openingTime,
            "closingTime" to timing.closingTime,
            "isManuallyClosed" to timing.isManuallyClosed,
            "closureReason" to timing.closureReason,
            "updatedAt" to FieldValue.serverTimestamp()
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
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "school_config/timings")
        false
    }

    suspend fun publishWhatsAppConfigToCloud(
        firestore: FirebaseFirestore,
        config: WhatsAppLinkConfigEntity,
        notifyFn: suspend (String, String, String) -> Boolean,
        sendNotification: Boolean
    ): Boolean = try {
        val data = hashMapOf<String, Any>(
            "channelUrl" to config.channelUrl,
            "channelTitle" to config.channelTitle,
            "districtGroupsUrl" to config.districtGroupsUrl,
            "districtGroupsTitle" to config.districtGroupsTitle,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val docRef = firestore.collection("whatsapp_config").document("main")
        commitSingleDocWithVersionBump(firestore, docRef, data, CloudSection.WHATSAPP_CONFIG.versionKey)
        if (sendNotification) {
            notifyFn("व्हाट्सएप चैनल एवं समूह लिंक अपडेट", "व्हाट्सएप कम्युनिटी", config.channelTitle)
        }
        true
    } catch (e: Exception) {
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "whatsapp_config/main")
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
        val data = hashMapOf<String, Any>(
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
            "publishedAt" to FieldValue.serverTimestamp()
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
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "video_tutorials/${getVideoDocId(video)}")
        false
    }

    suspend fun deleteVideoFromCloud(firestore: FirebaseFirestore, video: VideoTutorialEntity): Boolean = try {
        val batch = firestore.batch()
        val deletedIds = mutableSetOf<String>()
        fun addDelete(ref: DocumentReference) {
            if (isSafeDocId(ref.id) && deletedIds.add(ref.id)) batch.delete(ref)
        }
        addDelete(firestore.collection("video_tutorials").document(getVideoDocId(video)))
        if (video.youtubeId.isNotBlank()) {
            val cleanYtId = "vid_" + video.youtubeId.trim().replace(Regex("[^a-zA-Z0-9_-]+"), "_")
            addDelete(firestore.collection("video_tutorials").document(cleanYtId))
        }
        val matches = firestore.collection("video_tutorials").whereEqualTo("title", video.title).get().await()
        matches.documents.forEach { addDelete(it.reference) }
        batch.set(
            versionsDoc(firestore),
            mapOf(CloudSection.VIDEOS.versionKey to FieldValue.increment(1), "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )
        batch.commit().await()
        true
    } catch (e: Exception) {
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.DELETE, "video_tutorials/${getVideoDocId(video)}")
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
        val data = hashMapOf<String, Any>(
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
            "publishedAt" to FieldValue.serverTimestamp()
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
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "download_resources/${getDownloadDocId(download)}")
        false
    }

    suspend fun deleteDownloadFromCloud(firestore: FirebaseFirestore, download: DownloadResourceEntity): Boolean = try {
        val batch = firestore.batch()
        val canonicalId = getDownloadDocId(download)
        val deletedIds = mutableSetOf<String>()
        fun addDelete(docRef: DocumentReference) {
            if (isSafeDocId(docRef.id) && deletedIds.add(docRef.id)) {
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
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.DELETE, "download_resources/${getDownloadDocId(download)}")
        false
    }

    suspend fun publishGoogleDriveConfigToCloud(
        firestore: FirebaseFirestore,
        isEnabled: Boolean,
        folderUrl: String?,
        notifyFn: suspend (String, String, String) -> Boolean,
        sendNotification: Boolean
    ): Boolean = try {
        val data = hashMapOf<String, Any>(
            "isEnabled" to isEnabled,
            "folderUrl" to (folderUrl ?: DEFAULT_MAIN_DRIVE_FOLDER_URL),
            "updatedAt" to FieldValue.serverTimestamp()
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
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "app_configs/google_drive")
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
        val data = hashMapOf<String, Any>(
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
            "timestamp" to FieldValue.serverTimestamp()
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
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "news_articles/${getNewsDocId(news)}")
        false
    }

    suspend fun deleteNewsFromCloud(firestore: FirebaseFirestore, news: NewsArticleEntity): Boolean = try {
        val batch = firestore.batch()
        val deletedIds = mutableSetOf<String>()
        fun addDelete(ref: DocumentReference) {
            if (isSafeDocId(ref.id) && deletedIds.add(ref.id)) batch.delete(ref)
        }
        addDelete(firestore.collection("news_articles").document(getNewsDocId(news)))
        val matches = firestore.collection("news_articles").whereEqualTo("title", news.title).get().await()
        matches.documents.forEach { addDelete(it.reference) }
        batch.set(
            versionsDoc(firestore),
            mapOf(CloudSection.NEWS.versionKey to FieldValue.increment(1), "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )
        batch.commit().await()
        true
    } catch (e: Exception) {
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.DELETE, "news_articles/${getNewsDocId(news)}")
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
            val safeId = sanitizeDocId(box.id, "feature_${box.displayOrder}")
            val docRef = firestore.collection("feature_boxes").document(safeId)
            batch.set(
                docRef,
                hashMapOf<String, Any>(
                    "id" to safeId,
                    "title" to box.title,
                    "subtitle" to box.subtitle,
                    "iconType" to box.iconType,
                    "destinationRoute" to box.destinationRoute,
                    "displayOrder" to box.displayOrder,
                    "isVisible" to box.isVisible,
                    "isExternal" to box.isExternal,
                    "externalUrl" to box.externalUrl,
                    "timestamp" to FieldValue.serverTimestamp()
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
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "feature_boxes")
        false
    }

    suspend fun deleteFeatureBoxFromCloud(firestore: FirebaseFirestore, box: FeatureBoxEntity): Boolean = try {
        val safeId = sanitizeDocId(box.id, "feature_${box.displayOrder}")
        val batch = firestore.batch()
        batch.delete(firestore.collection("feature_boxes").document(safeId))
        batch.set(
            versionsDoc(firestore),
            mapOf(CloudSection.FEATURE_BOXES.versionKey to FieldValue.increment(1), "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )
        batch.commit().await()
        true
    } catch (e: Exception) {
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.DELETE, "feature_boxes/${box.id}")
        false
    }

    suspend fun publishTopThreeLinksToCloud(
        firestore: FirebaseFirestore,
        links: TopThreeLinksEntity,
        notifyFn: suspend (String, String, String) -> Boolean,
        sendNotification: Boolean
    ): Boolean = try {
        val data = hashMapOf<String, Any>(
            "link1Title" to links.link1Title,
            "link1Url" to links.link1Url,
            "link1Visible" to links.link1Visible,
            "link2Title" to links.link2Title,
            "link2Url" to links.link2Url,
            "link2Visible" to links.link2Visible,
            "link3Title" to links.link3Title,
            "link3Url" to links.link3Url,
            "link3Visible" to links.link3Visible,
            "timestamp" to FieldValue.serverTimestamp()
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
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "app_config/top_three_links")
        false
    }

    suspend fun publishFinancialBannerToCloud(
        firestore: FirebaseFirestore,
        banner: FinancialBannerEntity,
        notifyFn: suspend (String, String, String) -> Boolean,
        sendNotification: Boolean
    ): Boolean = try {
        val data = hashMapOf<String, Any>(
            "heading" to banner.heading,
            "supportingText" to banner.supportingText,
            "disclaimer" to banner.disclaimer,
            "buttonText" to banner.buttonText,
            "destinationType" to banner.destinationType,
            "destinationUrl" to banner.destinationUrl,
            "imageUrl" to banner.imageUrl,
            "isVisible" to banner.isVisible,
            "timestamp" to FieldValue.serverTimestamp()
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
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "app_config/financial_banner")
        false
    }

    suspend fun publishWhatsAppBannerToCloud(
        firestore: FirebaseFirestore,
        banner: WhatsAppBannerEntity,
        notifyFn: suspend (String, String, String) -> Boolean,
        sendNotification: Boolean
    ): Boolean = try {
        val data = hashMapOf<String, Any>(
            "bannerText" to banner.bannerText,
            "buttonText" to banner.buttonText,
            "channelUrl" to banner.channelUrl,
            "imageUrl" to banner.imageUrl,
            "isVisible" to banner.isVisible,
            "timestamp" to FieldValue.serverTimestamp()
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
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "app_config/whatsapp_banner")
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
        val data = hashMapOf<String, Any>(
            "id" to link.id,
            "title" to link.title,
            "url" to link.url,
            "isVisible" to link.isVisible,
            "displayOrder" to link.displayOrder,
            "lastUpdated" to System.currentTimeMillis(),
            "timestamp" to FieldValue.serverTimestamp()
        )
        commitSingleDocWithVersionBump(firestore, docRef, data, CloudSection.HOME_TOP_LINKS.versionKey)
        if (sendNotification) {
            notifyFn(link.title, "होम पेज लिंक", link.url)
        }
        true
    } catch (e: Exception) {
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "home_top_links/${getHomeTopLinkDocId(link)}")
        false
    }

    suspend fun deleteHomeTopLinkFromCloud(firestore: FirebaseFirestore, link: HomeTopLinkEntity): Boolean = try {
        val batch = firestore.batch()
        val deletedIds = mutableSetOf<String>()
        fun addDelete(ref: DocumentReference) {
            if (isSafeDocId(ref.id) && deletedIds.add(ref.id)) batch.delete(ref)
        }
        addDelete(firestore.collection("home_top_links").document(getHomeTopLinkDocId(link)))
        val matches = firestore.collection("home_top_links").whereEqualTo("title", link.title).get().await()
        matches.documents.forEach { addDelete(it.reference) }
        batch.set(
            versionsDoc(firestore),
            mapOf(CloudSection.HOME_TOP_LINKS.versionKey to FieldValue.increment(1), "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )
        batch.commit().await()
        true
    } catch (e: Exception) {
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.DELETE, "home_top_links/${getHomeTopLinkDocId(link)}")
        false
    }

    suspend fun publishComplaintOptionToCloud(
        firestore: FirebaseFirestore,
        option: ComplaintOptionEntity,
        notifyFn: suspend (String, String, String) -> Boolean,
        sendNotification: Boolean
    ): Boolean = try {
        val docRef = firestore.collection("complaint_options").document(getComplaintDocId(option))
        val data = hashMapOf<String, Any>(
            "id" to option.id,
            "title" to option.title,
            "description" to option.description,
            "phone" to option.phone,
            "email" to option.email,
            "linkUrl" to option.linkUrl,
            "isVisible" to option.isVisible,
            "displayOrder" to option.displayOrder,
            "lastUpdated" to System.currentTimeMillis(),
            "timestamp" to FieldValue.serverTimestamp()
        )
        commitSingleDocWithVersionBump(firestore, docRef, data, CloudSection.COMPLAINT_OPTIONS.versionKey)
        if (sendNotification) {
            notifyFn(option.title, "शिकायत निवारण", option.phone.ifBlank { option.email })
        }
        true
    } catch (e: Exception) {
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "complaint_options/${getComplaintDocId(option)}")
        false
    }

    suspend fun deleteComplaintOptionFromCloud(firestore: FirebaseFirestore, option: ComplaintOptionEntity): Boolean = try {
        val batch = firestore.batch()
        val deletedIds = mutableSetOf<String>()
        fun addDelete(ref: DocumentReference) {
            if (isSafeDocId(ref.id) && deletedIds.add(ref.id)) batch.delete(ref)
        }
        addDelete(firestore.collection("complaint_options").document(getComplaintDocId(option)))
        val matches = firestore.collection("complaint_options").whereEqualTo("title", option.title).get().await()
        matches.documents.forEach { addDelete(it.reference) }
        batch.set(
            versionsDoc(firestore),
            mapOf(CloudSection.COMPLAINT_OPTIONS.versionKey to FieldValue.increment(1), "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )
        batch.commit().await()
        true
    } catch (e: Exception) {
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.DELETE, "complaint_options/${getComplaintDocId(option)}")
        false
    }

    suspend fun publishFinancialServiceToCloud(
        firestore: FirebaseFirestore,
        service: FinancialServiceEntity,
        notifyFn: suspend (String, String, String) -> Boolean,
        sendNotification: Boolean
    ): Boolean = try {
        val docRef = firestore.collection("financial_services").document(getFinancialServiceDocId(service))
        val data = hashMapOf<String, Any>(
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
            "timestamp" to FieldValue.serverTimestamp()
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
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "financial_services/${getFinancialServiceDocId(service)}")
        false
    }

    suspend fun deleteFinancialServiceFromCloud(firestore: FirebaseFirestore, service: FinancialServiceEntity): Boolean = try {
        val batch = firestore.batch()
        val deletedIds = mutableSetOf<String>()
        fun addDelete(ref: DocumentReference) {
            if (isSafeDocId(ref.id) && deletedIds.add(ref.id)) batch.delete(ref)
        }
        addDelete(firestore.collection("financial_services").document(getFinancialServiceDocId(service)))
        val matches = firestore.collection("financial_services").whereEqualTo("title", service.title).get().await()
        matches.documents.forEach { addDelete(it.reference) }
        batch.set(
            versionsDoc(firestore),
            mapOf(CloudSection.FINANCIAL_SERVICES.versionKey to FieldValue.increment(1), "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )
        batch.commit().await()
        true
    } catch (e: Exception) {
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.DELETE, "financial_services/${getFinancialServiceDocId(service)}")
        false
    }

    suspend fun publishYouTubeFeedToCloud(
        firestore: FirebaseFirestore,
        feed: YouTubeFeedEntity,
        notifyFn: suspend (String, String, String) -> Boolean,
        isNew: Boolean
    ): Boolean = try {
        val docRef = firestore.collection("youtube_feeds").document(getYouTubeFeedDocId(feed))
        val data = hashMapOf<String, Any>(
            "id" to feed.id,
            "channelTitle" to feed.channelTitle,
            "feedUrl" to feed.feedUrl,
            "channelId" to feed.channelId,
            "isVisible" to feed.isVisible,
            "lastUpdated" to System.currentTimeMillis(),
            "timestamp" to FieldValue.serverTimestamp()
        )
        commitSingleDocWithVersionBump(firestore, docRef, data, CloudSection.YOUTUBE_FEEDS.versionKey)
        if (isNew) {
            notifyFn(feed.channelTitle, "यूट्यूब वीडियो चैनल", "नया चैनल RSS अपडेट")
        }
        true
    } catch (e: Exception) {
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "youtube_feeds/${getYouTubeFeedDocId(feed)}")
        false
    }

    suspend fun deleteYouTubeFeedFromCloud(firestore: FirebaseFirestore, feed: YouTubeFeedEntity): Boolean = try {
        val batch = firestore.batch()
        val deletedIds = mutableSetOf<String>()
        fun addDelete(ref: DocumentReference) {
            if (isSafeDocId(ref.id) && deletedIds.add(ref.id)) batch.delete(ref)
        }
        addDelete(firestore.collection("youtube_feeds").document(getYouTubeFeedDocId(feed)))
        val matches = firestore.collection("youtube_feeds").whereEqualTo("feedUrl", feed.feedUrl).get().await()
        matches.documents.forEach { addDelete(it.reference) }
        batch.set(
            versionsDoc(firestore),
            mapOf(CloudSection.YOUTUBE_FEEDS.versionKey to FieldValue.increment(1), "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )
        batch.commit().await()
        true
    } catch (e: Exception) {
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.DELETE, "youtube_feeds/${getYouTubeFeedDocId(feed)}")
        false
    }

    suspend fun publishNewsRssFeedToCloud(
        firestore: FirebaseFirestore,
        feed: NewsRssFeedEntity,
        notifyFn: suspend (String, String, String) -> Boolean,
        isNew: Boolean
    ): Boolean = try {
        val docRef = firestore.collection("news_rss_feeds").document(getNewsRssDocId(feed))
        val data = hashMapOf<String, Any>(
            "id" to feed.id,
            "title" to feed.title,
            "feedUrl" to feed.feedUrl,
            "isVisible" to feed.isVisible,
            "lastUpdated" to System.currentTimeMillis(),
            "timestamp" to FieldValue.serverTimestamp()
        )
        commitSingleDocWithVersionBump(firestore, docRef, data, CloudSection.NEWS_RSS_FEEDS.versionKey)
        if (isNew) {
            notifyFn(feed.title, "लाइव समाचार RSS", feed.feedUrl)
        }
        true
    } catch (e: Exception) {
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "news_rss_feeds/${getNewsRssDocId(feed)}")
        false
    }

    suspend fun deleteNewsRssFeedFromCloud(firestore: FirebaseFirestore, feed: NewsRssFeedEntity): Boolean = try {
        val batch = firestore.batch()
        val deletedIds = mutableSetOf<String>()
        fun addDelete(ref: DocumentReference) {
            if (isSafeDocId(ref.id) && deletedIds.add(ref.id)) batch.delete(ref)
        }
        addDelete(firestore.collection("news_rss_feeds").document(getNewsRssDocId(feed)))
        val matches = firestore.collection("news_rss_feeds").whereEqualTo("feedUrl", feed.feedUrl).get().await()
        matches.documents.forEach { addDelete(it.reference) }
        batch.set(
            versionsDoc(firestore),
            mapOf(CloudSection.NEWS_RSS_FEEDS.versionKey to FieldValue.increment(1), "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )
        batch.commit().await()
        true
    } catch (e: Exception) {
        FirestoreErrorHandler.handleFirestoreError(e, OperationType.DELETE, "news_rss_feeds/${getNewsRssDocId(feed)}")
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
            val serverNow = FieldValue.serverTimestamp()

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

            val notices = repository.allNoticesOnce().filter { it.title.isNotBlank() }
            checkAndQueue(CloudSection.NOTICES, notices.map { listOf(it.title, it.summary, it.category, it.issueDate, it.orderNumber, it.officialUrl, it.pdfUrl, it.imageUrl, it.isFeatured) }.hashCode()) {
                notices.map { n ->
                    firestore.collection("notices").document(getNoticeDocId(n)) to hashMapOf<String, Any>(
                        "title" to n.title.take(500), "summary" to n.summary.take(4000),
                        "category" to n.category.ifBlank { "सरकारी आदेश" }.take(120),
                        "issueDate" to n.issueDate.take(64), "department" to n.department.take(300),
                        "orderNumber" to n.orderNumber.take(120),
                        "officialUrl" to n.officialUrl.take(1000), "pdfUrl" to n.pdfUrl.take(1000),
                        "imageUrl" to n.imageUrl.take(1000),
                        "isFeatured" to n.isFeatured, "isPublished" to true, "timestamp" to serverNow
                    )
                }
            }

            val holidays = repository.allHolidaysOnce().filter { it.name.isNotBlank() }
            checkAndQueue(CloudSection.HOLIDAYS, holidays.map { listOf(it.name, it.dateStr, it.dayOfWeek, it.month, it.type, it.durationDays, it.description) }.hashCode()) {
                holidays.map { h ->
                    firestore.collection("holiday_calendars").document(getHolidayDocId(h)) to hashMapOf<String, Any>(
                        "name" to h.name.take(300), "dateStr" to h.dateStr.take(64), "dayOfWeek" to h.dayOfWeek.take(64),
                        "month" to h.month.coerceIn(1, 12), "type" to h.type.ifBlank { "अवकाश" }.take(120),
                        "durationDays" to h.durationDays.coerceIn(1, 366),
                        "description" to h.description.take(1000), "timestamp" to serverNow
                    )
                }
            }

            val activities = repository.allActivitiesOnce().filter { it.title.isNotBlank() }
            checkAndQueue(CloudSection.ACTIVITIES, activities.map { listOf(it.title, it.description, it.category, it.linkUrl, it.orderNumber) }.hashCode()) {
                activities.map { a ->
                    firestore.collection("activities").document(getActivityDocId(a)) to hashMapOf<String, Any>(
                        "title" to a.title.take(300), "description" to a.description.take(2000),
                        "category" to a.category.ifBlank { "गतिविधि" }.take(120),
                        "linkUrl" to a.linkUrl.take(1000), "orderNumber" to a.orderNumber.take(120), "timestamp" to serverNow
                    )
                }
            }

            val portals = repository.allPortalsOnce().filter { it.title.isNotBlank() }
            checkAndQueue(CloudSection.PORTALS, portals.map { listOf(it.title, it.subtitle, it.url, it.badge, it.colorHex) }.hashCode()) {
                portals.map { p ->
                    firestore.collection("quick_portals").document(getPortalDocId(p)) to hashMapOf<String, Any>(
                        "title" to p.title.take(200), "subtitle" to p.subtitle.take(300), "url" to p.url.take(1000),
                        "badge" to p.badge.take(64), "colorHex" to p.colorHex.take(32), "timestamp" to serverNow
                    )
                }
            }

            val rawDownloads = repository.allDownloadsOnce().filter { it.title.isNotBlank() }
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
                    firestore.collection("download_resources").document(getDownloadDocId(d)) to hashMapOf<String, Any>(
                        "cloudId" to d.cloudId.take(128),
                        "title" to d.title.take(500), "category" to d.category.ifBlank { "प्रपत्र" }.take(120),
                        "fileSize" to d.fileSize.take(64),
                        "pdfUrl" to d.pdfUrl.take(1000), "publicationDate" to d.publicationDate.take(64),
                        "orderNumber" to d.orderNumber.take(120),
                        "description" to d.description.take(2000), "fileType" to d.fileType.take(32),
                        "isVisible" to d.isVisible, "publishedAt" to serverNow
                    )
                }
            }

            val videos = repository.allVideosOnce().filter { it.title.isNotBlank() && (it.duration != "यूट्यूब" || it.isPinned || it.isFeaturedOnHome) }
            checkAndQueue(CloudSection.VIDEOS, videos.map { listOf(it.title, it.youtubeUrl, it.youtubeId, it.isPinned, it.isFeaturedOnHome, it.displayOrder) }.hashCode()) {
                videos.map { v ->
                    val thumb = if (v.thumbnailUrl.isNotBlank()) v.thumbnailUrl else YouTubeUrlHelper.getThumbnailUrl(v.youtubeId)
                    firestore.collection("video_tutorials").document(getVideoDocId(v)) to hashMapOf<String, Any>(
                        "id" to v.id, "title" to v.title.take(400), "category" to v.category.take(120),
                        "youtubeUrl" to v.youtubeUrl.take(1000), "youtubeId" to v.youtubeId.take(128), "thumbnailUrl" to thumb.take(1000),
                        "duration" to v.duration.take(64), "description" to v.description.take(2000),
                        "isPinned" to v.isPinned, "isFeaturedOnHome" to v.isFeaturedOnHome,
                        "displayOrder" to v.displayOrder, "publishedAt" to serverNow
                    )
                }
            }

            val news = repository.allNewsOnce().filter {
                it.title.isNotBlank() && (it.category != "लाइव RSS" || it.isPinned || it.isBreaking || it.source.contains("Admin") || it.source.contains("एडमिन"))
            }
            checkAndQueue(CloudSection.NEWS, news.map { listOf(it.title, it.summary, it.imageUrl, it.isPinned, it.pinOrder, it.isBreaking, it.isVisible) }.hashCode()) {
                news.map { n ->
                    firestore.collection("news_articles").document(getNewsDocId(n)) to hashMapOf<String, Any>(
                        "title" to n.title.take(500), "summary" to n.summary.take(2000), "content" to n.content.take(15000),
                        "category" to n.category.ifBlank { "शिक्षा समाचार" }.take(120), "publishedAt" to n.publishedAt.take(120),
                        "source" to n.source.take(200), "sourceUrl" to n.sourceUrl.take(1000), "imageUrl" to n.imageUrl.take(1000),
                        "isPinned" to n.isPinned, "pinOrder" to n.pinOrder, "isBreaking" to n.isBreaking,
                        "isVisible" to n.isVisible, "timestamp" to serverNow
                    )
                }
            }

            val banners = repository.allBannersOnce()
            if (banners.isNotEmpty()) {
                checkAndQueue(CloudSection.BANNERS, banners.map { listOf(it.bannerId, it.title, it.description, it.imageUrl, it.destinationUrl, it.orderIndex, it.isActive) }.hashCode()) {
                    banners.map { b ->
                        val docId = resolveBannerDocId(b)
                        firestore.collection("promo_banners").document(docId) to hashMapOf<String, Any>(
                            "bannerId" to docId, "title" to b.title.ifBlank { "बैनर" }, "description" to b.description,
                            "category" to b.category, "imageUrl" to b.imageUrl, "destinationUrl" to b.destinationUrl,
                            "buttonText" to b.buttonText, "orderIndex" to b.orderIndex, "isActive" to b.isActive,
                            "publishedAt" to serverNow
                        )
                    }
                }
            }

            val topLinks = repository.getAllHomeTopLinksOnce()
            checkAndQueue(CloudSection.HOME_TOP_LINKS, topLinks.map { listOf(it.id, it.title, it.url, it.isVisible, it.displayOrder) }.hashCode()) {
                topLinks.map { link ->
                    firestore.collection("home_top_links").document(getHomeTopLinkDocId(link)) to hashMapOf<String, Any>(
                        "id" to link.id, "title" to link.title, "url" to link.url,
                        "isVisible" to link.isVisible, "displayOrder" to link.displayOrder,
                        "lastUpdated" to System.currentTimeMillis(), "timestamp" to serverNow
                    )
                }
            }

            val complaints = repository.getAllComplaintOptionsOnce()
            checkAndQueue(CloudSection.COMPLAINT_OPTIONS, complaints.map { listOf(it.id, it.title, it.phone, it.email, it.linkUrl, it.isVisible, it.displayOrder) }.hashCode()) {
                complaints.map { c ->
                    firestore.collection("complaint_options").document(getComplaintDocId(c)) to hashMapOf<String, Any>(
                        "id" to c.id, "title" to c.title, "description" to c.description,
                        "phone" to c.phone, "email" to c.email, "linkUrl" to c.linkUrl,
                        "isVisible" to c.isVisible, "displayOrder" to c.displayOrder,
                        "lastUpdated" to System.currentTimeMillis(), "timestamp" to serverNow
                    )
                }
            }

            val financial = repository.getAllFinancialServicesOnce()
            checkAndQueue(CloudSection.FINANCIAL_SERVICES, financial.map { listOf(it.id, it.category, it.title, it.applyUrl, it.isVisible, it.displayOrder) }.hashCode()) {
                financial.map { f ->
                    firestore.collection("financial_services").document(getFinancialServiceDocId(f)) to hashMapOf<String, Any>(
                        "id" to f.id, "category" to f.category, "title" to f.title,
                        "description" to f.description, "applyUrl" to f.applyUrl, "isGoogleForm" to f.isGoogleForm,
                        "buttonText" to f.buttonText, "isVisible" to f.isVisible, "displayOrder" to f.displayOrder,
                        "lastUpdated" to System.currentTimeMillis(), "timestamp" to serverNow
                    )
                }
            }

            val boxes = repository.getAllFeatureBoxesOnce()
            if (boxes.isNotEmpty()) {
                checkAndQueue(CloudSection.FEATURE_BOXES, boxes.map { listOf(it.id, it.title, it.subtitle, it.displayOrder, it.isVisible, it.externalUrl) }.hashCode()) {
                    boxes.map { b ->
                        val safeBoxId = sanitizeDocId(b.id, "feature_${b.displayOrder}")
                        firestore.collection("feature_boxes").document(safeBoxId) to hashMapOf<String, Any>(
                            "id" to safeBoxId, "title" to b.title, "subtitle" to b.subtitle,
                            "iconType" to b.iconType, "destinationRoute" to b.destinationRoute,
                            "displayOrder" to b.displayOrder, "isVisible" to b.isVisible,
                            "isExternal" to b.isExternal, "externalUrl" to b.externalUrl, "timestamp" to serverNow
                        )
                    }
                }
            }

            val ytFeeds = repository.getAllYouTubeFeedsOnce().filter { it.channelTitle.isNotBlank() && it.feedUrl.isNotBlank() }
            if (ytFeeds.isNotEmpty()) {
                checkAndQueue(CloudSection.YOUTUBE_FEEDS, ytFeeds.map { listOf(it.id, it.channelTitle, it.feedUrl, it.channelId, it.isVisible) }.hashCode()) {
                    ytFeeds.map { f ->
                        firestore.collection("youtube_feeds").document(getYouTubeFeedDocId(f)) to hashMapOf<String, Any>(
                            "id" to f.id, "channelTitle" to f.channelTitle, "feedUrl" to f.feedUrl,
                            "channelId" to f.channelId, "isVisible" to f.isVisible,
                            "lastUpdated" to System.currentTimeMillis(), "timestamp" to serverNow
                        )
                    }
                }
            }

            val newsFeeds = repository.getAllNewsRssFeedsOnce().filter { it.title.isNotBlank() && it.feedUrl.isNotBlank() }
            if (newsFeeds.isNotEmpty()) {
                checkAndQueue(CloudSection.NEWS_RSS_FEEDS, newsFeeds.map { listOf(it.id, it.title, it.feedUrl, it.isVisible) }.hashCode()) {
                    newsFeeds.map { f ->
                        firestore.collection("news_rss_feeds").document(getNewsRssDocId(f)) to hashMapOf<String, Any>(
                            "id" to f.id, "title" to f.title, "feedUrl" to f.feedUrl,
                            "isVisible" to f.isVisible, "lastUpdated" to System.currentTimeMillis(),
                            "timestamp" to serverNow
                        )
                    }
                }
            }

            repository.getSchoolTimingOnce()?.let { t ->
                checkAndQueue(CloudSection.SCHOOL_TIMING, listOf(t.openingTime, t.closingTime, t.isManuallyClosed, t.closureReason).hashCode()) {
                    listOf(
                        firestore.collection("school_config").document("timings") to hashMapOf<String, Any>(
                            "openingTime" to t.openingTime, "closingTime" to t.closingTime,
                            "isManuallyClosed" to t.isManuallyClosed, "closureReason" to t.closureReason, "updatedAt" to serverNow
                        )
                    )
                }
            }

            repository.getWhatsAppDataOnce()?.let { wa ->
                checkAndQueue(CloudSection.WHATSAPP_CONFIG, listOf(wa.channelUrl, wa.channelTitle, wa.districtGroupsUrl, wa.districtGroupsTitle).hashCode()) {
                    listOf(
                        firestore.collection("whatsapp_config").document("main") to hashMapOf<String, Any>(
                            "channelUrl" to wa.channelUrl, "channelTitle" to wa.channelTitle.ifBlank { "शिक्षक साथी चैनल" },
                            "districtGroupsUrl" to wa.districtGroupsUrl, "districtGroupsTitle" to wa.districtGroupsTitle,
                            "updatedAt" to serverNow
                        )
                    )
                }
            }

            repository.getGoogleDriveConfigOnce()?.let { gd ->
                checkAndQueue(CloudSection.GOOGLE_DRIVE, listOf(gd.isEnabled, gd.folderUrl).hashCode()) {
                    listOf(
                        firestore.collection("app_configs").document("google_drive") to hashMapOf<String, Any>(
                            "isEnabled" to gd.isEnabled, "folderUrl" to gd.folderUrl, "updatedAt" to serverNow
                        )
                    )
                }
            }

            repository.getTopThreeLinksOnce()?.let { l ->
                checkAndQueue(CloudSection.TOP_THREE_LINKS, listOf(l.link1Title, l.link1Url, l.link1Visible, l.link2Title, l.link2Url, l.link2Visible, l.link3Title, l.link3Url, l.link3Visible).hashCode()) {
                    listOf(
                        firestore.collection("app_config").document("top_three_links") to hashMapOf<String, Any>(
                            "link1Title" to l.link1Title, "link1Url" to l.link1Url, "link1Visible" to l.link1Visible,
                            "link2Title" to l.link2Title, "link2Url" to l.link2Url, "link2Visible" to l.link2Visible,
                            "link3Title" to l.link3Title, "link3Url" to l.link3Url, "link3Visible" to l.link3Visible,
                            "timestamp" to serverNow
                        )
                    )
                }
            }

            repository.getFinancialBannerOnce()?.let { fb ->
                checkAndQueue(CloudSection.FINANCIAL_BANNER, listOf(fb.heading, fb.supportingText, fb.buttonText, fb.destinationUrl, fb.imageUrl, fb.isVisible).hashCode()) {
                    listOf(
                        firestore.collection("app_config").document("financial_banner") to hashMapOf<String, Any>(
                            "heading" to fb.heading, "supportingText" to fb.supportingText, "disclaimer" to fb.disclaimer,
                            "buttonText" to fb.buttonText, "destinationType" to fb.destinationType,
                            "destinationUrl" to fb.destinationUrl, "imageUrl" to fb.imageUrl, "isVisible" to fb.isVisible, "timestamp" to serverNow
                        )
                    )
                }
            }

            repository.getWhatsAppBannerOnce()?.let { wb ->
                checkAndQueue(CloudSection.WHATSAPP_BANNER, listOf(wb.bannerText, wb.buttonText, wb.channelUrl, wb.imageUrl, wb.isVisible).hashCode()) {
                    listOf(
                        firestore.collection("app_config").document("whatsapp_banner") to hashMapOf<String, Any>(
                            "bannerText" to wb.bannerText, "buttonText" to wb.buttonText, "channelUrl" to wb.channelUrl,
                            "imageUrl" to wb.imageUrl, "isVisible" to wb.isVisible, "timestamp" to serverNow
                        )
                    )
                }
            }

            val currentDa = repository.getCurrentDaPercent()
            checkAndQueue(CloudSection.SALARY_RULES, currentDa.hashCode()) {
                listOf(
                    firestore.collection("salary_rules").document("current") to hashMapOf<String, Any>(
                        "daPercent" to currentDa, "ruralHra" to 8.0, "urbanHra" to 16.0,
                        "effectiveDate" to "01/11/2026", "publishedAt" to serverNow
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
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "batch_sync")
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
            val serverNow = FieldValue.serverTimestamp()

            if (!firestore.collection("school_config").document("timings").get().await().exists()) {
                val localTiming = repository.getSchoolTimingOnce() ?: SchoolTimingEntity()
                batch.set(
                    firestore.collection("school_config").document("timings"),
                    hashMapOf<String, Any>(
                        "openingTime" to localTiming.openingTime, "closingTime" to localTiming.closingTime,
                        "isManuallyClosed" to localTiming.isManuallyClosed, "closureReason" to localTiming.closureReason,
                        "updatedAt" to serverNow
                    )
                )
                opsCount++
            }

            if (!firestore.collection("salary_rules").document("current").get().await().exists()) {
                val currentDa = repository.getCurrentDaPercent()
                batch.set(
                    firestore.collection("salary_rules").document("current"),
                    hashMapOf<String, Any>(
                        "daPercent" to currentDa, "ruralHra" to 8.0, "urbanHra" to 16.0,
                        "effectiveDate" to "01/11/2026", "publishedAt" to serverNow
                    )
                )
                opsCount++
            }

            if (firestore.collection("feature_boxes").limit(1).get().await().isEmpty) {
                val localBoxes = repository.getAllFeatureBoxesOnce().ifEmpty { DEFAULT_FEATURE_BOXES }
                for (box in localBoxes) {
                    val safeBoxId = sanitizeDocId(box.id, "feature_${box.displayOrder}")
                    batch.set(
                        firestore.collection("feature_boxes").document(safeBoxId),
                        hashMapOf<String, Any>(
                            "id" to safeBoxId, "title" to box.title, "subtitle" to box.subtitle,
                            "iconType" to box.iconType, "destinationRoute" to box.destinationRoute,
                            "displayOrder" to box.displayOrder, "isVisible" to box.isVisible,
                            "isExternal" to box.isExternal, "externalUrl" to box.externalUrl, "timestamp" to serverNow
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
                        hashMapOf<String, Any>(
                            "id" to link.id, "title" to link.title, "url" to link.url,
                            "isVisible" to link.isVisible, "displayOrder" to link.displayOrder,
                            "lastUpdated" to System.currentTimeMillis(), "timestamp" to serverNow
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
                        hashMapOf<String, Any>(
                            "id" to c.id, "title" to c.title, "description" to c.description,
                            "phone" to c.phone, "email" to c.email, "linkUrl" to c.linkUrl,
                            "isVisible" to c.isVisible, "displayOrder" to c.displayOrder,
                            "lastUpdated" to System.currentTimeMillis(), "timestamp" to serverNow
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
                        hashMapOf<String, Any>(
                            "id" to s.id, "category" to s.category, "title" to s.title,
                            "description" to s.description, "applyUrl" to s.applyUrl, "isGoogleForm" to s.isGoogleForm,
                            "buttonText" to s.buttonText, "isVisible" to s.isVisible, "displayOrder" to s.displayOrder,
                            "lastUpdated" to System.currentTimeMillis(), "timestamp" to serverNow
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
                        hashMapOf<String, Any>(
                            "id" to f.id, "channelTitle" to f.channelTitle, "channelId" to f.channelId,
                            "feedUrl" to f.feedUrl, "isVisible" to f.isVisible,
                            "lastUpdated" to System.currentTimeMillis(), "timestamp" to serverNow
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
                        hashMapOf<String, Any>(
                            "id" to f.id, "title" to f.title, "feedUrl" to f.feedUrl,
                            "isVisible" to f.isVisible, "lastUpdated" to System.currentTimeMillis(), "timestamp" to serverNow
                        )
                    )
                    opsCount++
                }
            }

            if (!firestore.collection("app_config").document("top_three_links").get().await().exists()) {
                val l = repository.getTopThreeLinksOnce() ?: DEFAULT_TOP_THREE_LINKS
                batch.set(
                    firestore.collection("app_config").document("top_three_links"),
                    hashMapOf<String, Any>(
                        "link1Title" to l.link1Title, "link1Url" to l.link1Url, "link1Visible" to l.link1Visible,
                        "link2Title" to l.link2Title, "link2Url" to l.link2Url, "link2Visible" to l.link2Visible,
                        "link3Title" to l.link3Title, "link3Url" to l.link3Url, "link3Visible" to l.link3Visible,
                        "timestamp" to serverNow
                    )
                )
                opsCount++
            }

            if (!firestore.collection("app_config").document("financial_banner").get().await().exists()) {
                val fb = repository.getFinancialBannerOnce() ?: DEFAULT_FINANCIAL_BANNER
                batch.set(
                    firestore.collection("app_config").document("financial_banner"),
                    hashMapOf<String, Any>(
                        "heading" to fb.heading, "supportingText" to fb.supportingText, "disclaimer" to fb.disclaimer,
                        "buttonText" to fb.buttonText, "destinationType" to fb.destinationType,
                        "destinationUrl" to fb.destinationUrl, "imageUrl" to fb.imageUrl, "isVisible" to fb.isVisible, "timestamp" to serverNow
                    )
                )
                opsCount++
            }

            if (!firestore.collection("app_config").document("whatsapp_banner").get().await().exists()) {
                val wb = repository.getWhatsAppBannerOnce() ?: DEFAULT_WHATSAPP_BANNER
                batch.set(
                    firestore.collection("app_config").document("whatsapp_banner"),
                    hashMapOf<String, Any>(
                        "bannerText" to wb.bannerText, "buttonText" to wb.buttonText,
                        "channelUrl" to wb.channelUrl, "isVisible" to wb.isVisible, "timestamp" to serverNow
                    )
                )
                opsCount++
            }

            if (!firestore.collection("whatsapp_config").document("main").get().await().exists()) {
                val wa = repository.getWhatsAppDataOnce() ?: WhatsAppLinkConfigEntity(id = 1)
                batch.set(
                    firestore.collection("whatsapp_config").document("main"),
                    hashMapOf<String, Any>(
                        "channelUrl" to wa.channelUrl, "channelTitle" to wa.channelTitle,
                        "districtGroupsUrl" to wa.districtGroupsUrl, "districtGroupsTitle" to wa.districtGroupsTitle,
                        "updatedAt" to serverNow
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
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "bootstrap_collections")
        }
    }

    private val SAFE_DOC_ID_REGEX = Regex("^[a-zA-Z0-9_-]{1,128}$")

    internal fun isSafeDocId(id: String): Boolean = SAFE_DOC_ID_REGEX.matches(id)

    private fun sanitizeDocId(raw: String, fallback: String): String {
        val cleaned = raw.trim().replace(Regex("[^a-zA-Z0-9_-]+"), "_").trim('_').take(128)
        return cleaned.ifBlank { fallback }
    }

    private fun buildSafeDocId(prefix: String, rawSeed: String, hashSource: String): String {
        val cleanPrefix = prefix.replace(Regex("[^a-zA-Z0-9_-]+"), "_").trim('_').ifBlank { "doc" }
        val cleanSeed = rawSeed.trim()
            .replace(Regex("[^a-zA-Z0-9_-]+"), "_")
            .trim('_')
            .take(36)
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(hashSource.trim().lowercase().toByteArray(Charsets.UTF_8))
            .take(5)
            .joinToString("") { "%02x".format(it) }
        val parts = listOfNotNull(cleanPrefix, cleanSeed.ifBlank { null }, digest)
        return parts.joinToString("_").take(80)
    }

    internal fun resolveBannerDocId(banner: PromoBannerEntity): String {
        val trimmed = banner.bannerId.trim()
        return if (isSafeDocId(trimmed)) trimmed
        else buildSafeDocId("banner", trimmed.ifBlank { banner.title }, "${trimmed}|${banner.title}|${banner.imageUrl}")
    }

    internal fun getNoticeDocId(notice: NoticeEntity): String =
        if (notice.orderNumber.isNotBlank()) {
            buildSafeDocId("ord", notice.orderNumber, "${notice.orderNumber}|${notice.title}")
        } else {
            buildSafeDocId("not", notice.title, "${notice.title}|${notice.issueDate}|${notice.department}")
        }

    internal fun getHolidayDocId(holiday: HolidayEntity): String =
        buildSafeDocId("hol", "${holiday.dateStr}_${holiday.name}", "${holiday.dateStr}|${holiday.name}|${holiday.type}")

    internal fun getActivityDocId(activity: EducationActivityEntity): String =
        if (activity.orderNumber.isNotBlank()) {
            buildSafeDocId("act", activity.orderNumber, "${activity.orderNumber}|${activity.title}")
        } else {
            buildSafeDocId("act", activity.title, "${activity.title}|${activity.category}")
        }

    internal fun getPortalDocId(portal: QuickPortalEntity): String =
        buildSafeDocId("port", portal.title, "${portal.title}|${portal.url}")

    internal fun getVideoDocId(video: VideoTutorialEntity): String =
        if (video.youtubeId.isNotBlank()) {
            sanitizeDocId("vid_${video.youtubeId}", buildSafeDocId("vid", video.title, video.youtubeId))
        } else {
            "vid_${if (video.id > 0) video.id else System.currentTimeMillis()}"
        }

    internal fun getDownloadDocId(download: DownloadResourceEntity): String =
        com.example.core.DownloadSyncHelper.resolveCanonicalDocId(download)

    internal fun getNewsDocId(news: NewsArticleEntity): String =
        buildSafeDocId("news", news.title, "${news.title}|${news.sourceUrl}")

    internal fun getHomeTopLinkDocId(link: HomeTopLinkEntity): String =
        if (link.id > 0) "top_${link.id}"
        else buildSafeDocId("top", link.title, "${link.title}|${link.url}")

    internal fun getComplaintDocId(option: ComplaintOptionEntity): String =
        if (option.id > 0) "comp_${option.id}"
        else buildSafeDocId("comp", option.title, "${option.title}|${option.phone}|${option.email}")

    internal fun getFinancialServiceDocId(service: FinancialServiceEntity): String {
        val cat = sanitizeDocId(service.category.lowercase(), "svc").take(16)
        return if (service.id > 0) "fin_${cat}_${service.id}"
        else buildSafeDocId("fin_$cat", service.title, "${service.category}|${service.title}|${service.applyUrl}")
    }

    internal fun getYouTubeFeedDocId(feed: YouTubeFeedEntity): String =
        if (feed.id > 0) "yt_${feed.id}"
        else buildSafeDocId("yt", feed.channelTitle, "${feed.channelTitle}|${feed.feedUrl}")

    internal fun getNewsRssDocId(feed: NewsRssFeedEntity): String =
        if (feed.id > 0) "news_rss_${feed.id}"
        else buildSafeDocId("news_rss", feed.title, "${feed.title}|${feed.feedUrl}")
}
