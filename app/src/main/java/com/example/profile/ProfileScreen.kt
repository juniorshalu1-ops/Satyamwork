package com.example.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.AppLanguage
import com.example.core.LocalAppLanguage
import com.example.database.entities.TeacherProfileEntity
import com.example.ui.theme.Gold500
import com.example.ui.theme.Gold700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onOpenAdminAccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val languageManager = LocalAppLanguage.current
    val isHi = languageManager.isHindi()

    val teacher by viewModel.teacherProfile.collectAsState()
    val backupJson by viewModel.backupExportResult.collectAsState()
    val restoreMsg by viewModel.restoreStatusMessage.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    var notificationsEnabled by remember { mutableStateOf(true) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackupFromUri(context, uri)
            Toast.makeText(context, "Google Drive / फ़ाइल से बैकअप लोड किया जा रहा है...", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(restoreMsg) {
        if (restoreMsg.isNotBlank()) {
            Toast.makeText(context, restoreMsg, Toast.LENGTH_LONG).show()
        }
    }

    fun copyTextToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "$label कॉपी किया गया", Toast.LENGTH_SHORT).show()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("profile_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Teacher Profile Header Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Navy800),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("teacher_profile_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(Gold500.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Gold500,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = teacher?.name ?: "शिक्षक का नाम",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite
                                )
                            )
                            Text(
                                text = teacher?.designation ?: "सहायक शिक्षक",
                                style = MaterialTheme.typography.bodySmall.copy(color = Gold500)
                            )
                            Text(
                                text = "${teacher?.schoolName ?: "राजकीय विद्यालय"} • UDISE: ${teacher?.udiseCode ?: "10010100101"}",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFC7D3DF))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // ID Details Box (HRMS, e-Shikshakosh, PRAN)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Navy900)
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ProfileInfoRow(
                                label = "HRMS आईडी:",
                                value = teacher?.hrmsId?.ifBlank { "BR-EDU-849201" } ?: "BR-EDU-849201",
                                onCopy = { copyTextToClipboard("HRMS ID", it) }
                            )
                            ProfileInfoRow(
                                label = "ई-शिक्षाकोष आईडी:",
                                value = teacher?.eshikshakoshId?.ifBlank { "ESK-521948" } ?: "ESK-521948",
                                onCopy = { copyTextToClipboard("ई-शिक्षाकोष ID", it) }
                            )
                            ProfileInfoRow(
                                label = "प्रान नंबर (PRAN):",
                                value = teacher?.pranNumber?.ifBlank { "110058291432" } ?: "110058291432",
                                onCopy = { copyTextToClipboard("PRAN Number", it) }
                            )
                            ProfileInfoRow(
                                label = "यूडाइस कोड (UDISE):",
                                value = teacher?.udiseCode?.ifBlank { "10010100101" } ?: "10010100101",
                                onCopy = { copyTextToClipboard("UDISE Code", it) }
                            )
                            ProfileInfoRow(
                                label = "प्रखंड / पंचायत:",
                                value = "${teacher?.district ?: "पटना"} • ${teacher?.block ?: "सदर"} • ${teacher?.panchayat ?: "ग्राम पंचायत"}",
                                onCopy = null
                            )
                            ProfileInfoRow(
                                label = "वेतन विवरण:",
                                value = "बेसिक ₹${teacher?.basicPay?.toInt() ?: 35400} • DA ${teacher?.daPercent?.toInt() ?: 53}% • HRA ${teacher?.hraCategory ?: "7.5%"}",
                                onCopy = null
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Gold700)
                    ) {
                        Text(
                            text = if (isHi) "प्रोफाइल विवरण संपादित करें" else "Edit Profile Details",
                            color = Navy900,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Language & Localization Selection
        item {
            SettingsCategoryCard(title = if (isHi) "भाषा चयन (Language Preference)" else "Language Preference") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = Navy800)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(if (isHi) "अनुप्रयोग भाषा" else "App Language", fontWeight = FontWeight.SemiBold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = isHi,
                            onClick = { languageManager.setLanguage(AppLanguage.HINDI) },
                            label = { Text("हिंदी (Default)") }
                        )
                        FilterChip(
                            selected = !isHi,
                            onClick = { languageManager.setLanguage(AppLanguage.ENGLISH) },
                            label = { Text("English") }
                        )
                    }
                }
            }
        }

        // Local & Cloud Drive Encrypted Backup (Server-load free)
        item {
            SettingsCategoryCard(title = if (isHi) "एन्क्रिप्टेड सुरक्षित बैकअप (Google Drive / डिवाइस)" else "Encrypted Backup (Drive / Device)") {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isHi) "आपका व्यक्तिगत डेटा डिवाइस में AES एन्क्रिप्शन द्वारा सुरक्षित रहता है। इसे 1 क्लिक में अपनी निजी Google Drive अथवा स्थानीय स्टोरेज में सुरक्षित रख सकते हैं। इससे किसी केंद्रीय सर्वर पर कोई भार नहीं पड़ता।" else "Your personal data is encrypted on device. Export directly to your Google Drive or local storage safely without central server dependency.",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.exportEncryptedBackupToDeviceOrDrive(context)
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("btn_encrypted_backup"),
                            colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                        ) {
                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isHi) "Drive / डिवाइस बैकअप (.ssb)" else "Drive / Device Backup", fontSize = 11.5.sp)
                        }
                        Button(
                            onClick = { showRestoreDialog = true },
                            modifier = Modifier.weight(0.9f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F))
                        ) {
                            Text(if (isHi) "रीस्टोर करें" else "Restore", fontSize = 11.5.sp)
                        }
                    }
                    if (restoreMsg.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = restoreMsg, fontSize = 11.sp, color = Color(0xFF1B5E20), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // App Information & Policies
        item {
            SettingsCategoryCard(title = if (isHi) "ऐप जानकारी एवं नीतियां" else "App Info & Legal") {
                Column {
                    SettingsRowItem(
                        icon = Icons.Default.PrivacyTip,
                        title = if (isHi) "गोपनीयता नीति (Privacy Policy)" else "Privacy Policy",
                        onClick = { showPrivacyDialog = true }
                    )
                    SettingsRowItem(
                        icon = Icons.Default.Info,
                        title = if (isHi) "ऐप के बारे में (About App) • शिक्षक साथी" else "About Shikshak Sathi",
                        onClick = { showAboutDialog = true }
                    )
                }
            }
        }

        // Hidden Admin Access - ONLY here in Profile Settings, never on Home screen
        item {
            Card(
                onClick = onOpenAdminAccess,
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFECEFF1)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_access_button")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Navy900.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = Navy900, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isHi) "प्रशासनिक पहुंच (Admin Access)" else "Administrative Access",
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                        Text(
                            text = if (isHi) "केवल अधिकृत शिक्षा पदाधिकारियों एवं व्यवस्थापकों हेतु" else "Authorized administrators only",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }

    // Comprehensive Edit Profile Dialog (Includes all requested fields)
    if (showEditDialog) {
        val t = teacher ?: TeacherProfileEntity()
        var name by remember { mutableStateOf(t.name) }
        var desig by remember { mutableStateOf(t.designation) }
        var hrmsId by remember { mutableStateOf(t.hrmsId) }
        var eshikshakoshId by remember { mutableStateOf(t.eshikshakoshId) }
        var pranNumber by remember { mutableStateOf(t.pranNumber) }
        var schoolName by remember { mutableStateOf(t.schoolName) }
        var udise by remember { mutableStateOf(t.udiseCode) }
        var dist by remember { mutableStateOf(t.district) }
        var blk by remember { mutableStateOf(t.block) }
        var panchayat by remember { mutableStateOf(t.panchayat) }
        var basic by remember { mutableStateOf(t.basicPay.toInt().toString()) }
        var daPercent by remember { mutableStateOf(t.daPercent.toInt().toString()) }
        var hraCat by remember { mutableStateOf(t.hraCategory) }

        var daDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(if (isHi) "शिक्षक एवं विद्यालय प्रोफाइल संपादन" else "Edit Teacher Profile") },
            text = {
                LazyColumn(
                    modifier = Modifier.height(420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("शिक्षक का नाम (Name)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = desig,
                            onValueChange = { desig = it },
                            label = { Text("पदनाम (Designation)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = hrmsId,
                            onValueChange = { hrmsId = it },
                            label = { Text("HRMS आईडी (HRMS ID)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = eshikshakoshId,
                            onValueChange = { eshikshakoshId = it },
                            label = { Text("ई-शिक्षाकोष आईडी (e-Shikshakosh ID)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = pranNumber,
                            onValueChange = { pranNumber = it },
                            label = { Text("प्रान नंबर (PRAN Number)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = schoolName,
                            onValueChange = { schoolName = it },
                            label = { Text("विद्यालय का नाम (School Name)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = udise,
                            onValueChange = { udise = it },
                            label = { Text("यूडाइस कोड (UDISE Code)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = dist,
                                onValueChange = { dist = it },
                                label = { Text("जिला (District)") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = blk,
                                onValueChange = { blk = it },
                                label = { Text("प्रखंड (Block)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = panchayat,
                            onValueChange = { panchayat = it },
                            label = { Text("पंचायत (Panchayat)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = basic,
                            onValueChange = { basic = it },
                            label = { Text("मूल वेतन (Basic Pay ₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    // DA % with dropdown
                    item {
                        Text(
                            text = "महंगाई भत्ता (DA %) - ड्रॉपडाउन से चुनें:",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = daPercent,
                                onValueChange = { daPercent = it },
                                label = { Text("DA प्रतिशत (%)") },
                                trailingIcon = {
                                    IconButton(onClick = { daDropdownExpanded = true }) {
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "DA Dropdown")
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(
                                expanded = daDropdownExpanded,
                                onDismissRequest = { daDropdownExpanded = false }
                            ) {
                                listOf("50", "53", "55", "58", "60", "63", "66", "69").forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text("$p% DA") },
                                        onClick = {
                                            daPercent = p
                                            daDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    // HRA Category Chips
                    item {
                        Text(
                            text = "मकान किराया भत्ता श्रेणी (HRA %):",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("5%", "7.5%", "10%", "20%").forEach { cat ->
                                FilterChip(
                                    selected = hraCat == cat,
                                    onClick = { hraCat = cat },
                                    label = {
                                        Text(
                                            when (cat) {
                                                "20%" -> "20% (पटना)"
                                                "10%" -> "10% (नगर)"
                                                "7.5%" -> "7.5% (कस्बा)"
                                                else -> "5% (ग्रामीण)"
                                            },
                                            fontSize = 10.5.sp
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Navy800,
                                        selectedLabelColor = PureWhite
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = t.copy(
                            name = name.trim(),
                            designation = desig.trim(),
                            hrmsId = hrmsId.trim(),
                            eshikshakoshId = eshikshakoshId.trim(),
                            pranNumber = pranNumber.trim(),
                            schoolName = schoolName.trim(),
                            udiseCode = udise.trim(),
                            district = dist.trim(),
                            block = blk.trim(),
                            panchayat = panchayat.trim(),
                            basicPay = basic.toDoubleOrNull() ?: t.basicPay,
                            daPercent = daPercent.toDoubleOrNull() ?: t.daPercent,
                            hraCategory = hraCat
                        )
                        viewModel.saveTeacherProfile(updated)
                        showEditDialog = false
                        Toast.makeText(context, if (isHi) "प्रोफाइल सफलतापूर्वक अपडेट की गई" else "Profile Updated", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                ) {
                    Text("अपडेट करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Restore Backup Dialog (Directly restores from Google Drive / Files or Local Device)
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = null,
                    tint = Navy800,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (isHi) "डेटा बैकअप रीस्टोर करें" else "Restore Backup Data",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Navy900
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isHi)
                            "आपने Google Drive या डिवाइस मेमोरी में जो सुरक्षित बैकअप (.ssb या JSON) सहेजा था, उसे यहां से सीधे पुनर्स्थापित करें:"
                        else
                            "Select and restore your encrypted backup (.ssb or JSON) from Google Drive or device storage:",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )

                    // 1. Google Drive / Storage File Picker
                    Button(
                        onClick = {
                            filePickerLauncher.launch(arrayOf("*/*"))
                            showRestoreDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHi) "Google Drive / स्टोरेज से बैकअप चुनें" else "Choose from Google Drive / Files",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 2. Direct Restore from Local Device Backup (One-click)
                    OutlinedButton(
                        onClick = {
                            viewModel.restoreFromLatestLocalBackup(context)
                            showRestoreDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Restore, contentDescription = null, tint = Navy800, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHi) "हालिया स्थानीय बैकअप से रीस्टोर करें" else "Restore Latest Device Backup",
                            fontSize = 12.sp,
                            color = Navy800
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("गोपनीयता नीति (Privacy Policy)") },
            text = {
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    item {
                        Text(
                            text = """
शिक्षक साथी (Shikshak Sathi)
गोपनीयता नीति एवं डेटा सुरक्षा सिद्धांत:

1. डेटा संप्रभुता (Local Device Security):
शिक्षकों का व्यक्तिगत विवरण, वेतन प्रविष्टियां, एचआरएमएस, ई-शिक्षाकोष व प्रान नंबर केवल आपके स्मार्टफोन के आंतरिक रूम (Room) डेटाबेस में सुरक्षित रहते हैं।

2. अनिवार्य लॉगिन एवं पंजीकरण मुक्त:
शिक्षक साथी ऐप का उपयोग करने के लिए शिक्षकों को किसी अनिवार्य साइन-अप अथवा व्यक्तिगत डेटा अपलोड की आवश्यकता नहीं है।

3. एन्क्रिप्टेड बैकअप (No Server Load):
बैकअप प्रक्रिया सीधे आपके स्मार्टफोन पर AES एन्क्रिप्शन के माध्यम से संपन्न होती है जिसे आप बिना किसी सर्वर निर्भरता के अपनी Google Drive या फोन मेमोरी में सहेज सकते हैं।

4. क्लाउड सिंक नीति:
केवल सामान्य विभागीय परिपत्र, आधिकारिक आदेश एवं अवकाश तालिका क्लाउड से प्राप्त किए जाते हैं। शिक्षकों का निजी डेटा बिना उनकी स्पष्ट अनुमति के कभी भी अपलोड नहीं किया जाता है।
                            """.trimIndent(),
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showPrivacyDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Navy800)) {
                    Text("स्वीकार है")
                }
            }
        )
    }

    // About App Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("शिक्षक साथी (Shikshak Sathi)") },
            text = {
                Column {
                    Text("संस्करण: 2.1.0 (Native Android Jetpack Compose)")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("बिहार राज्य के प्राथमिक, मध्य एवं माध्यमिक विद्यालय के शिक्षकों एवं प्रधानाध्यापकों के सहयोगार्थ एक संपूर्ण डिजिटल सेवा पोर्टल।")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("प्रमुख सुविधाएं: वेतन संगणना (ड्रॉपडाउन DA 50-69% व HRA 5-20%), प्रमुख शैक्षिक गतिविधियां (मशाल, इको क्लब, सुरक्षित शनिवार), पोर्टल लिंक्स, 30+ सरकारी प्रारूप, अवकाश कैलेंडर एवं एन्क्रिप्टेड बैकअप।", fontSize = 12.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(onClick = { showAboutDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Navy800)) {
                    Text("ठीक है")
                }
            }
        )
    }
}

@Composable
fun ProfileInfoRow(
    label: String,
    value: String,
    onCopy: ((String) -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFFA5B8CC),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                color = PureWhite,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (onCopy != null) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = { onCopy(value) },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = Gold500,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsCategoryCard(
    title: String,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Navy800),
                modifier = Modifier.padding(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 4.dp)
            )
            content()
        }
    }
}

@Composable
fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Navy800, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), modifier = Modifier.weight(1f))
    }
}
