package com.stella.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.stella.core.database.dao.CalendarEventDao
import com.stella.core.database.dao.DailyIntentDao
import com.stella.core.database.dao.EveningReviewDao
import com.stella.core.database.dao.HabitDao
import com.stella.core.database.dao.LifeLogDao
import com.stella.core.database.dao.SyncMetaDao
import com.stella.core.database.dao.TaskDao
import com.stella.core.database.entity.CalendarEventEntity
import com.stella.core.database.entity.DailyIntentEntity
import com.stella.core.database.entity.EveningReviewEntity
import com.stella.core.database.entity.HabitCheckInEntity
import com.stella.core.database.entity.HabitEntity
import com.stella.core.database.entity.LifeLogEntity
import com.stella.core.database.entity.SyncMetaEntity
import com.stella.core.database.entity.TaskEntity

@Database(
    entities = [
        HabitEntity::class,
        HabitCheckInEntity::class,
        TaskEntity::class,
        CalendarEventEntity::class,
        SyncMetaEntity::class,
        DailyIntentEntity::class,
        EveningReviewEntity::class,
        LifeLogEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class StellaDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun taskDao(): TaskDao
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun syncMetaDao(): SyncMetaDao
    abstract fun dailyIntentDao(): DailyIntentDao
    abstract fun eveningReviewDao(): EveningReviewDao
    abstract fun lifeLogDao(): LifeLogDao
}
