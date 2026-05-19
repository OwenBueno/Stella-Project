package com.stella.feature.dailyintent

data class BlockSchedule(
    val hour: Int,
    val minute: Int,
    val durationMinutes: Int,
) {
    fun displayTime(): String = "%02d:%02d".format(hour, minute)
}
