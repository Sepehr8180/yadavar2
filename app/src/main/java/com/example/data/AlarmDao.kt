package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY isDone ASC, hour ASC, minute ASC, id DESC")
    fun getAllAlarms(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE isEnabled = 1 AND isDone = 0")
    suspend fun getActiveAlarms(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE id = :id LIMIT 1")
    suspend fun getAlarmById(id: Long): AlarmEntity?

    @Query("SELECT * FROM alarms WHERE jalaliYear = :year AND jalaliMonth = :month AND jalaliDay = :day")
    fun getAlarmsForDate(year: Int, month: Int, day: Int): Flow<List<AlarmEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: AlarmEntity): Long

    @Update
    suspend fun updateAlarm(alarm: AlarmEntity)

    @Delete
    suspend fun deleteAlarm(alarm: AlarmEntity)

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteAlarmById(id: Long)

    @Query("UPDATE alarms SET isEnabled = :enabled WHERE id = :id")
    suspend fun updateAlarmStatus(id: Long, enabled: Boolean)

    @Query("UPDATE alarms SET isDone = :done WHERE id = :id")
    suspend fun updateAlarmDone(id: Long, done: Boolean)
}
