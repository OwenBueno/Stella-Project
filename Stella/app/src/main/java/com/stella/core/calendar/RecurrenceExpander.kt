package com.stella.core.calendar

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class EventOccurrence(
    val eventId: String,
    val title: String,
    val startAt: String,
    val endAt: String,
    val linkedTaskId: String?,
)

object RecurrenceExpander {
    private const val MAX_OCCURRENCES = 366

    fun expand(
        eventId: String,
        title: String,
        startAtIso: String,
        endAtIso: String,
        linkedTaskId: String?,
        rule: RecurrenceRule,
        rangeStart: Instant,
        rangeEnd: Instant,
        zone: ZoneId,
    ): List<EventOccurrence> {
        val start = Instant.parse(startAtIso)
        val end = Instant.parse(endAtIso)
        val duration = ChronoUnit.SECONDS.between(start, end).coerceAtLeast(60)
        if (rule.frequency == RecurrenceFrequency.NONE) {
            return if (start <= rangeEnd && end >= rangeStart) {
                listOf(EventOccurrence(eventId, title, startAtIso, endAtIso, linkedTaskId))
            } else {
                emptyList()
            }
        }

        val untilInstant = rule.until?.let { runCatching { Instant.parse(it) }.getOrNull() }
        val occurrences = mutableListOf<EventOccurrence>()
        var cursorStart = start
        var index = 0

        while (cursorStart <= rangeEnd && index < MAX_OCCURRENCES) {
            if (untilInstant != null && cursorStart > untilInstant) break
            val cursorEnd = cursorStart.plusSeconds(duration)
            if (cursorStart <= rangeEnd && cursorEnd >= rangeStart) {
                occurrences += EventOccurrence(
                    eventId = eventId,
                    title = title,
                    startAt = cursorStart.toString(),
                    endAt = cursorEnd.toString(),
                    linkedTaskId = linkedTaskId,
                )
            }
            val next = nextStart(cursorStart, start, rule, zone) ?: break
            if (!next.isAfter(cursorStart)) break
            cursorStart = next
            index++
        }
        return occurrences
    }

    fun datesWithOccurrences(
        occurrences: List<EventOccurrence>,
        zone: ZoneId,
    ): Set<LocalDate> = occurrences.map { Instant.parse(it.startAt).atZone(zone).toLocalDate() }.toSet()

    private fun nextStart(
        current: Instant,
        seriesStart: Instant,
        rule: RecurrenceRule,
        zone: ZoneId,
    ): Instant? {
        val interval = rule.interval.coerceAtLeast(1)
        val zoned = current.atZone(zone)
        return when (rule.frequency) {
            RecurrenceFrequency.DAILY -> zoned.plusDays(interval.toLong()).toInstant()
            RecurrenceFrequency.WEEKLY -> zoned.plusWeeks(interval.toLong()).toInstant()
            RecurrenceFrequency.MONTHLY -> zoned.plusMonths(interval.toLong()).toInstant()
            RecurrenceFrequency.YEARLY -> zoned.plusYears(interval.toLong()).toInstant()
            RecurrenceFrequency.CUSTOM -> when (rule.customUnit) {
                CustomRecurrenceUnit.WEEKS -> {
                    val days = rule.daysOfWeek.sorted()
                    if (days.isEmpty()) {
                        zoned.plusWeeks(interval.toLong()).toInstant()
                    } else {
                        nextWeeklyOnDays(zoned, days, interval)
                    }
                }
                CustomRecurrenceUnit.DAYS, null -> zoned.plusDays(interval.toLong()).toInstant()
            }
            RecurrenceFrequency.NONE -> null
        }
    }

    private fun nextWeeklyOnDays(
        from: java.time.ZonedDateTime,
        daysOfWeek: List<Int>,
        intervalWeeks: Int,
    ): Instant {
        var probe = from.plusDays(1)
        val maxDays = 7 * intervalWeeks + 7
        repeat(maxDays) {
            val dow = probe.dayOfWeek.value
            if (dow in daysOfWeek) {
                val weeksSinceStart = ChronoUnit.WEEKS.between(
                    from.toLocalDate().with(java.time.DayOfWeek.MONDAY),
                    probe.toLocalDate().with(java.time.DayOfWeek.MONDAY),
                )
                if (weeksSinceStart % intervalWeeks == 0L) {
                    return probe.toInstant()
                }
            }
            probe = probe.plusDays(1)
        }
        return from.plusWeeks(intervalWeeks.toLong()).toInstant()
    }
}
