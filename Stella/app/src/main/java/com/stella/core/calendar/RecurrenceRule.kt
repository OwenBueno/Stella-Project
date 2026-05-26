package com.stella.core.calendar

import kotlinx.serialization.Serializable

@Serializable
enum class RecurrenceFrequency {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    CUSTOM,
}

@Serializable
data class RecurrenceRule(
    val frequency: RecurrenceFrequency = RecurrenceFrequency.NONE,
    val interval: Int = 1,
    val daysOfWeek: List<Int> = emptyList(),
    val until: String? = null,
) {
    val customUnit: CustomRecurrenceUnit?
        get() = if (frequency == RecurrenceFrequency.CUSTOM) {
            when {
                daysOfWeek.isNotEmpty() -> CustomRecurrenceUnit.WEEKS
                else -> CustomRecurrenceUnit.DAYS
            }
        } else {
            null
        }
}

enum class CustomRecurrenceUnit {
    DAYS,
    WEEKS,
}
