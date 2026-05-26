package com.stella.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stella.core.ui.components.DawnScreenBackground
import com.stella.core.ui.components.stellaTextFieldColors
import com.stella.core.ui.theme.Primary
import com.stella.core.ui.theme.TextPrimary
import com.stella.core.ui.theme.TextSecondary

@Composable
fun AdvancedSettingsScreen(
    onRegisterNfc: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    useBackground: Boolean = true,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tagDisplay = state.nfcTagId?.let { formatTagId(it) } ?: "Not enrolled"

    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (useBackground) 24.dp else 0.dp, vertical = if (useBackground) 16.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
        SettingsGroupCard(title = "Server configuration") {
            OutlinedTextField(
                value = state.apiUrl,
                onValueChange = { viewModel.onEvent(SettingsUiEvent.ApiUrlChanged(it)) },
                label = { Text("API Endpoint URL") },
                placeholder = { Text("http://10.0.2.2:3000") },
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
                visualTransformation = PasswordVisualTransformation(),
                colors = stellaTextFieldColors(),
            )
            OutlinedButton(
                onClick = { viewModel.onEvent(SettingsUiEvent.SaveCredentials) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save credentials", color = Primary)
            }
        }

        SettingsGroupCard(title = "NFC hardware") {
            Text(
                "Bathroom tag",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
            Text(
                "Registered Tag ID: $tagDisplay",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
            )
            Button(
                onClick = onRegisterNfc,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = TextPrimary,
                ),
            ) {
                Text("Register New NFC Tag")
            }
        }

        SettingsGroupCard(title = "Friction / penalties") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Log skip penalties to Finances", color = TextPrimary)
                Switch(
                    checked = state.penaltyEnabled,
                    onCheckedChange = { viewModel.onEvent(SettingsUiEvent.PenaltyEnabledChanged(it)) },
                    colors = SwitchDefaults.colors(checkedTrackColor = Primary),
                )
            }
            OutlinedTextField(
                value = state.penaltyAmountInput,
                onValueChange = { viewModel.onEvent(SettingsUiEvent.PenaltyAmountChanged(it)) },
                label = { Text("Penalty amount (USD)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = stellaTextFieldColors(),
            )
            OutlinedButton(
                onClick = { viewModel.onEvent(SettingsUiEvent.SavePenaltySettings) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save penalty settings", color = Primary)
            }
        }

        state.message?.let { SettingsMessageBanner(it) }
        }
    }

    if (useBackground) {
        DawnScreenBackground { content() }
    } else {
        content()
    }
}

private fun formatTagId(raw: String): String =
    if (raw.length <= 12) raw else raw.take(12) + "…"
