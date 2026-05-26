package com.stella.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.stella.app.MainActivity
import com.stella.core.calendar.CalendarEventJson
import com.stella.core.calendar.RecurrenceExpander
import com.stella.core.calendar.RecurrenceFrequency
import com.stella.core.database.dao.CalendarEventDao
import com.stella.core.database.entity.CalendarEventEntity
import com.stella.core.util.TimeService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class EventReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val calendarEventDao: CalendarEventDao,
    private val timeService: TimeService,
) {
    suspend fun rescheduleAllActive() = withContext(Dispatchers.IO) {
        calendarEventDao.getAllActive().forEach { rescheduleEvent(it) }
    }

    fun rescheduleEvent(event: CalendarEventEntity) {
        cancelEvent(event.id, CalendarEventJson.decodeReminderOffsets(event.reminderOffsetsJson))
        if (event.deletedAt != null) return
        val offsets = CalendarEventJson.decodeReminderOffsets(event.reminderOffsetsJson)
        if (offsets.isEmpty()) return
        val rule = CalendarEventJson.decodeRecurrence(event.recurrenceRuleJson)
        val zone = timeService.zone()
        val occurrences = if (rule.frequency == RecurrenceFrequency.NONE) {
            RecurrenceExpander.expand(
                eventId = event.id,
                title = event.title,
                startAtIso = event.startAt,
                endAtIso = event.endAt,
                linkedTaskId = event.linkedTaskId,
                rule = rule,
                rangeStart = Instant.parse(event.startAt),
                rangeEnd = Instant.parse(event.endAt),
                zone = zone,
            )
        } else {
            val now = Instant.now()
            RecurrenceExpander.expand(
                eventId = event.id,
                title = event.title,
                startAtIso = event.startAt,
                endAtIso = event.endAt,
                linkedTaskId = event.linkedTaskId,
                rule = rule,
                rangeStart = now,
                rangeEnd = now.plus(90, ChronoUnit.DAYS),
                zone = zone,
            ).take(32)
        }
        occurrences.forEach { occurrence ->
            offsets.forEach { offset ->
                scheduleOne(event.id, event.title, occurrence.startAt, offset)
            }
        }
    }

    fun cancelEvent(eventId: String, offsets: List<Int>) {
        val workManager = WorkManager.getInstance(context)
        (offsets + REMINDER_OFFSET_PRESETS).distinct().forEach { offset ->
            workManager.cancelUniqueWork(workName(eventId, offset))
        }
    }

    private fun scheduleOne(eventId: String, title: String, startAtIso: String, offsetMinutes: Int) {
        val start = Instant.parse(startAtIso)
        val trigger = start.minusSeconds(offsetMinutes * 60L)
        val delayMs = java.time.Duration.between(Instant.now(), trigger).toMillis()
        if (delayMs <= 0) return
        val input = Data.Builder()
            .putString(EventReminderWorker.KEY_EVENT_ID, eventId)
            .putString(EventReminderWorker.KEY_TITLE, title)
            .putString(EventReminderWorker.KEY_START_AT, startAtIso)
            .putInt(EventReminderWorker.KEY_OFFSET_MINUTES, offsetMinutes)
            .build()
        val request = OneTimeWorkRequestBuilder<EventReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(input)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(eventId, offsetMinutes),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun showNotification(context: Context, title: String, startAtIso: String, offsetMinutes: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Calendar reminders",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
        val day = runCatching { timeService.toLocalDate(startAtIso).toString() }.getOrDefault("")
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_CALENDAR_DATE, day)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            (title.hashCode() + offsetMinutes),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val whenLabel = if (offsetMinutes == 0) "now" else "in $offsetMinutes min"
        manager.notify(
            (title.hashCode() + offsetMinutes) and 0x7FFFFFFF,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText("Event reminder ($whenLabel)")
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build(),
        )
    }

    private fun workName(eventId: String, offsetMinutes: Int) = "event_reminder_${eventId}_$offsetMinutes"

    companion object {
        const val CHANNEL_ID = "stella_calendar_reminders"
        val REMINDER_OFFSET_PRESETS = listOf(0, 15, 60, 1440)
    }
}
