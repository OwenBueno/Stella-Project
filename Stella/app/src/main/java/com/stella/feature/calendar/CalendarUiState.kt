package com.stella.feature.calendar

import com.stella.core.calendar.RecurrenceRule
import com.stella.core.data.CompletedActivityItem
import com.stella.core.data.DayStatus
import com.stella.core.data.ScheduledEventItem
import com.stella.core.database.entity.CalendarEventEntity
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

enum class EventEditorMode { CREATE, EDIT }

data class EventEditorDraft(
    val eventId: String? = null,
    val title: String = "",
    val startDate: LocalDate = LocalDate.now(),
    val startTime: LocalTime = LocalTime.of(9, 0),
    val endDate: LocalDate = LocalDate.now(),
    val endTime: LocalTime = LocalTime.of(10, 0),
    val recurrence: RecurrenceRule = RecurrenceRule(),
    val reminderOffsetsMinutes: List<Int> = emptyList(),
    val error: String? = null,
)

data class CalendarUiState(
    val month: YearMonth = YearMonth.now(),
    val today: LocalDate = LocalDate.now(),
    val dayStatuses: Map<LocalDate, DayStatus> = emptyMap(),
    val eventMasters: List<CalendarEventEntity> = emptyList(),
    val selectedDay: LocalDate? = null,
    val showDaySheet: Boolean = false,
    val dayCompleted: List<CompletedActivityItem> = emptyList(),
    val dayScheduled: List<ScheduledEventItem> = emptyList(),
    val showEventEditor: Boolean = false,
    val editorMode: EventEditorMode = EventEditorMode.CREATE,
    val editorDraft: EventEditorDraft = EventEditorDraft(),
    val isLoading: Boolean = true,
)

sealed interface CalendarUiEvent {
    data object PrevMonth : CalendarUiEvent
    data object NextMonth : CalendarUiEvent
    data class SelectDay(val date: LocalDate) : CalendarUiEvent
    data object DismissDaySheet : CalendarUiEvent
    data object OpenCreateEvent : CalendarUiEvent
    data class OpenEditEvent(val eventId: String) : CalendarUiEvent
    data object DismissEventEditor : CalendarUiEvent
    data class EditorTitleChanged(val title: String) : CalendarUiEvent
    data class EditorStartDateChanged(val date: LocalDate) : CalendarUiEvent
    data class EditorEndDateChanged(val date: LocalDate) : CalendarUiEvent
    data class EditorStartHourChanged(val hour: Int) : CalendarUiEvent
    data class EditorStartMinuteChanged(val minute: Int) : CalendarUiEvent
    data class EditorEndHourChanged(val hour: Int) : CalendarUiEvent
    data class EditorEndMinuteChanged(val minute: Int) : CalendarUiEvent
    data class EditorRecurrenceChanged(val rule: RecurrenceRule) : CalendarUiEvent
    data class EditorRemindersChanged(val offsets: List<Int>) : CalendarUiEvent
    data object SaveEvent : CalendarUiEvent
    data object DeleteEvent : CalendarUiEvent
}
