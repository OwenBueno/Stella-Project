package com.stella.core.util

import com.stella.core.data.CheckInStatus
import com.stella.core.data.HabitWithCheckIns
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate

@Serializable
data class HabitGridSnapshot(
    val weekStart: String,
    val cells: List<HabitGridCell>,
)

@Serializable
data class HabitGridCell(
    val habitId: String,
    val date: String,
    val status: String,
)

object HabitGridSnapshotUtil {
    private val json = Json { ignoreUnknownKeys = true }

    fun build(habits: List<HabitWithCheckIns>, weekDates: List<LocalDate>): String {
        val weekStart = weekDates.firstOrNull()?.let { DateUtils.formatDate(it) } ?: ""
        val cells = habits.flatMap { row ->
            weekDates.mapNotNull { date ->
                val checkIn = row.checkIns[DateUtils.formatDate(date)] ?: return@mapNotNull null
                HabitGridCell(
                    habitId = row.habit.id,
                    date = DateUtils.formatDate(date),
                    status = checkIn.status.name,
                )
            }
        }
        return json.encodeToString(HabitGridSnapshot(weekStart, cells))
    }
}
