package com.stella.feature.morning

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.content.ContextCompat
import com.stella.sync.MorningAlarmNotifications
import com.stella.sync.MorningLockEntryPoint
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import javax.inject.Inject

@AndroidEntryPoint
class MorningLockEnforcementService : Service() {
    @Inject lateinit var morningLockController: MorningLockController

    private val handler = Handler(Looper.getMainLooper())
    private var lastRelaunchAt = 0L

    private val enforcementLoop = object : Runnable {
        override fun run() {
            if (!morningLockController.isEnforcingNow()) {
                stopSelf()
                return
            }
            if (!morningLockController.isStellaMorningFlowInForeground()) {
                val now = System.currentTimeMillis()
                if (now - lastRelaunchAt >= RELAUNCH_THROTTLE_MS) {
                    lastRelaunchAt = now
                    relaunchMorningFlow()
                }
                if (morningLockController.canShowEnforcementOverlay() &&
                    MorningLockPermissions.canDrawOverlays(this@MorningLockEnforcementService)
                ) {
                    MorningLockOverlay.show(this@MorningLockEnforcementService)
                }
            }
            handler.postDelayed(this, LOOP_INTERVAL_MS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        MorningAlarmNotifications.ensureChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }
        val notification = MorningAlarmNotifications.buildEnforcementNotification(this)
        startForeground(MorningAlarmNotifications.NOTIFICATION_ENFORCEMENT_ID, notification)
        handler.removeCallbacks(enforcementLoop)
        handler.post(enforcementLoop)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(enforcementLoop)
        MorningLockOverlay.hide(this)
        super.onDestroy()
    }

    private fun relaunchMorningFlow() {
        val launch = Intent(this, MorningLockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        startActivity(launch)
    }

    companion object {
        private const val ACTION_START = "com.stella.morning.START_ENFORCEMENT"
        private const val ACTION_STOP = "com.stella.morning.STOP_ENFORCEMENT"
        private const val LOOP_INTERVAL_MS = 400L
        private const val RELAUNCH_THROTTLE_MS = 500L

        fun start(context: Context) {
            val intent = Intent(context, MorningLockEnforcementService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, MorningLockEnforcementService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun refreshEnforcement(context: Context) {
            val entryPoint = EntryPointAccessors.fromApplication(context, MorningLockEntryPoint::class.java)
            val controller = entryPoint.morningLockController()
            if (controller.isEnforcingNow()) {
                start(context)
            } else {
                stop(context)
            }
        }
    }
}
