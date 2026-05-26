package com.stella.app.navigation



import androidx.compose.foundation.background

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.material3.DrawerValue

import androidx.compose.material3.ModalNavigationDrawer

import androidx.compose.material3.rememberDrawerState

import androidx.compose.runtime.Composable

import androidx.compose.runtime.getValue

import androidx.compose.runtime.rememberCoroutineScope

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

import com.stella.feature.finances.FinanceScreen

import com.stella.feature.habits.HabitsScreen

import com.stella.feature.home.HomeScreen

import com.stella.feature.review.ReviewScreen


import com.stella.feature.tasks.TasksScreen

import kotlinx.coroutines.launch



@Composable

fun StellaNavHost(initialRoute: String = Routes.HOME) {

    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val destinationRoute = navBackStackEntry?.destination?.route
    val currentRoute = destinationRoute?.substringBefore("/")?.substringBefore("?")
    val showSettingsBack = destinationRoute != null &&
        destinationRoute != Routes.SETTINGS_HOME &&
        destinationRoute.startsWith("${Routes.SETTINGS}/")

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val scope = rememberCoroutineScope()



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

                header = stellaScreenHeaderForRoute(destinationRoute),

                showMenu = !showSettingsBack,

                onMenuClick = { scope.launch { drawerState.open() } },

                showBack = showSettingsBack,

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

                        onTaskClick = { id -> navigateTo(Routes.tasks(editTaskId = id)) },

                    )

                }

                composable(Routes.HABITS) { HabitsScreen() }

                composable(Routes.TASKS) { TasksScreen() }

                composable(

                    route = Routes.TASKS_WITH_EDIT,

                    arguments = listOf(

                        navArgument("editTaskId") {

                            type = NavType.StringType

                            nullable = true

                            defaultValue = null

                        },

                    ),

                ) { entry ->

                    val editTaskId = entry.arguments?.getString("editTaskId")

                    TasksScreen(initialEditTaskId = editTaskId)

                }

                composable(Routes.CALENDAR) { CalendarScreen() }
                composable(
                    route = Routes.CALENDAR_WITH_DATE,
                    arguments = listOf(
                        navArgument("openDate") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                    ),
                ) { entry ->
                    CalendarScreen(initialOpenDate = entry.arguments?.getString("openDate"))
                }

                composable(Routes.FINANCES) { FinanceScreen() }

                composable(Routes.REVIEW) { ReviewScreen() }

                settingsNavGraph(navController)

            }

        }

    }

}

