package com.example.cloudflare

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import okio.source
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

data class R2UploadResult(
    val url: String,
    val key: String,
    val sizeBytes: Long,
    val sizeFormatted: String,
    val fileType: String,
    val contentType: String,
    val version: Long
)

data class R2FileItem(
    val key: String,
    val url: String,
    val sizeBytes: Long,
    val sizeFormatted: String,
    val uploaded: String,
    val contentType: String,
    val title: String
)

data class R2HealthStatus(
    val ok: Boolean,
    val projectId: String,
    val publicR2Configured: Boolean
)

class R2HttpException(
    val statusCode: Int,
    message: String
) : IOException(message)

object R2StorageRepository {
    private const val TAG = "R2StorageRepository"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    suspend fun checkHealth(context: Context): Result<R2HealthStatus> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = R2Config.getWorkerBaseUrl(context)
            val request = Request.Builder()
                .url("$baseUrl/health")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        R2HttpException(
                            response.code,
                            "Worker HTTP ${response.code}: ${extractErrorMessage(bodyStr)}"
                        )
                    )
                }
                val json = JSONObject(bodyStr)
                Result.success(
                    R2HealthStatus(
                        ok = json.optBoolean("ok", false),
                        projectId = json.optString("projectId", ""),
                        publicR2Configured = json.optBoolean("publicR2Configured", false)
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "checkHealth failed", e)
            Result.failure(e)
        }
    }

    suspend fun uploadUri(
        context: Context,
        uri: Uri,
        folder: String,
        title: String,
        idToken: String,
        replaceUrl: String = "",
        onProgress: (Float) -> Unit = {}
    ): Result<R2UploadResult> = withContext(Dispatchers.IO) {
        try {
            if (idToken.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Admin Google Sign-In टोकन उपलब्ध नहीं है।"))
            }

            val resolver = context.contentResolver
            val meta = resolveUriMetadata(context, uri)
            val mimeType = meta.mimeType
            val fileName = meta.fileName
            val totalBytes = meta.sizeBytes

            val mediaType = mimeType.toMediaTypeOrNull() ?: "application/octet-stream".toMediaTypeOrNull()
            val fileBody = object : RequestBody() {
                override fun contentType(): MediaType? = mediaType

                override fun contentLength(): Long = if (totalBytes > 0L) totalBytes else -1L

                override fun writeTo(sink: BufferedSink) {
                    val input = resolver.openInputStream(uri)
                        ?: throw IOException("चयनित फाइल पढ़ी नहीं जा सकी")
                    input.use { stream ->
                        val source = stream.source()
                        var bytesWritten = 0L
                        val buffer = okio.Buffer()
                        var read: Long
                        while (source.read(buffer, 16L * 1024L).also { read = it } != -1L) {
                            sink.write(buffer, read)
                            bytesWritten += read
                            if (totalBytes > 0L) {
                                val fraction = (bytesWritten.toFloat() / totalBytes.toFloat()).coerceIn(0.02f, 0.96f)
                                onProgress(fraction)
                            }
                        }
                    }
                }
            }

            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", fileName, fileBody)
                .addFormDataPart("folder", folder)
                .addFormDataPart("title", title.ifBlank { fileName })

            val trimmedReplace = replaceUrl.trim()
            if (trimmedReplace.isNotBlank() && R2Config.isR2ManagedUrl(context, trimmedReplace)) {
                multipart.addFormDataPart("replaceUrl", trimmedReplace)
            }

            val baseUrl = R2Config.getWorkerBaseUrl(context)
            val request = Request.Builder()
                .url("$baseUrl/api/admin/upload")
                .header("Authorization", "Bearer $idToken")
                .post(multipart.build())
                .build()

            onProgress(0.05f)
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val err = extractErrorMessage(bodyStr)
                    return@withContext Result.failure(
                        R2HttpException(
                            response.code,
                            "अपलोड विफल (HTTP ${response.code}): $err"
                        )
                    )
                }

                val json = JSONObject(bodyStr)
                if (!json.optBoolean("success", false)) {
                    return@withContext Result.failure(
                        IOException(json.optString("error", "अपलोड असफल"))
                    )
                }

                onProgress(1f)
                val size = json.optLong("size", totalBytes.coerceAtLeast(0L))
                val resolvedType = json.optString("fileType", inferTypeTag(mimeType, fileName))
                Result.success(
                    R2UploadResult(
                        url = json.getString("url"),
                        key = json.getString("key"),
                        sizeBytes = size,
                        sizeFormatted = formatBytes(size),
                        fileType = resolvedType,
                        contentType = json.optString("contentType", mimeType),
                        version = json.optLong("version", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "R2 upload failed", e)
            Result.failure(e)
        }
    }

    suspend fun deleteByUrl(
        context: Context,
        url: String,
        idToken: String
    ): Result<Boolean> {
        val trimmed = url.trim()
        if (trimmed.isBlank() || !R2Config.isR2ManagedUrl(context, trimmed)) {
            return Result.success(false)
        }
        return deleteByUrlOrKey(context, trimmed, idToken)
    }

    suspend fun deleteByUrlOrKey(
        context: Context,
        urlOrKey: String,
        idToken: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val trimmed = urlOrKey.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.success(false)
        }
        val isHttpUrl = trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        if (isHttpUrl && !R2Config.isR2ManagedUrl(context, trimmed)) {
            return@withContext Result.success(false)
        }
        if (idToken.isBlank()) {
            return@withContext Result.failure(R2HttpException(401, "Missing auth token"))
        }

        try {
            val baseUrl = R2Config.getWorkerBaseUrl(context)
            val payload = JSONObject().apply {
                if (isHttpUrl) put("url", trimmed) else put("key", trimmed)
            }.toString()

            val request = Request.Builder()
                .url("$baseUrl/api/admin/delete")
                .header("Authorization", "Bearer $idToken")
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        R2HttpException(
                            response.code,
                            "Delete HTTP ${response.code}: ${extractErrorMessage(bodyStr)}"
                        )
                    )
                }
                val json = JSONObject(bodyStr)
                Result.success(json.optBoolean("success", false))
            }
        } catch (e: Exception) {
            Log.w(TAG, "R2 delete failed for $trimmed", e)
            Result.failure(e)
        }
    }

    suspend fun listFiles(
        context: Context,
        idToken: String,
        prefix: String = ""
    ): Result<List<R2FileItem>> = withContext(Dispatchers.IO) {
        if (idToken.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Missing auth token"))
        }
        try {
            val baseUrl = R2Config.getWorkerBaseUrl(context)
            val query = buildString {
                append("?limit=100")
                if (prefix.isNotBlank()) {
                    append("&prefix=").append(URLEncoder.encode(prefix, "UTF-8"))
                }
            }
            val request = Request.Builder()
                .url("$baseUrl/api/admin/files$query")
                .header("Authorization", "Bearer $idToken")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        R2HttpException(
                            response.code,
                            "List HTTP ${response.code}: ${extractErrorMessage(bodyStr)}"
                        )
                    )
                }
                val json = JSONObject(bodyStr)
                val arr = json.optJSONArray("files")
                val list = mutableListOf<R2FileItem>()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val size = obj.optLong("size", 0L)
                        val customMeta = obj.optJSONObject("customMetadata")
                        list.add(
                            R2FileItem(
                                key = obj.optString("key"),
                                url = obj.optString("url"),
                                sizeBytes = size,
                                sizeFormatted = formatBytes(size),
                                uploaded = obj.optString("uploaded", ""),
                                contentType = obj.optString("contentType", ""),
                                title = customMeta?.optString("title").orEmpty()
                            )
                        )
                    }
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Log.w(TAG, "listFiles failed", e)
            Result.failure(e)
        }
    }

    suspend fun publishManifest(
        context: Context,
        name: String,
        jsonData: JSONObject,
        idToken: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (idToken.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Missing auth token"))
        }
        try {
            val baseUrl = R2Config.getWorkerBaseUrl(context)
            val payload = JSONObject().apply {
                put("name", name)
                put("data", jsonData)
            }.toString()

            val request = Request.Builder()
                .url("$baseUrl/api/admin/publish-manifest")
                .header("Authorization", "Bearer $idToken")
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        R2HttpException(
                            response.code,
                            "Manifest HTTP ${response.code}: ${extractErrorMessage(bodyStr)}"
                        )
                    )
                }
                val json = JSONObject(bodyStr)
                Result.success(json.optString("url", ""))
            }
        } catch (e: Exception) {
            Log.w(TAG, "publishManifest failed", e)
            Result.failure(e)
        }
    }

    private data class UriMetadata(
        val fileName: String,
        val mimeType: String,
        val sizeBytes: Long
    )

    private fun resolveUriMetadata(context: Context, uri: Uri): UriMetadata {
        val resolver = context.contentResolver
        var displayName = ""
        var size = -1L

        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) displayName = cursor.getString(nameIdx).orEmpty()
                if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) size = cursor.getLong(sizeIdx)
            }
        }

        val mime = resolver.getType(uri)?.lowercase(Locale.ENGLISH) ?: when {
            displayName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
            displayName.endsWith(".png", ignoreCase = true) -> "image/png"
            displayName.endsWith(".webp", ignoreCase = true) -> "image/webp"
            else -> "image/jpeg"
        }

        if (displayName.isBlank()) {
            val ext = when {
                mime.contains("pdf") -> "pdf"
                mime.contains("png") -> "png"
                mime.contains("webp") -> "webp"
                else -> "jpg"
            }
            displayName = "document_${System.currentTimeMillis()}.$ext"
        }

        return UriMetadata(fileName = displayName, mimeType = mime, sizeBytes = size)
    }

    private fun inferTypeTag(mimeType: String, fileName: String): String {
        val lowerName = fileName.lowercase(Locale.ENGLISH)
        return when {
            mimeType.contains("pdf") || lowerName.endsWith(".pdf") -> "PDF"
            mimeType.contains("png") || lowerName.endsWith(".png") -> "PNG"
            mimeType.contains("webp") || lowerName.endsWith(".webp") -> "WEBP"
            else -> "JPG"
        }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "— KB"
        val kb = bytes / 1024.0
        return if (kb < 1024.0) {
            String.format(Locale.ENGLISH, "%.0f KB", kb.coerceAtLeast(1.0))
        } else {
            String.format(Locale.ENGLISH, "%.2f MB", kb / 1024.0)
        }
    }

    private fun extractErrorMessage(bodyStr: String): String {
        if (bodyStr.isBlank()) return "नेटवर्क या सर्वर त्रुटि"
        return runCatching {
            val json = JSONObject(bodyStr)
            json.optString("error").ifBlank { bodyStr.take(120) }
        }.getOrDefault(bodyStr.take(120))
    }
}
