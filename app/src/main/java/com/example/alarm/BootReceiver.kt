package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.SpiderClockDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = SpiderClockDatabase.getDatabase(context)
                    val enabledAlarms = db.alarmDao().getEnabledAlarmsSync()
                    for (alarm in enabledAlarms) {
                        AlarmScheduler.scheduleAlarm(context, alarm)
                    }
                } catch (_: Exception) {
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
