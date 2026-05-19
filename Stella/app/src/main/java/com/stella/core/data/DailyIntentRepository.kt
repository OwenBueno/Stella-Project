package com.stella.core.data

import com.stella.core.database.dao.DailyIntentDao
import com.stella.core.database.entity.DailyIntentEntity
import com.stella.core.util.DateUtils
import com.stella.core.util.TimeService
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyIntentRepository @Inject constructor(
    private val dailyIntentDao: DailyIntentDao,
    private val timeService: TimeService,
) {
    fun observeToday(): Flow<DailyIntentEntity?> =
        dailyIntentDao.observeByDate(DateUtils.formatDate(timeService.today()))

    suspend fun hasCompletedToday(): Boolean =
        dailyIntentDao.getByDate(DateUtils.formatDate(timeService.today())) != null

    suspend fun saveIntent(top3TaskIds: List<String>, nfcTagId: String): String {
        val now = Instant.now().toString()
        val date = DateUtils.formatDate(timeService.today())
        val existing = dailyIntentDao.getByDate(date)
        val id = existing?.id ?: UUID.randomUUID().toString()
        dailyIntentDao.upsert(
            DailyIntentEntity(
                id = id,
                date = date,
                top3TaskIds = top3TaskIds,
                completedAt = now,
                nfcTagId = nfcTagId,
                updatedAt = now,
                needsSync = true,
            ),
        )
        return id
    }
}
