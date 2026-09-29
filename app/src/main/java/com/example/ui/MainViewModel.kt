package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.YadavarApp
import com.example.alarm.AlarmReceiver
import com.example.alarm.AlarmScheduler
import com.example.data.AlarmEntity
import com.example.data.BirthdayEntity
import com.example.util.GeminiAiParser
import com.example.util.JalaliCalendar
import com.example.util.JalaliDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val yadavarApp = application as YadavarApp
    private val repository = yadavarApp.repository
    private val scheduler = yadavarApp.alarmScheduler
    private val prefs: SharedPreferences = application.getSharedPreferences("yadavar_prefs", Context.MODE_PRIVATE)

    val allAlarms: StateFlow<List<AlarmEntity>> = repository.allAlarms
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allBirthdays: StateFlow<List<BirthdayEntity>> = repository.allBirthdays
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _selectedDate = MutableStateFlow(JalaliCalendar.today())
    val selectedDate: StateFlow<JalaliDate> = _selectedDate.asStateFlow()

    private val _calendarYear = MutableStateFlow(JalaliCalendar.today().year)
    val calendarYear: StateFlow<Int> = _calendarYear.asStateFlow()

    private val _calendarMonth = MutableStateFlow(JalaliCalendar.today().month)
    val calendarMonth: StateFlow<Int> = _calendarMonth.asStateFlow()

    private val _filterTag = MutableStateFlow<String?>(null)
    val filterTag: StateFlow<String?> = _filterTag.asStateFlow()

    private val _filterPrio = MutableStateFlow<String?>(null)
    val filterPrio: StateFlow<String?> = _filterPrio.asStateFlow()

    private val _showAddEditSheet = MutableStateFlow(false)
    val showAddEditSheet: StateFlow<Boolean> = _showAddEditSheet.asStateFlow()

    private val _editingAlarm = MutableStateFlow<AlarmEntity?>(null)
    val editingAlarm: StateFlow<AlarmEntity?> = _editingAlarm.asStateFlow()

    private val _showAddBirthdaySheet = MutableStateFlow(false)
    val showAddBirthdaySheet: StateFlow<Boolean> = _showAddBirthdaySheet.asStateFlow()

    private val _showAiSettingsDialog = MutableStateFlow(false)
    val showAiSettingsDialog: StateFlow<Boolean> = _showAiSettingsDialog.asStateFlow()

    private val _isAiProcessing = MutableStateFlow(false)
    val isAiProcessing: StateFlow<Boolean> = _isAiProcessing.asStateFlow()

    private val _customApiKey = MutableStateFlow(prefs.getString("custom_gemini_api_key", "") ?: "")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _initialQuadrant = MutableStateFlow("urgent")
    val initialQuadrant: StateFlow<String> = _initialQuadrant.asStateFlow()

    init {
        seedInitialDataIfEmpty()
    }

    private fun seedInitialDataIfEmpty() {
        // Production installs start with an empty database.
        // Demo data should not create unexpected alarms for the user.
    }

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun setSelectedDate(date: JalaliDate) {
        _selectedDate.value = date
        _calendarYear.value = date.year
        _calendarMonth.value = date.month
    }

    fun prevMonth() {
        if (_calendarMonth.value == 1) {
            _calendarMonth.value = 12
            _calendarYear.value -= 1
        } else {
            _calendarMonth.value -= 1
        }
    }

    fun nextMonth() {
        if (_calendarMonth.value == 12) {
            _calendarMonth.value = 1
            _calendarYear.value += 1
        } else {
            _calendarMonth.value += 1
        }
    }

    fun setFilterTag(tag: String?) {
        _filterTag.value = if (_filterTag.value == tag) null else tag
    }

    fun setFilterPrio(prio: String?) {
        _filterPrio.value = if (_filterPrio.value == prio) null else prio
    }

    fun openAddAlarm(defaultPrio: String = "normal") {
        _editingAlarm.value = null
        _initialQuadrant.value = defaultPrio
        _showAddEditSheet.value = true
    }

    fun openEditAlarm(alarm: AlarmEntity) {
        _editingAlarm.value = alarm
        _showAddEditSheet.value = true
    }

    fun closeAddEditSheet() {
        _showAddEditSheet.value = false
        _editingAlarm.value = null
    }

    fun openAddBirthday() {
        _showAddBirthdaySheet.value = true
    }

    fun closeAddBirthday() {
        _showAddBirthdaySheet.value = false
    }

    fun openAiSettings() {
        _showAiSettingsDialog.value = true
    }

    fun closeAiSettings() {
        _showAiSettingsDialog.value = false
    }

    fun saveCustomApiKey(key: String) {
        prefs.edit().putString("custom_gemini_api_key", key).apply()
        _customApiKey.value = key
    }

    fun toggleAlarmEnabled(alarm: AlarmEntity) {
        viewModelScope.launch {
            val newStatus = !alarm.isEnabled
            repository.setAlarmEnabled(alarm.id, newStatus)
            val updated = alarm.copy(isEnabled = newStatus)
            if (newStatus) {
                scheduler.scheduleAlarm(updated)
            } else {
                scheduler.cancelAlarm(alarm.id)
            }
        }
    }

    fun toggleAlarmDone(alarm: AlarmEntity) {
        viewModelScope.launch {
            val newDone = !alarm.isDone
            repository.setAlarmDone(alarm.id, newDone)
            if (newDone) {
                scheduler.cancelAlarm(alarm.id)
            } else if (alarm.isEnabled) {
                scheduler.scheduleAlarm(alarm.copy(isDone = false))
            }
        }
    }

    fun updateAlarmPriority(alarm: AlarmEntity, newPrio: String) {
        viewModelScope.launch {
            val updated = alarm.copy(prio = newPrio)
            repository.updateAlarm(updated)
        }
    }

    fun saveAlarm(
        id: Long = 0,
        title: String,
        hour: Int,
        minute: Int,
        isDaily: Boolean,
        repeatDays: String,
        jalaliDate: JalaliDate,
        prio: String,
        tag: String,
        isVibrate: Boolean,
        snoozeMinutes: Int
    ) {
        viewModelScope.launch {
            val alarm = AlarmEntity(
                id = id,
                title = title.ifBlank { "یادآور جدید" },
                hour = hour,
                minute = minute,
                isDaily = isDaily,
                repeatDays = repeatDays,
                jalaliYear = jalaliDate.year,
                jalaliMonth = jalaliDate.month,
                jalaliDay = jalaliDate.day,
                prio = prio,
                tag = tag.ifBlank { "شخصی" },
                isEnabled = true,
                isVibrate = isVibrate,
                snoozeMinutes = snoozeMinutes,
                isDone = false
            )

            val savedId = repository.insertAlarm(alarm)
            val toSchedule = if (id == 0L) alarm.copy(id = savedId) else alarm
            scheduler.scheduleAlarm(toSchedule)
            closeAddEditSheet()
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            scheduler.cancelAlarm(alarm.id)
            repository.deleteAlarm(alarm)
        }
    }

    fun saveBirthday(
        name: String,
        day: Int,
        month: Int,
        year: Int?,
        hasAlarm: Boolean,
        notes: String
    ) {
        viewModelScope.launch {
            val bday = BirthdayEntity(
                name = name,
                day = day,
                month = month,
                year = year,
                hasAlarm = hasAlarm,
                notes = notes
            )
            repository.insertBirthday(bday)

            // If morning alarm is enabled, also create an annual/scheduled alarm for this birthday
            if (hasAlarm) {
                val today = JalaliCalendar.today()
                val targetYear = if (month < today.month || (month == today.month && day < today.day)) {
                    today.year + 1
                } else today.year

                val bdayAlarm = AlarmEntity(
                    title = "🎂 تولد $name",
                    hour = 9,
                    minute = 0,
                    isDaily = false,
                    repeatDays = "",
                    jalaliYear = targetYear,
                    jalaliMonth = month,
                    jalaliDay = day,
                    prio = "high",
                    tag = "تولد",
                    isEnabled = true
                )
                val aId = repository.insertAlarm(bdayAlarm)
                scheduler.scheduleAlarm(bdayAlarm.copy(id = aId))
            }
            closeAddBirthday()
        }
    }

    fun deleteBirthday(bday: BirthdayEntity) {
        viewModelScope.launch {
            repository.deleteBirthday(bday)
        }
    }

    /**
     * AI Natural Language & Voice Parser
     */
    fun processVoiceOrTextWithAi(text: String, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            _isAiProcessing.value = true
            try {
                val parsed = GeminiAiParser.parsePrompt(
                    rawText = text,
                    today = JalaliCalendar.today(),
                    customApiKey = _customApiKey.value
                )

                val alarm = AlarmEntity(
                    title = parsed.title,
                    hour = parsed.hour,
                    minute = parsed.minute,
                    isDaily = parsed.repeat == "daily",
                    // Preserve the weekday implied by the AI-selected Jalali date for weekly reminders.
                    repeatDays = if (parsed.repeat == "weekly") {
                        JalaliCalendar.dayOfWeek(
                            parsed.jalaliDate.year,
                            parsed.jalaliDate.month,
                            parsed.jalaliDate.day
                        ).toString()
                    } else "",
                    jalaliYear = parsed.jalaliDate.year,
                    jalaliMonth = parsed.jalaliDate.month,
                    jalaliDay = parsed.jalaliDate.day,
                    prio = parsed.prio,
                    tag = parsed.tags.firstOrNull() ?: "شخصی",
                    isEnabled = true
                )

                val savedId = repository.insertAlarm(alarm)
                scheduler.scheduleAlarm(alarm.copy(id = savedId))

                val formattedTime = JalaliCalendar.formatTime(parsed.hour, parsed.minute)
                val tagsStr = parsed.tags.joinToString(" ") { "#$it" }
                val prioLabel = when (parsed.prio) {
                    "urgent" -> "فوری"
                    "high" -> "مهم"
                    "low" -> "کم"
                    else -> "عادی"
                }

                onComplete("⏰ آلارم «${parsed.title}» برای ساعت $formattedTime تنظیم شد ($tagsStr · اولویت $prioLabel)")
            } catch (e: Exception) {
                onComplete("خطا در پردازش هوش مصنوعی: ${e.message}")
            } finally {
                _isAiProcessing.value = false
            }
        }
    }

    fun testAlarmTrigger(title: String = "آزمایش زنگ گوشی یادآور", prio: String = "urgent", tag: String = "تست") {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = "com.example.yadavar.ACTION_ALARM_TRIGGER"
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, 99999L)
                putExtra(AlarmScheduler.EXTRA_ALARM_TITLE, title)
                putExtra(AlarmScheduler.EXTRA_ALARM_TAG, tag)
                putExtra(AlarmScheduler.EXTRA_ALARM_PRIO, prio)
                putExtra(AlarmScheduler.EXTRA_IS_SNOOZE, false)
            }
            context.sendBroadcast(intent)
        }
    }
}
