package com.stella.feature.finances

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stella.core.data.DebtDirection
import com.stella.core.data.FinanceCategories
import com.stella.core.data.FinanceMonthSummary
import com.stella.core.data.TransactionType
import com.stella.core.database.entity.DebtEntity
import com.stella.core.database.entity.TransactionEntity
import com.stella.core.ui.components.StellaLabel
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val PanelShape = RoundedCornerShape(12.dp)

@Composable
fun FinanceJumpToToday(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 0.dp),
    ) {
        Text("Back to this month", color = Primary, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun FinanceSummaryPanel(summary: FinanceMonthSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .dawnPanel()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StellaLabel(text = "MONTHLY NET")
            Text(
                text = formatMoney(summary.netBalance),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                ),
                color = if (summary.netBalance >= 0) Success else Error,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SummaryMetric(
                label = "Income",
                amount = summary.ingress,
                color = Success,
                modifier = Modifier.weight(1f),
            )
            SummaryMetric(
                label = "Expenses",
                amount = summary.egress,
                color = Error,
                modifier = Modifier.weight(1f),
            )
            SummaryMetric(
                label = "Owed to me",
                amount = summary.owedToMe,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            SummaryMetric(
                label = "I owe",
                amount = summary.owedByMe,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    amount: Double,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(PanelShape)
            .background(DawnCardSurface.copy(alpha = 0.5f))
            .padding(horizontal = 6.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            formatMoney(amount),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
            ),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun FinanceAddTransactionButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Add transaction")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceEntrySheet(
    state: FinanceUiState,
    onEvent: (FinanceUiEvent) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()
    val isEdit = state.editingTransactionId != null
    val canSave = state.draftAmount.toDoubleOrNull()?.let { it > 0.0 } == true &&
        state.draftCategory.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = { onEvent(FinanceUiEvent.CloseEntrySheet) },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = if (isEdit) "Edit transaction" else "New transaction",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary,
            )
            Text("Type", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.draftType == TransactionType.INGRESS,
                    onClick = { onEvent(FinanceUiEvent.DraftTypeChanged(TransactionType.INGRESS)) },
                    label = { Text("Income") },
                    colors = filterChipColors(),
                )
                FilterChip(
                    selected = state.draftType == TransactionType.EGRESS,
                    onClick = { onEvent(FinanceUiEvent.DraftTypeChanged(TransactionType.EGRESS)) },
                    label = { Text("Expense") },
                    colors = filterChipColors(),
                )
            }
            OutlinedTextField(
                value = state.draftAmount,
                onValueChange = { onEvent(FinanceUiEvent.DraftAmountChanged(it)) },
                label = { Text("Amount") },
                placeholder = { Text("0.00") },
                modifier = Modifier.fillMaxWidth(),
                colors = stellaTextFieldColors(),
                singleLine = true,
            )
            FinanceCategoryField(
                transactionType = state.draftType,
                value = state.draftCategory,
                onValueChange = { onEvent(FinanceUiEvent.DraftCategoryChanged(it)) },
            )
            OutlinedTextField(
                value = state.draftDescription,
                onValueChange = { onEvent(FinanceUiEvent.DraftDescriptionChanged(it)) },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
                colors = stellaTextFieldColors(),
                singleLine = true,
            )
            state.error?.let {
                Text(it, color = Error, style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = { onEvent(FinanceUiEvent.SaveEntry) },
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave,
            ) {
                Text(if (isEdit) "Save changes" else "Add transaction")
            }
            if (isEdit) {
                OutlinedButton(
                    onClick = { onEvent(FinanceUiEvent.DeleteTransaction) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = Error)
                    Text("Delete transaction", color = Error, modifier = Modifier.padding(start = 8.dp))
                }
            }
            TextButton(
                onClick = { onEvent(FinanceUiEvent.CloseEntrySheet) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("Cancel", color = TextSecondary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceDebtSheet(
    state: FinanceUiState,
    onEvent: (FinanceUiEvent) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()
    val isEdit = state.editingDebtId != null
    val canSave = state.draftContactName.isNotBlank() &&
        state.draftDebtTotal.toDoubleOrNull()?.let { it > 0.0 } == true

    ModalBottomSheet(
        onDismissRequest = { onEvent(FinanceUiEvent.CloseDebtSheet) },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = if (isEdit) "Edit debt" else "New debt",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary,
            )
            OutlinedTextField(
                value = state.draftContactName,
                onValueChange = { onEvent(FinanceUiEvent.DraftContactChanged(it)) },
                label = { Text("Contact name") },
                modifier = Modifier.fillMaxWidth(),
                colors = stellaTextFieldColors(),
                singleLine = true,
            )
            Text("Direction", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.draftDebtDirection == DebtDirection.OWED_TO_ME,
                    onClick = { onEvent(FinanceUiEvent.DraftDebtDirectionChanged(DebtDirection.OWED_TO_ME)) },
                    label = { Text("Owed to me") },
                    colors = filterChipColors(),
                )
                FilterChip(
                    selected = state.draftDebtDirection == DebtDirection.OWED_BY_ME,
                    onClick = { onEvent(FinanceUiEvent.DraftDebtDirectionChanged(DebtDirection.OWED_BY_ME)) },
                    label = { Text("I owe") },
                    colors = filterChipColors(),
                )
            }
            OutlinedTextField(
                value = state.draftDebtTotal,
                onValueChange = { onEvent(FinanceUiEvent.DraftDebtTotalChanged(it)) },
                label = { Text("Total amount") },
                placeholder = { Text("0.00") },
                modifier = Modifier.fillMaxWidth(),
                colors = stellaTextFieldColors(),
                singleLine = true,
            )
            if (isEdit) {
                OutlinedTextField(
                    value = state.draftDebtRemaining,
                    onValueChange = { onEvent(FinanceUiEvent.DraftDebtRemainingChanged(it)) },
                    label = { Text("Remaining amount") },
                    placeholder = { Text("0.00") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = stellaTextFieldColors(),
                    singleLine = true,
                )
            }
            OutlinedTextField(
                value = state.draftNotes,
                onValueChange = { onEvent(FinanceUiEvent.DraftNotesChanged(it)) },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                colors = stellaTextFieldColors(),
                singleLine = true,
            )
            state.error?.let {
                Text(it, color = Error, style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = { onEvent(FinanceUiEvent.SaveDebt) },
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave,
            ) {
                Text(if (isEdit) "Save changes" else "Add debt")
            }
            if (isEdit) {
                OutlinedButton(
                    onClick = { onEvent(FinanceUiEvent.DeleteDebt) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = Error)
                    Text("Delete debt", color = Error, modifier = Modifier.padding(start = 8.dp))
                }
            }
            TextButton(
                onClick = { onEvent(FinanceUiEvent.CloseDebtSheet) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("Cancel", color = TextSecondary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinanceCategoryField(
    transactionType: TransactionType,
    value: String,
    onValueChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val builtIn = remember(transactionType) { FinanceCategories.forType(transactionType) }
    val filtered = remember(value, builtIn) {
        if (value.isBlank()) {
            builtIn
        } else {
            builtIn.filter { it.contains(value, ignoreCase = true) }
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text("Category") },
            placeholder = { Text(FinanceCategories.defaultFor(transactionType)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            singleLine = true,
            colors = stellaTextFieldColors(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            filtered.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category) },
                    onClick = {
                        onValueChange(category)
                        expanded = false
                    },
                )
            }
            if (filtered.isEmpty() && value.isNotBlank()) {
                DropdownMenuItem(
                    text = { Text("Use \"$value\"") },
                    onClick = { expanded = false },
                )
            }
        }
    }
}

@Composable
fun FinanceFilterTabs(
    selected: TransactionFilter,
    onSelected: (TransactionFilter) -> Unit,
) {
    val tabs = listOf(
        TransactionFilter.ALL to "All",
        TransactionFilter.INGRESS to "Income",
        TransactionFilter.EGRESS to "Expenses",
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        tabs.forEach { (filter, label) ->
            val isSelected = filter == selected
            Column(
                modifier = Modifier.clickable { onSelected(filter) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                    color = if (isSelected) TextPrimary else TextMuted,
                )
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(width = if (isSelected) 32.dp else 0.dp, height = 2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(if (isSelected) Primary else PrimaryGlow.copy(alpha = 0f)),
                )
            }
        }
    }
}

@Composable
fun FinanceTransactionList(
    transactions: List<TransactionEntity>,
    onTransactionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        StellaLabel(text = "LEDGER")
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "No entries this month.",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(top = 6.dp, bottom = 4.dp),
            ) {
                items(transactions, key = { it.id }) { tx ->
                    FinanceTransactionRow(
                        tx = tx,
                        onClick = { onTransactionClick(tx.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FinanceTransactionRow(
    tx: TransactionEntity,
    onClick: () -> Unit,
) {
    val isIngress = tx.type == TransactionType.INGRESS.wire
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .dawnPanel()
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(PanelShape)
                .background(DawnCardSurface),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = formatDateShort(tx.date),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                tx.category,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            tx.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1)
            }
        }
        Text(
            text = "${if (isIngress) "+" else "−"}${formatMoney(tx.amount)}",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            ),
            color = if (isIngress) Success else Error,
        )
    }
}

@Composable
fun FinanceDebtSection(
    debts: List<DebtEntity>,
    onAddDebt: () -> Unit,
    onDebtClick: (String) -> Unit,
    onResolve: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StellaLabel(text = "OPEN DEBTS")
        TextButton(onClick = onAddDebt) {
            Text("Add debt", color = Primary)
        }
    }
    if (debts.isEmpty()) {
        Text(
            "No open debts.",
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall,
        )
        return
    }
    Column(
        modifier = Modifier
            .heightIn(max = 100.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        debts.forEach { debt ->
            FinanceDebtRow(
                debt = debt,
                onClick = { onDebtClick(debt.id) },
                onResolve = onResolve,
            )
        }
    }
}

@Composable
private fun FinanceDebtRow(
    debt: DebtEntity,
    onClick: () -> Unit,
    onResolve: (String) -> Unit,
) {
    val progress = if (debt.totalAmount > 0) {
        ((debt.totalAmount - debt.remainingAmount) / debt.totalAmount).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .dawnPanel()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(debt.contactName, color = TextPrimary, fontWeight = FontWeight.Medium)
                Text(
                    if (debt.direction == DebtDirection.OWED_TO_ME.wire) "Owed to me" else "I owe",
                    color = TextMuted,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Primary,
                trackColor = DawnCardBorder,
            )
            Text(
                "${formatMoney(debt.remainingAmount)} remaining",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        TextButton(
            onClick = { onResolve(debt.id) },
            modifier = Modifier.align(Alignment.End),
        ) {
            Text("Resolve", color = Primary)
        }
    }
}

@Composable
private fun filterChipColors() = FilterChipDefaults.filterChipColors(
    labelColor = TextPrimary,
    containerColor = DawnCardSurface,
    selectedContainerColor = DawnCardSurface,
    selectedLabelColor = Primary,
)

private fun formatMoney(value: Double): String = String.format("$%.2f", value)

private fun formatDateShort(iso: String): String = runCatching {
    val date = Instant.parse(iso).atZone(ZoneId.systemDefault()).toLocalDate()
    "${date.month.name.take(3)}\n${date.dayOfMonth}"
}.getOrDefault("—")
