package com.stella.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.stella.app.MainActivity

object DiagnosticsNotifier {
    private const val CHANNEL_ID = "stella_diagnostics"

    fun showTestNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Diagnostics",
                    NotificationManager.IMPORTANCE_HIGH,
                ),
            )
        }
        manager.notify(
            9001,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Stella Diagnostics")
                .setContentText("Test notification fired successfully.")
                .setAutoCancel(true)
                .build(),
        )
    }
}
