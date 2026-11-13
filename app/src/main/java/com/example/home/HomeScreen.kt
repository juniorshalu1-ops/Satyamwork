package com.example.home

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.LocalAppLanguage
import com.example.core.YouTubeUrlHelper
import com.example.database.entities.FeatureBoxEntity
import com.example.database.entities.NewsArticleEntity
import com.example.database.entities.PromoBannerEntity
import com.example.database.entities.VideoTutorialEntity
import com.example.productivity.FeatureBoxCard
import com.example.videos.InAppYouTubePlayerDialog
import com.example.ui.components.AppNetworkImage
import com.example.ui.theme.Gold100
import com.example.ui.theme.Gold500
import com.example.ui.theme.Gold700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Teal700

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToFeature: (FeatureBoxEntity) -> Unit,
    onNavigateToNewsFeed: () -> Unit,
    onNavigateToVideoLibrary: () -> Unit,
    onNavigateToFinancialDetails: () -> Unit,
    onNavigateToHolidayCalendar: () -> Unit,
    onNavigateToProfile: () -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    val isHi = language.isHindi()

    val currentDateStr by viewModel.currentDateOnly.collectAsState()
    val schoolStatus by viewModel.schoolStatus.collectAsState()
    val teacher by viewModel.teacherProfile.collectAsState()
    val topLinks by viewModel.topThreeLinks.collectAsState()
    val dynamicTopLinks by viewModel.homeTopLinks.collectAsState()
    val financialBanner by viewModel.financialBanner.collectAsState()
    val whatsAppBanner by viewModel.whatsAppBanner.collectAsState()
    val promoBanners by viewModel.promoBanners.collectAsState()
    val newsList by viewModel.newsArticles.collectAsState()
    val videoList by viewModel.videoTutorials.collectAsState()
    val featuredVideos by viewModel.featuredHomeVideos.collectAsState()
    val featureBoxes by viewModel.featureBoxes.collectAsState()
    val activeLoudAlert by viewModel.activeLoudAlert.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var selectedNewsArticle by remember { mutableStateOf<NewsArticleEntity?>(null) }

    fun openUrlSafely(url: String) {
        if (url.isBlank()) return
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Administrative Announcement Banner (if any active)
        activeLoudAlert?.let { alert ->
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE65100)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = PureWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "📢 विशेष प्रशासनिक सूचना (${alert.section})",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFBF360C),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = alert.title,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF3E2723),
                                    fontSize = 12.sp
                                )
                            }
                            IconButton(onClick = { viewModel.dismissLoudAlert() }, modifier = Modifier.size(28.dp)) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // SECTION B: COMBINED BANNER (Teacher Profile + Date + School Status + Timings + Holidays)
        // Note: Live Digital Clock has been completely removed per instructions!
        // =====================================================================
        item {
            CombinedTeacherAndSchoolBanner(
                currentDateStr = currentDateStr,
                status = schoolStatus,
                teacherName = teacher?.name ?: "शिक्षक साथी",
                schoolName = teacher?.schoolName ?: "राजकीय / उत्क्रमित मध्य विद्यालय",
                udiseCode = teacher?.udiseCode ?: "10280401102",
                designation = teacher?.designation ?: "सहायक शिक्षक",
                onEditProfile = { showEditProfileDialog = true },
                onViewHolidayCalendar = onNavigateToHolidayCalendar
            )
        }

        // =====================================================================
        // SECTION C: DYNAMIC OFFICIAL LINKS (Admin Managed - Unlimited Links supported)
        // Exactly three equal-sized compact buttons or horizontal row
        // =====================================================================
        item {
            if (dynamicTopLinks.isNotEmpty()) {
                DynamicOfficialLinksRow(
                    links = dynamicTopLinks.filter { it.isVisible },
                    onOpenLink = { openUrlSafely(it) }
                )
            } else {
                ThreeOfficialLinksRow(
                    link1Title = topLinks?.link1Title ?: "eShikshakosh",
                    link1Url = topLinks?.link1Url ?: "https://eshikshakosh.bihar.gov.in",
                    link1Visible = topLinks?.link1Visible ?: true,
                    link2Title = topLinks?.link2Title ?: "HRMS Bihar",
                    link2Url = topLinks?.link2Url ?: "https://hrms.bihar.gov.in",
                    link2Visible = topLinks?.link2Visible ?: true,
                    link3Title = topLinks?.link3Title ?: "UDISE+",
                    link3Url = topLinks?.link3Url ?: "https://udiseplus.gov.in",
                    link3Visible = topLinks?.link3Visible ?: true,
                    onOpenLink = { openUrlSafely(it) }
                )
            }
        }

        // =====================================================================
        // SECTION D: TEACHER FINANCIAL SERVICES BANNER
        // Heading: "शिक्षकों के लिए विशेष वित्तीय सुविधाएं" (Personal Loan, Credit Card, Insurance)
        // =====================================================================
        item {
            TeacherFinancialServicesBanner(
                heading = financialBanner?.heading ?: "शिक्षकों के लिए विशेष वित्तीय सुविधाएं",
                supportingText = financialBanner?.supportingText ?: "शिक्षकों के लिए कम ब्याज दरों पर विभिन्न बैंकों एवं वित्तीय संस्थानों के विकल्प उपलब्ध।",
                disclaimer = financialBanner?.disclaimer ?: "*शर्तें लागू। अनुमोदन एवं ब्याज दरें बैंकों/वित्तीय संस्थानों के नियमानुसार।",
                buttonText = financialBanner?.buttonText ?: "विवरण देखें / आवेदन",
                destinationType = financialBanner?.destinationType ?: "IN_APP",
                destinationUrl = financialBanner?.destinationUrl ?: "",
                imageUrl = financialBanner?.imageUrl ?: "",
                versionTag = financialBanner?.lastUpdated ?: 0L,
                onClick = {
                    val destUrl = financialBanner?.destinationUrl ?: ""
                    if (financialBanner?.destinationType == "EXTERNAL_URL" && destUrl.isNotBlank()) {
                        openUrlSafely(destUrl)
                    } else if (financialBanner?.destinationType == "GOOGLE_FORM" && destUrl.isNotBlank()) {
                        openUrlSafely(destUrl)
                    } else {
                        onNavigateToFinancialDetails()
                    }
                }
            )
        }

        // =====================================================================
        // SECTION D2: PROMOTIONAL BANNERS CAROUSEL (Admin Controlled, 4-5 slots)
        // Shows only when active banners are present
        // =====================================================================
        val activePromos = promoBanners.filter { it.isActive }.sortedBy { it.orderIndex }
        if (activePromos.isNotEmpty()) {
            item {
                HomePromotionalBannersSection(
                    banners = activePromos.take(5),
                    onBannerClick = { banner ->
                        if (banner.destinationUrl.isNotBlank()) {
                            openUrlSafely(banner.destinationUrl)
                        }
                    }
                )
            }
        }

        // =====================================================================
        // SECTION E: NEWS SECTION (Latest 10 Published News & Updates)
        // Same content source as Updates/News tab
        // =====================================================================
        item {
            HomeNewsSection(
                newsList = newsList.take(10),
                onArticleClick = { selectedNewsArticle = it },
                onViewAllNews = onNavigateToNewsFeed
            )
        }

        // =====================================================================
        // SECTION F: VIDEO SECTION (Educational & Informational Videos)
        // Same content source as Video tab - self added & newest videos sorted first
        // =====================================================================
        item {
            val homeDisplayVideos = (featuredVideos + videoList)
                .sortedWith(compareByDescending<VideoTutorialEntity> { it.isPinned }.thenByDescending { it.id })
                .distinctBy {
                    if (it.id > 0L) it.id.toString()
                    else if (it.youtubeId.isNotBlank()) it.youtubeId
                    else it.youtubeUrl.ifBlank { it.title }
                }
                .take(8)
            HomeVideoSection(
                videoList = homeDisplayVideos,
                onVideoClick = { video ->
                    val urlOrId = if (video.youtubeUrl.isNotBlank()) video.youtubeUrl else video.youtubeId
                    YouTubeUrlHelper.openYouTubeVideo(context, urlOrId)
                },
                onViewAllVideos = onNavigateToVideoLibrary
            )
        }

        // =====================================================================
        // SECTION G: WHATSAPP CHANNEL JOINING BANNER
        // =====================================================================
        item {
            WhatsAppChannelJoiningBanner(
                bannerText = whatsAppBanner?.bannerText ?: "बिहार के शिक्षकों से जुड़ी महत्वपूर्ण सूचनाएं, अपडेट एवं जानकारियों के लिए हमारे WhatsApp Channel से जुड़ें।",
                buttonText = whatsAppBanner?.buttonText ?: "JOIN WHATSAPP CHANNEL",
                channelUrl = whatsAppBanner?.channelUrl ?: "https://whatsapp.com/channel/0029Va4yZ1m2k345",
                imageUrl = whatsAppBanner?.imageUrl ?: "",
                versionTag = whatsAppBanner?.lastUpdated ?: 0L,
                onJoinClick = { url -> openUrlSafely(url) }
            )
        }

        // =====================================================================
        // SECTION 4: 14 MAIN FEATURE BOXES ("मुख्य शिक्षक सुविधाएं")
        // Responsive 2-column grid of all 14 core teacher features
        // =====================================================================
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                val rawBoxes = if (featureBoxes.isNotEmpty()) featureBoxes else com.example.database.entities.DEFAULT_FEATURE_BOXES
                val effectiveBoxes = rawBoxes.map { box ->
                    when (box.id.lowercase()) {
                        "important_links", "links" -> box.copy(
                            title = "सक्रिय डिजिटल सेवा",
                            subtitle = if (box.subtitle == "आधिकारिक पोर्टल सूची" || box.subtitle.isBlank()) "ई-शिक्षाकोष, मेधासॉफ्ट व पोर्टल" else box.subtitle
                        )
                        "whatsapp_channel", "whatsapp" -> box.copy(
                            subtitle = whatsAppBanner?.bannerText?.takeIf { it.isNotBlank() } ?: box.subtitle,
                            externalUrl = whatsAppBanner?.channelUrl?.takeIf { it.isNotBlank() } ?: box.externalUrl
                        )
                        else -> box
                    }
                }
                val visibleBoxes = effectiveBoxes.filter { it.isVisible }.sortedBy { it.displayOrder }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "मुख्य शिक्षक सेवाएं",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )
                    Text(
                        text = "${visibleBoxes.size} सेवाएं",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Gold700
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                val chunked = visibleBoxes.chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    chunked.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val first = row[0]
                            Box(modifier = Modifier.weight(1f)) {
                                FeatureBoxCard(box = first, onClick = { onNavigateToFeature(first) })
                            }

                            if (row.size > 1) {
                                val second = row[1]
                                Box(modifier = Modifier.weight(1f)) {
                                    FeatureBoxCard(box = second, onClick = { onNavigateToFeature(second) })
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    // In-place Profile Edit Dialog
    if (showEditProfileDialog) {
        TeacherProfileEditDialog(
            currentName = teacher?.name ?: "",
            currentSchool = teacher?.schoolName ?: "",
            currentUdise = teacher?.udiseCode ?: "",
            currentDesignation = teacher?.designation ?: "",
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, school, udise, desig ->
                viewModel.saveTeacherProfileLocally(name, school, udise, desig)
                showEditProfileDialog = false
            }
        )
    }

    // News Details Dialog
    selectedNewsArticle?.let { article ->
        AlertDialog(
            onDismissRequest = { selectedNewsArticle = null },
            title = {
                Column {
                    Surface(color = Gold100, shape = RoundedCornerShape(4.dp)) {
                        Text(
                            text = article.category,
                            color = Navy900,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = article.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Navy900)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    }
                    Text(text = "दिनांक: ${article.publishedAt}", fontSize = 11.sp, color = Color.Gray)
                    Text(text = article.summary, fontSize = 13.5.sp, color = Color(0xFF37474F))
                    if (article.content.isNotBlank()) {
                        Text(text = article.content, fontSize = 13.sp, color = Color(0xFF263238), lineHeight = 19.sp)
                    }
                }
            },
            confirmButton = {
                if (article.sourceUrl.isNotBlank()) {
                    Button(onClick = { openUrlSafely(article.sourceUrl) }, colors = ButtonDefaults.buttonColors(containerColor = Navy800)) {
                        Text("स्रोतः लिंक खोलें")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedNewsArticle = null }) {
                    Text("बंद करें")
                }
            }
        )
    }
}

/**
 * COMBINED BANNER (Teacher Profile + Date + School Status + Timings + Holidays)
 */
@Composable
fun CombinedTeacherAndSchoolBanner(
    currentDateStr: String,
    status: TodaySchoolStatus,
    teacherName: String,
    schoolName: String,
    udiseCode: String,
    designation: String,
    onEditProfile: () -> Unit,
    onViewHolidayCalendar: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = PureWhite),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("combined_teacher_school_banner")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Prominent Date and School Open/Closed Status
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Navy900, Navy800)
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Date Display (Without ticking clock)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(Gold500, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = Navy900,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = currentDateStr,
                                color = PureWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "आज की तिथि",
                                color = Color(0xFFA5B8CC),
                                fontSize = 10.sp
                            )
                        }
                    }

                    // School Status Badge (OPEN / CLOSED)
                    Surface(
                        color = if (status.isOpen) Color(0xFF1B5E20) else Color(0xFFB71C1C),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.clickable { onViewHolidayCalendar() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(PureWhite, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (status.isOpen) "OPEN (खुला है)" else "CLOSED (बंद है)",
                                color = PureWhite,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }
            }

            // Body: Teacher Profile + School Timings + Holiday Details
            Column(modifier = Modifier.padding(14.dp)) {
                // Teacher Profile Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Gold100, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = Navy900,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = teacherName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFFE8F0FE),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = designation,
                                    fontSize = 10.sp,
                                    color = Color(0xFF1967D2),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "$schoolName • UDISE: $udiseCode",
                            fontSize = 11.5.sp,
                            color = Color(0xFF455A64),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onEditProfile,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = Teal700,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // School Timing & Holiday Strip
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(10.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE2E8F0))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = if (status.isOpen) Color(0xFF2E7D32) else Color(0xFFC62828),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "समय: ${status.openingTime} से ${status.closingTime}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Navy900
                                )
                            }

                            if (status.remainingTimeText.isNotBlank()) {
                                Text(
                                    text = status.remainingTimeText,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (status.isOpen) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                        }

                        if (status.holidayName.isNotBlank() || !status.isOpen) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (status.holidayName.isNotBlank()) "अवकाश: ${status.holidayName} (${status.holidayType})" else status.statusSubtitle,
                                fontSize = 11.sp,
                                color = Color(0xFFD32F2F),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DynamicOfficialLinksRow(
    links: List<com.example.database.entities.HomeTopLinkEntity>,
    onOpenLink: (String) -> Unit
) {
    if (links.isEmpty()) return
    if (links.size <= 3) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dynamic_official_links_row"),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            links.forEach { link ->
                OfficialLinkCompactButton(
                    title = link.title,
                    url = link.url,
                    onClick = { onOpenLink(link.url) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    } else {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dynamic_official_links_row"),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(links) { link ->
                OfficialLinkCompactButton(
                    title = link.title,
                    url = link.url,
                    onClick = { onOpenLink(link.url) },
                    modifier = Modifier.width(115.dp)
                )
            }
        }
    }
}

/**
 * THREE IMPORTANT OFFICIAL LINKS (eShikshakosh, HRMS Bihar, UDISE+)
 * Displayed in ONE horizontal row with equal-sized compact buttons
 */
@Composable
fun ThreeOfficialLinksRow(
    link1Title: String,
    link1Url: String,
    link1Visible: Boolean,
    link2Title: String,
    link2Url: String,
    link2Visible: Boolean,
    link3Title: String,
    link3Url: String,
    link3Visible: Boolean,
    onOpenLink: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("three_official_links_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (link1Visible) {
            OfficialLinkCompactButton(
                title = link1Title,
                url = link1Url,
                onClick = { onOpenLink(link1Url) },
                modifier = Modifier.weight(1f)
            )
        }
        if (link2Visible) {
            OfficialLinkCompactButton(
                title = link2Title,
                url = link2Url,
                onClick = { onOpenLink(link2Url) },
                modifier = Modifier.weight(1f)
            )
        }
        if (link3Visible) {
            OfficialLinkCompactButton(
                title = link3Title,
                url = link3Url,
                onClick = { onOpenLink(link3Url) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun OfficialLinkCompactButton(
    title: String,
    url: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = PureWhite),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .clickable { onClick() }
            .testTag("top_link_$title")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(0xFFE8F0FE), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = Color(0xFF1967D2),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = Navy900,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "पोर्टल लिंक",
                fontSize = 9.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * TEACHER FINANCIAL SERVICES BANNER
 */
@Composable
fun TeacherFinancialServicesBanner(
    heading: String,
    supportingText: String,
    disclaimer: String,
    buttonText: String,
    destinationType: String,
    destinationUrl: String,
    onClick: () -> Unit,
    imageUrl: String = "",
    versionTag: Long = 0L
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Navy900),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("teacher_financial_services_banner")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (imageUrl.isNotBlank()) {
                AppNetworkImage(
                    imageUrl = imageUrl,
                    contentDescription = heading,
                    versionTag = versionTag,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = heading,
                    color = Gold500,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp
                )
                Surface(
                    color = Color(0xFF1E2D42),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "कम ब्याज दरें",
                        color = PureWhite,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 3 Services Chips (Personal Loan, Credit Card, Insurance)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FinancialChip(icon = Icons.Default.MonetizationOn, label = "पर्सनल लोन")
                FinancialChip(icon = Icons.Default.CreditCard, label = "क्रेडिट कार्ड")
                FinancialChip(icon = Icons.Default.AccountBalance, label = "बीमा सुविधा")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = supportingText,
                color = Color(0xFFA5B8CC),
                fontSize = 11.5.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = disclaimer,
                    color = Color(0xFF78909C),
                    fontSize = 9.sp,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Gold500),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(
                        text = buttonText,
                        color = Navy900,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun FinancialChip(icon: ImageVector, label: String) {
    Surface(
        color = Color(0xFF1E2D42),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Gold500, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, color = PureWhite, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/**
 * HOME NEWS SECTION (Latest 10 published updates)
 */
@Composable
fun HomeNewsSection(
    newsList: List<NewsArticleEntity>,
    onArticleClick: (NewsArticleEntity) -> Unit,
    onViewAllNews: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(24.dp).background(Color(0xFFFFEBEE), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Newspaper, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(14.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "नवीनतम शिक्षा समाचार एवं अपडेट्स", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy900)
            }
            Text(
                text = "सभी देखें →",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = Teal700,
                modifier = Modifier.clickable { onViewAllNews() }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (newsList.isEmpty()) {
            Text(
                text = "वर्तमान में कोई समाचार उपलब्ध नहीं है।",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(8.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                newsList.forEach { article ->
                    ElevatedCard(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = PureWhite),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.5.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onArticleClick(article) }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(color = Color(0xFFE8F0FE), shape = RoundedCornerShape(4.dp)) {
                                    Text(
                                        text = article.category,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1967D2),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                                Text(text = article.publishedAt, fontSize = 10.sp, color = Color.Gray)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = article.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = article.summary,
                                fontSize = 11.5.sp,
                                color = Color(0xFF455A64),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * HOME VIDEO SECTION
 */
@Composable
fun HomeVideoSection(
    videoList: List<VideoTutorialEntity>,
    onVideoClick: (VideoTutorialEntity) -> Unit,
    onViewAllVideos: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(24.dp).background(Color(0xFFFFEBEE), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.PlayCircle, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(14.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "शैक्षणिक एवं विभागीय वीडियो", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Navy900)
            }
            Text(
                text = "सभी वीडियो →",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = Teal700,
                modifier = Modifier.clickable { onViewAllVideos() }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (videoList.isEmpty()) {
            Text(
                text = "वर्तमान में कोई वीडियो उपलब्ध नहीं है।",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(8.dp)
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(videoList, key = { it.id.takeIf { id -> id > 0L }?.toString() ?: it.youtubeUrl.ifBlank { it.title } }) { video ->
                    ElevatedCard(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = PureWhite),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .width(200.dp)
                            .clickable { onVideoClick(video) }
                    ) {
                        Column {
                            val thumbUrl = if (video.thumbnailUrl.isNotBlank()) {
                                video.thumbnailUrl
                            } else if (video.youtubeId.isNotBlank()) {
                                YouTubeUrlHelper.getThumbnailUrl(video.youtubeId)
                            } else {
                                ""
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .background(Navy900),
                                contentAlignment = Alignment.Center
                            ) {
                                if (thumbUrl.isNotBlank()) {
                                    AppNetworkImage(
                                        imageUrl = thumbUrl,
                                        contentDescription = video.title,
                                        versionTag = video.timestamp,
                                        showErrorText = false,
                                        placeholderBgColor = Navy900,
                                        accentColor = Gold500,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(Color.Transparent, Color(0x77000000))
                                                )
                                            )
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xDDFF0000)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = PureWhite,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                if (video.isPinned || video.isFeaturedOnHome) {
                                    Surface(
                                        color = Gold500,
                                        shape = RoundedCornerShape(3.dp),
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(4.dp)
                                    ) {
                                        Text(
                                            text = "खास",
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Navy900,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = video.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Navy900,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = video.category,
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * WHATSAPP CHANNEL JOINING BANNER
 */
@Composable
fun WhatsAppChannelJoiningBanner(
    bannerText: String,
    buttonText: String,
    channelUrl: String,
    onJoinClick: (String) -> Unit,
    imageUrl: String = "",
    versionTag: Long = 0L
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF25D366))),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("whatsapp_channel_joining_banner")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (imageUrl.isNotBlank()) {
                AppNetworkImage(
                    imageUrl = imageUrl,
                    contentDescription = bannerText,
                    versionTag = versionTag,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF25D366), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = null,
                    tint = PureWhite,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bannerText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1B5E20),
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = { onJoinClick(channelUrl) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = buttonText,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
        }
    }
}

/**
 * Teacher Profile Local Edit Dialog
 */
@Composable
fun TeacherProfileEditDialog(
    currentName: String,
    currentSchool: String,
    currentUdise: String,
    currentDesignation: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var school by remember { mutableStateOf(currentSchool) }
    var udise by remember { mutableStateOf(currentUdise) }
    var designation by remember { mutableStateOf(currentDesignation) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("शिक्षक विवरण संपादित करें", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("शिक्षक का नाम") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = designation,
                    onValueChange = { designation = it },
                    label = { Text("पदनाम (Designation)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = school,
                    onValueChange = { school = it },
                    label = { Text("विद्यालय का नाम") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = udise,
                    onValueChange = { udise = it },
                    label = { Text("यूडाइस (UDISE) कोड") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, school, udise, designation) },
                colors = ButtonDefaults.buttonColors(containerColor = Navy800)
            ) {
                Text("सहेजें")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}

@Composable
fun HomePromotionalBannersSection(
    banners: List<PromoBannerEntity>,
    onBannerClick: (PromoBannerEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (banners.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("home_promotional_banners_section")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(4.dp, 16.dp)
                        .background(Gold500, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "विशेष सूचना एवं सुविधाएं",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = Navy900
                )
            }
            Text(
                text = "${banners.size} उपलब्ध",
                fontSize = 11.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(banners, key = { it.id }) { banner ->
                ElevatedCard(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = PureWhite),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .width(280.dp)
                        .clickable { onBannerClick(banner) }
                        .testTag("promo_banner_${banner.id}")
                ) {
                    Column {
                        if (banner.imageUrl.isNotBlank()) {
                            AppNetworkImage(
                                imageUrl = banner.imageUrl,
                                contentDescription = banner.title,
                                versionTag = banner.timestamp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(115.dp)
                                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                            )
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            if (banner.category.isNotBlank()) {
                                Surface(
                                    color = Color(0xFFE8EAF6),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                ) {
                                    Text(
                                        text = banner.category,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Navy800,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = banner.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = Navy900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (banner.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = banner.description,
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF455A64),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { onBannerClick(banner) },
                                colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(
                                    text = banner.buttonText.ifBlank { "देखें" },
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
