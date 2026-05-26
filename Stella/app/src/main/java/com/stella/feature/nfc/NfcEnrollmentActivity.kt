package com.stella.feature.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import com.stella.BuildConfig
import com.stella.core.ui.theme.MorningLockGradientTop
import com.stella.core.ui.theme.StellaTheme
import com.stella.core.util.NfcUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NfcEnrollmentActivity : ComponentActivity() {
    private val viewModel: NfcEnrollmentViewModel by viewModels()
    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.statusBarColor = MorningLockGradientTop.toArgb()
        window.navigationBarColor = MorningLockGradientTop.toArgb()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        setContent {
            val message by viewModel.message.collectAsState()
            StellaTheme {
                NfcEnrollmentScreen(
                    message = message,
                    showDebugEnroll = BuildConfig.DEBUG,
                    onDebugEnroll = {
                        viewModel.registerDebugTag()
                        setResult(Activity.RESULT_OK)
                        finish()
                    },
                    onCancel = { finish() },
                )
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
            viewModel.onTagRead(tagId)
            setResult(Activity.RESULT_OK)
            finish()
        } else {
            viewModel.onReadFailed()
        }
    }
}
