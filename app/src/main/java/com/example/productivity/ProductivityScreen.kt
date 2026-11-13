package com.example.productivity

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.LocalAppLanguage
import com.example.ui.components.HeroBanner
import com.example.ui.theme.Gold100
import com.example.ui.theme.Gold700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite

@Composable
fun ProductivityScreen(
    viewModel: ProductivityViewModel,
    modifier: Modifier = Modifier
) {
    val language = LocalAppLanguage.current
    val isHi = language.isHindi()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = if (isHi) {
        listOf("संख्या से शब्द", "उम्र व सेवा गणना", "शिक्षक ज्ञान केंद्र", "दैनिक नोट्स", "कार्य सूची (Tasks)")
    } else {
        listOf("Number to Words", "Age & Service", "Knowledge Hub", "Notes", "Tasks")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("productivity_screen")
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
            0 -> NumberToWordsTab(viewModel, isHi)
            1 -> AgeAndServiceTab(viewModel, isHi)
            2 -> KnowledgeHubTab(isHi)
            3 -> NotesTab(viewModel, isHi)
            4 -> TasksTab(viewModel, isHi)
        }
    }
}

@Composable
fun NumberToWordsTab(viewModel: ProductivityViewModel, isHi: Boolean) {
    val number by viewModel.numberInput.collectAsState()
    val hindiResult by viewModel.hindiWordsResult.collectAsState()
    val englishResult by viewModel.englishWordsResult.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HeroBanner(
                title = if (isHi) "संख्या से शब्द रूपांतरण" else "Number to Words Converter",
                subtitle = if (isHi) "वेतन विपत्र, चेक एवं रसीद हेतु शुद्ध हिंदी एवं अंग्रेजी अक्षरांकन" else "Convert currency amounts to official words",
                icon = Icons.Default.Translate
            )
        }

        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = number,
                        onValueChange = { viewModel.convertNumber(it) },
                        label = { Text(if (isHi) "संख्या / राशि दर्ज करें (Enter Amount ₹)" else "Enter Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
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
                        text = "हिंदी अक्षरांकन (Hindi in Words):",
                        color = Color(0xFFD6E4F0),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = hindiResult,
                        color = Gold100,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "English in Words:",
                        color = Color(0xFFD6E4F0),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = englishResult,
                        color = PureWhite,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
fun AgeAndServiceTab(viewModel: ProductivityViewModel, isHi: Boolean) {
    val dob by viewModel.dobInput.collectAsState()
    val asOf by viewModel.asOfDateInput.collectAsState()
    val years by viewModel.ageResultYears.collectAsState()
    val months by viewModel.ageResultMonths.collectAsState()
    val days by viewModel.ageResultDays.collectAsState()
    val totalDays by viewModel.totalAgeDays.collectAsState()

    val joining by viewModel.joiningDateInput.collectAsState()
    val serviceText by viewModel.serviceResultText.textHi.collectAsState()
    val retirementDate by viewModel.retirementResult.collectAsState()
    val nextIncrement by viewModel.nextIncrementResult.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HeroBanner(
                title = if (isHi) "शिक्षक आयु, सेवा एवं सेवानिवृत्ति गणना" else "Age, Service & Retirement Calculator",
                subtitle = if (isHi) "बिहार सेवा संहिता 60 वर्ष नियमानुसार गणना" else "Official 60-year retirement & increment schedule",
                icon = Icons.Default.Cake
            )
        }

        // Age Calculator
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHi) "1. यथार्थ आयु गणना (Exact Age Calculator):" else "1. Age Calculator:",
                        fontWeight = FontWeight.Bold,
                        color = Navy800
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = dob,
                            onValueChange = { viewModel.dobInput.value = it },
                            label = { Text("जन्म तिथि (dd/MM/yyyy)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = asOf,
                            onValueChange = { viewModel.asOfDateInput.value = it },
                            label = { Text("कट-ऑफ दिनांक") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            viewModel.calculateAge()
                            viewModel.calculateRetirement()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                    ) {
                        Text(if (isHi) "आयु एवं सेवानिवृत्ति ज्ञात करें" else "Calculate Age")
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Navy900, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "कुल आयु: $years वर्ष $months माह $days दिन",
                                color = Gold100,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "कुल संचित दिवस: $totalDays दिन",
                                color = Color(0xFFC7D3DF),
                                fontSize = 11.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "सेवानिवृत्ति अनुमानित दिनांक (60 वर्ष): $retirementDate",
                                color = PureWhite,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Service Length & Next Increment
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHi) "2. कुल सेवा अवधि (Total Service Length):" else "2. Service Period:",
                        fontWeight = FontWeight.Bold,
                        color = Navy800
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = joining,
                        onValueChange = { viewModel.joiningDateInput.value = it },
                        label = { Text("सेवा में प्रथम योगदान दिनांक (dd/MM/yyyy)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.calculateService() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                    ) {
                        Text(if (isHi) "सेवा अवधि ज्ञात करें" else "Calculate Service")
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1B5E20), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "कुल सेवा काल: $serviceText",
                                color = PureWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "अगली वार्षिक वेतन वृद्धि (Next Increment): $nextIncrement",
                                color = PureWhite.copy(alpha = 0.9f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KnowledgeHubTab(isHi: Boolean) {
    val topics = if (isHi) {
        listOf(
            "राष्ट्रीय शिक्षा नीति (NEP 2020) मार्गदर्शिका" to "5+3+3+4 प्रारूप, मातृभाषा में शिक्षण एवं कौशल विकास",
            "निपुण भारत एवं बुनियादी साक्षरता (FLN)" to "वर्ग 1 से 3 के बच्चों में आधारभूत भाषा एवं गणितीय दक्षता",
            "SCERT बिहार पाठ्यचर्या एवं शिक्षण विधियां" to "गणित, विज्ञान, भाषा एवं सामाजिक विज्ञान गतिविधि आधारित शिक्षण",
            "निष्ठा (NISHTHA) एवं DIKSHA प्रशिक्षण संसाधन" to "डिजिटल शिक्षण मॉड्यूल, मूल्यांकन एवं प्रमाणन प्रक्रिया",
            "कक्षा कक्ष गतिविधियां एवं पाठ योजना (Lesson Plans)" to "दैनिक घंटीवार शिक्षण योजना एवं शिक्षण अधिगम सामग्री (TLM)",
            "विभागीय परिपत्र एवं सेवा संहिता नियमावली" to "बिहार राज्य शिक्षक संवर्ग नियमावली एवं सेवा शर्तें"
        )
    } else {
        listOf(
            "National Education Policy (NEP 2020)" to "5+3+3+4 structure, foundational learning",
            "NIPUN Bharat & FLN Mission" to "Foundational literacy and numeracy",
            "SCERT Bihar Curriculum & Methods" to "Activity-based pedagogy for Bihar schools",
            "NISHTHA & DIKSHA Teacher Training" to "Digital modules and continuous development",
            "Lesson Plans & Classroom Activities" to "Structured teaching plans and TLM ideas",
            "Bihar Teacher Cadre & Service Rules" to "Official service conditions and circulars"
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            HeroBanner(
                title = if (isHi) "शिक्षक ज्ञान केंद्र (Knowledge Hub)" else "Teacher Knowledge Hub",
                subtitle = if (isHi) "NEP, SCERT, पाठ योजना एवं शिक्षण अधिगम संसाधन" else "Official pedagogical resources & guidelines",
                icon = Icons.Default.MenuBook
            )
        }

        items(topics) { (title, desc) ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Gold700.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.MenuBook, contentDescription = null, tint = Gold700)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        Text(text = desc, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun NotesTab(viewModel: ProductivityViewModel, isHi: Boolean) {
    val notes by viewModel.notes.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                HeroBanner(
                    title = if (isHi) "शिक्षक व्यक्तिगत नोट्स" else "Teacher Personal Notes",
                    subtitle = if (isHi) "विद्यालयी कार्य, बैठक निर्देश एवं महत्वपूर्ण टिप्पणियां" else "Secure offline notes for teachers",
                    icon = Icons.Default.NoteAdd
                )
            }

            if (notes.isEmpty()) {
                item {
                    Text(
                        text = if (isHi) "कोई नोट नहीं है। नया नोट जोड़ने के लिए + बटन दबाएं।" else "No notes yet. Click + to add.",
                        modifier = Modifier.padding(16.dp),
                        color = Color.Gray
                    )
                }
            } else {
                items(notes) { note ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = note.title, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = note.content, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { viewModel.deleteNote(note) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = Navy800,
            contentColor = PureWhite
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Note")
        }
    }

    if (showDialog) {
        var noteTitle by remember { mutableStateOf("") }
        var noteContent by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (isHi) "नया नोट जोड़ें" else "Add Note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text("शीर्षक (Title)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = noteContent,
                        onValueChange = { noteContent = it },
                        label = { Text("विवरण (Content)") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (noteTitle.isNotBlank()) {
                            viewModel.saveNote(noteTitle.trim(), noteContent.trim())
                            showDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                ) {
                    Text("सहेजें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}

@Composable
fun TasksTab(viewModel: ProductivityViewModel, isHi: Boolean) {
    val tasks by viewModel.tasks.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                HeroBanner(
                    title = if (isHi) "शिक्षक कार्य सूची (Tasks / To-Do)" else "Teacher Tasks Manager",
                    subtitle = if (isHi) "परीक्षा, मूल्यांकन, MDM रिपोर्ट एवं समयबद्ध विभागीय कार्य" else "Track school duties & departmental deadlines",
                    icon = Icons.Default.CheckCircle
                )
            }

            if (tasks.isEmpty()) {
                item {
                    Text(
                        text = if (isHi) "कोई लंबित कार्य नहीं है। नया कार्य जोड़ने के लिए + बटन दबाएं।" else "No tasks added. Click + to add.",
                        modifier = Modifier.padding(16.dp),
                        color = Color.Gray
                    )
                }
            } else {
                items(tasks) { task ->
                    ElevatedCard(
                        onClick = { viewModel.toggleTask(task) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.toggleTask(task) }) {
                                Icon(
                                    imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (task.isCompleted) Color(0xFF1B5E20) else Color.Gray
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    fontWeight = FontWeight.Bold,
                                    style = if (task.isCompleted) MaterialTheme.typography.bodyMedium.copy(color = Color.Gray) else MaterialTheme.typography.bodyMedium
                                )
                                if (task.dueDate.isNotBlank()) {
                                    Text(
                                        text = "अंतिम तिथि: ${task.dueDate} • प्राथमिकता: ${task.priority}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.deleteTask(task) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = Navy800,
            contentColor = PureWhite
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task")
        }
    }

    if (showDialog) {
        var taskTitle by remember { mutableStateOf("") }
        var dueDate by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (isHi) "नया कार्य जोड़ें" else "Add Task") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        label = { Text("कार्य का नाम") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = { Text("अंतिम तिथि (e.g. 25/11/2026)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (taskTitle.isNotBlank()) {
                            viewModel.addTask(taskTitle.trim(), dueDate.trim())
                            showDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800)
                ) {
                    Text("जोड़ें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}
