package com.stella.core.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.stella.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "stella_settings",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun getApiBaseUrl(): String =
        prefs.getString(KEY_API_URL, BuildConfig.API_BASE_URL) ?: BuildConfig.API_BASE_URL

    fun setApiBaseUrl(url: String) {
        prefs.edit().putString(KEY_API_URL, url.trimEnd('/')).apply()
    }

    fun getApiKey(): String = prefs.getString(KEY_API_KEY, "") ?: ""

    fun setApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key.trim()).apply()
    }

    fun getNfcTagId(): String? = prefs.getString(KEY_NFC_TAG, null)?.takeIf { it.isNotBlank() }

    fun setNfcTagId(tagId: String) {
        prefs.edit().putString(KEY_NFC_TAG, tagId).apply()
    }

    fun clearNfcTagId() {
        prefs.edit().remove(KEY_NFC_TAG).apply()
    }

    fun hasNfcTagEnrolled(): Boolean = getNfcTagId() != null

    fun getUseDeviceTimezone(): Boolean =
        prefs.getBoolean(KEY_USE_DEVICE_TIMEZONE, true)

    fun setUseDeviceTimezone(useDevice: Boolean) {
        prefs.edit().putBoolean(KEY_USE_DEVICE_TIMEZONE, useDevice).apply()
        if (useDevice) {
            setTimeZoneId(ZoneId.systemDefault().id)
        }
    }

    fun getTimeZoneId(): String =
        prefs.getString(KEY_TIME_ZONE, ZoneId.systemDefault().id) ?: ZoneId.systemDefault().id

    fun effectiveTimeZoneId(): String =
        if (getUseDeviceTimezone()) ZoneId.systemDefault().id else getTimeZoneId()

    fun setTimeZoneId(zoneId: String) {
        val normalized = zoneId.trim().ifBlank { ZoneId.systemDefault().id }
        runCatching { ZoneId.of(normalized) }.getOrNull()
            ?: return
        prefs.edit().putString(KEY_TIME_ZONE, normalized).apply()
    }

    fun getEveningReviewHour(): Int = prefs.getInt(KEY_EVENING_HOUR, 20)

    fun getEveningReviewMinute(): Int = prefs.getInt(KEY_EVENING_MINUTE, 30)

    fun setEveningReviewTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_EVENING_HOUR, hour.coerceIn(0, 23))
            .putInt(KEY_EVENING_MINUTE, minute.coerceIn(0, 59))
            .apply()
    }

    fun getBlockDurationMinutes(): Int =
        prefs.getInt(KEY_BLOCK_DURATION, DEFAULT_BLOCK_DURATION).coerceIn(15, 480)

    fun setBlockDurationMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_BLOCK_DURATION, minutes.coerceIn(15, 480)).apply()
    }

    fun getDefaultTaskStartHour(): Int =
        prefs.getInt(KEY_DEFAULT_TASK_HOUR, DEFAULT_TASK_START_HOUR).coerceIn(0, 23)

    fun getDefaultTaskStartMinute(): Int =
        prefs.getInt(KEY_DEFAULT_TASK_MINUTE, DEFAULT_TASK_START_MINUTE).coerceIn(0, 59)

    fun setDefaultTaskStartTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_DEFAULT_TASK_HOUR, hour.coerceIn(0, 23))
            .putInt(KEY_DEFAULT_TASK_MINUTE, minute.coerceIn(0, 59))
            .apply()
    }

    fun getDefaultTaskSchedule(): BlockDefaults =
        BlockDefaults(
            hour = getDefaultTaskStartHour(),
            minute = getDefaultTaskStartMinute(),
            durationMinutes = getBlockDurationMinutes(),
        )

    fun isPenaltyEnabled(): Boolean = prefs.getBoolean(KEY_PENALTY_ENABLED, false)

    fun setPenaltyEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PENALTY_ENABLED, enabled).apply()
    }

    fun getPenaltyAmount(): Double =
        prefs.getFloat(KEY_PENALTY_AMOUNT, DEFAULT_PENALTY_AMOUNT.toFloat()).toDouble()

    fun setPenaltyAmount(amount: Double) {
        prefs.edit().putFloat(KEY_PENALTY_AMOUNT, amount.coerceAtLeast(0.0).toFloat()).apply()
    }

    fun isMorningLockEnabled(): Boolean = prefs.getBoolean(KEY_MORNING_LOCK_ENABLED, true)

    fun setMorningLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MORNING_LOCK_ENABLED, enabled).apply()
    }

    fun isMorningSetupCompleted(): Boolean = prefs.getBoolean(KEY_MORNING_SETUP_COMPLETED, false)

    fun setMorningSetupCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_MORNING_SETUP_COMPLETED, completed).apply()
    }

    fun getMorningWakeHour(): Int = prefs.getInt(KEY_MORNING_WAKE_HOUR, DEFAULT_MORNING_WAKE_HOUR)

    fun getMorningWakeMinute(): Int = prefs.getInt(KEY_MORNING_WAKE_MINUTE, DEFAULT_MORNING_WAKE_MINUTE)

    fun setMorningWakeTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_MORNING_WAKE_HOUR, hour.coerceIn(0, 23))
            .putInt(KEY_MORNING_WAKE_MINUTE, minute.coerceIn(0, 59))
            .apply()
    }

    fun getMorningAlarmSoundUri(): String? =
        prefs.getString(KEY_MORNING_ALARM_SOUND, null)?.takeIf { it.isNotBlank() }

    fun setMorningAlarmSoundUri(uri: String?) {
        if (uri.isNullOrBlank()) {
            prefs.edit().remove(KEY_MORNING_ALARM_SOUND).apply()
        } else {
            prefs.edit().putString(KEY_MORNING_ALARM_SOUND, uri).apply()
        }
    }

    fun getMorningAlarmSoundLabel(): String? =
        prefs.getString(KEY_MORNING_ALARM_SOUND_LABEL, null)?.takeIf { it.isNotBlank() }

    fun setMorningAlarmSoundLabel(label: String?) {
        if (label.isNullOrBlank()) {
            prefs.edit().remove(KEY_MORNING_ALARM_SOUND_LABEL).apply()
        } else {
            prefs.edit().putString(KEY_MORNING_ALARM_SOUND_LABEL, label).apply()
        }
    }

    fun getMorningAlarmVolumeRampSeconds(): Int =
        prefs.getInt(KEY_MORNING_ALARM_VOLUME_RAMP_SECONDS, DEFAULT_MORNING_ALARM_VOLUME_RAMP_SECONDS)
            .coerceIn(0, MAX_MORNING_ALARM_VOLUME_RAMP_SECONDS)

    fun setMorningAlarmVolumeRampSeconds(seconds: Int) {
        prefs.edit()
            .putInt(
                KEY_MORNING_ALARM_VOLUME_RAMP_SECONDS,
                seconds.coerceIn(0, MAX_MORNING_ALARM_VOLUME_RAMP_SECONDS),
            )
            .apply()
    }

    fun getMorningSetupStep(): String? =
        prefs.getString(KEY_MORNING_SETUP_STEP, null)?.takeIf { it.isNotBlank() }

    fun setMorningSetupStep(step: String) {
        prefs.edit().putString(KEY_MORNING_SETUP_STEP, step).apply()
    }

    fun clearMorningSetupStep() {
        prefs.edit().remove(KEY_MORNING_SETUP_STEP).apply()
    }

    data class BlockDefaults(
        val hour: Int,
        val minute: Int,
        val durationMinutes: Int,
    )

    companion object {
        private const val KEY_API_URL = "api_base_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_NFC_TAG = "nfc_tag_id"
        private const val KEY_TIME_ZONE = "time_zone_id"
        private const val KEY_USE_DEVICE_TIMEZONE = "use_device_timezone"
        private const val KEY_EVENING_HOUR = "evening_review_hour"
        private const val KEY_EVENING_MINUTE = "evening_review_minute"
        private const val KEY_BLOCK_DURATION = "block_duration_minutes"
        private const val KEY_DEFAULT_TASK_HOUR = "default_task_start_hour"
        private const val KEY_DEFAULT_TASK_MINUTE = "default_task_start_minute"
        const val DEFAULT_BLOCK_DURATION = 60
        const val DEFAULT_TASK_START_HOUR = 9
        const val DEFAULT_TASK_START_MINUTE = 0
        const val DEFAULT_PENALTY_AMOUNT = 5.0

        private const val KEY_PENALTY_ENABLED = "penalty_enabled"
        private const val KEY_PENALTY_AMOUNT = "penalty_amount"
        private const val KEY_MORNING_LOCK_ENABLED = "morning_lock_enabled"
        private const val KEY_MORNING_SETUP_COMPLETED = "morning_setup_completed"
        private const val KEY_MORNING_WAKE_HOUR = "morning_wake_hour"
        private const val KEY_MORNING_WAKE_MINUTE = "morning_wake_minute"
        private const val KEY_MORNING_ALARM_SOUND = "morning_alarm_sound_uri"
        private const val KEY_MORNING_ALARM_SOUND_LABEL = "morning_alarm_sound_label"
        private const val KEY_MORNING_ALARM_VOLUME_RAMP_SECONDS = "morning_alarm_volume_ramp_seconds"
        private const val KEY_MORNING_SETUP_STEP = "morning_setup_step"
        const val DEFAULT_MORNING_WAKE_HOUR = 6
        const val DEFAULT_MORNING_WAKE_MINUTE = 30
        const val DEFAULT_MORNING_ALARM_VOLUME_RAMP_SECONDS = 120
        const val MAX_MORNING_ALARM_VOLUME_RAMP_SECONDS = 600
        const val DEBUG_NFC_TAG = "debug-bathroom-tag"

        val SUGGESTED_TIME_ZONES: List<String> = listOf(
            "UTC",
            "America/New_York",
            "America/Chicago",
            "America/Denver",
            "America/Los_Angeles",
            "Europe/London",
            "Europe/Paris",
            "Europe/Berlin",
            "Asia/Tokyo",
            "Asia/Singapore",
            "Australia/Sydney",
        )
    }
}

