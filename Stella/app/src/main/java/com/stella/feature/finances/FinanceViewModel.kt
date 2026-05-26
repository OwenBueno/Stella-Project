package com.stella.feature.finances

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stella.core.data.DebtDirection
import com.stella.core.data.FinanceCategories
import com.stella.core.data.FinanceRepository
import com.stella.core.data.TransactionType
import com.stella.core.util.TimeService
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val financeRepository: FinanceRepository,
    private val timeService: TimeService,
) : ViewModel() {

    private val currentMonth = YearMonth.from(timeService.today())
    private val _state = MutableStateFlow(
        FinanceUiState(
            selectedMonth = currentMonth,
            isCurrentMonth = true,
        ),
    )
    val state: StateFlow<FinanceUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state
                .map { it.selectedMonth to it.filter }
                .distinctUntilChanged()
                .flatMapLatest { (month, filter) ->
                    val typeFilter = when (filter) {
                        TransactionFilter.ALL -> null
                        TransactionFilter.INGRESS -> TransactionType.INGRESS
                        TransactionFilter.EGRESS -> TransactionType.EGRESS
                    }
                    combine(
                        financeRepository.observeMonthTransactions(month, typeFilter),
                        financeRepository.observeMonthSummary(month),
                        financeRepository.observeUnresolvedDebts(),
                    ) { transactions, summary, debts ->
                        Triple(transactions, summary, debts)
                    }
                }
                .collect { (transactions, summary, debts) ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            transactions = transactions,
                            summary = summary,
                            unresolvedDebts = debts,
                        )
                    }
                }
        }
    }

    fun onEvent(event: FinanceUiEvent) {
        when (event) {
            FinanceUiEvent.PrevMonth -> shiftMonth(-1)
            FinanceUiEvent.NextMonth -> shiftMonth(1)
            FinanceUiEvent.JumpToCurrentMonth -> _state.update {
                it.copy(selectedMonth = currentMonth, isCurrentMonth = true)
            }
            is FinanceUiEvent.FilterChanged -> _state.update { it.copy(filter = event.filter) }
            FinanceUiEvent.OpenEntrySheet -> openCreateTransactionSheet()
            is FinanceUiEvent.OpenEditTransaction -> openEditTransactionSheet(event.transactionId)
            FinanceUiEvent.CloseEntrySheet -> _state.update { it.copy(showEntrySheet = false, editingTransactionId = null, error = null) }
            FinanceUiEvent.DeleteTransaction -> deleteTransaction()
            is FinanceUiEvent.DraftTypeChanged -> _state.update { current ->
                val previousType = current.draftType
                val newType = event.type
                val category = when {
                    current.draftCategory.isBlank() -> FinanceCategories.defaultFor(newType)
                    FinanceCategories.matchesType(current.draftCategory, previousType) ->
                        FinanceCategories.defaultFor(newType)
                    else -> current.draftCategory
                }
                current.copy(draftType = newType, draftCategory = category)
            }
            is FinanceUiEvent.DraftAmountChanged -> _state.update { it.copy(draftAmount = event.value) }
            is FinanceUiEvent.DraftCategoryChanged -> _state.update { it.copy(draftCategory = event.value) }
            is FinanceUiEvent.DraftDescriptionChanged -> _state.update { it.copy(draftDescription = event.value) }
            FinanceUiEvent.SaveEntry -> saveTransaction()
            FinanceUiEvent.OpenDebtSheet -> openCreateDebtSheet()
            is FinanceUiEvent.OpenEditDebt -> openEditDebtSheet(event.debtId)
            FinanceUiEvent.CloseDebtSheet -> _state.update { it.copy(showDebtSheet = false, editingDebtId = null, error = null) }
            FinanceUiEvent.DeleteDebt -> deleteDebt()
            is FinanceUiEvent.DraftContactChanged -> _state.update { it.copy(draftContactName = event.value) }
            is FinanceUiEvent.DraftDebtDirectionChanged -> _state.update { it.copy(draftDebtDirection = event.direction) }
            is FinanceUiEvent.DraftDebtTotalChanged -> _state.update { it.copy(draftDebtTotal = event.value) }
            is FinanceUiEvent.DraftDebtRemainingChanged -> _state.update { it.copy(draftDebtRemaining = event.value) }
            is FinanceUiEvent.DraftNotesChanged -> _state.update { it.copy(draftNotes = event.value) }
            FinanceUiEvent.SaveDebt -> saveDebt()
            is FinanceUiEvent.ResolveDebt -> viewModelScope.launch {
                financeRepository.resolveDebt(event.debtId)
            }
        }
    }

    private fun shiftMonth(delta: Int) {
        _state.update {
            val month = it.selectedMonth.plusMonths(delta.toLong())
            it.copy(selectedMonth = month, isCurrentMonth = month == currentMonth)
        }
    }

    private fun openCreateTransactionSheet() {
        _state.update {
            it.copy(
                showEntrySheet = true,
                editingTransactionId = null,
                draftType = TransactionType.EGRESS,
                draftAmount = "",
                draftCategory = FinanceCategories.defaultFor(TransactionType.EGRESS),
                draftDescription = "",
                error = null,
            )
        }
    }

    private fun openEditTransactionSheet(transactionId: String) {
        viewModelScope.launch {
            val tx = financeRepository.getTransaction(transactionId) ?: return@launch
            val type = TransactionType.entries.firstOrNull { it.wire == tx.type } ?: TransactionType.EGRESS
            _state.update {
                it.copy(
                    showEntrySheet = true,
                    editingTransactionId = tx.id,
                    draftType = type,
                    draftAmount = formatDraftAmount(tx.amount),
                    draftCategory = tx.category,
                    draftDescription = tx.description.orEmpty(),
                    error = null,
                )
            }
        }
    }

    private fun saveTransaction() {
        viewModelScope.launch {
            val current = _state.value
            val amount = current.draftAmount.toDoubleOrNull()
            if (amount == null || amount <= 0.0) {
                _state.update { it.copy(error = "Enter a valid amount.", showEntrySheet = true) }
                return@launch
            }
            val category = current.draftCategory.trim()
            if (category.isBlank()) {
                _state.update { it.copy(error = "Enter a category.", showEntrySheet = true) }
                return@launch
            }
            val description = current.draftDescription.trim().ifBlank { null }
            val editingId = current.editingTransactionId
            if (editingId != null) {
                financeRepository.updateTransaction(
                    id = editingId,
                    type = current.draftType,
                    amount = amount,
                    category = category,
                    description = description,
                )
            } else {
                financeRepository.addTransaction(
                    type = current.draftType,
                    amount = amount,
                    category = category,
                    description = description,
                )
            }
            _state.update {
                it.copy(
                    showEntrySheet = false,
                    editingTransactionId = null,
                    draftAmount = "",
                    draftCategory = "",
                    draftDescription = "",
                    error = null,
                )
            }
        }
    }

    private fun deleteTransaction() {
        viewModelScope.launch {
            val id = _state.value.editingTransactionId ?: return@launch
            financeRepository.deleteTransaction(id)
            _state.update {
                it.copy(
                    showEntrySheet = false,
                    editingTransactionId = null,
                    error = null,
                )
            }
        }
    }

    private fun openCreateDebtSheet() {
        _state.update {
            it.copy(
                showDebtSheet = true,
                editingDebtId = null,
                draftContactName = "",
                draftDebtDirection = DebtDirection.OWED_BY_ME,
                draftDebtTotal = "",
                draftDebtRemaining = "",
                draftNotes = "",
                error = null,
            )
        }
    }

    private fun openEditDebtSheet(debtId: String) {
        viewModelScope.launch {
            val debt = financeRepository.getDebt(debtId) ?: return@launch
            val direction = DebtDirection.entries.firstOrNull { it.wire == debt.direction }
                ?: DebtDirection.OWED_BY_ME
            _state.update {
                it.copy(
                    showDebtSheet = true,
                    editingDebtId = debt.id,
                    draftContactName = debt.contactName,
                    draftDebtDirection = direction,
                    draftDebtTotal = formatDraftAmount(debt.totalAmount),
                    draftDebtRemaining = formatDraftAmount(debt.remainingAmount),
                    draftNotes = debt.notes.orEmpty(),
                    error = null,
                )
            }
        }
    }

    private fun saveDebt() {
        viewModelScope.launch {
            val current = _state.value
            val contact = current.draftContactName.trim()
            if (contact.isBlank()) {
                _state.update { it.copy(error = "Enter a contact name.", showDebtSheet = true) }
                return@launch
            }
            val total = current.draftDebtTotal.toDoubleOrNull()
            if (total == null || total <= 0.0) {
                _state.update { it.copy(error = "Enter a valid total amount.", showDebtSheet = true) }
                return@launch
            }
            val remaining = current.draftDebtRemaining.toDoubleOrNull() ?: total
            if (remaining < 0.0) {
                _state.update { it.copy(error = "Remaining amount cannot be negative.", showDebtSheet = true) }
                return@launch
            }
            val notes = current.draftNotes.trim().ifBlank { null }
            val editingId = current.editingDebtId
            if (editingId != null) {
                financeRepository.updateDebt(
                    id = editingId,
                    contactName = contact,
                    direction = current.draftDebtDirection,
                    totalAmount = total,
                    remainingAmount = remaining,
                    notes = notes,
                )
            } else {
                financeRepository.addDebt(
                    contactName = contact,
                    direction = current.draftDebtDirection,
                    totalAmount = total,
                    notes = notes,
                )
            }
            _state.update {
                it.copy(
                    showDebtSheet = false,
                    editingDebtId = null,
                    draftContactName = "",
                    draftDebtTotal = "",
                    draftDebtRemaining = "",
                    draftNotes = "",
                    error = null,
                )
            }
        }
    }

    private fun deleteDebt() {
        viewModelScope.launch {
            val id = _state.value.editingDebtId ?: return@launch
            financeRepository.deleteDebt(id)
            _state.update {
                it.copy(
                    showDebtSheet = false,
                    editingDebtId = null,
                    error = null,
                )
            }
        }
    }

    private fun formatDraftAmount(amount: Double): String {
        val whole = amount.toLong()
        return if (amount == whole.toDouble()) whole.toString() else amount.toString()
    }
}
