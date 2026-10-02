package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.AlarmScheduler
import com.example.alarm.AlarmSoundType
import com.example.alarm.NotificationHelper
import com.example.alarm.SoundManager
import com.example.data.AlarmRepository
import com.example.data.SpiderClockDatabase
import com.example.model.AlarmCategory
import com.example.model.AlarmItem
import com.example.model.WakeChallenge
import com.example.ui.theme.SpiderThemePreset
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class SpiderClockViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AlarmRepository

    init {
        val db = SpiderClockDatabase.getDatabase(application)
        repository = AlarmRepository(db.alarmDao())
        NotificationHelper.createNotificationChannels(application)
    }

    // Alarms flow
    val allAlarms: StateFlow<List<AlarmItem>> = repository.allAlarms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Next scheduled alarm
    val nextUpcomingAlarm: StateFlow<AlarmItem?> = allAlarms.map { list ->
        val enabled = list.filter { it.isEnabled }
        if (enabled.isEmpty()) return@map null
        enabled.minByOrNull { alarm ->
            AlarmScheduler.calculateNextTriggerMillis(alarm.hour, alarm.minute, alarm.daysMask)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // App Preferences
    private val _theme = MutableStateFlow(SpiderThemePreset.GOTHIC_NOIR)
    val theme: StateFlow<SpiderThemePreset> = _theme.asStateFlow()

    private val _is24Hour = MutableStateFlow(false)
    val is24Hour: StateFlow<Boolean> = _is24Hour.asStateFlow()

    private val _isSmoothSecond = MutableStateFlow(true)
    val isSmoothSecond: StateFlow<Boolean> = _isSmoothSecond.asStateFlow()

    private val _showCogs = MutableStateFlow(true)
    val showCogs: StateFlow<Boolean> = _showCogs.asStateFlow()

    private val _useOriginalRepoClock = MutableStateFlow(true)
    val useOriginalRepoClock: StateFlow<Boolean> = _useOriginalRepoClock.asStateFlow()

    private val _defaultSnooze = MutableStateFlow(5)
    val defaultSnooze: StateFlow<Int> = _defaultSnooze.asStateFlow()

    // --- Stopwatch State ---
    private val _stopwatchRunning = MutableStateFlow(false)
    val stopwatchRunning: StateFlow<Boolean> = _stopwatchRunning.asStateFlow()

    private val _stopwatchElapsed = MutableStateFlow(0L)
    val stopwatchElapsed: StateFlow<Long> = _stopwatchElapsed.asStateFlow()

    private val _stopwatchLaps = MutableStateFlow<List<Long>>(emptyList())
    val stopwatchLaps: StateFlow<List<Long>> = _stopwatchLaps.asStateFlow()

    private var stopwatchJob: Job? = null
    private var stopwatchStartTime: Long = 0L

    // --- Timer State ---
    private val _timerTotalSeconds = MutableStateFlow(300) // Default 5 mins
    val timerTotalSeconds: StateFlow<Int> = _timerTotalSeconds.asStateFlow()

    private val _timerRemainingSeconds = MutableStateFlow(300)
    val timerRemainingSeconds: StateFlow<Int> = _timerRemainingSeconds.asStateFlow()

    private val _timerRunning = MutableStateFlow(false)
    val timerRunning: StateFlow<Boolean> = _timerRunning.asStateFlow()

    private var timerJob: Job? = null

    init {
        // Pre-populate with default alarm if database is completely empty on first launch
        viewModelScope.launch {
            val list = repository.getEnabledAlarmsSync()
            if (list.isEmpty()) {
                val defaultAlarm = AlarmItem(
                    hour = 7,
                    minute = 30,
                    title = "Spider Wake Up",
                    category = AlarmCategory.WAKEUP,
                    daysMask = 62, // Mon - Fri
                    isEnabled = true,
                    challenge = WakeChallenge.TAP_SPIDER
                )
                val id = repository.insertAlarm(defaultAlarm)
                AlarmScheduler.scheduleAlarm(getApplication(), defaultAlarm.copy(id = id))
            }
        }
    }

    // --- Alarm CRUD ---
    fun saveAlarm(alarm: AlarmItem) {
        viewModelScope.launch {
            val finalId = if (alarm.id == 0L) {
                repository.insertAlarm(alarm)
            } else {
                repository.updateAlarm(alarm)
                alarm.id
            }
            val savedAlarm = alarm.copy(id = finalId)
            if (savedAlarm.isEnabled) {
                AlarmScheduler.scheduleAlarm(getApplication(), savedAlarm)
            } else {
                AlarmScheduler.cancelAlarm(getApplication(), finalId)
            }
        }
    }

    fun toggleAlarm(alarm: AlarmItem, enabled: Boolean) {
        viewModelScope.launch {
            val updated = alarm.copy(isEnabled = enabled)
            repository.updateAlarm(updated)
            if (enabled) {
                AlarmScheduler.scheduleAlarm(getApplication(), updated)
            } else {
                AlarmScheduler.cancelAlarm(getApplication(), alarm.id)
            }
        }
    }

    fun deleteAlarm(alarm: AlarmItem) {
        viewModelScope.launch {
            AlarmScheduler.cancelAlarm(getApplication(), alarm.id)
            repository.deleteAlarm(alarm)
        }
    }

    fun scheduleQuickNap(minutes: Int) {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MINUTE, minutes)
            val h = cal.get(Calendar.HOUR_OF_DAY)
            val m = cal.get(Calendar.MINUTE)

            val napAlarm = AlarmItem(
                hour = h,
                minute = m,
                title = "Arachnid Power Nap ($minutes m)",
                category = AlarmCategory.WAKEUP,
                daysMask = 0, // One-time
                isEnabled = true,
                challenge = WakeChallenge.NONE
            )
            val id = repository.insertAlarm(napAlarm)
            AlarmScheduler.scheduleAlarm(getApplication(), napAlarm.copy(id = id))
        }
    }

    // --- Settings / Preferences ---
    fun setTheme(themePreset: SpiderThemePreset) {
        _theme.value = themePreset
    }

    fun set24Hour(value: Boolean) {
        _is24Hour.value = value
    }

    fun setSmoothSecond(value: Boolean) {
        _isSmoothSecond.value = value
    }

    fun setShowCogs(value: Boolean) {
        _showCogs.value = value
    }

    fun setUseOriginalRepoClock(value: Boolean) {
        _useOriginalRepoClock.value = value
    }

    fun setDefaultSnooze(minutes: Int) {
        _defaultSnooze.value = minutes
    }

    fun triggerTestNotification() {
        NotificationHelper.sendTestPushNotification(getApplication())
    }

    // --- Stopwatch Actions ---
    fun startStopwatch() {
        if (_stopwatchRunning.value) return
        _stopwatchRunning.value = true
        stopwatchStartTime = System.currentTimeMillis() - _stopwatchElapsed.value
        stopwatchJob = viewModelScope.launch {
            while (_stopwatchRunning.value) {
                _stopwatchElapsed.value = System.currentTimeMillis() - stopwatchStartTime
                delay(30)
            }
        }
    }

    fun pauseStopwatch() {
        _stopwatchRunning.value = false
        stopwatchJob?.cancel()
    }

    fun resetStopwatch() {
        pauseStopwatch()
        _stopwatchElapsed.value = 0L
        _stopwatchLaps.value = emptyList()
    }

    fun recordLap() {
        if (_stopwatchElapsed.value > 0) {
            _stopwatchLaps.value = listOf(_stopwatchElapsed.value) + _stopwatchLaps.value
        }
    }

    // --- Timer Actions ---
    fun setTimerSeconds(seconds: Int) {
        if (!_timerRunning.value && seconds > 0) {
            _timerTotalSeconds.value = seconds
            _timerRemainingSeconds.value = seconds
        }
    }

    fun setCustomTimer(hours: Int, minutes: Int, seconds: Int) {
        val total = (hours * 3600) + (minutes * 60) + seconds
        if (total > 0) {
            pauseTimer()
            _timerTotalSeconds.value = total
            _timerRemainingSeconds.value = total
        }
    }

    fun addTimerTime(secondsToAdd: Int) {
        if (!_timerRunning.value) {
            val newTotal = _timerTotalSeconds.value + secondsToAdd
            setTimerSeconds(newTotal)
        } else {
            _timerRemainingSeconds.value += secondsToAdd
            _timerTotalSeconds.value += secondsToAdd
        }
    }

    fun startTimer() {
        if (_timerRunning.value) return
        if (_timerRemainingSeconds.value <= 0) {
            _timerRemainingSeconds.value = _timerTotalSeconds.value
        }
        _timerRunning.value = true
        timerJob = viewModelScope.launch {
            while (_timerRunning.value && _timerRemainingSeconds.value > 0) {
                delay(1000)
                _timerRemainingSeconds.value--
            }
            if (_timerRemainingSeconds.value <= 0) {
                _timerRunning.value = false
                val totalSec = _timerTotalSeconds.value
                val formatted = if (totalSec >= 3600) {
                    "${totalSec / 3600}h ${(totalSec % 3600) / 60}m"
                } else if (totalSec >= 60) {
                    "${totalSec / 60}m ${totalSec % 60}s"
                } else {
                    "${totalSec}s"
                }
                NotificationHelper.sendTimerFinishedNotification(getApplication(), formatted)
                SoundManager.playSound(getApplication(), AlarmSoundType.STEAMPUNK_CHIME, false)
                delay(5000)
                SoundManager.stopSound()
            }
        }
    }

    fun pauseTimer() {
        _timerRunning.value = false
        timerJob?.cancel()
    }

    fun resetTimer() {
        pauseTimer()
        SoundManager.stopSound()
        _timerRemainingSeconds.value = _timerTotalSeconds.value
    }
}
