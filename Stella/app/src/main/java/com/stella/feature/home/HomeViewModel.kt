package com.stella.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stella.core.data.DailyIntentRepository
import com.stella.core.data.EveningReviewRepository
import com.stella.core.data.SettingsRepository
import com.stella.core.data.TaskRepository
import com.stella.core.data.TaskStatus
import com.stella.core.database.entity.TaskEntity
import com.stella.core.util.DateUtils
import com.stella.core.util.TimeService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class WeekDayUi(
    val date: LocalDate,
    val label: String,
    val dayNumber: Int,
    val isToday: Boolean,
    val isSelected: Boolean,
)

data class TaskRowUi(
    val id: String,
    val title: String,
    val status: String,
)

data class HomeUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val weekDays: List<WeekDayUi> = emptyList(),
    val dateLine: String = "",
    val clockText: String = "",
    val completionPercent: Int = 0,
    val activeTaskCount: Int = 0,
    val tasksInProgress: List<TaskRowUi> = emptyList(),
    val showEveningReviewBanner: Boolean = false,
    val isLoading: Boolean = true,
)

sealed interface HomeUiEvent {
    data class SelectWeekDay(val date: LocalDate) : HomeUiEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    taskRepository: TaskRepository,
    dailyIntentRepository: DailyIntentRepository,
    eveningReviewRepository: EveningReviewRepository,
    settingsRepository: SettingsRepository,
    private val timeService: TimeService,
) : ViewModel() {

    private val selectedDate = MutableStateFlow(timeService.today())
    private val _state = MutableStateFlow(HomeUiState(selectedDate = timeService.today()))
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")
    private val clockFormatter = DateTimeFormatter.ofPattern("hh:mm:ss a")

    init {
        viewModelScope.launch {
            while (true) {
                val now = LocalTime.now(timeService.zone())
                _state.update { it.copy(clockText = now.format(clockFormatter)) }
                delay(1000)
            }
        }

        viewModelScope.launch {
            selectedDate.flatMapLatest { date ->
                combine(
                    taskRepository.observeTasks(),
                    dailyIntentRepository.observeByDate(date),
                    eveningReviewRepository.observeToday(),
                ) { tasks, intent, eveningReview ->
                    buildDashboard(
                        selectedDate = date,
                        tasks = tasks,
                        plannedIds = intent?.plannedTaskIds.orEmpty(),
                        eveningReviewDone = eveningReview != null,
                        settingsRepository = settingsRepository,
                    ).copy(
                        selectedDate = date,
                        weekDays = buildWeekDays(date, timeService.today()),
                        dateLine = date.format(dateFormatter),
                    )
                }
            }.collect { home ->
                _state.update { current ->
                    home.copy(clockText = current.clockText)
                }
            }
        }
    }

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            is HomeUiEvent.SelectWeekDay -> selectedDate.value = event.date
        }
    }

    private fun buildDashboard(
        selectedDate: LocalDate,
        tasks: List<TaskEntity>,
        plannedIds: List<String>,
        eveningReviewDone: Boolean,
        settingsRepository: SettingsRepository,
    ): HomeUiState {
        val activeTasks = tasks.filter { it.deletedAt == null }
        val plannedSet = plannedIds.toSet()
        val scoped = activeTasks.filter { task ->
            task.id in plannedSet ||
                (task.scheduledAt?.let { timeService.toLocalDate(it) == selectedDate } == true)
        }.distinctBy { it.id }

        val doneCount = scoped.count { it.status == TaskStatus.DONE.name }
        val total = scoped.size
        val completionPercent = if (total > 0) (doneCount * 100) / total else 0
        val activeCount = scoped.count { it.status != TaskStatus.DONE.name }

        val tasksInProgress = scoped
            .filter { it.status != TaskStatus.DONE.name }
            .sortedWith(compareBy<TaskEntity> { taskSortEpoch(it) }.thenBy { it.title.lowercase() })
            .map { TaskRowUi(it.id, it.title, it.status) }

        val now = LocalTime.now(timeService.zone())
        val threshold = LocalTime.of(
            settingsRepository.getEveningReviewHour(),
            settingsRepository.getEveningReviewMinute(),
        )
        val showBanner = selectedDate == timeService.today() &&
            !eveningReviewDone &&
            !now.isBefore(threshold)

        return HomeUiState(
            selectedDate = selectedDate,
            completionPercent = completionPercent,
            activeTaskCount = activeCount,
            tasksInProgress = tasksInProgress,
            showEveningReviewBanner = showBanner,
            isLoading = false,
        )
    }

    private fun taskSortEpoch(task: TaskEntity): Long =
        task.scheduledAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
            ?: Long.MAX_VALUE

    private fun buildWeekDays(selected: LocalDate, today: LocalDate): List<WeekDayUi> {
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        return DateUtils.weekDates(weekStart, 7).map { date ->
            WeekDayUi(
                date = date,
                label = date.dayOfWeek.getDisplayName(
                    java.time.format.TextStyle.NARROW_STANDALONE,
                    java.util.Locale.getDefault(),
                ),
                dayNumber = date.dayOfMonth,
                isToday = date == today,
                isSelected = date == selected,
            )
        }
    }
}
