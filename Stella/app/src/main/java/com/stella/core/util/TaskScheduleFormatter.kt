package com.stella.core.util

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

object TaskScheduleFormatter {
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

    fun formatChip(scheduledAtIso: String?, timeService: TimeService): String? {
        if (scheduledAtIso == null) return null
        return runCatching {
            val zoned = Instant.parse(scheduledAtIso).atZone(timeService.zone())
            val date = zoned.toLocalDate()
            val today = timeService.today()
            val dayLabel = when (date) {
                today -> "Today"
                today.plusDays(1) -> "Tomorrow"
                else -> date.format(dateFormatter)
            }
            val timeLabel = zoned.format(timeFormatter)
            "$dayLabel • $timeLabel"
        }.getOrNull()
    }
}
