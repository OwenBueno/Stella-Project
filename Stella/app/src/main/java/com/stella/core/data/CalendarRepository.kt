package com.stella.core.data

import com.stella.core.database.dao.CalendarEventDao
import com.stella.core.database.entity.CalendarEventEntity
import com.stella.core.util.TimeService
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarRepository @Inject constructor(
    private val calendarEventDao: CalendarEventDao,
    private val timeService: TimeService,
) {
    fun observeMonth(month: YearMonth): Flow<List<CalendarEventEntity>> {
        val (from, to) = timeService.monthQueryRange(month)
        return calendarEventDao.observeInRange(from, to)
    }

    suspend fun addEvent(title: String, startAt: String, endAt: String) {
        addEvent(title, startAt, endAt, linkedTaskId = null)
    }

    suspend fun addEvent(
        title: String,
        startAt: String,
        endAt: String,
        linkedTaskId: String?,
    ) {
        val now = Instant.now().toString()
        calendarEventDao.upsert(
            CalendarEventEntity(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                startAt = startAt,
                endAt = endAt,
                linkedTaskId = linkedTaskId,
                createdAt = now,
                updatedAt = now,
                needsSync = true,
            ),
        )
    }
}
