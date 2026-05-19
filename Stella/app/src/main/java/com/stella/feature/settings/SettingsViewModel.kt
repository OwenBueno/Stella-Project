package com.stella.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stella.BuildConfig
import com.stella.core.data.SettingsRepository
import com.stella.core.data.SyncRepository
import com.stella.sync.EveningReviewScheduler
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
    val syncMessage: String? = null,
    val isSyncing: Boolean = false,
    val timeZoneId: String = ZoneId.systemDefault().id,
    val eveningHour: String = "20",
    val eveningMinute: String = "30",
    val blockDurationMinutes: String = "60",
    val block1Hour: String = "9",
    val block1Minute: String = "0",
    val block2Hour: String = "12",
    val block2Minute: String = "0",
    val block3Hour: String = "15",
    val block3Minute: String = "0",
    val nfcTagId: String? = null,
)

sealed interface SettingsUiEvent {
    data class ApiUrlChanged(val url: String) : SettingsUiEvent
    data class ApiKeyChanged(val key: String) : SettingsUiEvent
    data object SaveCredentials : SettingsUiEvent
    data object SyncNow : SettingsUiEvent
    data object PurgeLocal : SettingsUiEvent
    data class TimeZoneChanged(val value: String) : SettingsUiEvent
    data object UseDeviceTimeZone : SettingsUiEvent
    data class EveningHourChanged(val value: String) : SettingsUiEvent
    data class EveningMinuteChanged(val value: String) : SettingsUiEvent
    data class BlockDurationChanged(val value: String) : SettingsUiEvent
    data class Block1HourChanged(val value: String) : SettingsUiEvent
    data class Block1MinuteChanged(val value: String) : SettingsUiEvent
    data class Block2HourChanged(val value: String) : SettingsUiEvent
    data class Block2MinuteChanged(val value: String) : SettingsUiEvent
    data class Block3HourChanged(val value: String) : SettingsUiEvent
    data class Block3MinuteChanged(val value: String) : SettingsUiEvent
    data object SaveTimeAndBlocks : SettingsUiEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val syncRepository: SyncRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(loadFromRepository())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private fun loadFromRepository(): SettingsUiState = SettingsUiState(
        apiUrl = settingsRepository.getApiBaseUrl(),
        apiKey = settingsRepository.getApiKey(),
        timeZoneId = settingsRepository.getTimeZoneId(),
        eveningHour = settingsRepository.getEveningReviewHour().toString(),
        eveningMinute = settingsRepository.getEveningReviewMinute().toString(),
        blockDurationMinutes = settingsRepository.getBlockDurationMinutes().toString(),
        block1Hour = settingsRepository.getDefaultBlockHour(0).toString(),
        block1Minute = settingsRepository.getDefaultBlockMinute(0).toString(),
        block2Hour = settingsRepository.getDefaultBlockHour(1).toString(),
        block2Minute = settingsRepository.getDefaultBlockMinute(1).toString(),
        block3Hour = settingsRepository.getDefaultBlockHour(2).toString(),
        block3Minute = settingsRepository.getDefaultBlockMinute(2).toString(),
        nfcTagId = settingsRepository.getNfcTagId(),
    )

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.ApiUrlChanged -> _state.update { it.copy(apiUrl = event.url) }
            is SettingsUiEvent.ApiKeyChanged -> _state.update { it.copy(apiKey = event.key) }
            SettingsUiEvent.SaveCredentials -> {
                settingsRepository.setApiBaseUrl(_state.value.apiUrl)
                settingsRepository.setApiKey(_state.value.apiKey)
                _state.update { it.copy(syncMessage = "Credentials saved.") }
            }
            SettingsUiEvent.SyncNow -> viewModelScope.launch {
                _state.update { it.copy(isSyncing = true, syncMessage = null) }
                syncRepository.syncNow()
                    .onSuccess { msg -> _state.update { it.copy(isSyncing = false, syncMessage = msg) } }
                    .onFailure { e -> _state.update { it.copy(isSyncing = false, syncMessage = e.message) } }
            }
            SettingsUiEvent.PurgeLocal -> viewModelScope.launch {
                syncRepository.purgeLocal()
                _state.update { it.copy(syncMessage = "Local data purged") }
            }
            is SettingsUiEvent.TimeZoneChanged ->
                _state.update { it.copy(timeZoneId = event.value.trim()) }
            SettingsUiEvent.UseDeviceTimeZone ->
                _state.update { it.copy(timeZoneId = ZoneId.systemDefault().id) }
            is SettingsUiEvent.EveningHourChanged ->
                _state.update { it.copy(eveningHour = digitsOnly(event.value, 2)) }
            is SettingsUiEvent.EveningMinuteChanged ->
                _state.update { it.copy(eveningMinute = digitsOnly(event.value, 2)) }
            is SettingsUiEvent.BlockDurationChanged ->
                _state.update { it.copy(blockDurationMinutes = digitsOnly(event.value, 3)) }
            is SettingsUiEvent.Block1HourChanged ->
                _state.update { it.copy(block1Hour = digitsOnly(event.value, 2)) }
            is SettingsUiEvent.Block1MinuteChanged ->
                _state.update { it.copy(block1Minute = digitsOnly(event.value, 2)) }
            is SettingsUiEvent.Block2HourChanged ->
                _state.update { it.copy(block2Hour = digitsOnly(event.value, 2)) }
            is SettingsUiEvent.Block2MinuteChanged ->
                _state.update { it.copy(block2Minute = digitsOnly(event.value, 2)) }
            is SettingsUiEvent.Block3HourChanged ->
                _state.update { it.copy(block3Hour = digitsOnly(event.value, 2)) }
            is SettingsUiEvent.Block3MinuteChanged ->
                _state.update { it.copy(block3Minute = digitsOnly(event.value, 2)) }
            SettingsUiEvent.SaveTimeAndBlocks -> saveTimeAndBlocks()
        }
    }

    private fun saveTimeAndBlocks() {
        val zone = _state.value.timeZoneId
        if (runCatching { ZoneId.of(zone) }.isFailure) {
            _state.update { it.copy(syncMessage = "Invalid timezone. Use e.g. America/New_York") }
            return
        }
        settingsRepository.setTimeZoneId(zone)
        settingsRepository.setEveningReviewTime(
            _state.value.eveningHour.toIntOrNull() ?: 20,
            _state.value.eveningMinute.toIntOrNull() ?: 30,
        )
        settingsRepository.setBlockDurationMinutes(
            _state.value.blockDurationMinutes.toIntOrNull() ?: SettingsRepository.DEFAULT_BLOCK_DURATION,
        )
        settingsRepository.setDefaultBlockTime(
            0,
            _state.value.block1Hour.toIntOrNull() ?: 9,
            _state.value.block1Minute.toIntOrNull() ?: 0,
        )
        settingsRepository.setDefaultBlockTime(
            1,
            _state.value.block2Hour.toIntOrNull() ?: 12,
            _state.value.block2Minute.toIntOrNull() ?: 0,
        )
        settingsRepository.setDefaultBlockTime(
            2,
            _state.value.block3Hour.toIntOrNull() ?: 15,
            _state.value.block3Minute.toIntOrNull() ?: 0,
        )
        EveningReviewScheduler.schedule(context)
        _state.update { it.copy(syncMessage = "Time, blocks, and evening reminder saved.") }
    }

    private fun digitsOnly(value: String, maxLen: Int): String =
        value.filter { it.isDigit() }.take(maxLen)
}
