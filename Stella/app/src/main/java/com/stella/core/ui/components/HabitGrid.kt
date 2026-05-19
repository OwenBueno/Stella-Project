package com.stella.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stella.core.data.CheckInStatus
import com.stella.core.data.HabitWithCheckIns
import com.stella.core.ui.theme.Border
import com.stella.core.ui.theme.Error
import com.stella.core.ui.theme.Primary
import com.stella.core.ui.theme.Success
import com.stella.core.ui.theme.TextPrimary
import com.stella.core.util.DateUtils
import java.time.LocalDate

@Composable
fun HabitGrid(
    habits: List<HabitWithCheckIns>,
    weekDates: List<LocalDate>,
    today: LocalDate,
    onCellClick: (habitId: String, date: LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
) {
    val scrollState = rememberScrollState()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(modifier = Modifier.width(120.dp))
            weekDates.forEach { date ->
                StellaLabel(
                    text = date.dayOfWeek.name.take(3),
                    modifier = Modifier.width(48.dp),
                )
            }
        }
        habits.forEach { row ->
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = row.habit.name,
                    modifier = Modifier.width(120.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    maxLines = 1,
                )
                weekDates.forEach { date ->
                    val status = row.checkIns[DateUtils.formatDate(date)]
                    HabitCell(
                        status = status,
                        isToday = date == today,
                        isPast = date.isBefore(today),
                        onClick = {
                            if (!readOnly) onCellClick(row.habit.id, date)
                        },
                        readOnly = readOnly,
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitCell(
    status: CheckInStatus?,
    isToday: Boolean,
    isPast: Boolean,
    onClick: () -> Unit,
    readOnly: Boolean = false,
) {
    val background = when (status) {
        CheckInStatus.DONE -> Success.copy(alpha = 0.35f)
        CheckInStatus.MISSED -> Error.copy(alpha = 0.35f)
        else -> Color.Transparent
    }
    val borderColor = when {
        isToday -> Primary
        status != null -> Color.Transparent
        isPast -> Error.copy(alpha = 0.65f)
        else -> Border
    }
    Box(
        modifier = Modifier
            .size(48.dp)
            .border(1.dp, borderColor)
            .background(background)
            .then(
                if (readOnly) Modifier else Modifier.clickable(onClick = onClick),
            ),
        contentAlignment = Alignment.Center,
    ) {
        when (status) {
            CheckInStatus.DONE -> Icon(Icons.Default.Check, null, tint = Primary)
            CheckInStatus.MISSED -> Icon(Icons.Default.Close, null, tint = Error)
            else -> {}
        }
    }
}
