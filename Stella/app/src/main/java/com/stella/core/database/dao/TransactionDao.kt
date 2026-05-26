package com.stella.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stella.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query(
        """
        SELECT * FROM transactions
        WHERE deletedAt IS NULL
        AND date >= :startInclusive AND date < :endExclusive
        ORDER BY date DESC
        """,
    )
    fun observeForRange(startInclusive: String, endExclusive: String): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE deletedAt IS NULL
        AND date >= :startInclusive AND date < :endExclusive
        AND type = :type
        ORDER BY date DESC
        """,
    )
    fun observeForRangeByType(
        startInclusive: String,
        endExclusive: String,
        type: String,
    ): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE needsSync = 1")
    suspend fun getNeedingSync(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
