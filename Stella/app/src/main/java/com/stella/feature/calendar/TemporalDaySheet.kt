package com.stella.feature.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stella.core.data.ActivityKind
import com.stella.core.data.CompletedActivityItem
import com.stella.core.data.ScheduledEventItem
import com.stella.core.ui.theme.Primary
import com.stella.core.ui.theme.TextMuted
import com.stella.core.ui.theme.TextPrimary
import com.stella.core.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dayFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemporalDaySheet(
    day: LocalDate,
    completed: List<CompletedActivityItem>,
    scheduled: List<ScheduledEventItem>,
    onAddEvent: () -> Unit,
    onEditEvent: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = day.format(dayFormatter),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary,
            )

            SectionTitle("Completed Activities")
            if (completed.isEmpty()) {
                EmptyHint("Nothing completed on this day yet.")
            } else {
                completed.forEach { item ->
                    LogRow(
                        title = item.title,
                        subtitle = item.timeLabel,
                        prefix = if (item.kind == ActivityKind.HABIT) "Habit" else "Task",
                    )
                }
            }

            SectionTitle("Scheduled Events")
            if (scheduled.isEmpty()) {
                EmptyHint("No events scheduled for this day.")
            } else {
                scheduled.forEach { item ->
                    LogRow(
                        title = item.title,
                        subtitle = item.timeLabel,
                        prefix = if (item.hasLinkedTask) "Linked" else "Event",
                        onClick = { onEditEvent(item.eventId) },
                    )
                }
            }

            Button(onClick = onAddEvent, modifier = Modifier.fillMaxWidth()) {
                Text("Add event")
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = TextSecondary,
    )
}

@Composable
private fun EmptyHint(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = TextMuted)
}

@Composable
private fun LogRow(
    title: String,
    subtitle: String,
    prefix: String,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 6.dp),
    ) {
        Text(
            text = "$prefix • $title — $subtitle",
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
        )
    }
}
