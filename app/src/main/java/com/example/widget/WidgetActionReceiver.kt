package com.example.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.YadavarApp
import com.example.data.YadavarDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != WidgetUpdater.ACTION_TOGGLE_DONE) return
        val alarmId = intent.getLongExtra(WidgetUpdater.EXTRA_ALARM_ID, -1L)
        if (alarmId <= 0L) return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val database = YadavarDatabase.getInstance(appContext)
                val alarm = database.alarmDao().getAlarmById(alarmId) ?: return@launch
                if (!alarm.isDone) {
                    database.alarmDao().updateAlarmDone(alarmId, true)
                    (appContext as? YadavarApp)?.alarmScheduler?.cancelAlarm(alarmId)
                }
                WidgetUpdater.refreshAllSuspending(appContext)
            } catch (e: Exception) {
                Log.e("YadavarWidgetAction", "Could not complete reminder from widget", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
