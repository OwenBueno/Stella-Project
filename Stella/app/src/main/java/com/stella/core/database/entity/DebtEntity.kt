package com.stella.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey val id: String,
    val contactName: String,
    val direction: String,
    val totalAmount: Double,
    val remainingAmount: Double,
    val dueDate: String?,
    val notes: String?,
    val isResolved: Boolean = false,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
    val needsSync: Boolean = true,
)
