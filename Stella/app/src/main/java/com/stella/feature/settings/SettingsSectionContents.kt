package com.stella.feature.settings

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stella.core.data.SettingsRepository
import com.stella.core.ui.components.stellaTextFieldColors
import com.stella.core.ui.theme.Primary
import com.stella.core.ui.theme.TextPrimary
import com.stella.core.ui.theme.TextSecondary
import com.stella.feature.morning.MorningLockSetupActivity
import kotlin.math.roundToInt

@Composable
fun GeneralSettingsContent(viewModel: SettingsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsSectionScrollColumn {
        SettingsGroupCard(title = "Timezone") {
            SettingsToggleRow(
                label = "Use device timezone",
                description = "Automatically match this device's zone.",
                checked = state.useDeviceTimezone,
                onCheckedChange = { viewModel.onEvent(SettingsUiEvent.UseDeviceTimezoneChanged(it)) },
            )
            if (!state.useDeviceTimezone) {
                TimezonePickerField(
                    selectedZone = state.timeZoneId,
                    searchQuery = state.timeZoneSearch,
                    options = viewModel.filteredTimeZones(),
                    onSearchChange = { viewModel.onEvent(SettingsUiEvent.TimeZoneSearchChanged(it)) },
                    onZoneSelected = { viewModel.onEvent(SettingsUiEvent.TimeZoneChanged(it)) },
                )
            } else {
                Text(
                    "Active: ${java.time.ZoneId.systemDefault().id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
        state.message?.let { SettingsMessageBanner(it) }
    }
}

@Composable
fun MorningLockSettingsContent(viewModel: SettingsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.let { data ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                }
            }
            viewModel.onEvent(SettingsUiEvent.MorningAlarmSoundSelected(uri))
        }
    }

    SettingsSectionScrollColumn {
        SettingsGroupCard(title = "Morning lock") {
            SettingsToggleRow(
                label = "Morning lock enabled",
                description = "Wake alarm and NFC hostage until daily intent is complete.",
                checked = state.morningLockEnabled,
                onCheckedChange = { viewModel.onEvent(SettingsUiEvent.MorningLockEnabledChanged(it)) },
            )
            SettingsTimeChip(
                label = "Wake time",
                hour = state.morningWakeHour,
                minute = state.morningWakeMinute,
                enabled = state.morningLockEnabled,
                onTimeSelected = { h, m ->
                    viewModel.onEvent(SettingsUiEvent.MorningWakeTimeChanged(h, m))
                },
            )
            Text(
                buildString {
                    append("Setup: ")
                    append(if (state.morningSetupCompleted) "Complete" else "Incomplete")
                    append(" · NFC: ")
                    append(if (state.morningNfcEnrolled) "OK" else "Missing")
                    append(" · Overlay: ")
                    append(if (state.morningHasOverlay) "OK" else "Missing")
                    append(" · Alarms: ")
                    append(if (state.morningHasExactAlarms) "OK" else "Missing")
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Text(
                "Alarm sound: ${state.morningAlarmSoundLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            OutlinedButton(
                onClick = {
                    val existing = state.morningAlarmSoundUri?.let { Uri.parse(it) }
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing)
                    }
                    ringtonePickerLauncher.launch(intent)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Choose alarm sound")
            }
            SettingsRampDurationRow(
                label = "Volume ramp",
                valueSeconds = state.morningAlarmVolumeRampSeconds,
                onValueChange = { viewModel.onEvent(SettingsUiEvent.MorningAlarmVolumeRampChanged(it)) },
            )
            Text(
                "Alarm starts silent and reaches full volume over this duration.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Button(
                onClick = { viewModel.onEvent(SettingsUiEvent.SaveMorningLockSettings) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = TextPrimary,
                ),
            ) {
                Text("Save morning lock")
            }
            OutlinedButton(
                onClick = {
                    context.startActivity(Intent(context, MorningLockSetupActivity::class.java))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Set up morning lock")
            }
        }
        state.message?.let { SettingsMessageBanner(it) }
    }
}

@Composable
fun ScheduleSettingsContent(viewModel: SettingsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsSectionScrollColumn {
        SettingsGroupCard(title = "Schedule & block defaults") {
            Text(
                "Daily intent blocks use UTC storage and display in your timezone.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            SettingsSliderRow(
                label = "Default block duration",
                valueMinutes = state.blockDurationMinutes,
                valueRange = 15f..480f,
                steps = ((480 - 15) / 15) - 1,
                onValueChange = { snapped ->
                    val rounded = ((snapped + 7) / 15) * 15
                    viewModel.onEvent(SettingsUiEvent.BlockDurationChanged(rounded.coerceIn(15, 480)))
                },
            )
            SettingsTimeChip(
                label = "Default task start time",
                hour = state.defaultTaskStartHour,
                minute = state.defaultTaskStartMinute,
                enabled = true,
                onTimeSelected = { h, m ->
                    viewModel.onEvent(SettingsUiEvent.DefaultTaskStartChanged(h, m))
                },
            )
            SettingsTimeChip(
                label = "Evening review reminder",
                hour = state.eveningHour,
                minute = state.eveningMinute,
                enabled = true,
                onTimeSelected = { h, m ->
                    viewModel.onEvent(SettingsUiEvent.EveningTimeChanged(h, m))
                },
            )
            Button(
                onClick = { viewModel.onEvent(SettingsUiEvent.SaveScheduleDefaults) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = TextPrimary,
                ),
            ) {
                Text("Save schedule defaults")
            }
        }
        state.message?.let { SettingsMessageBanner(it) }
    }
}

@Composable
fun DeveloperSettingsContent(
    onRegisterNfc: () -> Unit,
    viewModel: SettingsViewModel,
) {
    AdvancedSettingsScreen(
        onRegisterNfc = onRegisterNfc,
        viewModel = viewModel,
        useBackground = false,
    )
}

@Composable
fun DiagnosticsSettingsContent() {
    DiagnosticsConsoleScreen(useBackground = false)
}

@Composable
private fun SettingsSectionScrollColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = { content() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimezonePickerField(
    selectedZone: String,
    searchQuery: String,
    options: List<String>,
    onSearchChange: (String) -> Unit,
    onZoneSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = if (expanded) searchQuery else selectedZone,
            onValueChange = {
                onSearchChange(it)
                expanded = true
            },
            readOnly = !expanded,
            label = { Text("Timezone (IANA)") },
            placeholder = { Text("Search or select…") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            singleLine = true,
            colors = stellaTextFieldColors(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { zone ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(zone) },
                    onClick = {
                        onZoneSelected(zone)
                        onSearchChange("")
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun SettingsRampDurationRow(
    label: String,
    valueSeconds: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snapped = (valueSeconds / RAMP_STEP_SECONDS.toFloat()).roundToInt() * RAMP_STEP_SECONDS
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            Text(
                formatRampDuration(snapped),
                style = MaterialTheme.typography.labelLarge,
                color = Primary,
            )
        }
        androidx.compose.material3.Slider(
            value = snapped.toFloat(),
            onValueChange = { raw ->
                val rounded = (raw / RAMP_STEP_SECONDS).roundToInt() * RAMP_STEP_SECONDS
                onValueChange(rounded.coerceIn(0, SettingsRepository.MAX_MORNING_ALARM_VOLUME_RAMP_SECONDS))
            },
            valueRange = 0f..SettingsRepository.MAX_MORNING_ALARM_VOLUME_RAMP_SECONDS.toFloat(),
            steps = (SettingsRepository.MAX_MORNING_ALARM_VOLUME_RAMP_SECONDS / RAMP_STEP_SECONDS) - 1,
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = Primary,
                activeTrackColor = Primary,
            ),
        )
    }
}

fun formatRampDuration(seconds: Int): String = when {
    seconds <= 0 -> "Instant"
    seconds < 60 -> "${seconds}s"
    seconds % 60 == 0 -> "${seconds / 60} min"
    else -> "${seconds / 60}m ${seconds % 60}s"
}

private const val RAMP_STEP_SECONDS = 30
