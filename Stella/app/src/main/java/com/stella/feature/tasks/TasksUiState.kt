package com.stella.feature.tasks

import com.stella.core.database.entity.TaskEntity
import java.time.LocalDate

enum class FrontlineTab {
    TODAY,
    TOMORROW,
    ALL,
}

enum class SchedulePickerMode {
    ADD,
    EDIT,
}

data class TaskCardUi(
    val task: TaskEntity,
    val sequenceLabel: String,
    val scheduleChip: String?,
)

data class TasksUiState(
    val selectedTab: FrontlineTab = FrontlineTab.TODAY,
    val composerExpanded: Boolean = false,
    val newTaskTitle: String = "",
    val composerError: String? = null,
    val activeTasks: List<TaskCardUi> = emptyList(),
    val completedTasks: List<TaskCardUi> = emptyList(),
    val showSchedulePicker: Boolean = false,
    val schedulePickerMode: SchedulePickerMode = SchedulePickerMode.ADD,
    val pickerDate: LocalDate = LocalDate.now(),
    val pickerHour: Int = 9,
    val pickerMinute: Int = 0,
    val editDraftTitle: String = "",
    val editingTaskId: String? = null,
    val isDraggingReorder: Boolean = false,
    val isLoading: Boolean = true,
)

sealed interface TasksUiEvent {
    data class SelectTab(val tab: FrontlineTab) : TasksUiEvent
    data object ExpandComposer : TasksUiEvent
    data object CollapseComposer : TasksUiEvent
    data class TitleChanged(val title: String) : TasksUiEvent
    data object AddForToday : TasksUiEvent
    data object AddForTomorrow : TasksUiEvent
    data object DismissSchedulePicker : TasksUiEvent
    data class PickerDateChanged(val date: LocalDate) : TasksUiEvent
    data class PickerHourChanged(val hour: Int) : TasksUiEvent
    data class PickerMinuteChanged(val minute: Int) : TasksUiEvent
    data class EditDraftTitleChanged(val title: String) : TasksUiEvent
    data object ConfirmSchedulePicker : TasksUiEvent
    data class OpenEditTask(val id: String) : TasksUiEvent
    data class ReorderActive(val fromIndex: Int, val toIndex: Int) : TasksUiEvent
    data object CommitReorder : TasksUiEvent
    data class ToggleStatus(val id: String) : TasksUiEvent
    data class Delete(val id: String) : TasksUiEvent
}
