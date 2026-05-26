package com.stella.feature.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stella.core.data.TaskStatus
import com.stella.core.ui.components.stellaTextFieldColors
import com.stella.core.ui.theme.DawnCardBorder
import com.stella.core.ui.theme.DawnCardSurface
import com.stella.core.ui.theme.Error
import com.stella.core.ui.theme.Primary
import com.stella.core.ui.theme.PrimaryGlow
import com.stella.core.ui.theme.Success
import com.stella.core.ui.theme.TextMuted
import com.stella.core.ui.theme.TextPrimary
import com.stella.core.ui.theme.TextSecondary
import com.stella.feature.home.dawnPanel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private val CardShape = RoundedCornerShape(12.dp)

@Composable
fun DirectiveComposer(
    expanded: Boolean,
    title: String,
    composerError: String?,
    onExpand: () -> Unit,
    onTitleChange: (String) -> Unit,
    onAddToday: () -> Unit,
    onAddTomorrow: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .dawnPanel()
            .clickable(enabled = !expanded, onClick = onExpand)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (!expanded) {
            Text(
                text = if (title.isBlank()) "New directive" else title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = if (title.isBlank()) TextMuted else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("New directive") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = stellaTextFieldColors(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = false,
                        onClick = onAddToday,
                        label = { Text("Today") },
                        colors = FilterChipDefaults.filterChipColors(
                            labelColor = TextPrimary,
                            containerColor = DawnCardSurface,
                        ),
                    )
                    FilterChip(
                        selected = false,
                        onClick = onAddTomorrow,
                        label = { Text("Tomorrow") },
                        colors = FilterChipDefaults.filterChipColors(
                            labelColor = TextPrimary,
                            containerColor = DawnCardSurface,
                        ),
                    )
                }
                composerError?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = Error,
                    )
                }
            }
        }
    }
}

@Composable
fun FrontlineTabs(
    selectedTab: FrontlineTab,
    onSelectTab: (FrontlineTab) -> Unit,
) {
    val tabs = listOf(
        FrontlineTab.TODAY to "Today",
        FrontlineTab.TOMORROW to "Tomorrow",
        FrontlineTab.ALL to "All Sequences",
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        tabs.forEach { (tab, label) ->
            val selected = tab == selectedTab
            Column(
                modifier = Modifier.clickable { onSelectTab(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                    color = if (selected) TextPrimary else TextMuted,
                )
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(width = if (selected) 32.dp else 0.dp, height = 2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(if (selected) Primary else PrimaryGlow.copy(alpha = 0f)),
                )
            }
        }
    }
}

@Composable
fun ReorderableActiveTaskList(
    tasks: List<TaskCardUi>,
    onReorder: (Int, Int) -> Unit,
    onReorderCommitted: () -> Unit,
    onEdit: (String) -> Unit,
    onToggle: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onReorder(from.index, to.index)
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(tasks, key = { it.task.id }) { card ->
            ReorderableItem(reorderState, key = card.task.id) { isDragging ->
                FrontlineTaskCard(
                    card = card,
                    isDragging = isDragging,
                    showDragHandle = true,
                    dragHandleModifier = Modifier.draggableHandle(
                        onDragStopped = { onReorderCommitted() },
                    ),
                    onEdit = { onEdit(card.task.id) },
                    onToggle = { onToggle(card.task.id) },
                    onDelete = { onDelete(card.task.id) },
                )
            }
        }
    }
}

@Composable
fun CompletedTaskList(
    tasks: List<TaskCardUi>,
    onEdit: (String) -> Unit,
    onToggle: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tasks.isEmpty()) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Completed",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = TextSecondary,
            modifier = Modifier.padding(top = 8.dp),
        )
        tasks.forEach { card ->
            FrontlineTaskCard(
                card = card,
                isDragging = false,
                showDragHandle = false,
                dragHandleModifier = Modifier,
                onEdit = { onEdit(card.task.id) },
                onToggle = { onToggle(card.task.id) },
                onDelete = { onDelete(card.task.id) },
            )
        }
    }
}

@Composable
fun FrontlineTaskCard(
    card: TaskCardUi,
    isDragging: Boolean,
    showDragHandle: Boolean,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    dragHandleModifier: Modifier = Modifier,
) {
    val task = card.task
    val done = task.status == TaskStatus.DONE.name
    val inProgress = task.status == TaskStatus.IN_PROGRESS.name
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(
                if (isDragging) DawnCardSurface.copy(alpha = 0.95f) else DawnCardSurface.copy(alpha = 0.75f),
            )
            .border(1.dp, if (isDragging) PrimaryGlow else DawnCardBorder, CardShape)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (showDragHandle) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Reorder",
                tint = TextMuted,
                modifier = dragHandleModifier.size(22.dp),
            )
        } else {
            Box(modifier = Modifier.size(22.dp))
        }
        Text(
            text = card.sequenceLabel,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            ),
            color = Primary,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onEdit),
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                ),
                color = if (done) TextMuted else TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            card.scheduleChip?.let { chip ->
                ScheduleTimeChip(text = chip)
            }
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    1.5.dp,
                    when {
                        done -> Success
                        inProgress -> Primary
                        else -> DawnCardBorder
                    },
                    RoundedCornerShape(8.dp),
                )
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            if (done) {
                Icon(Icons.Default.Check, contentDescription = "Completed", tint = Success, modifier = Modifier.size(16.dp))
            }
        }
        Icon(
            imageVector = Icons.Outlined.Delete,
            contentDescription = "Delete",
            tint = TextMuted,
            modifier = Modifier
                .size(20.dp)
                .clickable(onClick = onDelete),
        )
    }
}

@Composable
fun ScheduleTimeChip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = TextSecondary,
        modifier = Modifier.padding(top = 4.dp),
    )
}
