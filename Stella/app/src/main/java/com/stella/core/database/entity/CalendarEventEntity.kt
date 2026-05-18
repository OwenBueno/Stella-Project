package com.stella.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val startAt: String,
    val endAt: String,
    val linkedTaskId: String?,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
    val needsSync: Boolean = true,
)
