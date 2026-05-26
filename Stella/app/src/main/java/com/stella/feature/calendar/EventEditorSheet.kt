package com.stella.feature.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stella.core.ui.components.stellaTextFieldColors
import com.stella.core.ui.theme.Error
import com.stella.core.ui.theme.TextPrimary
import com.stella.core.ui.theme.TextSecondary
import com.stella.feature.tasks.ScheduleTimePickerContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditorSheet(
    mode: EventEditorMode,
    draft: EventEditorDraft,
    onTitleChange: (String) -> Unit,
    onStartDateChange: (java.time.LocalDate) -> Unit,
    onEndDateChange: (java.time.LocalDate) -> Unit,
    onStartHourChange: (Int) -> Unit,
    onStartMinuteChange: (Int) -> Unit,
    onEndHourChange: (Int) -> Unit,
    onEndMinuteChange: (Int) -> Unit,
    onRecurrenceChange: (com.stella.core.calendar.RecurrenceRule) -> Unit,
    onRemindersChange: (List<Int>) -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()
    val isEdit = mode == EventEditorMode.EDIT

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = if (isEdit) "Edit event" else "New event",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary,
            )
            OutlinedTextField(
                value = draft.title,
                onValueChange = onTitleChange,
                label = { Text("Event title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = stellaTextFieldColors(),
            )
            Text("Starts", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            ScheduleTimePickerContent(
                pickerDate = draft.startDate,
                pickerHour = draft.startTime.hour,
                pickerMinute = draft.startTime.minute,
                onDateChange = onStartDateChange,
                onHourChange = onStartHourChange,
                onMinuteChange = onStartMinuteChange,
            )
            Text("Ends", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            ScheduleTimePickerContent(
                pickerDate = draft.endDate,
                pickerHour = draft.endTime.hour,
                pickerMinute = draft.endTime.minute,
                onDateChange = onEndDateChange,
                onHourChange = onEndHourChange,
                onMinuteChange = onEndMinuteChange,
            )
            RecurrencePicker(rule = draft.recurrence, onRuleChange = onRecurrenceChange)
            ReminderPicker(
                selectedOffsets = draft.reminderOffsetsMinutes,
                onSelectionChange = onRemindersChange,
            )
            draft.error?.let {
                Text(it, color = Error, style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = draft.title.isNotBlank(),
            ) {
                Text(if (isEdit) "Save" else "Create event")
            }
            if (isEdit && onDelete != null) {
                OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = Error)
                    Text("Delete event", color = Error, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}
