package com.stella.feature.habits

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.stella.core.data.CheckInStatus
import com.stella.core.data.CheckInUi
import com.stella.core.data.HabitWithCheckIns
import com.stella.core.ui.theme.DawnCardBorder
import com.stella.core.ui.theme.DawnCardSurface
import com.stella.core.ui.theme.Primary
import com.stella.core.ui.theme.PrimaryGlow
import com.stella.core.ui.theme.Success
import com.stella.core.ui.theme.TextMuted
import com.stella.core.ui.theme.TextPrimary
import com.stella.core.ui.theme.TextSecondary
import com.stella.core.util.DateUtils
import com.stella.feature.home.dawnPanel
import java.time.LocalDate

private val CellShape = RoundedCornerShape(8.dp)
private const val NameColumnFraction = 0.34f
private const val CellGapDp = 4
private val NameGridSpacing = 10.dp

@Composable
fun HabitsAddToolbar(onAddClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onAddClick) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create habit",
                tint = Primary,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
fun WeekNavigator(
    weekLabel: String,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevWeek) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous week", tint = TextPrimary)
        }
        Text(
            text = weekLabel,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = TextSecondary,
        )
        IconButton(onClick = onNextWeek) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next week", tint = TextPrimary)
        }
    }
}

@Composable
fun HabitsNameHint(modifier: Modifier = Modifier) {
    Text(
        text = "Tap a habit name to rename or delete.",
        style = MaterialTheme.typography.bodySmall,
        color = TextMuted,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun HabitsTrackerGrid(
    habits: List<HabitWithCheckIns>,
    weekDates: List<LocalDate>,
    dayHeaders: List<String>,
    today: LocalDate,
    onHabitNameClick: (String) -> Unit,
    onCellClick: (habitId: String, date: LocalDate) -> Unit,
    onCellLongPress: (habitId: String, date: LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        GridHeaderRow(dayHeaders = dayHeaders)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            habits.forEach { row ->
                HabitGridRow(
                    row = row,
                    weekDates = weekDates,
                    today = today,
                    onHabitNameClick = { onHabitNameClick(row.habit.id) },
                    onCellClick = onCellClick,
                    onCellLongPress = onCellLongPress,
                )
            }
        }
    }
}

@Composable
private fun GridHeaderRow(dayHeaders: List<String>) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        val nameWidth = maxWidth * NameColumnFraction
        val gridWidth = maxWidth - nameWidth - NameGridSpacing - (CellGapDp * 7).dp
        val cellWidth = gridWidth / 7
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.width(nameWidth))
            Spacer(modifier = Modifier.width(NameGridSpacing))
            dayHeaders.take(7).forEach { label ->
                Box(
                    modifier = Modifier
                        .width(cellWidth)
                        .padding(horizontal = (CellGapDp / 2).dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = TextMuted,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HabitGridRow(
    row: HabitWithCheckIns,
    weekDates: List<LocalDate>,
    today: LocalDate,
    onHabitNameClick: () -> Unit,
    onCellClick: (habitId: String, date: LocalDate) -> Unit,
    onCellLongPress: (habitId: String, date: LocalDate) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val nameWidth = maxWidth * NameColumnFraction
        val gridWidth = maxWidth - nameWidth - NameGridSpacing - (CellGapDp * 7).dp
        val cellWidth = gridWidth / 7
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = row.habit.name,
                modifier = Modifier
                    .width(nameWidth)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onHabitNameClick)
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.width(NameGridSpacing))
            weekDates.take(7).forEach { date ->
                val checkIn = row.checkIns[DateUtils.formatDate(date)]
                val isDone = checkIn?.status == CheckInStatus.DONE
                HabitDayCell(
                    checkIn = checkIn,
                    isToday = date == today,
                    isDone = isDone,
                    modifier = Modifier
                        .width(cellWidth)
                        .padding(horizontal = (CellGapDp / 2).dp)
                        .aspectRatio(1f),
                    onClick = { onCellClick(row.habit.id, date) },
                    onLongClick = {
                        if (isDone && checkIn?.completedAt != null) {
                            onCellLongPress(row.habit.id, date)
                        }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HabitDayCell(
    checkIn: CheckInUi?,
    isToday: Boolean,
    isDone: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = if (isDone) {
        Success.copy(alpha = 0.35f)
    } else {
        DawnCardSurface
    }
    val borderColor = when {
        isToday && isDone -> PrimaryGlow
        isToday -> Primary.copy(alpha = 0.6f)
        else -> DawnCardBorder
    }
    Box(
        modifier = modifier
            .clip(CellShape)
            .border(1.dp, borderColor, CellShape)
            .background(background)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isDone) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Done",
                tint = Success,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
fun HabitsEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .dawnPanel()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "No habits yet. Tap + to add your first protocol.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
        )
    }
}

@Composable
fun CompletionTooltip(
    message: String,
    onDismiss: () -> Unit,
) {
    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Box(
            modifier = Modifier
                .padding(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DawnCardSurface)
                .border(1.dp, DawnCardBorder, RoundedCornerShape(12.dp))
                .clickable(onClick = onDismiss)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
            )
        }
    }
}
