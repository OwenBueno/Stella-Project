package com.stella.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stella.core.data.DayStatus
import com.stella.core.ui.theme.MorningLockPulseCore
import com.stella.core.ui.theme.Primary
import com.stella.core.ui.theme.PrimaryGlow
import com.stella.core.ui.theme.Success
import com.stella.core.ui.theme.TextMuted
import com.stella.core.ui.theme.TextPrimary
import com.stella.core.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val monthTitleFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

@Composable
fun MonthNavigator(
    month: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month", tint = TextPrimary)
        }
        Text(
            text = month.format(monthTitleFormatter),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary,
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next month", tint = TextPrimary)
        }
    }
}

@Composable
fun TemporalMonthGrid(
    month: YearMonth,
    today: LocalDate,
    dayStatuses: Map<LocalDate, DayStatus>,
    onDayClick: (LocalDate) -> Unit,
) {
    val firstDay = month.atDay(1)
    val daysInMonth = month.lengthOfMonth()
    val startOffset = firstDay.dayOfWeek.value % 7
    val cells = (0 until startOffset).map { null as LocalDate? } +
        (1..daysInMonth).map { month.atDay(it) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                week.forEach { date ->
                    TemporalDayCell(
                        date = date,
                        isToday = date == today,
                        status = date?.let { dayStatuses[it] },
                        onClick = { date?.let(onDayClick) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(7 - week.size) {
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                }
            }
        }
    }
}

@Composable
private fun TemporalDayCell(
    date: LocalDate?,
    isToday: Boolean,
    status: DayStatus?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .then(
                if (date != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isToday && date != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PrimaryGlow.copy(alpha = 0.35f)),
            )
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Primary),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            date?.let {
                Text(
                    text = it.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    ),
                    color = if (isToday) TextPrimary else TextSecondary,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    if (status?.hasCompletions == true) {
                        StatusDot(color = Success)
                    }
                    if (status?.hasScheduledEvents == true) {
                        StatusDot(color = Primary)
                    }
                    if (status?.hasLinkedTaskEvents == true) {
                        StatusDot(color = MorningLockPulseCore)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusDot(color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .size(4.dp)
            .clip(CircleShape)
            .background(color),
    )
}
