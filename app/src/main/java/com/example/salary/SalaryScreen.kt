package com.example.salary

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.DateUtils
import com.example.core.LocalAppLanguage
import com.example.core.NumberToWords
import com.example.core.SalaryPdfGenerator
import com.example.database.entities.SalaryCalculatorHelper
import com.example.ui.components.HeroBanner
import com.example.ui.theme.Gold100
import com.example.ui.theme.Gold700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite

@Composable
fun SalaryScreen(
    viewModel: SalaryViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    val isHi = language.isHindi()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = if (isHi) {
        listOf("वेतन कैलकुलेटर", "DA एरियर", "वेतन वृद्धि (3%)", "पे रिवीजन", "भविष्य अनुमान", "वेतन इतिहास")
    } else {
        listOf("Calculator", "DA Arrear", "Increment", "Pay Revision", "Future Estimate", "History")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("salary_screen")
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
            0 -> SalaryCalculatorTab(viewModel, isHi)
            1 -> DaArrearTab(viewModel, isHi)
            2 -> IncrementCalculatorTab(viewModel, isHi)
            3 -> PayRevisionTab(viewModel, isHi)
            4 -> FutureSalaryTab(viewModel, isHi)
            5 -> SalaryHistoryTab(viewModel, isHi)
        }
    }
}

@Composable
fun SalaryCalculatorTab(viewModel: SalaryViewModel, isHi: Boolean) {
    val context = LocalContext.current
    val basicPay by viewModel.basicPayInput.collectAsState()
    val daPercent by viewModel.daPercentInput.collectAsState()
    val hraCategory by viewModel.hraCategoryInput.collectAsState()
    val medicalAllowance by viewModel.medicalAllowanceInput.collectAsState()
    val otherAllowance by viewModel.otherAllowanceInput.collectAsState()
    val isNps by viewModel.isNpsInput.collectAsState()
    val gis by viewModel.gisInput.collectAsState()
    val incomeTax by viewModel.incomeTaxInput.collectAsState()
    val otherDeduction by viewModel.otherDeductionInput.collectAsState()
    val result by viewModel.calculationResult.collectAsState()

    var daDropdownExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Summary Card with Net Pay
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Navy900),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("net_salary_summary_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = if (isHi) "मासिक शुद्ध देय वेतन (Net Payable Salary)" else "Net Monthly Payable Salary",
                        color = Color(0xFFD6E4F0),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹%,.2f".format(result.netSalary),
                        color = Gold100,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "अक्षरे: ${NumberToWords.toHindiWords(result.netSalary.toLong())}",
                        color = PureWhite.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (isHi) "सकल वेतन (Gross)" else "Gross Salary",
                                color = Color(0xFFA5B8CC),
                                fontSize = 11.sp
                            )
                            Text(
                                text = "₹%,.2f".format(result.grossSalary),
                                color = PureWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isHi) "कुल कटौती (Deductions)" else "Total Deductions",
                                color = Color(0xFFA5B8CC),
                                fontSize = 11.sp
                            )
                            Text(
                                text = "₹%,.2f".format(result.totalDeductions),
                                color = Color(0xFFFF8A80),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Action Buttons: Save & Export PDF
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.saveCurrentCalculation("माहवार वेतन गणना")
                        Toast.makeText(context, if (isHi) "गणना इतिहास में सहेजी गई" else "Saved to History", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_salary_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (isHi) "सहेजें" else "Save")
                }

                Button(
                    onClick = {
                        try {
                            val pdfFile = viewModel.exportSalaryPdf(context)
                            SalaryPdfGenerator.sharePdf(context, pdfFile)
                        } catch (e: Exception) {
                            Toast.makeText(context, "PDF तैयार करने में त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("export_salary_pdf_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold700)
                ) {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, tint = Navy900, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (isHi) "PDF स्टेटमेंट शेयर करें" else "Share PDF", color = Navy900, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Inputs Section
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHi) "वेतन विवरण प्रविष्टि (Salary Inputs)" else "Enter Salary Details",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Navy800)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Basic Pay
                    OutlinedTextField(
                        value = basicPay,
                        onValueChange = {
                            viewModel.basicPayInput.value = it
                            viewModel.recalculate()
                        },
                        label = { Text(if (isHi) "मूल वेतन (Basic Pay ₹)" else "Basic Pay (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_basic_pay")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // DA Percentage with Dropdown
                    Text(
                        text = if (isHi) "महंगाई भत्ता (DA %) - ड्रॉपडाउन से चुनें या टाइप करें:" else "Dearness Allowance (DA %):",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = daPercent,
                            onValueChange = {
                                viewModel.daPercentInput.value = it
                                viewModel.recalculate()
                            },
                            label = { Text(if (isHi) "महंगाई भत्ता प्रतिशत (DA %)" else "DA Percent (%)") },
                            trailingIcon = {
                                IconButton(
                                    onClick = { daDropdownExpanded = true },
                                    modifier = Modifier.testTag("da_dropdown_button")
                                ) {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "DA Dropdown", tint = Navy800)
                                }
                            },
                            supportingText = { Text(if (isHi) "ड्रॉपडाउन से कोई भी प्रतिशत चुनें या टाइप करें (उदा० 53, 60, 63, 66, 69%)" else "Choose from dropdown or enter custom DA rate") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_da_percent")
                        )
                        DropdownMenu(
                            expanded = daDropdownExpanded,
                            onDismissRequest = { daDropdownExpanded = false }
                        ) {
                            listOf("50", "53", "55", "58", "60", "63", "66", "69", "72").forEach { percent ->
                                DropdownMenuItem(
                                    text = { Text("$percent% DA") },
                                    onClick = {
                                        viewModel.daPercentInput.value = percent
                                        viewModel.recalculate()
                                        daDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("53%", "60%", "63%", "66%", "69%").forEach { percentStr ->
                            val cleanVal = percentStr.replace("%", "")
                            FilterChip(
                                selected = daPercent == cleanVal,
                                onClick = {
                                    viewModel.daPercentInput.value = cleanVal
                                    viewModel.recalculate()
                                },
                                label = { Text(percentStr, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Navy800,
                                    selectedLabelColor = PureWhite
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // HRA Category Selector (5%, 7.5%, 10%, 20%)
                    Text(
                        text = if (isHi) "मकान किराया भत्ता श्रेणी (HRA Percent):" else "HRA Percent:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("5%", "7.5%", "10%", "20%").forEach { cat ->
                            FilterChip(
                                selected = hraCategory == cat,
                                onClick = {
                                    viewModel.hraCategoryInput.value = cat
                                    viewModel.recalculate()
                                },
                                label = {
                                    Text(
                                        when (cat) {
                                            "20%" -> "20% (पटना)"
                                            "10%" -> "10% (नगर/जिला)"
                                            "7.5%" -> "7.5% (कस्बा)"
                                            else -> "5% (ग्रामीण)"
                                        },
                                        fontSize = 11.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Navy800,
                                    selectedLabelColor = PureWhite
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Medical Allowance
                    OutlinedTextField(
                        value = medicalAllowance,
                        onValueChange = {
                            viewModel.medicalAllowanceInput.value = it
                            viewModel.recalculate()
                        },
                        label = { Text(if (isHi) "चिकित्सा भत्ता (Medical ₹)" else "Medical Allowance (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // NPS / GPF Choice
                    Text(
                        text = if (isHi) "पेंशन योजना (Pension Scheme):" else "Pension Scheme:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = isNps,
                            onClick = {
                                viewModel.isNpsInput.value = true
                                viewModel.recalculate()
                            },
                            label = { Text(if (isHi) "NPS (10% Basic+DA)" else "NPS (10%)") }
                        )
                        FilterChip(
                            selected = !isNps,
                            onClick = {
                                viewModel.isNpsInput.value = false
                                viewModel.recalculate()
                            },
                            label = { Text(if (isHi) "GPF (सामान्य भविष्य निधि)" else "GPF") }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Income Tax & GIS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = gis,
                            onValueChange = {
                                viewModel.gisInput.value = it
                                viewModel.recalculate()
                            },
                            label = { Text(if (isHi) "GIS (₹)" else "GIS (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = incomeTax,
                            onValueChange = {
                                viewModel.incomeTaxInput.value = it
                                viewModel.recalculate()
                            },
                            label = { Text(if (isHi) "आयकर (TDS ₹)" else "Income Tax (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Detailed Breakdown Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHi) "वेतन मदवार विस्तृत विवरण (Breakdown)" else "Itemized Salary Breakdown",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Navy800)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    BreakdownRow(if (isHi) "मूल वेतन (Basic Pay)" else "Basic Pay", result.basicPay, Color(0xFF1B5E20))
                    BreakdownRow(if (isHi) "महंगाई भत्ता (DA @ ${result.daPercent.toInt()}%)" else "DA (${result.daPercent.toInt()}%)", result.daAmount, Color(0xFF1B5E20))
                    BreakdownRow(if (isHi) "मकान किराया (HRA @ ${result.hraPercent.toInt()}%)" else "HRA (${result.hraPercent.toInt()}%)", result.hraAmount, Color(0xFF1B5E20))
                    BreakdownRow(if (isHi) "चिकित्सा भत्ता (Medical)" else "Medical", result.medicalAllowance, Color(0xFF1B5E20))
                    if (result.otherAllowance > 0) {
                        BreakdownRow(if (isHi) "अन्य भत्ते" else "Other Allowance", result.otherAllowance, Color(0xFF1B5E20))
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE0E0E0)))
                    Spacer(modifier = Modifier.height(6.dp))

                    BreakdownRow(
                        if (result.isNps) "NPS अंशदान (10%)" else "GPF अंशदान",
                        result.npsGpfDeduction,
                        Color(0xFFC62828),
                        isDeduction = true
                    )
                    BreakdownRow("GIS सामूहिक बीमा", result.gisDeduction, Color(0xFFC62828), isDeduction = true)
                    if (result.incomeTaxDeduction > 0) {
                        BreakdownRow("आयकर कटौती (TDS)", result.incomeTaxDeduction, Color(0xFFC62828), isDeduction = true)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE0E0E0)))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isHi) "शुद्ध भुगतान (Net Salary):" else "Net Salary:",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "₹%,.2f".format(result.netSalary),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                        )
                    }
                }
            }
        }

        // Mandatory Disclaimer Card
        item {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "⚠️ " + SalaryCalculatorHelper.SALARY_DISCLAIMER_HI,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFB78103),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = SalaryCalculatorHelper.SALARY_DISCLAIMER_EN,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF795548),
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun BreakdownRow(label: String, amount: Double, color: Color, isDeduction: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        Text(
            text = "${if (isDeduction) "-" else "+"} ₹%,.2f".format(amount),
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
    }
}

@Composable
fun DaArrearTab(viewModel: SalaryViewModel, isHi: Boolean) {
    val basic by viewModel.arrearBasicInput.collectAsState()
    val oldDa by viewModel.arrearOldDaInput.collectAsState()
    val newDa by viewModel.arrearNewDaInput.collectAsState()
    val months by viewModel.arrearMonthsInput.collectAsState()
    val totalArrear by viewModel.arrearTotalResult.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HeroBanner(
                title = if (isHi) "DA एरियर कैलकुलेटर" else "DA Arrear Calculator",
                subtitle = if (isHi) "DA वृद्धि उपरांत बकाया एरियर राशि की सटीक गणना" else "Calculate pending DA arrears accurately",
                icon = Icons.Default.TrendingUp
            )
        }

        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = basic,
                        onValueChange = { viewModel.arrearBasicInput.value = it },
                        label = { Text(if (isHi) "मूल वेतन (Basic Pay ₹)" else "Basic Pay (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = oldDa,
                            onValueChange = { viewModel.arrearOldDaInput.value = it },
                            label = { Text("पूर्व DA (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newDa,
                            onValueChange = { viewModel.arrearNewDaInput.value = it },
                            label = { Text("नवीन DA (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = months,
                        onValueChange = { viewModel.arrearMonthsInput.value = it },
                        label = { Text(if (isHi) "बकाया माह संख्या (Number of Months)" else "Months") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { viewModel.calculateDaArrear() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                    ) {
                        Text(if (isHi) "एरियर राशि की गणना करें" else "Calculate Arrear")
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Navy900),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHi) "कुल देय DA एरियर राशि:" else "Total Payable DA Arrear:",
                        color = Color(0xFFD6E4F0),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "₹%,.2f".format(totalArrear),
                        color = Gold100,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "अक्षरे: ${NumberToWords.toHindiWords(totalArrear.toLong())}",
                        color = PureWhite.copy(alpha = 0.85f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun IncrementCalculatorTab(viewModel: SalaryViewModel, isHi: Boolean) {
    val currentBasic by viewModel.incCurrentBasicInput.collectAsState()
    val newBasic by viewModel.incResultNewBasic.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HeroBanner(
                title = if (isHi) "वार्षिक वेतन वृद्धि (Increment Calculator)" else "Annual Increment Calculator",
                subtitle = if (isHi) "3% वार्षिक वृद्धि अनुसार 7वें वेतनमान में नवीन मूल वेतन" else "Calculate new Basic Pay after 3% annual increment",
                icon = Icons.Default.TrendingUp
            )
        }

        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = currentBasic,
                        onValueChange = { viewModel.incCurrentBasicInput.value = it },
                        label = { Text(if (isHi) "वर्तमान मूल वेतन (Current Basic Pay ₹)" else "Current Basic Pay") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { viewModel.calculateIncrement() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                    ) {
                        Text(if (isHi) "वृद्धि उपरांत वेतन ज्ञात करें" else "Calculate New Basic")
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHi) "वृद्धि उपरांत नवीन अनुमानित मूल वेतन (New Basic):" else "New Basic Pay after Increment:",
                        color = PureWhite.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹%,.0f".format(newBasic),
                        color = PureWhite,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "अक्षरे: ${NumberToWords.toHindiWords(newBasic.toLong())}",
                        color = PureWhite.copy(alpha = 0.9f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PayRevisionTab(viewModel: SalaryViewModel, isHi: Boolean) {
    val preBasic by viewModel.preRevisionBasicInput.collectAsState()
    val revised by viewModel.revisedBasicResult.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HeroBanner(
                title = if (isHi) "वेतन पुनरीक्षण (Pay Revision Fitment)" else "Pay Revision Calculator",
                subtitle = if (isHi) "7वें वेतन आयोग का फिटमेंट फैक्टर 2.57 समंजन" else "Fitment Factor 2.57 Revision Calculator",
                icon = Icons.Default.Calculate
            )
        }

        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = preBasic,
                        onValueChange = { viewModel.preRevisionBasicInput.value = it },
                        label = { Text(if (isHi) "पूर्व वेतनमान मूल वेतन (₹)" else "Pre-revised Basic") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { viewModel.calculatePayRevision() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                    ) {
                        Text(if (isHi) "पुनरीक्षित वेतन ज्ञात करें (2.57 Factor)" else "Calculate Revised Basic")
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Navy900),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHi) "पुनरीक्षित मूल वेतन (Revised Basic):" else "Revised Basic Pay:",
                        color = Color(0xFFD6E4F0),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹%,.0f".format(revised),
                        color = Gold100,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
fun FutureSalaryTab(viewModel: SalaryViewModel, isHi: Boolean) {
    val years by viewModel.futureYearsInput.collectAsState()
    val estimatedNet by viewModel.futureEstimatedNet.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HeroBanner(
                title = if (isHi) "भविष्य वेतन अनुमानक" else "Future Salary Estimator",
                subtitle = if (isHi) "आगामी वर्षों में इंक्रीमेंट एवं DA वृद्धि के आधार पर शुद्ध वेतन" else "Projected net salary with increments and DA increases",
                icon = Icons.Default.TrendingUp
            )
        }

        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = years,
                        onValueChange = { viewModel.futureYearsInput.value = it },
                        label = { Text(if (isHi) "आगामी वर्ष संख्या (e.g. 1 to 5 वर्ष)" else "Years Ahead (1 to 5)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { viewModel.calculateFutureSalary() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                    ) {
                        Text(if (isHi) "भविष्य वेतन का अनुमान लगाएं" else "Estimate Future Salary")
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B3B6F)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHi) "$years वर्ष पश्चात अनुमानित शुद्ध वेतन (Net Salary):" else "Estimated Net Salary after $years years:",
                        color = PureWhite.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (estimatedNet > 0) "₹%,.2f".format(estimatedNet) else "कृपया अनुमान बटन दबाएं",
                        color = Gold100,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
fun SalaryHistoryTab(viewModel: SalaryViewModel, isHi: Boolean) {
    val history by viewModel.salaryHistory.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (history.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.History, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isHi) "कोई पूर्व वेतन गणना उपलब्ध नहीं है" else "No saved salary history yet",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isHi) "कैलकुलेटर में 'सहेजें' बटन दबाकर वेतन पर्चियां यहां सुरक्षित करें।" else "Use 'Save' in calculator to keep history records here.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        } else {
            items(history) { item ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "माह: ${DateUtils.getMonthName(item.month, isHi)} ${item.year}",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "मूल: ₹${item.basicPay.toInt()} | DA: ${item.daPercent.toInt()}% | HRA: ${item.hraCategory}",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "शुद्ध देय: ₹%,.2f".format(item.netSalary),
                                color = Color(0xFF1B5E20),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { viewModel.deleteRecord(item) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
