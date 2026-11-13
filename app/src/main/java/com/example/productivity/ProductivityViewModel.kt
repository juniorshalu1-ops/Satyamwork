package com.example.productivity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.DateUtils
import com.example.core.NumberToWords
import com.example.database.AppDatabase
import com.example.database.AppRepository
import com.example.database.entities.PersonalNoteEntity
import com.example.database.entities.TeacherTaskEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date

class ProductivityViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository

    init {
        val db = AppDatabase.getDatabase(application, viewModelScope)
        repository = AppRepository(db)
    }

    val notes: StateFlow<List<PersonalNoteEntity>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TeacherTaskEntity>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 1. Number to Words
    val numberInput = MutableStateFlow("45230")
    val hindiWordsResult = MutableStateFlow(NumberToWords.toHindiWords(45230))
    val englishWordsResult = MutableStateFlow(NumberToWords.toEnglishWords(45230))

    fun convertNumber(input: String) {
        numberInput.value = input
        val num = input.toLongOrNull() ?: 0L
        hindiWordsResult.value = NumberToWords.toHindiWords(num)
        englishWordsResult.value = NumberToWords.toEnglishWords(num)
    }

    // 2. Age Calculator
    val dobInput = MutableStateFlow("15/08/1990")
    val asOfDateInput = MutableStateFlow(DateUtils.formatDate(Date()))
    val ageResultYears = MutableStateFlow(36)
    val ageResultMonths = MutableStateFlow(2)
    val ageResultDays = MutableStateFlow(16)
    val totalAgeDays = MutableStateFlow(13220L)

    fun calculateAge() {
        val dob = DateUtils.parseDate(dobInput.value) ?: return
        val asOf = DateUtils.parseDate(asOfDateInput.value) ?: Date()
        val res = DateUtils.calculateDifference(dob, asOf)
        ageResultYears.value = res.years
        ageResultMonths.value = res.months
        ageResultDays.value = res.days
        totalAgeDays.value = res.totalDays
    }

    // 3. Service Period Calculator
    val joiningDateInput = MutableStateFlow("01/01/2020")
    val serviceResultText = MutableFlowServiceResult()

    class MutableFlowServiceResult {
        val textHi = MutableStateFlow("6 वर्ष 10 माह 0 दिन")
        val textEn = MutableStateFlow("6 Years 10 Months 0 Days")
    }

    fun calculateService() {
        val joining = DateUtils.parseDate(joiningDateInput.value) ?: return
        val diff = DateUtils.calculateDifference(joining, Date())
        serviceResultText.textHi.value = "${diff.years} वर्ष ${diff.months} माह ${diff.days} दिन (कुल दिन: ${diff.totalDays})"
        serviceResultText.textEn.value = "${diff.years} Years ${diff.months} Months ${diff.days} Days (Total: ${diff.totalDays} days)"
    }

    // 4. Retirement Date
    val retirementResult = MutableStateFlow("31/08/2050")
    fun calculateRetirement() {
        val dob = DateUtils.parseDate(dobInput.value) ?: return
        val retDate = DateUtils.calculateRetirementDate(dob)
        retirementResult.value = DateUtils.formatDate(retDate)
    }

    // 5. Next Increment Date
    val nextIncrementResult = MutableStateFlow(DateUtils.getNextIncrementDate())

    // Notes CRUD
    fun saveNote(title: String, content: String, tag: String = "सामान्य") {
        viewModelScope.launch {
            repository.saveNote(
                PersonalNoteEntity(title = title, content = content, tag = tag)
            )
        }
    }

    fun deleteNote(note: PersonalNoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    // Tasks CRUD
    fun addTask(title: String, dueDate: String = "", priority: String = "मध्यम") {
        viewModelScope.launch {
            repository.saveTask(
                TeacherTaskEntity(title = title, dueDate = dueDate, priority = priority)
            )
        }
    }

    fun toggleTask(task: TeacherTaskEntity) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteTask(task: TeacherTaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }
}
