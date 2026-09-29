package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.util.JalaliCalendar
import com.example.util.JalaliDate

@Entity(tableName = "birthdays")
data class BirthdayEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val day: Int,          // 1..31
    val month: Int,        // 1..12
    val year: Int? = null, // Optional birth year, e.g. 1370
    val hasAlarm: Boolean = true,
    val alarmHour: Int = 9,
    val alarmMinute: Int = 0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun monthName(): String = JalaliCalendar.MON_NAMES.getOrElse(month - 1) { "" }

    fun formattedDate(): String {
        val y = if (year != null) " ${JalaliCalendar.toPersianDigits(year.toString())}" else ""
        return "${JalaliCalendar.toPersianDigits(day.toString())} ${monthName()}$y"
    }

    /**
     * Calculates days left until the next birthday occurrence from today.
     * Returns Triple(daysLeft, turningAge, nextJalaliDate)
     */
    fun nextOccurrenceInfo(today: JalaliDate = JalaliCalendar.today()): Triple<Int, Int?, JalaliDate> {
        val thisYearTarget = JalaliDate(
            year = today.year,
            month = month,
            day = Math.min(day, JalaliCalendar.monthLength(today.year, month))
        )

        val diffThisYear = JalaliCalendar.diffDays(thisYearTarget, today)
        return if (diffThisYear >= 0) {
            val turningAge = year?.let { today.year - it }
            Triple(diffThisYear, turningAge, thisYearTarget)
        } else {
            val nextYear = today.year + 1
            val nextYearTarget = JalaliDate(
                year = nextYear,
                month = month,
                day = Math.min(day, JalaliCalendar.monthLength(nextYear, month))
            )
            val diffNextYear = JalaliCalendar.diffDays(nextYearTarget, today)
            val turningAge = year?.let { nextYear - it }
            Triple(diffNextYear, turningAge, nextYearTarget)
        }
    }
}
