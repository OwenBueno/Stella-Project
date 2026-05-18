package com.stella.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.stella.core.database.dao.HabitDao
import com.stella.core.database.entity.CalendarEventEntity
import com.stella.core.database.entity.HabitCheckInEntity
import com.stella.core.database.entity.HabitEntity
import com.stella.core.database.entity.SyncMetaEntity
import com.stella.core.database.entity.TaskEntity

@Database(
    entities = [
        HabitEntity::class,
        HabitCheckInEntity::class,
        TaskEntity::class,
        CalendarEventEntity::class,
        SyncMetaEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class StellaDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
}
