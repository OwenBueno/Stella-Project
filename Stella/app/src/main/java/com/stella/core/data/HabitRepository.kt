package com.stella.core.data

import com.stella.core.database.dao.HabitDao
import com.stella.core.database.entity.HabitCheckInEntity
import com.stella.core.database.entity.HabitEntity
import com.stella.core.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class CheckInStatus { DONE, MISSED }

data class HabitWithCheckIns(
    val habit: HabitEntity,
    val checkIns: Map<String, CheckInStatus>,
)

@Singleton
class HabitRepository @Inject constructor(
    private val habitDao: HabitDao,
) {
    fun observeHabitsWithCheckIns(
        weekStart: LocalDate,
        days: Int = 7,
    ): Flow<List<HabitWithCheckIns>> {
        val from = DateUtils.formatDate(weekStart)
        val to = DateUtils.formatDate(weekStart.plusDays((days - 1).toLong()))
        return combine(
            habitDao.observeActiveHabits(),
            habitDao.observeCheckIns(from, to),
        ) { habits, checkIns ->
            habits.map { habit ->
                val byDate = checkIns
                    .filter { it.habitId == habit.id }
                    .associate { it.date to CheckInStatus.valueOf(it.status) }
                HabitWithCheckIns(habit, byDate)
            }
        }
    }

    suspend fun addHabit(name: String, sortOrder: Int) {
        val now = Instant.now().toString()
        habitDao.upsertHabit(
            HabitEntity(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                sortOrder = sortOrder,
                active = true,
                createdAt = now,
                updatedAt = now,
                needsSync = true,
            ),
        )
    }

    suspend fun toggleCheckIn(habitId: String, date: LocalDate) {
        val dateStr = DateUtils.formatDate(date)
        val existing = habitDao.getCheckIn(habitId, dateStr)
        val now = Instant.now().toString()
        val newStatus = when (existing?.status) {
            CheckInStatus.DONE.name -> CheckInStatus.MISSED.name
            CheckInStatus.MISSED.name -> CheckInStatus.DONE.name
            else -> CheckInStatus.DONE.name
        }
        habitDao.upsertCheckIn(
            HabitCheckInEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                habitId = habitId,
                date = dateStr,
                status = newStatus,
                updatedAt = now,
                needsSync = true,
            ),
        )
    }

    suspend fun setCheckInStatus(habitId: String, date: LocalDate, status: CheckInStatus) {
        val dateStr = DateUtils.formatDate(date)
        val existing = habitDao.getCheckIn(habitId, dateStr)
        val now = Instant.now().toString()
        habitDao.upsertCheckIn(
            HabitCheckInEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                habitId = habitId,
                date = dateStr,
                status = status.name,
                updatedAt = now,
                needsSync = true,
            ),
        )
    }
}
