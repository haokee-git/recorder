package org.haokee.recorder.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import org.haokee.recorder.MainActivity
import org.haokee.recorder.R

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val thoughtId = intent.getStringExtra("thought_id") ?: return
        val thoughtTitle = intent.getStringExtra("thought_title") ?: "感言提醒"

        // 调试日志：打印闹钟响的时间
        val currentTime = java.time.LocalDateTime.now()
        android.util.Log.d("AlarmReceiver", "========== 闹钟触发 ==========")
        android.util.Log.d("AlarmReceiver", "触发时间: $currentTime")
        android.util.Log.d("AlarmReceiver", "感言ID: $thoughtId")
        android.util.Log.d("AlarmReceiver", "感言标题: $thoughtTitle")
        android.util.Log.d("AlarmReceiver", "================================")

        // 直接播放铃声和振动
        playAlarmSound(context)
        triggerVibration(context)

        // 显示通知
        showNotification(context, thoughtTitle, thoughtId)
    }

    private fun playAlarmSound(context: Context) {
        try {
            // 获取系统闹钟铃声
            val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            // 创建 Ringtone 对象并播放
            val ringtone: Ringtone = RingtoneManager.getRingtone(context, alarmUri)

            // 设置为闹钟音频流
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                ringtone.streamType = AudioManager.STREAM_ALARM
            }

            // 播放铃声（会持续播放直到手动停止）
            ringtone.play()

            // 10秒后自动停止（防止一直响）
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (ringtone.isPlaying) {
                    ringtone.stop()
                }
            }, 10000)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun triggerVibration(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            // 振动模式：[等待, 振动, 等待, 振动, ...]
            val pattern = longArrayOf(0, 500, 200, 500, 200, 500, 200, 500, 200, 500)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(pattern, -1) // -1 表示不重复
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showNotification(context: Context, title: String, thoughtId: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 使用新的 CHANNEL_ID（解决渠道不更新的问题）
        val channelId = "thought_alarm_channel_v2"

        // Get default alarm sound
        val alarmSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "感言提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "感言闹钟提醒"
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
                // Set alarm sound to the channel
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
                setSound(alarmSound, audioAttributes)
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
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText("点击查看您的感言")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .setOngoing(false) // 允许用户滑动关闭通知（因为铃声会自动停止）
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(thoughtId.hashCode(), notification)
    }

    companion object {
        private const val CHANNEL_ID = "thought_alarm_channel_v2"
    }
}
