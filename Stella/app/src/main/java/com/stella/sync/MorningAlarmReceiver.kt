package com.stella.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.stella.feature.morning.MorningAlarmTrigger

class MorningAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pending = goAsync()
        val isTest = intent?.action == MorningAlarmScheduler.ACTION_MORNING_ALARM_TEST
        try {
            MorningAlarmTrigger.fireSync(context, isTest)
        } catch (e: Exception) {
            android.util.Log.e("MorningAlarmReceiver", "Morning alarm failed", e)
        } finally {
            pending.finish()
        }
    }
}
