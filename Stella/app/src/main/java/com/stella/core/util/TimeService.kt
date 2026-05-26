package com.stella.core.util

import com.stella.core.data.SettingsRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeService @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    fun zone(): ZoneId = runCatching {
        ZoneId.of(settingsRepository.effectiveTimeZoneId())
    }.getOrDefault(ZoneId.systemDefault())

    fun today(): LocalDate = LocalDate.now(zone())

    fun toInstantIso(date: LocalDate, hour: Int, minute: Int): String =
        date.atTime(
            hour.coerceIn(0, 23),
            minute.coerceIn(0, 59),
        ).atZone(zone()).toInstant().toString()

    fun toInstantIso(date: LocalDate, time: LocalTime): String =
        toInstantIso(date, time.hour, time.minute)

    fun toLocalDate(instantIso: String): LocalDate =
        Instant.parse(instantIso).atZone(zone()).toLocalDate()

    fun toLocalTime(instantIso: String): LocalTime =
        Instant.parse(instantIso).atZone(zone()).toLocalTime()

    fun formatLocalDateTime(instantIso: String): String {
        val zoned = Instant.parse(instantIso).atZone(zone())
        return zoned.format(DateTimeFormatter.ofPattern("MMM d, HH:mm"))
    }

    fun monthQueryRange(month: YearMonth): Pair<String, String> {
        val start = month.atDay(1).atStartOfDay(zone()).toInstant().toString()
        val end = month.atEndOfMonth().atTime(23, 59, 59).atZone(zone()).toInstant().toString()
        return start to end
    }

    fun dayInstantRange(date: LocalDate): Pair<String, String> {
        val start = date.atStartOfDay(zone()).toInstant().toString()
        val end = date.atTime(23, 59, 59).atZone(zone()).toInstant().toString()
        return start to end
    }

    fun dateKey(date: LocalDate): String = date.toString()

    fun zoneDisplayName(): String = zone().id
}
