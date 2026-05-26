package com.stella

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.stella.sync.EveningReviewScheduler
import com.stella.sync.EventReminderScheduler
import com.stella.sync.MorningAlarmScheduler
import com.stella.sync.MorningAlarmNotifications
import com.stella.sync.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class StellaApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var syncScheduler: SyncScheduler
    @Inject lateinit var eventReminderScheduler: EventReminderScheduler

    override fun onCreate() {
        super.onCreate()
        MorningAlarmNotifications.ensureChannels(this)
        syncScheduler.schedulePeriodic()
        EveningReviewScheduler.schedule(this)
        MorningAlarmScheduler.schedule(this)
        CoroutineScope(Dispatchers.IO).launch {
            eventReminderScheduler.rescheduleAllActive()
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
