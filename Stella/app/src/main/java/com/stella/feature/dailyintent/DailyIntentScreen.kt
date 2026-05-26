package com.stella.feature.dailyintent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stella.core.database.entity.TaskEntity
import com.stella.core.ui.theme.DawnGradientBottom
import com.stella.core.ui.theme.DawnGradientTop
import com.stella.core.ui.theme.Error
import com.stella.core.ui.theme.DawnCardBorder
import com.stella.core.ui.theme.DawnCardSurface
import com.stella.core.ui.theme.DawnCardSurfaceSubtle
import com.stella.core.ui.theme.Primary
import com.stella.core.ui.theme.PrimaryGradientEnd
import com.stella.core.ui.theme.StellaTheme
import com.stella.core.ui.theme.SurfaceVariant
import com.stella.core.ui.theme.TextMuted
import com.stella.core.ui.theme.TextPrimary
import com.stella.core.ui.theme.TextSecondary

private val PanelShape = RoundedCornerShape(14.dp)
private val FieldShape = RoundedCornerShape(14.dp)

private fun Modifier.dawnPanel(): Modifier = this
    .clip(PanelShape)
    .background(DawnCardSurface)
    .border(1.dp, DawnCardBorder, PanelShape)
@Composable
fun DailyIntentScreen(
    state: DailyIntentUiState,
    onEvent: (DailyIntentUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DawnGradientTop, DawnGradientBottom),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp),
        ) {
            DailyIntentHeader(
                plannedCount = state.plannedCount,
                onInfoClick = { onEvent(DailyIntentUiEvent.ShowInfoSheet) },
            )
            Spacer(modifier = Modifier.height(16.dp))
            PlannedTasksSection(
                plannedTasks = state.plannedTasks,
                onRemove = { onEvent(DailyIntentUiEvent.RemoveTaskFromPlan(it)) },
            )
            Spacer(modifier = Modifier.height(16.dp))
            DailyIntentSearchField(
                query = state.searchQuery,
                onQueryChange = { onEvent(DailyIntentUiEvent.SearchQueryChanged(it)) },
            )
            Spacer(modifier = Modifier.height(12.dp))
            DailyIntentTaskList(
                state = state,
                onAddTask = { onEvent(DailyIntentUiEvent.AddTaskToPlan(it)) },
                onCreateFromQuery = { onEvent(DailyIntentUiEvent.CreateTaskFromQuery) },
                modifier = Modifier.weight(1f),
            )
            state.error?.let {
                Text(
                    it,
                    color = Error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            StartMyDayButton(
                enabled = state.canUnlock,
                isSaving = state.isSaving,
                plannedCount = state.plannedCount,
                onClick = { onEvent(DailyIntentUiEvent.UnlockDay) },
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
    }

    if (state.showInfoSheet) {
        DailyIntentInfoSheet(
            timeZoneLabel = state.timeZoneLabel,
            defaultDurationMinutes = state.defaultDurationMinutes,
            plannedTasks = state.plannedTasks,
            blockSchedules = state.blockSchedules,
            onHourChange = { taskId, hour -> onEvent(DailyIntentUiEvent.BlockHourChanged(taskId, hour)) },
            onMinuteChange = { taskId, minute ->
                onEvent(DailyIntentUiEvent.BlockMinuteChanged(taskId, minute))
            },
            onDismiss = { onEvent(DailyIntentUiEvent.DismissInfoSheet) },
        )
    }
}

@Composable
private fun DailyIntentHeader(
    plannedCount: Int,
    onInfoClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Daily Intent",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                ),
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Add at least 3 tasks for today ($plannedCount/3)",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = TextSecondary,
            )
        }
        IconButton(onClick = onInfoClick) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = "Schedule and timezone info",
                tint = TextMuted,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun PlannedTasksSection(
    plannedTasks: List<PlannedTaskItem>,
    onRemove: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Today's plan",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
            ),
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp)
                .dawnPanel(),
        ) {
            if (plannedTasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Add tasks below to build your plan",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp),
                ) {
                    items(plannedTasks, key = { it.taskId }) { item ->
                        PlannedTaskRow(
                            title = item.title,
                            onRemove = { onRemove(item.taskId) },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlannedTaskRow(
    title: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DawnCardSurfaceSubtle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            color = TextPrimary,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove from plan",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun DailyIntentSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                "Search or create a task…",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
            )
        },
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = TextPrimary,
            fontWeight = FontWeight.Medium,
        ),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = FieldShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedBorderColor = Primary,
            unfocusedBorderColor = DawnCardBorder,
            focusedContainerColor = DawnCardSurface,
            unfocusedContainerColor = DawnCardSurface,
            cursorColor = Primary,
            focusedPlaceholderColor = TextSecondary,
            unfocusedPlaceholderColor = TextSecondary,
        ),
    )
}

@Composable
private fun DailyIntentTaskList(
    state: DailyIntentUiState,
    onAddTask: (String) -> Unit,
    onCreateFromQuery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "All tasks",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
            ),
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .dawnPanel(),
        ) {
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary, modifier = Modifier.size(28.dp))
                }
            }
            state.filteredTasks.isEmpty() && !state.canCreateFromQuery -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (state.searchQuery.isBlank()) "No tasks available. Create one above."
                        else "No matching tasks.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp),
                ) {
                    if (state.canCreateFromQuery) {
                        item(key = "create") {
                            CreateTaskRow(
                                label = state.createLabel,
                                onClick = onCreateFromQuery,
                            )
                        }
                    }
                    items(state.filteredTasks, key = { it.id }) { task ->
                        TaskPickRow(
                            title = task.title,
                            onClick = { onAddTask(task.id) },
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun CreateTaskRow(
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Add, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            label,
            color = Primary,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun TaskPickRow(
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            color = TextPrimary,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StartMyDayButton(
    enabled: Boolean,
    isSaving: Boolean,
    plannedCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = when {
        isSaving -> "Saving…"
        plannedCount < 3 -> "Start My Day ($plannedCount/3)"
        else -> "Start My Day"
    }
    val backgroundBrush = if (enabled) {
        Brush.horizontalGradient(listOf(Primary, PrimaryGradientEnd))
    } else {
        Brush.horizontalGradient(listOf(SurfaceVariant, SurfaceVariant))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundBrush)
            .clickable(enabled = enabled && !isSaving, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (enabled) TextPrimary else TextSecondary,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF12141A)
@Composable
private fun DailyIntentScreenEmptyPreview() {
    StellaTheme {
        DailyIntentScreen(
            state = DailyIntentUiState(isLoading = false),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF12141A)
@Composable
private fun DailyIntentScreenPartialPreview() {
    StellaTheme {
        DailyIntentScreen(
            state = DailyIntentUiState(
                isLoading = false,
                plannedTaskIds = listOf("task-1"),
                tasks = listOf(
                    TaskEntity(
                        id = "task-1",
                        title = "Write proposal",
                        notes = null,
                        scheduledAt = null,
                        durationMinutes = null,
                        status = "TODO",
                        sortOrder = 0,
                        priority = "HIGH",
                        createdAt = "",
                        updatedAt = "",
                    ),
                ),
                searchQuery = "meet",
            ),
            onEvent = {},
        )
    }
}
