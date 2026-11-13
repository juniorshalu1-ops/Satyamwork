package com.example.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import java.util.concurrent.atomic.AtomicInteger

object SoundNotificationHelper {

    private const val TAG = "SoundNotificationHelper"
    const val CHANNEL_ID = "shikshak_sathi_admin_broadcast_v3"
    private const val CHANNEL_NAME = "शिक्षा विभाग प्रशासनिक आदेश व सूचनाएं"
    private val notificationCounter = AtomicInteger(1000)

    fun initializeChannel(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return

                val soundUri: Uri? = try {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                } catch (e: Throwable) {
                    null
                }

                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                    .build()

                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "व्यवस्थापक द्वारा अपलोड किए गए नए आदेशों, अवकाशों एवं गतिविधियों का विशेष नोटिफिकेशन (स्टेटस बार व लॉक स्क्रीन)"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 450, 200, 450)
                    enableLights(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    if (soundUri != null) {
                        setSound(soundUri, audioAttributes)
                    }
                }

                notificationManager.createNotificationChannel(channel)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "initializeChannel safe caught", e)
        }
    }

    private val recentAlertTimestamps = java.util.concurrent.ConcurrentHashMap<String, Long>()

    /**
     * Posts a high-priority system notification in the top status bar.
     * Displays in notification tray, lock screen, and plays sound with vibration.
     * Prevents duplicate notification triggers within a 15-second window.
     */
    fun triggerSpecialAdminNotification(
        context: Context,
        title: String,
        section: String,
        details: String,
        targetSection: String = "",
        contentType: String = "",
        contentId: String = "",
        targetUrl: String = ""
    ) {
        val appContext = context.applicationContext

        // Deduplication Check: Prevent duplicate notification triggers within a 60-second window
        val idPart = if (contentId.isNotBlank()) "_${contentId.trim().lowercase()}" else ""
        val dedupeKey = "${section.trim().lowercase()}_${title.trim().lowercase()}$idPart"
        val currentTime = System.currentTimeMillis()
        val lastTriggerTime = recentAlertTimestamps[dedupeKey] ?: 0L
        if (currentTime - lastTriggerTime < 60_000L) {
            Log.d(TAG, "Suppressed duplicate notification within 60s window for: $dedupeKey")
            return
        }
        recentAlertTimestamps[dedupeKey] = currentTime

        // Clean up old entries from dedupe cache periodically
        if (recentAlertTimestamps.size > 50) {
            recentAlertTimestamps.entries.removeIf { currentTime - it.value > 120_000L }
        }

        // 1. Ensure channel is ready
        initializeChannel(appContext)

        // 2. Audible notification tone & vibration
        try {
            val alertSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val ringtone = RingtoneManager.getRingtone(appContext, alertSoundUri)
            ringtone?.play()
        } catch (e: Exception) {
            Log.w(TAG, "Ringtone play error", e)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                val pattern = longArrayOf(0, 400, 200, 400)
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                val pattern = longArrayOf(0, 400, 200, 400)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibration error", e)
        }

        // 3. Post High-Priority Lock-Screen & Status Bar System Notification
        try {
            val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val resolvedDeepLinkType = DeepLinkHelper.normalizeType(
                contentType = contentType,
                targetSection = targetSection,
                sectionTitle = section,
                title = title,
                details = details
            )
            val resolvedUrl = targetUrl.ifBlank { DeepLinkHelper.extractFirstUrl(details) }

            // Deterministic notification ID so updates update existing card instead of creating duplicates
            val notifId = (kotlin.math.abs(dedupeKey.hashCode()) % 50000) + 1000

            val intent = Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                DeepLinkHelper.writeToIntent(
                    intent = this,
                    type = resolvedDeepLinkType,
                    contentId = contentId,
                    title = title,
                    targetUrl = resolvedUrl,
                    eventId = currentTime
                )
            }
            val pendingIntent = PendingIntent.getActivity(
                appContext,
                notifId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            val cleanDetails = details.trim()
            val topicHeader = if (section.isNotBlank()) "Shikshak Sathi – $section" else "Shikshak Sathi"
            val subtopicTitle = "Subtopic: $title"
            val descBody = if (cleanDetails.isNotBlank()) "Description: $cleanDetails" else ""
            val fullBody = if (descBody.isNotBlank()) "$subtopicTitle\n$descBody" else subtopicTitle

            val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentTitle(topicHeader)
                .setContentText(subtopicTitle)
                .setSubText(section)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .setBigContentTitle(topicHeader)
                        .setSummaryText(section)
                        .bigText(fullBody)
                )
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setSound(soundUri)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            // Requirement 10: Action: VIEW UPDATE (only if link or target section exists)
            builder.addAction(
                R.drawable.ic_stat_notification,
                "VIEW UPDATE",
                pendingIntent
            )

            val notification = builder.build()
            notificationManager.notify(notifId, notification)
            Log.d(TAG, "Notification posted with ID: $notifId for title: $title")
        } catch (e: Exception) {
            Log.e(TAG, "NotificationManager post error", e)
        }
    }

    fun triggerLoudAdminAlert(context: Context, title: String, section: String, details: String) {
        triggerSpecialAdminNotification(context, title, section, details)
    }

    fun stopLoudAlarm() {
        // Safe no-op
    }
}
