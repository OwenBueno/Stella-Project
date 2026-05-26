package com.stella.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stella.core.database.entity.EveningReviewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EveningReviewDao {
    @Query("SELECT * FROM evening_reviews WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): EveningReviewEntity?

    @Query("SELECT * FROM evening_reviews WHERE date = :date LIMIT 1")
    fun observeByDate(date: String): Flow<EveningReviewEntity?>

    @Query("SELECT * FROM evening_reviews WHERE needsSync = 1")
    suspend fun getNeedingSync(): List<EveningReviewEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: EveningReviewEntity)

    @Query("DELETE FROM evening_reviews WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("DELETE FROM evening_reviews")
    suspend fun deleteAll()
}
