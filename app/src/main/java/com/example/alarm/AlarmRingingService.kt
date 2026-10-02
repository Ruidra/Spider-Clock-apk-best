package com.example.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class AlarmRingingService : Service() {
    companion object {
        private const val TAG = "AlarmRingingService"
        const val ACTION_START_RINGING = "com.example.spiderclock.ACTION_START_RINGING"
        const val ACTION_STOP_RINGING = "com.example.spiderclock.ACTION_STOP_RINGING"

        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_ALARM_TITLE = "alarm_title"
        const val EXTRA_ALARM_TIME = "alarm_time"
        const val EXTRA_SNOOZE_MINUTES = "snooze_minutes"
        const val EXTRA_VIBRATE = "vibrate"
        const val EXTRA_SOUND_TYPE = "sound_type"

        @Volatile
        var isRinging: Boolean = false
            private set
        @Volatile
        var currentAlarmId: Long = -1L
            private set
        @Volatile
        var currentAlarmTitle: String = "Wake Up"
            private set
    }

    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "SpiderClock:AlarmRingingService"
        ).apply {
            setReferenceCounted(false)
            acquire(10 * 60 * 1000L) // Max 10 minutes wake lock
        }

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_RINGING
        if (action == ACTION_STOP_RINGING) {
            stopSelf()
            return START_NOT_STICKY
        }

        val alarmId = intent?.getLongExtra(EXTRA_ALARM_ID, -1L) ?: -1L
        val title = intent?.getStringExtra(EXTRA_ALARM_TITLE) ?: "Wake Up"
        val timeStr = intent?.getStringExtra(EXTRA_ALARM_TIME) ?: ""
        val snoozeMin = intent?.getIntExtra(EXTRA_SNOOZE_MINUTES, 5) ?: 5
        val shouldVibrate = intent?.getBooleanExtra(EXTRA_VIBRATE, true) ?: true
        val soundTypeName = intent?.getStringExtra(EXTRA_SOUND_TYPE) ?: AlarmSoundType.SYSTEM_ALARM.name
        val soundType = try {
            AlarmSoundType.valueOf(soundTypeName)
        } catch (_: Exception) {
            AlarmSoundType.SYSTEM_ALARM
        }

        isRinging = true
        currentAlarmId = alarmId
        currentAlarmTitle = title

        // Build notification and start foreground
        val notification = NotificationHelper.buildRingingNotification(
            this,
            alarmId,
            title,
            timeStr,
            snoozeMin
        )
        startForeground(NotificationHelper.NOTIFICATION_ID_BASE + alarmId.toInt(), notification)

        // Play loud sound via SoundManager
        SoundManager.playSound(this, soundType, loop = true)

        // Start vibration
        if (shouldVibrate) {
            startVibration()
        }

        return START_STICKY
    }

    private fun startVibration() {
        try {
            val pattern = longArrayOf(0, 600, 300, 600, 300, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error vibrating", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRinging = false
        currentAlarmId = -1L
        SoundManager.stopSound()

        try {
            vibrator?.cancel()
        } catch (_: Exception) {}

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
    }
}
