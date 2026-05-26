package com.stella.feature.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stella.core.ui.components.DawnScreenBackground
import com.stella.core.ui.components.StellaCard
import com.stella.core.ui.theme.Primary
import com.stella.core.ui.theme.TextSecondary

@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    DawnScreenBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                if (!state.isLoading && !state.isReadOnly) {
                    GlowingPrimaryButton(
                        label = "Complete Review & Lock Day",
                        enabled = !state.isSaving,
                        onClick = { viewModel.onEvent(ReviewUiEvent.CloseDay) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                            .imePadding(),
                    )
                }
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = Primary)
                    return@Column
                }

                DailyCompletionRingsRow(
                    habitsProgress = state.habitsProgress,
                    tasksProgress = state.tasksProgress,
                )

                MonthlyDisciplineHeatmap(
                    monthLabel = state.monthLabel,
                    dayScores = state.dayScores,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    if (state.isReadOnly) "Today is closed. Review is read-only." else "Close the day with a calm, honest review.",
                    color = TextSecondary,
                )

                GlassTextCard(
                    title = "Planned vs Actual",
                    placeholder = "What planned activities shifted today, and why?",
                    value = state.plannedVsActual,
                    enabled = !state.isReadOnly,
                    onValueChange = { viewModel.onEvent(ReviewUiEvent.PlannedVsActualChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                GlassTextCard(
                    title = "Reflection",
                    placeholder = "Write down a brief reflection on your day...",
                    value = state.reflectionText,
                    enabled = !state.isReadOnly,
                    onValueChange = { viewModel.onEvent(ReviewUiEvent.ReflectionChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                )

                DisciplineStreakBadge(
                    streakDays = state.disciplineStreakDays,
                    modifier = Modifier.fillMaxWidth(),
                )

                state.message?.let { Text(it, color = Primary) }

                StellaCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("System log", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
                        if (state.logLines.isEmpty()) {
                            Text("No events logged yet.", color = TextSecondary)
                        } else {
                            state.logLines.forEach { line ->
                                Text(line, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }
                }

                if (!state.isReadOnly) {
                    Text(
                        "When you lock the day, your review is saved and synced.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}
