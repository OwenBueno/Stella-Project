package com.stella.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.stella.app.navigation.Routes
import com.stella.app.navigation.StellaNavHost
import com.stella.core.data.DailyIntentRepository
import com.stella.core.data.SettingsRepository
import com.stella.core.ui.theme.Background
import com.stella.core.ui.theme.StellaTheme
import com.stella.feature.morning.EnforcementReason
import com.stella.feature.morning.MorningLockActivity
import com.stella.feature.morning.MorningLockController
import com.stella.feature.morning.MorningLockSetupActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var dailyIntentRepository: DailyIntentRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var morningLockController: MorningLockController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.statusBarColor = Background.toArgb()
        window.navigationBarColor = Background.toArgb()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        lifecycleScope.launch {
            if (settingsRepository.isMorningLockEnabled() && morningLockController.needsSetup()) {
                startActivity(
                    Intent(this@MainActivity, MorningLockSetupActivity::class.java).apply {
                        putExtra(MorningLockSetupActivity.EXTRA_AFTER_SETUP, MorningLockSetupActivity.ROUTE_MAIN)
                    },
                )
                finish()
                return@launch
            }
            if (morningLockController.shouldEnforce()) {
                morningLockController.startEnforcement(EnforcementReason.APP_OPEN)
                startActivity(Intent(this@MainActivity, MorningLockActivity::class.java))
                finish()
                return@launch
            }
            val startReview = intent.getBooleanExtra(EXTRA_OPEN_REVIEW, false)
            val openCalendarDate = intent.getStringExtra(EXTRA_OPEN_CALENDAR_DATE)
            val initialRoute = when {
                startReview -> Routes.REVIEW
                openCalendarDate != null -> Routes.calendar(openCalendarDate)
                else -> Routes.HOME
            }
            setContent {
                StellaTheme {
                    StellaNavHost(initialRoute = initialRoute)
                }
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_REVIEW = "open_review"
        const val EXTRA_OPEN_CALENDAR_DATE = "open_calendar_date"
    }
}
