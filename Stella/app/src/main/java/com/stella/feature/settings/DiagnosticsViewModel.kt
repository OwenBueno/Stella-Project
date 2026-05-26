package com.stella.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stella.core.data.FinanceRepository
import com.stella.core.data.SettingsRepository
import com.stella.core.data.SyncRepository
import com.stella.core.database.dao.DailyIntentDao
import com.stella.core.database.dao.EveningReviewDao
import com.stella.core.network.StellaApi
import com.stella.core.util.DateUtils
import com.stella.core.util.TimeService
import com.stella.feature.morning.MorningAlarmTrigger
import com.stella.feature.morning.MorningLockPermissions
import com.stella.sync.DiagnosticsNotifier
import com.stella.sync.EveningReviewScheduler
import com.stella.sync.MorningAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiagnosticsUiState(
    val message: String? = null,
    val isSyncing: Boolean = false,
    val isBusy: Boolean = false,
)

sealed interface DiagnosticsUiEvent {
    data object PingApi : DiagnosticsUiEvent
    data object SimulateDailyReset : DiagnosticsUiEvent
    data object SyncNow : DiagnosticsUiEvent
    data object PurgeLocal : DiagnosticsUiEvent
    data object LogTestPenalty : DiagnosticsUiEvent
    data object ScheduleMorningTestAlarm : DiagnosticsUiEvent
    data object TriggerMorningTestAlarmNow : DiagnosticsUiEvent
    data object RefreshMorningPermissions : DiagnosticsUiEvent
}

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stellaApi: StellaApi,
    private val syncRepository: SyncRepository,
    private val financeRepository: FinanceRepository,
    private val settingsRepository: SettingsRepository,
    private val dailyIntentDao: DailyIntentDao,
    private val eveningReviewDao: EveningReviewDao,
    private val timeService: TimeService,
) : ViewModel() {

    fun morningPermissionStatus(): String {
        return buildString {
            append("NFC enrolled: ${settingsRepository.hasNfcTagEnrolled()}\n")
            append("Notifications: ${MorningLockPermissions.hasPostNotifications(context)}\n")
            append("Full-screen intent: ${MorningLockPermissions.canUseFullScreenIntent(context)}\n")
            append("Overlay: ${MorningLockPermissions.canDrawOverlays(context)}\n")
            append("Exact alarms: ${MorningLockPermissions.canScheduleExactAlarms(context)}\n")
            append("Battery exempt: ${MorningLockPermissions.isIgnoringBatteryOptimizations(context)}")
        }
    }

    private val _state = MutableStateFlow(DiagnosticsUiState())
    val state: StateFlow<DiagnosticsUiState> = _state.asStateFlow()

    fun onEvent(event: DiagnosticsUiEvent) {
        when (event) {
            DiagnosticsUiEvent.PingApi -> pingApi()
            DiagnosticsUiEvent.SimulateDailyReset -> simulateDailyReset()
            DiagnosticsUiEvent.SyncNow -> syncNow()
            DiagnosticsUiEvent.PurgeLocal -> purgeLocal()
            DiagnosticsUiEvent.LogTestPenalty -> logTestPenalty()
            DiagnosticsUiEvent.ScheduleMorningTestAlarm -> scheduleMorningTestDelayed()
            DiagnosticsUiEvent.TriggerMorningTestAlarmNow -> triggerMorningTestNow()
            DiagnosticsUiEvent.RefreshMorningPermissions -> {
                _state.update { it.copy(message = morningPermissionStatus()) }
            }
        }
    }

    private fun triggerMorningTestNow() {
        _state.update { it.copy(isBusy = true, message = null) }
        MorningAlarmTrigger.fire(context, isTest = true) { success, error ->
            _state.update {
                it.copy(
                    isBusy = false,
                    message = if (success) {
                        "Morning test alarm started (same as setup wizard)."
                    } else {
                        error ?: "Could not start morning test alarm."
                    },
                )
            }
        }
    }

    private fun scheduleMorningTestDelayed() {
        val blocked = MorningLockPermissions.testBlockedMessage(context)
        if (blocked != null) {
            _state.update { it.copy(message = blocked) }
            return
        }
        val result = MorningAlarmScheduler.scheduleTest(context, delaySeconds = 10)
        _state.update {
            it.copy(
                message = if (result.success) {
                    "Morning test alarm in 10 seconds (background, same flow as wizard)."
                } else {
                    result.error
                },
            )
        }
    }

    fun showTestNotification() {
        DiagnosticsNotifier.showTestNotification(context)
        _state.update { it.copy(message = "Test notification sent.") }
    }

    private fun pingApi() {
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, message = null) }
            runCatching { stellaApi.health() }
                .onSuccess { response ->
                    _state.update {
                        it.copy(isBusy = false, message = "API OK: ${response.status}")
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(isBusy = false, message = "Ping failed: ${e.message}")
                    }
                }
        }
    }

    private fun simulateDailyReset() {
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, message = null) }
            val dateKey = DateUtils.formatDate(timeService.today())
            dailyIntentDao.deleteByDate(dateKey)
            eveningReviewDao.deleteByDate(dateKey)
            EveningReviewScheduler.schedule(context)
            _state.update {
                it.copy(
                    isBusy = false,
                    message = "Today's daily intent and evening review cleared.",
                )
            }
        }
    }

    private fun syncNow() {
        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true, message = null) }
            syncRepository.syncNow()
                .onSuccess { msg -> _state.update { it.copy(isSyncing = false, message = msg) } }
                .onFailure { e -> _state.update { it.copy(isSyncing = false, message = e.message) } }
        }
    }

    private fun purgeLocal() {
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, message = null) }
            syncRepository.purgeLocal()
            _state.update { it.copy(isBusy = false, message = "Local data purged.") }
        }
    }

    private fun logTestPenalty() {
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, message = null) }
            val amount = settingsRepository.getPenaltyAmount()
            financeRepository.recordPenaltyEgress(
                taskId = "00000000-0000-4000-8000-000000000001",
                amount = amount,
                description = "Diagnostics test penalty",
            )
            _state.update {
                it.copy(isBusy = false, message = "Logged test penalty egress ($amount).")
            }
        }
    }
}
