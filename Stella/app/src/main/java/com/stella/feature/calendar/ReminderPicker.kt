package com.stella.feature.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stella.core.ui.theme.DawnCardSurface
import com.stella.core.ui.theme.Primary
import com.stella.core.ui.theme.TextPrimary
import com.stella.core.ui.theme.TextSecondary

data class ReminderOption(val minutesBefore: Int, val label: String)

val DEFAULT_REMINDER_OPTIONS = listOf(
    ReminderOption(0, "At time of event"),
    ReminderOption(15, "15 minutes before"),
    ReminderOption(60, "1 hour before"),
    ReminderOption(1440, "1 day before"),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderPicker(
    selectedOffsets: List<Int>,
    onSelectionChange: (List<Int>) -> Unit,
) {
    val chipColors = FilterChipDefaults.filterChipColors(
        labelColor = TextPrimary,
        containerColor = DawnCardSurface,
        selectedContainerColor = Primary.copy(alpha = 0.25f),
        selectedLabelColor = TextPrimary,
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Reminders", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
        Text(
            text = "Select one or more alerts (optional).",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DEFAULT_REMINDER_OPTIONS.forEach { option ->
                val selected = option.minutesBefore in selectedOffsets
                FilterChip(
                    selected = selected,
                    onClick = {
                        val next = if (selected) {
                            selectedOffsets - option.minutesBefore
                        } else {
                            selectedOffsets + option.minutesBefore
                        }
                        onSelectionChange(next.sorted())
                    },
                    label = { Text(option.label) },
                    colors = chipColors,
                )
            }
        }
    }
}
