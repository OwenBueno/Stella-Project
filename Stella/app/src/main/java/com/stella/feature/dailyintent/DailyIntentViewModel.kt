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

data class Top3Slot(
    val index: Int,
    val taskId: String?,
)

data class DailyIntentUiState(
    val slots: List<Top3Slot> = listOf(
        Top3Slot(0, null),
        Top3Slot(1, null),
        Top3Slot(2, null),
    ),
    val tasks: List<TaskEntity> = emptyList(),
    val blockSchedules: Map<String, BlockSchedule> = emptyMap(),
    val newTaskTitle: String = "",
    val timeZoneLabel: String = "",
    val defaultDurationMinutes: Int = 60,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
) {
    val selectedTaskIds: List<String> = slots.mapNotNull { it.taskId }
    val selectionCount: Int = selectedTaskIds.size
    val canUnlock: Boolean = selectionCount == 3 && !isSaving

    fun taskForId(id: String): TaskEntity? = tasks.find { it.id == id }

    fun scheduleFor(taskId: String?): BlockSchedule? = taskId?.let { blockSchedules[it] }
}

sealed interface DailyIntentUiEvent {
    data class NewTaskTitleChanged(val title: String) : DailyIntentUiEvent
    data object AddFrog : DailyIntentUiEvent
    data class SelectTask(val taskId: String) : DailyIntentUiEvent
    data class ClearSlot(val slotIndex: Int) : DailyIntentUiEvent
    data class BlockHourChanged(val taskId: String, val hour: Int) : DailyIntentUiEvent
    data class BlockMinuteChanged(val taskId: String, val minute: Int) : DailyIntentUiEvent
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
            timeZoneLabel = "",
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
                    current.copy(
                        tasks = open,
                        isLoading = false,
                        blockSchedules = mergeBlockSchedules(
                            selectedIds = current.selectedTaskIds,
                            slots = current.slots,
                            existing = current.blockSchedules,
                            tasks = open,
                        ),
                    )
                }
            }
        }
    }

    fun onEvent(event: DailyIntentUiEvent) {
        when (event) {
            is DailyIntentUiEvent.NewTaskTitleChanged ->
                _state.update { it.copy(newTaskTitle = event.title, error = null) }
            DailyIntentUiEvent.AddFrog -> addFrog()
            is DailyIntentUiEvent.SelectTask -> selectTask(event.taskId)
            is DailyIntentUiEvent.ClearSlot -> clearSlot(event.slotIndex)
            is DailyIntentUiEvent.BlockHourChanged -> updateSchedule(event.taskId) { it.copy(hour = event.hour.coerceIn(0, 23)) }
            is DailyIntentUiEvent.BlockMinuteChanged -> updateSchedule(event.taskId) { it.copy(minute = event.minute.coerceIn(0, 59)) }
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
            val existing = current.blockSchedules[taskId] ?: defaultScheduleForSlot(
                current.slots.indexOfFirst { it.taskId == taskId }.coerceAtLeast(0),
            )
            current.copy(
                blockSchedules = current.blockSchedules + (taskId to transform(existing)),
                error = null,
            )
        }
    }

    private fun addFrog() {
        val title = _state.value.newTaskTitle.trim()
        if (title.isEmpty()) {
            _state.update { it.copy(error = "Enter a task name to add a frog.") }
            return
        }
        viewModelScope.launch {
            val defaults = settingsRepository.getBlockScheduleForSlot(0)
            val today = timeService.today()
            val scheduledAt = timeService.toInstantIso(today, defaults.hour, defaults.minute)
            taskRepository.addTask(title, scheduledAt)
            _state.update { it.copy(newTaskTitle = "", error = null) }
        }
    }

    private fun selectTask(taskId: String) {
        _state.update { current ->
            val inSlot = current.slots.indexOfFirst { it.taskId == taskId }
            if (inSlot >= 0) {
                val newSlots = current.slots.mapIndexed { i, slot ->
                    if (i == inSlot) slot.copy(taskId = null) else slot
                }
                return@update current.copy(
                    slots = newSlots,
                    blockSchedules = mergeBlockSchedules(
                        selectedIds = newSlots.mapNotNull { it.taskId },
                        slots = newSlots,
                        existing = current.blockSchedules,
                        tasks = current.tasks,
                    ),
                    error = null,
                )
            }
            val emptyIndex = current.slots.indexOfFirst { it.taskId == null }
            if (emptyIndex < 0) {
                return@update current.copy(error = "Top 3 is full. Clear a slot or tap a selected task to remove it.")
            }
            val newSlots = current.slots.mapIndexed { i, slot ->
                if (i == emptyIndex) slot.copy(taskId = taskId) else slot
            }
            current.copy(
                slots = newSlots,
                blockSchedules = mergeBlockSchedules(
                    selectedIds = newSlots.mapNotNull { it.taskId },
                    slots = newSlots,
                    existing = current.blockSchedules,
                    tasks = current.tasks,
                ),
                error = null,
            )
        }
    }

    private fun clearSlot(slotIndex: Int) {
        _state.update { current ->
            val newSlots = current.slots.mapIndexed { i, slot ->
                if (i == slotIndex) slot.copy(taskId = null) else slot
            }
            current.copy(
                slots = newSlots,
                blockSchedules = mergeBlockSchedules(
                    selectedIds = newSlots.mapNotNull { it.taskId },
                    slots = newSlots,
                    existing = current.blockSchedules,
                    tasks = current.tasks,
                ),
                error = null,
            )
        }
    }

    private fun unlockDay() {
        val current = _state.value
        if (current.selectionCount != 3) {
            _state.update {
                it.copy(error = "Assign exactly 3 tasks to your Top 3 slots (${it.selectionCount}/3).")
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            try {
                val today = timeService.today()
                val now = Instant.now().toString()
                current.selectedTaskIds.forEach { taskId ->
                    val task = current.taskForId(taskId) ?: return@forEach
                    val schedule = current.blockSchedules[taskId]
                        ?: defaultScheduleForSlot(current.selectedTaskIds.indexOf(taskId))
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
                val intentId = dailyIntentRepository.saveIntent(current.selectedTaskIds, nfcTagId)
                lifeLogWriter.logMorningUnlock(intentId, nfcTagId)
                onUnlocked?.invoke()
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = e.message ?: "Failed to save") }
            }
        }
    }

    private fun mergeBlockSchedules(
        selectedIds: List<String>,
        slots: List<Top3Slot>,
        existing: Map<String, BlockSchedule>,
        tasks: List<TaskEntity>,
    ): Map<String, BlockSchedule> = selectedIds.associateWith { taskId ->
        existing[taskId]
            ?: scheduleFromTask(taskId, tasks)
            ?: defaultScheduleForSlot(slots.indexOfFirst { it.taskId == taskId }.coerceAtLeast(0))
    }

    private fun scheduleFromTask(taskId: String, tasks: List<TaskEntity>): BlockSchedule? {
        val task = tasks.find { it.id == taskId } ?: return null
        val scheduledAt = task.scheduledAt ?: return null
        return BlockSchedule(
            hour = timeService.toLocalTime(scheduledAt).hour,
            minute = timeService.toLocalTime(scheduledAt).minute,
            durationMinutes = task.durationMinutes ?: settingsRepository.getBlockDurationMinutes(),
        )
    }

    private fun defaultScheduleForSlot(slotIndex: Int): BlockSchedule {
        val defaults = settingsRepository.getBlockScheduleForSlot(slotIndex.coerceIn(0, 2))
        return BlockSchedule(
            hour = defaults.hour,
            minute = defaults.minute,
            durationMinutes = defaults.durationMinutes,
        )
    }
}
