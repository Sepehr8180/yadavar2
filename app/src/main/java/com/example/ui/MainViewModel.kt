package com.example.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.AlarmScheduler
import com.example.YadavarApp
import com.example.data.AlarmEntity
import com.example.data.BirthdayEntity
import com.example.data.FinancialEntity
import com.example.util.GeminiAiParser
import com.example.util.JalaliCalendar
import com.example.util.JalaliDate
import com.example.widget.WidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val TAB_ALARMS = 0
        const val TAB_CALENDAR = 1
        const val TAB_MATRIX = 2
        const val TAB_FINANCIAL = 3
        const val TAB_COMPLETED = 4
        const val TAB_BIRTHDAYS = 5
    }

    private val yadavarApp = application as YadavarApp
    private val repository = yadavarApp.repository
    private val scheduler = yadavarApp.alarmScheduler
    private val prefs: SharedPreferences = application.getSharedPreferences("yadavar_prefs", Context.MODE_PRIVATE)

    val allAlarms: StateFlow<List<AlarmEntity>> = repository.allAlarms.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allBirthdays: StateFlow<List<BirthdayEntity>> = repository.allBirthdays.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allFinancialItems: StateFlow<List<FinancialEntity>> = repository.allFinancialItems.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _selectedTab = MutableStateFlow(TAB_ALARMS)
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

    private val _showFinancialSheet = MutableStateFlow(false)
    val showFinancialSheet: StateFlow<Boolean> = _showFinancialSheet.asStateFlow()

    private val _editingFinancial = MutableStateFlow<FinancialEntity?>(null)
    val editingFinancial: StateFlow<FinancialEntity?> = _editingFinancial.asStateFlow()

    private val _showAiSettingsDialog = MutableStateFlow(false)
    val showAiSettingsDialog: StateFlow<Boolean> = _showAiSettingsDialog.asStateFlow()

    private val _isAiProcessing = MutableStateFlow(false)
    val isAiProcessing: StateFlow<Boolean> = _isAiProcessing.asStateFlow()

    private val _customApiKey = MutableStateFlow(prefs.getString("custom_gemini_api_key", "") ?: "")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _initialQuadrant = MutableStateFlow("urgent")
    val initialQuadrant: StateFlow<String> = _initialQuadrant.asStateFlow()

    init {
        // Repair/migrate generated birthday alarms for birthdays already stored on the device.
        viewModelScope.launch { reconcileBirthdayAlarms() }
    }

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun openBirthdays() {
        _selectedTab.value = TAB_BIRTHDAYS
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
        } else _calendarMonth.value -= 1
    }

    fun nextMonth() {
        if (_calendarMonth.value == 12) {
            _calendarMonth.value = 1
            _calendarYear.value += 1
        } else _calendarMonth.value += 1
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

    fun openAddBirthday() { _showAddBirthdaySheet.value = true }

    fun closeAddBirthday() { _showAddBirthdaySheet.value = false }

    fun openAddFinancial() {
        _editingFinancial.value = null
        _showFinancialSheet.value = true
    }

    fun openEditFinancial(item: FinancialEntity) {
        _editingFinancial.value = item
        _showFinancialSheet.value = true
    }

    fun closeFinancialSheet() {
        _showFinancialSheet.value = false
        _editingFinancial.value = null
    }

    fun openAiSettings() { _showAiSettingsDialog.value = true }

    fun closeAiSettings() { _showAiSettingsDialog.value = false }

    fun saveCustomApiKey(key: String) {
        prefs.edit().putString("custom_gemini_api_key", key).apply()
        _customApiKey.value = key
    }

    fun toggleAlarmEnabled(alarm: AlarmEntity) {
        viewModelScope.launch {
            val newStatus = !alarm.isEnabled
            repository.setAlarmEnabled(alarm.id, newStatus)
            val updated = alarm.copy(isEnabled = newStatus)
            if (newStatus && alarm.hasAlarm) scheduler.scheduleAlarm(updated)
            else scheduler.cancelAlarm(alarm.id)
            WidgetUpdater.refreshAll(getApplication<Application>())
        }
    }

    fun toggleAlarmDone(alarm: AlarmEntity) {
        viewModelScope.launch {
            val newDone = !alarm.isDone
            repository.setAlarmDone(alarm.id, newDone)
            if (newDone) scheduler.cancelAlarm(alarm.id)
            else if (alarm.isEnabled && alarm.hasAlarm) scheduler.scheduleAlarm(alarm.copy(isDone = false))
            WidgetUpdater.refreshAll(getApplication<Application>())
        }
    }

    fun updateAlarmPriority(alarm: AlarmEntity, newPrio: String) {
        viewModelScope.launch {
            repository.updateAlarm(alarm.copy(prio = newPrio, showInMatrix = newPrio != "none"))
            WidgetUpdater.refreshAll(getApplication<Application>())
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
        hasDate: Boolean,
        prio: String,
        tag: String,
        isVibrate: Boolean,
        snoozeMinutes: Int,
        hasAlarm: Boolean
    ) {
        viewModelScope.launch {
            val alarm = AlarmEntity(
                id = id,
                title = title.ifBlank { "یادآور جدید" },
                hour = hour,
                minute = minute,
                isDaily = isDaily,
                repeatDays = repeatDays,
                jalaliYear = if (hasDate) jalaliDate.year else 0,
                jalaliMonth = if (hasDate) jalaliDate.month else 0,
                jalaliDay = if (hasDate) jalaliDate.day else 0,
                hasDate = hasDate,
                prio = prio,
                tag = tag.ifBlank { "شخصی" },
                isEnabled = true,
                hasAlarm = hasAlarm && hasDate,
                isVibrate = isVibrate,
                showInMatrix = prio != "none",
                snoozeMinutes = snoozeMinutes,
                isDone = false
            )
            val savedId = repository.insertAlarm(alarm)
            val toSchedule = if (id == 0L) alarm.copy(id = savedId) else alarm
            if (toSchedule.hasAlarm) scheduler.scheduleAlarm(toSchedule) else scheduler.cancelAlarm(toSchedule.id)
            closeAddEditSheet()
            WidgetUpdater.refreshAll(getApplication<Application>())
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            scheduler.cancelAlarm(alarm.id)
            repository.deleteAlarm(alarm)
            WidgetUpdater.refreshAll(getApplication<Application>())
        }
    }

    fun saveBirthday(name: String, day: Int, month: Int, year: Int?, notes: String) {
        viewModelScope.launch {
            val bday = BirthdayEntity(name = name, day = day, month = month, year = year, hasAlarm = true, notes = notes)
            repository.insertBirthday(bday)
            ensureBirthdayAlarm(bday)
            closeAddBirthday()
            WidgetUpdater.refreshAll(getApplication<Application>())
        }
    }

    fun deleteBirthday(bday: BirthdayEntity) {
        viewModelScope.launch {
            val birthdayTitle = "${AlarmScheduler.BIRTHDAY_TITLE_PREFIX}${bday.name}"
            repository.allAlarms.first()
                .filter { AlarmScheduler.isBirthdayAlarm(it) && it.title == birthdayTitle }
                .forEach { alarm ->
                    scheduler.cancelAlarm(alarm.id)
                    repository.deleteAlarm(alarm)
                }
            repository.deleteBirthday(bday)
            WidgetUpdater.refreshAll(getApplication<Application>())
        }
    }

    private fun nextBirthdayOccurrence(birthday: BirthdayEntity): JalaliDate {
        val today = JalaliCalendar.today()
        fun dateForYear(year: Int) = JalaliDate(
            year = year,
            month = birthday.month,
            day = minOf(birthday.day, JalaliCalendar.monthLength(year, birthday.month))
        )
        var target = dateForYear(today.year)
        if (JalaliCalendar.diffDays(target, today) <= 0) target = dateForYear(today.year + 1)
        return target
    }

    private suspend fun ensureBirthdayAlarm(birthday: BirthdayEntity) {
        if (!birthday.hasAlarm) repository.updateBirthday(birthday.copy(hasAlarm = true))
        val alarmDate = JalaliCalendar.addDays(nextBirthdayOccurrence(birthday), -1)
        val title = "${AlarmScheduler.BIRTHDAY_TITLE_PREFIX}${birthday.name}"
        val matching = repository.allAlarms.first()
            .filter { AlarmScheduler.isBirthdayAlarm(it) && it.title == title }
        val existing = matching.firstOrNull()
        val base = existing?.copy(
            hour = 22,
            minute = 0,
            isDaily = false,
            repeatDays = "",
            jalaliYear = alarmDate.year,
            jalaliMonth = alarmDate.month,
            jalaliDay = alarmDate.day,
            hasDate = true,
            prio = "high",
            tag = AlarmScheduler.BIRTHDAY_TAG,
            isEnabled = true,
            hasAlarm = true,
            isDone = false,
            showInMatrix = false
        ) ?: AlarmEntity(
            title = title,
            hour = 22,
            minute = 0,
            isDaily = false,
            repeatDays = "",
            jalaliYear = alarmDate.year,
            jalaliMonth = alarmDate.month,
            jalaliDay = alarmDate.day,
            hasDate = true,
            prio = "high",
            tag = AlarmScheduler.BIRTHDAY_TAG,
            isEnabled = true,
            hasAlarm = true,
            showInMatrix = false
        )

        matching.drop(1).forEach { duplicate ->
            scheduler.cancelAlarm(duplicate.id)
            repository.deleteAlarm(duplicate)
        }
        val saved = if (existing != null) {
            scheduler.cancelAlarm(existing.id)
            repository.updateAlarm(base)
            base
        } else {
            val id = repository.insertAlarm(base)
            base.copy(id = id)
        }
        scheduler.scheduleAlarm(saved)
    }

    private suspend fun reconcileBirthdayAlarms() {
        val birthdays = repository.allBirthdays.first()
        birthdays.forEach { birthday -> ensureBirthdayAlarm(birthday) }
        WidgetUpdater.refreshAll(getApplication<Application>())
    }

    fun saveFinancial(
        id: Long = 0,
        title: String,
        amount: Long,
        date: JalaliDate,
        isMonthly: Boolean,
        endDate: JalaliDate?,
        notes: String
    ) {
        viewModelScope.launch {
            if (title.isBlank() || amount <= 0) return@launch
            val item = FinancialEntity(
                id = id,
                title = title.trim(),
                amount = amount,
                jalaliYear = date.year,
                jalaliMonth = date.month,
                jalaliDay = date.day,
                isMonthly = isMonthly,
                endJalaliYear = endDate?.year,
                endJalaliMonth = endDate?.month,
                endJalaliDay = endDate?.day,
                notes = notes.trim(),
                paidDates = _editingFinancial.value?.paidDates ?: ""
            )
            repository.insertFinancialItem(item)
            closeFinancialSheet()
            WidgetUpdater.refreshAll(getApplication<Application>())
        }
    }

    fun toggleFinancialPaid(item: FinancialEntity, occurrence: JalaliDate) {
        viewModelScope.launch {
            repository.updateFinancialItem(item.setPaidOn(occurrence, !item.isPaidOn(occurrence)))
            WidgetUpdater.refreshAll(getApplication<Application>())
        }
    }

    fun deleteFinancial(item: FinancialEntity) {
        viewModelScope.launch {
            repository.deleteFinancialItem(item)
            WidgetUpdater.refreshAll(getApplication<Application>())
        }
    }

    fun processVoiceOrTextWithAi(text: String, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            _isAiProcessing.value = true
            try {
                val parsed = GeminiAiParser.parsePrompt(
                    rawText = text,
                    today = JalaliCalendar.today(),
                    customApiKey = _customApiKey.value
                )

                val explicitDow = GeminiAiParser.detectWeekday(text)
                val repeatWeekly = parsed.repeat == "weekly" || explicitDow != null
                val correctedDate = explicitDow?.let {
                    JalaliCalendar.nextOrSameDayOfWeek(JalaliCalendar.today(), it)
                } ?: parsed.jalaliDate
                val repeatDays = if (repeatWeekly) {
                    (explicitDow ?: JalaliCalendar.dayOfWeek(correctedDate.year, correctedDate.month, correctedDate.day)).toString()
                } else ""

                val noAlarm = GeminiAiParser.requestsNoAlarm(text)
                val noDate = GeminiAiParser.requestsNoDate(text)
                val explicitlyExcludedFromMatrix = GeminiAiParser.requestsNoMatrix(text)
                // Never let an AI guess accidentally opt out: "none" is reserved for an explicit user request.
                val effectivePriority = if (explicitlyExcludedFromMatrix) "none" else (parsed.prio.takeUnless { it == "none" } ?: "normal")
                // AI reminders belong in the matrix by default; only an explicit opt-out excludes them.
                val showInMatrix = !explicitlyExcludedFromMatrix
                val alarm = AlarmEntity(
                    title = parsed.title,
                    hour = parsed.hour,
                    minute = parsed.minute,
                    isDaily = parsed.repeat == "daily",
                    repeatDays = repeatDays,
                    jalaliYear = if (noDate) 0 else correctedDate.year,
                    jalaliMonth = if (noDate) 0 else correctedDate.month,
                    jalaliDay = if (noDate) 0 else correctedDate.day,
                    hasDate = !noDate,
                    prio = effectivePriority,
                    tag = parsed.tags.firstOrNull() ?: "شخصی",
                    isEnabled = true,
                    hasAlarm = !noAlarm && !noDate,
                    showInMatrix = showInMatrix
                )

                val savedId = repository.insertAlarm(alarm)
                val saved = alarm.copy(id = savedId)
                if (saved.hasAlarm) scheduler.scheduleAlarm(saved)
                WidgetUpdater.refreshAll(getApplication<Application>())

                val formattedTime = JalaliCalendar.formatTime(parsed.hour, parsed.minute)
                val tagsStr = parsed.tags.joinToString(" ") { "#$it" }
                val prioLabel = when (effectivePriority) {
                    "urgent" -> "فوری"
                    "high" -> "مهم"
                    "low" -> "کم"
                    "none" -> "ندارد"
                    else -> "عادی"
                }
                val modeText = when {
                    explicitlyExcludedFromMatrix && noDate -> "بدون تاریخ · خارج از ماتریس"
                    noDate -> "فقط در ماتریس آیزنهاور · بدون تاریخ"
                    explicitlyExcludedFromMatrix -> "یادآور خارج از ماتریس"
                    noAlarm -> "یادآور بدون زنگ · با تاریخ"
                    else -> "آلارم گوشی · در ماتریس"
                }
                onComplete("✅ «${parsed.title}» ثبت شد — $modeText ($formattedTime · $tagsStr · اولویت $prioLabel)")
            } catch (e: Exception) {
                onComplete("خطا در پردازش هوش مصنوعی: ${e.message}")
            } finally {
                _isAiProcessing.value = false
            }
        }
    }
}
