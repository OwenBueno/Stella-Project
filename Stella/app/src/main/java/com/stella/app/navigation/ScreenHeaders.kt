package com.stella.app.navigation

import com.stella.core.ui.components.StellaScreenHeader

fun stellaScreenHeaderForRoute(route: String?): StellaScreenHeader {
    val base = route?.substringBefore("?") ?: Routes.HOME
    return when (base) {
        Routes.HOME -> StellaScreenHeader("CONTROL", "Control Center")
        Routes.HABITS -> StellaScreenHeader("DISCIPLINE", "Habits")
        Routes.TASKS -> StellaScreenHeader("OPERATIONS", "The Frontline")
        Routes.CALENDAR -> StellaScreenHeader("TEMPORAL", "Temporal Grid")
        Routes.FINANCES -> StellaScreenHeader("TREASURY", "Finances")
        Routes.REVIEW -> StellaScreenHeader("PROTOCOL", "Evening Review")
        Routes.SETTINGS_HOME,
        Routes.SETTINGS_MAIN,
        -> StellaScreenHeader("USER CONFIG", "Settings")
        Routes.SETTINGS_GENERAL -> StellaScreenHeader("USER CONFIG", "General")
        Routes.SETTINGS_MORNING -> StellaScreenHeader("USER CONFIG", "Morning lock")
        Routes.SETTINGS_SCHEDULE -> StellaScreenHeader("USER CONFIG", "Schedule")
        Routes.SETTINGS_DEVELOPER,
        Routes.SETTINGS_ADVANCED,
        -> StellaScreenHeader("SYSTEM", "Developer")
        Routes.SETTINGS_DIAGNOSTICS -> StellaScreenHeader("DEVELOPER", "Diagnostics")
        else -> StellaScreenHeader("STELLA", "Stella")
    }
}
