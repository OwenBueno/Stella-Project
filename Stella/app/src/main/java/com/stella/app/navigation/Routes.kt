package com.stella.app.navigation

object Routes {
    const val HOME = "home"
    const val HABITS = "habits"
    const val TASKS = "tasks"
    const val CALENDAR = "calendar"
    const val REVIEW = "review"
    const val SETTINGS = "settings"
    const val TASK_DETAIL = "tasks/{taskId}"

    fun taskDetail(taskId: String) = "tasks/$taskId"
}
