package com.stella.feature.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stella.core.data.SettingsRepository
import com.stella.core.ui.components.SettingsRow
import com.stella.core.ui.components.StellaSectionHeader
import com.stella.core.ui.components.stellaTextFieldColors
import com.stella.core.ui.theme.Error
import com.stella.core.ui.theme.Primary
import com.stella.core.ui.theme.TextPrimary
import com.stella.core.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    onRegisterNfc: () -> Unit,
    nfcTagSummary: String,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StellaSectionHeader(eyebrow = "System", title = "Stella OS")

        OutlinedTextField(
            value = state.apiUrl,
            onValueChange = { viewModel.onEvent(SettingsUiEvent.ApiUrlChanged(it)) },
            label = { Text("API URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = stellaTextFieldColors(),
        )
        OutlinedTextField(
            value = state.apiKey,
            onValueChange = { viewModel.onEvent(SettingsUiEvent.ApiKeyChanged(it)) },
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = stellaTextFieldColors(),
        )
        TextButton(onClick = { viewModel.onEvent(SettingsUiEvent.SaveCredentials) }) {
            Text("Save credentials", color = Primary)
        }

        SettingsRow(icon = Icons.Default.Link, label = "Endpoint", value = state.apiUrl)
        SettingsRow(icon = Icons.Default.Key, label = "API key", value = if (state.apiKey.isBlank()) "Not set" else "••••••")

        SettingsRow(icon = Icons.Default.Nfc, label = "Bathroom tag", value = nfcTagSummary)
        Button(onClick = onRegisterNfc, modifier = Modifier.fillMaxWidth()) {
            Text("Register bathroom NFC tag")
        }

        StellaSectionHeader(eyebrow = "Time", title = "Calendar & blocks")
        Text(
            "Daily intent blocks are saved as UTC and displayed in your timezone.",
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )

        OutlinedTextField(
            value = state.timeZoneId,
            onValueChange = { viewModel.onEvent(SettingsUiEvent.TimeZoneChanged(it)) },
            label = { Text("Timezone (IANA)") },
            placeholder = { Text("e.g. America/New_York") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = stellaTextFieldColors(),
        )
        Text(
            "Examples: ${SettingsRepository.SUGGESTED_TIME_ZONES.take(4).joinToString(", ")}",
            color = TextSecondary,
            style = MaterialTheme.typography.labelSmall,
        )
        TextButton(onClick = { viewModel.onEvent(SettingsUiEvent.UseDeviceTimeZone) }) {
            Text("Use device timezone", color = Primary)
        }

        OutlinedTextField(
            value = state.blockDurationMinutes,
            onValueChange = { viewModel.onEvent(SettingsUiEvent.BlockDurationChanged(it)) },
            label = { Text("Default block duration (minutes)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = stellaTextFieldColors(),
        )

        BlockDefaultFields(
            label = "Frog 1 default",
            hour = state.block1Hour,
            minute = state.block1Minute,
            onHourChange = { viewModel.onEvent(SettingsUiEvent.Block1HourChanged(it)) },
            onMinuteChange = { viewModel.onEvent(SettingsUiEvent.Block1MinuteChanged(it)) },
        )
        BlockDefaultFields(
            label = "Frog 2 default",
            hour = state.block2Hour,
            minute = state.block2Minute,
            onHourChange = { viewModel.onEvent(SettingsUiEvent.Block2HourChanged(it)) },
            onMinuteChange = { viewModel.onEvent(SettingsUiEvent.Block2MinuteChanged(it)) },
        )
        BlockDefaultFields(
            label = "Frog 3 default",
            hour = state.block3Hour,
            minute = state.block3Minute,
            onHourChange = { viewModel.onEvent(SettingsUiEvent.Block3HourChanged(it)) },
            onMinuteChange = { viewModel.onEvent(SettingsUiEvent.Block3MinuteChanged(it)) },
        )

        StellaSectionHeader(eyebrow = "Evening", title = "Review reminder")
        OutlinedTextField(
            value = state.eveningHour,
            onValueChange = { viewModel.onEvent(SettingsUiEvent.EveningHourChanged(it)) },
            label = { Text("Evening review hour (0–23)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = stellaTextFieldColors(),
        )
        OutlinedTextField(
            value = state.eveningMinute,
            onValueChange = { viewModel.onEvent(SettingsUiEvent.EveningMinuteChanged(it)) },
            label = { Text("Evening review minute") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = stellaTextFieldColors(),
        )
        TextButton(onClick = { viewModel.onEvent(SettingsUiEvent.SaveTimeAndBlocks) }) {
            Text("Save time & block settings", color = Primary)
        }

        Button(
            onClick = { viewModel.onEvent(SettingsUiEvent.SyncNow) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSyncing,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = Primary,
                contentColor = TextPrimary,
            ),
        ) {
            if (state.isSyncing) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
            }
            Text("Sync now")
        }

        state.syncMessage?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Error)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsRow(icon = Icons.Default.Delete, label = "Danger zone")
            Text(
                "Purge all local data. Server backup remains.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            TextButton(onClick = { viewModel.onEvent(SettingsUiEvent.PurgeLocal) }) {
                Text("Purge local data", color = Error)
            }
        }
    }
}

@Composable
private fun BlockDefaultFields(
    label: String,
    hour: String,
    minute: String,
    onHourChange: (String) -> Unit,
    onMinuteChange: (String) -> Unit,
) {
    SettingsRow(
        icon = Icons.Default.Schedule,
        label = label,
        value = "$hour:${minute.padStart(2, '0')}",
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = hour,
            onValueChange = onHourChange,
            label = { Text("Hour") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            colors = stellaTextFieldColors(),
        )
        OutlinedTextField(
            value = minute,
            onValueChange = onMinuteChange,
            label = { Text("Min") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            colors = stellaTextFieldColors(),
        )
    }
}
