package com.example.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class AppLanguage(val code: String, val displayName: String) {
    HINDI("hi", "हिंदी"),
    ENGLISH("en", "English")
}

class LocalizationManager(initialLanguage: AppLanguage = AppLanguage.HINDI) {
    var currentLanguage by mutableStateOf(initialLanguage)
        private set

    fun setLanguage(language: AppLanguage) {
        currentLanguage = language
    }

    fun toggleLanguage() {
        currentLanguage = if (currentLanguage == AppLanguage.HINDI) AppLanguage.ENGLISH else AppLanguage.HINDI
    }

    fun isHindi(): Boolean = currentLanguage == AppLanguage.HINDI

    fun get(hi: String, en: String): String {
        return if (currentLanguage == AppLanguage.HINDI) hi else en
    }
}

val LocalAppLanguage = compositionLocalOf { LocalizationManager() }
