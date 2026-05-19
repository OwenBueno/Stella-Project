package com.stella.feature.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stella.BuildConfig
import com.stella.core.data.SettingsRepository
import com.stella.core.ui.theme.Background
import com.stella.core.ui.theme.Primary
import com.stella.core.ui.theme.StellaTheme
import com.stella.core.ui.theme.TextPrimary
import com.stella.core.util.NfcUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NfcEnrollmentActivity : ComponentActivity() {
    @Inject lateinit var settingsRepository: SettingsRepository

    private var nfcAdapter: NfcAdapter? = null
    private var statusMessage by mutableStateOf("Hold your bathroom tag to the phone")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        setContent {
            StellaTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Background)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("Register NFC tag", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Text(statusMessage, color = TextPrimary)
                    if (BuildConfig.DEBUG) {
                        Button(onClick = {
                            settingsRepository.setNfcTagId(SettingsRepository.DEBUG_NFC_TAG)
                            setResult(Activity.RESULT_OK)
                            finish()
                        }) {
                            Text("Use debug tag (emulator)")
                        }
                    }
                    Button(onClick = { finish() }) {
                        Text("Cancel")
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        val tagId = NfcUtils.tagIdFromIntent(tag)
        if (tagId != null) {
            settingsRepository.setNfcTagId(tagId)
            statusMessage = "Tag registered."
            setResult(Activity.RESULT_OK)
            finish()
        } else {
            statusMessage = "Could not read tag. Try again."
        }
    }
}
