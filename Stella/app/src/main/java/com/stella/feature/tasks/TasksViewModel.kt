package com.stella.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stella.core.data.SettingsRepository
import com.stella.core.data.TaskRepository
import com.stella.core.data.TaskStatus
import com.stella.core.database.entity.TaskEntity
import com.stella.core.util.TaskScheduleFormatter
import com.stella.core.util.TimeService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val settingsRepository: SettingsRepository,
    private val timeService: TimeService,
) : ViewModel() {

    private val _state = MutableStateFlow(TasksUiState(pickerDate = timeService.today()))
    val state: StateFlow<TasksUiState> = _state.asStateFlow()

    private val selectedTab = MutableStateFlow(FrontlineTab.TODAY)
    private var lastActiveIds: List<String> = emptyList()

    init {
        viewModelScope.launch {
            combine(
                taskRepository.observeTaskLists(),
                selectedTab,
            ) { lists, tab -> lists to tab }
                .collect { (lists, tab) ->
                    val today = timeService.today()
                    val tomorrow = today.plusDays(1)
                    val filteredActive = when (tab) {
                        FrontlineTab.TODAY -> lists.active.filter { isOnDate(it, today) }
                        FrontlineTab.TOMORROW -> lists.active.filter { isOnDate(it, tomorrow) }
                        FrontlineTab.ALL -> lists.active
                    }
                    val filteredCompleted = when (tab) {
                        FrontlineTab.TODAY -> lists.completed.filter { isOnDate(it, today) }
                        FrontlineTab.TOMORROW -> lists.completed.filter { isOnDate(it, tomorrow) }
                        FrontlineTab.ALL -> lists.completed
                    }
                    lastActiveIds = filteredActive.map { it.id }
                    _state.update { current ->
                        val activeCards = if (current.isDraggingReorder) {
                            current.activeTasks
                        } else {
                            filteredActive.mapIndexed { index, task ->
                                toCardUi(task, index + 1)
                            }
                        }
                        current.copy(
                            selectedTab = tab,
                            isLoading = false,
                            activeTasks = activeCards,
                            completedTasks = filteredCompleted.mapIndexed { index, task ->
                                toCardUi(task, index + 1)
                            },
                        )
                    }
                }
        }
    }

    fun onEvent(event: TasksUiEvent) {
        when (event) {
            is TasksUiEvent.SelectTab -> {
                selectedTab.value = event.tab
                _state.update { it.copy(selectedTab = event.tab) }
            }
            TasksUiEvent.ExpandComposer -> _state.update { it.copy(composerExpanded = true, composerError = null) }
            TasksUiEvent.CollapseComposer -> _state.update {
                it.copy(composerExpanded = false, newTaskTitle = "", composerError = null)
            }
            is TasksUiEvent.TitleChanged -> _state.update { it.copy(newTaskTitle = event.title, composerError = null) }
            TasksUiEvent.AddForToday -> openSchedulePickerForAdd(timeService.today())
            TasksUiEvent.AddForTomorrow -> openSchedulePickerForAdd(timeService.today().plusDays(1))
            TasksUiEvent.DismissSchedulePicker -> _state.update {
                it.copy(
                    showSchedulePicker = false,
                    editingTaskId = null,
                    editDraftTitle = "",
                )
            }
            is TasksUiEvent.PickerDateChanged -> _state.update { it.copy(pickerDate = event.date) }
            is TasksUiEvent.PickerHourChanged -> _state.update { it.copy(pickerHour = event.hour) }
            is TasksUiEvent.PickerMinuteChanged -> _state.update { it.copy(pickerMinute = event.minute) }
            is TasksUiEvent.EditDraftTitleChanged -> _state.update { it.copy(editDraftTitle = event.title) }
            TasksUiEvent.ConfirmSchedulePicker -> confirmSchedulePicker()
            is TasksUiEvent.OpenEditTask -> openEditTask(event.id)
            is TasksUiEvent.ReorderActive -> reorderLocal(event.fromIndex, event.toIndex)
            TasksUiEvent.CommitReorder -> commitReorder()
            is TasksUiEvent.ToggleStatus -> viewModelScope.launch {
                taskRepository.cycleStatus(event.id)
            }
            is TasksUiEvent.Delete -> viewModelScope.launch {
                taskRepository.deleteTask(event.id)
                if (_state.value.editingTaskId == event.id) {
                    _state.update {
                        it.copy(
                            showSchedulePicker = false,
                            editingTaskId = null,
                            editDraftTitle = "",
                        )
                    }
                }
            }
        }
    }

    private fun openSchedulePickerForAdd(date: LocalDate) {
        val title = _state.value.newTaskTitle.trim()
        if (title.isEmpty()) {
            _state.update { it.copy(composerError = "Enter a directive name first.") }
            return
        }
        _state.update {
            it.copy(
                showSchedulePicker = true,
                schedulePickerMode = SchedulePickerMode.ADD,
                pickerDate = date,
                pickerHour = settingsRepository.getDefaultTaskStartHour(),
                pickerMinute = snapScheduleMinute(settingsRepository.getDefaultTaskStartMinute()),
                editingTaskId = null,
                editDraftTitle = "",
                composerError = null,
            )
        }
    }

    private fun openEditTask(id: String) {
        viewModelScope.launch {
            val task = _state.value.activeTasks.find { it.task.id == id }?.task
                ?: _state.value.completedTasks.find { it.task.id == id }?.task
                ?: taskRepository.getById(id)
                ?: return@launch
            val scheduled = task.scheduledAt?.let { runCatching { timeService.toLocalDate(it) }.getOrNull() }
            val localTime = task.scheduledAt?.let { runCatching { timeService.toLocalTime(it) }.getOrNull() }
            val today = timeService.today()
            val tab = when (scheduled) {
                today -> FrontlineTab.TODAY
                today.plusDays(1) -> FrontlineTab.TOMORROW
                else -> FrontlineTab.ALL
            }
            selectedTab.value = tab
            _state.update {
                it.copy(
                    selectedTab = tab,
                    showSchedulePicker = true,
                    schedulePickerMode = SchedulePickerMode.EDIT,
                    editingTaskId = id,
                    editDraftTitle = task.title,
                    pickerDate = scheduled ?: today,
                    pickerHour = localTime?.hour ?: settingsRepository.getDefaultTaskStartHour(),
                    pickerMinute = snapScheduleMinute(
                        localTime?.minute ?: settingsRepository.getDefaultTaskStartMinute(),
                    ),
                )
            }
        }
    }

    private fun confirmSchedulePicker() {
        when (_state.value.schedulePickerMode) {
            SchedulePickerMode.ADD -> {
                val title = _state.value.newTaskTitle.trim()
                if (title.isEmpty()) return
                val s = _state.value
                addTask(title, s.pickerDate, s.pickerHour, s.pickerMinute)
                _state.update {
                    it.copy(
                        showSchedulePicker = false,
                        newTaskTitle = "",
                        composerExpanded = false,
                    )
                }
            }
            SchedulePickerMode.EDIT -> saveEditedTask()
        }
    }

    private fun saveEditedTask() {
        val id = _state.value.editingTaskId ?: return
        val title = _state.value.editDraftTitle.trim()
        if (title.isEmpty()) return
        val s = _state.value
        viewModelScope.launch {
            val task = taskRepository.getById(id) ?: return@launch
            val scheduledAt = timeService.toInstantIso(s.pickerDate, s.pickerHour, s.pickerMinute)
            taskRepository.updateTask(
                task.copy(
                    title = title,
                    scheduledAt = scheduledAt,
                ),
            )
            _state.update {
                it.copy(
                    showSchedulePicker = false,
                    editingTaskId = null,
                    editDraftTitle = "",
                )
            }
        }
    }

    private fun reorderLocal(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex || fromIndex !in lastActiveIds.indices || toIndex !in lastActiveIds.indices) {
            return
        }
        val mutable = lastActiveIds.toMutableList()
        val id = mutable.removeAt(fromIndex)
        mutable.add(toIndex, id)
        lastActiveIds = mutable
        val reordered = mutable.mapIndexed { index, taskId ->
            val card = _state.value.activeTasks.find { it.task.id == taskId }
                ?: return@mapIndexed null
            card.copy(sequenceLabel = (index + 1).toString().padStart(2, '0'))
        }.filterNotNull()
        _state.update { it.copy(activeTasks = reordered, isDraggingReorder = true) }
    }

    private fun commitReorder() {
        val ids = lastActiveIds
        _state.update { it.copy(isDraggingReorder = false) }
        if (ids.isEmpty()) return
        viewModelScope.launch {
            taskRepository.reorderActiveTasks(ids)
        }
    }

    private fun addTask(title: String, date: LocalDate, hour: Int, minute: Int) {
        viewModelScope.launch {
            val scheduledAt = timeService.toInstantIso(date, hour, minute)
            taskRepository.addTask(title, scheduledAt)
        }
    }

    private fun toCardUi(task: TaskEntity, sequence: Int): TaskCardUi =
        TaskCardUi(
            task = task,
            sequenceLabel = sequence.toString().padStart(2, '0'),
            scheduleChip = TaskScheduleFormatter.formatChip(task.scheduledAt, timeService),
        )

    private fun isOnDate(task: TaskEntity, date: LocalDate): Boolean {
        val scheduled = task.scheduledAt?.let { runCatching { timeService.toLocalDate(it) }.getOrNull() }
            ?: return false
        return scheduled == date
    }
}
