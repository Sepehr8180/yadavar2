package com.example.util

import java.util.Calendar

data class JalaliDate(
    val year: Int,
    val month: Int,
    val day: Int
) : Comparable<JalaliDate> {
    override fun compareTo(other: JalaliDate): Int {
        if (year != other.year) return year.compareTo(other.year)
        if (month != other.month) return month.compareTo(other.month)
        return day.compareTo(other.day)
    }

    fun formatted(includeDayName: Boolean = false): String {
        val monthName = JalaliCalendar.MON_NAMES.getOrElse(month - 1) { "" }
        val dayName = if (includeDayName) "${JalaliCalendar.dayOfWeekName(JalaliCalendar.dayOfWeek(year, month, day))} " else ""
        return "$dayName${JalaliCalendar.toPersianDigits(day.toString())} $monthName ${JalaliCalendar.toPersianDigits(year.toString())}"
    }

    fun toKey(): String = "$year-$month-$day"
}

object JalaliCalendar {
    val MON_NAMES = listOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    val DOW_NAMES = listOf(
        "شنبه", "یک‌شنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه"
    )

    val DOW_SHORT = listOf(
        "ش", "ی", "د", "س", "چ", "پ", "ج"
    )

    private fun div(a: Int, b: Int): Int = a / b
    private fun mod(a: Int, b: Int): Int = a % b

    fun jalCal(jy: Int, noLeap: Boolean = false): Triple<Int, Int, Int> {
        val breaks = intArrayOf(
            -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210, 1635, 2060,
            2097, 2192, 2262, 2324, 2394, 2456, 3178
        )
        val bl = breaks.size
        val gy = jy + 621
        var leapJ = -14
        var jp = breaks[0]
        var jm = 0
        var jump = 0
        var leap = 0
        val leapG: Int
        val march: Int
        var n = 0

        for (i in 1 until bl) {
            jm = breaks[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += div(jump, 33) * 8 + div(mod(jump, 33), 4)
            jp = jm
        }
        n = jy - jp
        leapJ += div(n, 33) * 8 + div(mod(n, 33) + 3, 4)
        if (mod(jump, 33) == 4 && jump - n == 4) leapJ++
        leapG = div(gy, 4) - div((div(gy, 100) + 1) * 3, 4) - 150
        march = 20 + leapJ - leapG
        if (!noLeap) {
            if (jump - n < 6) n = n - jump + div(jump + 4, 33) * 33
            leap = mod(mod(n + 1, 33) - 1, 4)
            if (leap == -1) leap = 4
        }
        return Triple(leap, gy, march)
    }

    private fun g2d(gy: Int, gm: Int, gd: Int): Int {
        var d = div((gy + div(gm - 8, 6) + 100100) * 1461, 4) +
                div(153 * mod(gm + 9, 12) + 2, 5) +
                gd - 34840408
        d = d - div(div(gy + 100100 + div(gm - 8, 6), 100) * 3, 4) + 752
        return d
    }

    private fun d2g(jdn: Int): Triple<Int, Int, Int> {
        var j = 4 * jdn + 139361631
        j += div(div(4 * jdn + 183187720, 146097) * 3, 4) * 4 - 3908
        val i = div(mod(j, 1461), 4) * 5 + 308
        val gd = div(mod(i, 153), 5) + 1
        val gm = mod(div(i, 153), 12) + 1
        val gy = div(j, 1461) - 100100 + div(8 - gm, 6)
        return Triple(gy, gm, gd)
    }

    private fun j2d(jy: Int, jm: Int, jd: Int): Int {
        val r = jalCal(jy, true)
        return g2d(r.second, 3, r.third) + (jm - 1) * 31 - div(jm, 7) * (jm - 7) + jd - 1
    }

    private fun d2j(jdn: Int): JalaliDate {
        val (gy, _, _) = d2g(jdn)
        var jy = gy - 621
        val r = jalCal(jy, false)
        val jdn1f = g2d(gy, 3, r.third)
        var k = jdn - jdn1f
        val jm: Int
        val jd: Int
        if (k >= 0) {
            if (k <= 185) {
                jm = 1 + div(k, 31)
                jd = mod(k, 31) + 1
                return JalaliDate(jy, jm, jd)
            }
            k -= 186
        } else {
            jy--
            k += 179
            if (r.first == 1) k++
        }
        jm = 7 + div(k, 30)
        jd = mod(k, 30) + 1
        return JalaliDate(jy, jm, jd)
    }

    fun isLeap(jy: Int): Boolean = jalCal(jy, false).first == 0

    fun monthLength(jy: Int, jm: Int): Int {
        return when {
            jm in 1..6 -> 31
            jm in 7..11 -> 30
            isLeap(jy) -> 30
            else -> 29
        }
    }

    fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): Triple<Int, Int, Int> {
        return d2g(j2d(jy, jm, jd))
    }

    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): JalaliDate {
        return d2j(g2d(gy, gm, gd))
    }

    fun today(): JalaliDate {
        val c = Calendar.getInstance()
        return gregorianToJalali(
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH) + 1,
            c.get(Calendar.DAY_OF_MONTH)
        )
    }

    /**
     * Day of week: 0 = Saturday (شنبه), 1 = Sunday, ..., 6 = Friday (جمعه)
     */
    fun dayOfWeek(jy: Int, jm: Int, jd: Int): Int {
        val (gy, gm, gd) = jalaliToGregorian(jy, jm, jd)
        val c = Calendar.getInstance().apply {
            set(gy, gm - 1, gd, 12, 0, 0)
        }
        // Calendar.SUNDAY = 1, MONDAY = 2, ..., SATURDAY = 7
        // In Persian calendar: Saturday is day 0
        return when (c.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }
    }

    fun dayOfWeekName(dow: Int): String = DOW_NAMES.getOrElse(dow) { "" }

    fun addDays(date: JalaliDate, days: Int): JalaliDate {
        return d2j(j2d(date.year, date.month, date.day) + days)
    }

    fun diffDays(a: JalaliDate, b: JalaliDate): Int {
        return j2d(a.year, a.month, a.day) - j2d(b.year, b.month, b.day)
    }

    fun toPersianDigits(text: String): String {
        val persian = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val sb = java.lang.StringBuilder()
        for (ch in text) {
            if (ch in '0'..'9') {
                sb.append(persian[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatTime(hour: Int, minute: Int, persian: Boolean = true): String {
        val raw = String.format("%02d:%02d", hour, minute)
        return if (persian) toPersianDigits(raw) else raw
    }
}
