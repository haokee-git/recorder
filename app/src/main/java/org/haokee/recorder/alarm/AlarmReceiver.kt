package org.haokee.recorder.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import org.haokee.recorder.MainActivity
import org.haokee.recorder.R

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val thoughtId = intent.getStringExtra("thought_id") ?: return
        val thoughtTitle = intent.getStringExtra("thought_title") ?: "感言提醒"

        showNotification(context, thoughtTitle, thoughtId)
    }

    private fun showNotification(context: Context, title: String, thoughtId: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "感言提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "感言闹钟提醒"
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent to open app
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("thought_id", thoughtId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            thoughtId.hashCode(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // Use system alarm icon
            .setContentTitle(title)
            .setContentText("点击查看您的感言")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Sound, vibration, lights
            .setVibrate(longArrayOf(0, 500, 200, 500)) // Vibration pattern
            .setFullScreenIntent(pendingIntent, true) // Show as full screen on lock screen
            .build()

        notificationManager.notify(thoughtId.hashCode(), notification)
    }

    companion object {
        private const val CHANNEL_ID = "thought_alarm_channel"
    }
}
