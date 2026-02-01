package org.haokee.recorder.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.haokee.recorder.MainActivity
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

        // 调试日志：打印设置的时间
        android.util.Log.d("AlarmHelper", "========== 设置闹钟 ==========")
        android.util.Log.d("AlarmHelper", "原始时间: $alarmTime")
        android.util.Log.d("AlarmHelper", "调整后时间: $exactAlarmTime")
        android.util.Log.d("AlarmHelper", "时间戳: $triggerTime")
        android.util.Log.d("AlarmHelper", "当前时间: ${LocalDateTime.now()}")
        android.util.Log.d("AlarmHelper", "================================")

        // 使用 setAlarmClock() 确保精确触发（即使在 Doze 模式下）
        val showIntent = Intent(context, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            0,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = android.app.AlarmManager.AlarmClockInfo(
            triggerTime,
            showPendingIntent
        )

        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)

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
