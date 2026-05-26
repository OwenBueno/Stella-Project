package com.stella.sync

import com.stella.core.data.DailyIntentRepository
import com.stella.core.data.SettingsRepository
import com.stella.feature.morning.MorningLockController
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MorningLockEntryPoint {
    fun settingsRepository(): SettingsRepository
    fun dailyIntentRepository(): DailyIntentRepository
    fun morningLockController(): MorningLockController
}
