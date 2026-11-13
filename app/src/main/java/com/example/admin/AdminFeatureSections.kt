package com.example.admin

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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.example.cloudflare.R2Config
import com.example.cloudflare.R2UploadMode
import com.example.cloudflare.R2UploadPickerRow
import com.example.ui.components.AppNetworkImage
import com.example.ui.components.ImageUrlHelper
import com.example.core.YouTubeUrlHelper
import com.example.database.entities.VideoTutorialEntity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.entities.ComplaintOptionEntity
import com.example.database.entities.DownloadResourceEntity
import com.example.database.entities.FinancialServiceEntity
import com.example.database.entities.HomeTopLinkEntity
import com.example.database.entities.NewsArticleEntity
import com.example.database.entities.YouTubeFeedEntity
import com.example.ui.theme.Gold100
import com.example.ui.theme.Gold500
import com.example.ui.theme.Gold700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Teal700

// ====================================================================
// 1. HOME TOP LINKS MANAGEMENT (PART 5)
// Unlimited links with (+), edit/delete, individual ON/OFF toggle
// ====================================================================
@Composable
fun AdminDynamicTopLinksTab(
    viewModel: AdminViewModel,
    listState: LazyListState = rememberLazyListState()
) {
    val links by viewModel.homeTopLinks.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingLink by remember { mutableStateOf<HomeTopLinkEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        AdminSubHeaderWithAdd(
            title = "होम पेज लिंक्स प्रबंधन (Top Links)",
            subtitle = "होम स्क्रीन पर प्रदर्शित होने वाले आधिकारिक लिंक्स",
            onAddClick = {
                editingLink = null
                showDialog = true
            }
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(links, key = { it.id }) { link ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = PureWhite)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${link.displayOrder}. ${link.title}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Navy900
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = if (link.isVisible) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (link.isVisible) "दिखाई दे रहा है" else "छिपा हुआ",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (link.isVisible) Color(0xFF2E7D32) else Color(0xFFC62828),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = link.url, fontSize = 11.5.sp, color = Navy800, maxLines = 1)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = link.isVisible,
                                onCheckedChange = { viewModel.toggleHomeTopLinkVisibility(link) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PureWhite,
                                    checkedTrackColor = Teal700
                                )
                            )
                            IconButton(onClick = {
                                editingLink = link
                                showDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Navy800)
                            }
                            IconButton(onClick = { viewModel.deleteHomeTopLink(link) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFC62828))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        var title by remember { mutableStateOf(editingLink?.title ?: "") }
        var url by remember { mutableStateOf(editingLink?.url ?: "https://") }
        var order by remember { mutableStateOf((editingLink?.displayOrder ?: (links.size + 1)).toString()) }
        var sendNotification by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingLink == null) "नया होम पेज लिंक जोड़ें" else "लिंक सम्पादित करें") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("लिंक शीर्षक (e.g. eShikshakosh)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("वेबसाइट URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = order,
                        onValueChange = { order = it },
                        label = { Text("क्रम संख्या (Display Order)") },
                        modifier = Modifier.fillMaxWidth()
                    )
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
                        if (title.isNotBlank() && url.isNotBlank()) {
                            val entity = HomeTopLinkEntity(
                                id = editingLink?.id ?: 0L,
                                title = title.trim(),
                                url = url.trim(),
                                isVisible = editingLink?.isVisible ?: true,
                                displayOrder = order.toIntOrNull() ?: 1
                            )
                            viewModel.saveHomeTopLink(entity, sendNotification = sendNotification)
                            showDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                ) {
                    Text(if (editingLink == null) "जोड़ें" else "अपडेट करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("रद्द करें") }
            }
        )
    }
}

// ====================================================================
// 2. COMPLAINT (SIKAYAT) MANAGEMENT (PART 4)
// Admin adds/edits/deletes options with title, description, mobile, email, clickable link, individual ON/OFF toggle
// ====================================================================
@Composable
fun AdminComplaintsTab(
    viewModel: AdminViewModel,
    listState: LazyListState = rememberLazyListState()
) {
    val options by viewModel.complaintOptions.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingOption by remember { mutableStateOf<ComplaintOptionEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        AdminSubHeaderWithAdd(
            title = "शिकायत निवारण प्रबंधन (Grievance Portal)",
            subtitle = "शिकायत प्रकोष्ठ, हेल्पलाइन नंबर व संपर्क ईमेल",
            onAddClick = {
                editingOption = null
                showDialog = true
            }
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(options, key = { it.id }) { option ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = PureWhite)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${option.displayOrder}. ${option.title}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Navy900,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = option.isVisible,
                                onCheckedChange = { viewModel.toggleComplaintVisibility(option) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PureWhite,
                                    checkedTrackColor = Teal700
                                )
                            )
                        }

                        if (option.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = option.description, fontSize = 12.sp, color = Color(0xFF455A64))
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (option.phone.isNotBlank()) {
                                Text(text = "📞 ${option.phone}", fontSize = 11.5.sp, color = Teal700, fontWeight = FontWeight.SemiBold)
                            }
                            if (option.email.isNotBlank()) {
                                Text(text = "✉️ ${option.email}", fontSize = 11.5.sp, color = Navy800, maxLines = 1)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = {
                                editingOption = option
                                showDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Navy800)
                            }
                            IconButton(onClick = { viewModel.deleteComplaintOption(option) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFC62828))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        var title by remember { mutableStateOf(editingOption?.title ?: "") }
        var desc by remember { mutableStateOf(editingOption?.description ?: "") }
        var phone by remember { mutableStateOf(editingOption?.phone ?: "") }
        var email by remember { mutableStateOf(editingOption?.email ?: "") }
        var linkUrl by remember { mutableStateOf(editingOption?.linkUrl ?: "https://") }
        var order by remember { mutableStateOf((editingOption?.displayOrder ?: (options.size + 1)).toString()) }
        var sendNotification by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingOption == null) "नया शिकायत निवारण विकल्प जोड़ें" else "शिकायत विकल्प सम्पादित करें") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("शीर्षक (e.g. BPGORS लोक शिकायत)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("विवरण") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("मोबाइल / हेल्पलाइन नंबर") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("संपर्क ईमेल") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = linkUrl, onValueChange = { linkUrl = it }, label = { Text("पोर्टल वेबसाइट लिंक (URL)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = order, onValueChange = { order = it }, label = { Text("क्रम संख्या (Display Order)") }, modifier = Modifier.fillMaxWidth())
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
                            val entity = ComplaintOptionEntity(
                                id = editingOption?.id ?: 0L,
                                title = title.trim(),
                                description = desc.trim(),
                                phone = phone.trim(),
                                email = email.trim(),
                                linkUrl = linkUrl.trim(),
                                isVisible = editingOption?.isVisible ?: true,
                                displayOrder = order.toIntOrNull() ?: 1
                            )
                            viewModel.saveComplaintOption(entity, sendNotification = sendNotification)
                            showDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                ) {
                    Text(if (editingOption == null) "जोड़ें" else "अपडेट करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("रद्द करें") }
            }
        )
    }
}

// ====================================================================
// 3. DOWNLOADS SECTION MANAGEMENT (PART 7)
// PDF and JPG support, Google Drive preview/download, dynamic (+), independent ON/OFF toggle
// ====================================================================
@Composable
fun AdminDownloadsTab(
    viewModel: AdminViewModel,
    listState: LazyListState = rememberLazyListState()
) {
    val downloads by viewModel.downloads.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<DownloadResourceEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        AdminSubHeaderWithAdd(
            title = "डाउनलोड्स सामग्री प्रबंधन (PDF/JPG)",
            subtitle = "विभागीय प्रपत्र, फॉर्म, शासनादेश प्रति व प्रारूप",
            onAddClick = {
                editingItem = null
                showDialog = true
            }
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                val driveConfig by viewModel.googleDriveConfig.collectAsState()
                val isEnabled = driveConfig?.isEnabled ?: true
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Navy800)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Google Drive फ़ोल्डर एक्सेस",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isEnabled) "स्थिति: सक्रिय (उपयोगकर्ताओं को दिखाई देगा)" else "स्थिति: निष्क्रिय (उपयोगकर्ताओं से छुपा हुआ)",
                                    fontSize = 11.sp,
                                    color = if (isEnabled) Gold500 else Color(0xFFFF8A80),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { viewModel.toggleGoogleDriveEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PureWhite,
                                    checkedTrackColor = Gold500
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = Color(0x22FFFFFF),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "🔒 प्रशासनिक संदर्भ खाता: juniorshalu100@gmail.com (केवल व्यवस्थापक को दृश्यमान)",
                                fontSize = 10.sp,
                                color = Gold100,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            items(downloads, key = { it.id }) { item ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = PureWhite)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = if (item.fileType.equals("JPG", ignoreCase = true)) Color(0xFFFFF3E0) else Color(0xFFFFEBEE),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = item.fileType.uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.fileType.equals("JPG", ignoreCase = true)) Color(0xFFE65100) else Color(0xFFC62828),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = item.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = Navy900
                                )
                            }

                            Switch(
                                checked = item.isVisible,
                                onCheckedChange = { viewModel.toggleDownloadVisibility(item) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PureWhite,
                                    checkedTrackColor = Teal700
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "श्रेणी: ${item.category}", fontSize = 11.5.sp, color = Teal700, fontWeight = FontWeight.Medium)
                            Text(text = "• आकार: ${item.fileSize}", fontSize = 11.5.sp, color = Color.Gray)
                        }

                        if (item.pdfUrl.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "URL: ${item.pdfUrl}", fontSize = 10.5.sp, color = Navy800, maxLines = 1)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = {
                                editingItem = item
                                showDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Navy800)
                            }
                            IconButton(onClick = { viewModel.deleteDownloadResource(item) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFC62828))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        var title by remember(showDialog, editingItem?.id) { mutableStateOf(editingItem?.title ?: "") }
        var category by remember(showDialog, editingItem?.id) { mutableStateOf(editingItem?.category ?: "प्रपत्र") }
        var fileType by remember(showDialog, editingItem?.id) { mutableStateOf(editingItem?.fileType ?: "PDF") }
        var fileSize by remember(showDialog, editingItem?.id) { mutableStateOf(editingItem?.fileSize ?: "1.2 MB") }
        var url by remember(showDialog, editingItem?.id) { mutableStateOf(editingItem?.pdfUrl ?: "https://") }
        var orderNo by remember(showDialog, editingItem?.id) { mutableStateOf(editingItem?.orderNumber ?: "") }
        var desc by remember(showDialog, editingItem?.id) { mutableStateOf(editingItem?.description ?: "") }
        var sendNotification by remember(showDialog, editingItem?.id) { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingItem == null) "नई डाउनलोड सामग्री जोड़ें" else "डाउनलोड सामग्री सम्पादित करें") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("प्रपत्र / दस्तावेज का नाम") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("श्रेणी (e.g. छुट्टी प्रपत्र, वेतन)") }, modifier = Modifier.fillMaxWidth())
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = fileType,
                            onValueChange = { fileType = it.uppercase() },
                            label = { Text("फाइल टाइप (PDF / JPG)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = fileSize,
                            onValueChange = { fileSize = it },
                            label = { Text("फाइल आकार (e.g. 850 KB)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Cloudflare R2 / Drive / PDF / JPG लिंक (URL)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    R2UploadPickerRow(
                        adminViewModel = viewModel,
                        folder = R2Config.FOLDER_DOWNLOADS,
                        titleHint = title.ifBlank { "download_doc" },
                        existingUrl = url,
                        mode = R2UploadMode.PDF_OR_IMAGE,
                        testTagPrefix = "download_dialog",
                        onUploaded = { uploaded ->
                            url = uploaded.url
                            fileType = uploaded.fileType
                            fileSize = uploaded.sizeFormatted
                        }
                    )
                    if (ImageUrlHelper.isLikelyImageUrl(url, fileType)) {
                        AppNetworkImage(
                            imageUrl = url.trim(),
                            contentDescription = "Download Image Preview",
                            versionTag = editingItem?.timestamp ?: 0L,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                    OutlinedTextField(value = orderNo, onValueChange = { orderNo = it }, label = { Text("प्रपत्र क्रमांक / ज्ञापांक (वैकल्पिक)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("संक्षिप्त विवरण") }, modifier = Modifier.fillMaxWidth())

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
                            val entity = DownloadResourceEntity(
                                id = editingItem?.id ?: 0L,
                                cloudId = editingItem?.cloudId ?: "",
                                title = title.trim(),
                                category = category.trim(),
                                fileType = if (fileType.contains("JPG", ignoreCase = true)) "JPG" else "PDF",
                                fileSize = fileSize.trim(),
                                pdfUrl = url.trim(),
                                publicationDate = editingItem?.publicationDate ?: "",
                                orderNumber = orderNo.trim(),
                                description = desc.trim(),
                                localFilePath = editingItem?.localFilePath ?: "",
                                isVisible = editingItem?.isVisible ?: true,
                                downloadCount = editingItem?.downloadCount ?: 0,
                                displayOrder = editingItem?.displayOrder ?: 0,
                                timestamp = System.currentTimeMillis()
                            )
                            viewModel.saveDownloadResource(entity, sendNotification = sendNotification)
                            showDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                ) {
                    Text(if (editingItem == null) "जोड़ें" else "अपडेट करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("रद्द करें") }
            }
        )
    }
}

// ====================================================================
// 4. FINANCIAL SERVICES MANAGEMENT (LOAN, CARD, INVESTMENT) (PART 8)
// Separate admin sections for Loan, Credit Card, Investment with Google Forms & link support
// ====================================================================
@Composable
fun AdminFinancialServiceTab(
    category: String,
    titleText: String,
    viewModel: AdminViewModel,
    listState: LazyListState = rememberLazyListState()
) {
    val allServices by when (category) {
        "LOAN" -> viewModel.loanServices.collectAsState()
        "CREDIT_CARD" -> viewModel.creditCardServices.collectAsState()
        else -> viewModel.investmentServices.collectAsState()
    }

    var showDialog by remember { mutableStateOf(false) }
    var editingService by remember { mutableStateOf<FinancialServiceEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        AdminSubHeaderWithAdd(
            title = titleText,
            subtitle = "बैंक विकल्प, Google Forms व आवेदन लिंक्स प्रबंधन",
            onAddClick = {
                editingService = null
                showDialog = true
            }
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(allServices, key = { it.id }) { item ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = PureWhite)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${item.displayOrder}. ${item.title}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Navy900
                                )
                                if (item.isGoogleForm) {
                                    Surface(color = Gold100, shape = RoundedCornerShape(4.dp)) {
                                        Text(
                                            text = "Google Form लिंक",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Navy900,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Switch(
                                checked = item.isVisible,
                                onCheckedChange = { viewModel.toggleFinancialServiceVisibility(item) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PureWhite,
                                    checkedTrackColor = Teal700
                                )
                            )
                        }

                        if (item.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = item.description, fontSize = 12.sp, color = Color(0xFF455A64))
                        }

                        if (item.applyUrl.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "Link: ${item.applyUrl}", fontSize = 11.sp, color = Navy800, maxLines = 1)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = {
                                editingService = item
                                showDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Navy800)
                            }
                            IconButton(onClick = { viewModel.deleteFinancialService(item) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFC62828))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        var title by remember { mutableStateOf(editingService?.title ?: "") }
        var desc by remember { mutableStateOf(editingService?.description ?: "") }
        var applyUrl by remember { mutableStateOf(editingService?.applyUrl ?: "https://") }
        var isGoogleForm by remember { mutableStateOf(editingService?.isGoogleForm ?: false) }
        var btnText by remember { mutableStateOf(editingService?.buttonText ?: "आवेदन करें") }
        var order by remember { mutableStateOf((editingService?.displayOrder ?: (allServices.size + 1)).toString()) }
        var sendNotification by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingService == null) "नया $titleText विकल्प जोड़ें" else "विकल्प सम्पादित करें") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("शीर्षक (e.g. SBI शिक्षक ऋण)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("विवरण व मुख्य विशेषताएं") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = applyUrl, onValueChange = { applyUrl = it }, label = { Text("आवेदन URL / Google Form Link") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = btnText, onValueChange = { btnText = it }, label = { Text("बटन का नाम (e.g. फॉर्म भरें)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = order, onValueChange = { order = it }, label = { Text("क्रम संख्या") }, modifier = Modifier.fillMaxWidth())

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isGoogleForm, onCheckedChange = { isGoogleForm = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "यह एक Google Form लिंक है", fontSize = 12.sp)
                    }

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
                            val entity = FinancialServiceEntity(
                                id = editingService?.id ?: 0L,
                                category = category,
                                title = title.trim(),
                                description = desc.trim(),
                                applyUrl = applyUrl.trim(),
                                isGoogleForm = isGoogleForm,
                                buttonText = btnText.trim(),
                                isVisible = editingService?.isVisible ?: true,
                                displayOrder = order.toIntOrNull() ?: 1
                            )
                            viewModel.saveFinancialService(entity, sendNotification = sendNotification)
                            showDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                ) {
                    Text(if (editingService == null) "जोड़ें" else "अपडेट करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("रद्द करें") }
            }
        )
    }
}

// ====================================================================
// 5. YOUTUBE RSS FEED MANAGEMENT (PART 10)
// Admin adds/edits/deletes YouTube channel RSS URL, automatically fetches & displays videos
// ====================================================================
@Composable
fun AdminYouTubeRssContent(
    viewModel: AdminViewModel,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val feeds by viewModel.youtubeFeeds.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingFeed by remember { mutableStateOf<YouTubeFeedEntity?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = Color(0xFF1E2D42),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "यूट्यूब वीडियो RSS फीड प्रबंधन",
                        fontWeight = FontWeight.Bold,
                        color = PureWhite,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "यूट्यूब चैनल के नए वीडियो ऐप में स्वतः सिंक होते हैं",
                        color = Gold100,
                        fontSize = 10.5.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            isRefreshing = true
                            viewModel.refreshYouTubeFeeds { count ->
                                isRefreshing = false
                                Toast.makeText(
                                    context,
                                    if (count > 0) "$count नए यूट्यूब वीडियो सिंक हुए!" else "फीड रिफ्रेश पूर्ण (कोई नया वीडियो नहीं मिला)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        enabled = !isRefreshing,
                        colors = ButtonDefaults.buttonColors(containerColor = Gold500)
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Navy900)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Navy900, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("वीडियो फेच करें", color = Navy900, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            editingFeed = null
                            showDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Teal700)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("नया चैनल", color = PureWhite, fontSize = 11.sp)
                    }
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F0FE)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "💡 यूट्यूब RSS फीड URL प्रारूप:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Navy900
                        )
                        Text(
                            text = "https://www.youtube.com/feeds/videos.xml?channel_id=CHANNEL_ID",
                            fontSize = 11.sp,
                            color = Color(0xFF1967D2)
                        )
                    }
                }
            }

            items(feeds, key = { it.id }) { feed ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = PureWhite)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = feed.channelTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Navy900
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = feed.feedUrl, fontSize = 11.sp, color = Navy800, maxLines = 1)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = feed.isVisible,
                                onCheckedChange = { viewModel.toggleYouTubeFeedVisibility(feed) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PureWhite,
                                    checkedTrackColor = Teal700
                                )
                            )
                            IconButton(onClick = {
                                editingFeed = feed
                                showDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Navy800)
                            }
                            IconButton(onClick = { viewModel.deleteYouTubeFeed(feed) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFC62828))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        var channelTitle by remember { mutableStateOf(editingFeed?.channelTitle ?: "") }
        var feedUrl by remember { mutableStateOf(editingFeed?.feedUrl ?: "https://www.youtube.com/feeds/videos.xml?channel_id=") }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingFeed == null) "नया YouTube RSS चैनल जोड़ें" else "RSS फीड सम्पादित करें") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = channelTitle,
                        onValueChange = { channelTitle = it },
                        label = { Text("चैनल / प्लेलिस्ट का नाम") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = feedUrl,
                        onValueChange = { feedUrl = it },
                        label = { Text("यूट्यूब RSS फीड URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (channelTitle.isNotBlank() && feedUrl.isNotBlank()) {
                            val entity = YouTubeFeedEntity(
                                id = editingFeed?.id ?: 0L,
                                channelTitle = channelTitle.trim(),
                                feedUrl = feedUrl.trim(),
                                isVisible = editingFeed?.isVisible ?: true
                            )
                            viewModel.saveYouTubeFeed(entity)
                            showDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                ) {
                    Text(if (editingFeed == null) "सेव एवं फेच करें" else "अपडेट करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("रद्द करें") }
            }
        )
    }
}

// ====================================================================
// 5B. COMPLETE YOUTUBE VIDEO MANAGEMENT (PART 4 of Prompt)
// Direct YouTube URL add, auto ID extraction, live thumbnail, reorder, pin, home page toggle
// ====================================================================
@Composable
fun AdminVideosManagementTab(
    viewModel: AdminViewModel,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val videos by viewModel.videos.collectAsState()
    var selectedSubTab by rememberSaveable { mutableIntStateOf(0) } // 0: Managed Videos, 1: Channel RSS Feeds
    var showAddDialog by remember { mutableStateOf(false) }
    var editingVideo by remember { mutableStateOf<VideoTutorialEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = Color(0xFF1E2D42),
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
                            text = "वीडियो सामग्री प्रबंधन (YouTube Videos)",
                            fontWeight = FontWeight.Bold,
                            color = PureWhite,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "URL द्वारा वीडियो जोड़ें, पिन करें, क्रम बदलें व होम पेज पर दिखाएं",
                            color = Gold100,
                            fontSize = 10.5.sp
                        )
                    }

                    if (selectedSubTab == 0) {
                        Button(
                            onClick = {
                                editingVideo = null
                                showAddDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Gold500)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Navy900, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("नया वीडियो (+)", color = Navy900, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sub-tabs: 0: Managed Videos, 1: Channel RSS Feeds
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedSubTab == 0,
                        onClick = { selectedSubTab = 0 },
                        label = { Text("वीडियो सूची (${videos.size})", fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Gold500,
                            selectedLabelColor = Navy900
                        )
                    )
                    FilterChip(
                        selected = selectedSubTab == 1,
                        onClick = { selectedSubTab = 1 },
                        label = { Text("चैनल RSS फीड", fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Gold500,
                            selectedLabelColor = Navy900
                        )
                    )
                }
            }
        }

        if (selectedSubTab == 1) {
            AdminYouTubeRssContent(viewModel = viewModel)
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (videos.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = PureWhite)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(30.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(44.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("वर्तमान में कोई वीडियो नहीं है। 'नया वीडियो (+)' पर क्लिक करके YouTube URL से जोड़ें।", fontSize = 12.sp, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }
                } else {
                    items(videos, key = { it.id }) { video ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = PureWhite)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    // Thumbnail preview
                                    Box(
                                        modifier = Modifier
                                            .size(width = 110.dp, height = 75.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Navy900),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val thumb = if (video.thumbnailUrl.isNotBlank()) {
                                            video.thumbnailUrl
                                        } else if (video.youtubeId.isNotBlank()) {
                                            YouTubeUrlHelper.getThumbnailUrl(video.youtubeId)
                                        } else ""

                                        if (thumb.isNotBlank()) {
                                            AppNetworkImage(
                                                imageUrl = thumb,
                                                contentDescription = video.title,
                                                versionTag = video.timestamp,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Gold500)
                                        }

                                        // Duration
                                        Surface(
                                            color = Color(0xCC000000),
                                            shape = RoundedCornerShape(3.dp),
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(3.dp)
                                        ) {
                                            Text(
                                                text = video.duration,
                                                color = PureWhite,
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = video.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Navy900,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "श्रेणी: ${video.category} • ID: ${video.youtubeId.ifBlank { "N/A" }}",
                                            fontSize = 10.5.sp,
                                            color = Teal700
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            if (video.isPinned) {
                                                Surface(color = Gold500, shape = RoundedCornerShape(3.dp)) {
                                                    Text(text = "📌 पिन", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Navy900, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                                                }
                                            }
                                            if (video.isFeaturedOnHome) {
                                                Surface(color = Color(0xFFE8F0FE), shape = RoundedCornerShape(3.dp)) {
                                                    Text(text = "🏠 होम पेज", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1967D2), modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                                                }
                                            }
                                        }
                                    }
                                }

                                if (video.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = video.description,
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF455A64),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Action Buttons Row: Move Up/Down, Pin, Home Toggle, Edit, Delete
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Move Up
                                        IconButton(
                                            onClick = { viewModel.moveVideoOrder(video, moveUp = true) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = Navy800, modifier = Modifier.size(16.dp))
                                        }
                                        // Move Down
                                        IconButton(
                                            onClick = { viewModel.moveVideoOrder(video, moveUp = false) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = Navy800, modifier = Modifier.size(16.dp))
                                        }
                                        // Pin / Unpin
                                        OutlinedButton(
                                            onClick = { viewModel.togglePinVideo(video) },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(Icons.Default.PushPin, contentDescription = null, tint = if (video.isPinned) Gold700 else Color.Gray, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(if (video.isPinned) "पिन है" else "पिन", fontSize = 10.sp, color = if (video.isPinned) Gold700 else Color.Gray)
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        // Home Featured Toggle
                                        OutlinedButton(
                                            onClick = { viewModel.toggleFeatureOnHomeVideo(video) },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(Icons.Default.Home, contentDescription = null, tint = if (video.isFeaturedOnHome) Color(0xFF1967D2) else Color.Gray, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(if (video.isFeaturedOnHome) "होम पर है" else "होम", fontSize = 10.sp, color = if (video.isFeaturedOnHome) Color(0xFF1967D2) else Color.Gray)
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = {
                                            editingVideo = video
                                            showAddDialog = true
                                        }, modifier = Modifier.size(30.dp)) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Navy800, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(onClick = { viewModel.deleteVideoTutorial(video) }, modifier = Modifier.size(30.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFC62828), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var ytUrl by remember(showAddDialog, editingVideo?.id) { mutableStateOf(editingVideo?.youtubeUrl ?: "") }
        var autoYtId by remember(showAddDialog, editingVideo?.id) { mutableStateOf(editingVideo?.youtubeId ?: "") }
        var title by remember(showAddDialog, editingVideo?.id) { mutableStateOf(editingVideo?.title ?: "") }
        var desc by remember(showAddDialog, editingVideo?.id) { mutableStateOf(editingVideo?.description ?: "") }
        var category by remember(showAddDialog, editingVideo?.id) { mutableStateOf(editingVideo?.category ?: "ई-शिक्षाकोष") }
        var duration by remember(showAddDialog, editingVideo?.id) { mutableStateOf(editingVideo?.duration ?: "10:00") }
        var isFeaturedHome by remember(showAddDialog, editingVideo?.id) { mutableStateOf(editingVideo?.isFeaturedOnHome ?: true) }
        var isPinned by remember(showAddDialog, editingVideo?.id) { mutableStateOf(editingVideo?.isPinned ?: false) }
        var sendNotification by remember(showAddDialog, editingVideo?.id) { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (editingVideo == null) "नया YouTube वीडियो जोड़ें" else "वीडियो सम्पादित करें") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = ytUrl,
                        onValueChange = {
                            ytUrl = it
                            val extracted = YouTubeUrlHelper.extractVideoId(it)
                            if (extracted != null) {
                                autoYtId = extracted
                            }
                        },
                        label = { Text("YouTube वीडियो URL") },
                        placeholder = { Text("https://youtu.be/... या watch?v=...") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (autoYtId.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE8F5E9), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            val liveThumb = YouTubeUrlHelper.getThumbnailUrl(autoYtId)
                            AppNetworkImage(
                                imageUrl = liveThumb,
                                contentDescription = "Live Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 60.dp, height = 40.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("पहचाना गया Video ID: $autoYtId", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                Text("थंबनेल स्वतः सेट हो गया है", fontSize = 10.sp, color = Color.DarkGray)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("वीडियो का शीर्षक (Title)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("विवरण (Description)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("श्रेणी") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = duration,
                            onValueChange = { duration = it },
                            label = { Text("अवधि (MM:SS)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isFeaturedHome, onCheckedChange = { isFeaturedHome = it })
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("होम पेज पर प्रमुखता से दिखाएं", fontSize = 12.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isPinned, onCheckedChange = { isPinned = it })
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("शीर्ष पर पिन करें", fontSize = 12.sp)
                    }

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
                            val yId = if (autoYtId.isNotBlank()) autoYtId else (YouTubeUrlHelper.extractVideoId(ytUrl) ?: "")
                            val thumb = if (yId.isNotBlank()) YouTubeUrlHelper.getThumbnailUrl(yId) else ""
                            val entity = VideoTutorialEntity(
                                id = editingVideo?.id ?: 0L,
                                title = title,
                                category = category,
                                youtubeUrl = ytUrl,
                                youtubeId = yId,
                                thumbnailUrl = thumb,
                                duration = duration,
                                description = desc,
                                isPinned = isPinned,
                                isFeaturedOnHome = isFeaturedHome,
                                displayOrder = editingVideo?.displayOrder ?: (videos.size + 1)
                            )
                            viewModel.saveVideoTutorial(entity, sendNotification = sendNotification)
                            showAddDialog = false
                            Toast.makeText(context, "वीडियो सफलतापूर्वक सेव हुआ!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                ) {
                    Text(if (editingVideo == null) "सेव करें" else "अपडेट करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("रद्द करें") }
            }
        )
    }
}

@Composable
fun AdminYouTubeRssTab(
    viewModel: AdminViewModel,
    listState: LazyListState = rememberLazyListState()
) = AdminVideosManagementTab(viewModel, listState)

// ====================================================================
// 6. NEWS SECTION MANAGEMENT (PART 11)
// Admin adds/edits/deletes news with headline, content, external link, publication date, independent ON/OFF toggle
// ====================================================================
@Composable
fun AdminNewsTab(
    viewModel: AdminViewModel,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val newsList by viewModel.newsArticles.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingNews by remember { mutableStateOf<NewsArticleEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        AdminSubHeaderWithAdd(
            title = "शिक्षा समाचार एवं अपडेट्स प्रबंधन",
            subtitle = "विभागीय बुलेटिन, वेतन घोषणाएं व ताज़ा खबरें (अधिकतम 5 पिन)",
            onAddClick = {
                editingNews = null
                showDialog = true
            }
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(newsList, key = { it.id }) { news ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = PureWhite)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (news.isPinned) {
                                    Surface(
                                        color = Gold500,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "📌 पिन #${news.pinOrder}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Navy900,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = news.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Navy900
                                )
                            }
                            Switch(
                                checked = news.isVisible,
                                onCheckedChange = { viewModel.toggleNewsVisibility(news) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PureWhite,
                                    checkedTrackColor = Teal700
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = news.summary, fontSize = 12.sp, color = Color(0xFF455A64))

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "📅 ${news.publishedAt}", fontSize = 11.sp, color = Teal700)
                            Text(text = "• श्रेणी: ${news.category}", fontSize = 11.sp, color = Color.Gray)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Pin / Unpin button
                                OutlinedButton(
                                    onClick = {
                                        viewModel.togglePinNewsArticle(news) {
                                            Toast.makeText(context, "अधिकतम 5 समाचार ही पिन किए जा सकते हैं!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = "Pin/Unpin",
                                        tint = if (news.isPinned) Gold700 else Color.Gray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (news.isPinned) "अनपिन करें" else "पिन करें (शीर्ष 5)",
                                        fontSize = 10.5.sp,
                                        color = if (news.isPinned) Gold700 else Color.Gray
                                    )
                                }

                                if (news.isPinned) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { viewModel.movePinnedNewsOrder(news, moveUp = true) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = Navy800, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.movePinnedNewsOrder(news, moveUp = false) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = Navy800, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    editingNews = news
                                    showDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Navy800)
                                }
                                IconButton(onClick = { viewModel.deleteNewsArticle(news) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFC62828))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        var title by remember(showDialog, editingNews?.id) { mutableStateOf(editingNews?.title ?: "") }
        var summary by remember(showDialog, editingNews?.id) { mutableStateOf(editingNews?.summary ?: "") }
        var content by remember(showDialog, editingNews?.id) { mutableStateOf(editingNews?.content ?: "") }
        var category by remember(showDialog, editingNews?.id) { mutableStateOf(editingNews?.category ?: "शिक्षा समाचार") }
        var sourceUrl by remember(showDialog, editingNews?.id) { mutableStateOf(editingNews?.sourceUrl ?: "https://state.bihar.gov.in/educationbihar") }
        var imageUrl by remember(showDialog, editingNews?.id) { mutableStateOf(editingNews?.imageUrl ?: "") }
        var publishedAt by remember(showDialog, editingNews?.id) { mutableStateOf(editingNews?.publishedAt ?: "08/11/2026") }
        var sendNotification by remember(showDialog, editingNews?.id) { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingNews == null) "नया समाचार जोड़ें" else "समाचार सम्पादित करें") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("हेडलाइन (शीर्षक)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = summary, onValueChange = { summary = it }, label = { Text("संक्षिप्त विवरण (Summary)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("विस्तृत विवरण (Content)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("श्रेणी (e.g. विभागीय, वेतन)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = publishedAt, onValueChange = { publishedAt = it }, label = { Text("प्रकाशन तिथि (dd/MM/yyyy)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sourceUrl, onValueChange = { sourceUrl = it }, label = { Text("विस्तृत आदेश / संदर्भ URL") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("समाचार फोटो URL (R2 / Link)") }, modifier = Modifier.fillMaxWidth())
                    R2UploadPickerRow(
                        adminViewModel = viewModel,
                        folder = R2Config.FOLDER_NEWS,
                        titleHint = title.ifBlank { "news_photo" },
                        existingUrl = imageUrl,
                        mode = R2UploadMode.IMAGE_ONLY,
                        testTagPrefix = "news_dialog_img",
                        onUploaded = { uploaded -> imageUrl = uploaded.url }
                    )
                    if (imageUrl.isNotBlank()) {
                        AppNetworkImage(
                            imageUrl = imageUrl.trim(),
                            contentDescription = "News Image Preview",
                            versionTag = editingNews?.timestamp ?: 0L,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }

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
                            val entity = NewsArticleEntity(
                                id = editingNews?.id ?: 0L,
                                title = title.trim(),
                                summary = summary.trim(),
                                content = content.trim(),
                                category = category.trim(),
                                publishedAt = publishedAt.trim(),
                                source = editingNews?.source ?: "शिक्षा विभाग, बिहार",
                                sourceUrl = sourceUrl.trim(),
                                imageUrl = imageUrl.trim(),
                                isPinned = editingNews?.isPinned ?: false,
                                pinOrder = editingNews?.pinOrder ?: 0,
                                isBreaking = editingNews?.isBreaking ?: false,
                                isVisible = editingNews?.isVisible ?: true,
                                timestamp = System.currentTimeMillis()
                            )
                            viewModel.saveNewsArticle(entity, sendNotification = sendNotification)
                            showDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                ) {
                    Text(if (editingNews == null) "जोड़ें" else "अपडेट करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("रद्द करें") }
            }
        )
    }
}

// ====================================================================
// 7. NEWS RSS FEED MANAGEMENT (PART 11 & BLOGGER RSS)
// Manage external news blogs like The Magadh Times (https://themagadhtimes.blogspot.com/)
// ====================================================================
@Composable
fun AdminNewsRssTab(
    viewModel: AdminViewModel,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val feeds by viewModel.newsRssFeeds.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingFeed by remember { mutableStateOf<com.example.database.entities.NewsRssFeedEntity?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = Color(0xFF1E2D42),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "लाइव न्यूज़ RSS फीड्स प्रबंधन",
                        fontWeight = FontWeight.Bold,
                        color = PureWhite,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "The Magadh Times व अन्य शिक्षा ब्लॉग RSS फीड्स",
                        color = Gold100,
                        fontSize = 10.5.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            isRefreshing = true
                            viewModel.refreshNewsRssFeeds { count ->
                                isRefreshing = false
                                Toast.makeText(
                                    context,
                                    if (count > 0) "$count नए समाचार सिंक हुए!" else "फीड रिफ्रेश पूर्ण",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        enabled = !isRefreshing,
                        colors = ButtonDefaults.buttonColors(containerColor = Gold500)
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Navy900)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Navy900, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("समाचार फेच करें", color = Navy900, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            editingFeed = null
                            showDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Teal700)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("नया फीड (+)", color = PureWhite, fontSize = 11.sp)
                    }
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F0FE)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "💡 ब्लॉग / न्यूज़ RSS फीड URL प्रारूप:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Navy900
                        )
                        Text(
                            text = "https://themagadhtimes.blogspot.com/feeds/posts/default?alt=rss",
                            fontSize = 11.sp,
                            color = Color(0xFF1967D2)
                        )
                    }
                }
            }

            items(feeds, key = { it.id }) { feed ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = PureWhite)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = feed.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Navy900
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = feed.feedUrl, fontSize = 11.sp, color = Navy800, maxLines = 1)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = feed.isVisible,
                                onCheckedChange = { viewModel.toggleNewsRssFeedVisibility(feed) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PureWhite,
                                    checkedTrackColor = Teal700
                                )
                            )
                            IconButton(onClick = {
                                editingFeed = feed
                                showDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Navy800)
                            }
                            IconButton(onClick = { viewModel.deleteNewsRssFeed(feed) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFC62828))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        var title by remember { mutableStateOf(editingFeed?.title ?: "") }
        var feedUrl by remember { mutableStateOf(editingFeed?.feedUrl ?: "https://") }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingFeed == null) "नया न्यूज़ RSS फीड जोड़ें" else "RSS फीड सम्पादित करें") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("फीड नाम (e.g. द मगध टाइम्स)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = feedUrl,
                        onValueChange = { feedUrl = it },
                        label = { Text("RSS Feed URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank() && feedUrl.isNotBlank()) {
                            val entity = com.example.database.entities.NewsRssFeedEntity(
                                id = editingFeed?.id ?: 0L,
                                title = title.trim(),
                                feedUrl = feedUrl.trim(),
                                isVisible = editingFeed?.isVisible ?: true
                            )
                            viewModel.saveNewsRssFeed(entity)
                            showDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                ) {
                    Text(if (editingFeed == null) "सेव एवं फेच करें" else "अपडेट करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("रद्द करें") }
            }
        )
    }
}

// Common Sub-Header with Add (+) Button
@Composable
private fun AdminSubHeaderWithAdd(
    title: String,
    subtitle: String,
    onAddClick: () -> Unit
) {
    Surface(
        color = Color(0xFF1E2D42),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, color = PureWhite, fontSize = 14.sp)
                Text(text = subtitle, color = Gold100, fontSize = 10.5.sp)
            }
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = Gold500)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Navy900, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("नया जोड़ें (+)", color = Navy900, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
