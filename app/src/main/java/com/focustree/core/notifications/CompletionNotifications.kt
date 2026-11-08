package com.focustree.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.focustree.R

object CompletionNotifications {
    const val CHANNEL_ID = "focus_completion"

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Focus session", NotificationManager.IMPORTANCE_DEFAULT)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun show(
        context: Context,
        sessionId: String,
        intention: String,
    ) {
        if (!hasPermission(context)) return
        createChannel(context)
        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Focus session complete")
                .setContentText(intention.ifBlank { "Your focus time is complete." })
                .setAutoCancel(true)
                .build()
        try {
            NotificationManagerCompat.from(context).notify(sessionId.hashCode(), notification)
        } catch (_: SecurityException) {
            // Permission can be revoked between the permission check and notify().
        }
    }
}
