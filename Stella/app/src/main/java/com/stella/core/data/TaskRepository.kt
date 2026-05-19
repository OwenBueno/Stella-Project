package com.stella.core.data

import com.stella.core.database.dao.TaskDao
import com.stella.core.database.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class TaskStatus { TODO, IN_PROGRESS, DONE }

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
) {
    fun observeTasks(): Flow<List<TaskEntity>> = taskDao.observeActiveTasks()

    suspend fun getById(id: String): TaskEntity? = taskDao.getById(id)

    suspend fun addTask(title: String, scheduledAt: String?) {
        val now = Instant.now().toString()
        taskDao.upsert(
            TaskEntity(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                notes = null,
                scheduledAt = scheduledAt,
                durationMinutes = null,
                status = TaskStatus.TODO.name,
                priority = "HIGH",
                createdAt = now,
                updatedAt = now,
                needsSync = true,
            ),
        )
    }

    suspend fun cycleStatus(id: String) {
        val task = taskDao.getById(id) ?: return
        val next = when (task.status) {
            TaskStatus.TODO.name -> TaskStatus.IN_PROGRESS.name
            TaskStatus.IN_PROGRESS.name -> TaskStatus.DONE.name
            else -> TaskStatus.TODO.name
        }
        taskDao.upsert(
            task.copy(
                status = next,
                updatedAt = Instant.now().toString(),
                needsSync = true,
            ),
        )
    }

    suspend fun deleteTask(id: String) {
        val task = taskDao.getById(id) ?: return
        val now = Instant.now().toString()
        taskDao.upsert(
            task.copy(
                deletedAt = now,
                updatedAt = now,
                needsSync = true,
            ),
        )
    }

    suspend fun updateTask(task: TaskEntity) {
        taskDao.upsert(task.copy(updatedAt = Instant.now().toString(), needsSync = true))
    }
}
