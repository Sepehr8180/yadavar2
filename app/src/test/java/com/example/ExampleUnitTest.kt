package com.example

import com.example.util.JalaliCalendar
import com.example.util.JalaliDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testGregorianToJalali() {
        // 2026-09-29 is 1405-07-07
        val jalali = JalaliCalendar.gregorianToJalali(2026, 9, 29)
        assertEquals(1405, jalali.year)
        assertEquals(7, jalali.month)
        assertEquals(7, jalali.day)
    }

    @Test
    fun testJalaliToGregorian() {
        val (gy, gm, gd) = JalaliCalendar.jalaliToGregorian(1405, 7, 7)
        assertEquals(2026, gy)
        assertEquals(9, gm)
        assertEquals(29, gd)
    }

    @Test
    fun testMonthLengths() {
        // First 6 months have 31 days
        for (m in 1..6) {
            assertEquals(31, JalaliCalendar.monthLength(1405, m))
        }
        // Next 5 months have 30 days
        for (m in 7..11) {
            assertEquals(30, JalaliCalendar.monthLength(1405, m))
        }
        // Esfand in regular year is 29 days
        assertFalse(JalaliCalendar.isLeap(1405))
        assertEquals(29, JalaliCalendar.monthLength(1405, 12))
    }

    @Test
    fun testPersianDigits() {
        assertEquals("۱۲:۳۰", JalaliCalendar.toPersianDigits("12:30"))
        assertEquals("۱۴۰۵/۰۷/۰۷", JalaliCalendar.toPersianDigits("1405/07/07"))
    }
}

class WeekdayParsingTest {
    @Test
    fun explicitPersianWeekdayIsMappedCorrectly() {
        assertEquals(5, com.example.util.GeminiAiParser.detectWeekday("پنجشنبه ساعت ۹"))
        assertEquals(4, com.example.util.GeminiAiParser.detectWeekday("چهارشنبه"))
        assertEquals(1, com.example.util.GeminiAiParser.detectWeekday("یک‌شنبه"))
        assertEquals(0, com.example.util.GeminiAiParser.detectWeekday("شنبه"))
    }

    @Test
    fun nextOrSameWeekdayDoesNotShiftBackOneDay() {
        val currentDay = JalaliDate(1405, 7, 7) // Tuesday
        val thursday = com.example.util.JalaliCalendar.nextOrSameDayOfWeek(currentDay, 5)
        assertEquals(JalaliDate(1405, 7, 9), thursday)
    }
}
