package com.example.news

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import com.example.core.DeepLinkHelper
import com.example.core.DeepLinkType
import com.example.ui.components.AppNetworkImage
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import android.widget.Toast
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.LocalAppLanguage
import com.example.database.entities.NewsArticleEntity
import com.example.home.HomeViewModel
import com.example.ui.theme.Gold100
import com.example.ui.theme.Gold500
import com.example.ui.theme.Gold700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Teal700

@Composable
fun NewsFeedScreen(
    homeViewModel: HomeViewModel,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    val isHi = language.isHindi()

    val allNews by homeViewModel.newsArticles.collectAsState()
    val pendingDeepLink by homeViewModel.pendingDeepLink.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("सभी") }
    var selectedSourceTab by remember { mutableIntStateOf(0) } // 0: All, 1: Admin Editorial, 2: The Magadh Times Live RSS
    var viewingArticle by remember { mutableStateOf<NewsArticleEntity?>(null) }

    LaunchedEffect(pendingDeepLink, allNews) {
        val target = pendingDeepLink ?: return@LaunchedEffect
        if (target.type != DeepLinkType.NEWS) return@LaunchedEffect
        val match = DeepLinkHelper.findMatchingNews(allNews, target)
        if (match != null) {
            selectedSourceTab = 0
            selectedCategory = "सभी"
            searchQuery = ""
            viewingArticle = match
            homeViewModel.consumeDeepLink(target.eventId)
        }
    }

    val categories = listOf("सभी", "मगध टाइम्स RSS", "विभागीय", "वेतन", "नियुक्ति", "स्थानांतरण", "अवकाश", "सक्षमता")

    val filteredNews = allNews.filter { article ->
        val isRss = article.category.contains("RSS", ignoreCase = true) ||
                article.source.contains("Magadh", ignoreCase = true) ||
                article.source.contains("मगध", ignoreCase = true)
        val matchesSource = when (selectedSourceTab) {
            1 -> !isRss
            2 -> isRss
            else -> true
        }
        val matchesCategory = selectedCategory == "सभी" || article.category.contains(selectedCategory, ignoreCase = true)
        val matchesQuery = searchQuery.isBlank() ||
                article.title.contains(searchQuery, ignoreCase = true) ||
                article.summary.contains(searchQuery, ignoreCase = true) ||
                article.category.contains(searchQuery, ignoreCase = true) ||
                article.source.contains(searchQuery, ignoreCase = true)
        matchesSource && matchesCategory && matchesQuery
    }.sortedWith(
        compareByDescending<NewsArticleEntity> { it.isPinned }
            .thenBy { it.pinOrder }
            .thenByDescending { it.isBreaking }
            .thenByDescending { it.timestamp }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6F9))
            .testTag("news_feed_screen")
    ) {
        // Top Header
        Surface(
            color = Navy900,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "पीछे जाएँ",
                                tint = PureWhite
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Gold500, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Newspaper,
                            contentDescription = null,
                            tint = Navy900,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isHi) "शिक्षा समाचार एवं अपडेट" else "Bihar Education News",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                        Text(
                            text = if (isHi) "शिक्षा विभाग, बिहार के नवीनतम निर्णय व समाचार" else "Latest Departmental Notices & Bulletins",
                            fontSize = 11.sp,
                            color = Color(0xFFA5B8CC)
                        )
                    }

                    IconButton(
                        onClick = {
                            homeViewModel.refreshExternalFeeds {
                                Toast.makeText(context, "लाइव समाचार व RSS अपडेट हो गया!", Toast.LENGTH_SHORT).show()
                            }
                            Toast.makeText(context, "लाइव RSS सिंक हो रहा है...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "रिफ्रेश",
                            tint = Gold500
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = if (isHi) "समाचार खोजें (शीर्षक, विषय, श्रेणी)..." else "Search news by topic...",
                            fontSize = 13.sp,
                            color = Color(0xFF8E9BAE)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Gold500,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = PureWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite,
                        focusedBorderColor = Gold500,
                        unfocusedBorderColor = Color(0xFF324864),
                        focusedContainerColor = Color(0xFF1E2D42),
                        unfocusedContainerColor = Color(0xFF1E2D42)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("news_search_input")
                )
            }
        }

        // Dual Source Selection Tabs: All, Admin-Edits, The Magadh Times Live RSS
        val rssCount = allNews.count {
            it.category.contains("RSS", ignoreCase = true) ||
            it.source.contains("Magadh", ignoreCase = true) ||
            it.source.contains("मगध", ignoreCase = true)
        }
        val adminCount = (allNews.size - rssCount).coerceAtLeast(0)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedSourceTab == 0,
                onClick = { selectedSourceTab = 0 },
                label = {
                    Text(
                        text = "सभी (${allNews.size})",
                        fontSize = 11.5.sp,
                        fontWeight = if (selectedSourceTab == 0) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Navy900,
                    selectedLabelColor = Gold500,
                    containerColor = PureWhite,
                    labelColor = Navy800
                ),
                shape = RoundedCornerShape(16.dp)
            )

            FilterChip(
                selected = selectedSourceTab == 1,
                onClick = { selectedSourceTab = 1 },
                label = {
                    Text(
                        text = "विभागीय/सम्पादित ($adminCount)",
                        fontSize = 11.5.sp,
                        fontWeight = if (selectedSourceTab == 1) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Navy900,
                    selectedLabelColor = Gold500,
                    containerColor = PureWhite,
                    labelColor = Navy800
                ),
                shape = RoundedCornerShape(16.dp)
            )

            FilterChip(
                selected = selectedSourceTab == 2,
                onClick = { selectedSourceTab = 2 },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RssFeed,
                            contentDescription = null,
                            tint = if (selectedSourceTab == 2) PureWhite else Color(0xFFE65100),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "लाइव ब्लॉग ($rssCount)",
                            fontSize = 11.5.sp,
                            fontWeight = if (selectedSourceTab == 2) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFE65100),
                    selectedLabelColor = PureWhite,
                    containerColor = PureWhite,
                    labelColor = Color(0xFFE65100)
                ),
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Category Filter Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = category },
                    label = {
                        Text(
                            text = category,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Navy900,
                        selectedLabelColor = Gold500,
                        containerColor = PureWhite,
                        labelColor = Navy800
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) Gold500 else Color(0xFFD5DFE9)
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
            }
        }

        // News List
        if (filteredNews.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = Color(0xFFB0BEC5),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isHi) "कोई समाचार उपलब्ध नहीं है" else "No news available",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF546E7A)
                    )
                    Text(
                        text = if (isHi) "कृपया बाद में देखें अथवा खोज शब्द बदलें" else "Try changing your search query",
                        fontSize = 12.sp,
                        color = Color(0xFF90A4AE)
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredNews, key = { it.id }) { item ->
                    NewsItemCard(
                        news = item,
                        onClick = { viewingArticle = item },
                        onShare = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, item.title)
                                putExtra(Intent.EXTRA_TEXT, "${item.title}\n\n${item.summary}\n\nस्रोतः ${item.source}\nडाउनलोड करें 'शिक्षक साथी' ऐप")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "समाचार शेयर करें"))
                        }
                    )
                }
            }
        }
    }

    // Article Reader Dialog
    viewingArticle?.let { article ->
        AlertDialog(
            onDismissRequest = { viewingArticle = null },
            title = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            color = if (article.isBreaking) Color(0xFFFFEBEE) else Gold100,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (article.isBreaking) "ताज़ा खबर" else article.category,
                                color = if (article.isBreaking) Color(0xFFC62828) else Navy900,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Text(
                            text = article.publishedAt,
                            fontSize = 11.sp,
                            color = Color(0xFF78909C)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = article.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy900,
                        lineHeight = 22.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (article.imageUrl.isNotBlank()) {
                        AppNetworkImage(
                            imageUrl = article.imageUrl,
                            contentDescription = article.title,
                            versionTag = article.timestamp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Text(
                        text = if (article.content.isNotBlank()) article.content else article.summary,
                        fontSize = 14.5.sp,
                        color = Color(0xFF263238),
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Text(
                                text = "स्रोतः ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy800
                            )
                            Text(
                                text = "${article.source} | दिनांक: ${article.publishedAt}",
                                fontSize = 12.sp,
                                color = Color(0xFF455A64)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (article.sourceUrl.isNotBlank()) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.sourceUrl))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = Gold500,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "आधिकारिक लिंक", color = PureWhite, fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewingArticle = null }) {
                    Text(text = "बंद करें", color = Color(0xFF546E7A))
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = PureWhite
        )
    }
}

@Composable
private fun NewsItemCard(
    news: NewsArticleEntity,
    onClick: () -> Unit,
    onShare: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = PureWhite),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("news_item_${news.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Badges row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (news.isPinned) {
                        Surface(
                            color = Gold100,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = null,
                                    tint = Navy900,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "पिन किया गया",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Navy900
                                )
                            }
                        }
                    }

                    if (news.isBreaking) {
                        Surface(
                            color = Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "ब्रेकिंग",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC62828),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    val isRss = news.category.contains("RSS", ignoreCase = true) ||
                            news.source.contains("Magadh", ignoreCase = true) ||
                            news.source.contains("मगध", ignoreCase = true)

                    Surface(
                        color = if (isRss) Color(0xFFFFF3E0) else Color(0xFFE8F0FE),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            if (isRss) {
                                Icon(
                                    imageVector = Icons.Default.RssFeed,
                                    contentDescription = null,
                                    tint = Color(0xFFE65100),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                            }
                            Text(
                                text = news.category,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isRss) Color(0xFFE65100) else Color(0xFF1967D2)
                            )
                        }
                    }
                }

                Text(
                    text = news.publishedAt,
                    fontSize = 11.sp,
                    color = Color(0xFF90A4AE)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (news.imageUrl.isNotBlank()) {
                AppNetworkImage(
                    imageUrl = news.imageUrl,
                    contentDescription = news.title,
                    versionTag = news.timestamp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Title
            Text(
                text = news.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Navy900,
                lineHeight = 20.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Summary
            Text(
                text = news.summary,
                fontSize = 13.sp,
                color = Color(0xFF455A64),
                lineHeight = 18.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Footer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "स्रोत: ${news.source}",
                    fontSize = 11.sp,
                    color = Color(0xFF78909C)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color(0xFF546E7A),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    TextButton(
                        onClick = onClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "पूरा पढ़ें →",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Teal700
                        )
                    }
                }
            }
        }
    }
}
