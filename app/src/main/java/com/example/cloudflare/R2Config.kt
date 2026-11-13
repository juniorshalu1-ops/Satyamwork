package com.example.cloudflare

import android.content.Context
import android.net.Uri
import com.example.BuildConfig

object R2Config {
    private const val PREFS_NAME = "shikshak_sathi_r2_prefs"
    private const val KEY_WORKER_BASE_URL = "worker_base_url"

    const val FOLDER_NOTICES = "notices"
    const val FOLDER_DOWNLOADS = "downloads"
    const val FOLDER_NEWS = "news"
    const val FOLDER_BANNERS = "banners"
    const val FOLDER_MANIFESTS = "manifests"

    fun getDefaultWorkerBaseUrl(): String =
        BuildConfig.R2_WORKER_BASE_URL.trim().trimEnd('/')

    fun getPublicCustomBaseUrl(): String =
        BuildConfig.R2_PUBLIC_CUSTOM_DOMAIN.trim().trimEnd('/')

    fun getWorkerBaseUrl(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawSaved = prefs.getString(KEY_WORKER_BASE_URL, null)
        val saved = if (!rawSaved.isNullOrBlank()) normalizeBaseUrl(rawSaved) else null
        return if (
            !saved.isNullOrBlank() &&
            !saved.equals("https://shikshak-sathi-r2-worker.workers.dev", ignoreCase = true) &&
            (saved.startsWith("https://") || saved.startsWith("http://"))
        ) {
            saved
        } else {
            getDefaultWorkerBaseUrl()
        }
    }

    fun saveWorkerBaseUrl(context: Context, rawUrl: String): String {
        val cleaned = normalizeBaseUrl(rawUrl)
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (cleaned.isBlank()) {
            prefs.edit().remove(KEY_WORKER_BASE_URL).apply()
            return getDefaultWorkerBaseUrl()
        }
        prefs.edit().putString(KEY_WORKER_BASE_URL, cleaned).apply()
        return cleaned
    }

    fun resetWorkerBaseUrl(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_WORKER_BASE_URL).apply()
    }

    fun normalizeBaseUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim().trimEnd('/')
        if (trimmed.isEmpty()) return ""
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
        return runCatching {
            val uri = Uri.parse(withScheme)
            val scheme = uri.scheme?.lowercase()?.takeIf { it == "http" || it == "https" } ?: "https"
            val authority = uri.encodedAuthority?.trim().orEmpty()
            if (authority.isBlank()) {
                withScheme
            } else {
                val rawPath = uri.encodedPath.orEmpty().trimEnd('/')
                val cleanedPath = when {
                    rawPath.isEmpty() ||
                        rawPath.equals("/health", ignoreCase = true) ||
                        rawPath.startsWith("/api/", ignoreCase = true) ||
                        rawPath.startsWith("/files/", ignoreCase = true) ||
                        rawPath.equals("/files", ignoreCase = true) -> ""
                    else -> rawPath
                }
                "$scheme://$authority$cleanedPath"
            }
        }.getOrDefault(withScheme)
    }

    fun isR2ManagedUrl(context: Context, rawUrl: String): Boolean {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return false
        val workerHost = runCatching { Uri.parse(getWorkerBaseUrl(context)).host?.lowercase() }.getOrNull()
        val customPublicHost = getPublicCustomBaseUrl()
            .takeIf { it.isNotBlank() }
            ?.let { runCatching { Uri.parse(normalizeBaseUrl(it)).host?.lowercase() }.getOrNull() }
        val targetUri = runCatching { Uri.parse(trimmed) }.getOrNull() ?: return false
        val targetHost = targetUri.host?.lowercase() ?: return false
        val path = targetUri.path ?: ""

        if (targetHost.endsWith(".r2.dev") && path.length > 1) {
            return true
        }
        if (customPublicHost != null && targetHost == customPublicHost && path.length > 1) {
            return true
        }
        if (workerHost != null && targetHost == workerHost && path.startsWith("/files/")) {
            return true
        }
        if (targetHost.endsWith(".workers.dev") && path.startsWith("/files/")) {
            return true
        }
        return false
    }

    fun extractObjectKey(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return ""
        val uri = runCatching { Uri.parse(trimmed) }.getOrNull() ?: return ""
        val path = uri.path ?: return ""
        val host = uri.host?.lowercase().orEmpty()
        val customHost = getPublicCustomBaseUrl()
            .takeIf { it.isNotBlank() }
            ?.let { runCatching { Uri.parse(normalizeBaseUrl(it)).host?.lowercase() }.getOrNull() }
        return when {
            path.startsWith("/files/") -> path.removePrefix("/files/").trimStart('/')
            host.endsWith(".r2.dev") -> path.trimStart('/')
            customHost != null && host == customHost -> path.trimStart('/')
            else -> ""
        }
    }
}
