package com.stella.core.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DateUtils {
    val isoDate: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    val isoDateTime: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT

    /** Prefer [com.stella.core.util.TimeService.today] for timezone-aware "today". */
    fun today(): LocalDate = LocalDate.now()

    fun formatDate(date: LocalDate): String = date.format(isoDate)

    fun parseDate(value: String): LocalDate = LocalDate.parse(value, isoDate)

    fun weekDates(weekStart: LocalDate, days: Int = 7): List<LocalDate> =
        (0 until days).map { weekStart.plusDays(it.toLong()) }
}
