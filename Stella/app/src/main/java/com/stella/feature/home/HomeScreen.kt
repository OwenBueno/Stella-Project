package com.stella.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stella.core.ui.theme.TextSecondary
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stella.core.ui.components.StellaCard
import com.stella.core.ui.components.StellaSectionHeader
import com.stella.core.ui.components.StellaStatCard
import com.stella.core.ui.theme.Primary
import com.stella.core.ui.theme.TextPrimary

@Composable
fun HomeScreen(
    onNavigateToHabits: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToReview: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StellaSectionHeader(eyebrow = "Command", title = "Control Center")

        if (state.isLoading) {
            CircularProgressIndicator()
        } else {
            Text(state.greeting, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)

            if (state.showEveningReviewBanner) {
                StellaCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Evening review due", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("Close the day before you disconnect.", color = TextSecondary)
                        Button(onClick = onNavigateToReview, modifier = Modifier.fillMaxWidth()) {
                            Text("Start review")
                        }
                    }
                }
            }

            StellaStatCard(label = "Efficiency", value = "${state.efficiencyPercent}%")
            StellaStatCard(label = "Active protocols", value = state.habitCount.toString())

            StellaCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Critical directives", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    if (state.topTasks.isEmpty()) {
                        Text(
                            "No open directives.",
                            color = TextSecondary,
                        )
                    } else {
                        state.topTasks.forEach { task ->
                            Text("• ${task.title}", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        }
                    }
                }
            }

            Button(
                onClick = onNavigateToHabits,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("OPEN MATRIX")
            }
            Button(
                onClick = onNavigateToTasks,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
            ) {
                Text("OPEN FRONTLINE")
            }
        }
    }
}
