package com.stella.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stella.core.database.entity.HabitCheckInEntity
import com.stella.core.database.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE deletedAt IS NULL AND active = 1 ORDER BY sortOrder ASC")
    fun observeActiveHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE needsSync = 1")
    suspend fun getHabitsNeedingSync(): List<HabitEntity>

    @Query("SELECT * FROM habit_check_ins WHERE needsSync = 1")
    suspend fun getCheckInsNeedingSync(): List<HabitCheckInEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabit(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCheckIn(checkIn: HabitCheckInEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabits(habits: List<HabitEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCheckIns(checkIns: List<HabitCheckInEntity>)

    @Query(
        """
        SELECT * FROM habit_check_ins
        WHERE date >= :fromDate AND date <= :toDate
        """,
    )
    fun observeCheckIns(fromDate: String, toDate: String): Flow<List<HabitCheckInEntity>>

    @Query("SELECT * FROM habit_check_ins WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun getCheckIn(habitId: String, date: String): HabitCheckInEntity?

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun getHabit(id: String): HabitEntity?

    @Query("DELETE FROM habit_check_ins WHERE habitId = :habitId AND date = :date")
    suspend fun deleteCheckIn(habitId: String, date: String)

    @Query("DELETE FROM habits")
    suspend fun deleteAllHabits()

    @Query("DELETE FROM habit_check_ins")
    suspend fun deleteAllCheckIns()
}
