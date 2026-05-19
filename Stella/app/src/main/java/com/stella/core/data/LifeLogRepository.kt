package com.stella.core.data

import com.stella.core.database.dao.LifeLogDao
import com.stella.core.database.entity.LifeLogEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

object LifeLogType {
    const val MORNING_UNLOCK = "MORNING_UNLOCK"
    const val EVENING_REVIEW = "EVENING_REVIEW"
    const val HABIT_CHECKIN = "HABIT_CHECKIN"
    const val SYNC = "SYNC"
}

@Singleton
class LifeLogRepository @Inject constructor(
    private val lifeLogDao: LifeLogDao,
) {
    fun observeRecent(limit: Int = 20): Flow<List<LifeLogEntity>> =
        lifeLogDao.observeRecent(limit)

    fun observeRecentLines(limit: Int = 20): Flow<List<String>> =
        lifeLogDao.observeRecent(limit).map { logs ->
            logs.map { "${it.timestamp.take(16)} · ${it.type}" }
        }

    suspend fun append(type: String, payload: String) {
        val now = Instant.now().toString()
        lifeLogDao.upsert(
            LifeLogEntity(
                id = UUID.randomUUID().toString(),
                type = type,
                payload = payload,
                timestamp = now,
                updatedAt = now,
                needsSync = true,
            ),
        )
    }
}
