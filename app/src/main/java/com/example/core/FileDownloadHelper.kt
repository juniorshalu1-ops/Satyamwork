package com.example.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.cloudflare.R2Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit

object FileDownloadHelper {
    private const val TAG = "FileDownloadHelper"
    private const val CACHE_SUBDIR = "r2_docs_cache"
    private const val HTTP_CACHE_DIR = "r2_http_cache"
    private const val MAX_DOC_CACHE_BYTES = 75L * 1024L * 1024L

    @Volatile
    private var cachedClient: OkHttpClient? = null

    private fun getHttpClient(context: Context): OkHttpClient {
        return cachedClient ?: synchronized(this) {
            cachedClient ?: run {
                val httpCacheDir = File(context.applicationContext.cacheDir, HTTP_CACHE_DIR)
                OkHttpClient.Builder()
                    .cache(Cache(httpCacheDir, 25L * 1024L * 1024L))
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(45, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build()
                    .also { cachedClient = it }
            }
        }
    }

    fun isDirectMediaFileUrl(context: Context, rawUrl: String): Boolean {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return false
        if (GoogleDriveHelper.extractDriveFileId(trimmed) != null) return false
        if (R2Config.isR2ManagedUrl(context, trimmed)) return true
        val lowerPath = runCatching { Uri.parse(trimmed).path?.lowercase(Locale.ENGLISH) }.getOrNull().orEmpty()
        return lowerPath.endsWith(".pdf") ||
            lowerPath.endsWith(".jpg") ||
            lowerPath.endsWith(".jpeg") ||
            lowerPath.endsWith(".png") ||
            lowerPath.endsWith(".webp")
    }

    suspend fun openDocumentFast(
        context: Context,
        rawUrl: String,
        fallbackTypeHint: String = "PDF"
    ) {
        val cleanedUrl = rawUrl.trim()
        if (cleanedUrl.isBlank()) return

        if (!isDirectMediaFileUrl(context, cleanedUrl)) {
            openInBrowser(context, cleanedUrl)
            return
        }

        val result = withContext(Dispatchers.IO) {
            fetchAndCacheLocally(context, cleanedUrl, fallbackTypeHint)
        }

        result.onSuccess { (file, mimeType) ->
            try {
                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(contentUri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(viewIntent)
            } catch (e: Exception) {
                Log.w(TAG, "No local viewer for $mimeType; falling back to browser", e)
                openInBrowser(context, cleanedUrl)
            }
        }.onFailure {
            Toast.makeText(context, "ब्राउज़र में खोला जा रहा है...", Toast.LENGTH_SHORT).show()
            openInBrowser(context, cleanedUrl)
        }
    }

    private fun fetchAndCacheLocally(
        context: Context,
        url: String,
        fallbackTypeHint: String
    ): Result<Pair<File, String>> {
        return try {
            val cacheFolder = File(context.applicationContext.cacheDir, CACHE_SUBDIR).apply {
                if (!exists()) mkdirs()
            }
            val urlHash = sha256Short(url)
            val ext = inferExtension(url, fallbackTypeHint)
            val targetFile = File(cacheFolder, "doc_$urlHash.$ext")
            val mime = mimeForExtension(ext)

            if (targetFile.exists() && targetFile.length() > 0L) {
                targetFile.setLastModified(System.currentTimeMillis())
                return Result.success(targetFile to mime)
            }

            val client = getHttpClient(context)
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(IllegalStateException("HTTP ${response.code}"))
                }
                val respType = response.header("Content-Type")?.lowercase(Locale.ENGLISH).orEmpty()
                val actualExt = when {
                    respType.contains("pdf") -> "pdf"
                    respType.contains("png") -> "png"
                    respType.contains("webp") -> "webp"
                    respType.contains("jpeg") || respType.contains("jpg") -> "jpg"
                    else -> ext
                }
                val finalFile = if (actualExt == ext) targetFile else File(cacheFolder, "doc_$urlHash.$actualExt")
                val tmpFile = File(cacheFolder, "tmp_${urlHash}_${System.currentTimeMillis()}.$actualExt")

                val bodyStream = response.body?.byteStream()
                    ?: return Result.failure(IllegalStateException("Empty response body"))

                bodyStream.use { input ->
                    tmpFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                if (tmpFile.length() <= 0L) {
                    tmpFile.delete()
                    return Result.failure(IllegalStateException("Empty downloaded file"))
                }
                if (!tmpFile.renameTo(finalFile)) {
                    tmpFile.copyTo(finalFile, overwrite = true)
                    tmpFile.delete()
                }
                finalFile.setLastModified(System.currentTimeMillis())
                pruneCacheIfNeeded(cacheFolder)
                Result.success(finalFile to mimeForExtension(actualExt))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cache document locally: $url", e)
            Result.failure(e)
        }
    }

    private fun pruneCacheIfNeeded(cacheFolder: File) {
        val files = cacheFolder.listFiles()?.filter { it.isFile } ?: return
        var totalSize = files.sumOf { it.length() }
        if (totalSize <= MAX_DOC_CACHE_BYTES) return

        val oldestFirst = files.sortedBy { it.lastModified() }
        for (file in oldestFirst) {
            if (totalSize <= (MAX_DOC_CACHE_BYTES * 80) / 100) break
            val size = file.length()
            if (file.delete()) {
                totalSize -= size
            }
        }
    }

    private fun openInBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open URL in browser: $url", e)
        }
    }

    private fun inferExtension(url: String, fallbackHint: String): String {
        val path = runCatching { Uri.parse(url).path?.lowercase(Locale.ENGLISH) }.getOrNull().orEmpty()
        return when {
            path.endsWith(".pdf") -> "pdf"
            path.endsWith(".png") -> "png"
            path.endsWith(".webp") -> "webp"
            path.endsWith(".jpg") || path.endsWith(".jpeg") -> "jpg"
            fallbackHint.equals("JPG", ignoreCase = true) || fallbackHint.equals("JPEG", ignoreCase = true) -> "jpg"
            fallbackHint.equals("PNG", ignoreCase = true) -> "png"
            else -> "pdf"
        }
    }

    private fun mimeForExtension(ext: String): String = when (ext.lowercase(Locale.ENGLISH)) {
        "pdf" -> "application/pdf"
        "png" -> "image/png"
        "webp" -> "image/webp"
        else -> "image/jpeg"
    }

    private fun sha256Short(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return digest.take(12).joinToString("") { "%02x".format(it) }
    }
}
