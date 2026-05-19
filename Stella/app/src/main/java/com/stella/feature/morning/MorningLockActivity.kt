package com.stella.feature.morning

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stella.BuildConfig
import com.stella.core.data.SettingsRepository
import com.stella.core.ui.theme.StellaTheme
import com.stella.core.ui.theme.TextPrimary
import com.stella.core.util.NfcUtils
import com.stella.feature.dailyintent.DailyIntentActivity
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class MorningLockViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : androidx.lifecycle.ViewModel() {
    private val _message = MutableStateFlow("Scan your bathroom tag to start the day")
    val message: StateFlow<String> = _message.asStateFlow()

    fun onWrongTag() {
        _message.update { "Wrong tag. Use your enrolled bathroom tag." }
    }

    fun onNoTagEnrolled() {
        _message.update { "No tag enrolled. Register one in System settings first." }
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
    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (!viewModel.hasEnrolledTag()) {
            viewModel.onNoTagEnrolled()
        }
        setContent {
            val message by viewModel.message.collectAsState()
            StellaTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xCC000000))
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "MORNING LOCK",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                    )
                    Text(message, modifier = Modifier.padding(vertical = 24.dp), color = TextPrimary)
                    if (BuildConfig.DEBUG) {
                        Button(
                            onClick = { proceedToDailyIntent() },
                            modifier = Modifier.padding(top = 16.dp),
                        ) {
                            Text("Debug: skip NFC")
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val pending = PendingIntent.getActivity(
            this,
            0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        nfcAdapter?.enableForegroundDispatch(this, pending, null, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Block back — remain locked
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        val tagId = NfcUtils.tagIdFromIntent(tag)
        if (viewModel.verifyTag(tagId)) {
            proceedToDailyIntent()
        } else {
            viewModel.onWrongTag()
        }
    }

    private fun proceedToDailyIntent() {
        startActivity(Intent(this, DailyIntentActivity::class.java))
        finish()
    }
}
