package com.stella.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import java.time.LocalDate
import javax.inject.Inject

data class TasksUiState(
    val today: List<TaskEntity> = emptyList(),
    val tomorrow: List<TaskEntity> = emptyList(),
    val upcoming: List<TaskEntity> = emptyList(),
    val newTaskTitle: String = "",
    val isLoading: Boolean = true,
)

sealed interface TasksUiEvent {
    data class TitleChanged(val title: String) : TasksUiEvent
    data object AddForToday : TasksUiEvent
    data object AddForTomorrow : TasksUiEvent
    data class ToggleStatus(val id: String) : TasksUiEvent
    data class Delete(val id: String) : TasksUiEvent
}

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val timeService: TimeService,
) : ViewModel() {

    private val _state = MutableStateFlow(TasksUiState())
    val state: StateFlow<TasksUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            taskRepository.observeTasks().collect { tasks ->
                val today = timeService.today()
                val tomorrow = today.plusDays(1)
                _state.update {
                    it.copy(
                        isLoading = false,
                        today = tasks.filter { isOnDate(it, today) },
                        tomorrow = tasks.filter { isOnDate(it, tomorrow) },
                        upcoming = tasks.filter { task ->
                            val date = task.scheduledAt?.let { parseDate(it) }
                            date != null && date.isAfter(tomorrow)
                        },
                    )
                }
            }
        }
    }

    fun onEvent(event: TasksUiEvent) {
        when (event) {
            is TasksUiEvent.TitleChanged -> _state.update { it.copy(newTaskTitle = event.title) }
            TasksUiEvent.AddForToday -> addTask(timeService.today())
            TasksUiEvent.AddForTomorrow -> addTask(timeService.today().plusDays(1))
            is TasksUiEvent.ToggleStatus -> viewModelScope.launch {
                taskRepository.cycleStatus(event.id)
            }
            is TasksUiEvent.Delete -> viewModelScope.launch {
                taskRepository.deleteTask(event.id)
            }
        }
    }

    private fun addTask(date: LocalDate) {
        val title = _state.value.newTaskTitle.trim()
        if (title.isEmpty()) return
        viewModelScope.launch {
            val scheduledAt = timeService.toInstantIso(date, 9, 0)
            taskRepository.addTask(title, scheduledAt)
            _state.update { it.copy(newTaskTitle = "") }
        }
    }

    private fun isOnDate(task: TaskEntity, date: LocalDate): Boolean {
        val scheduled = task.scheduledAt?.let { parseDate(it) } ?: return false
        return scheduled == date
    }

    private fun parseDate(iso: String): LocalDate? = runCatching {
        timeService.toLocalDate(iso)
    }.getOrNull()
}
