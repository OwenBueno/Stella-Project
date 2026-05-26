package com.stella.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_intents")
data class DailyIntentEntity(
    @PrimaryKey val id: String,
    val date: String,
    val plannedTaskIds: List<String>,
    val completedAt: String,
    val nfcTagId: String,
    val updatedAt: String,
    val needsSync: Boolean = true,
)
