package com.example.core

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import android.util.Xml
import com.example.database.entities.NewsArticleEntity
import com.example.database.entities.VideoTutorialEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object RssSyncCache {
    private const val PREFS_NAME = "shikshak_sathi_rss_prefs"
    private const val KEY_LAST_YT_SYNC_MS = "last_yt_rss_sync_ms"
    private const val KEY_LAST_NEWS_SYNC_MS = "last_news_rss_sync_ms"
    private const val KEY_ETAG_PREFIX = "etag_"
    private const val KEY_MOD_PREFIX = "mod_"
    const val AUTO_REFRESH_COOLDOWN_MS = 4L * 60L * 60L * 1000L // 4 hours

    @Volatile
    private var prefs: SharedPreferences? = null
    private val memoryCache = ConcurrentHashMap<String, String>()

    fun init(context: Context) {
        if (prefs == null) {
            synchronized(this) {
                if (prefs == null) {
                    prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                }
            }
        }
    }

    fun shouldAutoRefreshYouTube(hasCachedVideos: Boolean): Boolean {
        if (!hasCachedVideos) return true
        val last = prefs?.getLong(KEY_LAST_YT_SYNC_MS, 0L)
            ?: (memoryCache[KEY_LAST_YT_SYNC_MS]?.toLongOrNull() ?: 0L)
        return (System.currentTimeMillis() - last) >= AUTO_REFRESH_COOLDOWN_MS
    }

    fun markYouTubeSyncedNow() {
        val now = System.currentTimeMillis()
        memoryCache[KEY_LAST_YT_SYNC_MS] = now.toString()
        prefs?.edit()?.putLong(KEY_LAST_YT_SYNC_MS, now)?.apply()
    }

    fun shouldAutoRefreshNews(hasCachedRssNews: Boolean): Boolean {
        if (!hasCachedRssNews) return true
        val last = prefs?.getLong(KEY_LAST_NEWS_SYNC_MS, 0L)
            ?: (memoryCache[KEY_LAST_NEWS_SYNC_MS]?.toLongOrNull() ?: 0L)
        return (System.currentTimeMillis() - last) >= AUTO_REFRESH_COOLDOWN_MS
    }

    fun markNewsSyncedNow() {
        val now = System.currentTimeMillis()
        memoryCache[KEY_LAST_NEWS_SYNC_MS] = now.toString()
        prefs?.edit()?.putLong(KEY_LAST_NEWS_SYNC_MS, now)?.apply()
    }

    fun getETag(url: String): String? {
        val key = KEY_ETAG_PREFIX + url.hashCode()
        return memoryCache[key] ?: prefs?.getString(key, null)
    }

    fun getLastModified(url: String): String? {
        val key = KEY_MOD_PREFIX + url.hashCode()
        return memoryCache[key] ?: prefs?.getString(key, null)
    }

    fun saveHttpValidators(url: String, etag: String?, lastModified: String?) {
        if (etag.isNullOrBlank() && lastModified.isNullOrBlank()) return
        val editor = prefs?.edit()
        if (!etag.isNullOrBlank()) {
            val k = KEY_ETAG_PREFIX + url.hashCode()
            memoryCache[k] = etag
            editor?.putString(k, etag)
        }
        if (!lastModified.isNullOrBlank()) {
            val k = KEY_MOD_PREFIX + url.hashCode()
            memoryCache[k] = lastModified
            editor?.putString(k, lastModified)
        }
        editor?.apply()
    }
}

object GoogleDriveHelper {

    const val MAIN_TEACHER_DRIVE_FOLDER = "https://drive.google.com/drive/folders/10g2H4sSk6tjCu2KgEBT9_MiAvd7CM-2M"

    /**
     * Extracts Google Drive file ID if present in the given URL.
     */
    fun extractDriveFileId(url: String): String? {
        if (!url.contains("drive.google.com", ignoreCase = true)) return null

        val fileMatch = Regex("""/file/d/([a-zA-Z0-9_-]+)""").find(url)
        if (fileMatch != null) return fileMatch.groupValues[1]

        val idMatch = Regex("""[?&]id=([a-zA-Z0-9_-]+)""").find(url)
        if (idMatch != null) return idMatch.groupValues[1]

        val ucMatch = Regex("""/uc\?.*id=([a-zA-Z0-9_-]+)""").find(url)
        if (ucMatch != null) return ucMatch.groupValues[1]

        return null
    }

    /**
     * Returns a direct download link for Google Drive files.
     */
    fun getDirectDownloadUrl(url: String): String {
        val fileId = extractDriveFileId(url)
        return if (fileId != null) {
            "https://drive.google.com/uc?export=download&id=$fileId"
        } else {
            url
        }
    }

    /**
     * Returns a preview viewer link for Google Drive files.
     */
    fun getPreviewUrl(url: String): String {
        val fileId = extractDriveFileId(url)
        return if (fileId != null) {
            "https://drive.google.com/file/d/$fileId/preview"
        } else {
            url
        }
    }
}

object YouTubeRssHelper {
    private const val TAG = "YouTubeRssHelper"

    /**
     * Fetches and parses a YouTube channel URL or RSS feed.
     * Supports:
     * 1. Official YouTube RSS XML feeds (/feeds/videos.xml?channel_id=...)
     * 2. Direct channel URLs (https://m.youtube.com/channel/UC... or @shikshaksathibihar)
     * 3. Fallback to YouTube official oEmbed API (https://www.youtube.com/oembed?url=...)
     */
    suspend fun fetchVideosFromRssFeed(feedUrl: String, channelTitle: String): List<VideoTutorialEntity> = withContext(Dispatchers.IO) {
        val videos = mutableListOf<VideoTutorialEntity>()
        var resolvedFeedUrl = feedUrl.trim()

        // Guard against legacy dummy placeholder channel ID
        if (resolvedFeedUrl.contains("UCshikshakbihar123", ignoreCase = true)) {
            return@withContext emptyList()
        }

        // Normalize channel URLs to RSS if applicable
        if (resolvedFeedUrl.contains("youtube.com/channel/", ignoreCase = true) && !resolvedFeedUrl.contains("feeds/videos.xml", ignoreCase = true)) {
            val channelId = resolvedFeedUrl.substringAfter("channel/").substringBefore("/").substringBefore("?")
            if (channelId.isNotBlank()) {
                resolvedFeedUrl = "https://www.youtube.com/feeds/videos.xml?channel_id=$channelId"
            }
        }

        var rssReturnedNotFound = false

        // Try Method 1: RSS XML endpoint (with ETag / If-Modified-Since 304 support)
        if (resolvedFeedUrl.contains("feeds/videos.xml", ignoreCase = true)) {
            var conn: HttpURLConnection? = null
            try {
                val url = URL(resolvedFeedUrl)
                conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    RssSyncCache.getETag(resolvedFeedUrl)?.let { setRequestProperty("If-None-Match", it) }
                    RssSyncCache.getLastModified(resolvedFeedUrl)?.let { setRequestProperty("If-Modified-Since", it) }
                    instanceFollowRedirects = true
                }

                when (val code = conn.responseCode) {
                    HttpURLConnection.HTTP_NOT_MODIFIED -> {
                        return@withContext emptyList()
                    }
                    HttpURLConnection.HTTP_OK -> {
                        RssSyncCache.saveHttpValidators(
                            resolvedFeedUrl,
                            conn.getHeaderField("ETag"),
                            conn.getHeaderField("Last-Modified")
                        )
                        val parsed = conn.inputStream.use { parseRssXml(it, channelTitle) }
                        if (parsed.isNotEmpty()) {
                            videos.addAll(parsed)
                            return@withContext videos
                        }
                    }
                    HttpURLConnection.HTTP_NOT_FOUND, HttpURLConnection.HTTP_GONE -> {
                        rssReturnedNotFound = true
                        Log.w(TAG, "YouTube RSS returned $code for $resolvedFeedUrl; skipping HTML fallback")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "RSS XML parse attempt failed: ${e.message}")
            } finally {
                conn?.disconnect()
            }
        }

        // Method 2: HTML + oEmbed fallback ONLY for non-RSS URLs (e.g. @handle) or non-404 XML edge cases
        if (!rssReturnedNotFound && !resolvedFeedUrl.contains("feeds/videos.xml", ignoreCase = true)) {
            var conn: HttpURLConnection? = null
            try {
                val channelPageUrl = when {
                    feedUrl.contains("@shikshaksathibihar", ignoreCase = true) || feedUrl.contains("UCm1BZKUQgC2W5ixkKqfQ9Aw", ignoreCase = true) ->
                        "https://www.youtube.com/@shikshaksathibihar"
                    feedUrl.contains("channel_id=", ignoreCase = true) -> {
                        val chId = feedUrl.substringAfter("channel_id=").substringBefore("&")
                        "https://www.youtube.com/channel/$chId"
                    }
                    else -> feedUrl
                }

                conn = (URL(channelPageUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    instanceFollowRedirects = true
                }

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val html = conn.inputStream.bufferedReader().use { it.readText() }
                    val idMatches = Regex("""\"videoId\":\"([a-zA-Z0-9_-]{11})\"""").findAll(html)
                        .map { it.groupValues[1] }
                        .distinct()
                        .take(15)
                        .toList()

                    for (vid in idMatches) {
                        val details = fetchVideoDetailsViaOEmbed(vid, channelTitle)
                        if (details != null) {
                            videos.add(details)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Channel page fallback failed", e)
            } finally {
                conn?.disconnect()
            }
        }

        // If still empty and it's the shikshaksathibihar channel, ensure the minister update video is present
        if (videos.isEmpty() && (feedUrl.contains("UCm1BZKUQgC2W5ixkKqfQ9Aw") || feedUrl.contains("shikshaksathibihar"))) {
            videos.add(
                VideoTutorialEntity(
                    title = "Education minister of bihar shri mithilesh Tiwari new video #biharnews #biharpolitics #bihar #tre4",
                    category = "शिक्षक साथी बिहार",
                    youtubeUrl = "https://www.youtube.com/watch?v=nigmHzbhDt8",
                    youtubeId = "nigmHzbhDt8",
                    thumbnailUrl = "https://i.ytimg.com/vi/nigmHzbhDt8/hqdefault.jpg",
                    description = "शिक्षक साथी बिहार आधिकारिक चैनल (@shikshaksathibihar)",
                    duration = "01:15",
                    isPinned = true,
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        videos
    }

    private fun fetchVideoDetailsViaOEmbed(videoId: String, defaultChannelName: String): VideoTutorialEntity? {
        return try {
            val oembedUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json"
            val conn = (URL(oembedUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val json = JSONObject(jsonStr)
                val title = json.optString("title", "यूट्यूब वीडियो")
                val author = json.optString("author_name", defaultChannelName)
                val thumb = json.optString("thumbnail_url", "https://i.ytimg.com/vi/$videoId/hqdefault.jpg")

                VideoTutorialEntity(
                    title = title,
                    category = if (author.isNotBlank()) author else defaultChannelName,
                    youtubeUrl = "https://www.youtube.com/watch?v=$videoId",
                    youtubeId = videoId,
                    thumbnailUrl = thumb,
                    description = "$author द्वारा प्रकाशित वीडियो",
                    duration = "यूट्यूब",
                    isPinned = false,
                    timestamp = System.currentTimeMillis()
                )
            } else {
                conn.disconnect()
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseRssXml(inputStream: InputStream, defaultCategory: String): List<VideoTutorialEntity> {
        val items = mutableListOf<VideoTutorialEntity>()
        try {
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)

            var eventType = parser.eventType
            var inEntry = false
            var currentVideoId = ""
            var currentTitle = ""
            var currentDescription = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val name = parser.name ?: ""
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (name.equals("entry", ignoreCase = true)) {
                            inEntry = true
                            currentVideoId = ""
                            currentTitle = ""
                            currentDescription = ""
                        } else if (inEntry) {
                            when {
                                name.endsWith("videoId", ignoreCase = true) -> {
                                    currentVideoId = parser.nextText().trim()
                                }
                                name.equals("title", ignoreCase = true) -> {
                                    currentTitle = parser.nextText().trim()
                                }
                                name.endsWith("description", ignoreCase = true) -> {
                                    currentDescription = parser.nextText().trim()
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (name.equals("entry", ignoreCase = true)) {
                            inEntry = false
                            if (currentVideoId.isNotBlank() && currentTitle.isNotBlank()) {
                                items.add(
                                    VideoTutorialEntity(
                                        title = currentTitle,
                                        category = if (defaultCategory.isNotBlank()) defaultCategory else "यूट्यूब वीडियो",
                                        youtubeUrl = "https://www.youtube.com/watch?v=$currentVideoId",
                                        youtubeId = currentVideoId,
                                        thumbnailUrl = "https://img.youtube.com/vi/$currentVideoId/hqdefault.jpg",
                                        description = currentDescription.take(200),
                                        duration = "यूट्यूब",
                                        isPinned = false,
                                        timestamp = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "XML parse error in YouTube RSS", e)
        }
        return items
    }
}

object NewsRssHelper {
    private const val TAG = "NewsRssHelper"

    /**
     * Fetches and parses a Blogger / standard RSS 2.0 XML feed (e.g. The Magadh Times).
     */
    suspend fun fetchNewsFromRssFeed(feedUrl: String, sourceName: String): List<NewsArticleEntity> = withContext(Dispatchers.IO) {
        val articles = mutableListOf<NewsArticleEntity>()
        val targetUrl = feedUrl.trim()
        var conn: HttpURLConnection? = null
        try {
            val url = URL(targetUrl)
            conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 10000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                RssSyncCache.getETag(targetUrl)?.let { setRequestProperty("If-None-Match", it) }
                RssSyncCache.getLastModified(targetUrl)?.let { setRequestProperty("If-Modified-Since", it) }
                instanceFollowRedirects = true
            }

            when (val code = conn.responseCode) {
                HttpURLConnection.HTTP_NOT_MODIFIED -> {
                    // Feed unchanged on server; skip download & XML parsing
                }
                HttpURLConnection.HTTP_OK -> {
                    RssSyncCache.saveHttpValidators(
                        targetUrl,
                        conn.getHeaderField("ETag"),
                        conn.getHeaderField("Last-Modified")
                    )
                    articles.addAll(conn.inputStream.use { parseRss2Xml(it, sourceName) })
                }
                else -> {
                    Log.w(TAG, "News RSS fetch failed with HTTP $code for $targetUrl")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching News RSS from $targetUrl", e)
        } finally {
            conn?.disconnect()
        }
        articles
    }

    private fun parseRss2Xml(inputStream: InputStream, defaultSource: String): List<NewsArticleEntity> {
        val items = mutableListOf<NewsArticleEntity>()
        try {
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)

            var eventType = parser.eventType
            var inItem = false
            var currentTitle = ""
            var currentLink = ""
            var currentDesc = ""
            var currentPubDate = ""
            var currentThumb = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val name = parser.name ?: ""
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (name.equals("item", ignoreCase = true) || name.equals("entry", ignoreCase = true)) {
                            inItem = true
                            currentTitle = ""
                            currentLink = ""
                            currentDesc = ""
                            currentPubDate = ""
                            currentThumb = ""
                        } else if (inItem) {
                            when {
                                name.equals("title", ignoreCase = true) -> {
                                    currentTitle = parser.nextText().trim()
                                }
                                name.equals("link", ignoreCase = true) -> {
                                    val href = parser.getAttributeValue(null, "href")
                                    if (!href.isNullOrBlank()) {
                                        currentLink = href.trim()
                                    } else {
                                        val text = parser.nextText().trim()
                                        if (text.isNotBlank()) currentLink = text
                                    }
                                }
                                name.equals("description", ignoreCase = true) ||
                                        name.equals("content", ignoreCase = true) ||
                                        name.equals("summary", ignoreCase = true) -> {
                                    val text = parser.nextText().trim()
                                    if (text.isNotBlank() && (currentDesc.isBlank() || name.equals("content", ignoreCase = true))) {
                                        currentDesc = text
                                    }
                                }
                                name.equals("pubDate", ignoreCase = true) ||
                                        name.equals("published", ignoreCase = true) ||
                                        name.equals("updated", ignoreCase = true) -> {
                                    val text = parser.nextText().trim()
                                    if (text.isNotBlank() && currentPubDate.isBlank()) {
                                        currentPubDate = text
                                    }
                                }
                                name.endsWith("thumbnail", ignoreCase = true) || name.equals("enclosure", ignoreCase = true) -> {
                                    val urlAttr = parser.getAttributeValue(null, "url")
                                    if (!urlAttr.isNullOrBlank() && currentThumb.isBlank()) {
                                        currentThumb = urlAttr.trim()
                                    }
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (name.equals("item", ignoreCase = true) || name.equals("entry", ignoreCase = true)) {
                            inItem = false
                            if (currentTitle.isNotBlank()) {
                                // Strip HTML tags from description for clean reading
                                val cleanDesc = Regex("<[^>]*>").replace(currentDesc, " ")
                                    .replace("&nbsp;", " ")
                                    .replace("&amp;", "&")
                                    .replace("&quot;", "\"")
                                    .replace("&#39;", "'")
                                    .replace("&apos;", "'")
                                    .replace(Regex("\\s+"), " ")
                                    .trim()

                                // If thumb not set, check if an img src was in description
                                var thumb = currentThumb
                                if (thumb.isBlank()) {
                                    val imgMatch = Regex("""<img[^>]+src=["']([^"']+)["']""").find(currentDesc)
                                    if (imgMatch != null) {
                                        thumb = imgMatch.groupValues[1]
                                    }
                                }

                                val displayDate = formatPubDate(currentPubDate)

                                items.add(
                                    NewsArticleEntity(
                                        title = currentTitle,
                                        summary = cleanDesc.take(180) + if (cleanDesc.length > 180) "..." else "",
                                        content = cleanDesc,
                                        category = "लाइव RSS",
                                        publishedAt = displayDate,
                                        source = defaultSource,
                                        sourceUrl = if (currentLink.isNotBlank()) currentLink else "https://themagadhtimes.blogspot.com/",
                                        imageUrl = thumb,
                                        isVisible = true,
                                        timestamp = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "XML parse error in News RSS", e)
        }
        return items
    }

    private fun formatPubDate(rawDate: String): String {
        if (rawDate.isBlank()) {
            return SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(Date())
        }
        return try {
            if (rawDate.contains("T")) {
                val iso = rawDate.substringBefore(".")
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)
                val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale("hi", "IN"))
                val date = inputFormat.parse(iso)
                if (date != null) outputFormat.format(date) else rawDate.take(10)
            } else {
                val inputFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
                val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale("hi", "IN"))
                val date = inputFormat.parse(rawDate)
                if (date != null) outputFormat.format(date) else rawDate.take(16)
            }
        } catch (_: Exception) {
            rawDate.take(16)
        }
    }
}

object YouTubeUrlHelper {
    fun extractVideoId(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.length == 11 && !trimmed.contains("/") && !trimmed.contains(".") && !trimmed.contains("?")) {
            return trimmed
        }
        val patterns = listOf(
            Regex("""(?:v=|v%3D|/v/|youtu\.be/|/embed/|/shorts/)([\w-]{11})"""),
            Regex("""(?:youtube\.com/(?:watch\?.*v=|embed/|shorts/)|youtu\.be/)([\w-]{11})"""),
            Regex("""^([\w-]{11})$""")
        )
        for (pattern in patterns) {
            val match = pattern.find(trimmed)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1]
            }
        }
        return ""
    }

    fun getThumbnailUrl(videoId: String): String {
        return if (videoId.isNotBlank()) "https://img.youtube.com/vi/$videoId/hqdefault.jpg" else ""
    }

    fun getThumbnailFallback(videoId: String): String {
        return if (videoId.isNotBlank()) "https://img.youtube.com/vi/$videoId/mqdefault.jpg" else ""
    }

    /**
     * Directly opens the YouTube video or Shorts in the official YouTube app,
     * or gracefully falls back to the mobile web browser if YouTube app is not available.
     * Prevents in-app black screen player issues completely.
     */
    fun openYouTubeVideo(context: Context, rawUrlOrId: String) {
        if (rawUrlOrId.isBlank()) return
        val videoId = extractVideoId(rawUrlOrId)
        val isShorts = rawUrlOrId.contains("/shorts/", ignoreCase = true)

        val webUrl = when {
            videoId.isNotBlank() && isShorts -> "https://www.youtube.com/shorts/$videoId"
            videoId.isNotBlank() -> "https://www.youtube.com/watch?v=$videoId"
            rawUrlOrId.startsWith("http://") || rawUrlOrId.startsWith("https://") -> rawUrlOrId
            else -> "https://www.youtube.com/watch?v=$rawUrlOrId"
        }

        // 1. Try launching the native YouTube app via app Uri scheme or package
        var launched = false
        if (videoId.isNotBlank()) {
            try {
                val appUri = if (isShorts) {
                    Uri.parse("https://www.youtube.com/shorts/$videoId")
                } else {
                    Uri.parse("vnd.youtube:$videoId")
                }
                val appIntent = Intent(Intent.ACTION_VIEW, appUri).apply {
                    setPackage("com.google.android.youtube")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(appIntent)
                launched = true
            } catch (_: Exception) {
                // Secondary attempt without package constraint for vnd.youtube
                try {
                    val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(appIntent)
                    launched = true
                } catch (_: Exception) {}
            }
        }

        // 2. Fallback to web browser if YouTube app is not installed or failed to launch
        if (!launched) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            } catch (e: Exception) {
                Log.e("YouTubeUrlHelper", "Failed to open video URL: $webUrl", e)
            }
        }
    }
}
