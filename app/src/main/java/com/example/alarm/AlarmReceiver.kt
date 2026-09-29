package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.data.YadavarDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_TITLE) ?: "زمان یادآوری!"
        val tag = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_TAG) ?: "یادآور"
        val prio = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_PRIO) ?: "high"
        val isSnooze = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_SNOOZE, false)

        // Start Alarm Foreground Service to play sound and vibrate
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START_RINGING
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_ALARM_TITLE, title)
            putExtra(AlarmScheduler.EXTRA_ALARM_TAG, tag)
            putExtra(AlarmScheduler.EXTRA_ALARM_PRIO, prio)
        }
        ContextCompat.startForegroundService(context, serviceIntent)

        // Launch full-screen ringing UI
        val activityIntent = Intent(context, AlarmRingingActivity::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_ALARM_TITLE, title)
            putExtra(AlarmScheduler.EXTRA_ALARM_TAG, tag)
            putExtra(AlarmScheduler.EXTRA_ALARM_PRIO, prio)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(activityIntent)

        // If not snooze and one-time alarm, update DB or re-schedule next occurrence
        if (!isSnooze && alarmId > 0) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = YadavarDatabase.getInstance(context)
                val alarm = db.alarmDao().getAlarmById(alarmId)
                if (alarm != null) {
                    if (alarm.isDaily || alarm.parseRepeatDays().isNotEmpty()) {
                        // Re-schedule for next occurrence
                        val scheduler = AlarmScheduler(context)
                        scheduler.scheduleAlarm(alarm)
                    } else {
                        // Single-time alarm finished
                        db.alarmDao().updateAlarmStatus(alarmId, false)
                    }
                }
            }
        }
    }
}
