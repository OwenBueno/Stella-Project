package com.stella.feature.morning

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService

object MorningLockPermissions {
    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.getSystemService<AlarmManager>() ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun hasPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun canUseFullScreenIntent(context: Context): Boolean {
        val manager = context.getSystemService<NotificationManager>() ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            manager.canUseFullScreenIntent()
        } else {
            true
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService<PowerManager>() ?: return false
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun overlaySettingsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun exactAlarmSettingsIntent(): Intent =
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun notificationSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun batteryOptimizationIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun allCriticalGranted(context: Context): Boolean =
        hasPostNotifications(context) &&
            canUseFullScreenIntent(context) &&
            canScheduleExactAlarms(context) &&
            canDrawOverlays(context)

    fun missingCriticalPermissions(context: Context): List<String> = buildList {
        if (!hasPostNotifications(context)) add("notifications")
        if (!canUseFullScreenIntent(context)) add("full-screen alarm")
        if (!canScheduleExactAlarms(context)) add("exact alarms")
        if (!canDrawOverlays(context)) add("draw over other apps")
    }

    fun testBlockedMessage(context: Context): String? {
        val missing = missingCriticalPermissions(context)
        if (missing.isEmpty()) return null
        return "Grant ${missing.joinToString(", ")} before testing. Use Refresh status after enabling each permission."
    }
}
