package com.stella.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stella.core.ui.components.StellaNavigationDrawer
import com.stella.core.ui.components.StellaTopBar
import com.stella.core.ui.theme.Background
import com.stella.feature.calendar.CalendarScreen
import com.stella.feature.habits.HabitsScreen
import com.stella.feature.home.HomeScreen
import com.stella.feature.review.ReviewScreen
import com.stella.core.data.SettingsRepository
import com.stella.feature.nfc.NfcEnrollmentActivity
import com.stella.feature.settings.SettingsScreen
import dagger.hilt.android.EntryPointAccessors
import androidx.compose.ui.platform.LocalContext
import com.stella.feature.tasks.TaskDetailScreen
import com.stella.feature.tasks.TasksScreen
import kotlinx.coroutines.launch

@Composable
fun StellaNavHost(initialRoute: String = Routes.HOME) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route?.substringBefore("/")
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val onTaskDetail = isTaskDetailRoute(navBackStackEntry?.destination?.route)

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !onTaskDetail,
        drawerContent = {
            StellaNavigationDrawer(
                items = stellaNavItems,
                currentRoute = currentRoute,
                onItemClick = { route ->
                    navigateTo(route)
                    scope.launch { drawerState.close() }
                },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background),
        ) {
            StellaTopBar(
                showMenu = !onTaskDetail,
                onMenuClick = { scope.launch { drawerState.open() } },
                showBack = onTaskDetail,
                onBack = { navController.popBackStack() },
            )
            NavHost(
                navController = navController,
                startDestination = initialRoute,
                modifier = Modifier.weight(1f),
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        onNavigateToHabits = { navigateTo(Routes.HABITS) },
                        onNavigateToTasks = { navigateTo(Routes.TASKS) },
                        onNavigateToReview = { navigateTo(Routes.REVIEW) },
                    )
                }
                composable(Routes.HABITS) { HabitsScreen() }
                composable(Routes.TASKS) {
                    TasksScreen(
                        onTaskClick = { id -> navController.navigate(Routes.taskDetail(id)) },
                    )
                }
                composable(
                    route = Routes.TASK_DETAIL,
                    arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
                ) { entry ->
                    TaskDetailScreen(
                        taskId = entry.arguments?.getString("taskId").orEmpty(),
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.CALENDAR) { CalendarScreen() }
                composable(Routes.REVIEW) { ReviewScreen() }
                composable(Routes.SETTINGS) {
                val context = LocalContext.current
                var nfcSummary by remember {
                    mutableStateOf(
                        EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            SettingsEntryPoint::class.java,
                        ).settingsRepository().getNfcTagId() ?: "Not enrolled",
                    )
                }
                val enrollLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult(),
                ) {
                    nfcSummary = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        SettingsEntryPoint::class.java,
                    ).settingsRepository().getNfcTagId() ?: "Not enrolled"
                }
                SettingsScreen(
                    onRegisterNfc = {
                        enrollLauncher.launch(
                            android.content.Intent(context, NfcEnrollmentActivity::class.java),
                        )
                    },
                    nfcTagSummary = nfcSummary,
                )
            }
            }
        }
    }
}
