package com.stella.feature.settings

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

/** @deprecated Use settings navigation graph ([SettingsSectionListScreen]). */
@Composable
fun SettingsScreen(
    onNavigateAdvanced: () -> Unit,
    onNavigateDiagnostics: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    SettingsSectionListScreen(
        onSectionSelected = { section ->
            when (section) {
                SettingsSection.Developer -> onNavigateAdvanced()
                SettingsSection.Diagnostics -> onNavigateDiagnostics()
                else -> Unit
            }
        },
    )
}
