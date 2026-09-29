package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.util.JalaliCalendar
import com.example.util.JalaliDate

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val hour: Int,
    val minute: Int,
    val isDaily: Boolean = false,
    val repeatDays: String = "", // Comma-separated Persian DOW indexes (0=Saturday..6=Friday)
    val jalaliYear: Int,
    val jalaliMonth: Int,
    val jalaliDay: Int,
    val prio: String = "normal", // "urgent", "high", "normal", "low"
    val tag: String = "شخصی", // "کار", "شخصی", "روتین", "خونه", "سلامت", "تولد"
    val isEnabled: Boolean = true,
    val isVibrate: Boolean = true,
    val snoozeMinutes: Int = 10,
    val isDone: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJalaliDate(): JalaliDate = JalaliDate(jalaliYear, jalaliMonth, jalaliDay)

    fun formattedTime(persian: Boolean = true): String {
        return JalaliCalendar.formatTime(hour, minute, persian)
    }

    fun parseRepeatDays(): Set<Int> {
        if (repeatDays.isBlank()) return emptySet()
        return repeatDays.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
    }

    fun getRepeatDaysLabel(): String {
        if (isDaily) return "هر روز"
        val days = parseRepeatDays()
        if (days.isEmpty()) {
            return "${JalaliCalendar.toPersianDigits(jalaliDay.toString())} ${JalaliCalendar.MON_NAMES.getOrElse(jalaliMonth - 1) { "" }}"
        }
        if (days.size == 7) return "هر روز"
        if (days == setOf(0, 1, 2, 3, 4)) return "روزهای کاری (ش تا چ)"
        if (days == setOf(5, 6)) return "آخر هفته (پ و ج)"
        return days.sorted().mapNotNull { JalaliCalendar.DOW_SHORT.getOrNull(it) }.joinToString("، ")
    }
}
