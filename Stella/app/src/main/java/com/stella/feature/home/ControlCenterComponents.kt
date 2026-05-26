package com.stella.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stella.core.ui.theme.DawnCardBorder
import com.stella.core.ui.theme.DawnCardSurface
import com.stella.core.ui.theme.Primary
import com.stella.core.ui.theme.TextPrimary
import com.stella.core.ui.theme.TextSecondary

private val PanelShape = RoundedCornerShape(14.dp)

internal val MetricRingSize = 72.dp

fun Modifier.dawnPanel(): Modifier = this
    .clip(PanelShape)
    .background(DawnCardSurface)
    .border(1.dp, DawnCardBorder, PanelShape)

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .dawnPanel()
            .padding(horizontal = 12.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun CompletionRing(
    percent: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(MetricRingSize)) {
            Canvas(modifier = Modifier.size(MetricRingSize)) {
                val strokeWidth = 8.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                val arcSize = Size(diameter, diameter)
                drawArc(
                    color = DawnCardBorder,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
                drawArc(
                    color = Primary,
                    startAngle = -90f,
                    sweepAngle = 360f * (percent.coerceIn(0, 100) / 100f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
            )
        }
        Text(
            text = "Complete",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
