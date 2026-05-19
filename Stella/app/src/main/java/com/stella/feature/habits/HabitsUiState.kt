package com.stella.feature.habits

import com.stella.core.data.HabitWithCheckIns
import java.time.LocalDate

data class HabitsUiState(
    val habits: List<HabitWithCheckIns> = emptyList(),
    val weekStart: LocalDate = LocalDate.now().minusDays(6),
    val weekDates: List<LocalDate> = emptyList(),
    val today: LocalDate = LocalDate.now(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val newHabitName: String = "",
    val error: String? = null,
)

sealed interface HabitsUiEvent {
    data object Refresh : HabitsUiEvent
    data object PrevWeek : HabitsUiEvent
    data object NextWeek : HabitsUiEvent
    data class CellClicked(val habitId: String, val date: LocalDate) : HabitsUiEvent
    data object ShowAddDialog : HabitsUiEvent
    data object HideAddDialog : HabitsUiEvent
    data class NewHabitNameChanged(val name: String) : HabitsUiEvent
    data object ConfirmAddHabit : HabitsUiEvent
}
