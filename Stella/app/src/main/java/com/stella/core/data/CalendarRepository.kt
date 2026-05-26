package com.stella.core.data

import com.stella.core.calendar.CalendarEventJson
import com.stella.core.calendar.EventOccurrence
import com.stella.core.calendar.RecurrenceExpander
import com.stella.core.calendar.RecurrenceRule
import com.stella.core.database.dao.CalendarEventDao
import com.stella.core.database.entity.CalendarEventEntity
import com.stella.core.util.TimeService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class ExpandedMonthEvents(
    val masters: List<CalendarEventEntity>,
    val occurrences: List<EventOccurrence>,
    val eventDates: Set<java.time.LocalDate>,
)

@Singleton
class CalendarRepository @Inject constructor(
    private val calendarEventDao: CalendarEventDao,
    private val timeService: TimeService,
) {
    fun observeMonth(month: YearMonth): Flow<List<CalendarEventEntity>> {
        val (from, to) = timeService.monthQueryRange(month)
        return calendarEventDao.observeInRange(from, to)
    }

    fun observeMonthExpanded(month: YearMonth): Flow<ExpandedMonthEvents> =
        calendarEventDao.observeAllActive().map { masters -> expandForRange(masters, month) }

    suspend fun getById(id: String): CalendarEventEntity? = calendarEventDao.getById(id)

    suspend fun addEvent(
        title: String,
        startAt: String,
        endAt: String,
        linkedTaskId: String? = null,
        recurrence: RecurrenceRule = RecurrenceRule(),
        reminderOffsetsMinutes: List<Int> = emptyList(),
    ): String {
        val now = Instant.now().toString()
        val id = UUID.randomUUID().toString()
        calendarEventDao.upsert(
            CalendarEventEntity(
                id = id,
                title = title.trim(),
                startAt = startAt,
                endAt = endAt,
                linkedTaskId = linkedTaskId,
                recurrenceRuleJson = CalendarEventJson.encodeRecurrence(recurrence),
                reminderOffsetsJson = CalendarEventJson.encodeReminderOffsets(reminderOffsetsMinutes),
                createdAt = now,
                updatedAt = now,
                needsSync = true,
            ),
        )
        return id
    }

    suspend fun addEvent(title: String, startAt: String, endAt: String, linkedTaskId: String?) {
        addEvent(title, startAt, endAt, linkedTaskId, RecurrenceRule(), emptyList())
    }

    suspend fun updateEvent(entity: CalendarEventEntity) {
        calendarEventDao.upsert(
            entity.copy(
                updatedAt = Instant.now().toString(),
                needsSync = true,
            ),
        )
    }

    suspend fun deleteEvent(id: String) {
        val event = calendarEventDao.getById(id) ?: return
        val now = Instant.now().toString()
        calendarEventDao.upsert(
            event.copy(
                deletedAt = now,
                updatedAt = now,
                needsSync = true,
            ),
        )
    }

    fun occurrencesForDay(
        masters: List<CalendarEventEntity>,
        day: java.time.LocalDate,
    ): List<EventOccurrence> {
        val (from, to) = timeService.dayInstantRange(day)
        val rangeStart = Instant.parse(from)
        val rangeEnd = Instant.parse(to)
        val zone = timeService.zone()
        return masters.flatMap { master ->
            RecurrenceExpander.expand(
                eventId = master.id,
                title = master.title,
                startAtIso = master.startAt,
                endAtIso = master.endAt,
                linkedTaskId = master.linkedTaskId,
                rule = CalendarEventJson.decodeRecurrence(master.recurrenceRuleJson),
                rangeStart = rangeStart,
                rangeEnd = rangeEnd,
                zone = zone,
            )
        }.sortedBy { it.startAt }
    }

    private fun expandForRange(
        masters: List<CalendarEventEntity>,
        month: YearMonth,
    ): ExpandedMonthEvents {
        val (from, to) = timeService.monthQueryRange(month)
        val rangeStart = Instant.parse(from)
        val rangeEnd = Instant.parse(to)
        val zone = timeService.zone()
        val occurrences = masters.flatMap { master ->
            RecurrenceExpander.expand(
                eventId = master.id,
                title = master.title,
                startAtIso = master.startAt,
                endAtIso = master.endAt,
                linkedTaskId = master.linkedTaskId,
                rule = CalendarEventJson.decodeRecurrence(master.recurrenceRuleJson),
                rangeStart = rangeStart,
                rangeEnd = rangeEnd,
                zone = zone,
            )
        }
        return ExpandedMonthEvents(
            masters = masters,
            occurrences = occurrences,
            eventDates = RecurrenceExpander.datesWithOccurrences(occurrences, zone),
        )
    }
}
