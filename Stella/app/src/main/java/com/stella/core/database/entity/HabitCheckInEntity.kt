package com.stella.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habit_check_ins",
    indices = [Index(value = ["habitId", "date"], unique = true)],
)
data class HabitCheckInEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val date: String,
    val status: String,
    val updatedAt: String,
    val needsSync: Boolean = true,
)
