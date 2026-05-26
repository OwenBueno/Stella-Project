package com.stella.feature.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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

@Composable
fun HabitsScreen(
    viewModel: HabitsViewModel = hiltViewModel(),
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
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HabitsAddToolbar(onAddClick = { viewModel.onEvent(HabitsUiEvent.ShowCreateSheet) })
                WeekNavigator(
                    weekLabel = state.weekLabel,
                    onPrevWeek = { viewModel.onEvent(HabitsUiEvent.PrevWeek) },
                    onNextWeek = { viewModel.onEvent(HabitsUiEvent.NextWeek) },
                )
                if (state.habits.isNotEmpty()) {
                    HabitsNameHint()
                }
                when {
                    state.habits.isEmpty() -> HabitsEmptyState(modifier = Modifier.weight(1f))
                    else -> HabitsTrackerGrid(
                        habits = state.habits,
                        weekDates = state.weekDates,
                        dayHeaders = state.dayHeaders,
                        today = state.today,
                        onHabitNameClick = { viewModel.onEvent(HabitsUiEvent.ShowEditSheet(it)) },
                        onCellClick = { habitId, date ->
                            viewModel.onEvent(HabitsUiEvent.CellClicked(habitId, date))
                        },
                        onCellLongPress = { habitId, date ->
                            viewModel.onEvent(HabitsUiEvent.CellLongPressed(habitId, date))
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        state.tooltip?.let { tooltip ->
            CompletionTooltip(
                message = tooltip.message,
                onDismiss = { viewModel.onEvent(HabitsUiEvent.DismissTooltip) },
            )
        }
    }

    when (val sheet = state.activeSheet) {
        HabitsSheet.Create -> CreateHabitSheet(
            draftName = state.draftName,
            onDraftChange = { viewModel.onEvent(HabitsUiEvent.DraftNameChanged(it)) },
            onConfirm = { viewModel.onEvent(HabitsUiEvent.ConfirmCreate) },
            onDismiss = { viewModel.onEvent(HabitsUiEvent.HideSheet) },
        )
        is HabitsSheet.Edit -> EditHabitSheet(
            draftName = state.draftName,
            onDraftChange = { viewModel.onEvent(HabitsUiEvent.DraftNameChanged(it)) },
            onConfirm = { viewModel.onEvent(HabitsUiEvent.ConfirmRename) },
            onDelete = { viewModel.onEvent(HabitsUiEvent.ConfirmDelete) },
            onDismiss = { viewModel.onEvent(HabitsUiEvent.HideSheet) },
        )
        null -> Unit
    }
}
