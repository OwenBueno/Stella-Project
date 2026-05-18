package com.stella.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sortOrder: Int,
    val active: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
    val needsSync: Boolean = true,
)
