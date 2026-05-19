package com.stella.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stella.core.data.CheckInStatus
import com.stella.core.data.EveningReviewRepository
import com.stella.core.data.HabitRepository
import com.stella.core.data.SettingsRepository
import com.stella.core.data.TaskRepository
import com.stella.core.data.TaskStatus
import com.stella.core.database.entity.TaskEntity
import com.stella.core.util.DateUtils
import com.stella.core.util.TimeService
import java.time.LocalTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class HomeUiState(
    val greeting: String = "",
    val efficiencyPercent: Int = 0,
    val topTasks: List<TaskEntity> = emptyList(),
    val habitCount: Int = 0,
    val showEveningReviewBanner: Boolean = false,
    val isLoading: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    habitRepository: HabitRepository,
    taskRepository: TaskRepository,
    eveningReviewRepository: EveningReviewRepository,
    settingsRepository: SettingsRepository,
    timeService: TimeService,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        val today = timeService.today()
        val formatter = DateTimeFormatter.ofPattern("EEEE, MMM d")
        val weekStart = today.minusDays(6)
        viewModelScope.launch {
            combine(
                habitRepository.observeHabitsWithCheckIns(weekStart),
                taskRepository.observeTasks(),
                eveningReviewRepository.observeToday(),
            ) { habits, tasks, eveningReview ->
                val totalCells = habits.size * 7
                val done = habits.sumOf { it.checkIns.values.count { s -> s == CheckInStatus.DONE } }
                val efficiency = if (totalCells > 0) (done * 100) / totalCells else 0
                val todayTasks = tasks
                    .filter { it.deletedAt == null && it.status != TaskStatus.DONE.name }
                    .take(3)
                val now = LocalTime.now(timeService.zone())
                val threshold = LocalTime.of(
                    settingsRepository.getEveningReviewHour(),
                    settingsRepository.getEveningReviewMinute(),
                )
                HomeUiState(
                    greeting = today.format(formatter),
                    efficiencyPercent = efficiency,
                    topTasks = todayTasks,
                    habitCount = habits.size,
                    showEveningReviewBanner = eveningReview == null && !now.isBefore(threshold),
                    isLoading = false,
                )
            }.collect { home ->
                _state.update { home }
            }
        }
    }
}
