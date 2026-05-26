package com.stella.feature.morning

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.stella.core.ui.theme.MorningLockPulseCore
import com.stella.core.ui.theme.MorningLockPulseRing

private const val PulseDurationMs = 2400
private const val RingCount = 3

@Composable
fun NfcScanPulse(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "nfc_pulse")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(PulseDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse_progress",
    )

    Box(
        modifier = modifier
            .size(240.dp)
            .semantics { contentDescription = "NFC scan area" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f * 0.88f

            for (ring in 0 until RingCount) {
                val phase = (progress + ring.toFloat() / RingCount) % 1f
                val radius = maxRadius * (0.35f + phase * 0.65f)
                val alpha = (1f - phase).coerceIn(0f, 1f) * 0.55f + 0.08f
                drawCircle(
                    color = MorningLockPulseRing.copy(alpha = alpha),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            }
            drawCircle(
                color = MorningLockPulseCore,
                radius = maxRadius * 0.12f,
                center = center,
            )
        }
        Icon(
            imageVector = Icons.Outlined.Nfc,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MorningLockPulseCore.copy(alpha = 0.85f),
        )
    }
}
