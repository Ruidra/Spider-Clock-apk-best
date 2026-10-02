package com.example.alarm

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.SpiderClockDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)

        when (action) {
            AlarmScheduler.ACTION_ALARM_TRIGGER -> {
                if (alarmId == -1L) return
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = SpiderClockDatabase.getDatabase(context)
                        val alarm = db.alarmDao().getAlarmById(alarmId)
                        if (alarm != null && alarm.isEnabled) {
                            // Start Ringing Foreground Service
                            val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
                                setAction(AlarmRingingService.ACTION_START_RINGING)
                                putExtra(AlarmRingingService.EXTRA_ALARM_ID, alarm.id)
                                putExtra(AlarmRingingService.EXTRA_ALARM_TITLE, alarm.title)
                                putExtra(AlarmRingingService.EXTRA_ALARM_TIME, alarm.formattedTime())
                                putExtra(AlarmRingingService.EXTRA_SNOOZE_MINUTES, alarm.snoozeMinutes)
                                putExtra(AlarmRingingService.EXTRA_VIBRATE, alarm.vibrate)
                                putExtra(AlarmRingingService.EXTRA_SOUND_TYPE, alarm.soundType.name)
                            }
                            ContextCompat.startForegroundService(context, serviceIntent)

                            // Launch Fullscreen Alarm Activity
                            val activityIntent = Intent(context, AlarmRingingActivity::class.java).apply {
                                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                            }
                            context.startActivity(activityIntent)

                            // If one-time, disable it
                            if (alarm.isOneTime()) {
                                db.alarmDao().updateAlarm(alarm.copy(isEnabled = false, lastFiredAt = System.currentTimeMillis()))
                            } else {
                                // Reschedule next repetition
                                db.alarmDao().updateAlarm(alarm.copy(lastFiredAt = System.currentTimeMillis()))
                                AlarmScheduler.scheduleAlarm(context, alarm)
                            }
                        }
                    } catch (_: Exception) {
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            AlarmScheduler.ACTION_SNOOZE -> {
                val snoozeMinutes = intent.getIntExtra(AlarmScheduler.EXTRA_SNOOZE_MINUTES, 5)
                // Stop ringing service
                val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
                    setAction(AlarmRingingService.ACTION_STOP_RINGING)
                }
                context.startService(serviceIntent)

                // Schedule snooze
                if (alarmId != -1L) {
                    AlarmScheduler.scheduleSnooze(context, alarmId, snoozeMinutes)
                }

                // Dismiss notification
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.cancel(NotificationHelper.NOTIFICATION_ID_BASE + alarmId.toInt())
            }

            AlarmScheduler.ACTION_DISMISS -> {
                // Stop ringing service
                val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
                    setAction(AlarmRingingService.ACTION_STOP_RINGING)
                }
                context.startService(serviceIntent)

                // Dismiss notification
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.cancel(NotificationHelper.NOTIFICATION_ID_BASE + alarmId.toInt())
            }
        }
    }
}
