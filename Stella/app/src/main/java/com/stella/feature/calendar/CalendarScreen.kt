package com.stella.feature.calendar

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stella.core.ui.theme.DawnGradientBottom
import com.stella.core.ui.theme.DawnGradientTop
import com.stella.core.ui.theme.Primary
import java.time.LocalDate

@Composable
fun CalendarScreen(
    initialOpenDate: String? = null,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    LaunchedEffect(initialOpenDate) {
        initialOpenDate?.let { raw ->
            runCatching { LocalDate.parse(raw) }.getOrNull()?.let { viewModel.openDayFromDeepLink(it) }
        }
    }

    LaunchedEffect(state.editorDraft.reminderOffsetsMinutes) {
        if (state.editorDraft.reminderOffsetsMinutes.isNotEmpty() &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

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
                MonthNavigator(
                    month = state.month,
                    onPrev = { viewModel.onEvent(CalendarUiEvent.PrevMonth) },
                    onNext = { viewModel.onEvent(CalendarUiEvent.NextMonth) },
                )
                TemporalMonthGrid(
                    month = state.month,
                    today = state.today,
                    dayStatuses = state.dayStatuses,
                    onDayClick = { viewModel.onEvent(CalendarUiEvent.SelectDay(it)) },
                )
            }
        }
    }

    val selectedDay = state.selectedDay
    if (state.showDaySheet && selectedDay != null) {
        TemporalDaySheet(
            day = selectedDay,
            completed = state.dayCompleted,
            scheduled = state.dayScheduled,
            onAddEvent = { viewModel.onEvent(CalendarUiEvent.OpenCreateEvent) },
            onEditEvent = { viewModel.onEvent(CalendarUiEvent.OpenEditEvent(it)) },
            onDismiss = { viewModel.onEvent(CalendarUiEvent.DismissDaySheet) },
        )
    }

    if (state.showEventEditor) {
        EventEditorSheet(
            mode = state.editorMode,
            draft = state.editorDraft,
            onTitleChange = { viewModel.onEvent(CalendarUiEvent.EditorTitleChanged(it)) },
            onStartDateChange = { viewModel.onEvent(CalendarUiEvent.EditorStartDateChanged(it)) },
            onEndDateChange = { viewModel.onEvent(CalendarUiEvent.EditorEndDateChanged(it)) },
            onStartHourChange = { viewModel.onEvent(CalendarUiEvent.EditorStartHourChanged(it)) },
            onStartMinuteChange = { viewModel.onEvent(CalendarUiEvent.EditorStartMinuteChanged(it)) },
            onEndHourChange = { viewModel.onEvent(CalendarUiEvent.EditorEndHourChanged(it)) },
            onEndMinuteChange = { viewModel.onEvent(CalendarUiEvent.EditorEndMinuteChanged(it)) },
            onRecurrenceChange = { viewModel.onEvent(CalendarUiEvent.EditorRecurrenceChanged(it)) },
            onRemindersChange = { viewModel.onEvent(CalendarUiEvent.EditorRemindersChanged(it)) },
            onSave = { viewModel.onEvent(CalendarUiEvent.SaveEvent) },
            onDelete = if (state.editorMode == EventEditorMode.EDIT) {
                { viewModel.onEvent(CalendarUiEvent.DeleteEvent) }
            } else {
                null
            },
            onDismiss = { viewModel.onEvent(CalendarUiEvent.DismissEventEditor) },
        )
    }
}
