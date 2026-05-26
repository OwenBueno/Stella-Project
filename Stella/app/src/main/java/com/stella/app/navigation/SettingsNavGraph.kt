package com.stella.app.navigation

import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.stella.feature.settings.SettingsSection
import com.stella.feature.settings.SettingsSectionDetailScreen
import com.stella.feature.settings.SettingsSectionListScreen
import com.stella.feature.settings.SettingsViewModel

fun NavGraphBuilder.settingsNavGraph(navController: NavHostController) {
    navigation(
        route = Routes.SETTINGS,
        startDestination = Routes.SETTINGS_HOME,
    ) {
        composable(Routes.SETTINGS_HOME) {
            SettingsSectionListScreen(
                onSectionSelected = { section ->
                    navController.navigate(section.route) {
                        launchSingleTop = true
                    }
                },
            )
        }

        SettingsSection.entries.forEach { section ->
            composable(section.route) { entry ->
                val settingsEntry = remember(entry) {
                    navController.getBackStackEntry(Routes.SETTINGS)
                }
                val viewModel: SettingsViewModel = hiltViewModel(settingsEntry)
                SettingsSectionDetailScreen(
                    section = section,
                    settingsViewModel = viewModel,
                )
            }
        }
    }
}
