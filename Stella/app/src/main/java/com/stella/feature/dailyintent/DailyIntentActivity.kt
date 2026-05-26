package com.stella.feature.dailyintent

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import com.stella.app.MainActivity
import com.stella.core.ui.theme.DawnGradientTop
import com.stella.core.ui.theme.StellaTheme
import com.stella.feature.morning.MorningAlarmRinger
import com.stella.feature.morning.MorningLockActivity
import com.stella.feature.morning.MorningLockController
import com.stella.feature.morning.MorningLockEnforcementService
import com.stella.feature.morning.MorningLockSetupActivity
import com.stella.feature.morning.MorningSetupStep
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DailyIntentActivity : ComponentActivity() {
    private val viewModel: DailyIntentViewModel by viewModels()

    @Inject lateinit var morningLockController: MorningLockController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyLockScreenWindowFlags()
        onBackPressedDispatcher.addCallback(this) {
            // Remain in morning flow until unlocked
        }
        val isTestAlarm = intent.getBooleanExtra(MorningLockActivity.EXTRA_IS_TEST_ALARM, false)
        viewModel.onUnlocked = {
            morningLockController.stopEnforcement()
            MorningAlarmRinger.stop()
            if (isTestAlarm) {
                startActivity(
                    MorningLockSetupActivity.intentForStep(
                        context = this,
                        step = MorningSetupStep.TEST,
                    ),
                )
            } else {
                startActivity(
                    Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    },
                )
            }
            finish()
        }
        setContent {
            val state by viewModel.state.collectAsState()
            StellaTheme {
                DailyIntentScreen(
                    state = state,
                    onEvent = viewModel::onEvent,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        morningLockController.setDailyIntentVisible(true)
    }

    override fun onPause() {
        morningLockController.setDailyIntentVisible(false)
        super.onPause()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (morningLockController.isEnforcingNow()) {
            MorningLockEnforcementService.start(this)
        }
    }

    private fun applyLockScreenWindowFlags() {
        window.statusBarColor = DawnGradientTop.toArgb()
        window.navigationBarColor = DawnGradientTop.toArgb()
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
}
