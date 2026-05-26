package com.stella.feature.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stella.core.ui.components.DawnScreenBackground
import com.stella.core.ui.theme.Error
import com.stella.core.ui.theme.Primary
import com.stella.core.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiagnosticsConsoleScreen(
    viewModel: DiagnosticsViewModel = hiltViewModel(),
    useBackground: Boolean = true,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (useBackground) 24.dp else 0.dp, vertical = if (useBackground) 16.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
        Text(
            "Quick triggers for development and QA.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DiagnosticTriggerButton(
                label = "Morning test now",
                enabled = !state.isBusy,
                onClick = { viewModel.onEvent(DiagnosticsUiEvent.TriggerMorningTestAlarmNow) },
            )
            DiagnosticTriggerButton(
                label = "Morning test alarm (10s)",
                enabled = !state.isBusy,
                onClick = { viewModel.onEvent(DiagnosticsUiEvent.ScheduleMorningTestAlarm) },
            )
            DiagnosticTriggerButton(
                label = "Morning permissions",
                enabled = !state.isBusy,
                onClick = { viewModel.onEvent(DiagnosticsUiEvent.RefreshMorningPermissions) },
            )
            DiagnosticTriggerButton(
                label = "Trigger Test Notification",
                enabled = !state.isBusy,
                onClick = { viewModel.showTestNotification() },
            )
            DiagnosticTriggerButton(
                label = "Simulate Daily Reset",
                enabled = !state.isBusy,
                onClick = { viewModel.onEvent(DiagnosticsUiEvent.SimulateDailyReset) },
            )
            DiagnosticTriggerButton(
                label = "Log Test Penalty",
                enabled = !state.isBusy,
                onClick = { viewModel.onEvent(DiagnosticsUiEvent.LogTestPenalty) },
            )
            DiagnosticTriggerButton(
                label = "Ping API Connection",
                enabled = !state.isBusy,
                onClick = { viewModel.onEvent(DiagnosticsUiEvent.PingApi) },
            )
        }

        if (state.isBusy || state.isSyncing) {
            CircularProgressIndicator(color = Primary)
        }

        state.message?.let { SettingsMessageBanner(it) }

        SettingsGroupCard(title = "Data & sync") {
            OutlinedButton(
                onClick = { viewModel.onEvent(DiagnosticsUiEvent.SyncNow) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSyncing,
            ) {
                Text("Sync now", color = Primary)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Error)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Danger zone", style = MaterialTheme.typography.titleSmall, color = Error)
            Text(
                "Purge all local data. Server backup remains.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            TextButton(
                onClick = { viewModel.onEvent(DiagnosticsUiEvent.PurgeLocal) },
                enabled = !state.isBusy,
            ) {
                Text("Purge local data", color = Error)
            }
        }
        }
    }

    if (useBackground) {
        DawnScreenBackground { content() }
    } else {
        content()
    }
}

@Composable
private fun DiagnosticTriggerButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.padding(0.dp),
    ) {
        Text(label, color = if (enabled) Primary else TextSecondary)
    }
}
