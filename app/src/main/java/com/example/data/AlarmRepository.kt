package com.example.data

import com.example.model.AlarmItem
import kotlinx.coroutines.flow.Flow

class AlarmRepository(private val alarmDao: AlarmDao) {
    val allAlarms: Flow<List<AlarmItem>> = alarmDao.getAllAlarms()

    suspend fun getEnabledAlarmsSync(): List<AlarmItem> = alarmDao.getEnabledAlarmsSync()

    suspend fun getAlarmById(id: Long): AlarmItem? = alarmDao.getAlarmById(id)

    suspend fun insertAlarm(alarm: AlarmItem): Long = alarmDao.insertAlarm(alarm)

    suspend fun updateAlarm(alarm: AlarmItem) = alarmDao.updateAlarm(alarm)

    suspend fun deleteAlarm(alarm: AlarmItem) = alarmDao.deleteAlarm(alarm)

    suspend fun deleteAlarmById(id: Long) = alarmDao.deleteById(id)
}
