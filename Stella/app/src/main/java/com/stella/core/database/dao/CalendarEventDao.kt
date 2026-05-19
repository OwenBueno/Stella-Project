package com.stella.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stella.core.database.entity.CalendarEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarEventDao {
    @Query(
        """
        SELECT * FROM calendar_events
        WHERE deletedAt IS NULL AND startAt >= :from AND startAt <= :to
        ORDER BY startAt ASC
        """,
    )
    fun observeInRange(from: String, to: String): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE needsSync = 1")
    suspend fun getNeedingSync(): List<CalendarEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: CalendarEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(events: List<CalendarEventEntity>)

    @Query("DELETE FROM calendar_events")
    suspend fun deleteAll()
}
