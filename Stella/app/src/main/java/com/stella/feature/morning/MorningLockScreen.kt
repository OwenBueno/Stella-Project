package com.stella.feature.morning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stella.core.ui.components.StellaLabel
import com.stella.core.ui.theme.Error
import com.stella.core.ui.theme.MorningLockGradientBottom
import com.stella.core.ui.theme.MorningLockGradientTop
import com.stella.core.ui.theme.StellaTheme
import com.stella.core.ui.theme.TextMuted
import com.stella.core.ui.theme.TextPrimary
import com.stella.core.ui.theme.TextSecondary

const val MorningLockDefaultMessage = "Scan your bathroom tag to start the day."

@Composable
fun MorningLockScreen(
    message: String,
    showDebugSkip: Boolean,
    onDebugSkip: () -> Unit,
    showEndTest: Boolean = false,
    onEndTest: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isError = message != MorningLockDefaultMessage
    val instructionColor = if (isError) Error else TextSecondary

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

            StellaLabel(text = "Morning lock")
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Rise & Shine",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = TextPrimary,
            )

            Spacer(modifier = Modifier.height(48.dp))

            NfcScanPulse()

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = instructionColor,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
            )

            Spacer(modifier = Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 24.dp),
            ) {
                if (showEndTest) {
                    TextButton(onClick = onEndTest) {
                        Text(
                            text = "End test",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                        )
                    }
                }
                if (showDebugSkip) {
                    TextButton(onClick = onDebugSkip) {
                        Text(
                            text = "Skip NFC (Debug)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted.copy(alpha = 0.4f),
                        )
                    }
                }
                if (!showEndTest && !showDebugSkip) {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF12141A)
@Composable
private fun MorningLockScreenPreview() {
    StellaTheme {
        MorningLockScreen(
            message = MorningLockDefaultMessage,
            showDebugSkip = true,
            onDebugSkip = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF12141A)
@Composable
private fun MorningLockScreenErrorPreview() {
    StellaTheme {
        MorningLockScreen(
            message = "Wrong tag. Use your enrolled bathroom tag.",
            showDebugSkip = false,
            onDebugSkip = {},
        )
    }
}
