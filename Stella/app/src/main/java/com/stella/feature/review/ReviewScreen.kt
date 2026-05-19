package com.stella.feature.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stella.core.ui.components.HabitGrid
import com.stella.core.ui.components.StellaCard
import com.stella.core.ui.components.StellaSectionHeader
import com.stella.core.ui.components.StellaStatCard
import com.stella.core.ui.components.stellaTextFieldColors
import com.stella.core.ui.theme.Primary
import com.stella.core.ui.theme.TextPrimary
import com.stella.core.ui.theme.TextSecondary

@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StellaSectionHeader(eyebrow = "Protocol", title = "Evening Review")

        if (state.isLoading) {
            CircularProgressIndicator()
        } else {
            StellaStatCard(label = "Habit completion", value = "${state.habitCompletionPercent}%")
            StellaStatCard(label = "Tasks completed", value = "${state.tasksCompleted}/${state.tasksTotal}")

            Text(
                if (state.isReadOnly) "Today is closed. Review is read-only." else "Close the day with an honest review.",
                color = TextSecondary,
            )

            OutlinedTextField(
                value = state.plannedVsActual,
                onValueChange = { viewModel.onEvent(ReviewUiEvent.PlannedVsActualChanged(it)) },
                label = { Text("Planned vs actual") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                enabled = !state.isReadOnly,
                colors = stellaTextFieldColors(),
            )
            OutlinedTextField(
                value = state.reflectionText,
                onValueChange = { viewModel.onEvent(ReviewUiEvent.ReflectionChanged(it)) },
                label = { Text("Reflection") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                enabled = !state.isReadOnly,
                colors = stellaTextFieldColors(),
            )

            StellaCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Habit snapshot", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    HabitGrid(
                        habits = state.habits,
                        weekDates = state.weekDates,
                        today = state.today,
                        onCellClick = { _, _ -> },
                        readOnly = true,
                    )
                }
            }

            if (!state.isReadOnly) {
                Button(
                    onClick = { viewModel.onEvent(ReviewUiEvent.CloseDay) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving,
                ) {
                    Text("Close the day")
                }
            }

            state.message?.let { Text(it, color = Primary) }

            StellaCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("System log", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    if (state.logLines.isEmpty()) {
                        Text("No events logged yet.", color = TextSecondary)
                    } else {
                        state.logLines.forEach { line ->
                            Text(line, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}
