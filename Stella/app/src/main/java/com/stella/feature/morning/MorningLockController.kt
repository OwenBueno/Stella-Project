package com.stella.feature.morning

import android.content.Context
import com.stella.core.data.DailyIntentRepository
import com.stella.core.data.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class EnforcementReason {
    ALARM,
    APP_OPEN,
    TEST,
}

@Singleton
class MorningLockController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val dailyIntentRepository: DailyIntentRepository,
) {
    private val _isEnforcing = MutableStateFlow(false)
    val isEnforcing: StateFlow<Boolean> = _isEnforcing.asStateFlow()

    private val _morningLockVisible = MutableStateFlow(false)
    private val _dailyIntentVisible = MutableStateFlow(false)
    private val _lockSurfaceReady = MutableStateFlow(false)

    suspend fun shouldEnforce(): Boolean {
        if (!settingsRepository.isMorningLockEnabled()) return false
        return !dailyIntentRepository.hasCompletedToday()
    }

    fun isEnforcingNow(): Boolean = _isEnforcing.value

    fun isLockSurfaceReady(): Boolean = _lockSurfaceReady.value

    fun setLockSurfaceReady(ready: Boolean) {
        _lockSurfaceReady.value = ready
    }

    fun canShowEnforcementOverlay(): Boolean =
        _isEnforcing.value && _lockSurfaceReady.value && !isStellaMorningFlowInForeground()

    fun startEnforcement(@Suppress("UNUSED_PARAMETER") reason: EnforcementReason) {
        if (_isEnforcing.value) return
        _isEnforcing.value = true
        _lockSurfaceReady.value = false
        MorningLockEnforcementService.start(context)
    }

    fun stopEnforcement() {
        if (!_isEnforcing.value && !_morningLockVisible.value && !_dailyIntentVisible.value) {
            MorningLockOverlay.hide(context)
            MorningLockEnforcementService.stop(context)
            return
        }
        _isEnforcing.value = false
        _morningLockVisible.value = false
        _dailyIntentVisible.value = false
        _lockSurfaceReady.value = false
        MorningLockOverlay.hide(context)
        MorningLockEnforcementService.stop(context)
        MorningAlarmRinger.stop()
    }

    fun setMorningLockVisible(visible: Boolean) {
        _morningLockVisible.value = visible
        if (visible) {
            MorningLockOverlay.hide(context)
        }
    }

    fun setDailyIntentVisible(visible: Boolean) {
        _dailyIntentVisible.value = visible
        if (visible) {
            MorningLockOverlay.hide(context)
        }
    }

    fun isStellaMorningFlowInForeground(): Boolean =
        _morningLockVisible.value || _dailyIntentVisible.value

    fun needsSetup(): Boolean =
        settingsRepository.isMorningLockEnabled() && !settingsRepository.isMorningSetupCompleted()
}
