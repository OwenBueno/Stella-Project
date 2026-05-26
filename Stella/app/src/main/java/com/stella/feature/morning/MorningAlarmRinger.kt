package com.stella.feature.morning

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import com.stella.sync.MorningLockEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

object MorningAlarmRinger {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    private var mediaPlayer: MediaPlayer? = null

    @Volatile
    private var volumeRampJob: Job? = null

    @Volatile
    private var alarmVolumeBeforeBoost: Int? = null

    @Volatile
    private var appContextForRestore: Context? = null

    fun start(context: Context) {
        stop()
        val appContext = context.applicationContext
        appContextForRestore = appContext
        scope.launch {
            val settings = EntryPointAccessors.fromApplication(
                appContext,
                MorningLockEntryPoint::class.java,
            ).settingsRepository()
            val uri = settings.resolveMorningAlarmSoundUri(appContext)
            val rampSeconds = settings.getMorningAlarmVolumeRampSeconds()
            val rampMs = rampSeconds * 1000L

            val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxStreamVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            alarmVolumeBeforeBoost = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val player = withContext(Dispatchers.IO) {
                runCatching {
                    MediaPlayer().apply {
                        setAudioAttributes(attributes)
                        setDataSource(appContext, uri)
                        isLooping = true
                        prepare()
                    }
                }.getOrNull()
            } ?: return@launch

            mediaPlayer = player
            player.setVolume(1f, 1f)

            if (rampMs <= 0L) {
                setAlarmStreamVolume(audioManager, maxStreamVolume)
            } else {
                setAlarmStreamVolume(audioManager, 0)
            }
            runCatching { player.start() }

            if (rampMs > 0L) {
                startStreamVolumeRamp(audioManager, maxStreamVolume, rampMs)
            }
        }
    }

    private fun startStreamVolumeRamp(
        audioManager: AudioManager,
        maxStreamVolume: Int,
        rampMs: Long,
    ) {
        volumeRampJob?.cancel()
        val startedAt = System.currentTimeMillis()
        volumeRampJob = scope.launch {
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startedAt
                val progress = (elapsed.toFloat() / rampMs).coerceIn(0f, 1f)
                val level = (maxStreamVolume * progress).roundToInt().coerceIn(0, maxStreamVolume)
                setAlarmStreamVolume(audioManager, level)
                if (progress >= 1f) break
                delay(VOLUME_RAMP_TICK_MS)
            }
            setAlarmStreamVolume(audioManager, maxStreamVolume)
        }
    }

    private fun setAlarmStreamVolume(audioManager: AudioManager, level: Int) {
        runCatching {
            audioManager.setStreamVolume(
                AudioManager.STREAM_ALARM,
                level,
                0,
            )
        }
    }

    fun stop() {
        volumeRampJob?.cancel()
        volumeRampJob = null
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
        mediaPlayer = null
        restoreAlarmStreamVolume()
    }

    private fun restoreAlarmStreamVolume() {
        val previous = alarmVolumeBeforeBoost ?: return
        val context = appContextForRestore ?: return
        alarmVolumeBeforeBoost = null
        appContextForRestore = null
        runCatching {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            setAlarmStreamVolume(audioManager, previous)
        }
    }

    private const val VOLUME_RAMP_TICK_MS = 200L
}
