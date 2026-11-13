package com.example.core

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.notificationDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "shikshak_notification_prefs"
)

object NotificationPreferences {
    private const val TAG = "NotificationPreferences"
    const val TOPIC_TEACHERS_ALL = "teachers_all"
    const val TOPIC_NOTICES_URGENT = "notices_urgent"

    private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    private val KEY_FCM_TOKEN = stringPreferencesKey("last_fcm_token")

    fun notificationsEnabledFlow(context: Context): Flow<Boolean> =
        context.applicationContext.notificationDataStore.data.map { prefs ->
            prefs[KEY_NOTIFICATIONS_ENABLED] ?: true
        }

    suspend fun isNotificationsEnabled(context: Context): Boolean =
        notificationsEnabledFlow(context).first()

    suspend fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        val appContext = context.applicationContext
        appContext.notificationDataStore.edit { prefs ->
            prefs[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
        syncTopicSubscriptions(enabled)
    }

    suspend fun saveFcmToken(context: Context, token: String) {
        if (token.isBlank()) return
        context.applicationContext.notificationDataStore.edit { prefs ->
            prefs[KEY_FCM_TOKEN] = token
        }
    }

    fun syncTopicSubscriptions(enabled: Boolean) {
        try {
            val messaging = FirebaseMessaging.getInstance()
            if (enabled) {
                messaging.subscribeToTopic(TOPIC_TEACHERS_ALL)
                    .addOnFailureListener { e -> Log.w(TAG, "Topic subscribe failed: $TOPIC_TEACHERS_ALL", e) }
                messaging.subscribeToTopic(TOPIC_NOTICES_URGENT)
                    .addOnFailureListener { e -> Log.w(TAG, "Topic subscribe failed: $TOPIC_NOTICES_URGENT", e) }
            } else {
                messaging.unsubscribeFromTopic(TOPIC_TEACHERS_ALL)
                    .addOnFailureListener { e -> Log.w(TAG, "Topic unsubscribe failed: $TOPIC_TEACHERS_ALL", e) }
                messaging.unsubscribeFromTopic(TOPIC_NOTICES_URGENT)
                    .addOnFailureListener { e -> Log.w(TAG, "Topic unsubscribe failed: $TOPIC_NOTICES_URGENT", e) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "FCM topic sync skipped", e)
        }
    }
}
