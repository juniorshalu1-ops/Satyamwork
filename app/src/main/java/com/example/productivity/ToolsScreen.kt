package com.example.productivity

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.LocalAppLanguage
import com.example.database.entities.FeatureBoxEntity
import com.example.home.HomeViewModel
import com.example.ui.theme.Gold100
import com.example.ui.theme.Gold500
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Teal700

@Composable
fun ToolsScreen(
    homeViewModel: HomeViewModel,
    productivityViewModel: ProductivityViewModel,
    onFeatureClick: (FeatureBoxEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val language = LocalAppLanguage.current
    val isHi = language.isHindi()
    val featureBoxes by homeViewModel.featureBoxes.collectAsState()
    val whatsAppBanner by homeViewModel.whatsAppBanner.collectAsState()
    val rawBoxes = if (featureBoxes.isNotEmpty()) featureBoxes else com.example.database.entities.DEFAULT_FEATURE_BOXES
    val effectiveBoxes = rawBoxes.map { box ->
        when (box.id.lowercase()) {
            "important_links", "links" -> box.copy(
                title = if (isHi) "सक्रिय डिजिटल सेवा" else "Digital Services (Portals)",
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6F9))
            .testTag("tools_screen")
    ) {
        // Tools Header
        Surface(
            color = Navy900,
            tonalElevation = 4.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Gold500, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = null,
                        tint = Navy900,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isHi) "मुख्य शिक्षक सुविधाएं एवं टूल्स" else "Teacher Tools & Facilities",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                    Text(
                        text = if (isHi) "अवकाश, पत्र, वेतन, डाउनलोड एवं उपयोगी सेवाएं" else "All 14 Core Teacher Services",
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
            item {
                Text(
                    text = if (isHi) "मुख्य शिक्षक सुविधाएं (${visibleBoxes.size} सेवाएं):" else "Core Teacher Features (${visibleBoxes.size} Services):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Navy900
                )
            }

            // 14 Feature Boxes Grid (2 columns)
            val chunked = visibleBoxes.chunked(2)
            items(chunked.size) { index ->
                val row = chunked[index]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val first = row[0]
                    Box(modifier = Modifier.weight(1f)) {
                        FeatureBoxCard(box = first, onClick = { onFeatureClick(first) })
                    }

                    if (row.size > 1) {
                        val second = row[1]
                        Box(modifier = Modifier.weight(1f)) {
                            FeatureBoxCard(box = second, onClick = { onFeatureClick(second) })
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun FeatureBoxCard(
    box: FeatureBoxEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = PureWhite),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("feature_box_${box.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(getFeatureBoxBgColor(box.id), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getFeatureBoxIcon(box.iconType, box.id),
                        contentDescription = null,
                        tint = getFeatureBoxTint(box.id),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "${box.displayOrder}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF90A4AE)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = box.title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = Navy900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (box.subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = box.subtitle,
                    fontSize = 10.5.sp,
                    color = Color(0xFF546E7A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

fun getFeatureBoxIcon(iconType: String, boxId: String): ImageVector {
    return when (iconType) {
        "CalendarMonth" -> Icons.Default.CalendarMonth
        "Link" -> Icons.Default.Link
        "Description" -> Icons.Default.Description
        "Newspaper" -> Icons.Default.Newspaper
        "Campaign" -> Icons.Default.Campaign
        "VideoLibrary" -> Icons.Default.VideoLibrary
        "AccountBalance" -> Icons.Default.AccountBalance
        "Download" -> Icons.Default.Download
        "EditDocument", "Edit" -> Icons.Default.Edit
        "SupportAgent" -> Icons.Default.SupportAgent
        "MonetizationOn" -> Icons.Default.MonetizationOn
        "CreditCard" -> Icons.Default.CreditCard
        "TrendingUp" -> Icons.Default.TrendingUp
        "Chat" -> Icons.Default.Chat
        else -> when (boxId) {
            "leave_holidays" -> Icons.Default.CalendarMonth
            "important_links" -> Icons.Default.Link
            "letters_orders" -> Icons.Default.Description
            "news_updates" -> Icons.Default.Newspaper
            "current_activities" -> Icons.Default.Campaign
            "videos" -> Icons.Default.VideoLibrary
            "salary" -> Icons.Default.AccountBalance
            "downloads" -> Icons.Default.Download
            "application_formats" -> Icons.Default.Edit
            "complaints" -> Icons.Default.SupportAgent
            "loan" -> Icons.Default.MonetizationOn
            "credit_card" -> Icons.Default.CreditCard
            "investment" -> Icons.Default.TrendingUp
            "whatsapp_channel" -> Icons.Default.Chat
            else -> Icons.Default.Widgets
        }
    }
}

fun getFeatureBoxBgColor(boxId: String): Color {
    return when (boxId) {
        "leave_holidays" -> Color(0xFFFFF3E0)
        "important_links" -> Color(0xFFE8F0FE)
        "letters_orders" -> Color(0xFFE0F2F1)
        "news_updates" -> Color(0xFFFFEBEE)
        "current_activities" -> Color(0xFFF3E5F5)
        "videos" -> Color(0xFFFFEBEE)
        "salary" -> Color(0xFFE8F5E9)
        "downloads" -> Color(0xFFE1F5FE)
        "application_formats" -> Color(0xFFFFFDE7)
        "complaints" -> Color(0xFFFFE0B2)
        "loan" -> Color(0xFFE8F5E9)
        "credit_card" -> Color(0xFFEDE7F6)
        "investment" -> Color(0xFFE0F7FA)
        "whatsapp_channel" -> Color(0xFFE8F5E9)
        else -> Color(0xFFF1F5F9)
    }
}

fun getFeatureBoxTint(boxId: String): Color {
    return when (boxId) {
        "leave_holidays" -> Color(0xFFE65100)
        "important_links" -> Color(0xFF1967D2)
        "letters_orders" -> Color(0xFF00796B)
        "news_updates" -> Color(0xFFC62828)
        "current_activities" -> Color(0xFF7B1FA2)
        "videos" -> Color(0xFFD32F2F)
        "salary" -> Color(0xFF2E7D32)
        "downloads" -> Color(0xFF0277BD)
        "application_formats" -> Color(0xFFF57F17)
        "complaints" -> Color(0xFFEF6C00)
        "loan" -> Color(0xFF1B5E20)
        "credit_card" -> Color(0xFF512DA8)
        "investment" -> Color(0xFF00695C)
        "whatsapp_channel" -> Color(0xFF25D366)
        else -> Navy800
    }
}
