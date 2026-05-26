package com.stella.feature.morning

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import com.stella.core.data.SettingsRepository

fun SettingsRepository.resolveMorningAlarmSoundUri(context: Context): Uri {
    val stored = getMorningAlarmSoundUri()
    if (!stored.isNullOrBlank()) {
        return Uri.parse(stored)
    }
    return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ?: Settings.System.DEFAULT_ALARM_ALERT_URI
}
