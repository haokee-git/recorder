package org.haokee.recorder.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object NotificationHelper {
    private const val CHANNEL_ID = "thought_alarm_confirmation_channel"
    private const val CHANNEL_NAME = "闹钟设置确认"
    private var notificationId = 1000 // Start from 1000 to avoid conflict with alarm notifications

    /**
     * 发送闹钟设置确认通知
     * @param context 上下文
     * @param thoughtTitle 感言标题（如果为空则使用"此感言"）
     * @param alarmTime 提醒时间
     */
    fun sendAlarmSetNotification(
        context: Context,
        thoughtTitle: String,
        alarmTime: LocalDateTime
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "闹钟设置成功的确认通知"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Format alarm time
        val timeFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm")
        val formattedTime = alarmTime.format(timeFormatter)

        // Use "此感言" if title is empty or too long
        val displayTitle = when {
            thoughtTitle.isBlank() -> "此感言"
            thoughtTitle.length > 20 -> thoughtTitle.take(20) + "..."
            else -> thoughtTitle
        }

        // Build notification content
        val content = "你已为「$displayTitle」设置 $formattedTime 的提醒"

        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle("提醒设置成功")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId++, notification)
    }
}
