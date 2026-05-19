package com.stella.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stella.core.data.CalendarRepository
import com.stella.core.database.entity.CalendarEventEntity
import com.stella.core.util.TimeService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class CalendarEventUi(
    val id: String,
    val title: String,
    val localTimeLabel: String,
)

data class CalendarUiState(
    val month: YearMonth = YearMonth.now(),
    val events: List<CalendarEventEntity> = emptyList(),
    val eventUi: List<CalendarEventUi> = emptyList(),
    val eventDays: Set<LocalDate> = emptySet(),
    val today: LocalDate = LocalDate.now(),
    val newEventTitle: String = "",
    val isLoading: Boolean = true,
)

sealed interface CalendarUiEvent {
    data object PrevMonth : CalendarUiEvent
    data object NextMonth : CalendarUiEvent
    data class TitleChanged(val title: String) : CalendarUiEvent
    data object AddEvent : CalendarUiEvent
}

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val timeService: TimeService,
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarUiState(today = timeService.today()))
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    init {
        observeMonth(_state.value.month)
    }

    fun onEvent(event: CalendarUiEvent) {
        when (event) {
            CalendarUiEvent.PrevMonth -> {
                val month = _state.value.month.minusMonths(1)
                _state.update { it.copy(month = month) }
                observeMonth(month)
            }
            CalendarUiEvent.NextMonth -> {
                val month = _state.value.month.plusMonths(1)
                _state.update { it.copy(month = month) }
                observeMonth(month)
            }
            is CalendarUiEvent.TitleChanged -> _state.update { it.copy(newEventTitle = event.title) }
            CalendarUiEvent.AddEvent -> viewModelScope.launch {
                val title = _state.value.newEventTitle.trim()
                if (title.isEmpty()) return@launch
                val day = timeService.today()
                val start = timeService.toInstantIso(day, 10, 0)
                val end = timeService.toInstantIso(day, 11, 0)
                calendarRepository.addEvent(title, start, end)
                _state.update { it.copy(newEventTitle = "") }
            }
        }
    }

    private fun observeMonth(month: YearMonth) {
        viewModelScope.launch {
            calendarRepository.observeMonth(month).collect { events ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        events = events,
                        today = timeService.today(),
                        eventDays = events.map { e -> timeService.toLocalDate(e.startAt) }.toSet(),
                        eventUi = events.map { e ->
                            CalendarEventUi(
                                id = e.id,
                                title = e.title,
                                localTimeLabel = timeService.formatLocalDateTime(e.startAt),
                            )
                        },
                    )
                }
            }
        }
    }
}
