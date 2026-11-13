package com.example.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.AdminViewModel
import com.example.cloudflare.AdminGoogleAuthHelper
import com.example.core.LocalAppLanguage
import com.example.ui.theme.Gold100
import com.example.ui.theme.Gold500
import com.example.ui.theme.Gold700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Teal700
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    adminViewModel: AdminViewModel,
    onNavigateToAdmin: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val languageManager = LocalAppLanguage.current
    val isHi = languageManager.isHindi()

    var showDisclaimerDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showContactDialog by remember { mutableStateOf(false) }
    var showGoogleLoginDialog by remember { mutableStateOf(false) }

    var notificationsEnabled by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6F9))
            .testTag("settings_screen")
    ) {
        // Settings Header
        Surface(
            color = Navy900,
            tonalElevation = 4.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "पीछे जाएँ",
                        tint = PureWhite
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isHi) "सेटिंग्स एवं ऐप विवरण" else "Settings & About App",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                    Text(
                        text = "शिक्षक साथी बिहार • वर्शन 2.1",
                        fontSize = 11.sp,
                        color = Color(0xFFA5B8CC)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Prominent Non-Government Affiliation Disclaimer Card (Mandatory Requirement)
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Gold700)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Gold500),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Navy900,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "महत्वपूर्ण स्पष्टीकरण (Important Disclaimer)",
                                fontWeight = FontWeight.Bold,
                                color = Navy900,
                                fontSize = 13.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Shikshak Sathi Bihar is an independent information-sharing platform created for teachers' convenience. It is NOT an official application of the Bihar Education Department or the Government of Bihar. Its purpose is to provide useful information and connect teachers on one platform.",
                            fontSize = 12.sp,
                            color = Color(0xFF37474F),
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "शिक्षक साथी बिहार शिक्षकों की सुविधा के लिए बनाया गया एक स्वतंत्र सूचना साझाकरण मंच है। यह बिहार शिक्षा विभाग या बिहार सरकार का आधिकारिक एप्लिकेशन नहीं है। इसका उद्देश्य उपयोगी जानकारी प्रदान करना और शिक्षकों को एक मंच पर जोड़ना है।",
                            fontSize = 11.5.sp,
                            color = Color(0xFF455A64),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Section: App Information & Legal
            item {
                Text(
                    text = if (isHi) "ऐप विवरण एवं कानूनी जानकारी" else "App Information & Legal",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy900,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            item {
                ElevatedCard(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = PureWhite),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SettingsRowItem(
                            icon = Icons.Default.Info,
                            title = if (isHi) "ऐप के बारे में (About App)" else "About App",
                            subtitle = "उद्देश्य, विशेषताएं एवं संस्करण विवरण",
                            onClick = { showDisclaimerDialog = true }
                        )

                        SettingsDivider()

                        SettingsRowItem(
                            icon = Icons.Default.PrivacyTip,
                            title = if (isHi) "गोपनीयता नीति (Privacy Policy)" else "Privacy Policy",
                            subtitle = "डेटा सुरक्षा, संग्रहण एवं गोपनीयता नियम",
                            onClick = { showPrivacyDialog = true }
                        )

                        SettingsDivider()

                        SettingsRowItem(
                            icon = Icons.Default.Email,
                            title = if (isHi) "संपर्क एवं सहायता (Contact Us)" else "Contact Us",
                            subtitle = "सुझाव, प्रतिक्रिया एवं तकनीकी सहायता",
                            onClick = { showContactDialog = true }
                        )
                    }
                }
            }

            // Section: Preferences & Settings
            item {
                Text(
                    text = if (isHi) "सामान्य प्राथमिकताएं (Preferences)" else "General Preferences",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy900,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            item {
                ElevatedCard(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = PureWhite),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFFE8F0FE), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = Color(0xFF1967D2),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (isHi) "विभागीय सूचनाएं व अलर्ट" else "Notifications & Alerts",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.5.sp,
                                        color = Navy900
                                    )
                                    Text(
                                        text = if (isHi) "नए आदेश, अवकाश एवं समाचार अलर्ट" else "Instant notices & holiday alerts",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { notificationsEnabled = it }
                            )
                        }

                        SettingsDivider()

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { languageManager.toggleLanguage() }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Gold100, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = Navy900,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (isHi) "भाषा (Language)" else "App Language",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.5.sp,
                                        color = Navy900
                                    )
                                    Text(
                                        text = if (isHi) "वर्तमान भाषा: हिंदी" else "Current: English",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Text(
                                text = if (isHi) "बदलें (Switch)" else "Switch to Hindi",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Teal700
                            )
                        }
                    }
                }
            }

            // Section: Administrative Access (Google Sign-In ONLY, no passwords)
            item {
                Text(
                    text = if (isHi) "प्रशासनिक पहुंच (Admin Portal)" else "Administrative Portal",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy900,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            item {
                ElevatedCard(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = PureWhite),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Navy900, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = Gold500,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isHi) "व्यवस्थापक लॉगिन (Admin Login)" else "Administrator Access",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Navy900
                                )
                                Text(
                                    text = "केवल अधिकृत Google खाते (Google Sign-In) द्वारा सुरक्षित प्रवेश",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val session = adminViewModel.session.value
                                if (session.isAuthenticated) {
                                    onNavigateToAdmin()
                                } else {
                                    showGoogleLoginDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Gold500,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (adminViewModel.session.value.isAuthenticated) "व्यवस्थापक पैनल खोलें (Admin Panel)" else "व्यवस्थापक लॉगिन करें (Admin Login)",
                                fontWeight = FontWeight.Bold,
                                color = PureWhite,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Admin Login Dialog (Google Sign-In ONLY: satyamprakash100@gmail.com)
    if (showGoogleLoginDialog) {
        var isAuthenticating by remember { mutableStateOf(false) }
        var errorMsg by remember { mutableStateOf("") }
        val isOnline = adminViewModel.isOnline()

        AlertDialog(
            onDismissRequest = { showGoogleLoginDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Navy800,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "व्यवस्थापक लॉगिन (Google Sign-In)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Navy900
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "व्यवस्थापक पोर्टल में केवल अधिकृत Google खाते (${AdminViewModel.SUPER_ADMIN_EMAIL}) द्वारा प्रवेश करें।",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )

                    if (!isOnline) {
                        Surface(
                            color = Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "ऑफलाइन: Google Sign-In हेतु सक्रिय इंटरनेट कनेक्शन आवश्यक है।",
                                color = Color(0xFFC62828),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            isAuthenticating = true
                            errorMsg = ""
                            coroutineScope.launch {
                                val res = AdminGoogleAuthHelper.signInWithGoogle(context, adminViewModel)
                                isAuthenticating = false
                                res.onSuccess {
                                    showGoogleLoginDialog = false
                                    Toast.makeText(context, "व्यवस्थापक स्वागत है", Toast.LENGTH_SHORT).show()
                                    onNavigateToAdmin()
                                }.onFailure { err ->
                                    errorMsg = err.message ?: "Google लॉगिन विफल रहा"
                                }
                            }
                        },
                        enabled = isOnline && !isAuthenticating,
                        colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("settings_admin_google_login_btn")
                    ) {
                        if (isAuthenticating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Gold500
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Gold500
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (isAuthenticating) "सत्यापन हो रहा है..." else "Google से साइन इन करें",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = PureWhite
                        )
                    }

                    if (errorMsg.isNotBlank()) {
                        Text(
                            text = errorMsg,
                            color = Color(0xFFC62828),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showGoogleLoginDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Disclaimer Dialog
    if (showDisclaimerDialog) {
        AlertDialog(
            onDismissRequest = { showDisclaimerDialog = false },
            title = { Text("About Shikshak Sathi Bihar", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Shikshak Sathi Bihar is an independent information-sharing platform created for teachers' convenience. It is NOT an official application of the Bihar Education Department or the Government of Bihar. Its purpose is to provide useful information and connect teachers on one platform.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Text(
                        text = "शिक्षक साथी बिहार शिक्षकों की सुविधा के लिए बनाया गया एक स्वतंत्र सूचना साझाकरण मंच है। यह बिहार शिक्षा विभाग या बिहार सरकार का आधिकारिक एप्लिकेशन नहीं है। इसका उद्देश्य उपयोगी जानकारी प्रदान करना और शिक्षकों को एक मंच पर जोड़ना है।",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showDisclaimerDialog = false }) {
                    Text("ठीक है")
                }
            }
        )
    }

    // Privacy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "शिक्षक साथी बिहार ऐप उपयोगकर्ताओं के व्यक्तिगत डेटा को पूरी तरह सुरक्षित रखता है। शिक्षक प्रोफाइल का विवरण केवल स्थानीय स्तर पर अथवा अधिकृत क्लाउड सिंक में सुरक्षित रूप से संरक्षित किया जाता है। किसी भी तीसरे पक्ष के साथ कोई गोपनीय जानकारी साझा नहीं की जाती।",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(onClick = { showPrivacyDialog = false }) {
                    Text("समझ गया")
                }
            }
        )
    }

    // Contact Dialog
    if (showContactDialog) {
        AlertDialog(
            onDismissRequest = { showContactDialog = false },
            title = { Text("Contact Us / Feedback", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("शिक्षक साथी बिहार सपोर्ट टीम:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("ईमेल: satyamprakash100@gmail.com", fontSize = 12.5.sp)
                    Text("व्हाट्सएप चैनल: शिक्षक साथी आधिकारिक चैनल", fontSize = 12.5.sp)
                    Text("किसी भी तकनीकी समस्या या सुझाव हेतु हमें ईमेल या व्हाट्सएप द्वारा संपर्क करें।", fontSize = 12.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(onClick = { showContactDialog = false }) {
                    Text("ठीक है")
                }
            }
        )
    }
}

@Composable
private fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFFF1F5F9), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Navy800,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
                color = Navy900
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
        Text(
            text = "›",
            fontSize = 18.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(Color(0xFFE2E8F0))
    )
}
