package com.stella.feature.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.stella.core.ui.theme.TextSecondary

@Composable
fun TasksScreen(
    initialEditTaskId: String? = null,
    viewModel: TasksViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(initialEditTaskId) {
        initialEditTaskId?.let { viewModel.onEvent(TasksUiEvent.OpenEditTask(it)) }
    }

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
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                DirectiveComposer(
                    expanded = state.composerExpanded,
                    title = state.newTaskTitle,
                    composerError = state.composerError,
                    onExpand = { viewModel.onEvent(TasksUiEvent.ExpandComposer) },
                    onTitleChange = { viewModel.onEvent(TasksUiEvent.TitleChanged(it)) },
                    onAddToday = { viewModel.onEvent(TasksUiEvent.AddForToday) },
                    onAddTomorrow = { viewModel.onEvent(TasksUiEvent.AddForTomorrow) },
                )
                FrontlineTabs(
                    selectedTab = state.selectedTab,
                    onSelectTab = { viewModel.onEvent(TasksUiEvent.SelectTab(it)) },
                )
                if (state.activeTasks.isEmpty() && state.completedTasks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No directives yet. Tap New directive to add one.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary,
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ReorderableActiveTaskList(
                            tasks = state.activeTasks,
                            onReorder = { from, to ->
                                viewModel.onEvent(TasksUiEvent.ReorderActive(from, to))
                            },
                            onReorderCommitted = {
                                viewModel.onEvent(TasksUiEvent.CommitReorder)
                            },
                            onEdit = { id -> viewModel.onEvent(TasksUiEvent.OpenEditTask(id)) },
                            onToggle = { viewModel.onEvent(TasksUiEvent.ToggleStatus(it)) },
                            onDelete = { viewModel.onEvent(TasksUiEvent.Delete(it)) },
                            modifier = Modifier.weight(1f),
                        )
                        CompletedTaskList(
                            tasks = state.completedTasks,
                            onEdit = { id -> viewModel.onEvent(TasksUiEvent.OpenEditTask(id)) },
                            onToggle = { viewModel.onEvent(TasksUiEvent.ToggleStatus(it)) },
                            onDelete = { viewModel.onEvent(TasksUiEvent.Delete(it)) },
                        )
                    }
                }
            }
        }
    }

    if (state.showSchedulePicker) {
        val editingId = state.editingTaskId
        SchedulePickerSheet(
            mode = state.schedulePickerMode,
            pickerDate = state.pickerDate,
            pickerHour = state.pickerHour,
            pickerMinute = state.pickerMinute,
            editDraftTitle = state.editDraftTitle,
            onEditTitleChange = { viewModel.onEvent(TasksUiEvent.EditDraftTitleChanged(it)) },
            onDateChange = { viewModel.onEvent(TasksUiEvent.PickerDateChanged(it)) },
            onHourChange = { viewModel.onEvent(TasksUiEvent.PickerHourChanged(it)) },
            onMinuteChange = { viewModel.onEvent(TasksUiEvent.PickerMinuteChanged(it)) },
            onConfirm = { viewModel.onEvent(TasksUiEvent.ConfirmSchedulePicker) },
            onDismiss = { viewModel.onEvent(TasksUiEvent.DismissSchedulePicker) },
            onDelete = if (editingId != null) {
                { viewModel.onEvent(TasksUiEvent.Delete(editingId)) }
            } else {
                null
            },
        )
    }
}
