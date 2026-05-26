package com.stella.core.data

import com.stella.core.calendar.EventOccurrence
import com.stella.core.database.dao.HabitDao
import com.stella.core.database.dao.TaskDao
import com.stella.core.util.TimeService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

data class DayStatus(
    val hasCompletions: Boolean,
    val hasScheduledEvents: Boolean,
    val hasLinkedTaskEvents: Boolean = false,
)

data class CompletedActivityItem(
    val id: String,
    val title: String,
    val timeLabel: String,
    val kind: ActivityKind,
)

enum class ActivityKind { HABIT, TASK }

data class ScheduledEventItem(
    val eventId: String,
    val title: String,
    val timeLabel: String,
    val hasLinkedTask: Boolean,
)

data class DayLog(
    val completed: List<CompletedActivityItem>,
    val scheduled: List<ScheduledEventItem>,
)

@Singleton
class TemporalDayRepository @Inject constructor(
    private val habitDao: HabitDao,
    private val taskDao: TaskDao,
    private val calendarRepository: CalendarRepository,
    private val timeService: TimeService,
) {
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    fun observeMonthStatuses(month: YearMonth): Flow<Map<LocalDate, DayStatus>> =
        combine(
            calendarRepository.observeMonthExpanded(month),
            habitDao.observeCheckIns(monthDateFrom(month), monthDateTo(month)),
            taskDao.observeCompletedInRange(
                timeService.monthQueryRange(month).first,
                timeService.monthQueryRange(month).second,
            ),
        ) { expanded, checkIns, completedTasks ->
            buildMonthStatuses(month, expanded, checkIns, completedTasks)
        }

    fun buildDayLog(
        day: LocalDate,
        masters: List<com.stella.core.database.entity.CalendarEventEntity>,
        checkIns: List<com.stella.core.database.entity.HabitCheckInEntity>,
        habits: List<com.stella.core.database.entity.HabitEntity>,
        completedTasks: List<com.stella.core.database.entity.TaskEntity>,
    ): DayLog {
        val dateKey = timeService.dateKey(day)
        val habitNames = habits.associateBy { it.id }

        val completed = mutableListOf<CompletedActivityItem>()
        checkIns.filter { it.date == dateKey && it.status == "DONE" && it.completedAt != null }
            .forEach { checkIn ->
                val name = habitNames[checkIn.habitId]?.name ?: "Habit"
                val at = checkIn.completedAt ?: return@forEach
                completed += CompletedActivityItem(
                    id = checkIn.id,
                    title = name,
                    timeLabel = formatTime(at),
                    kind = ActivityKind.HABIT,
                )
            }

        val (dayFrom, dayTo) = timeService.dayInstantRange(day)
        completedTasks.filter { task ->
            task.updatedAt >= dayFrom && task.updatedAt <= dayTo
        }.forEach { task ->
            completed += CompletedActivityItem(
                id = task.id,
                title = task.title,
                timeLabel = formatTime(task.updatedAt),
                kind = ActivityKind.TASK,
            )
        }

        val scheduled = calendarRepository.occurrencesForDay(masters, day).map { occ ->
            ScheduledEventItem(
                eventId = occ.eventId,
                title = occ.title,
                timeLabel = formatTime(occ.startAt),
                hasLinkedTask = occ.linkedTaskId != null,
            )
        }

        return DayLog(
            completed = completed.sortedBy { it.timeLabel },
            scheduled = scheduled,
        )
    }

    private fun buildMonthStatuses(
        month: YearMonth,
        expanded: ExpandedMonthEvents,
        checkIns: List<com.stella.core.database.entity.HabitCheckInEntity>,
        completedTasks: List<com.stella.core.database.entity.TaskEntity>,
    ): Map<LocalDate, DayStatus> {
        val result = mutableMapOf<LocalDate, DayStatus>()
        val daysInMonth = month.lengthOfMonth()
        val completionDates = mutableSetOf<LocalDate>()
        checkIns.filter { it.status == "DONE" && it.completedAt != null }
            .forEach { runCatching { LocalDate.parse(it.date) }.getOrNull()?.let(completionDates::add) }
        completedTasks.forEach { task ->
            runCatching { timeService.toLocalDate(task.updatedAt) }.getOrNull()?.let(completionDates::add)
        }

        val linkedDates = mutableSetOf<LocalDate>()
        expanded.occurrences.filter { it.linkedTaskId != null }.forEach { occ ->
            runCatching { timeService.toLocalDate(occ.startAt) }.getOrNull()?.let(linkedDates::add)
        }

        for (day in 1..daysInMonth) {
            val date = month.atDay(day)
            val hasEvents = date in expanded.eventDates
            result[date] = DayStatus(
                hasCompletions = date in completionDates,
                hasScheduledEvents = hasEvents,
                hasLinkedTaskEvents = date in linkedDates,
            )
        }
        return result
    }

    private fun formatTime(iso: String): String =
        runCatching {
            Instant.parse(iso).atZone(timeService.zone()).format(timeFormatter)
        }.getOrDefault("")

    private fun monthDateFrom(month: YearMonth): String = month.atDay(1).toString()
    private fun monthDateTo(month: YearMonth): String = month.atEndOfMonth().toString()
}
