package com.stella.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val amount: Double,
    val category: String,
    val description: String?,
    val date: String,
    val linkedTaskId: String?,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
    val needsSync: Boolean = true,
)
