package com.stella.feature.morning

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stella.core.ui.components.StellaLabel
import com.stella.core.ui.theme.MorningLockGradientBottom
import com.stella.core.ui.theme.MorningLockGradientTop
import com.stella.core.ui.theme.Primary
import com.stella.core.ui.theme.Success
import com.stella.core.ui.theme.TextMuted
import com.stella.core.ui.theme.TextPrimary
import com.stella.core.ui.theme.TextSecondary

@Composable
fun MorningLockSetupScreen(
    state: MorningSetupUiState,
    canAdvance: Boolean,
    onEvent: (MorningSetupUiEvent) -> Unit,
    onOpenNfcEnrollment: () -> Unit,
    onPickAlarmSound: () -> Unit,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { onEvent(MorningSetupUiEvent.RefreshPermissions) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(MorningLockGradientTop, MorningLockGradientBottom),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StellaLabel(text = "MORNING LOCK SETUP")
            Text(
                text = stepTitle(state.step),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
            )
            Text(
                text = stepBody(state.step),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            when (state.step) {
                MorningSetupStep.NFC -> {
                    PermissionStatusRow("NFC tag enrolled", state.nfcEnrolled)
                    OutlinedButton(onClick = onOpenNfcEnrollment, modifier = Modifier.fillMaxWidth()) {
                        Text("Register NFC tag")
                    }
                }
                MorningSetupStep.NOTIFICATIONS -> {
                    PermissionStatusRow("Notifications", state.hasNotifications)
                    PermissionStatusRow("Full-screen alarm", state.hasFullScreenIntent)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !state.hasNotifications) {
                        Button(
                            onClick = {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Allow notifications")
                        }
                    }
                    OutlinedButton(
                        onClick = { context.startActivity(MorningLockPermissions.notificationSettingsIntent(context)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Open notification settings")
                    }
                }
                MorningSetupStep.OVERLAY -> {
                    PermissionStatusRow("Draw over other apps", state.hasOverlay)
                    OutlinedButton(
                        onClick = { context.startActivity(MorningLockPermissions.overlaySettingsIntent(context)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Open overlay settings")
                    }
                }
                MorningSetupStep.EXACT_ALARMS -> {
                    PermissionStatusRow("Exact alarms", state.hasExactAlarms)
                    OutlinedButton(
                        onClick = { context.startActivity(MorningLockPermissions.exactAlarmSettingsIntent()) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Open alarm settings")
                    }
                }
                MorningSetupStep.BATTERY -> {
                    PermissionStatusRow("Battery unrestricted", state.batteryOptimized)
                    OutlinedButton(
                        onClick = { context.startActivity(MorningLockPermissions.batteryOptimizationIntent(context)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Request battery exemption")
                    }
                    Text(
                        "Optional but recommended so the wake alarm fires reliably.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                }
                MorningSetupStep.ALARM_SOUND -> {
                    Text(
                        "Selected: ${state.alarmSoundLabel}",
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedButton(
                        onClick = onPickAlarmSound,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Choose alarm sound")
                    }
                    OutlinedButton(
                        onClick = { onEvent(MorningSetupUiEvent.AlarmSoundSelected(null)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Use system default")
                    }
                }
                MorningSetupStep.TEST -> {
                    if (!state.criticalPermissionsReady) {
                        Text(
                            text = MorningLockPermissions.testBlockedMessage(context)
                                ?: "Complete notification, overlay, and alarm permissions above before testing.",
                            color = com.stella.core.ui.theme.Error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    state.testMessage?.let { msg ->
                        val isSuccess = state.testScheduled || msg.contains("launched", ignoreCase = true)
                        Text(
                            text = msg,
                            color = if (isSuccess) Success else com.stella.core.ui.theme.Error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Button(
                        onClick = { onEvent(MorningSetupUiEvent.TestAlarmNow) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isTesting && state.criticalPermissionsReady,
                    ) {
                        Text(if (state.isTesting) "Starting…" else "Test now")
                    }
                    OutlinedButton(
                        onClick = { onEvent(MorningSetupUiEvent.ScheduleTestAlarm) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isTesting && state.criticalPermissionsReady,
                    ) {
                        Text("Test in 10 seconds")
                    }
                }
                MorningSetupStep.DONE -> {
                    Text(
                        "You're set. The morning lock will activate at your wake time until you scan NFC and plan your day.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                else -> Unit
            }
            TextButton(onClick = { onEvent(MorningSetupUiEvent.RefreshPermissions) }) {
                Text("Refresh status", color = Primary)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.step != MorningSetupStep.WHY) {
                    OutlinedButton(
                        onClick = { onEvent(MorningSetupUiEvent.Back) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Back")
                    }
                }
                if (state.step == MorningSetupStep.DONE) {
                    Button(
                        onClick = {
                            onEvent(MorningSetupUiEvent.CompleteSetup)
                            onFinish()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Finish")
                    }
                } else {
                    Button(
                        onClick = { onEvent(MorningSetupUiEvent.Next) },
                        modifier = Modifier.weight(1f),
                        enabled = canAdvance,
                    ) {
                        Text("Continue")
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(label: String, granted: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = TextPrimary)
        Text(
            if (granted) "Ready" else "Needed",
            color = if (granted) Success else TextMuted,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private fun stepTitle(step: MorningSetupStep): String = when (step) {
    MorningSetupStep.WHY -> "Wake up with intention"
    MorningSetupStep.NFC -> "Bathroom NFC tag"
    MorningSetupStep.NOTIFICATIONS -> "Alarm notifications"
    MorningSetupStep.OVERLAY -> "Screen overlay"
    MorningSetupStep.EXACT_ALARMS -> "Reliable wake time"
    MorningSetupStep.BATTERY -> "Battery exemption"
    MorningSetupStep.ALARM_SOUND -> "Alarm sound"
    MorningSetupStep.TEST -> "Test your setup"
    MorningSetupStep.DONE -> "All set"
}

private fun stepBody(step: MorningSetupStep): String = when (step) {
    MorningSetupStep.WHY ->
        "Stella wakes you at your chosen time and keeps the phone locked until you scan your bathroom NFC tag and plan at least three tasks for the day."
    MorningSetupStep.NFC ->
        "Register the NFC tag you keep in your bathroom. This is the only way to start your morning routine in production."
    MorningSetupStep.NOTIFICATIONS ->
        "Notifications let Stella show the alarm on your lock screen using a full-screen alert."
    MorningSetupStep.OVERLAY ->
        "When your phone is already unlocked, Stella draws over other apps so you cannot browse until the routine is done."
    MorningSetupStep.EXACT_ALARMS ->
        "Exact alarms ensure your wake time fires on schedule, even in Doze mode."
    MorningSetupStep.BATTERY ->
        "Some phones delay alarms for battery saving. Exempting Stella improves reliability."
    MorningSetupStep.ALARM_SOUND ->
        "Pick a song or tone that wakes you up. It plays when the morning lock alarm fires."
    MorningSetupStep.TEST ->
        "Use Test now for an immediate preview, or Test in 10 seconds to mimic a real wake alarm."
    MorningSetupStep.DONE -> ""
}
