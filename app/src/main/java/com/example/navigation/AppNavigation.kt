package com.example.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.activities.ActivitiesScreen
import com.example.activities.ActivitiesViewModel
import com.example.admin.AdminScreen
import com.example.admin.AdminViewModel
import com.example.core.LocalAppLanguage
import com.example.database.entities.FeatureBoxEntity
import com.example.documents.DocumentsScreen
import com.example.documents.DocumentsViewModel
import com.example.downloads.DownloadsLibraryScreen
import com.example.home.HomeScreen
import com.example.home.HomeViewModel
import com.example.links.ImportantLinksScreen
import com.example.news.NewsAndHolidaysScreen
import com.example.news.NewsFeedScreen
import com.example.orders.OrdersAndCircularsScreen
import com.example.productivity.ProductivityViewModel
import com.example.productivity.ToolsScreen
import com.example.profile.ProfileScreen
import com.example.profile.ProfileViewModel
import com.example.profile.SettingsScreen
import com.example.salary.SalaryScreen
import com.example.salary.SalaryViewModel
import com.example.services.ComplaintsScreen
import com.example.services.CreditCardScreen
import com.example.services.InvestmentScreen
import com.example.services.LoanScreen
import com.example.ui.components.AppHeader
import com.example.ui.theme.Gold100
import com.example.ui.theme.Gold500
import com.example.ui.theme.Gold700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Teal700
import com.example.videos.VideoLibraryScreen
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val titleHi: String, val titleEn: String, val icon: ImageVector) {
    // 5 Primary Tabs
    object Home : Screen("home", "होम", "Home", Icons.Default.Home)
    object SalaryTab : Screen("salary_tab", "वेतन", "Betan", Icons.Default.AccountBalance)
    object NewsTab : Screen("news_tab", "अपडेट्स", "Updates", Icons.Default.Newspaper)
    object VideoTab : Screen("video_tab", "वीडियो", "Video", Icons.Default.VideoLibrary)
    object ToolsTab : Screen("tools_tab", "टूल्स", "Tools", Icons.Default.Build)

    // Dedicated Sub-Screens
    object Settings : Screen("settings", "सेटिंग्स व जानकारी", "Settings", Icons.Default.Settings)
    object Profile : Screen("profile", "शिक्षक प्रोफाइल", "Teacher Profile", Icons.Default.Person)
    object Admin : Screen("admin", "प्रशासन", "Admin", Icons.Default.Person)
    object VideoLibrary : Screen("video_library", "वीडियो गैलरी", "Videos", Icons.Default.VideoLibrary)
    object LatestOrders : Screen("latest_orders", "पत्र एवं आदेश", "Orders & Circulars", Icons.Default.Description)
    object OldOrders : Screen("old_orders", "पुराने आदेश", "Old Orders", Icons.Default.Description)
    object HolidayCalendar : Screen("holiday_calendar", "अवकाश तालिका", "Holidays", Icons.Default.Campaign)
    object Downloads : Screen("downloads", "डाउनलोड", "Downloads", Icons.Default.Description)
    object ImportantLinks : Screen("important_links", "सक्रिय डिजिटल सेवा", "Digital Services (Portals)", Icons.Default.Language)
    object Activities : Screen("activities", "वर्तमान गतिविधियां", "Ongoing Activities", Icons.Default.Campaign)
    object Documents : Screen("documents", "आवेदन प्रारूप", "Official Forms", Icons.Default.Description)
    object Complaints : Screen("complaints", "शिकायत निवारण", "Grievance Portal", Icons.Default.SupportAgent)
    object Loan : Screen("loan", "शिक्षक लोन सुविधा", "Teacher Loan", Icons.Default.MonetizationOn)
    object CreditCard : Screen("credit_card", "क्रेडिट कार्ड सेवा", "Credit Card", Icons.Default.CreditCard)
    object Investment : Screen("investment", "निवेश व बीमा", "Investment & Insurance", Icons.Default.TrendingUp)
}

@Composable
fun MainAppContainer() {
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    val isHi = language.isHindi()
    val coroutineScope = rememberCoroutineScope()

    // Sub-screen navigation stack (null means we are in the 5-tab Pager flow)
    var currentSubScreen by remember { mutableStateOf<Screen?>(null) }
    val subScreenBackStack = remember { mutableStateListOf<Screen>() }

    // 5 Primary Tabs: 0: HOME, 1: BETAN, 2: UPDATES/NEWS, 3: VIDEO, 4: TOOLS
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 5 })
    val homeListState = androidx.compose.foundation.lazy.rememberLazyListState()

    fun navigateToSubScreen(screen: Screen) {
        subScreenBackStack.add(screen)
        currentSubScreen = screen
    }

    fun popSubScreen() {
        if (subScreenBackStack.isNotEmpty()) {
            subScreenBackStack.removeAt(subScreenBackStack.size - 1)
            currentSubScreen = subScreenBackStack.lastOrNull()
        } else {
            currentSubScreen = null
        }
    }

    fun navigateToPrimaryTab(pageIndex: Int) {
        subScreenBackStack.clear()
        currentSubScreen = null
        coroutineScope.launch {
            pagerState.animateScrollToPage(pageIndex)
        }
    }

    fun openUrlSafely(url: String) {
        if (url.isBlank()) return
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    // Android Back Handler
    BackHandler(enabled = currentSubScreen != null || pagerState.currentPage != 0) {
        if (currentSubScreen != null) {
            popSubScreen()
        } else if (pagerState.currentPage != 0) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(0)
            }
        }
    }

    // Shared ViewModels
    val homeViewModel: HomeViewModel = viewModel()
    val salaryViewModel: SalaryViewModel = viewModel()
    val activitiesViewModel: ActivitiesViewModel = viewModel()
    val documentsViewModel: DocumentsViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    val adminViewModel: AdminViewModel = viewModel()
    val productivityViewModel: ProductivityViewModel = viewModel()

    val isInAdmin = currentSubScreen is Screen.Admin
    val isOnline by homeViewModel.isCloudOnline.collectAsState()
    val isRefreshing by homeViewModel.isManualRefreshing.collectAsState()
    val whatsAppBanner by homeViewModel.whatsAppBanner.collectAsState()

    // Handle Direct Notification Click Navigation (Cold start, Foreground, and Background states)
    fun handleDeepLinkTarget(target: com.example.core.DeepLinkTarget?) {
        if (target == null) return
        when (target.type) {
            com.example.core.DeepLinkType.NEWS -> {
                subScreenBackStack.clear()
                navigateToPrimaryTab(2)
                homeViewModel.dispatchDeepLink(target)
            }
            com.example.core.DeepLinkType.VIDEO -> {
                subScreenBackStack.clear()
                navigateToPrimaryTab(3)
                homeViewModel.dispatchDeepLink(target)
            }
            com.example.core.DeepLinkType.DOWNLOAD -> {
                subScreenBackStack.clear()
                subScreenBackStack.add(Screen.Downloads)
                homeViewModel.dispatchDeepLink(target)
            }
            com.example.core.DeepLinkType.NOTICE -> {
                subScreenBackStack.clear()
                subScreenBackStack.add(Screen.LatestOrders)
                homeViewModel.dispatchDeepLink(target)
            }
            com.example.core.DeepLinkType.HOLIDAY -> {
                subScreenBackStack.clear()
                subScreenBackStack.add(Screen.HolidayCalendar)
                homeViewModel.dispatchDeepLink(target)
            }
            com.example.core.DeepLinkType.HOME -> {
                subScreenBackStack.clear()
                navigateToPrimaryTab(0)
            }
        }
    }

    androidx.compose.runtime.DisposableEffect(context) {
        val activity = context as? androidx.activity.ComponentActivity
        val initialDeepLink = com.example.core.DeepLinkHelper.parseIntent(activity?.intent)
        if (initialDeepLink != null) {
            handleDeepLinkTarget(initialDeepLink)
            com.example.core.DeepLinkHelper.clearExtras(activity?.intent)
        }

        val listener = androidx.core.util.Consumer<android.content.Intent> { intent ->
            val newDeepLink = com.example.core.DeepLinkHelper.parseIntent(intent)
            if (newDeepLink != null) {
                handleDeepLinkTarget(newDeepLink)
                com.example.core.DeepLinkHelper.clearExtras(intent)
            }
        }
        activity?.addOnNewIntentListener(listener)
        onDispose {
            activity?.removeOnNewIntentListener(listener)
        }
    }

    // Shared Feature Click Router (Used identically on Home & Tools tabs)
    fun handleFeatureBoxClick(box: FeatureBoxEntity) {
        when (box.id.lowercase()) {
            "holiday", "holidays", "leave_holidays", "avashyak_avkaash" -> navigateToSubScreen(Screen.HolidayCalendar)
            "links", "important_links", "portals" -> navigateToSubScreen(Screen.ImportantLinks)
            "orders", "letters_orders", "patra_aadesh" -> navigateToSubScreen(Screen.LatestOrders)
            "news", "news_updates", "samachar" -> navigateToPrimaryTab(2)
            "activities", "school_activities", "current_activities", "ongoing_activities" -> navigateToSubScreen(Screen.Activities)
            "video", "videos", "tutorial" -> navigateToPrimaryTab(3)
            "salary", "betan", "salary_calculator" -> navigateToPrimaryTab(1)
            "downloads", "download", "download_center" -> navigateToSubScreen(Screen.Downloads)
            "applications", "documents", "forms", "application_formats", "prarup" -> navigateToSubScreen(Screen.Documents)
            "complaints", "complaint", "grievance" -> navigateToSubScreen(Screen.Complaints)
            "loan", "loans", "teacher_loan" -> navigateToSubScreen(Screen.Loan)
            "credit_card", "card" -> navigateToSubScreen(Screen.CreditCard)
            "investment", "invest", "bima" -> navigateToSubScreen(Screen.Investment)
            "whatsapp", "whatsapp_channel" -> {
                val bannerUrl = whatsAppBanner?.channelUrl?.takeIf { it.isNotBlank() }
                val url = bannerUrl ?: if (box.externalUrl.isNotBlank()) box.externalUrl else "https://whatsapp.com/channel/0029Va4yZ1m2k345"
                openUrlSafely(url)
            }
            else -> {
                if (box.externalUrl.isNotBlank()) {
                    openUrlSafely(box.externalUrl)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            if (!isInAdmin) {
                val currentTitle = if (currentSubScreen != null) {
                    if (isHi) currentSubScreen!!.titleHi else currentSubScreen!!.titleEn
                } else {
                    when (pagerState.currentPage) {
                        0 -> if (isHi) "शिक्षक साथी" else "Shikshak Sathi"
                        1 -> if (isHi) "वेतन कैलकुलेटर" else "Salary Calculator"
                        2 -> if (isHi) "शिक्षा समाचार व अपडेट्स" else "News & Updates"
                        3 -> if (isHi) "वीडियो ट्यूटोरियल" else "Video Tutorials"
                        4 -> if (isHi) "मुख्य शिक्षक सुविधाएं" else "Teacher Tools"
                        else -> "शिक्षक साथी"
                    }
                }

                val currentSubtitle = if (currentSubScreen != null) {
                    if (isHi) "शिक्षकों का डिजिटल साथी" else "Teachers Digital Companion"
                } else {
                    when (pagerState.currentPage) {
                        0 -> if (isHi) "शिक्षकों का डिजिटल साथी" else "Teachers Digital Companion"
                        1 -> if (isHi) "वेतन, DA एवं HRA गणना" else "Salary & Allowances"
                        2 -> if (isHi) "दैनिक विभागीय बुलेटिन व आदेश" else "Daily Bulletins"
                        3 -> if (isHi) "पोर्टल उपयोग एवं प्रशिक्षण वीडियो" else "Helpful Videos"
                        4 -> if (isHi) "14 आवश्यक शिक्षक सेवाएं" else "14 Core Teacher Services"
                        else -> ""
                    }
                }

                AppHeader(
                    title = currentTitle,
                    subtitle = currentSubtitle,
                    isOnline = isOnline,
                    navigationIcon = if (currentSubScreen != null) {
                        {
                            IconButton(
                                onClick = { popSubScreen() },
                                modifier = Modifier.testTag("header_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "पीछे जाएँ",
                                    tint = PureWhite
                                )
                            }
                        }
                    } else null,
                    actions = {
                        // Refresh Button
                        IconButton(
                            onClick = { homeViewModel.manualRefresh() },
                            modifier = Modifier.testTag("header_refresh_action")
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = PureWhite,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "रीफ्रेश करें",
                                    tint = PureWhite
                                )
                            }
                        }

                        // Profile Icon Button (Top-Right)
                        IconButton(
                            onClick = { navigateToSubScreen(Screen.Profile) },
                            modifier = Modifier.testTag("header_profile_action")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "शिक्षक प्रोफाइल",
                                tint = PureWhite
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (!isInAdmin && currentSubScreen == null) {
                // EXACTLY 5 menus: 1. HOME, 2. BETAN, 3. UPDATES/NEWS, 4. VIDEO, 5. TOOLS
                Surface(
                    color = Navy900,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .height(64.dp)
                            .testTag("main_bottom_nav"),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. HOME
                        val isHome = pagerState.currentPage == 0
                        BottomNavItem(
                            icon = Icons.Default.Home,
                            label = if (isHi) "होम" else "Home",
                            isSelected = isHome,
                            onClick = { navigateToPrimaryTab(0) },
                            modifier = Modifier.weight(1f)
                        )

                        // 2. BETAN (Salary)
                        val isBetan = pagerState.currentPage == 1
                        BottomNavItem(
                            icon = Icons.Default.AccountBalance,
                            label = if (isHi) "वेतन" else "Betan",
                            isSelected = isBetan,
                            onClick = { navigateToPrimaryTab(1) },
                            modifier = Modifier.weight(1f)
                        )

                        // 3. UPDATES / NEWS (Central prominent button)
                        val isNews = pagerState.currentPage == 2
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .clickable { navigateToPrimaryTab(2) }
                                .testTag("central_news_nav_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .offset(y = (-4).dp)
                                        .shadow(elevation = 6.dp, shape = CircleShape)
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = if (isNews) {
                                                    listOf(Gold500, Gold700)
                                                } else {
                                                    listOf(Color(0xFF1E2D42), Navy800)
                                                }
                                            ),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Newspaper,
                                        contentDescription = "UPDATES",
                                        tint = if (isNews) Navy900 else Gold500,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Text(
                                    text = if (isHi) "अपडेट्स" else "Updates",
                                    fontSize = 11.sp,
                                    fontWeight = if (isNews) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (isNews) Gold500 else Color(0xFFA5B8CC),
                                    modifier = Modifier.offset(y = (-2).dp)
                                )
                            }
                        }

                        // 4. VIDEO
                        val isVideo = pagerState.currentPage == 3
                        BottomNavItem(
                            icon = Icons.Default.VideoLibrary,
                            label = if (isHi) "वीडियो" else "Video",
                            isSelected = isVideo,
                            onClick = { navigateToPrimaryTab(3) },
                            modifier = Modifier.weight(1f)
                        )

                        // 5. TOOLS (Displays the same 14 feature boxes as Home's "मुख्य शिक्षक सुविधाएं")
                        val isTools = pagerState.currentPage == 4
                        BottomNavItem(
                            icon = Icons.Default.Build,
                            label = if (isHi) "टूल्स" else "Tools",
                            isSelected = isTools,
                            onClick = { navigateToPrimaryTab(4) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .then(if (currentSubScreen != null) Modifier.navigationBarsPadding() else Modifier)
        ) {
            if (currentSubScreen != null) {
                // Render Dedicated Sub-Screen
                when (currentSubScreen!!) {
                    is Screen.Settings -> SettingsScreen(
                        adminViewModel = adminViewModel,
                        onNavigateToAdmin = { navigateToSubScreen(Screen.Admin) },
                        onBack = { popSubScreen() }
                    )
                    is Screen.Profile -> ProfileScreen(
                        viewModel = profileViewModel,
                        onOpenAdminAccess = { navigateToSubScreen(Screen.Admin) }
                    )
                    is Screen.VideoLibrary -> VideoLibraryScreen(
                        repository = homeViewModel.repository,
                        onBack = { popSubScreen() },
                        homeViewModel = homeViewModel
                    )
                    is Screen.LatestOrders -> OrdersAndCircularsScreen(
                        homeViewModel = homeViewModel,
                        initialTab = 0,
                        onBack = { popSubScreen() }
                    )
                    is Screen.OldOrders -> OrdersAndCircularsScreen(
                        homeViewModel = homeViewModel,
                        initialTab = 1,
                        onBack = { popSubScreen() }
                    )
                    is Screen.HolidayCalendar -> {
                        val holidays by adminViewModel.holidays.collectAsState()
                        NewsAndHolidaysScreen(
                            homeViewModel = homeViewModel,
                            holidays = holidays,
                            initialTab = 1,
                            onBack = { popSubScreen() }
                        )
                    }
                    is Screen.Downloads -> DownloadsLibraryScreen(
                        homeViewModel = homeViewModel,
                        onBack = { popSubScreen() }
                    )
                    is Screen.ImportantLinks -> ImportantLinksScreen(
                        repository = homeViewModel.repository,
                        onBack = { popSubScreen() }
                    )
                    is Screen.Activities -> ActivitiesScreen(
                        viewModel = activitiesViewModel
                    )
                    is Screen.Documents -> DocumentsScreen(
                        viewModel = documentsViewModel
                    )
                    is Screen.Complaints -> ComplaintsScreen(
                        onBack = { popSubScreen() },
                        homeViewModel = homeViewModel
                    )
                    is Screen.Loan -> LoanScreen(
                        onBack = { popSubScreen() },
                        homeViewModel = homeViewModel
                    )
                    is Screen.CreditCard -> CreditCardScreen(
                        onBack = { popSubScreen() },
                        homeViewModel = homeViewModel
                    )
                    is Screen.Investment -> InvestmentScreen(
                        onBack = { popSubScreen() },
                        homeViewModel = homeViewModel
                    )
                    is Screen.Admin -> AdminScreen(
                        viewModel = adminViewModel,
                        onBackToTeacherInterface = { popSubScreen() }
                    )
                    else -> {}
                }
            } else {
                // 5 Primary Tabs: 0: HOME, 1: BETAN, 2: UPDATES, 3: VIDEO, 4: TOOLS
                // userScrollEnabled = false ensures the 5 bottom menus stay STABLE and do not switch on side swipe
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = false,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1
                ) { page ->
                    when (page) {
                        0 -> HomeScreen(
                            viewModel = homeViewModel,
                            onNavigateToFeature = { box -> handleFeatureBoxClick(box) },
                            onNavigateToNewsFeed = { navigateToPrimaryTab(2) },
                            onNavigateToVideoLibrary = { navigateToPrimaryTab(3) },
                            onNavigateToFinancialDetails = { navigateToSubScreen(Screen.Loan) },
                            onNavigateToHolidayCalendar = { navigateToSubScreen(Screen.HolidayCalendar) },
                            onNavigateToProfile = { navigateToSubScreen(Screen.Profile) },
                            listState = homeListState
                        )
                        1 -> SalaryScreen(
                            viewModel = salaryViewModel
                        )
                        2 -> NewsFeedScreen(
                            homeViewModel = homeViewModel,
                            onBack = null
                        )
                        3 -> VideoLibraryScreen(
                            repository = homeViewModel.repository,
                            onBack = { navigateToPrimaryTab(0) },
                            homeViewModel = homeViewModel
                        )
                        4 -> ToolsScreen(
                            homeViewModel = homeViewModel,
                            productivityViewModel = productivityViewModel,
                            onFeatureClick = { box -> handleFeatureBoxClick(box) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Gold500 else Color(0xFFA5B8CC),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Gold500 else Color(0xFFA5B8CC)
        )
    }
}
