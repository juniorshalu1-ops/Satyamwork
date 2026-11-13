package com.example

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.core.AppLanguage
import com.example.core.LocalAppLanguage
import com.example.core.LocalizationManager
import com.example.core.SoundNotificationHelper
import com.example.navigation.MainAppContainer
import com.example.ui.theme.ShikshakSathiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
        } catch (e: Throwable) {
            // Ignore edge to edge failure on older devices
        }

        try {
            SoundNotificationHelper.initializeChannel(this)
        } catch (e: Throwable) {
            android.util.Log.w("MainActivity", "Channel init error", e)
        }

        // Request runtime notification permission on Android 13+
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
                }
            }
        } catch (e: Throwable) {
            android.util.Log.w("MainActivity", "Notification permission request error", e)
        }
        setContent {
            val localizationManager = remember { LocalizationManager(AppLanguage.HINDI) }

            CompositionLocalProvider(LocalAppLanguage provides localizationManager) {
                ShikshakSathiTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        MainAppContainer()
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
