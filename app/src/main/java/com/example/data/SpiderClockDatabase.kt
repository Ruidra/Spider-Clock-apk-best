package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.alarm.AlarmSoundType
import com.example.model.AlarmCategory
import com.example.model.AlarmItem
import com.example.model.WakeChallenge

class Converters {
    @TypeConverter
    fun fromCategory(value: AlarmCategory): String = value.name

    @TypeConverter
    fun toCategory(value: String): AlarmCategory = try {
        AlarmCategory.valueOf(value)
    } catch (_: Exception) {
        AlarmCategory.WAKEUP
    }

    @TypeConverter
    fun fromChallenge(value: WakeChallenge): String = value.name

    @TypeConverter
    fun toChallenge(value: String): WakeChallenge = try {
        WakeChallenge.valueOf(value)
    } catch (_: Exception) {
        WakeChallenge.NONE
    }

    @TypeConverter
    fun fromSoundType(value: AlarmSoundType): String = value.name

    @TypeConverter
    fun toSoundType(value: String): AlarmSoundType = try {
        AlarmSoundType.valueOf(value)
    } catch (_: Exception) {
        AlarmSoundType.SYSTEM_ALARM
    }
}

@Database(entities = [AlarmItem::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class SpiderClockDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    companion object {
        @Volatile
        private var INSTANCE: SpiderClockDatabase? = null

        fun getDatabase(context: Context): SpiderClockDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SpiderClockDatabase::class.java,
                    "spider_clock.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
