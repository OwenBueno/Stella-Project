package com.stella.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stella.BuildConfig
import com.stella.core.data.SettingsRepository
import com.stella.feature.morning.MorningLockPermissions
import com.stella.sync.EveningReviewScheduler
import com.stella.sync.MorningAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

data class SettingsUiState(
    val apiUrl: String = BuildConfig.API_BASE_URL,
    val apiKey: String = "",
    val message: String? = null,
    val isSyncing: Boolean = false,
    val useDeviceTimezone: Boolean = true,
    val timeZoneId: String = ZoneId.systemDefault().id,
    val timeZoneSearch: String = "",
    val eveningHour: Int = 20,
    val eveningMinute: Int = 30,
    val blockDurationMinutes: Int = SettingsRepository.DEFAULT_BLOCK_DURATION,
    val defaultTaskStartHour: Int = SettingsRepository.DEFAULT_TASK_START_HOUR,
    val defaultTaskStartMinute: Int = SettingsRepository.DEFAULT_TASK_START_MINUTE,
    val nfcTagId: String? = null,
    val penaltyEnabled: Boolean = false,
    val penaltyAmount: Double = SettingsRepository.DEFAULT_PENALTY_AMOUNT,
    val penaltyAmountInput: String = SettingsRepository.DEFAULT_PENALTY_AMOUNT.toString(),
    val morningLockEnabled: Boolean = true,
    val morningWakeHour: Int = SettingsRepository.DEFAULT_MORNING_WAKE_HOUR,
    val morningWakeMinute: Int = SettingsRepository.DEFAULT_MORNING_WAKE_MINUTE,
    val morningSetupCompleted: Boolean = false,
    val morningHasOverlay: Boolean = false,
    val morningHasNotifications: Boolean = false,
    val morningHasExactAlarms: Boolean = false,
    val morningHasFullScreenIntent: Boolean = false,
    val morningNfcEnrolled: Boolean = false,
    val morningAlarmSoundLabel: String = "System default",
    val morningAlarmSoundUri: String? = null,
    val morningAlarmVolumeRampSeconds: Int = SettingsRepository.DEFAULT_MORNING_ALARM_VOLUME_RAMP_SECONDS,
)

sealed interface SettingsUiEvent {
    data class ApiUrlChanged(val url: String) : SettingsUiEvent
    data class ApiKeyChanged(val key: String) : SettingsUiEvent
    data object SaveCredentials : SettingsUiEvent
    data class UseDeviceTimezoneChanged(val enabled: Boolean) : SettingsUiEvent
    data class TimeZoneChanged(val value: String) : SettingsUiEvent
    data class TimeZoneSearchChanged(val query: String) : SettingsUiEvent
    data class BlockDurationChanged(val minutes: Int) : SettingsUiEvent
    data class DefaultTaskStartChanged(val hour: Int, val minute: Int) : SettingsUiEvent
    data class EveningTimeChanged(val hour: Int, val minute: Int) : SettingsUiEvent
    data object SaveScheduleDefaults : SettingsUiEvent
    data object RefreshNfcTag : SettingsUiEvent
    data class PenaltyEnabledChanged(val enabled: Boolean) : SettingsUiEvent
    data class PenaltyAmountChanged(val amount: String) : SettingsUiEvent
    data object SavePenaltySettings : SettingsUiEvent
    data class MorningLockEnabledChanged(val enabled: Boolean) : SettingsUiEvent
    data class MorningWakeTimeChanged(val hour: Int, val minute: Int) : SettingsUiEvent
    data object SaveMorningLockSettings : SettingsUiEvent
    data object RefreshMorningLockStatus : SettingsUiEvent
    data class MorningAlarmSoundSelected(val uri: android.net.Uri?) : SettingsUiEvent
    data class MorningAlarmVolumeRampChanged(val seconds: Int) : SettingsUiEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(loadFromRepository())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private fun loadFromRepository(): SettingsUiState = SettingsUiState(
        apiUrl = settingsRepository.getApiBaseUrl(),
        apiKey = settingsRepository.getApiKey(),
        useDeviceTimezone = settingsRepository.getUseDeviceTimezone(),
        timeZoneId = settingsRepository.getTimeZoneId(),
        eveningHour = settingsRepository.getEveningReviewHour(),
        eveningMinute = settingsRepository.getEveningReviewMinute(),
        blockDurationMinutes = settingsRepository.getBlockDurationMinutes(),
        defaultTaskStartHour = settingsRepository.getDefaultTaskStartHour(),
        defaultTaskStartMinute = settingsRepository.getDefaultTaskStartMinute(),
        nfcTagId = settingsRepository.getNfcTagId(),
        penaltyEnabled = settingsRepository.isPenaltyEnabled(),
        penaltyAmount = settingsRepository.getPenaltyAmount(),
        penaltyAmountInput = settingsRepository.getPenaltyAmount().toString(),
        morningLockEnabled = settingsRepository.isMorningLockEnabled(),
        morningWakeHour = settingsRepository.getMorningWakeHour(),
        morningWakeMinute = settingsRepository.getMorningWakeMinute(),
        morningSetupCompleted = settingsRepository.isMorningSetupCompleted(),
        morningHasOverlay = MorningLockPermissions.canDrawOverlays(context),
        morningHasNotifications = MorningLockPermissions.hasPostNotifications(context),
        morningHasExactAlarms = MorningLockPermissions.canScheduleExactAlarms(context),
        morningHasFullScreenIntent = MorningLockPermissions.canUseFullScreenIntent(context),
        morningNfcEnrolled = settingsRepository.hasNfcTagEnrolled(),
        morningAlarmSoundLabel = settingsRepository.getMorningAlarmSoundLabel()
            ?: if (settingsRepository.getMorningAlarmSoundUri() != null) "Custom sound" else "System default",
        morningAlarmSoundUri = settingsRepository.getMorningAlarmSoundUri(),
        morningAlarmVolumeRampSeconds = settingsRepository.getMorningAlarmVolumeRampSeconds(),
    )

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.ApiUrlChanged -> _state.update { it.copy(apiUrl = event.url) }
            is SettingsUiEvent.ApiKeyChanged -> _state.update { it.copy(apiKey = event.key) }
            SettingsUiEvent.SaveCredentials -> {
                settingsRepository.setApiBaseUrl(_state.value.apiUrl)
                settingsRepository.setApiKey(_state.value.apiKey)
                _state.update { it.copy(message = "Credentials saved.") }
            }
            is SettingsUiEvent.UseDeviceTimezoneChanged -> {
                settingsRepository.setUseDeviceTimezone(event.enabled)
                _state.update {
                    it.copy(
                        useDeviceTimezone = event.enabled,
                        timeZoneId = if (event.enabled) {
                            ZoneId.systemDefault().id
                        } else {
                            it.timeZoneId
                        },
                    )
                }
            }
            is SettingsUiEvent.TimeZoneChanged -> {
                _state.update { it.copy(timeZoneId = event.value.trim(), useDeviceTimezone = false) }
                settingsRepository.setUseDeviceTimezone(false)
            }
            is SettingsUiEvent.TimeZoneSearchChanged ->
                _state.update { it.copy(timeZoneSearch = event.query) }
            is SettingsUiEvent.BlockDurationChanged ->
                _state.update { it.copy(blockDurationMinutes = event.minutes.coerceIn(15, 480)) }
            is SettingsUiEvent.DefaultTaskStartChanged ->
                _state.update {
                    it.copy(
                        defaultTaskStartHour = event.hour.coerceIn(0, 23),
                        defaultTaskStartMinute = event.minute.coerceIn(0, 59),
                    )
                }
            is SettingsUiEvent.EveningTimeChanged ->
                _state.update {
                    it.copy(
                        eveningHour = event.hour.coerceIn(0, 23),
                        eveningMinute = event.minute.coerceIn(0, 59),
                    )
                }
            SettingsUiEvent.SaveScheduleDefaults -> saveScheduleDefaults()
            SettingsUiEvent.RefreshNfcTag ->
                _state.update { it.copy(nfcTagId = settingsRepository.getNfcTagId()) }
            is SettingsUiEvent.PenaltyEnabledChanged ->
                _state.update { it.copy(penaltyEnabled = event.enabled) }
            is SettingsUiEvent.PenaltyAmountChanged ->
                _state.update { it.copy(penaltyAmountInput = event.amount) }
            SettingsUiEvent.SavePenaltySettings -> savePenaltySettings()
            is SettingsUiEvent.MorningLockEnabledChanged ->
                _state.update { it.copy(morningLockEnabled = event.enabled) }
            is SettingsUiEvent.MorningWakeTimeChanged ->
                _state.update {
                    it.copy(
                        morningWakeHour = event.hour.coerceIn(0, 23),
                        morningWakeMinute = event.minute.coerceIn(0, 59),
                    )
                }
            SettingsUiEvent.SaveMorningLockSettings -> saveMorningLockSettings()
            SettingsUiEvent.RefreshMorningLockStatus -> refreshMorningLockStatus()
            is SettingsUiEvent.MorningAlarmSoundSelected -> onMorningAlarmSoundSelected(event.uri)
            is SettingsUiEvent.MorningAlarmVolumeRampChanged ->
                _state.update {
                    it.copy(
                        morningAlarmVolumeRampSeconds = event.seconds.coerceIn(
                            0,
                            SettingsRepository.MAX_MORNING_ALARM_VOLUME_RAMP_SECONDS,
                        ),
                    )
                }
        }
    }

    private fun onMorningAlarmSoundSelected(uri: android.net.Uri?) {
        if (uri == null) {
            settingsRepository.setMorningAlarmSoundUri(null)
            settingsRepository.setMorningAlarmSoundLabel(null)
        } else {
            settingsRepository.setMorningAlarmSoundUri(uri.toString())
            val title = android.media.RingtoneManager.getRingtone(context, uri)?.getTitle(context)
                ?: "Custom sound"
            settingsRepository.setMorningAlarmSoundLabel(title)
        }
        refreshMorningLockStatus()
        _state.update {
            it.copy(
                message = "Alarm sound updated.",
                morningAlarmSoundUri = settingsRepository.getMorningAlarmSoundUri(),
            )
        }
    }

    private fun refreshMorningLockStatus() {
        _state.update {
            it.copy(
                morningHasOverlay = MorningLockPermissions.canDrawOverlays(context),
                morningHasNotifications = MorningLockPermissions.hasPostNotifications(context),
                morningHasExactAlarms = MorningLockPermissions.canScheduleExactAlarms(context),
                morningHasFullScreenIntent = MorningLockPermissions.canUseFullScreenIntent(context),
                morningNfcEnrolled = settingsRepository.hasNfcTagEnrolled(),
                morningSetupCompleted = settingsRepository.isMorningSetupCompleted(),
                morningAlarmSoundLabel = settingsRepository.getMorningAlarmSoundLabel()
                    ?: if (settingsRepository.getMorningAlarmSoundUri() != null) {
                        "Custom sound"
                    } else {
                        "System default"
                    },
                morningAlarmSoundUri = settingsRepository.getMorningAlarmSoundUri(),
                morningAlarmVolumeRampSeconds = settingsRepository.getMorningAlarmVolumeRampSeconds(),
            )
        }
    }

    private fun saveMorningLockSettings() {
        settingsRepository.setMorningLockEnabled(_state.value.morningLockEnabled)
        settingsRepository.setMorningWakeTime(_state.value.morningWakeHour, _state.value.morningWakeMinute)
        settingsRepository.setMorningAlarmVolumeRampSeconds(_state.value.morningAlarmVolumeRampSeconds)
        MorningAlarmScheduler.schedule(context)
        _state.update { it.copy(message = "Morning lock settings saved.") }
    }

    private fun savePenaltySettings() {
        settingsRepository.setPenaltyEnabled(_state.value.penaltyEnabled)
        val amount = _state.value.penaltyAmountInput.toDoubleOrNull()
            ?: _state.value.penaltyAmount
        settingsRepository.setPenaltyAmount(amount)
        _state.update {
            it.copy(
                penaltyAmount = amount,
                penaltyAmountInput = amount.toString(),
                message = "Penalty settings saved.",
            )
        }
    }

    private fun saveScheduleDefaults() {
        val zone = _state.value.timeZoneId
        if (!_state.value.useDeviceTimezone && runCatching { ZoneId.of(zone) }.isFailure) {
            _state.update { it.copy(message = "Invalid timezone. Use e.g. America/New_York") }
            return
        }
        if (!_state.value.useDeviceTimezone) {
            settingsRepository.setTimeZoneId(zone)
        }
        settingsRepository.setUseDeviceTimezone(_state.value.useDeviceTimezone)
        settingsRepository.setEveningReviewTime(_state.value.eveningHour, _state.value.eveningMinute)
        settingsRepository.setBlockDurationMinutes(_state.value.blockDurationMinutes)
        settingsRepository.setDefaultTaskStartTime(
            _state.value.defaultTaskStartHour,
            _state.value.defaultTaskStartMinute,
        )
        EveningReviewScheduler.schedule(context)
        _state.update { it.copy(message = "Schedule defaults saved.") }
    }

    fun filteredTimeZones(): List<String> {
        val query = _state.value.timeZoneSearch.trim().lowercase()
        val base = SettingsRepository.SUGGESTED_TIME_ZONES +
            ZoneId.getAvailableZoneIds().sorted().take(200)
        val distinct = base.distinct()
        return if (query.isEmpty()) {
            distinct.take(24)
        } else {
            distinct.filter { it.lowercase().contains(query) }.take(30)
        }
    }
}
