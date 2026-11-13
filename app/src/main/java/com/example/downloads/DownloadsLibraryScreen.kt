package com.example.downloads

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderZip
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.heightIn
import com.example.core.DeepLinkHelper
import com.example.core.DeepLinkType
import com.example.core.FileDownloadHelper
import com.example.ui.components.AppNetworkImage
import com.example.ui.components.ImageUrlHelper
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
import com.example.database.entities.DownloadResourceEntity
import com.example.home.HomeViewModel
import com.example.ui.theme.Gold100
import com.example.ui.theme.Gold500
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Teal700

@Composable
fun DownloadsLibraryScreen(
    homeViewModel: HomeViewModel,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val language = LocalAppLanguage.current
    val isHi = language.isHindi()

    val downloads by homeViewModel.downloadResources.collectAsState()
    val driveConfig by homeViewModel.googleDriveConfig.collectAsState()
    val pendingDeepLink by homeViewModel.pendingDeepLink.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("सभी") }
    var viewingResource by remember { mutableStateOf<DownloadResourceEntity?>(null) }

    LaunchedEffect(pendingDeepLink, downloads) {
        val target = pendingDeepLink ?: return@LaunchedEffect
        if (target.type != DeepLinkType.DOWNLOAD) return@LaunchedEffect
        val match = DeepLinkHelper.findMatchingDownload(downloads, target)
        if (match != null) {
            selectedCategory = "सभी"
            searchQuery = ""
            viewingResource = match
            homeViewModel.consumeDeepLink(target.eventId)
        }
    }

    val categories = listOf("सभी", "प्रपत्र", "छुट्टी प्रपत्र", "वेतन सत्यापन", "विभागीय नियमावली", "परीक्षा")

    val filteredList = downloads.filter { item ->
        val isVisible = item.isVisible
        val matchesCategory = selectedCategory == "सभी" || item.category.contains(selectedCategory, ignoreCase = true)
        val matchesQuery = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.category.contains(searchQuery, ignoreCase = true) ||
                item.description.contains(searchQuery, ignoreCase = true)
        isVisible && matchesCategory && matchesQuery
    }.sortedWith(compareBy<DownloadResourceEntity> { it.displayOrder }.thenByDescending { it.timestamp })

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6F9))
            .testTag("downloads_library_screen")
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
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Navy900,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isHi) "डाउनलोड एवं महत्वपूर्ण प्रपत्र" else "Downloads & Resources",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                        Text(
                            text = if (isHi) "आधिकारिक PDF, आवेदन प्रारूप व विभागीय नियमावली" else "Official Forms, Guides & Circulars",
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
                            text = if (isHi) "प्रपत्र, नियम या प्रारूप खोजें..." else "Search forms, rules, guides...",
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
                        .testTag("downloads_search_input")
                )
            }
        }

        // Category Filter Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
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

        // List with Google Drive Card always on top
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Official Google Drive Folder Card (Administrator-Controlled)
            if (driveConfig?.isEnabled != false) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Navy800),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("google_drive_folder_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(Gold500, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderZip,
                                        contentDescription = null,
                                        tint = Navy900,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "समस्त प्रपत्र एवं फॉर्म (Google Drive फ़ोल्डर)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = PureWhite
                                    )
                                    Text(
                                        text = "विभागीय प्रपत्र एवं शासनादेश अभिलेख (PDF/JPG)",
                                        fontSize = 11.sp,
                                        color = Gold100
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            val targetDriveUrl = driveConfig?.folderUrl?.ifBlank { com.example.core.GoogleDriveHelper.MAIN_TEACHER_DRIVE_FOLDER }
                                ?: com.example.core.GoogleDriveHelper.MAIN_TEACHER_DRIVE_FOLDER
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetDriveUrl))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Gold500),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("ड्राइव खोलें", color = Navy900, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (filteredList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FolderZip,
                                contentDescription = null,
                                tint = Color(0xFFB0BEC5),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (isHi) "कोई प्रपत्र उपलब्ध नहीं है" else "No downloadable forms found",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF546E7A)
                            )
                            Text(
                                text = if (isHi) "कृपया ऊपर दिए गए Google Drive फ़ोल्डर को देखें" else "Check the Google Drive folder above",
                                fontSize = 12.sp,
                                color = Color(0xFF90A4AE)
                            )
                        }
                    }
                }
            } else {
                items(filteredList, key = { it.id }) { item ->
                    DownloadItemCard(
                        item = item,
                        onClick = { viewingResource = item },
                        onDownload = {
                            val rawUrl = if (item.pdfUrl.isNotBlank()) item.pdfUrl else "https://state.bihar.gov.in/educationbihar"
                            val downloadUrl = com.example.core.GoogleDriveHelper.getDirectDownloadUrl(rawUrl)
                            coroutineScope.launch {
                                FileDownloadHelper.openDocumentFast(context, downloadUrl, item.fileType)
                            }
                        },
                        onShare = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, item.title)
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "${item.title}\nश्रेणी: ${item.category}\nआकार: ${item.fileSize}\n\nडाउनलोड लिंक: ${item.pdfUrl}\n\n'शिक्षक साथी' ऐप द्वारा साझा किया गया"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "प्रपत्र लिंक शेयर करें"))
                        }
                    )
                }
            }
        }
    }

    // Detail Dialog
    viewingResource?.let { item ->
        AlertDialog(
            onDismissRequest = { viewingResource = null },
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
                                text = item.category,
                                color = Navy900,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Text(
                            text = item.fileSize,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Teal700
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = item.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy900,
                        lineHeight = 22.sp
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (ImageUrlHelper.isLikelyImageUrl(item.pdfUrl, item.fileType)) {
                        AppNetworkImage(
                            imageUrl = item.pdfUrl,
                            contentDescription = item.title,
                            versionTag = item.timestamp,
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 220.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Text(
                        text = item.description,
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "फॉर्मेट: ${item.fileType.uppercase()} दस्तावेज (${item.fileSize})",
                                fontSize = 12.sp,
                                color = Color(0xFF455A64)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val rawUrl = if (item.pdfUrl.isNotBlank()) item.pdfUrl else "https://state.bihar.gov.in/educationbihar"
                    val previewUrl = com.example.core.GoogleDriveHelper.getPreviewUrl(rawUrl)
                    val downloadUrl = com.example.core.GoogleDriveHelper.getDirectDownloadUrl(rawUrl)

                    if (com.example.core.GoogleDriveHelper.extractDriveFileId(rawUrl) != null) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(previewUrl)))
                                } catch (_: Exception) {}
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "प्रिव्यू", fontSize = 12.sp, color = Navy900, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                FileDownloadHelper.openDocumentFast(context, downloadUrl, item.fileType)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = Gold500,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "डाउनलोड (${item.fileType})", color = PureWhite, fontSize = 12.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewingResource = null }) {
                    Text(text = "बंद करें", color = Color(0xFF546E7A))
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = PureWhite
        )
    }
}

@Composable
private fun DownloadItemCard(
    item: DownloadResourceEntity,
    onClick: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit
) {
    val isJpg = item.fileType.equals("JPG", ignoreCase = true)
    ElevatedCard(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = PureWhite),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("download_item_${item.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // PDF or JPG Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(if (isJpg) Color(0xFFFFF3E0) else Color(0xFFFFEBEE), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isJpg) Icons.Default.Description else Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = if (isJpg) Color(0xFFE65100) else Color(0xFFC62828),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = if (isJpg) Color(0xFFFFF3E0) else Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = item.fileType.uppercase(),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isJpg) Color(0xFFE65100) else Color(0xFFC62828),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        color = Color(0xFFE8F0FE),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = item.category,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1967D2),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = item.fileSize,
                        fontSize = 10.sp,
                        color = Color(0xFF78909C)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy900,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.description,
                        fontSize = 11.sp,
                        color = Color(0xFF546E7A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

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

                IconButton(
                    onClick = onDownload,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Navy800, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Download",
                        tint = Gold500,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
