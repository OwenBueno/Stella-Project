package com.stella.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stella.core.database.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query(
        """
        SELECT * FROM tasks WHERE deletedAt IS NULL AND status != 'DONE'
        ORDER BY sortOrder ASC, scheduledAt ASC
        """,
    )
    fun observeActiveTasks(): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks WHERE deletedAt IS NULL AND status = 'DONE'
        ORDER BY updatedAt DESC
        """,
    )
    fun observeCompletedTasks(): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks WHERE deletedAt IS NULL AND status = 'DONE'
        AND updatedAt >= :fromIso AND updatedAt <= :toIso
        ORDER BY updatedAt ASC
        """,
    )
    fun observeCompletedInRange(fromIso: String, toIso: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE needsSync = 1")
    suspend fun getNeedingSync(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()
}
