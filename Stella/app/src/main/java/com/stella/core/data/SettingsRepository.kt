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

    fun getTimeZoneId(): String =
        prefs.getString(KEY_TIME_ZONE, ZoneId.systemDefault().id) ?: ZoneId.systemDefault().id

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

    fun getDefaultBlockHour(slotIndex: Int): Int =
        prefs.getInt(slotHourKey(slotIndex), defaultHourForSlot(slotIndex)).coerceIn(0, 23)

    fun getDefaultBlockMinute(slotIndex: Int): Int =
        prefs.getInt(slotMinuteKey(slotIndex), 0).coerceIn(0, 59)

    fun setDefaultBlockTime(slotIndex: Int, hour: Int, minute: Int) {
        prefs.edit()
            .putInt(slotHourKey(slotIndex), hour.coerceIn(0, 23))
            .putInt(slotMinuteKey(slotIndex), minute.coerceIn(0, 59))
            .apply()
    }

    fun getBlockScheduleForSlot(slotIndex: Int): BlockDefaults =
        BlockDefaults(
            hour = getDefaultBlockHour(slotIndex),
            minute = getDefaultBlockMinute(slotIndex),
            durationMinutes = getBlockDurationMinutes(),
        )

    private fun slotHourKey(index: Int) = when (index) {
        0 -> KEY_BLOCK1_HOUR
        1 -> KEY_BLOCK2_HOUR
        else -> KEY_BLOCK3_HOUR
    }

    private fun slotMinuteKey(index: Int) = when (index) {
        0 -> KEY_BLOCK1_MINUTE
        1 -> KEY_BLOCK2_MINUTE
        else -> KEY_BLOCK3_MINUTE
    }

    private fun defaultHourForSlot(index: Int): Int = when (index) {
        0 -> 9
        1 -> 12
        else -> 15
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
        private const val KEY_EVENING_HOUR = "evening_review_hour"
        private const val KEY_EVENING_MINUTE = "evening_review_minute"
        private const val KEY_BLOCK_DURATION = "block_duration_minutes"
        private const val KEY_BLOCK1_HOUR = "block1_hour"
        private const val KEY_BLOCK1_MINUTE = "block1_minute"
        private const val KEY_BLOCK2_HOUR = "block2_hour"
        private const val KEY_BLOCK2_MINUTE = "block2_minute"
        private const val KEY_BLOCK3_HOUR = "block3_hour"
        private const val KEY_BLOCK3_MINUTE = "block3_minute"
        const val DEFAULT_BLOCK_DURATION = 60
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
