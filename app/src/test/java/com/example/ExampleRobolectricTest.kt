package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.alarm.AlarmScheduler
import com.example.alarm.AlarmSoundType
import com.example.model.AlarmCategory
import com.example.model.AlarmItem
import com.example.model.WakeChallenge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Spider Clock", appName)
    }

    @Test
    fun `alarm item formatted time and sound selection`() {
        val alarm = AlarmItem(
            hour = 7,
            minute = 30,
            title = "Spider Wake Up",
            category = AlarmCategory.WAKEUP,
            daysMask = 62,
            challenge = WakeChallenge.TAP_SPIDER,
            snoozeMinutes = 15,
            soundType = AlarmSoundType.GOTHIC_BELL
        )
        assertEquals("07:30 AM", alarm.formattedTime(is24Hour = false))
        assertEquals("07:30", alarm.formattedTime(is24Hour = true))
        assertEquals("Weekdays (Mon-Fri)", alarm.getDaysDescription())
        assertEquals(AlarmSoundType.GOTHIC_BELL, alarm.soundType)
        assertEquals(15, alarm.snoozeMinutes)
    }

    @Test
    fun `alarm scheduler calculates upcoming trigger`() {
        val nextTrigger = AlarmScheduler.calculateNextTriggerMillis(hour = 8, minute = 0, daysMask = 0)
        assertTrue("Trigger should be in the future", nextTrigger > System.currentTimeMillis() - 5000)
    }

    @Test
    fun `recurring schedule description for daily and weekly`() {
        val dailyAlarm = AlarmItem(hour = 6, minute = 0, daysMask = 127)
        assertEquals("Daily", dailyAlarm.getDaysDescription())

        val weeklyAlarm = AlarmItem(hour = 9, minute = 0, daysMask = 2) // Monday only
        assertEquals("Weekly on Mon", weeklyAlarm.getDaysDescription())
    }
}
