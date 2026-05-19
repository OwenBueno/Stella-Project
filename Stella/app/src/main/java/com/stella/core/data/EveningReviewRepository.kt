package com.stella.core.data

import com.stella.core.database.dao.EveningReviewDao
import com.stella.core.database.entity.EveningReviewEntity
import com.stella.core.util.DateUtils
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EveningReviewRepository @Inject constructor(
    private val eveningReviewDao: EveningReviewDao,
) {
    fun observeToday(): Flow<EveningReviewEntity?> =
        eveningReviewDao.observeByDate(DateUtils.formatDate(DateUtils.today()))

    suspend fun getToday(): EveningReviewEntity? =
        eveningReviewDao.getByDate(DateUtils.formatDate(DateUtils.today()))

    suspend fun saveReview(
        plannedVsActual: String,
        reflectionText: String,
        habitGridSnapshot: String,
    ) {
        val now = Instant.now().toString()
        val date = DateUtils.formatDate(DateUtils.today())
        val existing = eveningReviewDao.getByDate(date)
        eveningReviewDao.upsert(
            EveningReviewEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                date = date,
                plannedVsActual = plannedVsActual.trim(),
                reflectionText = reflectionText.trim(),
                habitGridSnapshot = habitGridSnapshot,
                completedAt = now,
                updatedAt = now,
                needsSync = true,
            ),
        )
    }
}
