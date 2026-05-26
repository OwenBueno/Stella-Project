package com.stella.feature.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stella.core.data.CheckInStatus
import com.stella.core.data.HabitRepository
import com.stella.core.util.DateUtils
import com.stella.core.util.TimeService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val timeService: TimeService,
) : ViewModel() {

    private val weekRangeFormatter = DateTimeFormatter.ofPattern("MMM d")
    private val tooltipFormatter = DateTimeFormatter.ofPattern("MMMM d 'at' h:mm a")

    private val _state = MutableStateFlow(HabitsUiState())
    val state: StateFlow<HabitsUiState> = _state.asStateFlow()

    init {
        val weekStart = DateUtils.mondayWeekStart(timeService.today())
        _state.value = applyWeek(weekStart, _state.value)
        viewModelScope.launch {
            _state
                .map { it.weekStart }
                .distinctUntilChanged()
                .flatMapLatest { start -> habitRepository.observeHabitsWithCheckIns(start) }
                .collect { habits ->
                    _state.update { it.copy(isLoading = false, habits = habits, error = null) }
                }
        }
    }

    fun onEvent(event: HabitsUiEvent) {
        when (event) {
            HabitsUiEvent.Refresh -> Unit
            HabitsUiEvent.PrevWeek -> shiftWeek(-7)
            HabitsUiEvent.NextWeek -> shiftWeek(7)
            is HabitsUiEvent.CellClicked -> viewModelScope.launch {
                habitRepository.toggleCheckIn(event.habitId, event.date)
            }
            is HabitsUiEvent.CellLongPressed -> showCompletionTooltip(event.habitId, event.date)
            HabitsUiEvent.DismissTooltip -> _state.update { it.copy(tooltip = null) }
            HabitsUiEvent.ShowCreateSheet -> _state.update {
                it.copy(activeSheet = HabitsSheet.Create, draftName = "")
            }
            HabitsUiEvent.HideSheet -> _state.update {
                it.copy(activeSheet = null, draftName = "")
            }
            is HabitsUiEvent.ShowEditSheet -> {
                val habit = _state.value.habits.find { it.habit.id == event.habitId }?.habit
                if (habit != null) {
                    _state.update {
                        it.copy(
                            activeSheet = HabitsSheet.Edit(habit.id, habit.name),
                            draftName = habit.name,
                        )
                    }
                }
            }
            is HabitsUiEvent.DraftNameChanged -> _state.update { it.copy(draftName = event.name) }
            HabitsUiEvent.ConfirmCreate -> viewModelScope.launch {
                val name = _state.value.draftName.trim()
                if (name.isNotEmpty()) {
                    habitRepository.addHabit(name, _state.value.habits.size)
                }
                _state.update { it.copy(activeSheet = null, draftName = "") }
            }
            HabitsUiEvent.ConfirmRename -> viewModelScope.launch {
                val sheet = _state.value.activeSheet as? HabitsSheet.Edit ?: return@launch
                val name = _state.value.draftName.trim()
                if (name.isNotEmpty()) {
                    habitRepository.updateHabitName(sheet.habitId, name)
                }
                _state.update { it.copy(activeSheet = null, draftName = "") }
            }
            HabitsUiEvent.ConfirmDelete -> viewModelScope.launch {
                val sheet = _state.value.activeSheet as? HabitsSheet.Edit ?: return@launch
                habitRepository.deleteHabit(sheet.habitId)
                _state.update { it.copy(activeSheet = null, draftName = "") }
            }
        }
    }

    private fun shiftWeek(days: Long) {
        val newStart = _state.value.weekStart.plusDays(days)
        _state.update { applyWeek(newStart, it) }
    }

    private fun applyWeek(weekStart: LocalDate, current: HabitsUiState): HabitsUiState {
        val weekDates = DateUtils.weekDates(weekStart)
        val weekEnd = weekDates.lastOrNull() ?: weekStart
        val weekLabel = "${weekStart.format(weekRangeFormatter)} – ${weekEnd.format(weekRangeFormatter)}"
        return current.copy(
            weekStart = weekStart,
            weekDates = weekDates,
            weekLabel = weekLabel,
            today = timeService.today(),
            dayHeaders = DateUtils.mondayDayHeaders,
        )
    }

    private fun showCompletionTooltip(habitId: String, date: LocalDate) {
        val dateStr = DateUtils.formatDate(date)
        val checkIn = _state.value.habits
            .find { it.habit.id == habitId }
            ?.checkIns
            ?.get(dateStr)
        if (checkIn?.status != CheckInStatus.DONE) return
        val completedAt = checkIn.completedAt ?: return
        val message = runCatching {
            val zoned = Instant.parse(completedAt).atZone(timeService.zone())
            val formatted = zoned.format(tooltipFormatter)
            "Completed on $formatted"
        }.getOrNull() ?: return
        _state.update { it.copy(tooltip = CompletionTooltipUi(message)) }
    }
}
