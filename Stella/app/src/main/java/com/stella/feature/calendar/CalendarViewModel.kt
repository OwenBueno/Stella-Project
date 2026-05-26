package com.stella.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stella.core.calendar.CalendarEventJson
import com.stella.core.data.CalendarRepository
import com.stella.core.data.TemporalDayRepository
import com.stella.core.database.dao.HabitDao
import com.stella.core.database.dao.TaskDao
import com.stella.core.util.TimeService
import com.stella.feature.tasks.snapScheduleMinute
import com.stella.sync.EventReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.stella.core.data.CompletedActivityItem
import com.stella.core.data.ScheduledEventItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val temporalDayRepository: TemporalDayRepository,
    private val habitDao: HabitDao,
    private val taskDao: TaskDao,
    private val timeService: TimeService,
    private val eventReminderScheduler: EventReminderScheduler,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.from(timeService.today()))
    private val selectedDay = MutableStateFlow<LocalDate?>(null)
    private val eventMasters = MutableStateFlow(emptyList<com.stella.core.database.entity.CalendarEventEntity>())

    private val _state = MutableStateFlow(
        CalendarUiState(
            month = YearMonth.from(timeService.today()),
            today = timeService.today(),
        ),
    )
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            month.flatMapLatest { currentMonth ->
                combine(
                    calendarRepository.observeMonthExpanded(currentMonth),
                    temporalDayRepository.observeMonthStatuses(currentMonth),
                    selectedDay,
                ) { expanded, statuses, day ->
                    eventMasters.value = expanded.masters
                    CalendarUiState(
                        month = currentMonth,
                        today = timeService.today(),
                        isLoading = false,
                        dayStatuses = statuses,
                        eventMasters = expanded.masters,
                        selectedDay = day,
                        showDaySheet = day != null && _state.value.showDaySheet,
                        dayCompleted = _state.value.dayCompleted,
                        dayScheduled = _state.value.dayScheduled,
                        showEventEditor = _state.value.showEventEditor,
                        editorMode = _state.value.editorMode,
                        editorDraft = _state.value.editorDraft,
                    )
                }
            }.collect { merged ->
                _state.value = merged
            }
        }
        viewModelScope.launch {
            selectedDay.flatMapLatest { day ->
                if (day == null) {
                    flowOf(emptyList<CompletedActivityItem>() to emptyList<ScheduledEventItem>())
                } else {
                    val dateKey = timeService.dateKey(day)
                    val (from, to) = timeService.dayInstantRange(day)
                    combine(
                        habitDao.observeCheckIns(dateKey, dateKey),
                        habitDao.observeActiveHabits(),
                        taskDao.observeCompletedInRange(from, to),
                        eventMasters,
                    ) { checkIns, habits, tasks, masters ->
                        val log = temporalDayRepository.buildDayLog(day, masters, checkIns, habits, tasks)
                        log.completed to log.scheduled
                    }
                }
            }.collect { (completed, scheduled) ->
                _state.update { it.copy(dayCompleted = completed, dayScheduled = scheduled) }
            }
        }
    }

    fun onEvent(event: CalendarUiEvent) {
        when (event) {
            CalendarUiEvent.PrevMonth -> shiftMonth(-1)
            CalendarUiEvent.NextMonth -> shiftMonth(1)
            is CalendarUiEvent.SelectDay -> openDay(event.date)
            CalendarUiEvent.DismissDaySheet -> {
                selectedDay.value = null
                _state.update { it.copy(showDaySheet = false, selectedDay = null) }
            }
            CalendarUiEvent.OpenCreateEvent -> openCreateEditor()
            is CalendarUiEvent.OpenEditEvent -> openEditEditor(event.eventId)
            CalendarUiEvent.DismissEventEditor -> _state.update {
                it.copy(showEventEditor = false, editorDraft = EventEditorDraft())
            }
            is CalendarUiEvent.EditorTitleChanged -> updateDraft { it.copy(title = event.title, error = null) }
            is CalendarUiEvent.EditorStartDateChanged -> updateDraft { it.copy(startDate = event.date) }
            is CalendarUiEvent.EditorEndDateChanged -> updateDraft { it.copy(endDate = event.date) }
            is CalendarUiEvent.EditorStartHourChanged -> updateDraft {
                it.copy(startTime = LocalTime.of(event.hour.coerceIn(0, 23), it.startTime.minute))
            }
            is CalendarUiEvent.EditorStartMinuteChanged -> updateDraft {
                it.copy(startTime = LocalTime.of(it.startTime.hour, snapScheduleMinute(event.minute)))
            }
            is CalendarUiEvent.EditorEndHourChanged -> updateDraft {
                it.copy(endTime = LocalTime.of(event.hour.coerceIn(0, 23), it.endTime.minute))
            }
            is CalendarUiEvent.EditorEndMinuteChanged -> updateDraft {
                it.copy(endTime = LocalTime.of(it.endTime.hour, snapScheduleMinute(event.minute)))
            }
            is CalendarUiEvent.EditorRecurrenceChanged -> updateDraft { it.copy(recurrence = event.rule) }
            is CalendarUiEvent.EditorRemindersChanged -> updateDraft {
                it.copy(reminderOffsetsMinutes = event.offsets)
            }
            CalendarUiEvent.SaveEvent -> saveEvent()
            CalendarUiEvent.DeleteEvent -> deleteEvent()
        }
    }

    fun openDayFromDeepLink(date: LocalDate) {
        month.value = YearMonth.from(date)
        openDay(date)
    }

    private fun shiftMonth(delta: Int) {
        month.value = _state.value.month.plusMonths(delta.toLong())
    }

    private fun openDay(date: LocalDate) {
        selectedDay.value = date
        _state.update { it.copy(selectedDay = date, showDaySheet = true) }
    }

    private fun openCreateEditor() {
        val day = _state.value.selectedDay ?: timeService.today()
        _state.update {
            it.copy(
                showEventEditor = true,
                editorMode = EventEditorMode.CREATE,
                editorDraft = EventEditorDraft(
                    startDate = day,
                    endDate = day,
                    startTime = LocalTime.of(9, 0),
                    endTime = LocalTime.of(10, 0),
                ),
            )
        }
    }

    private fun openEditEditor(eventId: String) {
        viewModelScope.launch {
            val event = calendarRepository.getById(eventId) ?: return@launch
            _state.update {
                it.copy(
                    showEventEditor = true,
                    showDaySheet = false,
                    editorMode = EventEditorMode.EDIT,
                    editorDraft = EventEditorDraft(
                        eventId = event.id,
                        title = event.title,
                        startDate = timeService.toLocalDate(event.startAt),
                        endDate = timeService.toLocalDate(event.endAt),
                        startTime = timeService.toLocalTime(event.startAt),
                        endTime = timeService.toLocalTime(event.endAt),
                        recurrence = CalendarEventJson.decodeRecurrence(event.recurrenceRuleJson),
                        reminderOffsetsMinutes = CalendarEventJson.decodeReminderOffsets(
                            event.reminderOffsetsJson,
                        ),
                    ),
                )
            }
        }
    }

    private fun saveEvent() {
        val draft = _state.value.editorDraft
        val title = draft.title.trim()
        if (title.isEmpty()) {
            updateDraft { it.copy(error = "Enter an event title.") }
            return
        }
        val startIso = timeService.toInstantIso(draft.startDate, draft.startTime)
        val endIso = timeService.toInstantIso(draft.endDate, draft.endTime)
        if (Instant.parse(endIso) <= Instant.parse(startIso)) {
            updateDraft { it.copy(error = "End time must be after start time.") }
            return
        }
        viewModelScope.launch {
            val entity = when (_state.value.editorMode) {
                EventEditorMode.CREATE -> {
                    val id = calendarRepository.addEvent(
                        title = title,
                        startAt = startIso,
                        endAt = endIso,
                        recurrence = draft.recurrence,
                        reminderOffsetsMinutes = draft.reminderOffsetsMinutes,
                    )
                    calendarRepository.getById(id)!!
                }
                EventEditorMode.EDIT -> {
                    val existing = calendarRepository.getById(draft.eventId!!) ?: return@launch
                    val updated = existing.copy(
                        title = title,
                        startAt = startIso,
                        endAt = endIso,
                        recurrenceRuleJson = CalendarEventJson.encodeRecurrence(draft.recurrence),
                        reminderOffsetsJson = CalendarEventJson.encodeReminderOffsets(
                            draft.reminderOffsetsMinutes,
                        ),
                    )
                    calendarRepository.updateEvent(updated)
                    updated
                }
            }
            eventReminderScheduler.rescheduleEvent(entity)
            _state.update {
                it.copy(showEventEditor = false, editorDraft = EventEditorDraft())
            }
        }
    }

    private fun deleteEvent() {
        val id = _state.value.editorDraft.eventId ?: return
        viewModelScope.launch {
            val existing = calendarRepository.getById(id)
            calendarRepository.deleteEvent(id)
            existing?.let {
                eventReminderScheduler.cancelEvent(
                    id,
                    CalendarEventJson.decodeReminderOffsets(it.reminderOffsetsJson),
                )
            }
            _state.update {
                it.copy(
                    showEventEditor = false,
                    showDaySheet = false,
                    selectedDay = null,
                    editorDraft = EventEditorDraft(),
                )
            }
            selectedDay.value = null
        }
    }

    private fun updateDraft(transform: (EventEditorDraft) -> EventEditorDraft) {
        _state.update { it.copy(editorDraft = transform(it.editorDraft)) }
    }
}
