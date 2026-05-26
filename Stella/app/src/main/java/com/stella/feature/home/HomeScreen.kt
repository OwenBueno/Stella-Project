package com.stella.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stella.core.data.TaskStatus
import com.stella.core.ui.theme.DawnCardBorder
import com.stella.core.ui.theme.DawnCardSurface
import com.stella.core.ui.theme.DawnCardSurfaceSubtle
import com.stella.core.ui.theme.DawnGradientBottom
import com.stella.core.ui.theme.DawnGradientTop
import com.stella.core.ui.theme.Primary
import com.stella.core.ui.theme.PrimaryGlow
import com.stella.core.ui.theme.StellaTheme
import com.stella.core.ui.theme.TextMuted
import com.stella.core.ui.theme.TextPrimary
import com.stella.core.ui.theme.TextSecondary
import java.time.LocalDate

private val TaskRowHeight = 56.dp
private val MaxVisibleTasks = 3

@Composable
fun HomeScreen(
    onNavigateToHabits: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToReview: () -> Unit = {},
    onTaskClick: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DawnGradientTop, DawnGradientBottom),
                ),
            ),
    ) {
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LiveClockBlock(dateLine = state.dateLine, clockText = state.clockText)
                WeekCalendarStrip(
                    weekDays = state.weekDays,
                    onSelectDay = { viewModel.onEvent(HomeUiEvent.SelectWeekDay(it)) },
                )

                if (state.showEveningReviewBanner) {
                    EveningReviewBanner(onNavigateToReview = onNavigateToReview)
                }

                MetricsRow(
                    completionPercent = state.completionPercent,
                    activeTaskCount = state.activeTaskCount,
                )

                TasksInProgressSection(
                    modifier = Modifier.weight(1f),
                    tasks = state.tasksInProgress,
                    onTaskClick = onTaskClick,
                    onNavigateToTasks = onNavigateToTasks,
                )
            }
        }
    }
}

@Composable
private fun LiveClockBlock(
    dateLine: String,
    clockText: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = dateLine,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = TextSecondary,
        )
        Text(
            text = clockText,
            style = MaterialTheme.typography.displaySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
            ),
            color = TextPrimary,
        )
    }
}

@Composable
private fun WeekCalendarStrip(
    weekDays: List<WeekDayUi>,
    onSelectDay: (LocalDate) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(weekDays, key = { it.date.toString() }) { day ->
            WeekDayPill(day = day, onClick = { onSelectDay(day.date) })
        }
    }
}

@Composable
private fun WeekDayPill(
    day: WeekDayUi,
    onClick: () -> Unit,
) {
    val selected = day.isSelected
    val today = day.isToday
    Column(
        modifier = Modifier
            .width(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                when {
                    selected -> Modifier
                        .background(Primary)
                        .border(1.dp, PrimaryGlow, RoundedCornerShape(12.dp))
                    today -> Modifier
                        .background(Primary.copy(alpha = 0.25f))
                        .border(1.dp, Primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    else -> Modifier
                        .background(DawnCardSurface)
                        .border(1.dp, DawnCardBorder, RoundedCornerShape(12.dp))
                },
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = day.label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) TextPrimary else TextMuted,
        )
        Text(
            text = day.dayNumber.toString(),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = if (selected) TextPrimary else TextSecondary,
        )
    }
}

@Composable
private fun MetricsRow(
    completionPercent: Int,
    activeTaskCount: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MetricCard(modifier = Modifier.weight(1f).fillMaxHeight()) {
            CompletionRing(percent = completionPercent)
        }
        MetricCard(modifier = Modifier.weight(1f).fillMaxHeight()) {
            ActiveTasksMetric(activeTaskCount = activeTaskCount)
        }
    }
}

@Composable
private fun ActiveTasksMetric(activeTaskCount: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(MetricRingSize),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = activeTaskCount.toString(),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
            )
        }
        Text(
            text = "Active",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun TasksInProgressSection(
    tasks: List<TaskRowUi>,
    onTaskClick: (String) -> Unit,
    onNavigateToTasks: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listScroll = rememberScrollState()
    val maxListHeight = TaskRowHeight * MaxVisibleTasks + 12.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Tasks in Progress",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary,
            )
            IconButton(
                onClick = onNavigateToTasks,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Open Frontline",
                    tint = Primary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .dawnPanel()
                .padding(vertical = 6.dp),
        ) {
            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No active tasks for this day.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = maxListHeight)
                        .verticalScroll(listScroll),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    tasks.forEach { task ->
                        TaskInProgressRow(
                            task = task,
                            onClick = { onTaskClick(task.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskInProgressRow(
    task: TaskRowUi,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TaskRowHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(DawnCardSurfaceSubtle)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val inProgress = task.status == TaskStatus.IN_PROGRESS.name
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (inProgress) Primary.copy(alpha = 0.2f) else DawnCardSurface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (inProgress) Icons.Default.PlayArrow else Icons.Outlined.Circle,
                contentDescription = null,
                tint = if (inProgress) Primary else TextMuted,
                modifier = Modifier.size(if (inProgress) 18.dp else 14.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = TextPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun EveningReviewBanner(onNavigateToReview: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .dawnPanel()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "Evening review due",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary,
        )
        Text(
            "Close the day before you disconnect.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        OutlinedButton(
            onClick = onNavigateToReview,
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.6f)),
        ) {
            Text("Start review", color = Primary)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF12141A)
@Composable
private fun HomeScreenPreview() {
    StellaTheme {
        HomeScreen(
            onNavigateToHabits = {},
            onNavigateToTasks = {},
        )
    }
}
