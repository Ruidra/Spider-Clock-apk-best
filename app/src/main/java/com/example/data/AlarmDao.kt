package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.AlarmItem
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    fun getAllAlarms(): Flow<List<AlarmItem>>

    @Query("SELECT * FROM alarms WHERE isEnabled = 1")
    suspend fun getEnabledAlarmsSync(): List<AlarmItem>

    @Query("SELECT * FROM alarms WHERE id = :id LIMIT 1")
    suspend fun getAlarmById(id: Long): AlarmItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: AlarmItem): Long

    @Update
    suspend fun updateAlarm(alarm: AlarmItem)

    @Delete
    suspend fun deleteAlarm(alarm: AlarmItem)

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteById(id: Long)
}
