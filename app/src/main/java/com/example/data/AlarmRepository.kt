package com.example.data

import kotlinx.coroutines.flow.Flow

class AlarmRepository(
    private val alarmDao: AlarmDao,
    private val birthdayDao: BirthdayDao
) {
    // --- Alarms ---
    val allAlarms: Flow<List<AlarmEntity>> = alarmDao.getAllAlarms()

    fun getAlarmsForDate(year: Int, month: Int, day: Int): Flow<List<AlarmEntity>> {
        return alarmDao.getAlarmsForDate(year, month, day)
    }

    suspend fun getActiveAlarms(): List<AlarmEntity> {
        return alarmDao.getActiveAlarms()
    }

    suspend fun getAlarmById(id: Long): AlarmEntity? {
        return alarmDao.getAlarmById(id)
    }

    suspend fun insertAlarm(alarm: AlarmEntity): Long {
        return alarmDao.insertAlarm(alarm)
    }

    suspend fun updateAlarm(alarm: AlarmEntity) {
        alarmDao.updateAlarm(alarm)
    }

    suspend fun deleteAlarm(alarm: AlarmEntity) {
        alarmDao.deleteAlarm(alarm)
    }

    suspend fun deleteAlarmById(id: Long) {
        alarmDao.deleteAlarmById(id)
    }

    suspend fun setAlarmEnabled(id: Long, enabled: Boolean) {
        alarmDao.updateAlarmStatus(id, enabled)
    }

    suspend fun setAlarmDone(id: Long, done: Boolean) {
        alarmDao.updateAlarmDone(id, done)
    }

    // --- Birthdays ---
    val allBirthdays: Flow<List<BirthdayEntity>> = birthdayDao.getAllBirthdays()

    suspend fun insertBirthday(birthday: BirthdayEntity): Long {
        return birthdayDao.insertBirthday(birthday)
    }

    suspend fun updateBirthday(birthday: BirthdayEntity) {
        birthdayDao.updateBirthday(birthday)
    }

    suspend fun deleteBirthday(birthday: BirthdayEntity) {
        birthdayDao.deleteBirthday(birthday)
    }

    suspend fun deleteBirthdayById(id: Long) {
        birthdayDao.deleteBirthdayById(id)
    }
}
