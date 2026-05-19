package com.stella.feature.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stella.core.data.HabitRepository
import com.stella.core.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HabitsUiState())
    val state: StateFlow<HabitsUiState> = _state.asStateFlow()

    init {
        onEvent(HabitsUiEvent.Refresh)
    }

    fun onEvent(event: HabitsUiEvent) {
        when (event) {
            HabitsUiEvent.Refresh -> observeHabits()
            HabitsUiEvent.PrevWeek -> _state.update {
                val start = it.weekStart.minusDays(7)
                it.copy(weekStart = start, weekDates = DateUtils.weekDates(start))
            }
            HabitsUiEvent.NextWeek -> _state.update {
                val start = it.weekStart.plusDays(7)
                it.copy(weekStart = start, weekDates = DateUtils.weekDates(start))
            }
            is HabitsUiEvent.CellClicked -> viewModelScope.launch {
                habitRepository.toggleCheckIn(event.habitId, event.date)
            }
            HabitsUiEvent.ShowAddDialog -> _state.update { it.copy(showAddDialog = true) }
            HabitsUiEvent.HideAddDialog -> _state.update {
                it.copy(showAddDialog = false, newHabitName = "")
            }
            is HabitsUiEvent.NewHabitNameChanged -> _state.update { it.copy(newHabitName = event.name) }
            HabitsUiEvent.ConfirmAddHabit -> viewModelScope.launch {
                val name = _state.value.newHabitName.trim()
                if (name.isNotEmpty()) {
                    habitRepository.addHabit(name, _state.value.habits.size)
                }
                _state.update { it.copy(showAddDialog = false, newHabitName = "") }
            }
        }
    }

    private fun observeHabits() {
        val weekStart = _state.value.weekStart
        val weekDates = DateUtils.weekDates(weekStart)
        _state.update { it.copy(weekDates = weekDates, today = DateUtils.today()) }
        viewModelScope.launch {
            habitRepository.observeHabitsWithCheckIns(weekStart).collect { habits ->
                _state.update {
                    it.copy(isLoading = false, habits = habits, error = null)
                }
            }
        }
    }
}
