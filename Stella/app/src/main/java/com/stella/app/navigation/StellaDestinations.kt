package com.stella.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import com.stella.core.ui.components.StellaNavItem

val stellaNavItems = listOf(
    StellaNavItem(Routes.HOME, "Control", Icons.Default.Home),
    StellaNavItem(Routes.HABITS, "Matrix", Icons.Default.GridView),
    StellaNavItem(Routes.TASKS, "Frontline", Icons.Default.CheckCircle),
    StellaNavItem(Routes.CALENDAR, "Calendar", Icons.Default.CalendarToday),
    StellaNavItem(Routes.REVIEW, "Review", Icons.Default.Assessment),
    StellaNavItem(Routes.SETTINGS, "System", Icons.Default.Settings),
)

private val rootRoutes = setOf(
    Routes.HOME,
    Routes.HABITS,
    Routes.TASKS,
    Routes.CALENDAR,
    Routes.REVIEW,
    Routes.SETTINGS,
)

fun isRootDestination(route: String?): Boolean = route != null && route in rootRoutes

fun isTaskDetailRoute(route: String?): Boolean =
    route != null && route.startsWith("tasks/") && route != Routes.TASKS
