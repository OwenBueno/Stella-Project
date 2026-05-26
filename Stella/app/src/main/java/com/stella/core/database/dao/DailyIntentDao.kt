package com.stella.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stella.core.database.entity.DailyIntentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyIntentDao {
    @Query("SELECT * FROM daily_intents WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): DailyIntentEntity?

    @Query("SELECT * FROM daily_intents WHERE date = :date LIMIT 1")
    fun observeByDate(date: String): Flow<DailyIntentEntity?>

    @Query("SELECT * FROM daily_intents WHERE needsSync = 1")
    suspend fun getNeedingSync(): List<DailyIntentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DailyIntentEntity)

    @Query("DELETE FROM daily_intents WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("DELETE FROM daily_intents")
    suspend fun deleteAll()
}
