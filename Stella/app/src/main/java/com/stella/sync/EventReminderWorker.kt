package com.stella.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class EventReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val eventReminderScheduler: EventReminderScheduler,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: return Result.success()
        val startAt = inputData.getString(KEY_START_AT) ?: return Result.success()
        val offset = inputData.getInt(KEY_OFFSET_MINUTES, 0)
        eventReminderScheduler.showNotification(applicationContext, title, startAt, offset)
        return Result.success()
    }

    companion object {
        const val KEY_EVENT_ID = "event_id"
        const val KEY_TITLE = "title"
        const val KEY_START_AT = "start_at"
        const val KEY_OFFSET_MINUTES = "offset_minutes"
    }
}
