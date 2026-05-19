package com.stella.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.stella.app.MainActivity
import com.stella.core.data.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object EveningReviewScheduler {
    private const val WORK_NAME = "evening_review_reminder"
    private const val CHANNEL_ID = "stella_evening_review"

    fun schedule(context: Context) {
        val settings = EntryPointAccessors.fromApplication(
            context,
            EveningSchedulerEntryPoint::class.java,
        ).settingsRepository()
        val zone = runCatching { ZoneId.of(settings.getTimeZoneId()) }.getOrDefault(ZoneId.systemDefault())
        val target = LocalTime.of(settings.getEveningReviewHour(), settings.getEveningReviewMinute())
        val now = ZonedDateTime.now(zone)
        var next = now.with(target)
        if (next.isBefore(now)) {
            next = next.plusDays(1)
        }
        val delayMinutes = Duration.between(now, next).toMinutes().coerceAtLeast(1)
        val request = PeriodicWorkRequestBuilder<EveningReviewWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun showNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Evening review",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_REVIEW, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        manager.notify(
            2001,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Stella")
                .setContentText("Time for your evening review.")
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build(),
        )
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface EveningSchedulerEntryPoint {
    fun settingsRepository(): SettingsRepository
}
