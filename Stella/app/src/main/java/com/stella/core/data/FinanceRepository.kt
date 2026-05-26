package com.stella.core.data

import com.stella.core.database.dao.DebtDao
import com.stella.core.database.dao.TransactionDao
import com.stella.core.database.entity.DebtEntity
import com.stella.core.database.entity.TransactionEntity
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@Singleton
class FinanceRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val debtDao: DebtDao,
    private val settingsRepository: SettingsRepository,
) {
    fun observeMonthTransactions(
        yearMonth: YearMonth,
        typeFilter: TransactionType?,
    ): Flow<List<TransactionEntity>> {
        val zone = ZoneId.of(settingsRepository.effectiveTimeZoneId())
        val start = yearMonth.atDay(1).atStartOfDay(zone).toInstant().toString()
        val end = yearMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toString()
        return if (typeFilter == null) {
            transactionDao.observeForRange(start, end)
        } else {
            transactionDao.observeForRangeByType(start, end, typeFilter.wire)
        }
    }

    fun observeUnresolvedDebts(): Flow<List<DebtEntity>> =
        debtDao.observeByResolved(resolved = false)

    fun observeMonthSummary(yearMonth: YearMonth): Flow<FinanceMonthSummary> {
        val txFlow = observeMonthTransactions(yearMonth, typeFilter = null)
        val debtFlow = observeUnresolvedDebts()
        return combine(txFlow, debtFlow) { transactions, debts ->
            computeSummary(transactions, debts)
        }
    }

    fun computeSummary(
        transactions: List<TransactionEntity>,
        unresolvedDebts: List<DebtEntity>,
    ): FinanceMonthSummary {
        var ingress = 0.0
        var egress = 0.0
        transactions.forEach { tx ->
            if (tx.type == TransactionType.INGRESS.wire) ingress += tx.amount
            else egress += tx.amount
        }
        var owedToMe = 0.0
        var owedByMe = 0.0
        unresolvedDebts.forEach { debt ->
            if (debt.direction == DebtDirection.OWED_TO_ME.wire) {
                owedToMe += debt.remainingAmount
            } else {
                owedByMe += debt.remainingAmount
            }
        }
        return FinanceMonthSummary(
            ingress = ingress,
            egress = egress,
            netBalance = ingress - egress,
            owedToMe = owedToMe,
            owedByMe = owedByMe,
        )
    }

    suspend fun getTransaction(id: String): TransactionEntity? =
        transactionDao.getById(id)

    suspend fun getDebt(id: String): DebtEntity? =
        debtDao.getById(id)

    suspend fun addTransaction(
        type: TransactionType,
        amount: Double,
        category: String,
        description: String?,
        date: Instant = Instant.now(),
        linkedTaskId: String? = null,
    ): TransactionEntity {
        val now = Instant.now().toString()
        val entity = TransactionEntity(
            id = UUID.randomUUID().toString(),
            type = type.wire,
            amount = amount,
            category = category,
            description = description?.takeIf { it.isNotBlank() },
            date = date.toString(),
            linkedTaskId = linkedTaskId,
            createdAt = now,
            updatedAt = now,
            needsSync = true,
        )
        transactionDao.upsert(entity)
        return entity
    }

    suspend fun updateTransaction(
        id: String,
        type: TransactionType,
        amount: Double,
        category: String,
        description: String?,
    ) {
        val existing = transactionDao.getById(id) ?: return
        transactionDao.upsert(
            existing.copy(
                type = type.wire,
                amount = amount,
                category = category.trim(),
                description = description?.takeIf { it.isNotBlank() },
                updatedAt = Instant.now().toString(),
                needsSync = true,
            ),
        )
    }

    suspend fun deleteTransaction(id: String) {
        val existing = transactionDao.getById(id) ?: return
        val now = Instant.now().toString()
        transactionDao.upsert(
            existing.copy(
                deletedAt = now,
                updatedAt = now,
                needsSync = true,
            ),
        )
    }

    suspend fun addDebt(
        contactName: String,
        direction: DebtDirection,
        totalAmount: Double,
        dueDate: Instant? = null,
        notes: String? = null,
    ): DebtEntity {
        val now = Instant.now().toString()
        val entity = DebtEntity(
            id = UUID.randomUUID().toString(),
            contactName = contactName.trim(),
            direction = direction.wire,
            totalAmount = totalAmount,
            remainingAmount = totalAmount,
            dueDate = dueDate?.toString(),
            notes = notes?.takeIf { it.isNotBlank() },
            isResolved = false,
            createdAt = now,
            updatedAt = now,
            needsSync = true,
        )
        debtDao.upsert(entity)
        return entity
    }

    suspend fun updateDebtRemaining(id: String, remainingAmount: Double) {
        updateDebt(id) { it.copy(remainingAmount = remainingAmount.coerceAtLeast(0.0)) }
    }

    suspend fun updateDebt(
        id: String,
        contactName: String,
        direction: DebtDirection,
        totalAmount: Double,
        remainingAmount: Double,
        notes: String?,
    ) {
        updateDebt(id) {
            it.copy(
                contactName = contactName.trim(),
                direction = direction.wire,
                totalAmount = totalAmount,
                remainingAmount = remainingAmount.coerceIn(0.0, totalAmount),
                notes = notes?.takeIf { it.isNotBlank() },
            )
        }
    }

    suspend fun deleteDebt(id: String) {
        val existing = debtDao.getById(id) ?: return
        val now = Instant.now().toString()
        debtDao.upsert(
            existing.copy(
                deletedAt = now,
                updatedAt = now,
                needsSync = true,
            ),
        )
    }

    suspend fun resolveDebt(id: String) {
        updateDebt(id) {
            it.copy(
                isResolved = true,
                remainingAmount = 0.0,
            )
        }
    }

    suspend fun recordPenaltyEgress(taskId: String, amount: Double, description: String? = null) {
        addTransaction(
            type = TransactionType.EGRESS,
            amount = amount,
            category = FinanceCategories.PENALTY,
            description = description ?: "Task skip penalty",
            linkedTaskId = taskId,
        )
    }

    private suspend fun updateDebt(id: String, transform: (DebtEntity) -> DebtEntity) {
        val existing = debtDao.getById(id) ?: return
        val updated = transform(existing).copy(
            updatedAt = Instant.now().toString(),
            needsSync = true,
        )
        debtDao.upsert(updated)
    }
}
