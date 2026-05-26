package com.stella.core.calendar

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object CalendarEventJson {
    private val json = Json { ignoreUnknownKeys = true }

    fun encodeRecurrence(rule: RecurrenceRule?): String? =
        rule?.takeIf { it.frequency != RecurrenceFrequency.NONE }
            ?.let { json.encodeToString(it) }

    fun decodeRecurrence(raw: String?): RecurrenceRule =
        raw?.takeIf { it.isNotBlank() }?.let {
            runCatching { json.decodeFromString<RecurrenceRule>(it) }.getOrNull()
        } ?: RecurrenceRule()

    fun encodeReminderOffsets(offsets: List<Int>): String? =
        offsets.takeIf { it.isNotEmpty() }?.let { json.encodeToString(it.sorted()) }

    fun decodeReminderOffsets(raw: String?): List<Int> =
        raw?.takeIf { it.isNotBlank() }?.let {
            runCatching { json.decodeFromString<List<Int>>(it) }.getOrElse { emptyList() }
        } ?: emptyList()
}
