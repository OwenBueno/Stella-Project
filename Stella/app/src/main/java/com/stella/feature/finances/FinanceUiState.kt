package com.stella.feature.finances

import com.stella.core.data.DebtDirection
import com.stella.core.data.FinanceMonthSummary
import com.stella.core.data.TransactionType
import com.stella.core.database.entity.DebtEntity
import com.stella.core.database.entity.TransactionEntity
import java.time.YearMonth

enum class TransactionFilter {
    ALL,
    INGRESS,
    EGRESS,
}

data class FinanceUiState(
    val isLoading: Boolean = true,
    val selectedMonth: YearMonth = YearMonth.now(),
    val isCurrentMonth: Boolean = true,
    val filter: TransactionFilter = TransactionFilter.ALL,
    val summary: FinanceMonthSummary = FinanceMonthSummary(0.0, 0.0, 0.0, 0.0, 0.0),
    val transactions: List<TransactionEntity> = emptyList(),
    val unresolvedDebts: List<DebtEntity> = emptyList(),
    val showEntrySheet: Boolean = false,
    val editingTransactionId: String? = null,
    val draftType: TransactionType = TransactionType.EGRESS,
    val draftAmount: String = "",
    val draftCategory: String = "",
    val draftDescription: String = "",
    val showDebtSheet: Boolean = false,
    val editingDebtId: String? = null,
    val draftContactName: String = "",
    val draftDebtDirection: DebtDirection = DebtDirection.OWED_BY_ME,
    val draftDebtTotal: String = "",
    val draftDebtRemaining: String = "",
    val draftNotes: String = "",
    val error: String? = null,
)

sealed interface FinanceUiEvent {
    data object PrevMonth : FinanceUiEvent
    data object NextMonth : FinanceUiEvent
    data object JumpToCurrentMonth : FinanceUiEvent
    data class FilterChanged(val filter: TransactionFilter) : FinanceUiEvent
    data object OpenEntrySheet : FinanceUiEvent
    data class OpenEditTransaction(val transactionId: String) : FinanceUiEvent
    data object CloseEntrySheet : FinanceUiEvent
    data object DeleteTransaction : FinanceUiEvent
    data class DraftTypeChanged(val type: TransactionType) : FinanceUiEvent
    data class DraftAmountChanged(val value: String) : FinanceUiEvent
    data class DraftCategoryChanged(val value: String) : FinanceUiEvent
    data class DraftDescriptionChanged(val value: String) : FinanceUiEvent
    data object SaveEntry : FinanceUiEvent
    data object OpenDebtSheet : FinanceUiEvent
    data class OpenEditDebt(val debtId: String) : FinanceUiEvent
    data object CloseDebtSheet : FinanceUiEvent
    data object DeleteDebt : FinanceUiEvent
    data class DraftContactChanged(val value: String) : FinanceUiEvent
    data class DraftDebtDirectionChanged(val direction: DebtDirection) : FinanceUiEvent
    data class DraftDebtTotalChanged(val value: String) : FinanceUiEvent
    data class DraftDebtRemainingChanged(val value: String) : FinanceUiEvent
    data class DraftNotesChanged(val value: String) : FinanceUiEvent
    data object SaveDebt : FinanceUiEvent
    data class ResolveDebt(val debtId: String) : FinanceUiEvent
}
