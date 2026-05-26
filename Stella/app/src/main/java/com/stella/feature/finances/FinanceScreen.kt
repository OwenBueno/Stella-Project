package com.stella.feature.finances

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stella.core.ui.theme.DawnGradientBottom
import com.stella.core.ui.theme.DawnGradientTop
import com.stella.core.ui.theme.Primary
import com.stella.feature.calendar.MonthNavigator

@Composable
fun FinanceScreen(
    viewModel: FinanceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DawnGradientTop, DawnGradientBottom),
                ),
            ),
    ) {
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MonthNavigator(
                    month = state.selectedMonth,
                    onPrev = { viewModel.onEvent(FinanceUiEvent.PrevMonth) },
                    onNext = { viewModel.onEvent(FinanceUiEvent.NextMonth) },
                )
                if (!state.isCurrentMonth) {
                    FinanceJumpToToday(onClick = { viewModel.onEvent(FinanceUiEvent.JumpToCurrentMonth) })
                }
                FinanceSummaryPanel(summary = state.summary)
                FinanceAddTransactionButton(
                    onClick = { viewModel.onEvent(FinanceUiEvent.OpenEntrySheet) },
                )
                FinanceFilterTabs(
                    selected = state.filter,
                    onSelected = { viewModel.onEvent(FinanceUiEvent.FilterChanged(it)) },
                )
                FinanceTransactionList(
                    transactions = state.transactions,
                    onTransactionClick = { viewModel.onEvent(FinanceUiEvent.OpenEditTransaction(it)) },
                    modifier = Modifier.weight(1f),
                )
                FinanceDebtSection(
                    debts = state.unresolvedDebts,
                    onAddDebt = { viewModel.onEvent(FinanceUiEvent.OpenDebtSheet) },
                    onDebtClick = { viewModel.onEvent(FinanceUiEvent.OpenEditDebt(it)) },
                    onResolve = { viewModel.onEvent(FinanceUiEvent.ResolveDebt(it)) },
                )
            }
        }
    }

    if (state.showEntrySheet) {
        FinanceEntrySheet(
            state = state,
            onEvent = viewModel::onEvent,
        )
    }

    if (state.showDebtSheet) {
        FinanceDebtSheet(
            state = state,
            onEvent = viewModel::onEvent,
        )
    }
}
