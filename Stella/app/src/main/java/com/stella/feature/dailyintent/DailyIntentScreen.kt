package com.stella.feature.dailyintent

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stella.core.data.TaskStatus
import com.stella.core.database.entity.TaskEntity
import com.stella.core.ui.components.StellaCard
import com.stella.core.ui.components.StellaLabel
import com.stella.core.ui.components.StellaSectionHeader
import com.stella.core.ui.components.stellaTextFieldColors
import com.stella.core.ui.theme.Border
import com.stella.core.ui.theme.Error
import com.stella.core.ui.theme.Primary
import com.stella.core.ui.theme.Success
import com.stella.core.ui.theme.TextPrimary
import com.stella.core.ui.theme.TextSecondary

private val HOUR_OPTIONS = (6..22).toList()
private val MINUTE_OPTIONS = listOf(0, 15, 30, 45)

@Composable
fun DailyIntentScreen(
    state: DailyIntentUiState,
    onEvent: (DailyIntentUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StellaSectionHeader(eyebrow = "Morning", title = "Daily Intent")
        Text(
            "Pick your Top 3 frogs and block time on your calendar. Times use your timezone from Settings.",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "Timezone: ${state.timeZoneLabel} · Block length: ${state.defaultDurationMinutes} min",
            color = TextSecondary,
            style = MaterialTheme.typography.labelSmall,
        )

        Text(
            "Top 3: ${state.selectionCount}/3",
            color = if (state.selectionCount == 3) Success else Primary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )

        state.slots.forEach { slot ->
            Top3SlotCard(
                slot = slot,
                task = slot.taskId?.let { state.taskForId(it) },
                schedule = state.scheduleFor(slot.taskId),
                onClear = { onEvent(DailyIntentUiEvent.ClearSlot(slot.index)) },
                onHourChange = { hour ->
                    slot.taskId?.let { onEvent(DailyIntentUiEvent.BlockHourChanged(it, hour)) }
                },
                onMinuteChange = { minute ->
                    slot.taskId?.let { onEvent(DailyIntentUiEvent.BlockMinuteChanged(it, minute)) }
                },
            )
        }

        StellaLabel(text = "Add a frog")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = state.newTaskTitle,
                onValueChange = { onEvent(DailyIntentUiEvent.NewTaskTitleChanged(it)) },
                label = { Text("New task for today") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = stellaTextFieldColors(),
            )
            TextButton(onClick = { onEvent(DailyIntentUiEvent.AddFrog) }) {
                Text("Add", color = Primary)
            }
        }

        StellaLabel(text = "Your tasks — tap to fill next slot")
        if (state.isLoading) {
            CircularProgressIndicator()
        } else if (state.tasks.isEmpty()) {
            StellaCard {
                Text(
                    "No tasks yet. Add at least 3 frogs above, then assign them to the slots.",
                    color = TextSecondary,
                )
            }
        } else {
            state.tasks.forEach { task ->
                val schedule = state.scheduleFor(task.id)
                TaskPickRow(
                    task = task,
                    selected = state.selectedTaskIds.contains(task.id),
                    slotNumber = state.selectedTaskIds.indexOf(task.id).takeIf { it >= 0 }?.plus(1),
                    timeHint = schedule?.displayTime(),
                    onClick = { onEvent(DailyIntentUiEvent.SelectTask(task.id)) },
                )
            }
        }

        state.error?.let {
            Text(it, color = Error, style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = { onEvent(DailyIntentUiEvent.UnlockDay) },
            enabled = state.canUnlock,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                when {
                    state.isSaving -> "Saving…"
                    state.selectionCount < 3 -> "Unlock my day (${state.selectionCount}/3)"
                    else -> "Unlock my day"
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Top3SlotCard(
    slot: Top3Slot,
    task: TaskEntity?,
    schedule: BlockSchedule?,
    onClear: () -> Unit,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
) {
    StellaCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StellaLabel(text = "Frog ${slot.index + 1}", accent = task != null)
                if (task != null) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Close, contentDescription = "Clear slot", tint = TextSecondary)
                    }
                }
            }
            if (task == null) {
                Text(
                    "Empty — tap a task below",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(task.title, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                schedule?.let {
                    Text(
                        "Calendar block at ${it.displayTime()} (${it.durationMinutes} min)",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TimeDropdown(
                        label = "Hour",
                        value = schedule?.hour ?: 9,
                        options = HOUR_OPTIONS,
                        onSelected = onHourChange,
                        modifier = Modifier.weight(1f),
                    )
                    TimeDropdown(
                        label = "Minute",
                        value = schedule?.minute ?: 0,
                        options = MINUTE_OPTIONS,
                        onSelected = onMinuteChange,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDropdown(
    label: String,
    value: Int,
    options: List<Int>,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val display = if (label == "Hour") value.toString() else "%02d".format(value)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = stellaTextFieldColors(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(if (label == "Hour") option.toString() else "%02d".format(option))
                    },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun TaskPickRow(
    task: TaskEntity,
    selected: Boolean,
    slotNumber: Int?,
    timeHint: String?,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) Success else Border
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(task.title, color = TextPrimary)
            val subtitle = buildList {
                if (task.status == TaskStatus.DONE.name) add("Completed")
                timeHint?.let { add("Block $it") }
            }.joinToString(" · ")
            if (subtitle.isNotEmpty()) {
                Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
            }
        }
        when {
            slotNumber != null -> Text("#$slotNumber", color = Success, fontWeight = FontWeight.Bold)
            else -> Text("Tap to assign", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        }
    }
}
