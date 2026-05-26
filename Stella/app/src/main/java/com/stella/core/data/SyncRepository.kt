package com.stella.core.data

import com.stella.core.database.dao.CalendarEventDao
import com.stella.core.database.dao.DailyIntentDao
import com.stella.core.database.dao.DebtDao
import com.stella.core.database.dao.EveningReviewDao
import com.stella.core.database.dao.HabitDao
import com.stella.core.database.dao.LifeLogDao
import com.stella.core.database.dao.SyncMetaDao
import com.stella.core.database.dao.TaskDao
import com.stella.core.database.dao.TransactionDao
import com.stella.core.database.entity.CalendarEventEntity
import com.stella.core.database.entity.DailyIntentEntity
import com.stella.core.database.entity.DebtEntity
import com.stella.core.database.entity.EveningReviewEntity
import com.stella.core.database.entity.HabitCheckInEntity
import com.stella.core.database.entity.HabitEntity
import com.stella.core.database.entity.LifeLogEntity
import com.stella.core.database.entity.SyncMetaEntity
import com.stella.core.database.entity.TaskEntity
import com.stella.core.database.entity.TransactionEntity
import com.stella.core.network.StellaApi
import com.stella.core.network.SyncCheckInDto
import com.stella.core.network.SyncDailyIntentDto
import com.stella.core.network.SyncDebtDto
import com.stella.core.network.SyncEventDto
import com.stella.core.network.SyncEveningReviewDto
import com.stella.core.network.SyncHabitDto
import com.stella.core.network.SyncLifeLogDto
import com.stella.core.network.SyncPushRequest
import com.stella.core.network.SyncTaskDto
import com.stella.core.network.SyncTransactionDto
import com.stella.sync.EventReminderScheduler
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val api: StellaApi,
    private val habitDao: HabitDao,
    private val taskDao: TaskDao,
    private val calendarEventDao: CalendarEventDao,
    private val dailyIntentDao: DailyIntentDao,
    private val eveningReviewDao: EveningReviewDao,
    private val lifeLogDao: LifeLogDao,
    private val transactionDao: TransactionDao,
    private val debtDao: DebtDao,
    private val syncMetaDao: SyncMetaDao,
    private val lifeLogWriter: LifeLogWriter,
    private val eventReminderScheduler: EventReminderScheduler,
) {
    suspend fun syncNow(): Result<String> = runCatching {
        val meta = ensureMeta()
        val pushed = push(meta)
        pull(meta)
        lifeLogWriter.logSync("push+pull", pushed)
        "Sync complete"
    }

    suspend fun purgeLocal() {
        habitDao.deleteAllHabits()
        habitDao.deleteAllCheckIns()
        taskDao.deleteAll()
        calendarEventDao.deleteAll()
        dailyIntentDao.deleteAll()
        eveningReviewDao.deleteAll()
        lifeLogDao.deleteAll()
        transactionDao.deleteAll()
        debtDao.deleteAll()
    }

    private suspend fun ensureMeta(): SyncMetaEntity {
        val existing = syncMetaDao.get()
        if (existing != null) return existing
        val meta = SyncMetaEntity(
            deviceId = UUID.randomUUID().toString(),
            lastPushedAt = null,
            lastPulledAt = null,
        )
        syncMetaDao.upsert(meta)
        return meta
    }

    private suspend fun push(meta: SyncMetaEntity): String {
        val habitEntities = habitDao.getHabitsNeedingSync()
        val checkInEntities = habitDao.getCheckInsNeedingSync()
        val taskEntities = taskDao.getNeedingSync()
        val eventEntities = calendarEventDao.getNeedingSync()
        val intentEntities = dailyIntentDao.getNeedingSync()
        val reviewEntities = eveningReviewDao.getNeedingSync()
        val logEntities = lifeLogDao.getNeedingSync()
        val transactionEntities = transactionDao.getNeedingSync()
        val debtEntities = debtDao.getNeedingSync()

        val hasWork = habitEntities.isNotEmpty() || checkInEntities.isNotEmpty() ||
            taskEntities.isNotEmpty() || eventEntities.isNotEmpty() ||
            intentEntities.isNotEmpty() || reviewEntities.isNotEmpty() ||
            logEntities.isNotEmpty() || transactionEntities.isNotEmpty() ||
            debtEntities.isNotEmpty()

        if (!hasWork) return "nothing to push"

        val pushedAt = Instant.now().toString()
        api.syncPush(
            SyncPushRequest(
                deviceId = meta.deviceId,
                pushedAt = pushedAt,
                habits = habitEntities.map { it.toDto() },
                habitCheckIns = checkInEntities.map { it.toDto() },
                tasks = taskEntities.map { it.toDto() },
                events = eventEntities.map { it.toDto() },
                dailyIntents = intentEntities.map { it.toDto() },
                eveningReviews = reviewEntities.map { it.toDto() },
                lifeLogs = logEntities.map { it.toDto() },
                transactions = transactionEntities.map { it.toDto() },
                debts = debtEntities.map { it.toDto() },
            ),
        )

        habitEntities.forEach { habitDao.upsertHabit(it.copy(needsSync = false)) }
        checkInEntities.forEach { habitDao.upsertCheckIn(it.copy(needsSync = false)) }
        taskEntities.forEach { taskDao.upsert(it.copy(needsSync = false)) }
        eventEntities.forEach { calendarEventDao.upsert(it.copy(needsSync = false)) }
        intentEntities.forEach { dailyIntentDao.upsert(it.copy(needsSync = false)) }
        reviewEntities.forEach { eveningReviewDao.upsert(it.copy(needsSync = false)) }
        logEntities.forEach { lifeLogDao.upsert(it.copy(needsSync = false)) }
        transactionEntities.forEach { transactionDao.upsert(it.copy(needsSync = false)) }
        debtEntities.forEach { debtDao.upsert(it.copy(needsSync = false)) }

        syncMetaDao.upsert(meta.copy(lastPushedAt = pushedAt))
        return "h${habitEntities.size} t${taskEntities.size} i${intentEntities.size}"
    }

    private suspend fun pull(meta: SyncMetaEntity) {
        val response = api.syncPull(meta.lastPulledAt)
        response.habits.forEach { dto ->
            habitDao.upsertHabit(
                HabitEntity(
                    id = dto.id,
                    name = dto.name,
                    sortOrder = dto.sortOrder,
                    active = dto.active,
                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt,
                    deletedAt = dto.deletedAt,
                    needsSync = false,
                ),
            )
        }
        response.habitCheckIns.forEach { dto ->
            habitDao.upsertCheckIn(
                HabitCheckInEntity(
                    id = dto.id,
                    habitId = dto.habitId,
                    date = dto.date,
                    status = dto.status,
                    completedAt = dto.completedAt,
                    updatedAt = dto.updatedAt,
                    needsSync = false,
                ),
            )
        }
        response.tasks.forEach { dto ->
            taskDao.upsert(
                TaskEntity(
                    id = dto.id,
                    title = dto.title,
                    notes = dto.notes,
                    scheduledAt = dto.scheduledAt,
                    durationMinutes = dto.durationMinutes,
                    status = dto.status,
                    sortOrder = dto.sortOrder,
                    priority = dto.priority,
                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt,
                    deletedAt = dto.deletedAt,
                    needsSync = false,
                ),
            )
        }
        response.events.forEach { dto ->
            calendarEventDao.upsert(
                CalendarEventEntity(
                    id = dto.id,
                    title = dto.title,
                    startAt = dto.startAt,
                    endAt = dto.endAt,
                    linkedTaskId = dto.linkedTaskId,
                    recurrenceRuleJson = dto.recurrenceRuleJson,
                    reminderOffsetsJson = dto.reminderOffsetsJson,
                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt,
                    deletedAt = dto.deletedAt,
                    needsSync = false,
                ),
            )
        }
        response.dailyIntents.forEach { dto ->
            dailyIntentDao.upsert(
                DailyIntentEntity(
                    id = dto.id,
                    date = dto.date,
                    plannedTaskIds = dto.plannedTaskIds,
                    completedAt = dto.completedAt,
                    nfcTagId = dto.nfcTagId,
                    updatedAt = dto.updatedAt,
                    needsSync = false,
                ),
            )
        }
        response.eveningReviews.forEach { dto ->
            eveningReviewDao.upsert(
                EveningReviewEntity(
                    id = dto.id,
                    date = dto.date,
                    plannedVsActual = dto.plannedVsActual,
                    reflectionText = dto.reflectionText,
                    habitGridSnapshot = dto.habitGridSnapshot.toString(),
                    completedAt = dto.completedAt,
                    updatedAt = dto.updatedAt,
                    needsSync = false,
                ),
            )
        }
        response.lifeLogs.forEach { dto ->
            lifeLogDao.upsert(
                LifeLogEntity(
                    id = dto.id,
                    type = dto.type,
                    payload = dto.payload,
                    timestamp = dto.timestamp,
                    updatedAt = dto.updatedAt,
                    needsSync = false,
                ),
            )
        }
        response.transactions.forEach { dto ->
            transactionDao.upsert(
                TransactionEntity(
                    id = dto.id,
                    type = dto.type,
                    amount = dto.amount,
                    category = dto.category,
                    description = dto.description,
                    date = dto.date,
                    linkedTaskId = dto.linkedTaskId,
                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt,
                    deletedAt = dto.deletedAt,
                    needsSync = false,
                ),
            )
        }
        response.debts.forEach { dto ->
            debtDao.upsert(
                DebtEntity(
                    id = dto.id,
                    contactName = dto.contactName,
                    direction = dto.direction,
                    totalAmount = dto.totalAmount,
                    remainingAmount = dto.remainingAmount,
                    dueDate = dto.dueDate,
                    notes = dto.notes,
                    isResolved = dto.isResolved,
                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt,
                    deletedAt = dto.deletedAt,
                    needsSync = false,
                ),
            )
        }
        syncMetaDao.upsert(meta.copy(lastPulledAt = response.serverTime))
        eventReminderScheduler.rescheduleAllActive()
    }

    private fun HabitEntity.toDto() = SyncHabitDto(
        id = id,
        name = name,
        sortOrder = sortOrder,
        active = active,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )

    private fun HabitCheckInEntity.toDto() = SyncCheckInDto(
        id = id,
        habitId = habitId,
        date = date,
        status = status,
        updatedAt = updatedAt,
        completedAt = completedAt,
    )

    private fun TaskEntity.toDto() = SyncTaskDto(
        id = id,
        title = title,
        notes = notes,
        scheduledAt = scheduledAt,
        durationMinutes = durationMinutes,
        status = status,
        sortOrder = sortOrder,
        priority = priority,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )

    private fun CalendarEventEntity.toDto() = SyncEventDto(
        id = id,
        title = title,
        startAt = startAt,
        endAt = endAt,
        linkedTaskId = linkedTaskId,
        recurrenceRuleJson = recurrenceRuleJson,
        reminderOffsetsJson = reminderOffsetsJson,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )

    private fun DailyIntentEntity.toDto() = SyncDailyIntentDto(
        id = id,
        date = date,
        plannedTaskIds = plannedTaskIds,
        completedAt = completedAt,
        nfcTagId = nfcTagId,
        updatedAt = updatedAt,
    )

    private fun EveningReviewEntity.toDto() = SyncEveningReviewDto(
        id = id,
        date = date,
        plannedVsActual = plannedVsActual,
        reflectionText = reflectionText,
        habitGridSnapshot = kotlinx.serialization.json.Json.parseToJsonElement(habitGridSnapshot),
        completedAt = completedAt,
        updatedAt = updatedAt,
    )

    private fun LifeLogEntity.toDto() = SyncLifeLogDto(
        id = id,
        type = type,
        payload = payload,
        timestamp = timestamp,
        updatedAt = updatedAt,
    )

    private fun TransactionEntity.toDto() = SyncTransactionDto(
        id = id,
        type = type,
        amount = amount,
        category = category,
        description = description,
        date = date,
        linkedTaskId = linkedTaskId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )

    private fun DebtEntity.toDto() = SyncDebtDto(
        id = id,
        contactName = contactName,
        direction = direction,
        totalAmount = totalAmount,
        remainingAmount = remainingAmount,
        dueDate = dueDate,
        notes = notes,
        isResolved = isResolved,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}
