package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.util.JalaliCalendar
import com.example.util.JalaliDate

@Entity(tableName = "financial_items")
data class FinancialEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Long,
    val jalaliYear: Int,
    val jalaliMonth: Int,
    val jalaliDay: Int,
    val isMonthly: Boolean = false,
    val endJalaliYear: Int? = null,
    val endJalaliMonth: Int? = null,
    val endJalaliDay: Int? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun startDate(): JalaliDate = JalaliDate(jalaliYear, jalaliMonth, jalaliDay)

    fun endDate(): JalaliDate? {
        val y = endJalaliYear ?: return null
        val m = endJalaliMonth ?: return null
        val d = endJalaliDay ?: return null
        return JalaliDate(y, m, d)
    }

    /**
     * Returns the payment occurrence for this item inside the requested month, or null.
     * Monthly payments stay on the original day; for shorter months they fall on the
     * last valid day of that month.
     */
    fun occurrenceInMonth(year: Int, month: Int): JalaliDate? {
        val monthStart = JalaliDate(year, month, 1)
        if (monthStart < startDate().copy(day = 1)) return null

        val end = endDate()
        if (end != null && monthStart > end.copy(day = 1)) return null

        if (!isMonthly) {
            return if (jalaliYear == year && jalaliMonth == month) {
                val day = minOf(jalaliDay, JalaliCalendar.monthLength(year, month))
                JalaliDate(year, month, day).takeIf { end == null || it <= end }
            } else null
        }

        val day = minOf(jalaliDay, JalaliCalendar.monthLength(year, month))
        val candidate = JalaliDate(year, month, day)
        return when {
            candidate < startDate() -> null
            end != null && candidate > end -> null
            else -> candidate
        }
    }

    fun occursOn(date: JalaliDate): Boolean = occurrenceInMonth(date.year, date.month) == date

    fun formattedAmount(): String = JalaliCalendar.formatNumber(amount)

    fun frequencyLabel(): String = if (!isMonthly) "یک‌بار" else {
        val end = endDate()
        if (end == null) "هر ماه" else "هر ماه تا ${end.formatted()}"
    }
}
