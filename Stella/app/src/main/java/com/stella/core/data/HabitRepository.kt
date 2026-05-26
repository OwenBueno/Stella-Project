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

data class CheckInUi(
    val status: CheckInStatus,
    val completedAt: String?,
)

data class HabitWithCheckIns(
    val habit: HabitEntity,
    val checkIns: Map<String, CheckInUi>,
)

@Singleton
class HabitRepository @Inject constructor(
    private val habitDao: HabitDao,
) {
    fun observeActiveHabits(): Flow<List<HabitEntity>> = habitDao.observeActiveHabits()

    fun observeCheckIns(fromDate: String, toDate: String): Flow<List<HabitCheckInEntity>> =
        habitDao.observeCheckIns(fromDate, toDate)

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
                    .associate {
                        it.date to CheckInUi(
                            status = CheckInStatus.valueOf(it.status),
                            completedAt = it.completedAt,
                        )
                    }
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

    suspend fun updateHabitName(habitId: String, name: String) {
        val habit = habitDao.getHabit(habitId) ?: return
        val now = Instant.now().toString()
        habitDao.upsertHabit(
            habit.copy(
                name = name.trim(),
                updatedAt = now,
                needsSync = true,
            ),
        )
    }

    suspend fun deleteHabit(habitId: String) {
        val habit = habitDao.getHabit(habitId) ?: return
        val now = Instant.now().toString()
        habitDao.upsertHabit(
            habit.copy(
                active = false,
                deletedAt = now,
                updatedAt = now,
                needsSync = true,
            ),
        )
    }

    suspend fun toggleCheckIn(habitId: String, date: LocalDate) {
        val dateStr = DateUtils.formatDate(date)
        val existing = habitDao.getCheckIn(habitId, dateStr)
        if (existing?.status == CheckInStatus.DONE.name) {
            habitDao.deleteCheckIn(habitId, dateStr)
            return
        }
        val now = Instant.now().toString()
        habitDao.upsertCheckIn(
            HabitCheckInEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                habitId = habitId,
                date = dateStr,
                status = CheckInStatus.DONE.name,
                completedAt = now,
                updatedAt = now,
                needsSync = true,
            ),
        )
    }

    suspend fun setCheckInStatus(habitId: String, date: LocalDate, status: CheckInStatus) {
        val dateStr = DateUtils.formatDate(date)
        val existing = habitDao.getCheckIn(habitId, dateStr)
        val now = Instant.now().toString()
        val completedAt = if (status == CheckInStatus.DONE) now else null
        habitDao.upsertCheckIn(
            HabitCheckInEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                habitId = habitId,
                date = dateStr,
                status = status.name,
                completedAt = completedAt,
                updatedAt = now,
                needsSync = true,
            ),
        )
    }
}
