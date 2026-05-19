package com.stella.feature.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stella.core.data.CheckInStatus
import com.stella.core.data.EveningReviewRepository
import com.stella.core.data.HabitRepository
import com.stella.core.data.LifeLogRepository
import com.stella.core.data.LifeLogWriter
import com.stella.core.data.SyncRepository
import com.stella.core.data.TaskRepository
import com.stella.core.data.TaskStatus
import com.stella.core.util.DateUtils
import com.stella.core.util.HabitGridSnapshotUtil
import com.stella.core.util.TimeService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewUiState(
    val habitCompletionPercent: Int = 0,
    val tasksCompleted: Int = 0,
    val tasksTotal: Int = 0,
    val plannedVsActual: String = "",
    val reflectionText: String = "",
    val habits: List<com.stella.core.data.HabitWithCheckIns> = emptyList(),
    val weekDates: List<java.time.LocalDate> = emptyList(),
    val today: java.time.LocalDate = DateUtils.today(),
    val isReadOnly: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val logLines: List<String> = emptyList(),
    val message: String? = null,
)

sealed interface ReviewUiEvent {
    data class PlannedVsActualChanged(val value: String) : ReviewUiEvent
    data class ReflectionChanged(val value: String) : ReviewUiEvent
    data object CloseDay : ReviewUiEvent
}

@HiltViewModel
class ReviewViewModel @Inject constructor(
    habitRepository: HabitRepository,
    taskRepository: TaskRepository,
    private val eveningReviewRepository: EveningReviewRepository,
    private val lifeLogRepository: LifeLogRepository,
    private val lifeLogWriter: LifeLogWriter,
    private val syncRepository: SyncRepository,
    timeService: TimeService,
) : ViewModel() {

    private val _state = MutableStateFlow(ReviewUiState())
    val state: StateFlow<ReviewUiState> = _state.asStateFlow()

    init {
        val weekStart = timeService.today().minusDays(6)
        val weekDates = DateUtils.weekDates(weekStart)
        viewModelScope.launch {
            combine(
                habitRepository.observeHabitsWithCheckIns(weekStart),
                taskRepository.observeTasks(),
                eveningReviewRepository.observeToday(),
                lifeLogRepository.observeRecentLines(20),
            ) { habits, tasks, review, logs ->
                val totalCells = habits.size * 7
                val doneCells = habits.sumOf { h ->
                    h.checkIns.values.count { it == CheckInStatus.DONE }
                }
                val percent = if (totalCells > 0) (doneCells * 100) / totalCells else 0
                val completed = tasks.count { it.status == TaskStatus.DONE.name }
                ReviewUiState(
                    habitCompletionPercent = percent,
                    tasksCompleted = completed,
                    tasksTotal = tasks.size,
                    plannedVsActual = review?.plannedVsActual.orEmpty(),
                    reflectionText = review?.reflectionText.orEmpty(),
                    habits = habits,
                    weekDates = weekDates,
                    today = timeService.today(),
                    isReadOnly = review != null,
                    logLines = logs,
                    isLoading = false,
                )
            }.collect { incoming ->
                _state.update { current ->
                    incoming.copy(
                        plannedVsActual = when {
                            incoming.isReadOnly -> incoming.plannedVsActual
                            current.plannedVsActual.isNotEmpty() -> current.plannedVsActual
                            else -> incoming.plannedVsActual
                        },
                        reflectionText = when {
                            incoming.isReadOnly -> incoming.reflectionText
                            current.reflectionText.isNotEmpty() -> current.reflectionText
                            else -> incoming.reflectionText
                        },
                    )
                }
            }
        }
    }

    fun onEvent(event: ReviewUiEvent) {
        when (event) {
            is ReviewUiEvent.PlannedVsActualChanged ->
                _state.update { it.copy(plannedVsActual = event.value) }
            is ReviewUiEvent.ReflectionChanged ->
                _state.update { it.copy(reflectionText = event.value) }
            ReviewUiEvent.CloseDay -> closeDay()
        }
    }

    private fun closeDay() {
        if (_state.value.isReadOnly) return
        val current = _state.value
        if (current.plannedVsActual.isBlank() || current.reflectionText.isBlank()) {
            _state.update { it.copy(message = "Fill in both reflection fields.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, message = null) }
            val snapshot = HabitGridSnapshotUtil.build(current.habits, current.weekDates)
            eveningReviewRepository.saveReview(
                plannedVsActual = current.plannedVsActual,
                reflectionText = current.reflectionText,
                habitGridSnapshot = snapshot,
            )
            val reviewId = eveningReviewRepository.getToday()?.id ?: return@launch
            lifeLogWriter.logEveningReview(reviewId)
            syncRepository.syncNow()
            _state.update { it.copy(isSaving = false, isReadOnly = true, message = "Day closed.") }
        }
    }
}
