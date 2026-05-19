package com.stella.core.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LifeLogWriter @Inject constructor(
    private val lifeLogRepository: LifeLogRepository,
) {
    suspend fun logMorningUnlock(dailyIntentId: String, nfcTagId: String) {
        lifeLogRepository.append(
            LifeLogType.MORNING_UNLOCK,
            """{"dailyIntentId":"$dailyIntentId","nfcTagId":"$nfcTagId"}""",
        )
    }

    suspend fun logEveningReview(eveningReviewId: String) {
        lifeLogRepository.append(
            LifeLogType.EVENING_REVIEW,
            """{"eveningReviewId":"$eveningReviewId"}""",
        )
    }

    suspend fun logSync(direction: String, counts: String) {
        lifeLogRepository.append(
            LifeLogType.SYNC,
            """{"direction":"$direction","counts":"$counts"}""",
        )
    }
}
