package com.stella.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.stella.core.data.SettingsRepository
import com.stella.feature.morning.MorningLockActivity
import dagger.hilt.android.EntryPointAccessors

object MorningAlarmNotifications {
    const val CHANNEL_ENFORCEMENT_ID = "stella_morning_enforcement"
    const val NOTIFICATION_ALARM_ID = 7101
    const val NOTIFICATION_ENFORCEMENT_ID = 7102

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val settings = entryPoint(context).settingsRepository()
        val channelId = alarmChannelId(settings)
        manager.deleteNotificationChannel(channelId)
        manager.createNotificationChannel(
            NotificationChannel(
                channelId,
                "Morning alarm",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Wakes you for the morning lock flow (audio plays via in-app alarm)"
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ENFORCEMENT_ID,
                "Morning lock active",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shown while morning lock is enforcing"
            },
        )
    }

    fun showAlarmFullScreen(context: Context) {
        ensureChannels(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ALARM_ID, buildAlarmNotification(context))
    }

    fun buildAlarmNotification(context: Context): Notification {
        ensureChannels(context)
        val settings = entryPoint(context).settingsRepository()
        val channelId = alarmChannelId(settings)
        val launchIntent = Intent(context, MorningLockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPending = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val contentPending = PendingIntent.getActivity(
            context,
            1,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Morning lock")
            .setContentText("Scan your bathroom tag to start the day")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOngoing(true)
            .setContentIntent(contentPending)
            .setFullScreenIntent(fullScreenPending, true)
            .build()
    }

    fun buildEnforcementNotification(context: Context): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ENFORCEMENT_ID) == null) {
                ensureChannels(context)
            }
        }
        val launchIntent = Intent(context, MorningLockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            2,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_ENFORCEMENT_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Morning lock active")
            .setContentText("Complete your morning routine to unlock")
            .setOngoing(true)
            .setContentIntent(pending)
            .build()
    }

    private fun alarmChannelId(settings: SettingsRepository): String {
        val soundKey = settings.getMorningAlarmSoundUri() ?: "default"
        return "stella_morning_alarm_${soundKey.hashCode()}"
    }

    private fun entryPoint(context: Context): MorningLockEntryPoint =
        EntryPointAccessors.fromApplication(context, MorningLockEntryPoint::class.java)
}
