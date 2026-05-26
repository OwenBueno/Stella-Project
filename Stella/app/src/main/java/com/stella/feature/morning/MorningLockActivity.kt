package com.stella.feature.morning

import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.stella.BuildConfig
import com.stella.core.data.SettingsRepository
import com.stella.core.ui.theme.MorningLockGradientTop
import com.stella.core.ui.theme.StellaTheme
import com.stella.core.util.NfcUtils
import com.stella.feature.dailyintent.DailyIntentActivity
import com.stella.sync.MorningAlarmNotifications
import com.stella.sync.MorningAlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MorningLockViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : androidx.lifecycle.ViewModel() {
    private val _message = MutableStateFlow(MorningLockDefaultMessage)
    val message: StateFlow<String> = _message.asStateFlow()

    fun onWrongTag() {
        _message.update { "Wrong tag. Use your enrolled bathroom tag." }
    }

    fun onNoTagEnrolled() {
        _message.update { "No tag enrolled. Complete morning lock setup first." }
    }

    fun hasEnrolledTag(): Boolean = settingsRepository.hasNfcTagEnrolled()

    fun verifyTag(scanned: String?): Boolean {
        val enrolled = settingsRepository.getNfcTagId()
        return NfcUtils.matchesEnrolled(scanned, enrolled)
    }
}

@AndroidEntryPoint
class MorningLockActivity : ComponentActivity() {
    private val viewModel: MorningLockViewModel by viewModels()

    @Inject lateinit var morningLockController: MorningLockController

    private var nfcAdapter: NfcAdapter? = null
    private var morningAlarmActivated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyLockScreenWindowFlags()
        onBackPressedDispatcher.addCallback(this) {
            // Consume back — remain locked
        }
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (!viewModel.hasEnrolledTag()) {
            viewModel.onNoTagEnrolled()
        }
        val isTestAlarm = intent.getBooleanExtra(EXTRA_IS_TEST_ALARM, false)
        setContent {
            val message by viewModel.message.collectAsState()
            StellaTheme {
                MorningLockScreen(
                    message = message,
                    showDebugSkip = BuildConfig.DEBUG,
                    onDebugSkip = { proceedToDailyIntent() },
                    showEndTest = isTestAlarm,
                    onEndTest = { endTestAndReturnToSetup() },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        morningLockController.setMorningLockVisible(true)
        morningLockController.setLockSurfaceReady(true)
        activateMorningAlarmIfNeeded()
        val pending = PendingIntent.getActivity(
            this,
            0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        nfcAdapter?.enableForegroundDispatch(this, pending, null, null)
    }

    override fun onPause() {
        morningLockController.setMorningLockVisible(false)
        nfcAdapter?.disableForegroundDispatch(this)
        super.onPause()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (morningLockController.isEnforcingNow()) {
            MorningLockEnforcementService.start(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_FROM_ALARM, false)) {
            MorningAlarmRinger.start(this)
        }
        val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        val tagId = NfcUtils.tagIdFromIntent(tag)
        if (viewModel.verifyTag(tagId)) {
            proceedToDailyIntent()
        } else {
            viewModel.onWrongTag()
        }
    }

    private fun proceedToDailyIntent() {
        startActivity(
            Intent(this, DailyIntentActivity::class.java).apply {
                putExtra(EXTRA_IS_TEST_ALARM, intent.getBooleanExtra(EXTRA_IS_TEST_ALARM, false))
            },
        )
        finish()
    }

    private fun endTestAndReturnToSetup() {
        morningLockController.stopEnforcement()
        MorningAlarmRinger.stop()
        val setupIntent = MorningLockSetupActivity.intentForStep(
            context = this,
            step = MorningSetupStep.TEST,
            afterSetup = intent.getStringExtra(MorningLockSetupActivity.EXTRA_AFTER_SETUP),
        )
        startActivity(setupIntent)
        finish()
    }

    private fun activateMorningAlarmIfNeeded() {
        if (morningAlarmActivated) return
        val fromAlarm = intent.getBooleanExtra(EXTRA_FROM_ALARM, false)
        val isTest = intent.getBooleanExtra(EXTRA_IS_TEST_ALARM, false)
        if (fromAlarm || isTest) {
            morningAlarmActivated = true
            val reason = if (isTest) EnforcementReason.TEST else EnforcementReason.ALARM
            if (!morningLockController.isEnforcingNow()) {
                morningLockController.startEnforcement(reason)
            }
            MorningAlarmRinger.start(this)
            MorningAlarmNotifications.showAlarmFullScreen(this)
            if (!isTest) {
                MorningAlarmScheduler.schedule(this)
            }
            return
        }
        lifecycleScope.launch {
            if (morningLockController.shouldEnforce() && !morningLockController.isEnforcingNow()) {
                morningLockController.startEnforcement(EnforcementReason.APP_OPEN)
            }
        }
    }

    private fun applyLockScreenWindowFlags() {
        window.statusBarColor = MorningLockGradientTop.toArgb()
        window.navigationBarColor = MorningLockGradientTop.toArgb()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            )
        }
    }

    companion object {
        const val EXTRA_FROM_ALARM = "from_alarm"
        const val EXTRA_IS_TEST_ALARM = "is_test_alarm"
    }
}
