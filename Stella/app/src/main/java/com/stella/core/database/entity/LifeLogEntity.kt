package com.stella.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "life_logs")
data class LifeLogEntity(
    @PrimaryKey val id: String,
    val type: String,
    val payload: String,
    val timestamp: String,
    val updatedAt: String,
    val needsSync: Boolean = true,
)
