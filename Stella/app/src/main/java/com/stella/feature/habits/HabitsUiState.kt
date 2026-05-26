package com.stella.feature.habits

import com.stella.core.data.HabitWithCheckIns
import com.stella.core.util.DateUtils
import java.time.LocalDate

data class CompletionTooltipUi(
    val message: String,
)

sealed interface HabitsSheet {
    data object Create : HabitsSheet
    data class Edit(val habitId: String, val habitName: String) : HabitsSheet
}

data class HabitsUiState(
    val habits: List<HabitWithCheckIns> = emptyList(),
    val weekStart: LocalDate = LocalDate.now(),
    val weekDates: List<LocalDate> = emptyList(),
    val weekLabel: String = "",
    val dayHeaders: List<String> = DateUtils.mondayDayHeaders,
    val today: LocalDate = LocalDate.now(),
    val isLoading: Boolean = true,
    val activeSheet: HabitsSheet? = null,
    val draftName: String = "",
    val tooltip: CompletionTooltipUi? = null,
    val error: String? = null,
)

sealed interface HabitsUiEvent {
    data object Refresh : HabitsUiEvent
    data object PrevWeek : HabitsUiEvent
    data object NextWeek : HabitsUiEvent
    data class CellClicked(val habitId: String, val date: LocalDate) : HabitsUiEvent
    data class CellLongPressed(val habitId: String, val date: LocalDate) : HabitsUiEvent
    data object DismissTooltip : HabitsUiEvent
    data object ShowCreateSheet : HabitsUiEvent
    data object HideSheet : HabitsUiEvent
    data class ShowEditSheet(val habitId: String) : HabitsUiEvent
    data class DraftNameChanged(val name: String) : HabitsUiEvent
    data object ConfirmCreate : HabitsUiEvent
    data object ConfirmRename : HabitsUiEvent
    data object ConfirmDelete : HabitsUiEvent
}
