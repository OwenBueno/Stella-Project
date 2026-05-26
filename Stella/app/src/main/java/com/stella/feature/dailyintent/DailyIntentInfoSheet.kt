package com.stella.feature.dailyintent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stella.core.ui.components.stellaTextFieldColors
import com.stella.core.ui.theme.TextPrimary
import com.stella.core.ui.theme.TextSecondary

private val HOUR_OPTIONS = (6..22).toList()
private val MINUTE_OPTIONS = listOf(0, 15, 30, 45)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyIntentInfoSheet(
    timeZoneLabel: String,
    defaultDurationMinutes: Int,
    plannedTasks: List<PlannedTaskItem>,
    blockSchedules: Map<String, BlockSchedule>,
    onHourChange: (String, Int) -> Unit,
    onMinuteChange: (String, Int) -> Unit,
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Schedule defaults",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Text(
                "Timezone: $timeZoneLabel",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            Text(
                "Block length: $defaultDurationMinutes min per task",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            Text(
                "Set start times for each planned task. Calendar blocks are created when you start your day.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            if (plannedTasks.isEmpty()) {
                Text(
                    "Add tasks to your plan to set block times.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            } else {
                plannedTasks.forEachIndexed { index, item ->
                    val schedule = blockSchedules[item.taskId]
                        ?: BlockSchedule(9 + (index % 3) * 3, 0, defaultDurationMinutes)
                    TaskTimeRow(
                        label = item.title,
                        schedule = schedule,
                        onHourChange = { onHourChange(item.taskId, it) },
                        onMinuteChange = { onMinuteChange(item.taskId, it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskTimeRow(
    label: String,
    schedule: BlockSchedule,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
            maxLines = 2,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CompactTimeDropdown(
                label = "Hour",
                value = schedule.hour,
                options = HOUR_OPTIONS,
                display = { it.toString() },
                onSelected = onHourChange,
                modifier = Modifier.weight(1f),
            )
            CompactTimeDropdown(
                label = "Min",
                value = schedule.minute,
                options = MINUTE_OPTIONS,
                display = { "%02d".format(it) },
                onSelected = onMinuteChange,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${schedule.durationMinutes} min",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun CompactTimeDropdown(
    label: String,
    value: Int,
    options: List<Int>,
    display: (Int) -> String,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedTextField(
            value = display(value),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            colors = stellaTextFieldColors(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(display(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
