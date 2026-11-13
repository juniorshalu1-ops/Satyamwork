package com.example.documents

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ApplicationPdfGenerator
import com.example.core.LocalAppLanguage
import com.example.database.entities.DocumentDraftEntity
import com.example.ui.components.HeroBanner
import com.example.ui.theme.Gold700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite

@Composable
fun DocumentsScreen(
    viewModel: DocumentsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    val isHi = language.isHindi()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = if (isHi) {
        listOf("सरकारी प्रारूप (Templates)", "AI आवेदन लेखक", "संपादक (Editor)", "सहेजे गए आवेदन")
    } else {
        listOf("Templates (30+)", "AI Application Writer", "Editor", "Saved Drafts")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("documents_screen")
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
            0 -> TemplatesListTab(
                viewModel = viewModel,
                isHi = isHi,
                onTemplateSelected = { selectedTab = 2 }
            )
            1 -> AiApplicationWriterTab(
                viewModel = viewModel,
                isHi = isHi,
                onGenerated = { selectedTab = 2 }
            )
            2 -> DocumentEditorTab(viewModel = viewModel, isHi = isHi)
            3 -> SavedDraftsTab(
                viewModel = viewModel,
                isHi = isHi,
                onDraftSelected = { selectedTab = 2 }
            )
        }
    }
}

@Composable
fun TemplatesListTab(
    viewModel: DocumentsViewModel,
    isHi: Boolean,
    onTemplateSelected: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("सभी") }

    val categories = listOf("सभी", "अवकाश (Leaves)", "वेतन एवं सेवा (Salary & Service)", "प्रशासनिक (Administrative)", "स्थानांतरण (Transfer)")

    val filtered = OfficialTemplatesData.templates.filter { item ->
        val matchesCategory = selectedCategory == "सभी" || item.category == selectedCategory
        val matchesSearch = item.titleHi.contains(searchQuery, ignoreCase = true) ||
                item.titleEn.contains(searchQuery, ignoreCase = true) ||
                item.defaultSubjectHi.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            HeroBanner(
                title = if (isHi) "स्मार्ट आवेदन एवं सरकारी पत्र" else "Smart Documents & Letters",
                subtitle = if (isHi) "30+ आधिकारिक ऑफलाइन प्रारूप (अवकाश, वेतन, स्थानांतरण, अनापत्ति)" else "30+ Official Bihar Teacher Templates",
                icon = Icons.Default.Description
            )
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(if (isHi) "आवेदन पत्र खोजें (Search Templates)..." else "Search Templates...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )
        }

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

        items(filtered) { template ->
            ElevatedCard(
                onClick = {
                    viewModel.loadTemplate(template, isHi)
                    onTemplateSelected()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isHi) template.titleHi else template.titleEn,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "विषय: ${if (isHi) template.defaultSubjectHi else template.defaultSubjectEn}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.5.sp
                            ),
                            maxLines = 1
                        )
                    }
                    IconButton(onClick = {
                        viewModel.loadTemplate(template, isHi)
                        onTemplateSelected()
                    }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = Navy800)
                    }
                }
            }
        }
    }
}

@Composable
fun AiApplicationWriterTab(
    viewModel: DocumentsViewModel,
    isHi: Boolean,
    onGenerated: () -> Unit
) {
    var recipient by remember { mutableStateOf("प्रखंड शिक्षा पदाधिकारी / प्रधानाध्यापक") }
    var appType by remember { mutableStateOf("आकस्मिक अवकाश / वेतन / स्थानांतरण") }
    var reason by remember { mutableStateOf("") }
    var dates by remember { mutableStateOf("") }

    val isLoading by viewModel.isAiLoading.collectAsState()
    val notice by viewModel.aiStatusNotice.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HeroBanner(
                title = if (isHi) "AI आवेदन लेखक (AI Application Writer)" else "AI Application Writer",
                subtitle = if (isHi) "शिक्षा विभाग के लिए शुद्ध मानक प्रशासनिक प्रारूप में स्वतः आवेदन निर्माण" else "Automatic official application generator for teachers",
                badgeText = "Limited Official AI",
                icon = Icons.Default.AutoAwesome
            )
        }

        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHi) "आवेदन हेतु विवरण प्रविष्ट करें:" else "Enter Details:",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Navy800)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = recipient,
                        onValueChange = { recipient = it },
                        label = { Text(if (isHi) "प्राप्तकर्ता पदाधिकारी (Recipient, e.g. BEO / DEO / HM)" else "Recipient Authority") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = appType,
                        onValueChange = { appType = it },
                        label = { Text(if (isHi) "आवेदन का प्रकार (Type, e.g. अवकाश, वेतन, स्थानांतरण)" else "Application Type") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text(if (isHi) "मुख्य कारण / पृष्ठभूमि (Reason in simple words)" else "Reason") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = dates,
                        onValueChange = { dates = it },
                        label = { Text(if (isHi) "दिनांक / अवधि (Dates / Duration)" else "Dates") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.generateWithAi(reason, dates, appType, isHi)
                            onGenerated()
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("तैयार किया जा रहा है...")
                        } else {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Gold700, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isHi) "AI से आवेदन पत्र तैयार करें" else "Draft Application with AI")
                        }
                    }

                    if (notice.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = notice, fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentEditorTab(
    viewModel: DocumentsViewModel,
    isHi: Boolean
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var recipient by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    val activeTitle by viewModel.activeTitle.collectAsState()
    val activeRecipient by viewModel.activeRecipient.collectAsState()
    val activeSubject by viewModel.activeSubject.collectAsState()
    val activeBody by viewModel.activeBody.collectAsState()

    title = activeTitle
    recipient = activeRecipient
    subject = activeSubject
    body = activeBody

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHi) "आवेदन पत्र संपादक (Document Editor)" else "Application Editor",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Navy800)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            viewModel.activeTitle.value = it
                        },
                        label = { Text(if (isHi) "आवेदन शीर्षक (Title)" else "Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = recipient,
                        onValueChange = {
                            recipient = it
                            viewModel.activeRecipient.value = it
                        },
                        label = { Text(if (isHi) "सेवा में / To (Recipient Address)" else "Recipient") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = subject,
                        onValueChange = {
                            subject = it
                            viewModel.activeSubject.value = it
                        },
                        label = { Text(if (isHi) "विषय (Subject)" else "Subject") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = body,
                        onValueChange = {
                            body = it
                            viewModel.activeBody.value = it
                        },
                        label = { Text(if (isHi) "आवेदन का मुख्य भाग (Body Text)" else "Body") },
                        minLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action buttons: Copy, Save, Share PDF
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val fullText = "सेवा में,\n$recipient\n\nविषय: $subject\n\n$body"
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Application", fullText))
                                Toast.makeText(context, if (isHi) "टेक्स्ट कॉपी किया गया" else "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isHi) "कॉपी" else "Copy")
                        }

                        Button(
                            onClick = {
                                viewModel.saveCurrentDraft()
                                Toast.makeText(context, if (isHi) "ड्राफ्ट सहेजा गया" else "Draft saved", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isHi) "सहेजें" else "Save")
                        }

                        Button(
                            onClick = {
                                try {
                                    val pdfFile = viewModel.exportPdf(context)
                                    ApplicationPdfGenerator.sharePdf(context, pdfFile)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1.3f),
                            colors = ButtonDefaults.buttonColors(containerColor = Gold700)
                        ) {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, tint = Navy900, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isHi) "PDF शेयर" else "Share PDF", color = Navy900, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SavedDraftsTab(
    viewModel: DocumentsViewModel,
    isHi: Boolean,
    onDraftSelected: () -> Unit
) {
    val drafts by viewModel.drafts.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (drafts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.Description, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = if (isHi) "कोई सहेजा गया ड्राफ्ट नहीं है" else "No saved drafts yet", fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isHi) "संपादक में 'सहेजें' बटन दबाकर आवेदन यहां सुरक्षित करें।" else "Save drafts in editor to view here.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        } else {
            items(drafts) { draft ->
                ElevatedCard(
                    onClick = {
                        viewModel.loadDraft(draft)
                        onDraftSelected()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = draft.title, fontWeight = FontWeight.Bold)
                            Text(
                                text = "विषय: ${draft.subject}",
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "दिनांक: ${draft.dateStr}",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                        IconButton(onClick = { viewModel.deleteDraft(draft) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
