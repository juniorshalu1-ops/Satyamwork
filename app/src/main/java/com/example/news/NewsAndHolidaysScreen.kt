package com.example.news

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.core.DeepLinkHelper
import com.example.core.DeepLinkType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.DateUtils
import com.example.core.LocalAppLanguage
import com.example.database.entities.HolidayEntity
import com.example.database.entities.NoticeEntity
import com.example.home.HomeViewModel
import com.example.ui.components.HeroBanner
import com.example.ui.theme.Gold700
import com.example.ui.theme.Navy800
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Teal700

@Composable
fun NewsAndHolidaysScreen(
    homeViewModel: HomeViewModel,
    holidays: List<HolidayEntity>,
    initialTab: Int = 1,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val language = LocalAppLanguage.current
    val isHi = language.isHindi()

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val tabTitles = if (isHi) {
        listOf("विभागीय आदेश एवं सूचनाएं", "अवकाश कैलेंडर 2026", "सहेजे गए पत्र")
    } else {
        listOf("Official Circulars", "Holiday Calendar 2026", "Bookmarked")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("news_and_holidays_screen")
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Navy800,
            contentColor = PureWhite,
            edgePadding = 12.dp
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> NoticesTab(homeViewModel, isHi, showBookmarksOnly = false)
            1 -> HolidaysTab(holidays, isHi, homeViewModel)
            2 -> NoticesTab(homeViewModel, isHi, showBookmarksOnly = true)
        }
    }
}

@Composable
fun NoticesTab(
    viewModel: HomeViewModel,
    isHi: Boolean,
    showBookmarksOnly: Boolean
) {
    val context = LocalContext.current
    val notices by viewModel.latestNotices.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("सभी") }

    val categories = listOf("सभी", "वेतन/DA", "सरकारी आदेश", "अवकाश", "स्थानांतरण", "BPSC/TRE", "प्रशिक्षण")

    val filtered = notices.filter { item ->
        val matchesBookmark = !showBookmarksOnly || item.isBookmarked
        val matchesCategory = selectedCategory == "सभी" || item.category == selectedCategory
        val matchesSearch = item.title.contains(searchQuery, ignoreCase = true) ||
                item.summary.contains(searchQuery, ignoreCase = true) ||
                item.orderNumber.contains(searchQuery, ignoreCase = true)
        matchesBookmark && matchesCategory && matchesSearch
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            HeroBanner(
                title = if (isHi) "बिहार शिक्षक समाचार एवं आदेश" else "Bihar Teacher Circulars",
                subtitle = if (isHi) "शिक्षा विभाग, वित्त विभाग एवं प्राथमिक शिक्षा निदेशालय के प्रामाणिक आदेश" else "Authentic orders & notifications from Education Department",
                icon = Icons.Default.Notifications
            )
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(if (isHi) "आदेश / पत्रांक / विषय खोजें..." else "Search Orders...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (!showBookmarksOnly) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.5.sp) }
                        )
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Text(
                    text = if (isHi) "कोई आदेश या पत्र उपलब्ध नहीं है।" else "No matching circulars found.",
                    modifier = Modifier.padding(16.dp),
                    color = Color.Gray
                )
            }
        } else {
            items(filtered) { notice ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Teal700.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = notice.category,
                                    color = Teal700,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        val shareText = "${notice.title}\n${notice.summary}\nआदेश संख्या: ${notice.orderNumber}\nदिनांक: ${notice.issueDate}\nस्रोतः शिक्षक साथी"
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "आदेश साझा करें"))
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { viewModel.toggleBookmark(notice) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (notice.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = "Bookmark",
                                        tint = if (notice.isBookmarked) Gold700 else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = notice.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = notice.summary,
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "पत्रांक: ${notice.orderNumber.ifBlank { "विभागीय" }} • ${notice.issueDate}",
                                fontSize = 10.5.sp,
                                color = Color.Gray
                            )
                            TextButton(onClick = {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(notice.officialUrl))
                                context.startActivity(browserIntent)
                            }) {
                                Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isHi) "आधिकारिक पोर्टल" else "Official Portal", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HolidaysTab(
    holidays: List<HolidayEntity>,
    isHi: Boolean,
    homeViewModel: HomeViewModel? = null
) {
    var selectedMonth by remember { mutableIntStateOf(0) } // 0 = all
    var viewingHoliday by remember { mutableStateOf<HolidayEntity?>(null) }

    val pendingDeepLink by (homeViewModel?.pendingDeepLink?.collectAsState()
        ?: remember { mutableStateOf(null) })

    LaunchedEffect(pendingDeepLink, holidays) {
        val target = pendingDeepLink ?: return@LaunchedEffect
        if (target.type != DeepLinkType.HOLIDAY) return@LaunchedEffect
        val match = DeepLinkHelper.findMatchingHoliday(holidays, target)
        if (match != null) {
            selectedMonth = 0
            viewingHoliday = match
            homeViewModel?.consumeDeepLink(target.eventId)
        }
    }

    val filtered = if (selectedMonth == 0) holidays else holidays.filter { it.month == selectedMonth }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            HeroBanner(
                title = if (isHi) "बिहार विद्यालय अवकाश तालिका 2026" else "Bihar School Holiday Calendar 2026",
                subtitle = if (isHi) "शिक्षा विभाग, बिहार सरकार द्वारा अनुमोदित आधिकारिक अवकाश सूची" else "Approved official holidays for Bihar government schools",
                icon = Icons.Default.CalendarMonth
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = selectedMonth == 0,
                        onClick = { selectedMonth = 0 },
                        label = { Text(if (isHi) "संपूर्ण वर्ष" else "All Year") }
                    )
                }
                for (m in 1..12) {
                    item {
                        FilterChip(
                            selected = selectedMonth == m,
                            onClick = { selectedMonth = m },
                            label = { Text(DateUtils.getMonthName(m, isHi)) }
                        )
                    }
                }
            }
        }

        items(filtered, key = { it.id.takeIf { id -> id > 0L }?.toString() ?: "${it.dateStr}_${it.name}" }) { holiday ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewingHoliday = holiday },
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = holiday.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${holiday.dateStr} (${holiday.dayOfWeek}) • ${holiday.type}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    if (holiday.durationDays > 1) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE65100).copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${holiday.durationDays} दिवस",
                                color = Color(0xFFE65100),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    viewingHoliday?.let { holiday ->
        AlertDialog(
            onDismissRequest = { viewingHoliday = null },
            title = {
                Text(
                    text = holiday.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Navy800
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "दिनांक: ${holiday.dateStr} (${holiday.dayOfWeek})", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = "प्रकार: ${holiday.type}", fontSize = 13.sp, color = Teal700)
                    Text(text = "अवधि: ${holiday.durationDays} दिन", fontSize = 13.sp)
                    if (holiday.description.isNotBlank()) {
                        Text(text = holiday.description, fontSize = 13.sp, color = Color(0xFF455A64))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewingHoliday = null }) {
                    Text("बंद करें")
                }
            }
        )
    }
}
