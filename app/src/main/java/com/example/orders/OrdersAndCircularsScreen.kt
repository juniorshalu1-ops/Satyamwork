package com.example.orders

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.core.DeepLinkHelper
import com.example.core.DeepLinkType
import com.example.core.FileDownloadHelper
import com.example.ui.components.AppNetworkImage
import kotlinx.coroutines.launch
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
import com.example.database.entities.NoticeEntity
import com.example.home.HomeViewModel
import com.example.ui.theme.Gold100
import com.example.ui.theme.Gold500
import com.example.ui.theme.Gold700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Teal700

@Composable
fun OrdersAndCircularsScreen(
    homeViewModel: HomeViewModel,
    initialTab: Int = 0, // 0: Latest Orders, 1: Old Orders & Archive
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val language = LocalAppLanguage.current
    val isHi = language.isHindi()

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("सभी") }
    var selectedYear by remember { mutableStateOf("सभी वर्ष") }
    var viewingNotice by remember { mutableStateOf<NoticeEntity?>(null) }

    val allNotices by homeViewModel.notices.collectAsState()
    val pendingDeepLink by homeViewModel.pendingDeepLink.collectAsState()

    LaunchedEffect(pendingDeepLink, allNotices) {
        val target = pendingDeepLink ?: return@LaunchedEffect
        if (target.type != DeepLinkType.NOTICE) return@LaunchedEffect
        val match = DeepLinkHelper.findMatchingNotice(allNotices, target)
        if (match != null) {
            selectedCategory = "सभी"
            selectedYear = "सभी वर्ष"
            searchQuery = ""
            if (!(match.issueDate.contains("2026") || match.issueDate.contains("2025") || match.isFeatured)) {
                selectedTab = 1
            }
            viewingNotice = match
            homeViewModel.consumeDeepLink(target.eventId)
        }
    }

    val categories = listOf("सभी", "वेतन/DA", "अवकाश", "स्थानांतरण", "BPSC/TRE", "प्रशिक्षण", "सरकारी आदेश")
    val years = listOf("सभी वर्ष", "2026", "2025", "2024", "2023", "पूर्व आदेश")

    // Filter by tab, category, search query, and year
    val filteredNotices = allNotices.filter { notice ->
        val matchesCategory = selectedCategory == "सभी" || notice.category.contains(selectedCategory, ignoreCase = true)

        val matchesYear = if (selectedYear == "सभी वर्ष") {
            true
        } else if (selectedYear == "पूर्व आदेश") {
            !notice.issueDate.contains("2026") && !notice.issueDate.contains("2025") && !notice.issueDate.contains("2024")
        } else {
            notice.issueDate.contains(selectedYear)
        }

        val matchesTab = if (selectedTab == 0) {
            // Latest: 2026 or 2025 or featured
            notice.issueDate.contains("2026") || notice.issueDate.contains("2025") || notice.isFeatured
        } else {
            // Archive / Old orders
            true
        }

        val matchesQuery = searchQuery.isBlank() ||
                notice.title.contains(searchQuery, ignoreCase = true) ||
                notice.orderNumber.contains(searchQuery, ignoreCase = true) ||
                notice.summary.contains(searchQuery, ignoreCase = true) ||
                notice.department.contains(searchQuery, ignoreCase = true)

        matchesCategory && matchesYear && matchesTab && matchesQuery
    }.sortedWith(
        compareByDescending<NoticeEntity> { it.isFeatured }
            .thenByDescending { it.timestamp }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6F9))
            .testTag("orders_screen")
    ) {
        // Header
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
                            imageVector = if (selectedTab == 0) Icons.Default.Description else Icons.Default.History,
                            contentDescription = null,
                            tint = Navy900,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (selectedTab == 0) {
                                if (isHi) "नवीनतम विभागीय आदेश" else "Latest Government Orders"
                            } else {
                                if (isHi) "पुराने आदेश एवं अभिलेखागार" else "Old Orders & Circulars Archive"
                            },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                        Text(
                            text = if (isHi) "शिक्षा विभाग बिहार के आधिकारिक पत्र, अधिसूचना व संकल्प" else "Official Letters, Notifications & Resolutions",
                            fontSize = 11.sp,
                            color = Color(0xFFA5B8CC)
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
                            text = if (isHi) "आदेश संख्या, विषय, या विभाग खोजें..." else "Search by order no, topic, dept...",
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
                        .testTag("orders_search_input")
                )
            }
        }

        // Tabs: Latest vs Old/Archive
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Navy800,
            contentColor = PureWhite,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Gold500,
                    height = 3.dp
                )
            },
            edgePadding = 12.dp
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = if (isHi) "नवीनतम आदेश (2025-2026)" else "Latest Orders",
                        fontSize = 13.sp,
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 0) Gold500 else Color(0xFFA5B8CC)
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = if (isHi) "पुराने आदेश एवं परिपत्र (Archive)" else "Old Orders & Circulars",
                        fontSize = 13.sp,
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 1) Gold500 else Color(0xFFA5B8CC)
                    )
                }
            )
        }

        // Secondary Filters: Category Chips & Year Chips
        Column(modifier = Modifier.background(PureWhite)) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
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
                            containerColor = Color(0xFFF1F5F9),
                            labelColor = Navy800
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) Gold500 else Color(0xFFD5DFE9)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            if (selectedTab == 1) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(years) { yr ->
                        val isSelected = selectedYear == yr
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedYear = yr },
                            label = {
                                Text(
                                    text = yr,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Teal700,
                                selectedLabelColor = PureWhite,
                                containerColor = Color(0xFFF8FAFC),
                                labelColor = Color(0xFF334155)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        // Orders List
        if (filteredNotices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Color(0xFFB0BEC5),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isHi) "कोई आदेश उपलब्ध नहीं है" else "No orders found",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF546E7A)
                    )
                    Text(
                        text = if (isHi) "कृपया खोज शब्द या श्रेणी फ़िल्टर बदलें" else "Try changing your search filters",
                        fontSize = 12.sp,
                        color = Color(0xFF90A4AE)
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredNotices, key = { it.id }) { notice ->
                    OrderItemCard(
                        notice = notice,
                        onClick = { viewingNotice = notice },
                        onBookmarkToggle = { homeViewModel.toggleNoticeBookmark(notice.id) },
                        onOpenPdf = {
                            val url = if (notice.pdfUrl.isNotBlank()) notice.pdfUrl else notice.officialUrl
                            coroutineScope.launch {
                                FileDownloadHelper.openDocumentFast(context, url, "PDF")
                            }
                        },
                        onShare = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, notice.title)
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "आदेश सं.: ${notice.orderNumber}\n${notice.title}\n\nदिनांक: ${notice.issueDate}\nविभाग: ${notice.department}\n\nडाउनलोड करें 'शिक्षक साथी' ऐप"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "आदेश शेयर करें"))
                        }
                    )
                }
            }
        }
    }

    // Detail Dialog
    viewingNotice?.let { notice ->
        AlertDialog(
            onDismissRequest = { viewingNotice = null },
            title = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            color = Gold100,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = notice.category,
                                color = Navy900,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Text(
                            text = notice.issueDate,
                            fontSize = 11.sp,
                            color = Color(0xFF78909C)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = notice.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy900,
                        lineHeight = 22.sp
                    )

                    if (notice.orderNumber.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ज्ञापांक / आदेश संख्या: ${notice.orderNumber}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Teal700
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (notice.imageUrl.isNotBlank()) {
                        AppNetworkImage(
                            imageUrl = notice.imageUrl,
                            contentDescription = notice.title,
                            versionTag = notice.timestamp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Text(
                        text = notice.summary,
                        fontSize = 14.sp,
                        color = Color(0xFF263238),
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "जारीकर्ता विभाग:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy800
                            )
                            Text(
                                text = notice.department,
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
                        val targetUrl = if (notice.pdfUrl.isNotBlank()) notice.pdfUrl else notice.officialUrl
                        coroutineScope.launch {
                            FileDownloadHelper.openDocumentFast(context, targetUrl, "PDF")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = Gold500,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "PDF देखें / डाउनलोड", color = PureWhite, fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewingNotice = null }) {
                    Text(text = "बंद करें", color = Color(0xFF546E7A))
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = PureWhite
        )
    }
}

@Composable
private fun OrderItemCard(
    notice: NoticeEntity,
    onClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onOpenPdf: () -> Unit,
    onShare: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = PureWhite),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("order_item_${notice.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Category Badge, Date, Bookmark
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = Color(0xFFE8F0FE),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = notice.category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1967D2),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    if (notice.orderNumber.isNotBlank()) {
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "सं: ${notice.orderNumber}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = notice.issueDate,
                        fontSize = 11.sp,
                        color = Color(0xFF90A4AE)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onBookmarkToggle,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (notice.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (notice.isBookmarked) Gold700 else Color(0xFFB0BEC5),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Title
            Text(
                text = notice.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Navy900,
                lineHeight = 20.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Summary
            Text(
                text = notice.summary,
                fontSize = 13.sp,
                color = Color(0xFF455A64),
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Action row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = notice.department,
                    fontSize = 11.sp,
                    color = Color(0xFF78909C),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
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

                    Button(
                        onClick = onOpenPdf,
                        colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = Gold500,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PDF आदेश",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    }
                }
            }
        }
    }
}
