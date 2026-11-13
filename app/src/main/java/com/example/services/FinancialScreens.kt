package com.example.services

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.database.entities.ComplaintOptionEntity
import com.example.database.entities.FinancialServiceEntity
import com.example.home.HomeViewModel
import com.example.ui.theme.Gold100
import com.example.ui.theme.Gold500
import com.example.ui.theme.Gold700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Teal700

@Composable
fun ComplaintsScreen(
    onBack: () -> Unit,
    homeViewModel: HomeViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val complaintOptions by homeViewModel.complaintOptions.collectAsState()

    fun dialPhone(phone: String) {
        if (phone.isBlank()) return
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.trim()}"))
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun sendEmail(email: String) {
        if (email.isBlank()) return
        try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${email.trim()}"))
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun openUrl(url: String) {
        if (url.isBlank()) return
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url.trim()))
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6F9))
            .testTag("complaints_screen")
    ) {
        ServiceHeader(
            title = "शिकायत एवं परिवाद निवारण",
            subtitle = "ऑनलाइन परिवाद, लोक शिकायत व विभागीय संपर्क",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Introductory Info Card
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F0FE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "बिहार लोक शिकायत निवारण अधिकार (BPGORS)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Navy900
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "वेतन भुगतान, सेवा संपुष्टि, मातृत्व अवकाश अथवा विभागीय समस्याओं के समाधान हेतु आधिकारिक हेल्पलाइन व ऑनलाइन पोर्टल।",
                            fontSize = 12.sp,
                            color = Color(0xFF37474F)
                        )
                    }
                }
            }

            item {
                Text(
                    text = "सक्रिय शिकायत एवं परिवाद निवारण विकल्प:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = Navy900
                )
            }

            val visibleOptions = complaintOptions.filter { it.isVisible }.sortedBy { it.displayOrder }
            val displayList = if (visibleOptions.isNotEmpty()) visibleOptions else com.example.database.entities.DEFAULT_COMPLAINT_OPTIONS

            items(displayList, key = { it.id }) { option ->
                ComplaintCard(
                    option = option,
                    onDial = { dialPhone(option.phone) },
                    onEmail = { sendEmail(option.email) },
                    onOpenLink = { openUrl(option.linkUrl) }
                )
            }
        }
    }
}

@Composable
fun ComplaintCard(
    option: ComplaintOptionEntity,
    onDial: () -> Unit,
    onEmail: () -> Unit,
    onOpenLink: () -> Unit
) {
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
                        .size(36.dp)
                        .background(Color(0xFFE8F0FE), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SupportAgent,
                        contentDescription = null,
                        tint = Color(0xFF1967D2),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = option.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = Navy900
                    )
                }
            }

            if (option.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = option.description,
                    fontSize = 12.sp,
                    color = Color(0xFF455A64)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons (Call, Email, Open Link)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (option.phone.isNotBlank()) {
                    Button(
                        onClick = onDial,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(15.dp), tint = PureWhite)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "कॉल करें", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (option.email.isNotBlank()) {
                    OutlinedButton(
                        onClick = onEmail,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Email, contentDescription = null, modifier = Modifier.size(15.dp), tint = Navy800)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "ईमेल", fontSize = 11.5.sp, color = Navy800, fontWeight = FontWeight.Bold)
                    }
                }

                if (option.linkUrl.isNotBlank()) {
                    Button(
                        onClick = onOpenLink,
                        colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp), tint = Gold500)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "पोर्टल खोलें", fontSize = 11.5.sp, color = PureWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LoanScreen(
    onBack: () -> Unit,
    homeViewModel: HomeViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val loanServices by homeViewModel.loanServices.collectAsState()

    fun openUrl(url: String) {
        if (url.isBlank()) return
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url.trim()))
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6F9))
            .testTag("loan_screen")
    ) {
        ServiceHeader(
            title = "शिक्षकों के लिए पर्सनल लोन सुविधा",
            subtitle = "कम ब्याज दर एवं न्यूनतम दस्तावेज",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Navy900),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Surface(color = Gold500, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "विशेष शिक्षक दरें",
                                color = Navy900,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "वेतनभोगी शिक्षकों हेतु ₹50,000 से ₹15,00,000 तक ऋण",
                            color = PureWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "विभिन्न राष्ट्रीयकृत बैंकों (SBI, PNB, BOB, Canara आदि) के शिक्षक ऋण विकल्पों की तुलना एवं परामर्श।",
                            color = Color(0xFFA5B8CC),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            val visibleLoans = loanServices.filter { it.isVisible }.sortedBy { it.displayOrder }
            val displayLoans = if (visibleLoans.isNotEmpty()) visibleLoans else com.example.database.entities.DEFAULT_FINANCIAL_SERVICES.filter { it.category == "LOAN" }

            items(displayLoans, key = { it.id }) { item ->
                FinancialServiceCard(
                    service = item,
                    icon = Icons.Default.MonetizationOn,
                    onApply = { openUrl(item.applyUrl) }
                )
            }

            item {
                Text(
                    text = "*अस्वीकरण: यह अनुभाग केवल सूचनात्मक है। ऋण स्वीकृति एवं अंतिम ब्याज दरें बैंकों के क्रेडिट स्कोर एवं सेवा शर्तों पर निर्भर करती हैं।",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
fun CreditCardScreen(
    onBack: () -> Unit,
    homeViewModel: HomeViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cardServices by homeViewModel.creditCardServices.collectAsState()

    fun openUrl(url: String) {
        if (url.isBlank()) return
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url.trim()))
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6F9))
            .testTag("credit_card_screen")
    ) {
        ServiceHeader(
            title = "शिक्षक क्रेडिट कार्ड विकल्प",
            subtitle = "लाइफटाइम फ्री व रिवार्ड कार्ड",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = PureWhite),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(38.dp).background(Gold100, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.CreditCard, contentDescription = null, tint = Navy900, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = "लाइफटाइम फ्री (No Annual Fee) क्रेडिट कार्ड", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Navy900)
                                Text(text = "पेट्रोल, बिल भुगतान एवं ग्रोसरी पर 5% तक कैशबैक", fontSize = 11.5.sp, color = Teal700)
                            }
                        }
                    }
                }
            }

            val visibleCards = cardServices.filter { it.isVisible }.sortedBy { it.displayOrder }
            val displayCards = if (visibleCards.isNotEmpty()) visibleCards else com.example.database.entities.DEFAULT_FINANCIAL_SERVICES.filter { it.category == "CREDIT_CARD" }

            items(displayCards, key = { it.id }) { item ->
                FinancialServiceCard(
                    service = item,
                    icon = Icons.Default.CreditCard,
                    onApply = { openUrl(item.applyUrl) }
                )
            }
        }
    }
}

@Composable
fun InvestmentScreen(
    onBack: () -> Unit,
    homeViewModel: HomeViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val investmentServices by homeViewModel.investmentServices.collectAsState()

    fun openUrl(url: String) {
        if (url.isBlank()) return
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url.trim()))
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6F9))
            .testTag("investment_screen")
    ) {
        ServiceHeader(
            title = "शिक्षक निवेश एवं पेंशन योजनाएं",
            subtitle = "NPS, GPF, PPF व सुरक्षित बचत",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Navy800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "सेवानिवृत्ति एवं भविष्य सुरक्षा", color = Gold500, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "शिक्षकों के लिए प्रमाणित व सुरक्षित सरकारी बचत विकल्प", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            val visibleInvestments = investmentServices.filter { it.isVisible }.sortedBy { it.displayOrder }
            val displayInvestments = if (visibleInvestments.isNotEmpty()) visibleInvestments else com.example.database.entities.DEFAULT_FINANCIAL_SERVICES.filter { it.category == "INVESTMENT" }

            items(displayInvestments, key = { it.id }) { item ->
                FinancialServiceCard(
                    service = item,
                    icon = Icons.Default.TrendingUp,
                    onApply = { openUrl(item.applyUrl) }
                )
            }
        }
    }
}

@Composable
fun FinancialServiceCard(
    service: FinancialServiceEntity,
    icon: ImageVector,
    onApply: () -> Unit
) {
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
                        .size(34.dp)
                        .background(Color(0xFFE8F0FE), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = Navy800, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = service.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = Navy900
                    )
                    if (service.isGoogleForm) {
                        Surface(color = Gold100, shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = "Google Form सहायता",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy900,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            if (service.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = service.description,
                    fontSize = 12.sp,
                    color = Color(0xFF455A64)
                )
            }

            if (service.applyUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onApply,
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (service.isGoogleForm) Icons.Default.Link else Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = Gold500
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (service.buttonText.isNotBlank()) service.buttonText else "आवेदन करें / विवरण देखें",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ServiceHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Surface(color = Navy900, tonalElevation = 4.dp) {
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
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                Text(text = subtitle, fontSize = 11.sp, color = Color(0xFFA5B8CC))
            }
        }
    }
}
