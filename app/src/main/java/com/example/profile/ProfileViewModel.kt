package com.example.profile

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.EncryptedBackupManager
import com.example.database.AppDatabase
import com.example.database.AppRepository
import com.example.database.entities.TeacherProfileEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.nio.charset.StandardCharsets

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository

    init {
        val db = AppDatabase.getDatabase(application, viewModelScope)
        repository = AppRepository(db)
    }

    val teacherProfile: StateFlow<TeacherProfileEntity?> = repository.teacherProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val backupExportResult = MutableStateFlow<String?>(null)
    val restoreStatusMessage = MutableStateFlow("")

    fun saveTeacherProfile(profile: TeacherProfileEntity) {
        viewModelScope.launch {
            repository.saveTeacherProfile(profile)
        }
    }

    fun exportBackup() {
        viewModelScope.launch {
            val json = repository.exportBackupJson()
            backupExportResult.value = json
        }
    }

    fun exportEncryptedBackupToDeviceOrDrive(context: Context) {
        viewModelScope.launch {
            try {
                val json = repository.exportBackupJson()
                val backupFile = EncryptedBackupManager.createEncryptedBackupFile(context, json)
                EncryptedBackupManager.shareBackupToDriveOrDevice(context, backupFile)
            } catch (e: Exception) {
                restoreStatusMessage.value = "एन्क्रिप्टेड बैकअप निर्माण में त्रुटि: ${e.message}"
            }
        }
    }

    fun importBackupFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader(StandardCharsets.UTF_8).readText()
                } ?: ""
                if (content.isNotBlank()) {
                    val trimmed = content.trim()
                    val jsonToRestore = if (trimmed.startsWith("{")) {
                        trimmed
                    } else {
                        try {
                            EncryptedBackupManager.decryptData(trimmed)
                        } catch (e: Exception) {
                            trimmed
                        }
                    }
                    val success = repository.importBackupJson(jsonToRestore)
                    restoreStatusMessage.value = if (success) {
                        "Google Drive / स्टोरेज से डेटा सफलतापूर्वक पुनर्स्थापित हो गया!"
                    } else {
                        "डेटा पुनर्स्थापना विफल: अमान्य बैकअप फाइल"
                    }
                } else {
                    restoreStatusMessage.value = "चयनित बैकअप फाइल रिक्त है।"
                }
            } catch (e: Exception) {
                restoreStatusMessage.value = "फाइल पढ़ने में त्रुटि: ${e.localizedMessage}"
            }
        }
    }

    fun restoreFromLatestLocalBackup(context: Context) {
        viewModelScope.launch {
            try {
                val backupDir = File(context.cacheDir, "backups")
                val latestFile = backupDir.listFiles()?.sortedByDescending { it.lastModified() }?.firstOrNull()
                if (latestFile != null && latestFile.exists()) {
                    val content = latestFile.readText(StandardCharsets.UTF_8).trim()
                    val jsonToRestore = if (content.startsWith("{")) content else EncryptedBackupManager.decryptData(content)
                    val success = repository.importBackupJson(jsonToRestore)
                    restoreStatusMessage.value = if (success) {
                        "डिवाइस के हालिया बैकअप (${latestFile.name}) से डेटा सफलतापूर्वक रीस्टोर किया गया!"
                    } else {
                        "बैकअप डेटा अमान्य है।"
                    }
                } else {
                    restoreStatusMessage.value = "डिवाइस में कोई पूर्व बैकअप नहीं मिला। कृपया Google Drive या फ़ाइल मैनेजर से चुनें।"
                }
            } catch (e: Exception) {
                restoreStatusMessage.value = "स्थानीय बैकअप से रीस्टोर विफल: ${e.localizedMessage}"
            }
        }
    }

    fun importBackup(jsonString: String) {
        viewModelScope.launch {
            val trimmed = jsonString.trim()
            val jsonToRestore = if (trimmed.startsWith("{")) {
                trimmed
            } else {
                try {
                    EncryptedBackupManager.decryptData(trimmed)
                } catch (e: Exception) {
                    trimmed
                }
            }
            val success = repository.importBackupJson(jsonToRestore)
            restoreStatusMessage.value = if (success) {
                "डेटा सफलतापूर्वक पुनर्स्थापित किया गया (Data restored successfully)"
            } else {
                "डेटा पुनर्स्थापना विफल रही: अमान्य बैकअप (Invalid backup data)"
            }
        }
    }
}
