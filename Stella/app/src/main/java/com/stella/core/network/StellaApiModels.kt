package com.stella.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SyncPushRequest(
    val deviceId: String,
    val pushedAt: String,
    val habits: List<SyncHabitDto> = emptyList(),
    val habitCheckIns: List<SyncCheckInDto> = emptyList(),
    val tasks: List<SyncTaskDto> = emptyList(),
    val events: List<SyncEventDto> = emptyList(),
    val dailyIntents: List<SyncDailyIntentDto> = emptyList(),
    val eveningReviews: List<SyncEveningReviewDto> = emptyList(),
    val lifeLogs: List<SyncLifeLogDto> = emptyList(),
)

@Serializable
data class SyncHabitDto(
    val id: String,
    val name: String,
    val sortOrder: Int,
    val active: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
)

@Serializable
data class SyncCheckInDto(
    val id: String,
    val habitId: String,
    val date: String,
    val status: String,
    val updatedAt: String,
)

@Serializable
data class SyncTaskDto(
    val id: String,
    val title: String,
    val notes: String? = null,
    val scheduledAt: String? = null,
    val durationMinutes: Int? = null,
    val status: String,
    val priority: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
)

@Serializable
data class SyncEventDto(
    val id: String,
    val title: String,
    val startAt: String,
    val endAt: String,
    val linkedTaskId: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
)

@Serializable
data class SyncDailyIntentDto(
    val id: String,
    val date: String,
    val top3TaskIds: List<String>,
    val completedAt: String,
    val nfcTagId: String,
    val updatedAt: String,
)

@Serializable
data class SyncEveningReviewDto(
    val id: String,
    val date: String,
    val plannedVsActual: String? = null,
    val reflectionText: String? = null,
    val habitGridSnapshot: JsonElement,
    val completedAt: String,
    val updatedAt: String,
)

@Serializable
data class SyncLifeLogDto(
    val id: String,
    val type: String,
    val payload: String,
    val timestamp: String,
    val updatedAt: String,
)

@Serializable
data class SyncPushResponse(
    val accepted: Int,
    val conflicts: List<SyncConflict> = emptyList(),
)

@Serializable
data class SyncConflict(
    val entity: String,
    val id: String,
    val serverDocument: kotlinx.serialization.json.JsonObject? = null,
)

@Serializable
data class SyncPullResponse(
    val serverTime: String,
    val habits: List<SyncHabitDto> = emptyList(),
    val habitCheckIns: List<SyncCheckInDto> = emptyList(),
    val tasks: List<SyncTaskDto> = emptyList(),
    val events: List<SyncEventDto> = emptyList(),
    val dailyIntents: List<SyncDailyIntentDto> = emptyList(),
    val eveningReviews: List<SyncEveningReviewDto> = emptyList(),
    val lifeLogs: List<SyncLifeLogDto> = emptyList(),
)
