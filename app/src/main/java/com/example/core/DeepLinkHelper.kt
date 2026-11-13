package com.example.core

import android.content.Intent
import com.example.database.entities.DownloadResourceEntity
import com.example.database.entities.HolidayEntity
import com.example.database.entities.NewsArticleEntity
import com.example.database.entities.NoticeEntity
import com.example.database.entities.VideoTutorialEntity

enum class DeepLinkType {
    NEWS,
    NOTICE,
    HOLIDAY,
    DOWNLOAD,
    VIDEO,
    HOME
}

data class DeepLinkTarget(
    val type: DeepLinkType,
    val contentId: String = "",
    val title: String = "",
    val targetUrl: String = "",
    val eventId: Long = 0L
)

object DeepLinkHelper {

    const val EXTRA_TARGET_SECTION = "TARGET_SECTION"
    const val EXTRA_CONTENT_TYPE = "EXTRA_CONTENT_TYPE"
    const val EXTRA_CONTENT_ID = "EXTRA_CONTENT_ID"
    const val EXTRA_CONTENT_TITLE = "EXTRA_CONTENT_TITLE"
    const val EXTRA_TARGET_URL = "TARGET_URL"
    const val EXTRA_DEEPLINK_EVENT_ID = "EXTRA_DEEPLINK_EVENT_ID"

    private val urlRegex = Regex("""https?://\S+""")

    fun extractFirstUrl(text: String): String {
        return urlRegex.find(text)?.value?.trimEnd('.', ',', ')', ']') ?: ""
    }

    fun toTargetSection(type: DeepLinkType): String {
        return when (type) {
            DeepLinkType.NEWS -> "news"
            DeepLinkType.NOTICE -> "notices"
            DeepLinkType.HOLIDAY -> "avakash"
            DeepLinkType.DOWNLOAD -> "downloads"
            DeepLinkType.VIDEO -> "videos"
            DeepLinkType.HOME -> "home"
        }
    }

    fun normalizeType(
        contentType: String = "",
        targetSection: String = "",
        sectionTitle: String = "",
        title: String = "",
        details: String = ""
    ): DeepLinkType {
        val candidates = listOf(contentType, targetSection, sectionTitle).map { it.trim().lowercase() }
        for (c in candidates) {
            if (c.isEmpty()) continue
            when {
                c == "news" || c == "news_feed" || c.contains("समाचार") || c.contains("न्यूज़") || c.contains("news") ->
                    return DeepLinkType.NEWS
                c == "notices" || c == "notice" || c == "orders" || c == "latest_orders" ||
                    c.contains("आदेश") || c.contains("नोटिस") || c.contains("पत्र") || c.contains("notice") ->
                    return DeepLinkType.NOTICE
                c == "avakash" || c == "holiday" || c == "holidays" || c == "holiday_calendar" ||
                    c.contains("अवकाश") || c.contains("holiday") || c.contains("छुट्टी") ->
                    return DeepLinkType.HOLIDAY
                c == "downloads" || c == "download" || c.contains("डाउनलोड") || c.contains("प्रपत्र") || c.contains("download") ->
                    return DeepLinkType.DOWNLOAD
                c == "videos" || c == "video" || c == "youtube" || c == "video_library" ||
                    c.contains("वीडियो") || c.contains("यूट्यूब") || c.contains("youtube") || c.contains("video") ->
                    return DeepLinkType.VIDEO
            }
        }
        val fallback = "$title $details".lowercase()
        return when {
            fallback.contains("youtube.com") || fallback.contains("youtu.be") -> DeepLinkType.VIDEO
            fallback.contains("डाउनलोड") || fallback.contains("प्रपत्र") -> DeepLinkType.DOWNLOAD
            fallback.contains("अवकाश") -> DeepLinkType.HOLIDAY
            fallback.contains("आदेश") || fallback.contains("ज्ञापांक") -> DeepLinkType.NOTICE
            fallback.contains("समाचार") -> DeepLinkType.NEWS
            else -> DeepLinkType.HOME
        }
    }

    fun writeToIntent(
        intent: Intent,
        type: DeepLinkType,
        contentId: String = "",
        title: String = "",
        targetUrl: String = "",
        eventId: Long = System.currentTimeMillis()
    ) {
        intent.putExtra(EXTRA_TARGET_SECTION, toTargetSection(type))
        intent.putExtra(EXTRA_CONTENT_TYPE, type.name.lowercase())
        if (contentId.isNotBlank()) intent.putExtra(EXTRA_CONTENT_ID, contentId.trim())
        if (title.isNotBlank()) intent.putExtra(EXTRA_CONTENT_TITLE, title.trim())
        if (targetUrl.isNotBlank()) intent.putExtra(EXTRA_TARGET_URL, targetUrl.trim())
        intent.putExtra(EXTRA_DEEPLINK_EVENT_ID, eventId)
    }

    fun parseIntent(intent: Intent?): DeepLinkTarget? {
        if (intent == null) return null
        val contentType = intent.getStringExtra(EXTRA_CONTENT_TYPE).orEmpty()
        val targetSection = intent.getStringExtra(EXTRA_TARGET_SECTION).orEmpty()
        val contentId = intent.getStringExtra(EXTRA_CONTENT_ID).orEmpty().trim()
        val title = intent.getStringExtra(EXTRA_CONTENT_TITLE).orEmpty().trim()
        val targetUrl = intent.getStringExtra(EXTRA_TARGET_URL).orEmpty().trim()
        val eventId = intent.getLongExtra(EXTRA_DEEPLINK_EVENT_ID, 0L)

        if (contentType.isBlank() && targetSection.isBlank() && contentId.isBlank() && targetUrl.isBlank()) {
            return null
        }

        val resolvedType = normalizeType(
            contentType = contentType,
            targetSection = targetSection,
            title = title,
            details = targetUrl
        )
        if (resolvedType == DeepLinkType.HOME && contentId.isBlank() && targetUrl.isBlank()) {
            return null
        }
        return DeepLinkTarget(
            type = resolvedType,
            contentId = contentId,
            title = title,
            targetUrl = targetUrl,
            eventId = if (eventId > 0L) eventId else System.currentTimeMillis()
        )
    }

    fun clearExtras(intent: Intent?) {
        intent ?: return
        intent.removeExtra(EXTRA_TARGET_SECTION)
        intent.removeExtra(EXTRA_CONTENT_TYPE)
        intent.removeExtra(EXTRA_CONTENT_ID)
        intent.removeExtra(EXTRA_CONTENT_TITLE)
        intent.removeExtra(EXTRA_TARGET_URL)
        intent.removeExtra(EXTRA_DEEPLINK_EVENT_ID)
    }

    fun findMatchingNews(list: List<NewsArticleEntity>, target: DeepLinkTarget): NewsArticleEntity? {
        if (list.isEmpty()) return null
        val cid = target.contentId.trim()
        val title = target.title.trim()
        val url = target.targetUrl.trim()

        if (cid.isNotBlank()) {
            cid.toLongOrNull()?.let { numericId ->
                list.find { it.id == numericId }?.let { return it }
            }
            list.find { it.sourceUrl.trim().equals(cid, ignoreCase = true) }?.let { return it }
            list.find { it.title.trim().equals(cid, ignoreCase = true) }?.let { return it }
        }
        if (url.isNotBlank()) {
            list.find { it.sourceUrl.trim().equals(url, ignoreCase = true) }?.let { return it }
        }
        if (title.isNotBlank()) {
            list.find { it.title.trim().equals(title, ignoreCase = true) }?.let { return it }
            list.find { it.title.contains(title, ignoreCase = true) || title.contains(it.title, ignoreCase = true) }?.let { return it }
        }
        return null
    }

    fun findMatchingNotice(list: List<NoticeEntity>, target: DeepLinkTarget): NoticeEntity? {
        if (list.isEmpty()) return null
        val cid = target.contentId.trim()
        val title = target.title.trim()
        val url = target.targetUrl.trim()

        if (cid.isNotBlank()) {
            cid.toLongOrNull()?.let { numericId ->
                list.find { it.id == numericId }?.let { return it }
            }
            list.find { it.orderNumber.isNotBlank() && it.orderNumber.trim().equals(cid, ignoreCase = true) }?.let { return it }
            list.find { it.pdfUrl.isNotBlank() && it.pdfUrl.trim().equals(cid, ignoreCase = true) }?.let { return it }
            list.find { it.title.trim().equals(cid, ignoreCase = true) }?.let { return it }
        }
        if (url.isNotBlank()) {
            list.find {
                it.pdfUrl.trim().equals(url, ignoreCase = true) ||
                    it.officialUrl.trim().equals(url, ignoreCase = true)
            }?.let { return it }
        }
        if (title.isNotBlank()) {
            list.find { it.title.trim().equals(title, ignoreCase = true) }?.let { return it }
            list.find { it.title.contains(title, ignoreCase = true) || title.contains(it.title, ignoreCase = true) }?.let { return it }
        }
        return null
    }

    fun buildHolidayContentId(holiday: HolidayEntity): String {
        return "${holiday.dateStr.trim()}|${holiday.name.trim()}"
    }

    fun findMatchingHoliday(list: List<HolidayEntity>, target: DeepLinkTarget): HolidayEntity? {
        if (list.isEmpty()) return null
        val cid = target.contentId.trim()
        val title = target.title.trim()

        if (cid.isNotBlank()) {
            cid.toLongOrNull()?.let { numericId ->
                list.find { it.id == numericId }?.let { return it }
            }
            list.find { buildHolidayContentId(it).equals(cid, ignoreCase = true) }?.let { return it }
            list.find { it.dateStr.trim().equals(cid, ignoreCase = true) }?.let { return it }
            list.find { it.name.trim().equals(cid, ignoreCase = true) }?.let { return it }
        }
        if (title.isNotBlank()) {
            list.find { it.name.trim().equals(title, ignoreCase = true) }?.let { return it }
            list.find { title.contains(it.name, ignoreCase = true) || it.name.contains(title, ignoreCase = true) }?.let { return it }
        }
        return null
    }

    fun findMatchingDownload(list: List<DownloadResourceEntity>, target: DeepLinkTarget): DownloadResourceEntity? {
        if (list.isEmpty()) return null
        val cid = target.contentId.trim()
        val title = target.title.trim()
        val url = target.targetUrl.trim()

        if (cid.isNotBlank()) {
            list.find { it.cloudId.isNotBlank() && it.cloudId.trim().equals(cid, ignoreCase = true) }?.let { return it }
            cid.toLongOrNull()?.let { numericId ->
                list.find { it.id == numericId }?.let { return it }
            }
            list.find { it.pdfUrl.isNotBlank() && it.pdfUrl.trim().equals(cid, ignoreCase = true) }?.let { return it }
            list.find { it.title.trim().equals(cid, ignoreCase = true) }?.let { return it }
        }
        if (url.isNotBlank()) {
            list.find { it.pdfUrl.trim().equals(url, ignoreCase = true) }?.let { return it }
        }
        if (title.isNotBlank()) {
            list.find { it.title.trim().equals(title, ignoreCase = true) }?.let { return it }
            list.find { it.title.contains(title, ignoreCase = true) || title.contains(it.title, ignoreCase = true) }?.let { return it }
        }
        return null
    }

    fun findMatchingVideo(list: List<VideoTutorialEntity>, target: DeepLinkTarget): VideoTutorialEntity? {
        if (list.isEmpty()) return null
        val cid = target.contentId.trim()
        val title = target.title.trim()
        val url = target.targetUrl.trim()

        if (cid.isNotBlank()) {
            list.find { it.youtubeId.isNotBlank() && it.youtubeId.trim().equals(cid, ignoreCase = true) }?.let { return it }
            cid.toLongOrNull()?.let { numericId ->
                list.find { it.id == numericId }?.let { return it }
            }
            list.find { it.youtubeUrl.trim().equals(cid, ignoreCase = true) }?.let { return it }
            list.find { it.title.trim().equals(cid, ignoreCase = true) }?.let { return it }
        }
        if (url.isNotBlank()) {
            list.find { it.youtubeUrl.trim().equals(url, ignoreCase = true) }?.let { return it }
        }
        if (title.isNotBlank()) {
            list.find { it.title.trim().equals(title, ignoreCase = true) }?.let { return it }
            list.find { it.title.contains(title, ignoreCase = true) || title.contains(it.title, ignoreCase = true) }?.let { return it }
        }
        return null
    }
}
