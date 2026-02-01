package org.haokee.recorder.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDateTime
import java.time.ZoneId

object AlarmHelper {
    fun scheduleAlarm(
        context: Context,
        thoughtId: String,
        thoughtTitle: String,
        alarmTime: LocalDateTime
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("thought_id", thoughtId)
            putExtra("thought_title", thoughtTitle)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            thoughtId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 将秒数和纳秒设为0，确保在整分钟的第0秒响
        val exactAlarmTime = alarmTime.withSecond(0).withNano(0)
        val triggerTime = exactAlarmTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Check if we can schedule exact alarms on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                // Fallback to inexact alarm if exact alarm permission not granted
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }

        // Send immediate confirmation notification (使用调整后的精确时间)
        NotificationHelper.sendAlarmSetNotification(context, thoughtTitle, exactAlarmTime)
    }

    fun cancelAlarm(context: Context, thoughtId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            thoughtId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}
