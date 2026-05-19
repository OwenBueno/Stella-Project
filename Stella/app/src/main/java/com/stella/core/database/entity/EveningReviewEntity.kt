package com.stella.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "evening_reviews")
data class EveningReviewEntity(
    @PrimaryKey val id: String,
    val date: String,
    val plannedVsActual: String?,
    val reflectionText: String?,
    val habitGridSnapshot: String,
    val completedAt: String,
    val updatedAt: String,
    val needsSync: Boolean = true,
)
