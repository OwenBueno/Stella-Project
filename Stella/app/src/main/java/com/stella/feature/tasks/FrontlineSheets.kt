package com.stella.feature.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stella.core.ui.components.stellaTextFieldColors
import com.stella.core.ui.theme.DawnCardBorder
import com.stella.core.ui.theme.DawnCardSurface
import com.stella.core.ui.theme.Error
import com.stella.core.ui.theme.TextPrimary
import com.stella.core.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private val MINUTE_STEPS = (0..55 step 5).toList()
private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulePickerSheet(
    mode: SchedulePickerMode,
    pickerDate: LocalDate,
    pickerHour: Int,
    pickerMinute: Int,
    editDraftTitle: String = "",
    onEditTitleChange: (String) -> Unit = {},
    onDateChange: (LocalDate) -> Unit,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isEdit = mode == SchedulePickerMode.EDIT

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = if (isEdit) "Edit directive" else "Schedule directive",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary,
            )
            if (isEdit) {
                Text(
                    text = "Drag the handle on the list to change sequence.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                OutlinedTextField(
                    value = editDraftTitle,
                    onValueChange = onEditTitleChange,
                    label = { Text("Directive name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = stellaTextFieldColors(),
                )
            }
            ScheduleTimePickerContent(
                pickerDate = pickerDate,
                pickerHour = pickerHour,
                pickerMinute = pickerMinute,
                onDateChange = onDateChange,
                onHourChange = onHourChange,
                onMinuteChange = onMinuteChange,
            )
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isEdit || editDraftTitle.isNotBlank(),
            ) {
                Text(if (isEdit) "Save" else "Add directive")
            }
            if (isEdit && onDelete != null) {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = Error)
                    Text("Delete directive", color = Error, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
fun ScheduleTimePickerContent(
    pickerDate: LocalDate,
    pickerHour: Int,
    pickerMinute: Int,
    onDateChange: (LocalDate) -> Unit,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onDateChange(pickerDate.minusDays(1)) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous day", tint = TextPrimary)
            }
            Text(
                text = pickerDate.format(dateFormatter),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = TextSecondary,
            )
            IconButton(onClick = { onDateChange(pickerDate.plusDays(1)) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next day", tint = TextPrimary)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TimeStepperField(
                label = "Hour",
                valueText = formatHourDisplay(pickerHour),
                onPrevious = { onHourChange((pickerHour + 23) % 24) },
                onNext = { onHourChange((pickerHour + 1) % 24) },
                modifier = Modifier.weight(1f),
            )
            TimeStepperField(
                label = "Minute",
                valueText = "%02d".format(pickerMinute),
                onPrevious = { onMinuteChange(previousMinuteStep(pickerMinute)) },
                onNext = { onMinuteChange(nextMinuteStep(pickerMinute)) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TimeStepperField(
    label: String,
    valueText: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, DawnCardBorder, RoundedCornerShape(12.dp))
                .background(DawnCardSurface)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Decrease $label", tint = TextPrimary)
            }
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onNext) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Increase $label", tint = TextPrimary)
            }
        }
    }
}

private fun formatHourDisplay(hour: Int): String {
    val normalized = hour.coerceIn(0, 23)
    val period = if (normalized < 12) "AM" else "PM"
    val twelveHour = when (val h = normalized % 12) {
        0 -> 12
        else -> h
    }
    return "$twelveHour $period"
}

private fun snapMinuteToStep(minute: Int): Int =
    MINUTE_STEPS.minByOrNull { abs(it - minute.coerceIn(0, 59)) } ?: 0

private fun nextMinuteStep(current: Int): Int {
    val snapped = snapMinuteToStep(current)
    val index = MINUTE_STEPS.indexOf(snapped)
    return MINUTE_STEPS[(index + 1) % MINUTE_STEPS.size]
}

private fun previousMinuteStep(current: Int): Int {
    val snapped = snapMinuteToStep(current)
    val index = MINUTE_STEPS.indexOf(snapped)
    return MINUTE_STEPS[(index - 1 + MINUTE_STEPS.size) % MINUTE_STEPS.size]
}

fun snapScheduleMinute(minute: Int): Int = snapMinuteToStep(minute)
