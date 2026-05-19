package com.stella.feature.tasks

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.stella.core.ui.components.stellaTextFieldColors
import com.stella.core.ui.theme.TextSecondary
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stella.core.data.TaskStatus
import com.stella.core.database.entity.TaskEntity
import com.stella.core.ui.components.StellaSectionHeader
import com.stella.core.ui.theme.Border
import com.stella.core.ui.theme.Error
import com.stella.core.ui.theme.Primary
import com.stella.core.ui.theme.Success

@Composable
fun TasksScreen(
    onTaskClick: (String) -> Unit,
    viewModel: TasksViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StellaSectionHeader(eyebrow = "Operations", title = "The Frontline")

        OutlinedTextField(
            value = state.newTaskTitle,
            onValueChange = { viewModel.onEvent(TasksUiEvent.TitleChanged(it)) },
            label = { Text("New directive") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = stellaTextFieldColors(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { viewModel.onEvent(TasksUiEvent.AddForToday) }) {
                Text("Today", color = Primary)
            }
            TextButton(onClick = { viewModel.onEvent(TasksUiEvent.AddForTomorrow) }) {
                Text("Tomorrow", color = Primary)
            }
        }

        TaskSection("Today", state.today, onTaskClick, viewModel)
        TaskSection("Tomorrow", state.tomorrow, onTaskClick, viewModel)
        TaskSection("Upcoming", state.upcoming, onTaskClick, viewModel)
    }
}

@Composable
private fun TaskSection(
    title: String,
    tasks: List<TaskEntity>,
    onTaskClick: (String) -> Unit,
    viewModel: TasksViewModel,
) {
    if (tasks.isEmpty()) return
    Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = Primary)
    tasks.forEach { task ->
        TaskRow(
            task = task,
            onClick = { onTaskClick(task.id) },
            onToggle = { viewModel.onEvent(TasksUiEvent.ToggleStatus(task.id)) },
            onDelete = { viewModel.onEvent(TasksUiEvent.Delete(task.id)) },
        )
    }
}

@Composable
private fun TaskRow(
    task: TaskEntity,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val done = task.status == TaskStatus.DONE.name
    val started = task.status == TaskStatus.IN_PROGRESS.name
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Border)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .border(
                    2.dp,
                    when {
                        done -> Success
                        started -> Primary
                        else -> Border
                    },
                )
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            if (done) Icon(Icons.Default.Check, null, tint = Primary, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                ),
                color = if (done) TextSecondary else MaterialTheme.colorScheme.onSurface,
            )
            task.scheduledAt?.let {
                Text(it.take(16), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
        if (task.priority == "HIGH") {
            Box(modifier = Modifier.size(8.dp).border(1.dp, Error))
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}
