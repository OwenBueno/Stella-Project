package com.stella.feature.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stella.core.data.TaskRepository
import com.stella.core.database.entity.TaskEntity
import com.stella.core.ui.components.StellaSectionHeader
import com.stella.core.ui.theme.TextPrimary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
) : ViewModel() {
    var task by mutableStateOf<TaskEntity?>(null)
        private set

    fun load(id: String) {
        viewModelScope.launch {
            task = taskRepository.getById(id)
        }
    }
}

@Composable
fun TaskDetailScreen(
    taskId: String,
    onBack: () -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(taskId) { viewModel.load(taskId) }
    val task = viewModel.task

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimary,
            )
        }
        StellaSectionHeader(eyebrow = "Directive", title = task?.title ?: "Loading…")
        task?.let {
            Text(
                "Status: ${it.status}",
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
            )
            it.notes?.let { notes ->
                Text(notes, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            }
            it.scheduledAt?.let { at ->
                Text("Scheduled: $at", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            }
        }
    }
}
