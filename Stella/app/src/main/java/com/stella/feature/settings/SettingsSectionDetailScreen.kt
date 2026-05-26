package com.stella.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.stella.core.ui.components.DawnScreenBackground
import com.stella.feature.nfc.NfcEnrollmentActivity

@Composable
fun SettingsSectionDetailScreen(
    section: SettingsSection,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val nfcLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        if (it.resultCode == android.app.Activity.RESULT_OK) {
            settingsViewModel.onEvent(SettingsUiEvent.RefreshNfcTag)
        }
    }

    DawnScreenBackground(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (section) {
                SettingsSection.General -> GeneralSettingsContent(settingsViewModel)
                SettingsSection.Morning -> MorningLockSettingsContent(settingsViewModel)
                SettingsSection.Schedule -> ScheduleSettingsContent(settingsViewModel)
                SettingsSection.Developer -> DeveloperSettingsContent(
                    viewModel = settingsViewModel,
                    onRegisterNfc = {
                        nfcLauncher.launch(
                            android.content.Intent(context, NfcEnrollmentActivity::class.java),
                        )
                    },
                )
                SettingsSection.Diagnostics -> DiagnosticsSettingsContent()
            }
        }
    }
}
