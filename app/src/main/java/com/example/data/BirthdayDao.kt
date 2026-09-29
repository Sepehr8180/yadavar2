package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BirthdayDao {
    @Query("SELECT * FROM birthdays ORDER BY month ASC, day ASC")
    fun getAllBirthdays(): Flow<List<BirthdayEntity>>

    @Query("SELECT * FROM birthdays WHERE id = :id LIMIT 1")
    suspend fun getBirthdayById(id: Long): BirthdayEntity?

    @Query("SELECT * FROM birthdays WHERE month = :month AND day = :day")
    suspend fun getBirthdaysForDate(month: Int, day: Int): List<BirthdayEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBirthday(birthday: BirthdayEntity): Long

    @Update
    suspend fun updateBirthday(birthday: BirthdayEntity)

    @Delete
    suspend fun deleteBirthday(birthday: BirthdayEntity)

    @Query("DELETE FROM birthdays WHERE id = :id")
    suspend fun deleteBirthdayById(id: Long)
}
