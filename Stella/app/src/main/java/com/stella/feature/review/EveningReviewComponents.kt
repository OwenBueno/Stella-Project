package com.stella.feature.review

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stella.core.ui.components.StellaCard
import com.stella.core.ui.components.stellaTextFieldColors
import com.stella.core.ui.theme.DawnCardSurface
import com.stella.core.ui.theme.Primary
import com.stella.core.ui.theme.TextPrimary
import com.stella.core.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.min

data class RingProgress(
    val label: String,
    val valueLabel: String,
    val progress: Float,
    val accent: Color,
)

@Composable
fun DailyCompletionRingsRow(
    habitsProgress: RingProgress,
    tasksProgress: RingProgress,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CompletionRingCard(
            ring = habitsProgress,
            modifier = Modifier.weight(1f),
        )
        CompletionRingCard(
            ring = tasksProgress,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun CompletionRingCard(
    ring: RingProgress,
    modifier: Modifier = Modifier,
    ringSize: Dp = 72.dp,
) {
    StellaCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CompletionRing(
                progress = ring.progress,
                accent = ring.accent,
                modifier = Modifier.size(ringSize),
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    ring.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    ring.valueLabel,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "Today",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun CompletionRing(
    progress: Float,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val clamped = progress.coerceIn(0f, 1f)
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
        val pad = stroke.width / 2f + 2.dp.toPx()
        val size = Size(this.size.width - pad * 2, this.size.height - pad * 2)
        val topLeft = Offset(pad, pad)

        drawArc(
            color = accent.copy(alpha = 0.18f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = size,
            style = stroke,
        )
        drawArc(
            brush = Brush.linearGradient(
                colors = listOf(accent.copy(alpha = 0.75f), accent),
            ),
            startAngle = -90f,
            sweepAngle = 360f * clamped,
            useCenter = false,
            topLeft = topLeft,
            size = size,
            style = stroke,
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.03f),
            radius = min(size.width, size.height) / 2.6f,
            center = center,
        )
    }
}

@Composable
fun GlassTextCard(
    title: String,
    placeholder: String,
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    StellaCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
            )
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                minLines = 4,
                placeholder = { Text(placeholder, color = TextSecondary.copy(alpha = 0.7f)) },
                colors = stellaTextFieldColors(),
            )
        }
    }
}

@Composable
fun DisciplineStreakBadge(
    streakDays: Int,
    modifier: Modifier = Modifier,
) {
    StellaCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Discipline streak",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary,
                )
                Text(
                    if (streakDays <= 0) "Start a streak tonight" else "$streakDays-day streak",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary,
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Primary.copy(alpha = 0.18f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Keep going",
                    style = MaterialTheme.typography.labelMedium,
                    color = Primary,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonthlyDisciplineHeatmap(
    monthLabel: String,
    dayScores: Map<LocalDate, Float>,
    modifier: Modifier = Modifier,
) {
    StellaCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Monthly discipline",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
            )
            Text(
                monthLabel,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary,
            )

            val month = dayScores.keys.firstOrNull()?.let { YearMonth.from(it) } ?: YearMonth.now()
            val days = month.lengthOfMonth()
            val first = month.atDay(1)
            val startOffset = (first.dayOfWeek.value - 1).coerceIn(0, 6) // Monday=0

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                maxItemsInEachRow = 7,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(startOffset) {
                    Spacer(modifier = Modifier.size(12.dp))
                }
                for (d in 1..days) {
                    val date = month.atDay(d)
                    val score = dayScores[date] ?: 0f
                    HeatCell(score = score)
                }
            }

            HeatLegendRow(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun HeatCell(score: Float, modifier: Modifier = Modifier) {
    val bucket = scoreToBucket(score)
    val color = when (bucket) {
        0 -> DawnCardSurface.copy(alpha = 0.55f)
        1 -> Primary.copy(alpha = 0.18f)
        2 -> Primary.copy(alpha = 0.32f)
        3 -> Primary.copy(alpha = 0.48f)
        else -> Primary.copy(alpha = 0.68f)
    }
    Box(
        modifier = modifier
            .size(12.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color),
    )
}

@Composable
private fun HeatLegendRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Low", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            (0..4).forEach { b -> HeatCell(score = b / 4f) }
        }
        Text("High", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

private fun scoreToBucket(score: Float): Int =
    when {
        score <= 0f -> 0
        score < 0.25f -> 1
        score < 0.5f -> 2
        score < 0.75f -> 3
        else -> 4
    }

@Composable
fun GlowingPrimaryButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glow = if (enabled) Primary.copy(alpha = 0.35f) else Primary.copy(alpha = 0.12f)
    Box(
        modifier = modifier
            .shadow(
                elevation = 22.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = glow,
                spotColor = glow,
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Primary.copy(alpha = if (enabled) 0.95f else 0.5f),
                        Primary.copy(alpha = if (enabled) 0.75f else 0.35f),
                    ),
                ),
            )
            .padding(2.dp),
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = TextPrimary,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = TextPrimary.copy(alpha = 0.6f),
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        ) {
            Text(
                label,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

