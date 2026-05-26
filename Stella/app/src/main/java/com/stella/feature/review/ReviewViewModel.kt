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
import com.stella.core.database.entity.HabitCheckInEntity
import com.stella.core.database.entity.TaskEntity
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
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class ReviewUiState(
    val habitsProgress: RingProgress = RingProgress(
        label = "Habits",
        valueLabel = "—",
        progress = 0f,
        accent = com.stella.core.ui.theme.Primary,
    ),
    val tasksProgress: RingProgress = RingProgress(
        label = "Tasks",
        valueLabel = "0/0",
        progress = 0f,
        accent = com.stella.core.ui.theme.Primary,
    ),
    val plannedVsActual: String = "",
    val reflectionText: String = "",
    val habits: List<com.stella.core.data.HabitWithCheckIns> = emptyList(),
    val weekDates: List<java.time.LocalDate> = emptyList(),
    val today: java.time.LocalDate = DateUtils.today(),
    val monthLabel: String = "",
    val dayScores: Map<LocalDate, Float> = emptyMap(),
    val disciplineStreakDays: Int = 0,
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
        val today = timeService.today()
        val weekStart = today.minusDays(6)
        val weekDates = DateUtils.weekDates(weekStart)
        val month = YearMonth.from(today)
        val (monthFromIso, monthToIso) = timeService.monthQueryRange(month)
        val monthLabel = month.month.name.lowercase().replaceFirstChar { it.titlecase() } + " " + month.year

        viewModelScope.launch {
            val coreFlow = combine(
                habitRepository.observeHabitsWithCheckIns(weekStart),
                taskRepository.observeTasks(),
                habitRepository.observeActiveHabits(),
                habitRepository.observeCheckIns(month.atDay(1).toString(), month.atEndOfMonth().toString()),
            ) { weekHabits, tasks, activeHabits, monthCheckIns ->
                val todayKey = timeService.dateKey(today)

                val todayHabitsTotal = activeHabits.size
                val todayHabitsDone = weekHabits.count { it.checkIns[todayKey]?.status == CheckInStatus.DONE }
                val habitsProgress = RingProgress(
                    label = "Habits",
                    valueLabel = if (todayHabitsTotal == 0) "—" else "${(todayHabitsDone * 100) / todayHabitsTotal}%",
                    progress = if (todayHabitsTotal == 0) 0f else todayHabitsDone.toFloat() / todayHabitsTotal.toFloat(),
                    accent = com.stella.core.ui.theme.Primary,
                )

                val (todayFrom, todayTo) = timeService.dayInstantRange(today)
                val scheduledToday = tasks.count { t ->
                    val scheduledDate = t.scheduledAt?.let { runCatching { timeService.toLocalDate(it) }.getOrNull() }
                    scheduledDate == today
                }
                val doneToday = tasks.count { t ->
                    t.status == TaskStatus.DONE.name && t.updatedAt >= todayFrom && t.updatedAt <= todayTo
                }
                val tasksProgress = RingProgress(
                    label = "Tasks",
                    valueLabel = "$doneToday/$scheduledToday",
                    progress = if (scheduledToday == 0) 0f else doneToday.toFloat() / scheduledToday.toFloat(),
                    accent = com.stella.core.ui.theme.Primary,
                )

                val dayScores = buildMonthScores(
                    month = month,
                    activeHabitsCount = activeHabits.size,
                    monthCheckIns = monthCheckIns,
                    tasks = tasks,
                    timeService = timeService,
                    monthFromIso = monthFromIso,
                    monthToIso = monthToIso,
                )
                val streak = computeStreak(today = today, dayScores = dayScores)

                ReviewUiState(
                    habitsProgress = habitsProgress,
                    tasksProgress = tasksProgress,
                    habits = weekHabits,
                    weekDates = weekDates,
                    today = today,
                    monthLabel = monthLabel,
                    dayScores = dayScores,
                    disciplineStreakDays = streak,
                    isLoading = false,
                )
            }

            combine(
                coreFlow,
                eveningReviewRepository.observeToday(),
                lifeLogRepository.observeRecentLines(20),
            ) { core, review, logs ->
                core.copy(
                    plannedVsActual = review?.plannedVsActual.orEmpty(),
                    reflectionText = review?.reflectionText.orEmpty(),
                    isReadOnly = review != null,
                    logLines = logs,
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

private fun buildMonthScores(
    month: YearMonth,
    activeHabitsCount: Int,
    monthCheckIns: List<HabitCheckInEntity>,
    tasks: List<TaskEntity>,
    timeService: TimeService,
    monthFromIso: String,
    monthToIso: String,
): Map<LocalDate, Float> {
    val doneHabitsByDate: Map<LocalDate, Int> =
        monthCheckIns
            .asSequence()
            .filter { it.status == CheckInStatus.DONE.name }
            .mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }
            .groupingBy { it }
            .eachCount()

    val scheduledTasksByDate: Map<LocalDate, Int> =
        tasks.asSequence()
            .mapNotNull { t -> t.scheduledAt?.let { runCatching { timeService.toLocalDate(it) }.getOrNull() } }
            .filter { YearMonth.from(it) == month }
            .groupingBy { it }
            .eachCount()

    val doneTasksByDate: Map<LocalDate, Int> =
        tasks.asSequence()
            .filter { it.status == TaskStatus.DONE.name && it.updatedAt >= monthFromIso && it.updatedAt <= monthToIso }
            .mapNotNull { runCatching { timeService.toLocalDate(it.updatedAt) }.getOrNull() }
            .groupingBy { it }
            .eachCount()

    val result = LinkedHashMap<LocalDate, Float>()
    for (day in 1..month.lengthOfMonth()) {
        val date = month.atDay(day)
        val habitsDone = doneHabitsByDate[date] ?: 0
        val habitsRatio = if (activeHabitsCount > 0) habitsDone.toFloat() / activeHabitsCount.toFloat() else null

        val tasksScheduled = scheduledTasksByDate[date] ?: 0
        val tasksDone = doneTasksByDate[date] ?: 0
        val tasksRatio = if (tasksScheduled > 0) tasksDone.toFloat() / tasksScheduled.toFloat() else null

        val score = when {
            habitsRatio != null && tasksRatio != null -> (habitsRatio + tasksRatio) / 2f
            habitsRatio != null -> habitsRatio
            tasksRatio != null -> tasksRatio
            else -> 0f
        }.coerceIn(0f, 1f)

        result[date] = score
    }
    return result
}

private fun computeStreak(
    today: LocalDate,
    dayScores: Map<LocalDate, Float>,
): Int {
    var streak = 0
    var cursor = today
    while (true) {
        val score = dayScores[cursor] ?: 0f
        if (score <= 0f) break
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}
