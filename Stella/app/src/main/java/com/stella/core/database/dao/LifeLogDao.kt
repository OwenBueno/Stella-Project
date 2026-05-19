package com.stella.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stella.core.database.entity.LifeLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LifeLogDao {
    @Query("SELECT * FROM life_logs ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<LifeLogEntity>>

    @Query("SELECT * FROM life_logs WHERE needsSync = 1")
    suspend fun getNeedingSync(): List<LifeLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LifeLogEntity)

    @Query("DELETE FROM life_logs")
    suspend fun deleteAll()
}
