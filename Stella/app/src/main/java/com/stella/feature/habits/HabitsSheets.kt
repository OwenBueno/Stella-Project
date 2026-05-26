package com.stella.feature.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stella.core.ui.components.stellaTextFieldColors
import com.stella.core.ui.theme.Error
import com.stella.core.ui.theme.Primary
import com.stella.core.ui.theme.TextPrimary
import com.stella.core.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateHabitSheet(
    draftName: String,
    onDraftChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    HabitNameSheet(
        title = "New habit",
        draftName = draftName,
        onDraftChange = onDraftChange,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        confirmLabel = "Create",
        showDelete = false,
        onDelete = {},
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHabitSheet(
    draftName: String,
    onDraftChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    HabitNameSheet(
        title = "Update habit",
        draftName = draftName,
        onDraftChange = onDraftChange,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        confirmLabel = "Save",
        showDelete = true,
        onDelete = onDelete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitNameSheet(
    title: String,
    draftName: String,
    onDraftChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String,
    showDelete: Boolean,
    onDelete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary,
            )
            OutlinedTextField(
                value = draftName,
                onValueChange = onDraftChange,
                label = { Text("Habit name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = stellaTextFieldColors(),
            )
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                enabled = draftName.isNotBlank(),
            ) {
                Text(confirmLabel)
            }
            if (showDelete) {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = Error,
                            modifier = Modifier.size(20.dp),
                        )
                        Text("Delete habit", color = Error)
                    }
                }
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("Cancel", color = TextSecondary)
            }
        }
    }
}
