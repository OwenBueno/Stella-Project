package com.stella.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stella.core.ui.components.StellaLabel
import com.stella.core.ui.components.StellaSectionHeader
import com.stella.core.ui.components.stellaTextFieldColors
import com.stella.core.ui.theme.Border
import com.stella.core.ui.theme.Primary
import com.stella.core.ui.theme.TextPrimary
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val month = state.month
    val today = state.today

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StellaSectionHeader(eyebrow = "Temporal", title = "Temporal Grid")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { viewModel.onEvent(CalendarUiEvent.PrevMonth) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month", tint = TextPrimary)
            }
            Text(
                month.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            IconButton(onClick = { viewModel.onEvent(CalendarUiEvent.NextMonth) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next month", tint = TextPrimary)
            }
        }

        MonthGrid(month = month, today = today, eventDays = state.eventDays)

        OutlinedTextField(
            value = state.newEventTitle,
            onValueChange = { viewModel.onEvent(CalendarUiEvent.TitleChanged(it)) },
            label = { Text("New event") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = stellaTextFieldColors(),
        )
        TextButton(onClick = { viewModel.onEvent(CalendarUiEvent.AddEvent) }) {
            Text("Add to month", color = Primary)
        }

        state.eventUi.forEach { event ->
            Text("• ${event.title} — ${event.localTimeLabel}", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    eventDays: Set<LocalDate>,
) {
    val firstDay = month.atDay(1)
    val daysInMonth = month.lengthOfMonth()
    val startOffset = firstDay.dayOfWeek.value % 7

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                StellaLabel(text = it, modifier = Modifier.weight(1f))
            }
        }
        val cells = (0 until startOffset).map { null } +
            (1..daysInMonth).map { month.atDay(it) }
        cells.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { date ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .border(
                                1.dp,
                                if (date == today) Primary else Border,
                            )
                            .background(
                                if (date != null && eventDays.contains(date)) {
                                    Primary.copy(alpha = 0.2f)
                                } else {
                                    androidx.compose.ui.graphics.Color.Transparent
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        date?.let {
                            Text(
                                text = it.dayOfMonth.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary,
                            )
                        }
                    }
                }
                repeat(7 - week.size) {
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                }
            }
        }
    }
}
