package com.stella.feature.dailyintent

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stella.app.MainActivity
import com.stella.core.ui.theme.Background
import com.stella.core.ui.theme.StellaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DailyIntentActivity : ComponentActivity() {
    private val viewModel: DailyIntentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.onUnlocked = {
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                },
            )
            finish()
        }
        setContent {
            val state by viewModel.state.collectAsState()
            StellaTheme {
                DailyIntentScreen(
                    state = state,
                    onEvent = viewModel::onEvent,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Background)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                )
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Remain in morning flow until unlocked
    }
}
