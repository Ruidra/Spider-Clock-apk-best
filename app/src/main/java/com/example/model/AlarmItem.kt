package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.alarm.AlarmSoundType

enum class AlarmCategory(val displayName: String, val iconName: String) {
    WAKEUP("Wake Up", "alarm"),
    EVENT("Event", "event"),
    MEDICATION("Medication", "medication"),
    WORK("Work / Meeting", "work"),
    CUSTOM("Custom", "star")
}

enum class WakeChallenge(val displayName: String, val description: String) {
    NONE("Normal", "Instant swipe to dismiss"),
    TAP_SPIDER("Catch the Spider", "Tap the quick spider 5 times to prove you are awake"),
    MATH_PUZZLE("Spider Math", "Solve an arithmetic puzzle before dismissing"),
    WEB_UNTANGLE("Untangle Silk", "Drag the spider through web checkpoints")
}

enum class RecurringPreset(val displayName: String, val mask: Int) {
    ONCE("Once", 0),
    DAILY("Daily (Every Day)", 127),
    WEEKDAYS("Weekdays (Mon-Fri)", 62),
    WEEKENDS("Weekends (Sat-Sun)", 65),
    WEEKLY("Weekly (Specific Day)", -1),
    CUSTOM("Custom Days", -2)
}

@Entity(tableName = "alarms")
data class AlarmItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val title: String = "Wake Up",
    val category: AlarmCategory = AlarmCategory.WAKEUP,
    val daysMask: Int = 0, // Bitmask: bit 0: Sun, 1: Mon, 2: Tue, 3: Wed, 4: Thu, 5: Fri, 6: Sat. 0 means one-time.
    val isEnabled: Boolean = true,
    val vibrate: Boolean = true,
    val challenge: WakeChallenge = WakeChallenge.NONE,
    val snoozeMinutes: Int = 5,
    val soundType: AlarmSoundType = AlarmSoundType.SYSTEM_ALARM,
    val createdAt: Long = System.currentTimeMillis(),
    val lastFiredAt: Long = 0
) {
    fun isOneTime(): Boolean = daysMask == 0

    fun isDayEnabled(dayOfWeekFromSundayZero: Int): Boolean {
        return (daysMask and (1 shl dayOfWeekFromSundayZero)) != 0
    }

    fun formattedTime(is24Hour: Boolean = false): String {
        return if (is24Hour) {
            String.format("%02d:%02d", hour, minute)
        } else {
            val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
            val amPm = if (hour < 12) "AM" else "PM"
            String.format("%02d:%02d %s", h, minute, amPm)
        }
    }

    fun getDaysDescription(): String {
        if (daysMask == 0) return "Once"
        if (daysMask == 127) return "Daily"
        if (daysMask == 62) return "Weekdays (Mon-Fri)"
        if (daysMask == 65) return "Weekends (Sat-Sun)"
        val names = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val active = mutableListOf<String>()
        for (i in 0..6) {
            if ((daysMask and (1 shl i)) != 0) {
                active.add(names[i])
            }
        }
        return if (active.size == 1) "Weekly on ${active[0]}" else active.joinToString(", ")
    }
}
