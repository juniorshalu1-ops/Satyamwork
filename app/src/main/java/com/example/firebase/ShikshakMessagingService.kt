package com.example.firebase

import android.content.Context
import android.util.Log
import com.example.core.NotificationPreferences
import com.example.core.SoundNotificationHelper
import com.example.database.AppDatabase
import com.example.database.AppRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Handles incoming FCM push notifications and silent background cache-invalidation signals
 * dispatched by Cloud Functions (`onBroadcastAlertWritten`, `onNoticeWrittenPush`,
 * `onBannerWrittenSync`, `onHolidayWrittenSync`), as well as FCM token refreshes.
 */
class ShikshakMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val appContext = applicationContext
        serviceScope.launch {
            try {
                NotificationPreferences.saveFcmToken(appContext, token)
                val enabled = NotificationPreferences.isNotificationsEnabled(appContext)
                if (enabled) {
                    NotificationPreferences.syncTopicSubscriptions(true)
                }
                Log.d(TAG, "Refreshed FCM token saved and topic subscriptions verified")
            } catch (t: Throwable) {
                Log.w(TAG, "onNewToken processing warning", t)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val appContext = applicationContext
        val data = remoteMessage.data
        val syncSection = data["syncSection"]?.trim().orEmpty()
        val alertId = data["alertId"]?.trim().orEmpty()
        val title = data["title"]?.trim().takeIf { !it.isNullOrBlank() }
            ?: remoteMessage.notification?.title?.trim().orEmpty()
        val body = data["body"]?.trim().takeIf { !it.isNullOrBlank() }
            ?: remoteMessage.notification?.body?.trim().orEmpty()
        val section = data["section"]?.trim().orEmpty().ifBlank { "विभागीय अपडेट" }
        val contentType = data["contentType"]?.trim().orEmpty()
        val contentId = data["contentId"]?.trim().orEmpty()
        val targetSection = data["targetSection"]?.trim().orEmpty()
        val targetUrl = data["targetUrl"]?.trim().orEmpty()

        serviceScope.launch {
            // 1. Trigger immediate incremental cloud sync on silent sync signals or content alerts
            if (syncSection.isNotBlank() || title.isNotBlank()) {
                try {
                    val db = AppDatabase.getDatabase(appContext, serviceScope)
                    val repository = AppRepository(db)
                    FirebaseManager.getInstance(appContext).syncChangedCollectionsOnce(repository)
                } catch (t: Throwable) {
                    Log.w(TAG, "Background FCM sync skipped for section=$syncSection", t)
                }
            }

            // 2. Display notification in foreground / for data-only alert pushes
            if (syncSection.isBlank() && title.isNotBlank()) {
                try {
                    val notificationsEnabled = NotificationPreferences.isNotificationsEnabled(appContext)
                    if (!notificationsEnabled) return@launch

                    val shouldNotify = synchronized(alertLock) {
                        val prefs = appContext.getSharedPreferences(CLOUD_PREFS_NAME, Context.MODE_PRIVATE)
                        val lastAlertId = prefs.getString(CloudSyncEngine.KEY_LAST_ALERT_ID, "").orEmpty()
                        if (alertId.isNotBlank() && alertId == lastAlertId) {
                            false
                        } else {
                            if (alertId.isNotBlank()) {
                                prefs.edit().putString(CloudSyncEngine.KEY_LAST_ALERT_ID, alertId).apply()
                            }
                            true
                        }
                    }

                    if (shouldNotify) {
                        SoundNotificationHelper.triggerSpecialAdminNotification(
                            context = appContext,
                            title = title,
                            section = section,
                            details = body,
                            targetSection = targetSection,
                            contentType = contentType,
                            contentId = contentId,
                            targetUrl = targetUrl
                        )
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "FCM foreground notification display error", t)
                }
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "ShikshakMessagingSvc"
        const val CLOUD_PREFS_NAME = "shikshak_cloud_prefs"
        val alertLock = Any()
    }
}
