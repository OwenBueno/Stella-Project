package com.stella.app.navigation

object Routes {
    const val HOME = "home"
    const val HABITS = "habits"
    const val TASKS = "tasks"
    const val TASKS_WITH_EDIT = "tasks?editTaskId={editTaskId}"
    const val CALENDAR = "calendar"
    const val CALENDAR_WITH_DATE = "calendar?openDate={openDate}"
    const val REVIEW = "review"
    const val FINANCES = "finances"
    const val SETTINGS = "settings"
    const val SETTINGS_HOME = "settings/home"
    const val SETTINGS_MAIN = "settings/home"
    const val SETTINGS_GENERAL = "settings/general"
    const val SETTINGS_MORNING = "settings/morning"
    const val SETTINGS_SCHEDULE = "settings/schedule"
    const val SETTINGS_DEVELOPER = "settings/developer"
    const val SETTINGS_ADVANCED = "settings/developer"
    const val SETTINGS_DIAGNOSTICS = "settings/diagnostics"

    fun tasks(editTaskId: String? = null): String =
        if (editTaskId != null) "tasks?editTaskId=$editTaskId" else TASKS

    fun calendar(openDate: String? = null): String =
        if (openDate != null) "calendar?openDate=$openDate" else CALENDAR
}
