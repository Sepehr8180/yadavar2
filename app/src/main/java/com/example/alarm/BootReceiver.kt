package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.YadavarDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("BootReceiver", "Received action: $action, rescheduling alarms...")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = YadavarDatabase.getInstance(context)
                val activeAlarms = db.alarmDao().getActiveAlarms()
                val scheduler = AlarmScheduler(context)

                for (alarm in activeAlarms) {
                    scheduler.scheduleAlarm(alarm)
                }
                Log.d("BootReceiver", "Rescheduled ${activeAlarms.size} active alarms successfully.")
            } catch (e: Exception) {
                Log.e("BootReceiver", "Error rescheduling alarms on boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
