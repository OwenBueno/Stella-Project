package com.stella.core.di

import android.content.Context
import androidx.room.Room
import com.stella.core.database.StellaDatabase
import com.stella.core.database.dao.CalendarEventDao
import com.stella.core.database.dao.DailyIntentDao
import com.stella.core.database.dao.EveningReviewDao
import com.stella.core.database.dao.HabitDao
import com.stella.core.database.dao.LifeLogDao
import com.stella.core.database.dao.SyncMetaDao
import com.stella.core.database.dao.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): StellaDatabase =
        Room.databaseBuilder(context, StellaDatabase::class.java, "stella.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideHabitDao(database: StellaDatabase): HabitDao = database.habitDao()

    @Provides
    fun provideTaskDao(database: StellaDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideCalendarEventDao(database: StellaDatabase): CalendarEventDao =
        database.calendarEventDao()

    @Provides
    fun provideSyncMetaDao(database: StellaDatabase): SyncMetaDao = database.syncMetaDao()

    @Provides
    fun provideDailyIntentDao(database: StellaDatabase): DailyIntentDao = database.dailyIntentDao()

    @Provides
    fun provideEveningReviewDao(database: StellaDatabase): EveningReviewDao =
        database.eveningReviewDao()

    @Provides
    fun provideLifeLogDao(database: StellaDatabase): LifeLogDao = database.lifeLogDao()
}
