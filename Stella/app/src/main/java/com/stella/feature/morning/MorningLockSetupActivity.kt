package com.stella.feature.morning

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.stella.app.MainActivity
import com.stella.core.ui.theme.StellaTheme
import com.stella.feature.nfc.NfcEnrollmentActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MorningLockSetupActivity : ComponentActivity() {
    private val viewModel: MorningLockSetupViewModel by viewModels()

    private val nfcEnrollmentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.onEvent(MorningSetupUiEvent.RefreshPermissions)
    }

    private val ringtonePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.let { data ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, android.net.Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                }
            }
            viewModel.onEvent(MorningSetupUiEvent.AlarmSoundSelected(uri))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewModel.initializeStep(intent.getStringExtra(EXTRA_INITIAL_STEP))
        setContent {
            val state by viewModel.state.collectAsState()
            StellaTheme {
                MorningLockSetupScreen(
                    state = state,
                    canAdvance = viewModel.canAdvanceFromCurrentStep(),
                    onEvent = viewModel::onEvent,
                    onOpenNfcEnrollment = {
                        nfcEnrollmentLauncher.launch(Intent(this, NfcEnrollmentActivity::class.java))
                    },
                    onPickAlarmSound = { launchRingtonePicker() },
                    onFinish = { finishSetup() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.initializeStep(intent.getStringExtra(EXTRA_INITIAL_STEP))
    }

    override fun onResume() {
        super.onResume()
        viewModel.onEvent(MorningSetupUiEvent.RefreshPermissions)
    }

    private fun launchRingtonePicker() {
        val existingUri = viewModel.state.value.alarmSoundUri?.let { android.net.Uri.parse(it) }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select morning alarm")
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
        }
        ringtonePickerLauncher.launch(intent)
    }

    private fun finishSetup() {
        val next = intent.getStringExtra(EXTRA_AFTER_SETUP)
        if (next == ROUTE_MAIN) {
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
        finish()
    }

    companion object {
        const val EXTRA_AFTER_SETUP = "after_setup"
        const val EXTRA_INITIAL_STEP = "initial_step"
        const val ROUTE_MAIN = "main"

        fun intentForStep(
            context: android.content.Context,
            step: MorningSetupStep,
            afterSetup: String? = null,
        ): android.content.Intent =
            android.content.Intent(context, MorningLockSetupActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_INITIAL_STEP, step.name)
                afterSetup?.let { putExtra(EXTRA_AFTER_SETUP, it) }
            }
    }
}
