package com.stella.feature.morning

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.stella.sync.MorningAlarmScheduler
import com.stella.sync.MorningLockEntryPoint
import dagger.hilt.android.EntryPointAccessors
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

object MorningAlarmTrigger {
    private val mainHandler = Handler(Looper.getMainLooper())

    fun fire(context: Context, isTest: Boolean, onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        val appContext = context.applicationContext
        if (Looper.myLooper() == Looper.getMainLooper()) {
            deliverResult(onComplete, runCatching { executeFire(context, appContext, isTest) })
        } else {
            mainHandler.post {
                deliverResult(onComplete, runCatching { executeFire(context, appContext, isTest) })
            }
        }
    }

    fun fireSync(context: Context, isTest: Boolean) {
        val latch = CountDownLatch(1)
        var success = false
        var error: String? = null
        fire(context, isTest) { ok, msg ->
            success = ok
            error = msg
            latch.countDown()
        }
        if (!latch.await(10, TimeUnit.SECONDS)) {
            throw IllegalStateException("Morning alarm timed out")
        }
        if (!success) {
            throw IllegalStateException(error ?: "Morning alarm failed")
        }
    }

    private fun deliverResult(
        onComplete: (Boolean, String?) -> Unit,
        result: Result<Unit>,
    ) {
        result.fold(
            onSuccess = { onComplete(true, null) },
            onFailure = { onComplete(false, it.message ?: "Failed to start morning alarm") },
        )
    }

    private fun executeFire(callerContext: Context, appContext: Context, isTest: Boolean) {
        val entryPoint = EntryPointAccessors.fromApplication(appContext, MorningLockEntryPoint::class.java)
        val settings = entryPoint.settingsRepository()
        val controller = entryPoint.morningLockController()

        if (!isTest && !settings.isMorningLockEnabled()) return
        if (!isTest) {
            val shouldEnforce = runBlocking(Dispatchers.IO) { controller.shouldEnforce() }
            if (!shouldEnforce) {
                MorningAlarmScheduler.schedule(appContext)
                return
            }
        }

        val launchIntent = Intent(appContext, MorningLockActivity::class.java).apply {
            if (callerContext !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(MorningLockActivity.EXTRA_FROM_ALARM, true)
            putExtra(MorningLockActivity.EXTRA_IS_TEST_ALARM, isTest)
        }
        callerContext.startActivity(launchIntent)
    }
}
