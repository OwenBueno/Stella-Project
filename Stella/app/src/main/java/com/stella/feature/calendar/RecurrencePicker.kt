package com.stella.feature.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stella.core.calendar.CustomRecurrenceUnit
import com.stella.core.calendar.RecurrenceFrequency
import com.stella.core.calendar.RecurrenceRule
import com.stella.core.ui.components.stellaTextFieldColors
import com.stella.core.ui.theme.DawnCardSurface
import com.stella.core.ui.theme.Primary
import com.stella.core.ui.theme.TextPrimary
import com.stella.core.ui.theme.TextSecondary
import java.time.DayOfWeek

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecurrencePicker(
    rule: RecurrenceRule,
    onRuleChange: (RecurrenceRule) -> Unit,
) {
    val chipColors = FilterChipDefaults.filterChipColors(
        labelColor = TextPrimary,
        containerColor = DawnCardSurface,
        selectedContainerColor = Primary.copy(alpha = 0.25f),
        selectedLabelColor = TextPrimary,
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Repeat", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RecurrencePreset.entries.forEach { preset ->
                FilterChip(
                    selected = preset.isSelected(rule),
                    onClick = { onRuleChange(preset.toRule(rule)) },
                    label = { Text(preset.label) },
                    colors = chipColors,
                )
            }
        }
        if (rule.frequency == RecurrenceFrequency.CUSTOM) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Custom repeat",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = rule.customUnit == CustomRecurrenceUnit.DAYS,
                        onClick = {
                            onRuleChange(
                                rule.copy(
                                    frequency = RecurrenceFrequency.CUSTOM,
                                    daysOfWeek = emptyList(),
                                    interval = rule.interval.coerceAtLeast(1),
                                ),
                            )
                        },
                        label = { Text("Every N days") },
                        colors = chipColors,
                    )
                    FilterChip(
                        selected = rule.customUnit == CustomRecurrenceUnit.WEEKS,
                        onClick = {
                            onRuleChange(
                                rule.copy(
                                    frequency = RecurrenceFrequency.CUSTOM,
                                    daysOfWeek = if (rule.daysOfWeek.isEmpty()) {
                                        listOf(DayOfWeek.MONDAY.value)
                                    } else {
                                        rule.daysOfWeek
                                    },
                                    interval = rule.interval.coerceAtLeast(1),
                                ),
                            )
                        },
                        label = { Text("Every N weeks") },
                        colors = chipColors,
                    )
                }
                OutlinedTextField(
                    value = rule.interval.toString(),
                    onValueChange = { raw ->
                        val n = raw.filter { it.isDigit() }.toIntOrNull()?.coerceIn(1, 99) ?: 1
                        onRuleChange(
                            rule.copy(
                                frequency = RecurrenceFrequency.CUSTOM,
                                interval = n,
                            ),
                        )
                    },
                    label = {
                        Text(
                            when (rule.customUnit) {
                                CustomRecurrenceUnit.WEEKS -> "Repeat every (weeks)"
                                CustomRecurrenceUnit.DAYS -> "Repeat every (days)"
                                else -> "Repeat every"
                            },
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = stellaTextFieldColors(),
                )
                if (rule.customUnit == CustomRecurrenceUnit.WEEKS) {
                    Text(
                        text = "On days",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        DayOfWeek.entries.forEach { dow ->
                            val daySelected = dow.value in rule.daysOfWeek
                            FilterChip(
                                selected = daySelected,
                                onClick = {
                                    val next = if (daySelected) {
                                        rule.daysOfWeek - dow.value
                                    } else {
                                        rule.daysOfWeek + dow.value
                                    }
                                    onRuleChange(
                                        rule.copy(
                                            frequency = RecurrenceFrequency.CUSTOM,
                                            daysOfWeek = next.sorted(),
                                        ),
                                    )
                                },
                                label = { Text(dow.name.take(3)) },
                                colors = chipColors,
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class RecurrencePreset(val label: String) {
    NONE("Does not repeat"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly"),
    CUSTOM("Custom"),
    ;

    fun isSelected(rule: RecurrenceRule): Boolean = when (this) {
        NONE -> rule.frequency == RecurrenceFrequency.NONE
        CUSTOM -> rule.frequency == RecurrenceFrequency.CUSTOM
        else -> toFrequency() == rule.frequency
    }

    fun toFrequency(): RecurrenceFrequency = when (this) {
        NONE -> RecurrenceFrequency.NONE
        DAILY -> RecurrenceFrequency.DAILY
        WEEKLY -> RecurrenceFrequency.WEEKLY
        MONTHLY -> RecurrenceFrequency.MONTHLY
        YEARLY -> RecurrenceFrequency.YEARLY
        CUSTOM -> RecurrenceFrequency.CUSTOM
    }

    fun toRule(current: RecurrenceRule): RecurrenceRule = when (this) {
        NONE -> RecurrenceRule(RecurrenceFrequency.NONE)
        DAILY -> RecurrenceRule(RecurrenceFrequency.DAILY, interval = 1)
        WEEKLY -> RecurrenceRule(RecurrenceFrequency.WEEKLY, interval = 1)
        MONTHLY -> RecurrenceRule(RecurrenceFrequency.MONTHLY, interval = 1)
        YEARLY -> RecurrenceRule(RecurrenceFrequency.YEARLY, interval = 1)
        CUSTOM -> RecurrenceRule(
            frequency = RecurrenceFrequency.CUSTOM,
            interval = if (current.frequency == RecurrenceFrequency.CUSTOM) {
                current.interval.coerceAtLeast(1)
            } else {
                1
            },
            daysOfWeek = if (current.frequency == RecurrenceFrequency.CUSTOM) {
                current.daysOfWeek
            } else {
                emptyList()
            },
        )
    }
}
