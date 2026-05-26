package com.stella.feature.morning

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.stella.core.data.SettingsRepository
import com.stella.sync.MorningAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class MorningSetupStep {
    WHY,
    NFC,
    NOTIFICATIONS,
    OVERLAY,
    EXACT_ALARMS,
    BATTERY,
    ALARM_SOUND,
    TEST,
    DONE,
}

data class MorningSetupUiState(
    val step: MorningSetupStep = MorningSetupStep.WHY,
    val nfcEnrolled: Boolean = false,
    val hasNotifications: Boolean = false,
    val hasFullScreenIntent: Boolean = false,
    val hasOverlay: Boolean = false,
    val hasExactAlarms: Boolean = false,
    val batteryOptimized: Boolean = false,
    val alarmSoundLabel: String = "System default",
    val alarmSoundUri: String? = null,
    val hasAlarmSound: Boolean = true,
    val testScheduled: Boolean = false,
    val testMessage: String? = null,
    val isTesting: Boolean = false,
    val criticalPermissionsReady: Boolean = false,
)

sealed interface MorningSetupUiEvent {
    data object Next : MorningSetupUiEvent
    data object Back : MorningSetupUiEvent
    data object RefreshPermissions : MorningSetupUiEvent
    data object ScheduleTestAlarm : MorningSetupUiEvent
    data object TestAlarmNow : MorningSetupUiEvent
    data class AlarmSoundSelected(val uri: Uri?) : MorningSetupUiEvent
    data object CompleteSetup : MorningSetupUiEvent
}

@HiltViewModel
class MorningLockSetupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MorningSetupUiState())
    val state: StateFlow<MorningSetupUiState> = _state.asStateFlow()

    init {
        refreshPermissions()
    }

    fun initializeStep(intentStepName: String?) {
        val step = parseStep(intentStepName)
            ?: settingsRepository.getMorningSetupStep()?.let(::parseStep)
            ?: MorningSetupStep.WHY
        _state.update { it.copy(step = step) }
        settingsRepository.setMorningSetupStep(step.name)
    }

    fun onEvent(event: MorningSetupUiEvent) {
        when (event) {
            MorningSetupUiEvent.Next -> advance()
            MorningSetupUiEvent.Back -> retreat()
            MorningSetupUiEvent.RefreshPermissions -> refreshPermissions()
            MorningSetupUiEvent.ScheduleTestAlarm -> scheduleTest(delaySeconds = 10)
            MorningSetupUiEvent.TestAlarmNow -> triggerTestNow()
            is MorningSetupUiEvent.AlarmSoundSelected -> onAlarmSoundSelected(event.uri)
            MorningSetupUiEvent.CompleteSetup -> {
                settingsRepository.setMorningSetupCompleted(true)
                settingsRepository.clearMorningSetupStep()
                MorningAlarmScheduler.schedule(context)
            }
        }
    }

    fun onAlarmSoundSelected(uri: Uri?) {
        if (uri == null) {
            settingsRepository.setMorningAlarmSoundUri(null)
            settingsRepository.setMorningAlarmSoundLabel(null)
        } else {
            settingsRepository.setMorningAlarmSoundUri(uri.toString())
            val title = RingtoneManager.getRingtone(context, uri)?.getTitle(context) ?: "Custom sound"
            settingsRepository.setMorningAlarmSoundLabel(title)
        }
        refreshPermissions()
    }

    private fun scheduleTest(delaySeconds: Int) {
        MorningLockPermissions.testBlockedMessage(context)?.let { blocked ->
            _state.update { it.copy(testScheduled = false, testMessage = blocked) }
            return
        }
        val result = MorningAlarmScheduler.scheduleTest(context, delaySeconds)
        _state.update {
            it.copy(
                testScheduled = result.success,
                testMessage = if (result.success) {
                    "Test alarm in $delaySeconds seconds. You can leave this screen — the lock will appear on top."
                } else {
                    result.error
                },
            )
        }
    }

    private fun triggerTestNow() {
        MorningLockPermissions.testBlockedMessage(context)?.let { blocked ->
            _state.update { it.copy(testMessage = blocked) }
            return
        }
        _state.update { it.copy(isTesting = true, testMessage = null) }
        MorningAlarmTrigger.fire(context, isTest = true) { success, error ->
            _state.update {
                it.copy(
                    isTesting = false,
                    testMessage = if (success) {
                        "Morning lock launched. Complete the flow or use debug skip."
                    } else {
                        error ?: "Could not start test alarm."
                    },
                )
            }
        }
    }

    fun refreshPermissions() {
        val uri = settingsRepository.getMorningAlarmSoundUri()
        val label = settingsRepository.getMorningAlarmSoundLabel()
            ?: if (uri != null) "Custom sound" else "System default"
        _state.update {
            it.copy(
                alarmSoundUri = uri,
                alarmSoundLabel = label,
                nfcEnrolled = settingsRepository.hasNfcTagEnrolled(),
                hasNotifications = MorningLockPermissions.hasPostNotifications(context),
                hasFullScreenIntent = MorningLockPermissions.canUseFullScreenIntent(context),
                hasOverlay = MorningLockPermissions.canDrawOverlays(context),
                hasExactAlarms = MorningLockPermissions.canScheduleExactAlarms(context),
                batteryOptimized = MorningLockPermissions.isIgnoringBatteryOptimizations(context),
                hasAlarmSound = true,
                criticalPermissionsReady = MorningLockPermissions.allCriticalGranted(context),
            )
        }
    }

    fun canAdvanceFromCurrentStep(): Boolean = when (_state.value.step) {
        MorningSetupStep.WHY -> true
        MorningSetupStep.NFC -> _state.value.nfcEnrolled
        MorningSetupStep.NOTIFICATIONS ->
            _state.value.hasNotifications && _state.value.hasFullScreenIntent
        MorningSetupStep.OVERLAY -> _state.value.hasOverlay
        MorningSetupStep.EXACT_ALARMS -> _state.value.hasExactAlarms
        MorningSetupStep.BATTERY -> true
        MorningSetupStep.ALARM_SOUND -> _state.value.hasAlarmSound
        MorningSetupStep.TEST -> _state.value.criticalPermissionsReady
        MorningSetupStep.DONE -> true
    }

    private fun advance() {
        val steps = MorningSetupStep.entries
        val index = steps.indexOf(_state.value.step)
        if (index < steps.lastIndex) {
            setStep(steps[index + 1], clearTestMessage = true)
            refreshPermissions()
        }
    }

    private fun retreat() {
        val steps = MorningSetupStep.entries
        val index = steps.indexOf(_state.value.step)
        if (index > 0) {
            setStep(steps[index - 1], clearTestMessage = true)
        }
    }

    private fun setStep(step: MorningSetupStep, clearTestMessage: Boolean = false) {
        _state.update {
            it.copy(
                step = step,
                testMessage = if (clearTestMessage) null else it.testMessage,
            )
        }
        settingsRepository.setMorningSetupStep(step.name)
    }

    private fun parseStep(name: String?): MorningSetupStep? =
        name?.let { runCatching { MorningSetupStep.valueOf(it) }.getOrNull() }
}
