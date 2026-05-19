package com.stella.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.stella.core.data.EveningReviewRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class EveningReviewWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val eveningReviewRepository: EveningReviewRepository,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (eveningReviewRepository.getToday() == null) {
            EveningReviewScheduler.showNotification(applicationContext)
        }
        return Result.success()
    }
}
