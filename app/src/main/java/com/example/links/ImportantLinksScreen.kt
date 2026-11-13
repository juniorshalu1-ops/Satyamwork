package com.example.links

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.LocalAppLanguage
import com.example.ui.theme.Gold100
import com.example.ui.theme.Gold500
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Teal700

data class ImportantLinkItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String, // "उपस्थिति व प्रबंधन", "छात्रवृत्ति व डीबीटी", "प्रशिक्षण व पाठ्यक्रम", "वेतन व वित्त", "परीक्षा व भर्ती"
    val url: String,
    val badge: String = "आधिकारिक पोर्टल"
)

@Composable
fun ImportantLinksScreen(
    repository: com.example.database.AppRepository? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    val isHi = language.isHindi()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("सभी") }

    val dbPortals = if (repository != null) {
        repository.allPortals.collectAsState(initial = emptyList()).value
    } else {
        emptyList()
    }

    val defaultList = remember {
        listOf(
            ImportantLinkItem(
                id = "eshikshakosh",
                title = "e-Shikshakosh (ई-शिक्षाकोष)",
                subtitle = "शिक्षकों एवं छात्रों की दैनिक ऑनलाइन उपस्थिति व विद्यालय डेटा पोर्टल",
                category = "उपस्थिति व प्रबंधन",
                url = "https://eshikshakosh.bihar.gov.in"
            ),
            ImportantLinkItem(
                id = "medhasoft",
                title = "Medhasoft (मेधासॉफ्ट)",
                subtitle = "डीबीटी, छात्रवृत्ति, पोशाक, साइकिल योजना एवं छात्र डेटा प्रविष्टि",
                category = "छात्रवृत्ति व डीबीटी",
                url = "https://medhasoft.bih.nic.in"
            ),
            ImportantLinkItem(
                id = "mdm_bihar",
                title = "PM POSHAN / MDM Bihar (मध्याह्न भोजन)",
                subtitle = "दैनिक एमडीएम रिपोर्टिंग, खाद्यान्न उठाव एवं मेनू प्रबंधन पोर्टल",
                category = "उपस्थिति व प्रबंधन",
                url = "https://mdm.bih.nic.in"
            ),
            ImportantLinkItem(
                id = "diksha",
                title = "DIKSHA Portal (दीक्षा)",
                subtitle = "एनसीईआरटी/एससीईआरटी डिजिटल पाठ्यपुस्तकें एवं शिक्षक ऑनलाइन प्रशिक्षण",
                category = "प्रशिक्षण व पाठ्यक्रम",
                url = "https://diksha.gov.in"
            ),
            ImportantLinkItem(
                id = "udise",
                title = "UDISE+ Portal (यूडाइस प्लस)",
                subtitle = "विद्यालय अधोसंरचना, शिक्षक व छात्र प्रोफाइल राष्ट्रीय शिक्षा पोर्टल",
                category = "उपस्थिति व प्रबंधन",
                url = "https://udiseplus.gov.in"
            ),
            ImportantLinkItem(
                id = "vsk",
                title = "Vidya Samiksha Kendra (VSK Bihar)",
                subtitle = "बिहार विद्यालय समीक्षा केंद्र - मॉनिटरिंग, सर्वेक्षण एवं विश्लेषण",
                category = "उपस्थिति व प्रबंधन",
                url = "https://vsk.bihar.gov.in"
            ),
            ImportantLinkItem(
                id = "apaar",
                title = "APAAR / One Nation One Student ID",
                subtitle = "छात्रों की 12-अंकों वाली आजीवन विशिष्ट शैक्षणिक आईडी जनरेशन पोर्टल",
                category = "उपस्थिति व प्रबंधन",
                url = "https://apaar.education.gov.in"
            ),
            ImportantLinkItem(
                id = "cfms",
                title = "CFMS Bihar (वेतन व बिलिंग)",
                subtitle = "व्यापक वित्तीय प्रबंधन प्रणाली, शिक्षक पे-स्लिप एवं ई-पेमेंट",
                category = "वेतन व वित्त",
                url = "https://cfms.bihar.gov.in"
            ),
            ImportantLinkItem(
                id = "hrms",
                title = "HRMS Bihar (मानव संपदा पोर्टल)",
                subtitle = "सेवा पुस्तिका, छुट्टी प्रबंधन, जीपीएफ एवं स्थानांतरण आवेदन",
                category = "वेतन व वित्त",
                url = "https://hrms.bihar.gov.in"
            ),
            ImportantLinkItem(
                id = "education_portal",
                title = "शिक्षा विभाग, बिहार सरकार (Official)",
                subtitle = "आधिकारिक अधिसूचनाएं, सर्कुलर, प्रेस विज्ञप्ति एवं विभागीय आदेश",
                category = "उपस्थिति व प्रबंधन",
                url = "https://state.bihar.gov.in/educationbihar"
            ),
            ImportantLinkItem(
                id = "bpsc",
                title = "BPSC (बिहार लोक सेवा आयोग)",
                subtitle = "शिक्षक नियुक्ति परीक्षा (TRE) विज्ञापन, परिणाम एवं कट-ऑफ",
                category = "परीक्षा व भर्ती",
                url = "https://www.bpsc.bih.nic.in"
            ),
            ImportantLinkItem(
                id = "bseb",
                title = "BSEB (बिहार विद्यालय परीक्षा समिति)",
                subtitle = "सक्षमता परीक्षा, STET, DElEd एवं बोर्ड परीक्षा आधिकारिक पोर्टल",
                category = "परीक्षा व भर्ती",
                url = "https://biharboardonline.bihar.gov.in"
            )
        )
    }

    val linkList = remember(dbPortals) {
        val mappedPortals = dbPortals.filter { it.isActive }.map { p ->
            ImportantLinkItem(
                id = p.id.toString(),
                title = p.title,
                subtitle = p.subtitle.ifBlank { p.badge },
                category = if (p.badge.isNotBlank()) p.badge else "आधिकारिक पोर्टल",
                url = p.url,
                badge = p.badge.ifBlank { "डिजिटल सेवा" }
            )
        }
        val remainingDefaults = defaultList.filter { def ->
            mappedPortals.none { it.title.equals(def.title, ignoreCase = true) || it.url.equals(def.url, ignoreCase = true) }
        }
        mappedPortals + remainingDefaults
    }

    val categories = remember(linkList) {
        val cats = linkedSetOf("सभी", "उपस्थिति व प्रबंधन", "छात्रवृत्ति व डीबीटी", "प्रशिक्षण व पाठ्यक्रम", "वेतन व वित्त", "परीक्षा व भर्ती")
        linkList.forEach { item ->
            if (item.category.isNotBlank()) cats.add(item.category)
        }
        cats.toList()
    }

    val filteredLinks = linkList.filter { item ->
        val matchesCategory = selectedCategory == "सभी" || item.category == selectedCategory
        val matchesQuery = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.subtitle.contains(searchQuery, ignoreCase = true) ||
                item.category.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesQuery
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6F9))
            .testTag("important_links_screen")
    ) {
        // Top Header
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
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = Navy900,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isHi) "सक्रिय डिजिटल सेवा" else "Active Digital Services (Portals)",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                        Text(
                            text = if (isHi) "ई-शिक्षाकोष, मेधासॉफ्ट, एमडीएम, दीक्षा, सीएफएमएस आधिकारिक डिजिटल पोर्टल" else "Official digital portals & teacher resources",
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
                            text = if (isHi) "पोर्टल या वेबसाइट खोजें..." else "Search portals & links...",
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
                        .testTag("links_search_input")
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

        // Links List
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredLinks, key = { it.id }) { item ->
                ElevatedCard(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = PureWhite),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                        .testTag("link_item_${item.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFFE8F0FE), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = Color(0xFF1967D2),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = Gold100,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = item.category,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Navy900,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = item.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = item.subtitle,
                                fontSize = 11.sp,
                                color = Color(0xFF546E7A),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, item.title)
                                    putExtra(Intent.EXTRA_TEXT, "${item.title}\n${item.subtitle}\n\nपोर्टल लिंक: ${item.url}\n\n'शिक्षक साथी' ऐप द्वारा साझा किया गया")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "लिंक शेयर करें"))
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = Color(0xFF546E7A),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Navy800, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open Portal",
                                tint = Gold500,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
