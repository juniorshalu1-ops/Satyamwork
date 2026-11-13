package com.example.documents

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai_application.AiApplicationRequest
import com.example.ai_application.AiApplicationResponse
import com.example.ai_application.AiApplicationService
import com.example.core.ApplicationPdfGenerator
import com.example.core.DateUtils
import com.example.database.AppDatabase
import com.example.database.AppRepository
import com.example.database.entities.DocumentDraftEntity
import com.example.database.entities.TeacherProfileEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date

class DocumentsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository

    init {
        val db = AppDatabase.getDatabase(application, viewModelScope)
        repository = AppRepository(db)
    }

    val drafts: StateFlow<List<DocumentDraftEntity>> = repository.allDrafts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val teacherProfile: StateFlow<TeacherProfileEntity?> = repository.teacherProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Current Active Editor
    val activeTitle = MutableStateFlow("")
    val activeRecipient = MutableStateFlow("")
    val activeSubject = MutableStateFlow("")
    val activeBody = MutableStateFlow("")
    val activeTemplateId = MutableStateFlow("custom")
    val isAiLoading = MutableStateFlow(false)
    val aiStatusNotice = MutableStateFlow("")

    fun loadTemplate(template: OfficialTemplate, isHindi: Boolean = true) {
        activeTemplateId.value = template.id
        activeTitle.value = if (isHindi) template.titleHi else template.titleEn
        activeRecipient.value = if (isHindi) template.defaultRecipientHi else template.defaultRecipientEn
        activeSubject.value = if (isHindi) template.defaultSubjectHi else template.defaultSubjectEn
        activeBody.value = if (isHindi) template.defaultBodyHi else template.defaultBodyEn
    }

    fun loadDraft(draft: DocumentDraftEntity) {
        activeTemplateId.value = draft.templateId
        activeTitle.value = draft.title
        activeRecipient.value = draft.recipient
        activeSubject.value = draft.subject
        activeBody.value = draft.body
    }

    fun generateWithAi(
        reason: String,
        dates: String,
        appType: String,
        isHindi: Boolean = true
    ) {
        val teacher = teacherProfile.value ?: TeacherProfileEntity()
        viewModelScope.launch {
            isAiLoading.value = true
            aiStatusNotice.value = "आवेदन तैयार किया जा रहा है... (Writing application...)"
            val request = AiApplicationRequest(
                recipient = activeRecipient.value.ifBlank { "सेवा में,\nप्रखंड शिक्षा पदाधिकारी / प्रधानाध्यापक" },
                applicationType = appType.ifBlank { activeTitle.value },
                reason = reason,
                dates = dates,
                applicantName = teacher.name,
                designation = teacher.designation,
                schoolName = teacher.schoolName,
                district = teacher.district,
                language = if (isHindi) "hi" else "en"
            )
            val response: AiApplicationResponse = AiApplicationService.generateApplication(request)
            activeSubject.value = response.subject
            activeBody.value = response.body
            aiStatusNotice.value = response.notice
            isAiLoading.value = false
        }
    }

    fun saveCurrentDraft() {
        val teacher = teacherProfile.value ?: TeacherProfileEntity()
        viewModelScope.launch {
            val draft = DocumentDraftEntity(
                templateId = activeTemplateId.value,
                title = activeTitle.value.ifBlank { "आवेदन पत्र" },
                recipient = activeRecipient.value,
                subject = activeSubject.value,
                body = activeBody.value,
                applicantName = teacher.name,
                designation = teacher.designation,
                schoolName = teacher.schoolName,
                district = teacher.district,
                dateStr = DateUtils.formatDate(Date())
            )
            repository.saveDraft(draft)
        }
    }

    fun deleteDraft(draft: DocumentDraftEntity) {
        viewModelScope.launch {
            repository.deleteDraft(draft)
        }
    }

    fun exportPdf(context: Context): File {
        val teacher = teacherProfile.value ?: TeacherProfileEntity()
        val draft = DocumentDraftEntity(
            templateId = activeTemplateId.value,
            title = activeTitle.value.ifBlank { "आवेदन पत्र" },
            recipient = activeRecipient.value,
            subject = activeSubject.value,
            body = activeBody.value,
            applicantName = teacher.name,
            designation = teacher.designation,
            schoolName = teacher.schoolName,
            district = teacher.district,
            dateStr = DateUtils.formatDate(Date())
        )
        return ApplicationPdfGenerator.generatePdf(context, draft)
    }
}
