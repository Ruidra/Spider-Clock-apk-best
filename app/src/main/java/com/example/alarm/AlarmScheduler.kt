package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.model.AlarmItem
import java.util.Calendar

object AlarmScheduler {
    private const val TAG = "AlarmScheduler"
    const val ACTION_ALARM_TRIGGER = "com.example.spiderclock.ACTION_ALARM_TRIGGER"
    const val ACTION_SNOOZE = "com.example.spiderclock.ACTION_SNOOZE"
    const val ACTION_DISMISS = "com.example.spiderclock.ACTION_DISMISS"

    const val EXTRA_ALARM_ID = "extra_alarm_id"
    const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"

    fun scheduleAlarm(context: Context, alarm: AlarmItem) {
        if (!alarm.isEnabled) {
            cancelAlarm(context, alarm.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            putExtra(EXTRA_ALARM_ID, alarm.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = calculateNextTriggerMillis(alarm.hour, alarm.minute, alarm.daysMask)
        Log.d(TAG, "Scheduling alarm ${alarm.id} for $triggerAtMillis (${java.util.Date(triggerAtMillis)})")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    val clockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, pendingIntent)
                    alarmManager.setAlarmClock(clockInfo, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                val clockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, pendingIntent)
                alarmManager.setAlarmClock(clockInfo, pendingIntent)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Exact alarm permission denied, falling back to setAndAllowWhileIdle", e)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancelAlarm(context: Context, alarmId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun scheduleSnooze(context: Context, alarmId: Long, minutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAtMillis = System.currentTimeMillis() + (minutes * 60 * 1000L)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val clockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, pendingIntent)
            alarmManager.setAlarmClock(clockInfo, pendingIntent)
        } catch (e: Exception) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun calculateNextTriggerMillis(hour: Int, minute: Int, daysMask: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If one-time (daysMask == 0)
        if (daysMask == 0) {
            if (target.timeInMillis <= now.timeInMillis) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis
        }

        // Repeating alarm: check today and following 7 days
        for (i in 0..7) {
            val dayOfWeek = target.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday, etc.
            val bitIndex = dayOfWeek - 1 // 0 = Sunday, 1 = Monday ... 6 = Saturday
            val isDayActive = (daysMask and (1 shl bitIndex)) != 0

            if (isDayActive && target.timeInMillis > now.timeInMillis) {
                return target.timeInMillis
            }
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        return target.timeInMillis
    }

    fun formatCountdown(targetMillis: Long): String {
        val diff = targetMillis - System.currentTimeMillis()
        if (diff <= 0) return "Due now"
        val totalMinutes = diff / (60 * 1000)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        val days = hours / 24
        val remHours = hours % 24

        return when {
            days > 0 -> "In ${days}d ${remHours}h"
            hours > 0 -> "In ${hours}h ${minutes}m"
            else -> "In ${minutes}m"
        }
    }
}
