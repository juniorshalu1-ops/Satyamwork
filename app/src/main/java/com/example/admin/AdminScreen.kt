package com.example.admin

import android.widget.Toast
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.database.entities.FeatureBoxEntity
import com.example.database.entities.FinancialBannerEntity
import com.example.database.entities.TopThreeLinksEntity
import com.example.database.entities.WhatsAppBannerEntity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cloudflare.AdminGoogleAuthHelper
import com.example.cloudflare.AdminR2StorageTab
import com.example.cloudflare.R2Config
import com.example.cloudflare.R2UploadMode
import com.example.cloudflare.R2UploadPickerRow
import com.example.core.SoundNotificationHelper
import com.example.database.entities.EducationActivityEntity
import com.example.database.entities.HolidayEntity
import com.example.database.entities.NoticeEntity
import com.example.database.entities.QuickPortalEntity
import com.example.ui.components.AppNetworkImage
import com.example.ui.components.HeroBanner
import com.example.ui.theme.Gold100
import com.example.ui.theme.Gold500
import com.example.ui.theme.Gold700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import androidx.compose.foundation.BorderStroke
import com.example.ui.theme.Teal700
import kotlinx.coroutines.launch

@Composable
fun AdminNotificationToggleRow(
    sendNotification: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (sendNotification) Color(0xFFE8F5E9) else Color(0xFFF5F5F5),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (sendNotification) Color(0xFF81C784) else Color(0xFFE0E0E0)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = if (sendNotification) Icons.Default.Campaign else Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = if (sendNotification) Color(0xFF2E7D32) else Color(0xFF757575),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (sendNotification) "📢 सूचना भेजें (With Notification)" else "🔕 शांत सेव (Silent - बिना सूचना)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = if (sendNotification) Color(0xFF1B5E20) else Color(0xFF424242)
                    )
                    Text(
                        text = if (sendNotification) "उपयोगकर्ताओं को लाइव नोटिफिकेशन जाएगा" else "केवल डेटाबेस में सेव होगा, कोई अलर्ट नहीं",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
            Switch(
                checked = sendNotification,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PureWhite,
                    checkedTrackColor = Color(0xFF2E7D32)
                )
            )
        }
    }
}

@Composable
fun AdminScreen(
    viewModel: AdminViewModel,
    onBackToTeacherInterface: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val session by viewModel.session.collectAsState()

    if (!session.isAuthenticated) {
        AdminLoginDialog(
            viewModel = viewModel,
            onCancel = onBackToTeacherInterface
        )
    } else {
        AdminDashboardContent(
            viewModel = viewModel,
            session = session,
            onLogout = {
                viewModel.logout()
                coroutineScope.launch {
                    AdminGoogleAuthHelper.clearCredentialSession(context)
                }
                onBackToTeacherInterface()
            },
            onBack = onBackToTeacherInterface,
            modifier = modifier
        )
    }
}

@Composable
fun AdminLoginDialog(
    viewModel: AdminViewModel,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var errorMsg by remember { mutableStateOf("") }
    var isAuthenticating by remember { mutableStateOf(false) }
    val isOnline = viewModel.isOnline()

    AlertDialog(
        onDismissRequest = onCancel,
        icon = {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "व्यवस्थापक सुरक्षा",
                tint = Navy800,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "व्यवस्थापक लॉगिन (Google Only)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Navy900
                )
                Text(
                    text = "केवल अधिकृत Google खाता: ${AdminViewModel.SUPER_ADMIN_EMAIL}",
                    fontSize = 11.5.sp,
                    color = Gold700,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "शिक्षक साथी व्यवस्थापक पोर्टल और Cloudflare R2 फाइल अपलोड हेतु अपने अधिकृत Google खाते से साइन-इन करें।",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )

                if (!isOnline) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "ऑफलाइन सूचना",
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Google लॉगिन हेतु इंटरनेट कनेक्शन चालू करें।",
                                color = Color(0xFFE65100),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        if (!viewModel.isOnline()) {
                            errorMsg = "Google लॉगिन हेतु इंटरनेट कनेक्शन आवश्यक है!"
                            return@Button
                        }
                        isAuthenticating = true
                        errorMsg = ""
                        coroutineScope.launch {
                            val res = AdminGoogleAuthHelper.signInWithGoogle(context, viewModel)
                            isAuthenticating = false
                            res.onSuccess {
                                Toast.makeText(context, "व्यवस्थापक स्वागत है", Toast.LENGTH_SHORT).show()
                            }.onFailure { err ->
                                errorMsg = err.message ?: "Google साइन-इन विफल रहा"
                            }
                        }
                    },
                    enabled = isOnline && !isAuthenticating,
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("admin_google_login_btn")
                ) {
                    if (isAuthenticating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Gold500
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Firebase Google सत्यापन हो रहा है...",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Google Sign-In",
                            modifier = Modifier.size(18.dp),
                            tint = Gold500
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Google से लॉगिन करें (Sign In)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    }
                }

                if (errorMsg.isNotBlank()) {
                    Text(
                        text = errorMsg,
                        color = Color(0xFFC62828),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("रद्द करें (Cancel)", color = Color.Gray)
            }
        }
    )
}

data class AdminDashboardCardItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun AdminDashboardContent(
    viewModel: AdminViewModel,
    session: AdminSession,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeAdminSubSection by viewModel.activeAdminSubSection.collectAsState()
    val adminGridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = viewModel.savedDashboardGridIndex,
        initialFirstVisibleItemScrollOffset = viewModel.savedDashboardGridOffset
    )
    LaunchedEffect(
        adminGridState.firstVisibleItemIndex,
        adminGridState.firstVisibleItemScrollOffset
    ) {
        viewModel.savedDashboardGridIndex = adminGridState.firstVisibleItemIndex
        viewModel.savedDashboardGridOffset = adminGridState.firstVisibleItemScrollOffset
    }
    val subSectionListStates = remember { mutableMapOf<String, LazyListState>() }
    fun listStateFor(key: String): LazyListState =
        subSectionListStates.getOrPut(key) { LazyListState() }

    val notices by viewModel.notices.collectAsState()
    val holidays by viewModel.holidays.collectAsState()
    val activities by viewModel.activities.collectAsState()
    val downloads by viewModel.downloads.collectAsState()
    val topLinks by viewModel.homeTopLinks.collectAsState()
    val complaints by viewModel.complaintOptions.collectAsState()
    val loanServices by viewModel.loanServices.collectAsState()
    val creditCardServices by viewModel.creditCardServices.collectAsState()
    val investmentServices by viewModel.investmentServices.collectAsState()
    val youtubeFeeds by viewModel.youtubeFeeds.collectAsState()
    val newsArticles by viewModel.newsArticles.collectAsState()
    val newsFeeds by viewModel.newsRssFeeds.collectAsState()
    val featureBoxes by viewModel.featureBoxes.collectAsState()
    val portals by viewModel.portals.collectAsState()
    val videos by viewModel.videos.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val publishedDa by viewModel.currentPublishedDa.collectAsState()
    val hraUrban by viewModel.currentHraUrban.collectAsState()
    val schoolTiming by viewModel.schoolTiming.collectAsState()

    val dashboardCards = listOf(
        AdminDashboardCardItem("notices", "विभागीय आदेश", "${notices.size} आदेश सक्रिय", Icons.Default.Description, Color(0xFF1E88E5)),
        AdminDashboardCardItem("holidays", "अवकाश तालिका 2026", "${holidays.size} अवकाश दर्ज", Icons.Default.Event, Color(0xFF43A047)),
        AdminDashboardCardItem("activities", "वर्तमान गतिविधियां", "${activities.size} गतिविधियां सक्रिय", Icons.Default.Lightbulb, Color(0xFFFB8C00)),
        AdminDashboardCardItem("important_links", "डिजिटल पोर्टल (सुविधा 3)", "${portals.size} पोर्टल सक्रिय", Icons.Default.Language, Color(0xFF00897B)),
        AdminDashboardCardItem("whatsapp_channel", "व्हाट्सएप चैनल (सुविधा 14)", "ऑफिशियल चैनल प्रबंधन", Icons.Default.Campaign, Color(0xFF25D366)),
        AdminDashboardCardItem("features", "मुख्य शिक्षक सेवाएं (14+)", "${featureBoxes.count { it.isVisible }} सुविधाएं चालू", Icons.Default.Widgets, Color(0xFF8E24AA)),
        AdminDashboardCardItem("alert", "विशेष घोषणा (Alert)", "रेड / यलो ब्रॉडकास्ट", Icons.Default.Campaign, Color(0xFFE53935)),
        AdminDashboardCardItem("salary_rules", "वेतन व DA नियम", "DA: ${publishedDa.toInt()}% • HRA: ${hraUrban.toInt()}%", Icons.Default.AttachMoney, Color(0xFF00897B)),
        AdminDashboardCardItem("school_timing", "विद्यालय समय व बंदी", "समय: ${schoolTiming?.openingTime ?: "09:00 AM"} - ${schoolTiming?.closingTime ?: "04:30 PM"}", Icons.Default.AccessTime, Color(0xFF6D4C41)),
        AdminDashboardCardItem("top_links", "होम पेज लिंक्स (Top)", "${topLinks.size} लिंक्स सक्रिय", Icons.Default.Link, Color(0xFF039BE5)),
        AdminDashboardCardItem("complaints", "शिकायत निवारण (Sikayat)", "${complaints.size} शिकायत प्रकोष्ठ", Icons.Default.Security, Color(0xFFD81B60)),
        AdminDashboardCardItem("downloads", "डाउनलोड्स (PDF/JPG)", "${downloads.size} फॉर्म व Drive", Icons.Default.ArrowDownward, Color(0xFF3949AB)),
        AdminDashboardCardItem("loan", "शिक्षक लोन सुविधा", "${loanServices.size} बैंक लोन विकल्प", Icons.Default.AccountBalance, Color(0xFF546E7A)),
        AdminDashboardCardItem("card", "क्रेडिट कार्ड सेवा", "${creditCardServices.size} कार्ड विकल्प", Icons.Default.CreditCard, Color(0xFFF4511E)),
        AdminDashboardCardItem("invest", "निवेश व पेंशन", "${investmentServices.size} बचत योजनाएं", Icons.Default.TrendingUp, Color(0xFF2E7D32)),
        AdminDashboardCardItem("video_rss", "वीडियो सामग्री (YouTube)", "${videos.size} वीडियो लिंक सक्रिय", Icons.Default.VideoLibrary, Color(0xFFE53935)),
        AdminDashboardCardItem("news", "शिक्षा समाचार", "${newsArticles.size} समाचार सक्रिय", Icons.Default.Newspaper, Color(0xFF00ACC1)),
        AdminDashboardCardItem("news_rss", "लाइव ब्लॉग RSS (मगध टाइम्स)", "${newsFeeds.size} ब्लॉग RSS स्रोत", Icons.Default.RssFeed, Color(0xFFEF6C00)),
        AdminDashboardCardItem("banners", "बैनर प्रबंधन", "होम, लोन व WhatsApp बैनर", Icons.Default.Image, Color(0xFF7E57C2)),
        AdminDashboardCardItem("r2_storage", "Cloudflare R2 CDN", "PDF, इमेज, बैनर क्लाउड स्टोरेज", Icons.Default.CloudDone, Color(0xFFF57C00)),
        AdminDashboardCardItem("audit_logs", "सिस्टम ऑडिट लॉग", "${auditLogs.size} लॉग रिकॉर्ड्स", Icons.Default.History, Color(0xFF757575)),
        AdminDashboardCardItem("stats", "डैशबोर्ड सांख्यिकी", "सभी टेबल्स का विवरण", Icons.Default.Dashboard, Color(0xFF1565C0))
    )

    // Intercept hardware/system back button to go back to Admin Grid when inside a sub-screen
    BackHandler(enabled = activeAdminSubSection != null) {
        viewModel.setActiveAdminSubSection(null)
    }

    val activeCard = dashboardCards.find { it.id == activeAdminSubSection }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_dashboard")
    ) {
        // Admin Top Header Bar with dedicated Back Arrow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Navy900)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    IconButton(
                        onClick = {
                            if (activeAdminSubSection != null) {
                                viewModel.setActiveAdminSubSection(null)
                            } else {
                                onBack()
                            }
                        },
                        modifier = Modifier.testTag("admin_header_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = if (activeAdminSubSection != null) "Back to Admin Dashboard" else "Back to User App",
                            tint = PureWhite
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = activeCard?.title ?: "Admin Control Center",
                            color = PureWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1
                        )
                        Text(
                            text = if (activeAdminSubSection != null) "सब-पेज प्रबंधन | टैप करके वापस जाएं" else "Super Admin: ${session.username}",
                            color = Gold100,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isSyncingNow by viewModel.isSyncing.collectAsState()
                    Button(
                        onClick = {
                            viewModel.syncAllDataToCloud { success ->
                                Toast.makeText(
                                    context,
                                    if (success) "क्लाउड पर सभी डेटा सफलतापूर्वक सिंक हुआ!" else "सिंक विफल: इंटरनेट जांचें",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        enabled = !isSyncingNow,
                        colors = ButtonDefaults.buttonColors(containerColor = Gold500),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("admin_top_sync_btn")
                    ) {
                        if (isSyncingNow) {
                            CircularProgressIndicator(modifier = Modifier.size(13.dp), strokeWidth = 2.dp, color = Navy900)
                        } else {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Navy900, modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("सिंक करें", color = Navy900, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (activeAdminSubSection != null) {
                        OutlinedButton(
                            onClick = { viewModel.setActiveAdminSubSection(null) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("डैशबोर्ड", fontSize = 11.sp)
                        }
                    } else {
                        Button(
                            onClick = onLogout,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Logout", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Real-Time Sync Action Bar
        val isSyncing by viewModel.isSyncing.collectAsState()
        val syncMessage by viewModel.syncStatusMessage.collectAsState()

        Surface(
            color = Color(0xFF1E3A5F),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "डेटा सर्वर सिंक (Cloud Real-Time Sync)",
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = syncMessage ?: "अपलोड के बाद तुरंत सभी शिक्षकों को डेटा भेजने हेतु सिंक दबाएं",
                        color = Gold100,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        viewModel.syncAllDataToCloud { success ->
                            Toast.makeText(
                                context,
                                if (success) "क्लाउड पर सभी डेटा सफलतापूर्वक सिंक हुआ!" else "सिंक विफल: इंटरनेट जांचें",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    enabled = !isSyncing,
                    colors = ButtonDefaults.buttonColors(containerColor = Gold500),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("admin_force_sync_btn")
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = Navy900)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("सिंक हो रहा...", color = Navy900, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Navy900, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("क्लाउड सिंक", color = Navy900, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Sub-Screen vs 2-Column Grid Routing
        if (activeAdminSubSection == null) {
            // Main Admin Dashboard 2-Column Grid
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF4F6F9))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "प्रबंधन अनुभाग (Select Section to Edit)",
                        fontWeight = FontWeight.Bold,
                        color = Navy900,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "${dashboardCards.size} मॉड्युल्स",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = adminGridState,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("admin_dashboard_grid")
                ) {
                    items(dashboardCards, key = { it.id }) { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(106.dp)
                                .clickable { viewModel.setActiveAdminSubSection(item.id) }
                                .testTag("admin_card_${item.id}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(item.color.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.title,
                                            tint = item.color,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = item.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Navy900,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = item.subtitle,
                                        fontSize = 9.5.sp,
                                        color = Color.Gray,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Dedicated Sub-Screen Content
            when (activeAdminSubSection) {
                "notices" -> AdminNoticesTab(viewModel, listStateFor("notices"))
                "holidays" -> AdminHolidaysTab(viewModel, listStateFor("holidays"))
                "activities" -> AdminActivitiesTab(viewModel, listStateFor("activities"))
                "important_links" -> AdminImportantLinksTab(viewModel, listStateFor("important_links"))
                "whatsapp_channel" -> AdminWhatsAppChannelTab(viewModel, listStateFor("whatsapp_channel"))
                "alert" -> AdminUrgentAlertTab(viewModel, listStateFor("alert"))
                "salary_rules" -> AdminSalaryRulesTab(viewModel, listStateFor("salary_rules"))
                "school_timing" -> AdminSchoolTimingTab(viewModel, listStateFor("school_timing"))
                "features" -> AdminFeatureBoxesTab(viewModel, listStateFor("features"))
                "top_links" -> AdminDynamicTopLinksTab(viewModel, listStateFor("top_links"))
                "complaints" -> AdminComplaintsTab(viewModel, listStateFor("complaints"))
                "downloads" -> AdminDownloadsTab(viewModel, listStateFor("downloads"))
                "loan" -> AdminFinancialServiceTab(category = "LOAN", titleText = "शिक्षक लोन सुविधा", viewModel = viewModel, listState = listStateFor("loan"))
                "card" -> AdminFinancialServiceTab(category = "CREDIT_CARD", titleText = "क्रेडिट कार्ड सेवा", viewModel = viewModel, listState = listStateFor("card"))
                "invest" -> AdminFinancialServiceTab(category = "INVESTMENT", titleText = "निवेश व पेंशन", viewModel = viewModel, listState = listStateFor("invest"))
                "video_rss" -> AdminYouTubeRssTab(viewModel, listStateFor("video_rss"))
                "news" -> AdminNewsTab(viewModel, listStateFor("news"))
                "news_rss" -> AdminNewsRssTab(viewModel, listStateFor("news_rss"))
                "banners" -> AdminBannersConfigTab(viewModel, listStateFor("banners"))
                "r2_storage" -> AdminR2StorageTab(viewModel, listStateFor("r2_storage"))
                "audit_logs" -> AdminAuditLogsTab(viewModel, listStateFor("audit_logs"))
                "stats" -> AdminStatsTab(viewModel, listStateFor("stats"))
                else -> AdminStatsTab(viewModel, listStateFor("stats"))
            }
        }
    }
}

// -------------------------------------------------------------------
// TAB 0: STATS
// -------------------------------------------------------------------
@Composable
fun AdminStatsTab(
    viewModel: AdminViewModel,
    listState: LazyListState = rememberLazyListState()
) {
    val notices by viewModel.notices.collectAsState()
    val holidays by viewModel.holidays.collectAsState()
    val activities by viewModel.activities.collectAsState()
    val portals by viewModel.portals.collectAsState()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HeroBanner(
                title = "सिस्टम स्थिति एवं सांख्यिकी",
                subtitle = "विभागीय मुख्य व्यवस्थापक • लाइव क्लाउड डेटा सिंक",
                icon = Icons.Default.AdminPanelSettings
            )
        }

        item {
            ElevatedCard(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFE8F5E9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "📢 विशेष प्रशासनिक लाइव नोटिफिकेशन सक्रिय", fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20), fontSize = 13.sp)
                        Text(text = "आपके द्वारा अपलोड/संशोधित की गई प्रत्येक सामग्री सभी शिक्षकों के ऐप में तुरंत अपडेट होगी और लॉक स्क्रीन एवं बैकग्राउंड में विशेष नोटिफिकेशन भेजेगी।", fontSize = 11.sp, color = Color(0xFF2E7D32))
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdminStatCard("कुल विभागीय आदेश", "${notices.size}", Color(0xFF0D47A1), Modifier.weight(1f))
                AdminStatCard("अवकाश प्रविष्टियां", "${holidays.size}", Color(0xFFE65100), Modifier.weight(1f))
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdminStatCard("सक्रिय गतिविधियां (मशाल आदि)", "${activities.size}", Color(0xFF6A1B9A), Modifier.weight(1f))
                AdminStatCard("डिजिटल पोर्टल (सुविधा 3)", "${portals.size}", Color(0xFF00695C), Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun AdminStatCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = title, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

// -------------------------------------------------------------------
// TAB 1: NOTICES (ADD, EDIT, DELETE)
// -------------------------------------------------------------------
@Composable
fun AdminNoticesTab(
    viewModel: AdminViewModel,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val notices by viewModel.notices.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingNotice by remember { mutableStateOf<NoticeEntity?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "विभागीय आदेश (${notices.size}):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = {
                                viewModel.syncAllDataToCloud { s ->
                                    Toast.makeText(context, if (s) "क्लाउड सिंक सफल!" else "सिंक विफल", Toast.LENGTH_SHORT).show()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("admin_notices_sync_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("सिंक", fontSize = 11.5.sp)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("admin_add_notice_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("नया आदेश", fontSize = 11.5.sp)
                        }
                    }
                }
            }

            items(notices, key = { it.id }) { notice ->
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        if (notice.imageUrl.isNotBlank()) {
                            AppNetworkImage(
                                imageUrl = notice.imageUrl,
                                contentDescription = notice.title,
                                versionTag = notice.timestamp,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = notice.title, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = "श्रेणी: ${notice.category} • ज्ञापांक: ${notice.orderNumber}", fontSize = 11.sp, color = Color.Gray)
                                if (notice.summary.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = notice.summary, fontSize = 10.5.sp, color = Color.DarkGray, maxLines = 2)
                                }
                            }
                            Row {
                                IconButton(onClick = { editingNotice = notice }) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = Navy800)
                                }
                                IconButton(onClick = { viewModel.deleteNotice(notice) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Notice Dialog
    if (showAddDialog) {
        var title by remember(showAddDialog) { mutableStateOf("") }
        var summary by remember(showAddDialog) { mutableStateOf("") }
        var cat by remember(showAddDialog) { mutableStateOf("सरकारी आदेश") }
        var orderNo by remember(showAddDialog) { mutableStateOf("") }
        var pdfUrl by remember(showAddDialog) { mutableStateOf("") }
        var imageUrl by remember(showAddDialog) { mutableStateOf("") }
        var sendNotification by remember(showAddDialog) { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("नया परिपत्र प्रकाशित करें") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("शीर्षक (Title)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = summary, onValueChange = { summary = it }, label = { Text("सारांश (Summary)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = cat, onValueChange = { cat = it }, label = { Text("श्रेणी (e.g. वेतन/DA, अवकाश)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = orderNo, onValueChange = { orderNo = it }, label = { Text("ज्ञापांक / पत्रांक संख्या") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = pdfUrl, onValueChange = { pdfUrl = it }, label = { Text("PDF लिंक URL (R2 / Drive)") }, modifier = Modifier.fillMaxWidth())
                    R2UploadPickerRow(
                        adminViewModel = viewModel,
                        folder = R2Config.FOLDER_NOTICES,
                        titleHint = title.ifBlank { "notice_pdf" },
                        existingUrl = pdfUrl,
                        mode = R2UploadMode.PDF_ONLY,
                        testTagPrefix = "add_notice_pdf",
                        onUploaded = { uploaded -> pdfUrl = uploaded.url }
                    )
                    OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("इमेज URL (फोटो / आदेश स्कैन)") }, modifier = Modifier.fillMaxWidth())
                    R2UploadPickerRow(
                        adminViewModel = viewModel,
                        folder = R2Config.FOLDER_NOTICES,
                        titleHint = title.ifBlank { "notice_img" },
                        existingUrl = imageUrl,
                        mode = R2UploadMode.IMAGE_ONLY,
                        testTagPrefix = "add_notice_img",
                        onUploaded = { uploaded -> imageUrl = uploaded.url }
                    )
                    if (imageUrl.isNotBlank()) {
                        AppNetworkImage(
                            imageUrl = imageUrl.trim(),
                            contentDescription = "Notice Image Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    AdminNotificationToggleRow(
                        sendNotification = sendNotification,
                        onCheckedChange = { sendNotification = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.createNotice(
                                title = title.trim(),
                                summary = summary.trim(),
                                category = cat.trim(),
                                orderNumber = orderNo.trim(),
                                isFeatured = true,
                                sendNotification = sendNotification,
                                pdfUrl = pdfUrl.trim(),
                                imageUrl = imageUrl.trim()
                            )
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                ) {
                    Text("प्रकाशित करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("रद्द करें") }
            }
        )
    }

    // Edit Notice Dialog
    editingNotice?.let { notice ->
        var editTitle by remember(notice.id) { mutableStateOf(notice.title) }
        var editSummary by remember(notice.id) { mutableStateOf(notice.summary) }
        var editCat by remember(notice.id) { mutableStateOf(notice.category) }
        var editOrderNo by remember(notice.id) { mutableStateOf(notice.orderNumber) }
        var editPdfUrl by remember(notice.id) { mutableStateOf(notice.pdfUrl) }
        var editImageUrl by remember(notice.id) { mutableStateOf(notice.imageUrl) }
        var sendNotification by remember(notice.id) { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { editingNotice = null },
            title = { Text("आदेश संपादित करें (Edit Notice)") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("शीर्षक") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editSummary, onValueChange = { editSummary = it }, label = { Text("सारांश") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editCat, onValueChange = { editCat = it }, label = { Text("श्रेणी") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editOrderNo, onValueChange = { editOrderNo = it }, label = { Text("ज्ञापांक संख्या") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editPdfUrl, onValueChange = { editPdfUrl = it }, label = { Text("PDF लिंक URL (R2 / Drive)") }, modifier = Modifier.fillMaxWidth())
                    R2UploadPickerRow(
                        adminViewModel = viewModel,
                        folder = R2Config.FOLDER_NOTICES,
                        titleHint = editTitle.ifBlank { "notice_pdf" },
                        existingUrl = editPdfUrl,
                        mode = R2UploadMode.PDF_ONLY,
                        testTagPrefix = "edit_notice_pdf",
                        onUploaded = { uploaded -> editPdfUrl = uploaded.url }
                    )
                    OutlinedTextField(value = editImageUrl, onValueChange = { editImageUrl = it }, label = { Text("इमेज URL (फोटो / आदेश स्कैन)") }, modifier = Modifier.fillMaxWidth())
                    R2UploadPickerRow(
                        adminViewModel = viewModel,
                        folder = R2Config.FOLDER_NOTICES,
                        titleHint = editTitle.ifBlank { "notice_img" },
                        existingUrl = editImageUrl,
                        mode = R2UploadMode.IMAGE_ONLY,
                        testTagPrefix = "edit_notice_img",
                        onUploaded = { uploaded -> editImageUrl = uploaded.url }
                    )
                    if (editImageUrl.isNotBlank()) {
                        AppNetworkImage(
                            imageUrl = editImageUrl.trim(),
                            contentDescription = "Notice Image Preview",
                            versionTag = notice.timestamp,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    AdminNotificationToggleRow(
                        sendNotification = sendNotification,
                        onCheckedChange = { sendNotification = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editTitle.isNotBlank()) {
                            viewModel.updateNotice(
                                notice = notice,
                                newTitle = editTitle.trim(),
                                newSummary = editSummary.trim(),
                                newCategory = editCat.trim(),
                                newOrderNumber = editOrderNo.trim(),
                                isFeatured = true,
                                sendNotification = sendNotification,
                                newPdfUrl = editPdfUrl.trim(),
                                newImageUrl = editImageUrl.trim()
                            )
                            editingNotice = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                ) {
                    Text("अद्यतन करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingNotice = null }) { Text("रद्द करें") }
            }
        )
    }
}

// -------------------------------------------------------------------
// TAB 2: HOLIDAYS (ADD, EDIT, DELETE)
// -------------------------------------------------------------------
@Composable
fun AdminHolidaysTab(
    viewModel: AdminViewModel,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val holidays by viewModel.holidays.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingHoliday by remember { mutableStateOf<HolidayEntity?>(null) }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "अवकाश तालिका (${holidays.size}):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            viewModel.syncAllDataToCloud { s ->
                                Toast.makeText(context, if (s) "क्लाउड सिंक सफल!" else "सिंक विफल", Toast.LENGTH_SHORT).show()
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("admin_holidays_sync_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("सिंक", fontSize = 11.5.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = { showDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("admin_add_holiday_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("अवकाश जोड़ें", fontSize = 11.5.sp)
                    }
                }
            }
        }

        items(holidays, key = { it.id }) { holiday ->
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = holiday.name, fontWeight = FontWeight.Bold)
                        Text(text = "${holiday.dateStr} • ${holiday.type} (${holiday.durationDays} दिन)", fontSize = 11.sp, color = Color.Gray)
                    }
                    Row {
                        IconButton(onClick = { editingHoliday = holiday }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = Navy800)
                        }
                        IconButton(onClick = { viewModel.deleteHoliday(holiday) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        var name by remember { mutableStateOf("") }
        var dateStr by remember { mutableStateOf("01/01/2026") }
        var type by remember { mutableStateOf("त्योहार") }
        var month by remember { mutableIntStateOf(1) }
        var duration by remember { mutableIntStateOf(1) }
        var sendNotification by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("नया अवकाश जोड़ें") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("अवकाश का नाम") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = dateStr, onValueChange = { dateStr = it }, label = { Text("दिनांक (dd/MM/yyyy)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("प्रकार (e.g. त्योहार / सरकारी अवकाश)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    AdminNotificationToggleRow(
                        sendNotification = sendNotification,
                        onCheckedChange = { sendNotification = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.addHoliday(name.trim(), dateStr.trim(), type.trim(), month, duration, sendNotification = sendNotification)
                            showDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                ) {
                    Text("जोड़ें एवं सहेजें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("रद्द करें") }
            }
        )
    }

    editingHoliday?.let { hol ->
        var editName by remember { mutableStateOf(hol.name) }
        var editDate by remember { mutableStateOf(hol.dateStr) }
        var editType by remember { mutableStateOf(hol.type) }
        var editDays by remember { mutableIntStateOf(hol.durationDays) }
        var sendNotification by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { editingHoliday = null },
            title = { Text("अवकाश संशोधित करें") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("अवकाश का नाम") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editDate, onValueChange = { editDate = it }, label = { Text("दिनांक (dd/MM/yyyy)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editType, onValueChange = { editType = it }, label = { Text("प्रकार") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    AdminNotificationToggleRow(
                        sendNotification = sendNotification,
                        onCheckedChange = { sendNotification = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            viewModel.updateHoliday(hol, editName.trim(), editDate.trim(), editType.trim(), hol.month, editDays, sendNotification = sendNotification)
                            editingHoliday = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                ) {
                    Text("सहेजें")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingHoliday = null }) { Text("रद्द करें") }
            }
        )
    }
}

// -------------------------------------------------------------------
// TAB 3: ACTIVITIES (MASHAL, ECO CLUB - ADD, EDIT, DELETE)
// -------------------------------------------------------------------
@Composable
fun AdminActivitiesTab(
    viewModel: AdminViewModel,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val activities by viewModel.activities.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAct by remember { mutableStateOf<EducationActivityEntity?>(null) }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "गतिविधियां (${activities.size}):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            viewModel.syncAllDataToCloud { s ->
                                Toast.makeText(context, if (s) "क्लाउड सिंक सफल!" else "सिंक विफल", Toast.LENGTH_SHORT).show()
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("admin_activities_sync_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("सिंक", fontSize = 11.5.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("admin_add_activity_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("नई गतिविधि", fontSize = 11.5.sp)
                    }
                }
            }
        }

        items(activities, key = { it.id }) { act ->
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = act.title, fontWeight = FontWeight.Bold)
                        Text(text = "वर्ग: ${act.category} • लिंक: ${act.linkUrl}", fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                        if (act.description.isNotBlank()) {
                            Text(text = act.description, fontSize = 10.5.sp, color = Color.DarkGray, maxLines = 2)
                        }
                    }
                    Row {
                        IconButton(onClick = { editingAct = act }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = Navy800)
                        }
                        IconButton(onClick = { viewModel.deleteActivity(act) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var cat by remember { mutableStateOf("अभियान") }
        var desc by remember { mutableStateOf("") }
        var linkUrl by remember { mutableStateOf("https://state.bihar.gov.in/educationbihar") }
        var orderNo by remember { mutableStateOf("") }
        var sendNotification by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("नई गतिविधि / अभियान जोड़ें (मशाल, इको क्लब आदि)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("गतिविधि का नाम (e.g. मशाल, इको क्लब)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = cat, onValueChange = { cat = it }, label = { Text("श्रेणी (अभियान / पर्यावरण / खेलकूद / सुरक्षा)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("विवरण / उद्देश्य") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = linkUrl, onValueChange = { linkUrl = it }, label = { Text("आधिकारिक लिंक / पोर्टल URL") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = orderNo, onValueChange = { orderNo = it }, label = { Text("पत्रांक / आदेश संख्या") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    AdminNotificationToggleRow(
                        sendNotification = sendNotification,
                        onCheckedChange = { sendNotification = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.addActivity(title.trim(), cat.trim(), desc.trim(), linkUrl.trim(), orderNo.trim(), sendNotification = sendNotification)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                ) {
                    Text("जोड़ें एवं सहेजें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("रद्द करें") }
            }
        )
    }

    editingAct?.let { act ->
        var editTitle by remember { mutableStateOf(act.title) }
        var editCat by remember { mutableStateOf(act.category) }
        var editDesc by remember { mutableStateOf(act.description) }
        var editLink by remember { mutableStateOf(act.linkUrl) }
        var editOrder by remember { mutableStateOf(act.orderNumber) }
        var sendNotification by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { editingAct = null },
            title = { Text("गतिविधि संपादित करें") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("गतिविधि का नाम") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editCat, onValueChange = { editCat = it }, label = { Text("श्रेणी") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editDesc, onValueChange = { editDesc = it }, label = { Text("विवरण") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editLink, onValueChange = { editLink = it }, label = { Text("लिंक URL") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editOrder, onValueChange = { editOrder = it }, label = { Text("पत्रांक संख्या") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    AdminNotificationToggleRow(
                        sendNotification = sendNotification,
                        onCheckedChange = { sendNotification = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editTitle.isNotBlank()) {
                            viewModel.updateActivity(act, editTitle.trim(), editCat.trim(), editDesc.trim(), editLink.trim(), editOrder.trim(), sendNotification = sendNotification)
                            editingAct = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                ) {
                    Text("सहेजें")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingAct = null }) { Text("रद्द करें") }
            }
        )
    }
}

// -------------------------------------------------------------------
// TAB 4: QUICK PORTALS & LINKS (ADD, EDIT, DELETE)
// -------------------------------------------------------------------
@Composable
fun AdminPortalsTab(
    viewModel: AdminViewModel,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val portals by viewModel.portals.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingPortal by remember { mutableStateOf<QuickPortalEntity?>(null) }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "डिजिटल पोर्टल (सुविधा 3) (${portals.size}):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            viewModel.syncAllDataToCloud { s ->
                                Toast.makeText(context, if (s) "क्लाउड सिंक सफल!" else "सिंक विफल", Toast.LENGTH_SHORT).show()
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("admin_portals_sync_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("सिंक", fontSize = 11.5.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("admin_add_portal_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("नया डिजिटल पोर्टल (+)", fontSize = 11.5.sp)
                    }
                }
            }
        }

        items(portals, key = { it.id }) { portal ->
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = portal.title, fontWeight = FontWeight.Bold)
                        Text(text = portal.subtitle, fontSize = 11.sp, color = Color.Gray)
                        Text(text = portal.url, fontSize = 10.sp, color = Navy800, maxLines = 1)
                    }
                    Row {
                        IconButton(onClick = { editingPortal = portal }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = Navy800)
                        }
                        IconButton(onClick = { viewModel.deletePortal(portal) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var subtitle by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("https://") }
        var badge by remember { mutableStateOf("डिजिटल पोर्टल") }
        var sendNotification by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("नया डिजिटल पोर्टल (सुविधा 3) जोड़ें") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("पोर्टल का नाम (e.g. ई-शिक्षाकोष)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = subtitle, onValueChange = { subtitle = it }, label = { Text("उप-शीर्षक / संक्षिप्त विवरण") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("वेबसाइट URL") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = badge, onValueChange = { badge = it }, label = { Text("टैग / बैज") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    AdminNotificationToggleRow(
                        sendNotification = sendNotification,
                        onCheckedChange = { sendNotification = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.addPortal(title.trim(), subtitle.trim(), url.trim(), badge.trim(), "#0D47A1", sendNotification = sendNotification)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                ) {
                    Text("जोड़ें एवं सहेजें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("रद्द करें") }
            }
        )
    }

    editingPortal?.let { p ->
        var editTitle by remember { mutableStateOf(p.title) }
        var editSub by remember { mutableStateOf(p.subtitle) }
        var editUrl by remember { mutableStateOf(p.url) }
        var editBadge by remember { mutableStateOf(p.badge) }
        var sendNotification by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { editingPortal = null },
            title = { Text("डिजिटल पोर्टल (सुविधा 3) संपादित करें") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("पोर्टल का नाम") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editSub, onValueChange = { editSub = it }, label = { Text("विवरण") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editUrl, onValueChange = { editUrl = it }, label = { Text("URL") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editBadge, onValueChange = { editBadge = it }, label = { Text("बैज") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    AdminNotificationToggleRow(
                        sendNotification = sendNotification,
                        onCheckedChange = { sendNotification = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editTitle.isNotBlank()) {
                            viewModel.updatePortal(p, editTitle.trim(), editSub.trim(), editUrl.trim(), editBadge.trim(), p.colorHex, sendNotification = sendNotification)
                            editingPortal = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                ) {
                    Text("सहेजें")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPortal = null }) { Text("रद्द करें") }
            }
        )
    }
}

@Composable
fun AdminImportantLinksTab(
    viewModel: AdminViewModel,
    listState: LazyListState = rememberLazyListState()
) = AdminPortalsTab(viewModel, listState)

@Composable
fun AdminWhatsAppChannelTab(
    viewModel: AdminViewModel,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val whatsAppBanner by viewModel.whatsAppBanner.collectAsState()
    val featureBoxes by viewModel.featureBoxes.collectAsState()
    val waBox = featureBoxes.find { it.id == "whatsapp" }

    var waTitle by remember(waBox) { mutableStateOf(waBox?.title ?: "व्हाट्सएप चैनल") }
    var waSub by remember(waBox) { mutableStateOf(waBox?.subtitle ?: "शिक्षक साथी आधिकारिक चैनल") }
    var waUrl by remember(waBox) { mutableStateOf(waBox?.externalUrl ?: "https://whatsapp.com/channel/0029Va4x7Kq90x2s") }
    var waIsActive by remember(waBox) { mutableStateOf(waBox?.isVisible ?: true) }

    var bannerText by remember(whatsAppBanner) { mutableStateOf(whatsAppBanner?.bannerText ?: "बिहार के शिक्षकों से जुड़ी महत्वपूर्ण सूचनाएं व अपडेट्स प्राप्त करें।") }
    var bannerBtn by remember(whatsAppBanner) { mutableStateOf(whatsAppBanner?.buttonText ?: "JOIN WHATSAPP CHANNEL") }
    var bannerUrl by remember(whatsAppBanner) { mutableStateOf(whatsAppBanner?.channelUrl ?: "https://whatsapp.com/channel/0029Va4x7Kq90x2s") }
    var bannerVisible by remember(whatsAppBanner) { mutableStateOf(whatsAppBanner?.isVisible ?: true) }

    fun openUrl(url: String) {
        if (url.isBlank()) return
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "लिंक खोलने में त्रुटि", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HeroBanner(
                title = "व्हाट्सएप चैनल प्रबंधन (सुविधा 14)",
                subtitle = "सर्विस कार्ड नं. 14 एवं होम स्क्रीन जॉइनिंग बैनर का संपूर्ण नियंत्रण व लाइव सिंक",
                icon = Icons.Default.Campaign
            )
        }

        // Section 1: Service No. 14 Feature Box Management
        item {
            ElevatedCard(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = PureWhite),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "सुविधा 14: मुख्य स्क्रीन व्हाट्सएप कार्ड",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Navy900
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (waIsActive) "सक्रिय (ON)" else "बंद (OFF)", fontSize = 11.sp, color = if (waIsActive) Color(0xFF2E7D32) else Color.Red)
                            Spacer(modifier = Modifier.width(4.dp))
                            Switch(checked = waIsActive, onCheckedChange = { waIsActive = it })
                        }
                    }
                    OutlinedTextField(
                        value = waTitle,
                        onValueChange = { waTitle = it },
                        label = { Text("कार्ड शीर्षक (Title)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = waSub,
                        onValueChange = { waSub = it },
                        label = { Text("उप-शीर्षक / विवरण (Subtitle)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = waUrl,
                        onValueChange = { waUrl = it },
                        label = { Text("व्हाट्सएप चैनल URL / इनवाइट लिंक") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                if (waBox != null) {
                                    viewModel.updateFeatureBox(waBox, waTitle.trim(), waSub.trim(), waUrl.trim(), waIsActive)
                                }
                                Toast.makeText(context, "सुविधा 14 सफलतापूर्वक अपडेट हुई!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("सेव व लाइव सिंक")
                        }
                        OutlinedButton(
                            onClick = { openUrl(waUrl) },
                            modifier = Modifier.weight(0.7f)
                        ) {
                            Text("चैनल जांचें")
                        }
                    }
                }
            }
        }

        // Section 2: Home Screen WhatsApp Join Banner
        item {
            ElevatedCard(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = PureWhite),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "होम स्क्रीन व्हाट्सएप जॉइनिंग बैनर",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Navy900
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (bannerVisible) "चालू" else "बंद", fontSize = 11.sp, color = if (bannerVisible) Color(0xFF2E7D32) else Color.Red)
                            Spacer(modifier = Modifier.width(4.dp))
                            Switch(checked = bannerVisible, onCheckedChange = { bannerVisible = it })
                        }
                    }
                    OutlinedTextField(
                        value = bannerText,
                        onValueChange = { bannerText = it },
                        label = { Text("बैनर संदेश (Message)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = bannerBtn,
                            onValueChange = { bannerBtn = it },
                            label = { Text("बटन टेक्स्ट") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = bannerUrl,
                            onValueChange = { bannerUrl = it },
                            label = { Text("चैनल लिंक URL") },
                            modifier = Modifier.weight(1.5f)
                        )
                    }
                    Button(
                        onClick = {
                            viewModel.saveWhatsAppBanner(bannerText.trim(), bannerBtn.trim(), bannerUrl.trim(), bannerVisible)
                            Toast.makeText(context, "व्हाट्सएप बैनर अपडेट हुआ!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Campaign, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("व्हाट्सएप बैनर अपडेट करें व लाइव सिंक", color = PureWhite)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------
// TAB 5: URGENT BROADCAST & LOUD ALARM TRIGGER
// -------------------------------------------------------------------
@Composable
fun AdminUrgentAlertTab(
    viewModel: AdminViewModel,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var section by remember { mutableStateOf("विशेष प्रशासनिक निर्देश") }
    var details by remember { mutableStateOf("") }

    val broadcastHistory by viewModel.broadcastHistory.collectAsState()
    val isLoadingHistory by viewModel.isLoadingBroadcastHistory.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadBroadcastHistory(forceServer = false)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HeroBanner(
                title = "विशेष प्रशासनिक घोषणा (Broadcast)",
                subtitle = "सभी उपयोगकर्ता उपकरणों पर पृष्ठभूमि एवं लॉक स्क्रीन पर उच्च-प्राथमिकता नोटिफिकेशन भेजें",
                icon = Icons.Default.Campaign
            )
        }

        item {
            ElevatedCard(
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "📢 विशेष घोषणा जारी करें (Post Live Notification):",
                        fontWeight = FontWeight.Bold,
                        color = Navy900,
                        fontSize = 14.sp
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("घोषणा का मुख्य शीर्षक") },
                        placeholder = { Text("उदा. शीतकालीन समय परिवर्तन / आवश्यक बैठक") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = section,
                        onValueChange = { section = it },
                        label = { Text("अनुभाग / विषय") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = details,
                        onValueChange = { details = it },
                        label = { Text("विस्तृत निर्देश एवं आदेश विवरण") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                viewModel.sendBroadcastNotification(title.trim(), section.trim(), details.trim())
                                Toast.makeText(context, "विशेष नोटिफिकेशन सभी उपयोगकर्ताओं को भेजा गया!", Toast.LENGTH_LONG).show()
                                title = ""
                                details = ""
                            } else {
                                Toast.makeText(context, "कृपया शीर्षक दर्ज करें", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📢 घोषणा पोस्ट करें (सभी उपयोगकर्ताओं को नोटिफिकेशन भेजें)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "पिछले 10 ब्रॉडकास्ट इतिहास (${broadcastHistory.size}/10):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = Navy900
                )
                IconButton(
                    onClick = { viewModel.loadBroadcastHistory(forceServer = true) },
                    enabled = !isLoadingHistory,
                    modifier = Modifier.testTag("refresh_broadcast_history_btn")
                ) {
                    if (isLoadingHistory) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Navy800)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "इतिहास रीफ्रेश करें",
                            tint = Navy800
                        )
                    }
                }
            }
        }

        if (broadcastHistory.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = if (isLoadingHistory) "नवीनतम 10 ब्रॉडकास्ट लोड हो रहे हैं..." else "कोई हालिया ब्रॉडकास्ट इतिहास उपलब्ध नहीं है।",
                        modifier = Modifier.padding(16.dp),
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            items(broadcastHistory.take(10), key = { it.alertId.ifBlank { it.timestamp.toString() } }) { alert ->
                val formattedDate = remember(alert.timestamp) {
                    java.text.SimpleDateFormat("dd/MM/yyyy hh:mm a", java.util.Locale.getDefault())
                        .format(java.util.Date(alert.timestamp))
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFFE8EAF6),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = alert.section,
                                    color = Navy900,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = formattedDate,
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                        Text(
                            text = alert.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Navy900
                        )
                        if (alert.details.isNotBlank()) {
                            Text(
                                text = alert.details,
                                fontSize = 11.5.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------
// TAB 6: SALARY RULES
// -------------------------------------------------------------------
@Composable
fun AdminSalaryRulesTab(
    viewModel: AdminViewModel,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    var daInput by remember { mutableStateOf("53") }
    var ruralHra by remember { mutableStateOf("8") }
    var urbanHra by remember { mutableStateOf("16") }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HeroBanner(
                title = "वेतन एवं DA/HRA नियम प्रबंधन",
                subtitle = "राज्यव्यापी शिक्षकों हेतु प्रकाशित दरें अद्यतन करें",
                icon = Icons.Default.TrendingUp
            )
        }

        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "वर्तमान वेतनमान दरें (Current Rules):", fontWeight = FontWeight.Bold, color = Navy800)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = daInput,
                        onValueChange = { daInput = it },
                        label = { Text("प्रकाशित DA प्रतिशत (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = ruralHra,
                            onValueChange = { ruralHra = it },
                            label = { Text("ग्रामीण/प्रखंड HRA (%)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = urbanHra,
                            onValueChange = { urbanHra = it },
                            label = { Text("शहरी/पटना HRA (%)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val newDa = daInput.toDoubleOrNull() ?: 53.0
                            val rHra = ruralHra.toDoubleOrNull() ?: 8.0
                            val uHra = urbanHra.toDoubleOrNull() ?: 16.0
                            viewModel.publishSalaryRule(newDa, rHra, uHra)
                            Toast.makeText(context, "वेतन नियम सफलतापूर्वक प्रकाशित किए गए", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                    ) {
                        Text("नवीन नियम प्रकाशित करें एवं ब्रॉडकास्ट करें")
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------
// TAB 7: AUDIT LOGS
// -------------------------------------------------------------------
@Composable
fun AdminAuditLogsTab(
    viewModel: AdminViewModel,
    listState: LazyListState = rememberLazyListState()
) {
    val logs by viewModel.auditLogs.collectAsState()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "प्रशासनिक सुरक्षा एवं परिवर्तन ऑडिट लॉग (${logs.size}):",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Navy900
            )
        }

        if (logs.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                    Text(
                        text = "कोई ऑडिट लॉग दर्ज नहीं है।",
                        modifier = Modifier.padding(16.dp),
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            items(logs, key = { it.id }) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = log.action, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Navy800)
                            Text(text = log.adminUser, fontSize = 11.sp, color = Gold700, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = log.details, fontSize = 11.sp, color = Color.DarkGray)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------
// TAB 8: PROMOTIONAL BANNERS MANAGEMENT
// -------------------------------------------------------------------
@Composable
fun AdminPromoBannersTab(
    viewModel: AdminViewModel,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val banners by viewModel.promoBanners.collectAsState()
    var editingBanner by remember { mutableStateOf<com.example.database.entities.PromoBannerEntity?>(null) }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Gold100),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "होम स्क्रीन प्रमोशनल बैनर प्रबंधन (3 Banners)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Navy900
                    )
                    Text(
                        text = "यहाँ से आप पर्सनल लोन, क्रेडिट कार्ड या कोई भी तीसरा बैनर संपादित कर सकते हैं। बिना नया APK जारी किए सभी शिक्षकों को लाइव दिखाई देगा।",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                }
            }
        }

        items(banners, key = { it.id }) { banner ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Navy900,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = banner.category,
                                color = Gold500,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Text(
                            text = if (banner.isActive) "सक्रिय (Active)" else "निष्क्रिय (Inactive)",
                            color = if (banner.isActive) Color(0xFF2E7D32) else Color.Red,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = banner.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Navy900)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = banner.description, fontSize = 12.sp, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "URL: ${banner.destinationUrl}", fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                    Text(text = "बटन: ${banner.buttonText}", fontSize = 11.sp, color = Teal700, fontWeight = FontWeight.SemiBold)

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { editingBanner = banner },
                            colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = Gold500)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("संपादित करें", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    editingBanner?.let { current ->
        var title by remember(current.id) { mutableStateOf(current.title) }
        var desc by remember(current.id) { mutableStateOf(current.description) }
        var imgUrl by remember(current.id) { mutableStateOf(current.imageUrl) }
        var destUrl by remember(current.id) { mutableStateOf(current.destinationUrl) }
        var btnText by remember(current.id) { mutableStateOf(current.buttonText) }
        var isActive by remember(current.id) { mutableStateOf(current.isActive) }

        AlertDialog(
            onDismissRequest = { editingBanner = null },
            title = { Text("बैनर संपादित करें (${current.category})", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("बैनर शीर्षक") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("संक्षिप्त विवरण") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = imgUrl,
                        onValueChange = { imgUrl = it },
                        label = { Text("इमेज URL (बैनर फोटो)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    R2UploadPickerRow(
                        adminViewModel = viewModel,
                        folder = R2Config.FOLDER_BANNERS,
                        titleHint = title.ifBlank { "promo_banner" },
                        existingUrl = imgUrl,
                        mode = R2UploadMode.IMAGE_ONLY,
                        testTagPrefix = "promo_tab_img",
                        onUploaded = { uploaded -> imgUrl = uploaded.url }
                    )
                    if (imgUrl.isNotBlank()) {
                        AppNetworkImage(
                            imageUrl = imgUrl.trim(),
                            contentDescription = "Promo Banner Preview",
                            versionTag = current.timestamp,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                    OutlinedTextField(
                        value = destUrl,
                        onValueChange = { destUrl = it },
                        label = { Text("गंतव्य URL (Destination Link)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = btnText,
                        onValueChange = { btnText = it },
                        label = { Text("बटन टेक्स्ट (उदा. आवेदन करें)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("सक्रिय रखें (Enable):", fontSize = 13.sp)
                        androidx.compose.material3.Switch(
                            checked = isActive,
                            onCheckedChange = { isActive = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = current.copy(
                            title = title.trim(),
                            description = desc.trim(),
                            imageUrl = imgUrl.trim(),
                            destinationUrl = destUrl.trim(),
                            buttonText = btnText.trim(),
                            isActive = isActive,
                            timestamp = System.currentTimeMillis()
                        )
                        viewModel.savePromoBanner(updated)
                        editingBanner = null
                        Toast.makeText(context, "बैनर अपडेट किया गया एवं क्लाउड पर प्रकाशित हुआ", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                ) {
                    Text("सहेजें व क्लाउड सिंक")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingBanner = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}

// -------------------------------------------------------------------
// TAB 9: SCHOOL TIMINGS & CLOSURE MANAGEMENT
// -------------------------------------------------------------------
@Composable
fun AdminSchoolTimingTab(
    viewModel: AdminViewModel,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val currentTiming by viewModel.schoolTiming.collectAsState()

    var openingTime by remember(currentTiming) { mutableStateOf(currentTiming?.openingTime ?: "09:00 AM") }
    var closingTime by remember(currentTiming) { mutableStateOf(currentTiming?.closingTime ?: "04:30 PM") }
    var isClosed by remember(currentTiming) { mutableStateOf(currentTiming?.isManuallyClosed ?: false) }
    var closureReason by remember(currentTiming) { mutableStateOf(currentTiming?.closureReason ?: "") }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Gold100),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "विद्यालय समय एवं प्रशासनिक अवकाश/बंदी नियंत्रण",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Navy900
                    )
                    Text(
                        text = "यहाँ से आप विद्यालय खुलने एवं बंद होने का समय बदल सकते हैं या शीतलहर, अत्यधिक गर्मी, चुनाव आदि कारणों से विशेष बंदी घोषित कर सकते हैं।",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "दैनिक विद्यालय समय निर्धारण:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Navy900)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = openingTime,
                            onValueChange = { openingTime = it },
                            label = { Text("खुलने का समय") },
                            placeholder = { Text("09:00 AM") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = closingTime,
                            onValueChange = { closingTime = it },
                            label = { Text("बंद होने का समय") },
                            placeholder = { Text("04:30 PM") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.LightGray))
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(text = "विशेष आकस्मिक विद्यालय बंदी (Admin Closure):", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Navy900)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isClosed) "विद्यालय बंद घोषित है (CLOSED)" else "विद्यालय सामान्य रूप से खुला है (NORMAL)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = if (isClosed) Color(0xFFC62828) else Color(0xFF2E7D32)
                        )
                        androidx.compose.material3.Switch(
                            checked = isClosed,
                            onCheckedChange = { isClosed = it }
                        )
                    }

                    if (isClosed) {
                        OutlinedTextField(
                            value = closureReason,
                            onValueChange = { closureReason = it },
                            label = { Text("बंदी का आधिकारिक कारण") },
                            placeholder = { Text("उदा. अत्यधिक शीतलहर के कारण जिलाधिकारी का आदेश") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.updateSchoolTimings(openingTime, closingTime, isClosed, closureReason)
                            Toast.makeText(context, "विद्यालय समय व स्थिति अपडेट हो गई", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("समय व बंदी स्थिति सहेजें (Save & Sync)")
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------
// TAB 10: WHATSAPP LINKS MANAGEMENT
// -------------------------------------------------------------------
@Composable
fun AdminWhatsAppConfigTab(
    viewModel: AdminViewModel,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val currentConfig by viewModel.whatsAppConfig.collectAsState()

    var channelUrl by remember(currentConfig) { mutableStateOf(currentConfig?.channelUrl ?: "https://whatsapp.com/channel/0029Va4yZ1m2k345") }
    var channelTitle by remember(currentConfig) { mutableStateOf(currentConfig?.channelTitle ?: "शिक्षा विभाग बिहार - शिक्षक साथी आधिकारिक चैनल") }
    var groupsUrl by remember(currentConfig) { mutableStateOf(currentConfig?.districtGroupsUrl ?: "https://state.bihar.gov.in/educationbihar") }
    var groupsTitle by remember(currentConfig) { mutableStateOf(currentConfig?.districtGroupsTitle ?: "बिहार के 38 जिलों एवं प्रखंडों के शिक्षक व्हाट्सएप ग्रुप") }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Gold100),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "व्हाट्सएप चैनल एवं जिला ग्रुप लिंक प्रबंधन",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Navy900
                    )
                    Text(
                        text = "यहाँ से आप होम स्क्रीन पर दिखने वाले व्हाट्सएप चैनल का URL और बिहार के 38 जिलों के व्हाट्सएप ग्रुप निर्देशिका का वेब लिंक कभी भी बदल सकते हैं।",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "कार्ड A: आधिकारिक व्हाट्सएप चैनल:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Navy900)

                    OutlinedTextField(
                        value = channelTitle,
                        onValueChange = { channelTitle = it },
                        label = { Text("चैनल शीर्षक") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = channelUrl,
                        onValueChange = { channelUrl = it },
                        label = { Text("चैनल इनवाइट लिंक (URL)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.LightGray))
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(text = "कार्ड B: जिला/प्रखंड व्हाट्सएप ग्रुप वेबसाइट:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Navy900)

                    OutlinedTextField(
                        value = groupsTitle,
                        onValueChange = { groupsTitle = it },
                        label = { Text("ग्रुप डायरेक्टरी शीर्षक") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = groupsUrl,
                        onValueChange = { groupsUrl = it },
                        label = { Text("ग्रुप डायरेक्टरी वेब URL") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            viewModel.updateWhatsAppLinks(channelUrl, channelTitle, groupsUrl, groupsTitle)
                            Toast.makeText(context, "व्हाट्सएप लिंक सफलतापूर्वक अपडेट किए गए", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("व्हाट्सएप लिंक्स सहेजें व क्लाउड सिंक करें")
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------
// TAB 11: 14 MAIN FEATURE BOXES (ORDERING, EDIT, VISIBILITY)
// -------------------------------------------------------------------
@Composable
fun AdminFeatureBoxesTab(
    viewModel: AdminViewModel,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val boxes by viewModel.featureBoxes.collectAsState()
    val sortedBoxes = remember(boxes) { boxes.sortedBy { it.displayOrder } }
    var editingBox by remember { mutableStateOf<FeatureBoxEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ElevatedCard(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Navy900),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "मुख्य शिक्षक सेवाएं (${sortedBoxes.size} सेवाएं)",
                                color = PureWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "1-14 सेवाएं सुरक्षित हैं। आप असीमित नई सेवाएं (15, 16, 17...) जोड़ सकते हैं जो होम स्क्रीन पर तुरंत लाइव दिखेंगी।",
                                color = Gold100,
                                fontSize = 11.sp
                            )
                        }
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Gold500),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Navy900, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("नई सेवा (+)", color = Navy900, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        items(sortedBoxes, key = { it.id }) { box ->
            val index = sortedBoxes.indexOf(box)
            ElevatedCard(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (box.isVisible) PureWhite else Color(0xFFF0F0F0)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Order Badge
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Navy800),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${box.displayOrder}",
                            color = Gold500,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = box.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Navy900
                        )
                        if (box.subtitle.isNotBlank()) {
                            Text(
                                text = box.subtitle,
                                fontSize = 11.sp,
                                color = Color.DarkGray,
                                maxLines = 1
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (box.isVisible) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (box.isVisible) "● दृश्यमान (Visible)" else "○ छुपा हुआ (Hidden)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (box.isVisible) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                            if (box.externalUrl.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "वेब लिंक",
                                    fontSize = 10.sp,
                                    color = Teal700,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Move Up
                    IconButton(
                        onClick = { viewModel.moveFeatureBox(box, -1) },
                        enabled = index > 0,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Move Up",
                            tint = if (index > 0) Navy800 else Color.LightGray
                        )
                    }

                    // Move Down
                    IconButton(
                        onClick = { viewModel.moveFeatureBox(box, 1) },
                        enabled = index < sortedBoxes.size - 1,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Move Down",
                            tint = if (index < sortedBoxes.size - 1) Navy800 else Color.LightGray
                        )
                    }

                    // Edit
                    IconButton(
                        onClick = { editingBox = box },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Navy800
                        )
                    }

                    // Delete (for custom added services)
                    if (box.id.startsWith("service_") || box.displayOrder > 14) {
                        IconButton(
                            onClick = {
                                viewModel.deleteFeatureBox(box)
                                Toast.makeText(context, "सेवा हटाई गई", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFC62828)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var subtitle by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("https://") }
        var isVisible by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = "नई मुख्य सेवा जोड़ें (#${(sortedBoxes.maxOfOrNull { it.displayOrder } ?: 14) + 1})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("सेवा का नाम (Title)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = subtitle,
                        onValueChange = { subtitle = it },
                        label = { Text("संक्षिप्त विवरण (Subtitle)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("वेब लिंक URL (Click Destination)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("होम स्क्रीन पर चालू रखें (ON/OFF)", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        Switch(
                            checked = isVisible,
                            onCheckedChange = { isVisible = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.addNewFeatureBox(title, subtitle, url, "Widgets", isVisible)
                            Toast.makeText(context, "नई सेवा सफलतापूर्वक जोड़ी गई!", Toast.LENGTH_SHORT).show()
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                ) {
                    Text("जोड़ें व लाइव सिंक")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    editingBox?.let { box ->
        var title by remember(box.id) { mutableStateOf(box.title) }
        var subtitle by remember(box.id) { mutableStateOf(box.subtitle) }
        var url by remember(box.id) { mutableStateOf(box.externalUrl) }
        var isVisible by remember(box.id) { mutableStateOf(box.isVisible) }

        AlertDialog(
            onDismissRequest = { editingBox = null },
            title = {
                Text(
                    text = "सुविधा बॉक्स संपादन (#${box.displayOrder})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("शीर्षक (Title)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = subtitle,
                        onValueChange = { subtitle = it },
                        label = { Text("संक्षिप्त विवरण (Subtitle)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("वेब लिंक URL (वैकल्पिक)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("होम/टूल्स में दिखाएँ (Visible)", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        Switch(
                            checked = isVisible,
                            onCheckedChange = { isVisible = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateFeatureBox(box, title, subtitle, url, isVisible)
                        Toast.makeText(context, "सुविधा बॉक्स अपडेट हुआ", Toast.LENGTH_SHORT).show()
                        editingBox = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                ) {
                    Text("सहेजें व क्लाउड सिंक")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingBox = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}

// -------------------------------------------------------------------
// TAB 12: TOP THREE OFFICIAL LINKS
// -------------------------------------------------------------------
@Composable
fun AdminTopThreeLinksTab(
    viewModel: AdminViewModel,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val currentLinks by viewModel.topThreeLinks.collectAsState()

    var eshikshakosh by remember(currentLinks) {
        mutableStateOf(currentLinks?.link1Url ?: "https://eshikshakosh.bihar.gov.in")
    }
    var hrms by remember(currentLinks) {
        mutableStateOf(currentLinks?.link2Url ?: "https://hrms.bihar.gov.in")
    }
    var udise by remember(currentLinks) {
        mutableStateOf(currentLinks?.link3Url ?: "https://udiseplus.gov.in")
    }
    var isVisible by remember(currentLinks) {
        mutableStateOf(currentLinks?.link1Visible ?: true)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ElevatedCard(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Navy900),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "शीर्ष 3 महत्वपूर्ण आधिकारिक लिंक्स",
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "होम स्क्रीन पर एक क्षैतिज पंक्ति में 3 आधिकारिक बटन्स (eShikshakosh, HRMS Bihar, UDISE+) के URL व दृश्यता को यहाँ से अपडेट करें।",
                        color = Gold100,
                        fontSize = 11.sp
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = eshikshakosh,
                        onValueChange = { eshikshakosh = it },
                        label = { Text("1. eShikshakosh लिंक URL") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = hrms,
                        onValueChange = { hrms = it },
                        label = { Text("2. HRMS Bihar लिंक URL") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = udise,
                        onValueChange = { udise = it },
                        label = { Text("3. UDISE+ लिंक URL") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "होम स्क्रीन पर प्रदर्शित करें (Show on Home)",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                        Switch(
                            checked = isVisible,
                            onCheckedChange = { isVisible = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            viewModel.saveTopThreeLinks(eshikshakosh, hrms, udise, isVisible)
                            Toast.makeText(context, "शीर्ष 3 लिंक्स सफलतापूर्वक अपडेट किए गए!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("लिंक्स सहेजें व क्लाउड सिंक करें", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------
// TAB 13: PROMOTIONAL & SERVICE BANNERS MANAGEMENT (Requirement 7)
// Unlimited banners + master switch + individual ON/OFF + Add/Edit/Delete
// -------------------------------------------------------------------
@Composable
fun AdminBannersConfigTab(
    viewModel: AdminViewModel,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val banners by viewModel.promoBanners.collectAsState()
    val currentFin by viewModel.financialBanner.collectAsState()
    val currentWa by viewModel.whatsAppBanner.collectAsState()

    var showAddBannerDialog by remember { mutableStateOf(false) }
    var editingBanner by remember { mutableStateOf<com.example.database.entities.PromoBannerEntity?>(null) }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Master Home Screen Banner ON/OFF Switch
        item {
            ElevatedCard(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Navy900),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "होम स्क्रीन बैनर मुख्य स्विच (Master Banner Switch)",
                                color = PureWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val isMasterOn = currentFin?.isVisible ?: true
                            Text(
                                text = if (isMasterOn) "स्थिति: सक्रिय (सभी बैनर होम स्क्रीन पर प्रदर्शित होंगे)" else "स्थिति: बंद (होम स्क्रीन से सभी बैनर छिपे हुए हैं)",
                                color = if (isMasterOn) Gold500 else Color(0xFFFF8A80),
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = currentFin?.isVisible ?: true,
                            onCheckedChange = { isEnabled ->
                                currentFin?.let { fin ->
                                    viewModel.saveFinancialBanner(fin.heading, fin.supportingText, fin.buttonText, fin.destinationUrl, isEnabled, imageUrl = fin.imageUrl)
                                }
                                currentWa?.let { wa ->
                                    viewModel.saveWhatsAppBanner(wa.bannerText, wa.buttonText, wa.channelUrl, isEnabled, imageUrl = wa.imageUrl)
                                }
                                Toast.makeText(context, if (isEnabled) "होम स्क्रीन बैनर चालू किए गए" else "होम स्क्रीन बैनर बंद किए गए", Toast.LENGTH_SHORT).show()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = PureWhite, checkedTrackColor = Gold500)
                        )
                    }
                }
            }
        }

        // Editable Card 1: Financial Banner
        item {
            var finHeading by remember(currentFin) { mutableStateOf(currentFin?.heading ?: "") }
            var finSupporting by remember(currentFin) { mutableStateOf(currentFin?.supportingText ?: "") }
            var finBtnText by remember(currentFin) { mutableStateOf(currentFin?.buttonText ?: "") }
            var finUrl by remember(currentFin) { mutableStateOf(currentFin?.destinationUrl ?: "") }
            var finImageUrl by remember(currentFin) { mutableStateOf(currentFin?.imageUrl ?: "") }
            var finVisible by remember(currentFin) { mutableStateOf(currentFin?.isVisible ?: true) }

            ElevatedCard(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = PureWhite),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "फाइनेंशियल सहायता / लोन बैनर",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Navy900
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (finVisible) "चालू" else "बंद", fontSize = 11.sp, color = if (finVisible) Color(0xFF2E7D32) else Color.Red)
                            Spacer(modifier = Modifier.width(4.dp))
                            Switch(checked = finVisible, onCheckedChange = { finVisible = it })
                        }
                    }
                    OutlinedTextField(
                        value = finHeading,
                        onValueChange = { finHeading = it },
                        label = { Text("बैनर मुख्य शीर्षक") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = finSupporting,
                        onValueChange = { finSupporting = it },
                        label = { Text("उप-शीर्षक / विवरण") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = finImageUrl,
                        onValueChange = { finImageUrl = it },
                        label = { Text("बैनर इमेज URL (वैकल्पिक)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    R2UploadPickerRow(
                        adminViewModel = viewModel,
                        folder = R2Config.FOLDER_BANNERS,
                        titleHint = finHeading.ifBlank { "financial_banner" },
                        existingUrl = finImageUrl,
                        mode = R2UploadMode.IMAGE_ONLY,
                        testTagPrefix = "fin_banner_img",
                        onUploaded = { uploaded -> finImageUrl = uploaded.url }
                    )
                    if (finImageUrl.isNotBlank()) {
                        AppNetworkImage(
                            imageUrl = finImageUrl.trim(),
                            contentDescription = "Financial Banner Preview",
                            versionTag = currentFin?.lastUpdated ?: 0L,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = finBtnText,
                            onValueChange = { finBtnText = it },
                            label = { Text("बटन टेक्स्ट") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = finUrl,
                            onValueChange = { finUrl = it },
                            label = { Text("लिंक URL") },
                            modifier = Modifier.weight(1.5f)
                        )
                    }
                    Button(
                        onClick = {
                            viewModel.saveFinancialBanner(finHeading.trim(), finSupporting.trim(), finBtnText.trim(), finUrl.trim(), finVisible, imageUrl = finImageUrl.trim())
                            Toast.makeText(context, "फाइनेंशियल बैनर अपडेट हुआ!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("फाइनेंशियल बैनर सेव करें व लाइव सिंक")
                    }
                }
            }
        }

        // Editable Card 2: WhatsApp Channel Banner
        item {
            var waBannerText by remember(currentWa) { mutableStateOf(currentWa?.bannerText ?: "") }
            var waBtnText by remember(currentWa) { mutableStateOf(currentWa?.buttonText ?: "") }
            var waChannelUrl by remember(currentWa) { mutableStateOf(currentWa?.channelUrl ?: "") }
            var waImageUrl by remember(currentWa) { mutableStateOf(currentWa?.imageUrl ?: "") }
            var waVisible by remember(currentWa) { mutableStateOf(currentWa?.isVisible ?: true) }

            ElevatedCard(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = PureWhite),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "व्हाट्सएप चैनल जॉइन बैनर",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Navy900
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (waVisible) "चालू" else "बंद", fontSize = 11.sp, color = if (waVisible) Color(0xFF2E7D32) else Color.Red)
                            Spacer(modifier = Modifier.width(4.dp))
                            Switch(checked = waVisible, onCheckedChange = { waVisible = it })
                        }
                    }
                    OutlinedTextField(
                        value = waBannerText,
                        onValueChange = { waBannerText = it },
                        label = { Text("बैनर संदेश (Message)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = waImageUrl,
                        onValueChange = { waImageUrl = it },
                        label = { Text("व्हाट्सएप बैनर इमेज URL (वैकल्पिक)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    R2UploadPickerRow(
                        adminViewModel = viewModel,
                        folder = R2Config.FOLDER_BANNERS,
                        titleHint = "whatsapp_banner",
                        existingUrl = waImageUrl,
                        mode = R2UploadMode.IMAGE_ONLY,
                        testTagPrefix = "wa_banner_img",
                        onUploaded = { uploaded -> waImageUrl = uploaded.url }
                    )
                    if (waImageUrl.isNotBlank()) {
                        AppNetworkImage(
                            imageUrl = waImageUrl.trim(),
                            contentDescription = "WhatsApp Banner Preview",
                            versionTag = currentWa?.lastUpdated ?: 0L,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = waBtnText,
                            onValueChange = { waBtnText = it },
                            label = { Text("बटन टेक्स्ट") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = waChannelUrl,
                            onValueChange = { waChannelUrl = it },
                            label = { Text("चैनल लिंक URL") },
                            modifier = Modifier.weight(1.5f)
                        )
                    }
                    Button(
                        onClick = {
                            viewModel.saveWhatsAppBanner(waBannerText.trim(), waBtnText.trim(), waChannelUrl.trim(), waVisible, imageUrl = waImageUrl.trim())
                            Toast.makeText(context, "व्हाट्सएप बैनर अपडेट हुआ!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Campaign, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("व्हाट्सएप बैनर सेव करें व लाइव सिंक", color = PureWhite)
                    }
                }
            }
        }

        // Section: Unlimited Custom Promotional Banners
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "प्रमोशनल बैनर सूची (${banners.size} बैनर)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Navy900
                    )
                    Text(
                        text = "असीमित बैनर जोड़ें, व्यक्तिगत ON/OFF व लाइव सिंक",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
                Button(
                    onClick = {
                        editingBanner = null
                        showAddBannerDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold500),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Navy900, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("नया बैनर (+)", color = Navy900, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        items(banners, key = { it.id }) { banner ->
            ElevatedCard(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (banner.isActive) PureWhite else Color(0xFFF5F5F5)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Navy900,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${banner.orderIndex}. ${banner.category}",
                                color = Gold500,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (banner.isActive) "सक्रिय (ON)" else "बंद (OFF)",
                                color = if (banner.isActive) Color(0xFF2E7D32) else Color(0xFFC62828),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = banner.isActive,
                                onCheckedChange = { active ->
                                    val updated = banner.copy(isActive = active)
                                    viewModel.savePromoBanner(updated)
                                }
                            )
                        }
                    }

                    if (banner.imageUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        AppNetworkImage(
                            imageUrl = banner.imageUrl,
                            contentDescription = banner.title,
                            versionTag = banner.timestamp,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = banner.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Navy900)
                    if (banner.description.isNotBlank()) {
                        Text(text = banner.description, fontSize = 11.5.sp, color = Color.DarkGray)
                    }
                    if (banner.destinationUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "गंतव्य लिंक: ${banner.destinationUrl}", fontSize = 10.5.sp, color = Teal700, maxLines = 1)
                    }
                    if (banner.imageUrl.isNotBlank()) {
                        Text(text = "इमेज URL: ${banner.imageUrl}", fontSize = 10.sp, color = Color.Gray, maxLines = 1)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = {
                            editingBanner = banner
                            showAddBannerDialog = true
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Navy800)
                        }
                        IconButton(onClick = {
                            viewModel.deletePromoBanner(banner)
                            Toast.makeText(context, "बैनर हटाया गया", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFC62828))
                        }
                    }
                }
            }
        }
    }

    if (showAddBannerDialog) {
        var title by remember(showAddBannerDialog, editingBanner?.id) { mutableStateOf(editingBanner?.title ?: "") }
        var desc by remember(showAddBannerDialog, editingBanner?.id) { mutableStateOf(editingBanner?.description ?: "") }
        var category by remember(showAddBannerDialog, editingBanner?.id) { mutableStateOf(editingBanner?.category ?: "प्रमोशनल") }
        var imgUrl by remember(showAddBannerDialog, editingBanner?.id) { mutableStateOf(editingBanner?.imageUrl ?: "") }
        var destUrl by remember(showAddBannerDialog, editingBanner?.id) { mutableStateOf(editingBanner?.destinationUrl ?: "https://") }
        var btnText by remember(showAddBannerDialog, editingBanner?.id) { mutableStateOf(editingBanner?.buttonText ?: "विवरण देखें") }
        var order by remember(showAddBannerDialog, editingBanner?.id) { mutableStateOf((editingBanner?.orderIndex ?: (banners.size + 1)).toString()) }
        var isActive by remember(showAddBannerDialog, editingBanner?.id) { mutableStateOf(editingBanner?.isActive ?: true) }
        var sendNotification by remember(showAddBannerDialog, editingBanner?.id) { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddBannerDialog = false },
            title = {
                Text(
                    text = if (editingBanner == null) "नया प्रमोशनल बैनर जोड़ें" else "बैनर सम्पादित करें",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("बैनर मुख्य शीर्षक") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("संक्षिप्त विवरण") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("श्रेणी (e.g. पर्सनल लोन, क्रेडिट कार्ड)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = destUrl, onValueChange = { destUrl = it }, label = { Text("गंतव्य लिंक URL (Click Destination)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = imgUrl, onValueChange = { imgUrl = it }, label = { Text("इमेज URL / फोटो लिंक") }, modifier = Modifier.fillMaxWidth())
                    R2UploadPickerRow(
                        adminViewModel = viewModel,
                        folder = R2Config.FOLDER_BANNERS,
                        titleHint = title.ifBlank { "promo_banner" },
                        existingUrl = imgUrl,
                        mode = R2UploadMode.IMAGE_ONLY,
                        testTagPrefix = "banner_dialog_img",
                        onUploaded = { uploaded -> imgUrl = uploaded.url }
                    )
                    if (imgUrl.isNotBlank()) {
                        AppNetworkImage(
                            imageUrl = imgUrl.trim(),
                            contentDescription = "Promo Banner Preview",
                            versionTag = editingBanner?.timestamp ?: 0L,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(value = btnText, onValueChange = { btnText = it }, label = { Text("बटन टेक्स्ट") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = order, onValueChange = { order = it }, label = { Text("क्रम संख्या") }, modifier = Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("सक्रिय रखें (ON/OFF)", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        Switch(checked = isActive, onCheckedChange = { isActive = it })
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    AdminNotificationToggleRow(
                        sendNotification = sendNotification,
                        onCheckedChange = { sendNotification = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val bId = editingBanner?.bannerId?.ifBlank { null } ?: "banner_${System.currentTimeMillis()}"
                            val entity = com.example.database.entities.PromoBannerEntity(
                                id = editingBanner?.id ?: 0L,
                                bannerId = bId,
                                title = title.trim(),
                                description = desc.trim(),
                                category = category.trim(),
                                imageUrl = imgUrl.trim(),
                                destinationUrl = destUrl.trim(),
                                buttonText = btnText.trim(),
                                orderIndex = order.toIntOrNull() ?: 1,
                                isActive = isActive,
                                timestamp = System.currentTimeMillis()
                            )
                            viewModel.savePromoBanner(entity, sendNotification = sendNotification)
                            Toast.makeText(context, "बैनर सफलतापूर्वक सेव हुआ!", Toast.LENGTH_SHORT).show()
                            showAddBannerDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                ) {
                    Text(if (editingBanner == null) "जोड़ें व सहेजें" else "अपडेट करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBannerDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}

