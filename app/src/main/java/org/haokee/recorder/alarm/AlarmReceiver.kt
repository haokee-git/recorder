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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.haokee.recorder.MainActivity
import org.haokee.recorder.R
import org.haokee.recorder.data.local.ThoughtDatabase
import org.haokee.recorder.data.repository.SettingsRepository

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

        // Launch AlarmActivity with full-screen intent
        launchAlarmActivity(context, thoughtId, thoughtTitle)
    }

    private fun launchAlarmActivity(context: Context, thoughtId: String, thoughtTitle: String) {
        // Get thought details from database
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = ThoughtDatabase.getDatabase(context)
                val thought = database.thoughtDao().getThoughtById(thoughtId)

                val content = thought?.content ?: ""

                // Create full-screen intent for AlarmActivity
                val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("thought_id", thoughtId)
                    putExtra("thought_title", thoughtTitle)
                    putExtra("thought_content", content)
                }

                val fullScreenPendingIntent = PendingIntent.getActivity(
                    context,
                    thoughtId.hashCode(),
                    alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Show notification with full-screen intent
                showNotification(context, thoughtTitle, thoughtId, fullScreenPendingIntent)

                // For devices that support it, directly start the activity
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // On Android 10+, full-screen intent might not work if user disabled it
                    // So we also try to start the activity directly
                    try {
                        context.startActivity(alarmIntent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showNotification(
        context: Context,
        title: String,
        thoughtId: String,
        fullScreenPendingIntent: PendingIntent
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val settings = SettingsRepository(context)
        val soundEnabled = settings.getAlarmSound()
        val vibrationEnabled = settings.getAlarmVibration()

        // Dynamic channel ID based on sound/vibration settings
        val channelId = "thought_alarm_s${if (soundEnabled) 1 else 0}_v${if (vibrationEnabled) 1 else 0}"

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "感言提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "感言闹钟提醒"
                enableLights(true)
                setShowBadge(true)

                if (soundEnabled) {
                    val alarmSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val audioAttributes = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                    setSound(alarmSound, audioAttributes)
                } else {
                    setSound(null, null)
                }

                enableVibration(vibrationEnabled)
                if (!vibrationEnabled) {
                    vibrationPattern = longArrayOf(0)
                }
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Build notification with full-screen intent
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText("点击查看您的感言")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (!soundEnabled) {
            builder.setSilent(true)
        }

        notificationManager.notify(thoughtId.hashCode(), builder.build())
    }
}
