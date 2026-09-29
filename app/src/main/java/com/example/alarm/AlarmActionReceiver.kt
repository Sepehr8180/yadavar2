package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DISMISS = "com.example.yadavar.ACTION_DISMISS"
        const val ACTION_SNOOZE = "com.example.yadavar.ACTION_SNOOZE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_TITLE) ?: "یادآور"
        val tag = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_TAG) ?: "یادآور"
        val prio = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_PRIO) ?: "high"

        // Stop foreground service immediately
        val stopIntent = Intent(context, AlarmService::class.java).apply {
            this.action = AlarmService.ACTION_STOP_RINGING
        }
        context.startService(stopIntent)

        if (action == ACTION_SNOOZE) {
            val scheduler = AlarmScheduler(context)
            scheduler.scheduleSnooze(alarmId, title, prio, tag, snoozeMinutes = 10)
        }
    }
}
