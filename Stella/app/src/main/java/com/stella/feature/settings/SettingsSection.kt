package com.stella.feature.settings

import com.stella.app.navigation.Routes

enum class SettingsSection(
    val route: String,
    val label: String,
) {
    General(Routes.SETTINGS_GENERAL, "General"),
    Morning(Routes.SETTINGS_MORNING, "Morning lock"),
    Schedule(Routes.SETTINGS_SCHEDULE, "Schedule"),
    Developer(Routes.SETTINGS_DEVELOPER, "Developer"),
    Diagnostics(Routes.SETTINGS_DIAGNOSTICS, "Diagnostics"),
    ;

    companion object {
        fun fromRoute(route: String?): SettingsSection =
            entries.firstOrNull { it.route == route?.substringBefore("?") } ?: General
    }
}
