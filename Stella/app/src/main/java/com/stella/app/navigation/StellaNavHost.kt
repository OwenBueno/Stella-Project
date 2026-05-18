package com.stella.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.stella.feature.calendar.CalendarScreen
import com.stella.feature.habits.HabitsScreen
import com.stella.feature.home.HomeScreen
import com.stella.feature.settings.SettingsScreen
import com.stella.feature.tasks.TasksScreen

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit,
)

@Composable
fun StellaNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items = listOf(
        BottomNavItem(Routes.HOME, "Home") { Icon(Icons.Default.Home, contentDescription = null) },
        BottomNavItem(Routes.HABITS, "Habits") { Icon(Icons.Default.GridView, contentDescription = null) },
        BottomNavItem(Routes.TASKS, "Tasks") { Icon(Icons.Default.CheckCircle, contentDescription = null) },
        BottomNavItem(Routes.CALENDAR, "Calendar") { Icon(Icons.Default.CalendarToday, contentDescription = null) },
        BottomNavItem(Routes.SETTINGS, "Settings") { Icon(Icons.Default.Settings, contentDescription = null) },
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = item.icon,
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.HOME) { HomeScreen() }
            composable(Routes.HABITS) { HabitsScreen() }
            composable(Routes.TASKS) { TasksScreen() }
            composable(Routes.CALENDAR) { CalendarScreen() }
            composable(Routes.SETTINGS) { SettingsScreen() }
        }
    }
}
