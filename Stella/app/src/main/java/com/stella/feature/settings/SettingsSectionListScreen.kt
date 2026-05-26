package com.stella.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Translate
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.stella.core.ui.components.DawnScreenBackground

@Composable
fun SettingsSectionListScreen(
    onSectionSelected: (SettingsSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    DawnScreenBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingsSection.entries.forEach { section ->
                SettingsNavRow(
                    icon = section.icon(),
                    title = section.label,
                    subtitle = section.subtitle(),
                    onClick = { onSectionSelected(section) },
                )
            }
        }
    }
}

private fun SettingsSection.icon(): ImageVector = when (this) {
    SettingsSection.General -> Icons.Default.Translate
    SettingsSection.Morning -> Icons.Default.Alarm
    SettingsSection.Schedule -> Icons.Default.Schedule
    SettingsSection.Developer -> Icons.Default.Code
    SettingsSection.Diagnostics -> Icons.Default.Terminal
}

private fun SettingsSection.subtitle(): String = when (this) {
    SettingsSection.General -> "Timezone and locale"
    SettingsSection.Morning -> "Wake alarm, sound, and setup"
    SettingsSection.Schedule -> "Blocks, tasks, and evening review"
    SettingsSection.Developer -> "API, NFC, and penalties"
    SettingsSection.Diagnostics -> "Tests, sync, and dev tools"
}
