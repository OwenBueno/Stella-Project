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
import com.stella.core.ui.theme.Background
import com.stella.core.ui.theme.StellaTheme
import com.stella.feature.morning.MorningLockActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var dailyIntentRepository: DailyIntentRepository

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
            if (!dailyIntentRepository.hasCompletedToday()) {
                startActivity(Intent(this@MainActivity, MorningLockActivity::class.java))
                finish()
                return@launch
            }
            val startReview = intent.getBooleanExtra(EXTRA_OPEN_REVIEW, false)
            setContent {
                StellaTheme {
                    StellaNavHost(
                        initialRoute = if (startReview) Routes.REVIEW else Routes.HOME,
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_REVIEW = "open_review"
    }
}
