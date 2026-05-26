package com.stella.feature.nfc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stella.core.ui.components.StellaLabel
import com.stella.core.ui.theme.MorningLockGradientBottom
import com.stella.core.ui.theme.MorningLockGradientTop
import com.stella.core.ui.theme.Primary
import com.stella.core.ui.theme.TextPrimary
import com.stella.core.ui.theme.TextSecondary
import com.stella.feature.morning.NfcScanPulse

@Composable
fun NfcEnrollmentScreen(
    message: String,
    showDebugEnroll: Boolean,
    onDebugEnroll: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(MorningLockGradientTop, MorningLockGradientBottom),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            StellaLabel(text = "NFC enrollment")
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Register bathroom tag",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(48.dp))
            NfcScanPulse()
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.weight(1f))
            if (showDebugEnroll) {
                OutlinedButton(
                    onClick = onDebugEnroll,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Use debug tag (emulator)", color = Primary)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            Button(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary.copy(alpha = 0.2f),
                    contentColor = TextPrimary,
                ),
            ) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
