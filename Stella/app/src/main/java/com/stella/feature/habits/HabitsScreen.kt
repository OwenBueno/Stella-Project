package com.stella.feature.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stella.core.ui.components.HabitGrid
import com.stella.core.ui.components.StellaSectionHeader
import com.stella.core.ui.components.stellaTextFieldColors
import com.stella.core.ui.theme.Primary
import com.stella.core.ui.theme.SurfaceCard
import com.stella.core.ui.theme.TextPrimary
import com.stella.core.ui.theme.TextSecondary
import com.stella.core.util.DateUtils

@Composable
fun HabitsScreen(
    viewModel: HabitsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StellaSectionHeader(eyebrow = "Discipline", title = "The Matrix", modifier = Modifier.weight(1f))
            IconButton(onClick = { viewModel.onEvent(HabitsUiEvent.ShowAddDialog) }) {
                Icon(Icons.Default.Add, contentDescription = "Add habit", tint = Primary)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { viewModel.onEvent(HabitsUiEvent.PrevWeek) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous week", tint = TextPrimary)
            }
            Text(
                text = "${DateUtils.formatDate(state.weekStart)} — ${DateUtils.formatDate(state.weekDates.lastOrNull() ?: state.weekStart)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            IconButton(onClick = { viewModel.onEvent(HabitsUiEvent.NextWeek) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next week", tint = TextPrimary)
            }
        }

        when {
            state.isLoading -> CircularProgressIndicator()
            state.habits.isEmpty() -> Text(
                "No habits yet. Tap + to add your first protocol.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
            )
            else -> HabitGrid(
                habits = state.habits,
                weekDates = state.weekDates,
                today = state.today,
                onCellClick = { habitId, date ->
                    viewModel.onEvent(HabitsUiEvent.CellClicked(habitId, date))
                },
            )
        }
    }

    if (state.showAddDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(HabitsUiEvent.HideAddDialog) },
            containerColor = SurfaceCard,
            titleContentColor = TextPrimary,
            textContentColor = TextPrimary,
            title = { Text("New protocol", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = state.newHabitName,
                    onValueChange = { viewModel.onEvent(HabitsUiEvent.NewHabitNameChanged(it)) },
                    label = { Text("Habit name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = stellaTextFieldColors(),
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(HabitsUiEvent.ConfirmAddHabit) }) {
                    Text("Commit", color = Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(HabitsUiEvent.HideAddDialog) }) {
                    Text("Abort", color = TextSecondary)
                }
            },
        )
    }
}
