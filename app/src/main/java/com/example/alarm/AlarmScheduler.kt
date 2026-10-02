package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.AlarmEntity
import com.example.util.JalaliCalendar
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        const val TAG = "AlarmScheduler"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_ALARM_TITLE = "extra_alarm_title"
        const val EXTRA_ALARM_TAG = "extra_alarm_tag"
        const val EXTRA_ALARM_PRIO = "extra_alarm_prio"
        const val EXTRA_IS_SNOOZE = "extra_is_snooze"
    }

    fun scheduleAlarm(alarm: AlarmEntity) {
        if (!alarm.hasAlarm || !alarm.hasDate || !alarm.isEnabled || alarm.isDone) {
            cancelAlarm(alarm.id)
            return
        }

        val triggerAtMillis = calculateNextTriggerMillis(alarm)
        if (triggerAtMillis <= System.currentTimeMillis()) {
            Log.w(TAG, "Trigger time is in the past for alarm ${alarm.id}")
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.yadavar.ACTION_ALARM_TRIGGER"
            putExtra(EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_ALARM_TITLE, alarm.title)
            putExtra(EXTRA_ALARM_TAG, alarm.tag)
            putExtra(EXTRA_ALARM_PRIO, alarm.prio)
            putExtra(EXTRA_IS_SNOOZE, false)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Show pending intent when user taps the alarm clock icon in system status bar
        val showIntent = Intent(context, com.example.MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            alarm.id.toInt(),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    val clockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)
                    alarmManager.setAlarmClock(clockInfo, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else {
                val clockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)
                alarmManager.setAlarmClock(clockInfo, pendingIntent)
            }
            Log.d(TAG, "Alarm ${alarm.id} scheduled for $triggerAtMillis")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling exact alarm", e)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm", e)
        }
    }

    fun scheduleSnooze(alarmId: Long, title: String, prio: String, tag: String, snoozeMinutes: Int = 10) {
        val triggerAtMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.yadavar.ACTION_ALARM_TRIGGER"
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_TITLE, "$title (تعویق)")
            putExtra(EXTRA_ALARM_TAG, tag)
            putExtra(EXTRA_ALARM_PRIO, prio)
            putExtra(EXTRA_IS_SNOOZE, true)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (alarmId + 100000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, com.example.MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            (alarmId + 100000).toInt(),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val clockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)
            alarmManager.setAlarmClock(clockInfo, pendingIntent)
        } catch (e: Exception) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelAlarm(alarmId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.yadavar.ACTION_ALARM_TRIGGER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }

        // Cancel potential snooze
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (alarmId + 100000).toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (snoozePendingIntent != null) {
            alarmManager.cancel(snoozePendingIntent)
            snoozePendingIntent.cancel()
        }
    }

    fun calculateNextTriggerMillis(alarm: AlarmEntity): Long {
        if (!alarm.hasDate) return Long.MAX_VALUE
        val now = Calendar.getInstance()
        val repeatDays = alarm.parseRepeatDays()

        if (alarm.isDaily) {
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (target.timeInMillis <= now.timeInMillis) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis
        }

        if (repeatDays.isNotEmpty()) {
            for (dayOffset in 0..7) {
                val candidate = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, dayOffset)
                    set(Calendar.HOUR_OF_DAY, alarm.hour)
                    set(Calendar.MINUTE, alarm.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                // Check if already passed today
                if (dayOffset == 0 && candidate.timeInMillis <= now.timeInMillis) {
                    continue
                }

                // Check day of week in Persian: 0=Saturday..6=Friday
                val dowPersian = when (candidate.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.SATURDAY -> 0
                    Calendar.SUNDAY -> 1
                    Calendar.MONDAY -> 2
                    Calendar.TUESDAY -> 3
                    Calendar.WEDNESDAY -> 4
                    Calendar.THURSDAY -> 5
                    Calendar.FRIDAY -> 6
                    else -> 0
                }

                if (dowPersian in repeatDays) {
                    return candidate.timeInMillis
                }
            }
        }

        // Specific Jalali date
        val (gy, gm, gd) = JalaliCalendar.jalaliToGregorian(alarm.jalaliYear, alarm.jalaliMonth, alarm.jalaliDay)
        val target = Calendar.getInstance().apply {
            set(gy, gm - 1, gd, alarm.hour, alarm.minute, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return target.timeInMillis
    }

    fun formatRemainingTime(alarm: AlarmEntity): String {
        val next = calculateNextTriggerMillis(alarm)
        val diff = next - System.currentTimeMillis()
        if (diff <= 0) return "زمان زنگ رسیده است"

        val totalMinutes = diff / (60 * 1000)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        val days = hours / 24
        val remHours = hours % 24

        return when {
            days > 0 -> "زنگ در ${JalaliCalendar.toPersianDigits(days.toString())} روز و ${JalaliCalendar.toPersianDigits(remHours.toString())} ساعت دیگر"
            hours > 0 -> "زنگ در ${JalaliCalendar.toPersianDigits(hours.toString())} ساعت و ${JalaliCalendar.toPersianDigits(minutes.toString())} دقیقه دیگر"
            minutes > 0 -> "زنگ در ${JalaliCalendar.toPersianDigits(minutes.toString())} دقیقه دیگر"
            else -> "زنگ در کمتر از ۱ دقیقه دیگر"
        }
    }
}
