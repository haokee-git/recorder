package org.haokee.recorder.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import org.haokee.recorder.MainActivity
import java.time.LocalDateTime
import java.time.ZoneId

object AlarmHelper {
    /**
     * 检查并请求精确闹钟权限
     * @return true 如果有权限，false 如果没有权限（会自动打开设置页面）
     */
    fun checkAndRequestAlarmPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                // 没有权限，打开设置页面
                Toast.makeText(context, "需要授予精确闹钟权限，即将跳转到设置页面", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return false
            }
        }
        return true
    }

    fun scheduleAlarm(
        context: Context,
        thoughtId: String,
        thoughtTitle: String,
        alarmTime: LocalDateTime
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 先检查权限
        if (!checkAndRequestAlarmPermission(context)) {
            Toast.makeText(context, "请在设置中授予精确闹钟权限后重试", Toast.LENGTH_SHORT).show()
            return
        }

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

        // 使用 setAlarmClock() 确保精确触发（权限已检查）
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
        android.util.Log.d("AlarmHelper", "闹钟设置成功，使用 setAlarmClock()")

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
