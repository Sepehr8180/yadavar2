package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.util.Log
import com.example.MainActivity
import com.example.R
import com.example.alarm.AlarmScheduler
import com.example.data.AlarmEntity
import com.example.data.BirthdayEntity
import com.example.data.FinancialEntity
import com.example.data.YadavarDatabase
import com.example.util.JalaliCalendar
import com.example.util.JalaliDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

/** Shared renderer and refresh coordinator for all Yadavar home-screen widgets. */
object WidgetUpdater {
    private const val TAG = "YadavarWidgets"
    const val ACTION_TOGGLE_DONE = "com.aistudio.yadavar.alrm.widget.TOGGLE_DONE"
    const val EXTRA_ALARM_ID = "widget_alarm_id"
    const val EXTRA_WIDGET_TAB = "widget_tab"
    const val EXTRA_OPEN_ADD_ALARM = "widget_open_add_alarm"
    const val TAB_ALARMS = 0
    const val TAB_CALENDAR = 1
    const val TAB_MATRIX = 2
    const val TAB_FINANCIAL = 3
    const val TAB_BIRTHDAYS = 5

    fun refreshAll(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            refreshAllSuspending(appContext)
        }
    }

    suspend fun refreshAllSuspending(context: Context) {
        val appContext = context.applicationContext
        try {
            val database = YadavarDatabase.getInstance(appContext)
            val alarms = database.alarmDao().getAllAlarms().first()
            val birthdays = database.birthdayDao().getAllBirthdays().first()
            val financialItems = database.financialDao().getAllFinancialItems().first()
            val manager = AppWidgetManager.getInstance(appContext)
            manager.getAppWidgetIds(ComponentName(appContext, EisenhowerMatrixWidgetProvider::class.java))
                .forEach { id -> manager.updateAppWidget(id, matrixRemoteViews(appContext, id, alarms)) }
            manager.getAppWidgetIds(ComponentName(appContext, TodayWidgetProvider::class.java))
                .forEach { id -> manager.updateAppWidget(id, todayRemoteViews(appContext, id, alarms, birthdays, financialItems)) }
            manager.getAppWidgetIds(ComponentName(appContext, VoiceReminderWidgetProvider::class.java))
                .forEach { id -> manager.updateAppWidget(id, voiceRemoteViews(appContext)) }
        } catch (e: Exception) {
            Log.e(TAG, "Could not refresh widgets", e)
        }
    }

    fun openTabPendingIntent(context: Context, tab: Int, requestCode: Int = 300 + tab): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_WIDGET_TAB, tab)
        }
        return PendingIntent.getActivity(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun newAlarmPendingIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_WIDGET_TAB, TAB_ALARMS)
            putExtra(EXTRA_OPEN_ADD_ALARM, true)
        }
        return PendingIntent.getActivity(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun matrixRemoteViews(context: Context, widgetId: Int, allAlarms: List<AlarmEntity>): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_eisenhower_matrix)
        views.setTextViewText(R.id.widget_matrix_date, JalaliCalendar.today().formatted(includeDayName = true))
        views.setOnClickPendingIntent(R.id.widget_matrix_header, openTabPendingIntent(context, TAB_MATRIX))
        views.setOnClickPendingIntent(R.id.widget_matrix_add, newAlarmPendingIntent(context, 320 + widgetId))

        val tasks = allAlarms.filter {
            !it.isDone && it.showInMatrix && it.prio != "none" && !AlarmScheduler.isBirthdayAlarm(it)
        }
        renderQuadrant(context, views, R.id.widget_tasks_urgent, tasks.filter { it.prio == "urgent" }, widgetId)
        renderQuadrant(context, views, R.id.widget_tasks_high, tasks.filter { it.prio == "high" }, widgetId)
        renderQuadrant(context, views, R.id.widget_tasks_normal, tasks.filter { it.prio == "normal" }, widgetId)
        renderQuadrant(context, views, R.id.widget_tasks_low, tasks.filter { it.prio == "low" }, widgetId)
        return views
    }

    private fun renderQuadrant(
        context: Context,
        parent: RemoteViews,
        containerId: Int,
        tasks: List<AlarmEntity>,
        widgetId: Int
    ) {
        parent.removeAllViews(containerId)
        if (tasks.isEmpty()) {
            val empty = RemoteViews(context.packageName, R.layout.widget_empty_row)
            empty.setTextViewText(R.id.widget_empty_text, "کاری نیست")
            parent.addView(containerId, empty)
            return
        }
        val shownTasks = tasks.sortedBy { it.hour * 60 + it.minute }.take(2)
        shownTasks.forEach { alarm ->
            val row = RemoteViews(context.packageName, R.layout.widget_task_row)
            row.setTextViewText(R.id.widget_task_title, alarm.title)
            row.setTextViewText(R.id.widget_task_check, "□")
            row.setOnClickPendingIntent(
                R.id.widget_task_check,
                toggleDonePendingIntent(context, alarm.id, requestCodeForTask(widgetId, alarm.id, 100_000_000))
            )
            row.setOnClickPendingIntent(
                R.id.widget_task_title,
                openTabPendingIntent(context, TAB_MATRIX, 500000 + alarm.id.toInt())
            )
            parent.addView(containerId, row)
        }
        val remaining = tasks.size - shownTasks.size
        if (remaining > 0) {
            val more = RemoteViews(context.packageName, R.layout.widget_empty_row)
            more.setTextViewText(R.id.widget_empty_text, "+ ${JalaliCalendar.toPersianDigits(remaining.toString())} مورد دیگر")
            parent.addView(containerId, more)
        }
    }

    private fun todayRemoteViews(
        context: Context,
        widgetId: Int,
        allAlarms: List<AlarmEntity>,
        birthdays: List<BirthdayEntity>,
        financialItems: List<FinancialEntity>
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_today)
        val today = JalaliCalendar.today()
        views.setTextViewText(R.id.widget_today_date, today.formatted(includeDayName = true))
        views.setOnClickPendingIntent(R.id.widget_today_header, openTabPendingIntent(context, TAB_CALENDAR))

        val dow = JalaliCalendar.dayOfWeek(today.year, today.month, today.day).toString()
        val dueToday = allAlarms.filter { alarm ->
            if (alarm.isDone || !alarm.isEnabled || !alarm.hasDate || AlarmScheduler.isBirthdayAlarm(alarm)) return@filter false
            when {
                alarm.isDaily -> true
                alarm.parseRepeatDays().isNotEmpty() -> dow in alarm.parseRepeatDays().map { it.toString() }.toSet()
                else -> alarm.jalaliYear == today.year && alarm.jalaliMonth == today.month && alarm.jalaliDay == today.day
            }
        }.sortedBy { it.hour * 60 + it.minute }

        views.removeAllViews(R.id.widget_today_tasks)
        if (dueToday.isEmpty()) {
            val empty = RemoteViews(context.packageName, R.layout.widget_empty_row)
            empty.setTextViewText(R.id.widget_empty_text, "کار زمان‌بندی‌شده‌ای برای امروز نیست")
            views.addView(R.id.widget_today_tasks, empty)
        } else {
            dueToday.take(2).forEach { alarm ->
                val row = RemoteViews(context.packageName, R.layout.widget_task_row)
                row.setTextViewText(R.id.widget_task_title, alarm.title)
                row.setTextViewText(R.id.widget_task_check, "□")
                row.setOnClickPendingIntent(
                    R.id.widget_task_check,
                    toggleDonePendingIntent(context, alarm.id, requestCodeForTask(widgetId, alarm.id, 700_000_000))
                )
                row.setOnClickPendingIntent(
                    R.id.widget_task_title,
                    openTabPendingIntent(context, TAB_ALARMS, 700000 + alarm.id.toInt())
                )
                views.addView(R.id.widget_today_tasks, row)
            }
            if (dueToday.size > 2) {
                val more = RemoteViews(context.packageName, R.layout.widget_empty_row)
                more.setTextViewText(R.id.widget_empty_text, "+ ${JalaliCalendar.toPersianDigits((dueToday.size - 2).toString())} کار دیگر")
                views.addView(R.id.widget_today_tasks, more)
            }
        }

        val nextAlarm = nextScheduledAlarm(context, allAlarms)
        val alarmText = if (nextAlarm == null) "آلارم فعال بعدی: ندارد" else {
            val calendar = Calendar.getInstance().apply { timeInMillis = nextAlarm.second }
            "${nextAlarm.first.title} · ${JalaliCalendar.formatTime(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))}"
        }
        views.setTextViewText(R.id.widget_today_next_alarm, alarmText)
        views.setOnClickPendingIntent(R.id.widget_today_alarm_row, openTabPendingIntent(context, TAB_ALARMS, 810 + widgetId))

        val nextBirthday = birthdays.map { it to it.nextOccurrenceInfo(today) }
            .minByOrNull { it.second.first }
        val birthdayText = when {
            nextBirthday == null -> "تولد بعدی: ثبت نشده"
            nextBirthday.second.first == 0 -> "تولد امروز: ${nextBirthday.first.name}"
            else -> "${nextBirthday.first.name} · ${JalaliCalendar.toPersianDigits(nextBirthday.second.first.toString())} روز تا تولد"
        }
        views.setTextViewText(R.id.widget_today_birthday, birthdayText)
        views.setOnClickPendingIntent(R.id.widget_today_birthday_row, openTabPendingIntent(context, TAB_BIRTHDAYS, 820 + widgetId))

        val nextPayment = nextUnpaidPayment(financialItems, today)
        val paymentText = if (nextPayment == null) "قسط بعدی: مورد پرداخت‌نشده‌ای نیست" else {
            "${nextPayment.first.title} · ${JalaliCalendar.formatNumber(nextPayment.first.amount)} · ${nextPayment.second.formatted()}"
        }
        views.setTextViewText(R.id.widget_today_payment, paymentText)
        views.setOnClickPendingIntent(R.id.widget_today_payment_row, openTabPendingIntent(context, TAB_FINANCIAL, 830 + widgetId))
        return views
    }

    private fun nextScheduledAlarm(context: Context, alarms: List<AlarmEntity>): Pair<AlarmEntity, Long>? {
        val candidates = alarms.filter {
            it.hasAlarm && it.hasDate && it.isEnabled && !it.isDone && !AlarmScheduler.isBirthdayAlarm(it)
        }
        if (candidates.isEmpty()) return null
        // Calculate using the same scheduler rules as the app; fallback to stored time if needed.
        val scheduler = AlarmScheduler(context)
        return candidates.mapNotNull { alarm ->
            val trigger = runCatching { scheduler.calculateNextTriggerMillis(alarm) }.getOrDefault(Long.MAX_VALUE)
            if (trigger > System.currentTimeMillis() && trigger < Long.MAX_VALUE) alarm to trigger else null
        }.minByOrNull { it.second }
    }

    private fun nextUnpaidPayment(items: List<FinancialEntity>, today: JalaliDate): Pair<FinancialEntity, JalaliDate>? {
        val candidates = mutableListOf<Pair<FinancialEntity, JalaliDate>>()
        items.forEach { item ->
            if (!item.isMonthly) {
                val occurrence = item.startDate()
                if (!item.isPaidOn(occurrence)) candidates += item to occurrence
            } else {
                for (offset in 0..24) {
                    var year = today.year
                    var month = today.month + offset
                    while (month > 12) { month -= 12; year++ }
                    val occurrence = item.occurrenceInMonth(year, month) ?: continue
                    if (!item.isPaidOn(occurrence)) {
                        candidates += item to occurrence
                        break
                    }
                }
            }
        }
        return candidates.minWithOrNull(compareBy<Pair<FinancialEntity, JalaliDate>> { it.second }.thenBy { it.first.amount })
    }

    private fun requestCodeForTask(widgetId: Int, alarmId: Long, base: Int): Int {
        val value = base.toLong() + widgetId.toLong() * 1_000_000L + (alarmId % 1_000_000L)
        return (value % Int.MAX_VALUE).toInt()
    }

    private fun toggleDonePendingIntent(context: Context, alarmId: Long, requestCode: Int): PendingIntent {
        val intent = Intent(context, WidgetActionReceiver::class.java).apply {
            action = ACTION_TOGGLE_DONE
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun voiceRemoteViews(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_voice_reminder)
        val intent = Intent(context, VoiceReminderActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pending = PendingIntent.getActivity(
            context, 901, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_voice_root, pending)
        return views
    }

}
