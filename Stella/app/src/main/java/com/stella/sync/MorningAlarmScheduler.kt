package com.stella.sync

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.stella.core.data.SettingsRepository
import com.stella.feature.morning.MorningLockPermissions
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object MorningAlarmScheduler {
    const val ACTION_MORNING_ALARM = "com.stella.action.MORNING_ALARM"
    const val ACTION_MORNING_ALARM_TEST = "com.stella.action.MORNING_ALARM_TEST"

    private const val REQUEST_CODE_WAKE = 9101
    private const val REQUEST_CODE_TEST = 9102

    fun schedule(context: Context) {
        val settings = entryPoint(context).settingsRepository()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelWake(context, alarmManager)
        if (!settings.isMorningLockEnabled()) return
        if (!settings.isMorningSetupCompleted()) return
        if (!MorningLockPermissions.canScheduleExactAlarms(context)) return

        val triggerAt = nextWakeMillis(settings)
        val pending = pendingIntent(context, ACTION_MORNING_ALARM, REQUEST_CODE_WAKE)
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerAt, showIntent(context)),
            pending,
        )
    }

    fun scheduleTest(context: Context, delaySeconds: Int = 10): ScheduleResult {
        if (!MorningLockPermissions.canScheduleExactAlarms(context)) {
            return ScheduleResult(false, "Allow exact alarms in Settings first.")
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + delaySeconds * 1000L
        val pending = pendingIntent(context, ACTION_MORNING_ALARM_TEST, REQUEST_CODE_TEST)
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerAt, showIntent(context, isTest = true)),
            pending,
        )
        return ScheduleResult(true, null)
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelWake(context, alarmManager)
        alarmManager.cancel(pendingIntent(context, ACTION_MORNING_ALARM_TEST, REQUEST_CODE_TEST))
    }

    private fun cancelWake(context: Context, alarmManager: AlarmManager) {
        alarmManager.cancel(pendingIntent(context, ACTION_MORNING_ALARM, REQUEST_CODE_WAKE))
    }

    private fun showIntent(context: Context, isTest: Boolean = false): PendingIntent {
        val intent = Intent(context, com.stella.feature.morning.MorningLockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(com.stella.feature.morning.MorningLockActivity.EXTRA_FROM_ALARM, true)
            putExtra(com.stella.feature.morning.MorningLockActivity.EXTRA_IS_TEST_ALARM, isTest)
        }
        return PendingIntent.getActivity(
            context,
            REQUEST_CODE_WAKE + 100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun nextWakeMillis(settings: SettingsRepository): Long {
        val zone = runCatching { ZoneId.of(settings.effectiveTimeZoneId()) }
            .getOrDefault(ZoneId.systemDefault())
        val target = LocalTime.of(settings.getMorningWakeHour(), settings.getMorningWakeMinute())
        val now = ZonedDateTime.now(zone)
        var next = now.with(target).withSecond(0).withNano(0)
        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }
        return next.toInstant().toEpochMilli()
    }

    private fun pendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MorningAlarmReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun entryPoint(context: Context): MorningLockEntryPoint =
        EntryPointAccessors.fromApplication(context, MorningLockEntryPoint::class.java)

    data class ScheduleResult(val success: Boolean, val error: String?)
}
