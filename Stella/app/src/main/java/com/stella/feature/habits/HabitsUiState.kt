package com.stella.feature.habits

data class HabitRowUi(
    val id: String,
    val name: String,
)

data class HabitsUiState(
    val habits: List<HabitRowUi> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

sealed interface HabitsUiEvent {
    data object Refresh : HabitsUiEvent
}
