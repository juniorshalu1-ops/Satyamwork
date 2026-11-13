package com.example.videos

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.DeepLinkHelper
import com.example.core.DeepLinkType
import com.example.core.YouTubeUrlHelper
import com.example.home.HomeViewModel
import com.example.ui.components.AppNetworkImage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.AppRepository
import com.example.database.entities.VideoTutorialEntity
import com.example.ui.theme.Gold500
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoLibraryScreen(
    repository: AppRepository,
    onBack: () -> Unit,
    homeViewModel: HomeViewModel? = null
) {
    val context = LocalContext.current
    val videos by repository.allVideos.collectAsState(initial = emptyList())
    val pendingDeepLink by (homeViewModel?.pendingDeepLink?.collectAsState()
        ?: remember { mutableStateOf(null) })

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("सभी") }

    LaunchedEffect(pendingDeepLink, videos) {
        val target = pendingDeepLink ?: return@LaunchedEffect
        if (target.type != DeepLinkType.VIDEO) return@LaunchedEffect
        val match = DeepLinkHelper.findMatchingVideo(videos, target)
        val urlToOpen = when {
            match != null -> match.youtubeUrl.ifBlank { match.youtubeId }
            target.targetUrl.isNotBlank() -> target.targetUrl
            target.contentId.isNotBlank() -> target.contentId
            else -> ""
        }
        if (match != null || target.targetUrl.isNotBlank()) {
            selectedCategory = "सभी"
            searchQuery = ""
            homeViewModel?.consumeDeepLink(target.eventId)
            if (urlToOpen.isNotBlank()) {
                YouTubeUrlHelper.openYouTubeVideo(context, urlToOpen)
            }
        }
    }

    val dynamicCategories = remember(videos) {
        val cats = linkedSetOf("सभी", "ई-शिक्षाकोष", "वेतन नियम", "प्रशिक्षण", "दीक्षा पोर्टल", "डीबीटी योजना")
        videos.forEach { v ->
            if (v.category.isNotBlank()) cats.add(v.category.trim())
        }
        cats.toList()
    }

    val filteredVideos = videos.filter { vid ->
        val matchesCat = selectedCategory == "सभी" || vid.category.equals(selectedCategory, ignoreCase = true)
        val matchesSearch = searchQuery.isBlank() ||
                vid.title.contains(searchQuery, ignoreCase = true) ||
                vid.description.contains(searchQuery, ignoreCase = true) ||
                vid.category.contains(searchQuery, ignoreCase = true)
        matchesCat && matchesSearch
    }.sortedWith(compareByDescending<VideoTutorialEntity> { it.isPinned }.thenByDescending { it.id })



    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "वीडियो लाइब्रेरी (Video Library)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                        Text(
                            text = "शैक्षिक ट्यूटोरियल एवं ई-शिक्षाकोष मार्गदर्शिका",
                            fontSize = 10.5.sp,
                            color = Color(0xFFA5B8CC)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PureWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy900),
                modifier = Modifier.testTag("video_library_topbar")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("video_library_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Search Input
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("वीडियो शीर्षक या विषय खोजें...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Navy800)
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Category Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(dynamicCategories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.5.sp) }
                        )
                    }
                }
            }

            // Results Count Banner
            item {
                Text(
                    text = "उपलब्ध वीडियो: ${filteredVideos.size}",
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (filteredVideos.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7FA))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("कोई वीडियो नहीं मिला।", fontSize = 13.sp, color = Color.Gray)
                        }
                    }
                }
            } else {
                items(
                    items = filteredVideos,
                    key = { it.id.takeIf { id -> id > 0L }?.toString() ?: it.youtubeId.ifBlank { it.youtubeUrl.ifBlank { it.title } } }
                ) { video ->
                    VideoCardItem(
                        video = video,
                        onPlayClick = {
                            val urlOrId = if (video.youtubeUrl.isNotBlank()) video.youtubeUrl else video.youtubeId
                            YouTubeUrlHelper.openYouTubeVideo(context, urlOrId)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun VideoCardItem(
    video: VideoTutorialEntity,
    onPlayClick: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlayClick() }
    ) {
        Column {
            // Real YouTube Thumbnail + Fallback
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(Color(0xFF0F2027)),
                contentAlignment = Alignment.Center
            ) {
                val thumbUrl = if (video.thumbnailUrl.isNotBlank()) {
                    video.thumbnailUrl
                } else if (video.youtubeId.isNotBlank()) {
                    YouTubeUrlHelper.getThumbnailUrl(video.youtubeId)
                } else {
                    ""
                }

                if (thumbUrl.isNotBlank()) {
                    AppNetworkImage(
                        imageUrl = thumbUrl,
                        contentDescription = video.title,
                        versionTag = video.timestamp,
                        showErrorText = false,
                        placeholderBgColor = Color(0xFF0F2027),
                        accentColor = Gold500,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF1E3A5F), Color(0xFF0F2027))
                                )
                            )
                    )
                }

                // Dark gradient overlay for readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0x88000000))
                            )
                        )
                )

                // YouTube Red Play Button
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xEEFF0000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Video",
                        tint = PureWhite,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Duration Tag
                Surface(
                    color = Color(0xCC000000),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = video.duration.ifBlank { "YouTube" },
                        color = PureWhite,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Category Tag & Pinned Badge
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = Gold500,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = video.category,
                            color = Navy900,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (video.isPinned) {
                        Surface(
                            color = Color(0xFFD32F2F),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = "Pinned",
                                    tint = PureWhite,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "पिन किया गया",
                                    color = PureWhite,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = video.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = Navy900,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (video.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = video.description,
                        fontSize = 11.5.sp,
                        color = Color.DarkGray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "▶ YouTube पर देखें",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC62828)
                    )

                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = Color(0xFFC62828),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * DIRECT YOUTUBE OPENER DIALOG FALLBACK
 * Launches YouTube App or Browser immediately without black screen
 */
@Composable
fun InAppYouTubePlayerDialog(
    video: VideoTutorialEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    androidx.compose.runtime.LaunchedEffect(video) {
        val target = if (video.youtubeUrl.isNotBlank()) video.youtubeUrl else video.youtubeId
        YouTubeUrlHelper.openYouTubeVideo(context, target)
        onDismiss()
    }
}
