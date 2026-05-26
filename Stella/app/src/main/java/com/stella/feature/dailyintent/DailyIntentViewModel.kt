package com.stella.feature.dailyintent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stella.core.data.CalendarRepository
import com.stella.core.data.DailyIntentRepository
import com.stella.core.data.LifeLogWriter
import com.stella.core.data.SettingsRepository
import com.stella.core.data.TaskRepository
import com.stella.core.data.TaskStatus
import com.stella.core.database.entity.TaskEntity
import com.stella.core.util.TimeService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class PlannedTaskItem(
    val taskId: String,
    val title: String,
)

data class DailyIntentUiState(
    val plannedTaskIds: List<String> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val blockSchedules: Map<String, BlockSchedule> = emptyMap(),
    val searchQuery: String = "",
    val timeZoneLabel: String = "",
    val defaultDurationMinutes: Int = 60,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val showInfoSheet: Boolean = false,
) {
    val plannedCount: Int = plannedTaskIds.size
    val canUnlock: Boolean = plannedCount >= 3 && !isSaving

    val plannedTasks: List<PlannedTaskItem>
        get() = plannedTaskIds.mapNotNull { id ->
            tasks.find { it.id == id }?.let { PlannedTaskItem(it.id, it.title) }
        }

    val filteredTasks: List<TaskEntity>
        get() {
            val query = searchQuery.trim()
            val available = tasks.filter { it.id !in plannedTaskIds }
            if (query.isEmpty()) return available
            return available.filter { it.title.contains(query, ignoreCase = true) }
        }

    val canCreateFromQuery: Boolean
        get() {
            val query = searchQuery.trim()
            if (query.isEmpty()) return false
            return tasks.none { it.title.equals(query, ignoreCase = true) }
        }

    val createLabel: String
        get() = "Create \"${searchQuery.trim()}\""

    fun taskForId(id: String): TaskEntity? = tasks.find { it.id == id }

    fun scheduleFor(taskId: String): BlockSchedule? = blockSchedules[taskId]
}

sealed interface DailyIntentUiEvent {
    data class SearchQueryChanged(val query: String) : DailyIntentUiEvent
    data object CreateTaskFromQuery : DailyIntentUiEvent
    data class AddTaskToPlan(val taskId: String) : DailyIntentUiEvent
    data class RemoveTaskFromPlan(val taskId: String) : DailyIntentUiEvent
    data class BlockHourChanged(val taskId: String, val hour: Int) : DailyIntentUiEvent
    data class BlockMinuteChanged(val taskId: String, val minute: Int) : DailyIntentUiEvent
    data object ShowInfoSheet : DailyIntentUiEvent
    data object DismissInfoSheet : DailyIntentUiEvent
    data object UnlockDay : DailyIntentUiEvent
}

@HiltViewModel
class DailyIntentViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val calendarRepository: CalendarRepository,
    private val dailyIntentRepository: DailyIntentRepository,
    private val settingsRepository: SettingsRepository,
    private val lifeLogWriter: LifeLogWriter,
    private val timeService: TimeService,
) : ViewModel() {
    private val _state = MutableStateFlow(
        DailyIntentUiState(
            defaultDurationMinutes = settingsRepository.getBlockDurationMinutes(),
        ),
    )
    val state: StateFlow<DailyIntentUiState> = _state.asStateFlow()

    var onUnlocked: (() -> Unit)? = null

    init {
        refreshTimeLabels()
        viewModelScope.launch {
            taskRepository.observeTasks().collect { tasks ->
                val open = tasks
                    .filter { it.deletedAt == null }
                    .sortedWith(
                        compareBy<TaskEntity> { it.status == TaskStatus.DONE.name }
                            .thenBy { it.title.lowercase() },
                    )
                _state.update { current ->
                    val merged = mergeBlockSchedules(
                        plannedIds = current.plannedTaskIds,
                        existing = current.blockSchedules,
                        tasks = open,
                    )
                    current.copy(
                        tasks = open,
                        isLoading = false,
                        blockSchedules = merged,
                    )
                }
            }
        }
    }

    fun onEvent(event: DailyIntentUiEvent) {
        when (event) {
            is DailyIntentUiEvent.SearchQueryChanged ->
                _state.update { it.copy(searchQuery = event.query, error = null) }
            DailyIntentUiEvent.CreateTaskFromQuery -> createTaskFromQuery()
            is DailyIntentUiEvent.AddTaskToPlan -> addTaskToPlan(event.taskId)
            is DailyIntentUiEvent.RemoveTaskFromPlan -> removeTaskFromPlan(event.taskId)
            is DailyIntentUiEvent.BlockHourChanged ->
                updateSchedule(event.taskId) { it.copy(hour = event.hour.coerceIn(0, 23)) }
            is DailyIntentUiEvent.BlockMinuteChanged ->
                updateSchedule(event.taskId) { it.copy(minute = event.minute.coerceIn(0, 59)) }
            DailyIntentUiEvent.ShowInfoSheet -> _state.update { it.copy(showInfoSheet = true) }
            DailyIntentUiEvent.DismissInfoSheet -> _state.update { it.copy(showInfoSheet = false) }
            DailyIntentUiEvent.UnlockDay -> unlockDay()
        }
    }

    private fun refreshTimeLabels() {
        _state.update {
            it.copy(
                timeZoneLabel = timeService.zoneDisplayName(),
                defaultDurationMinutes = settingsRepository.getBlockDurationMinutes(),
            )
        }
    }

    private fun updateSchedule(taskId: String, transform: (BlockSchedule) -> BlockSchedule) {
        _state.update { current ->
            val index = current.plannedTaskIds.indexOf(taskId).coerceAtLeast(0)
            val existing = current.blockSchedules[taskId] ?: defaultScheduleForIndex(index)
            val updated = transform(existing)
            current.copy(
                blockSchedules = current.blockSchedules + (taskId to updated),
                error = null,
            )
        }
    }

    private fun createTaskFromQuery() {
        val title = _state.value.searchQuery.trim()
        if (title.isEmpty()) {
            _state.update { it.copy(error = "Enter a task name to create.") }
            return
        }
        if (!_state.value.canCreateFromQuery) return
        viewModelScope.launch {
            val index = _state.value.plannedTaskIds.size
            val schedule = defaultScheduleForIndex(index)
            val today = timeService.today()
            val scheduledAt = timeService.toInstantIso(today, schedule.hour, schedule.minute)
            val newId = taskRepository.addTask(title, scheduledAt)
            addTaskToPlanInternal(newId, clearSearch = true)
        }
    }

    private fun addTaskToPlan(taskId: String) {
        if (taskId in _state.value.plannedTaskIds) return
        addTaskToPlanInternal(taskId, clearSearch = false)
    }

    private fun addTaskToPlanInternal(taskId: String, clearSearch: Boolean) {
        _state.update { current ->
            if (taskId in current.plannedTaskIds) return@update current
            val newPlanned = current.plannedTaskIds + taskId
            val merged = mergeBlockSchedules(
                plannedIds = newPlanned,
                existing = current.blockSchedules,
                tasks = current.tasks,
            )
            current.copy(
                plannedTaskIds = newPlanned,
                searchQuery = if (clearSearch) "" else current.searchQuery,
                blockSchedules = merged,
                error = null,
            )
        }
    }

    private fun removeTaskFromPlan(taskId: String) {
        _state.update { current ->
            val newPlanned = current.plannedTaskIds.filter { it != taskId }
            val merged = mergeBlockSchedules(
                plannedIds = newPlanned,
                existing = current.blockSchedules,
                tasks = current.tasks,
            )
            current.copy(
                plannedTaskIds = newPlanned,
                blockSchedules = merged,
                error = null,
            )
        }
    }

    private fun unlockDay() {
        val current = _state.value
        if (current.plannedCount < 3) {
            _state.update {
                it.copy(error = "Add at least 3 tasks to start your day (${it.plannedCount}/3).")
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            try {
                val today = timeService.today()
                val now = Instant.now().toString()
                current.plannedTaskIds.forEachIndexed { index, taskId ->
                    val task = current.taskForId(taskId) ?: return@forEachIndexed
                    val schedule = current.blockSchedules[taskId]
                        ?: defaultScheduleForIndex(index)
                    val startIso = timeService.toInstantIso(today, schedule.hour, schedule.minute)
                    val endInstant = Instant.parse(startIso).plusSeconds(schedule.durationMinutes * 60L)
                    calendarRepository.addEvent(
                        title = task.title,
                        startAt = startIso,
                        endAt = endInstant.toString(),
                        linkedTaskId = taskId,
                    )
                    taskRepository.updateTask(
                        task.copy(
                            scheduledAt = startIso,
                            durationMinutes = schedule.durationMinutes,
                            updatedAt = now,
                            needsSync = true,
                        ),
                    )
                }
                val nfcTagId = settingsRepository.getNfcTagId() ?: SettingsRepository.DEBUG_NFC_TAG
                val intentId = dailyIntentRepository.saveIntent(current.plannedTaskIds, nfcTagId)
                lifeLogWriter.logMorningUnlock(intentId, nfcTagId)
                onUnlocked?.invoke()
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = e.message ?: "Failed to save") }
            }
        }
    }

    private fun mergeBlockSchedules(
        plannedIds: List<String>,
        existing: Map<String, BlockSchedule>,
        tasks: List<TaskEntity>,
    ): Map<String, BlockSchedule> = plannedIds.mapIndexed { index, taskId ->
        taskId to (
            existing[taskId]
                ?: scheduleFromTask(taskId, tasks)
                ?: defaultScheduleForIndex(index)
            )
    }.toMap()

    private fun scheduleFromTask(taskId: String, tasks: List<TaskEntity>): BlockSchedule? {
        val task = tasks.find { it.id == taskId } ?: return null
        val scheduledAt = task.scheduledAt ?: return null
        return BlockSchedule(
            hour = timeService.toLocalTime(scheduledAt).hour,
            minute = timeService.toLocalTime(scheduledAt).minute,
            durationMinutes = task.durationMinutes ?: settingsRepository.getBlockDurationMinutes(),
        )
    }

    private fun defaultScheduleForIndex(index: Int): BlockSchedule {
        val defaults = settingsRepository.getDefaultTaskSchedule()
        val offsetMinutes = index * defaults.durationMinutes
        val totalMinutes = defaults.hour * 60 + defaults.minute + offsetMinutes
        val capped = totalMinutes.coerceAtMost(22 * 60 + 59)
        return BlockSchedule(
            hour = capped / 60,
            minute = capped % 60,
            durationMinutes = defaults.durationMinutes,
        )
    }
}
