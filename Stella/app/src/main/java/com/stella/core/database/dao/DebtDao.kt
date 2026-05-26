package com.stella.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stella.core.database.entity.DebtEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
    @Query(
        """
        SELECT * FROM debts
        WHERE deletedAt IS NULL AND isResolved = :resolved
        ORDER BY updatedAt DESC
        """,
    )
    fun observeByResolved(resolved: Boolean): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DebtEntity?

    @Query("SELECT * FROM debts WHERE needsSync = 1")
    suspend fun getNeedingSync(): List<DebtEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DebtEntity)

    @Query("DELETE FROM debts")
    suspend fun deleteAll()
}
