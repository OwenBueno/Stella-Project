package com.stella.core.data

import com.stella.core.database.dao.TaskDao
import com.stella.core.database.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class TaskStatus { TODO, IN_PROGRESS, DONE }

data class TaskLists(
    val active: List<TaskEntity>,
    val completed: List<TaskEntity>,
)

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
) {
    fun observeTaskLists(): Flow<TaskLists> = combine(
        taskDao.observeActiveTasks(),
        taskDao.observeCompletedTasks(),
    ) { active, completed -> TaskLists(active, completed) }

    fun observeTasks(): Flow<List<TaskEntity>> = combine(
        taskDao.observeActiveTasks(),
        taskDao.observeCompletedTasks(),
    ) { active, completed -> active + completed }

    suspend fun getById(id: String): TaskEntity? = taskDao.getById(id)

    suspend fun addTask(title: String, scheduledAt: String?): String {
        val now = Instant.now().toString()
        val id = UUID.randomUUID().toString()
        val nextOrder = nextActiveSortOrder()
        taskDao.upsert(
            TaskEntity(
                id = id,
                title = title.trim(),
                notes = null,
                scheduledAt = scheduledAt,
                durationMinutes = null,
                status = TaskStatus.TODO.name,
                sortOrder = nextOrder,
                priority = "HIGH",
                createdAt = now,
                updatedAt = now,
                needsSync = true,
            ),
        )
        return id
    }

    suspend fun cycleStatus(id: String) {
        val task = taskDao.getById(id) ?: return
        val next = when (task.status) {
            TaskStatus.TODO.name -> TaskStatus.IN_PROGRESS.name
            TaskStatus.IN_PROGRESS.name -> TaskStatus.DONE.name
            else -> TaskStatus.TODO.name
        }
        val now = Instant.now().toString()
        var updated = task.copy(status = next, updatedAt = now, needsSync = true)
        if (task.status == TaskStatus.DONE.name && next != TaskStatus.DONE.name) {
            updated = updated.copy(sortOrder = nextActiveSortOrder())
        }
        taskDao.upsert(updated)
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

    suspend fun reorderActiveTasks(visibleIdsInNewOrder: List<String>) {
        if (visibleIdsInNewOrder.isEmpty()) return
        val active = taskDao.observeActiveTasks().first()
        val visibleSet = visibleIdsInNewOrder.toSet()
        var visibleIndex = 0
        val reordered = active.map { task ->
            if (task.id in visibleSet) {
                val id = visibleIdsInNewOrder[visibleIndex++]
                active.first { it.id == id }
            } else {
                task
            }
        }
        val now = Instant.now().toString()
        reordered.forEachIndexed { index, task ->
            taskDao.upsert(
                task.copy(
                    sortOrder = index,
                    updatedAt = now,
                    needsSync = true,
                ),
            )
        }
    }

    private suspend fun nextActiveSortOrder(): Int {
        val active = taskDao.observeActiveTasks().first()
        return (active.maxOfOrNull { it.sortOrder } ?: -1) + 1
    }
}
