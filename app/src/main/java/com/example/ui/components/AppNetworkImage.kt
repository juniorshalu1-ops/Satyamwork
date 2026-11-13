package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.CachePolicy
import coil.request.ImageRequest

object ImageUrlHelper {

    private val driveFileRegex = Regex("""https?://drive\.google\.com/file/d/([a-zA-Z0-9_-]+)""")
    private val driveIdParamRegex = Regex("""[?&]id=([a-zA-Z0-9_-]+)""")

    fun normalizeImageUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return ""

        val cleaned = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed.replace(" ", "%20")
        } else if (trimmed.startsWith("www.")) {
            "https://$trimmed".replace(" ", "%20")
        } else {
            trimmed
        }

        if (cleaned.contains("drive.google.com", ignoreCase = true)) {
            val fileMatch = driveFileRegex.find(cleaned)
            val id = fileMatch?.groupValues?.getOrNull(1)
                ?: driveIdParamRegex.find(cleaned)?.groupValues?.getOrNull(1)
            if (!id.isNullOrBlank()) {
                return "https://drive.google.com/uc?export=view&id=$id"
            }
        }
        return cleaned
    }

    fun buildCacheKey(rawUrl: String, versionTag: Long = 0L): String {
        val normalized = normalizeImageUrl(rawUrl)
        if (normalized.isEmpty()) return ""
        return if (versionTag > 0L) "$normalized#v=$versionTag" else normalized
    }

    fun isLikelyImageUrl(url: String, fileType: String = ""): Boolean {
        if (fileType.equals("JPG", ignoreCase = true) ||
            fileType.equals("JPEG", ignoreCase = true) ||
            fileType.equals("PNG", ignoreCase = true) ||
            fileType.equals("WEBP", ignoreCase = true)
        ) {
            return url.isNotBlank()
        }
        val lower = url.trim().lowercase()
        return lower.endsWith(".jpg") ||
            lower.endsWith(".jpeg") ||
            lower.endsWith(".png") ||
            lower.endsWith(".webp") ||
            lower.endsWith(".gif")
    }
}

@Composable
fun AppNetworkImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    versionTag: Long = 0L,
    showErrorText: Boolean = true,
    placeholderBgColor: Color = Color(0xFFF1F5F9),
    accentColor: Color = Color(0xFF1E3A5F)
) {
    val context = LocalContext.current
    val normalizedUrl = remember(imageUrl) { ImageUrlHelper.normalizeImageUrl(imageUrl) }
    val cacheKey = remember(normalizedUrl, versionTag) {
        ImageUrlHelper.buildCacheKey(normalizedUrl, versionTag)
    }

    if (normalizedUrl.isBlank()) {
        ImageErrorPlaceholder(
            modifier = modifier,
            bgColor = placeholderBgColor,
            showErrorText = showErrorText,
            label = "इमेज उपलब्ध नहीं"
        )
        return
    }

    val request = remember(normalizedUrl, cacheKey) {
        ImageRequest.Builder(context)
            .data(normalizedUrl)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .build()
    }

    SubcomposeAsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
    ) {
        when (painter.state) {
            is AsyncImagePainter.State.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(placeholderBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = accentColor
                    )
                }
            }
            is AsyncImagePainter.State.Error -> {
                ImageErrorPlaceholder(
                    modifier = Modifier.fillMaxSize(),
                    bgColor = placeholderBgColor,
                    showErrorText = showErrorText,
                    label = "इमेज लोड नहीं हुई"
                )
            }
            else -> {
                SubcomposeAsyncImageContent()
            }
        }
    }
}

@Composable
private fun ImageErrorPlaceholder(
    modifier: Modifier = Modifier,
    bgColor: Color = Color(0xFFF1F5F9),
    showErrorText: Boolean = true,
    label: String = "इमेज लोड नहीं हुई"
) {
    Box(
        modifier = modifier.background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.BrokenImage,
                contentDescription = label,
                tint = Color(0xFF90A4AE),
                modifier = Modifier.size(28.dp)
            )
            if (showErrorText) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = Color(0xFF607D8B),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
