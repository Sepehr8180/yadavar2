package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AlarmEntity::class, BirthdayEntity::class],
    version = 2,
    exportSchema = false
)
abstract class YadavarDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun birthdayDao(): BirthdayDao

    companion object {
        @Volatile
        private var INSTANCE: YadavarDatabase? = null

        fun getInstance(context: Context): YadavarDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    YadavarDatabase::class.java,
                    "yadavar_alarms.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
