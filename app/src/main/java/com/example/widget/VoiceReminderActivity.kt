package com.example.widget

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.YadavarApp
import com.example.data.AlarmEntity
import com.example.ui.theme.MyApplicationTheme
import com.example.util.GeminiAiParser
import com.example.util.JalaliCalendar
import com.example.util.JalaliDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class VoiceReminderDraft(
    val rawText: String,
    val title: String,
    val date: JalaliDate,
    val hasDate: Boolean,
    val hour: Int,
    val minute: Int,
    val isDaily: Boolean,
    val repeatDays: String,
    val prio: String,
    val tag: String,
    val hasAlarm: Boolean,
    val showInMatrix: Boolean
)

private data class VoiceReminderUiState(
    val isParsing: Boolean = false,
    val error: String? = null,
    val draft: VoiceReminderDraft? = null
)

class VoiceReminderActivity : ComponentActivity() {
    private val uiState = mutableStateOf(VoiceReminderUiState())
    private lateinit var speechLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        speechLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
                if (spoken.isNotBlank()) parseSpeech(spoken) else uiState.value = uiState.value.copy(error = "صدایی تشخیص داده نشد؛ دوباره تلاش کن.", isParsing = false)
            } else {
                uiState.value = uiState.value.copy(error = "تشخیص صدا لغو شد. برای شروع دوباره دکمه میکروفون را بزن.", isParsing = false)
            }
        }
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchRecognizer()
            else uiState.value = VoiceReminderUiState(error = "برای ثبت یادآور صوتی، اجازه استفاده از میکروفون لازم است.")
        }

        setContent {
            MyApplicationTheme(darkTheme = false) {
                androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    VoiceReminderScreen(
                        state = uiState.value,
                        onStartListening = { startListening() },
                        onTitleChange = { newTitle -> updateDraft { it.copy(title = newTitle) } },
                        onUpdateDraft = { updated -> uiState.value = uiState.value.copy(draft = updated, error = null) },
                        onEditDate = { showDatePicker() },
                        onEditTime = { showTimePicker() },
                        onSave = { saveDraft() },
                        onCancel = { finish() }
                    )
                }
            }
        }

        window.decorView.post { startListening() }
    }

    private fun startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            launchRecognizer()
        }
    }

    private fun launchRecognizer() {
        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "یادآورت را بگو؛ مثلاً فردا ساعت ۹ جلسه با تیم")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        try {
            speechLauncher.launch(speechIntent)
        } catch (_: Exception) {
            uiState.value = VoiceReminderUiState(error = "سرویس تشخیص گفتار روی این دستگاه در دسترس نیست.")
        }
    }

    private fun parseSpeech(rawText: String) {
        uiState.value = VoiceReminderUiState(isParsing = true)
        lifecycleScope.launch {
            try {
                val prefs = getSharedPreferences("yadavar_prefs", MODE_PRIVATE)
                val parsed = withContext(Dispatchers.IO) {
                    GeminiAiParser.parsePrompt(
                        rawText = rawText,
                        today = JalaliCalendar.today(),
                        customApiKey = prefs.getString("custom_gemini_api_key", "")
                    )
                }
                val explicitDow = GeminiAiParser.detectWeekday(rawText)
                val isWeekly = parsed.repeat == "weekly" || explicitDow != null
                val date = explicitDow?.let { JalaliCalendar.nextOrSameDayOfWeek(JalaliCalendar.today(), it) } ?: parsed.jalaliDate
                val repeatDays = if (isWeekly) {
                    (explicitDow ?: JalaliCalendar.dayOfWeek(date.year, date.month, date.day)).toString()
                } else ""
                val noAlarm = GeminiAiParser.requestsNoAlarm(rawText)
                val noDate = GeminiAiParser.requestsNoDate(rawText)
                val noMatrix = GeminiAiParser.requestsNoMatrix(rawText)
                val matrixPriority = parsed.prio.takeUnless { it == "none" } ?: "normal"
                uiState.value = VoiceReminderUiState(
                    draft = VoiceReminderDraft(
                        rawText = rawText,
                        title = parsed.title,
                        date = date,
                        hasDate = !noDate,
                        hour = parsed.hour,
                        minute = parsed.minute,
                        isDaily = parsed.repeat == "daily",
                        repeatDays = repeatDays,
                        prio = if (noMatrix) "none" else matrixPriority,
                        tag = parsed.tags.firstOrNull() ?: "شخصی",
                        hasAlarm = !noAlarm && !noDate,
                        showInMatrix = !noMatrix
                    )
                )
            } catch (e: Exception) {
                uiState.value = VoiceReminderUiState(error = "پردازش یادآور انجام نشد: ${e.message ?: "خطای نامشخص"}")
            }
        }
    }

    private fun updateDraft(transform: (VoiceReminderDraft) -> VoiceReminderDraft) {
        val current = uiState.value.draft ?: return
        uiState.value = uiState.value.copy(draft = transform(current))
    }

    private fun showDatePicker() {
        val draft = uiState.value.draft ?: return
        val (gy, gm, gd) = JalaliCalendar.jalaliToGregorian(draft.date.year, draft.date.month, draft.date.day)
        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val selected = JalaliCalendar.gregorianToJalali(selectedYear, selectedMonth + 1, selectedDay)
            updateDraft { old ->
                old.copy(
                    date = selected,
                    hasDate = true,
                    hasAlarm = old.hasAlarm,
                    repeatDays = if (old.repeatDays.isNotBlank() && !old.isDaily) {
                        JalaliCalendar.dayOfWeek(selected.year, selected.month, selected.day).toString()
                    } else old.repeatDays
                )
            }
        }, gy, gm - 1, gd).show()
    }

    private fun showTimePicker() {
        val draft = uiState.value.draft ?: return
        TimePickerDialog(this, { _, hour, minute ->
            updateDraft { it.copy(hour = hour, minute = minute) }
        }, draft.hour, draft.minute, true).show()
    }

    private fun saveDraft() {
        val draft = uiState.value.draft ?: return
        if (draft.title.isBlank()) {
            uiState.value = uiState.value.copy(error = "عنوان یادآور را وارد کن.")
            return
        }
        lifecycleScope.launch {
            try {
                val app = application as YadavarApp
                val alarm = AlarmEntity(
                    title = draft.title.trim(),
                    hour = draft.hour,
                    minute = draft.minute,
                    isDaily = draft.isDaily,
                    repeatDays = draft.repeatDays,
                    jalaliYear = if (draft.hasDate) draft.date.year else 0,
                    jalaliMonth = if (draft.hasDate) draft.date.month else 0,
                    jalaliDay = if (draft.hasDate) draft.date.day else 0,
                    hasDate = draft.hasDate,
                    prio = if (draft.showInMatrix) draft.prio.takeUnless { it == "none" } ?: "normal" else "none",
                    tag = draft.tag,
                    hasAlarm = draft.hasAlarm && draft.hasDate,
                    showInMatrix = draft.showInMatrix,
                    isVibrate = true,
                    snoozeMinutes = 10
                )
                val id = withContext(Dispatchers.IO) { app.repository.insertAlarm(alarm) }
                val savedAlarm = alarm.copy(id = id)
                if (savedAlarm.hasAlarm) app.alarmScheduler.scheduleAlarm(savedAlarm)
                WidgetUpdater.refreshAll(this@VoiceReminderActivity)
                Toast.makeText(this@VoiceReminderActivity, "یادآور «${savedAlarm.title}» ذخیره شد", Toast.LENGTH_LONG).show()
                finish()
            } catch (e: Exception) {
                uiState.value = uiState.value.copy(error = "ذخیره یادآور انجام نشد: ${e.message ?: "خطای نامشخص"}")
            }
        }
    }
}

@Composable
private fun VoiceReminderScreen(
    state: VoiceReminderUiState,
    onStartListening: () -> Unit,
    onTitleChange: (String) -> Unit,
    onUpdateDraft: (VoiceReminderDraft) -> Unit,
    onEditDate: () -> Unit,
    onEditTime: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    var priorityMenuExpanded by remember(state.draft?.prio, state.draft?.showInMatrix) { mutableStateOf(false) }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("یادآور صوتی", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = onCancel) { Text("بستن") }
            }
            Text("حرفت را به یادآور تبدیل کن و قبل از ثبت بررسی‌اش کن.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)

            if (state.isParsing) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator()
                        Text("در حال تحلیل متن و آماده‌سازی یادآور…")
                    }
                }
            }

            state.error?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            val draft = state.draft
            if (draft == null) {
                Button(onClick = onStartListening, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Text("شروع ضبط صدا", modifier = Modifier.padding(start = 8.dp))
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("متن تشخیص‌داده‌شده", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        Text(draft.rawText, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = draft.title,
                            onValueChange = { onTitleChange(it) },
                            label = { Text("عنوان یادآور") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("تاریخ", fontWeight = FontWeight.Medium)
                                Text(if (draft.hasDate) draft.date.formatted(includeDayName = true) else "بدون تاریخ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                            OutlinedButton(onClick = onEditDate, enabled = draft.hasDate) { Text("ویرایش تاریخ") }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = draft.hasDate, onCheckedChange = { checked ->
                                onUpdateDraft(draft.copy(hasDate = checked, hasAlarm = if (checked) draft.hasAlarm else false))
                            })
                            Text("تاریخ مشخص دارد")
                        }
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("ساعت", fontWeight = FontWeight.Medium)
                                Text(JalaliCalendar.formatTime(draft.hour, draft.minute), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                            OutlinedButton(onClick = onEditTime) { Text("ویرایش ساعت") }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = draft.hasAlarm, enabled = draft.hasDate, onCheckedChange = { checked ->
                                onUpdateDraft(draft.copy(hasAlarm = checked && draft.hasDate))
                            })
                            Text("آلارم گوشی فعال باشد", color = if (draft.hasDate) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = draft.showInMatrix, onCheckedChange = { checked ->
                                onUpdateDraft(draft.copy(showInMatrix = checked, prio = if (checked && draft.prio == "none") "normal" else if (!checked) "none" else draft.prio))
                            })
                            Text("نمایش در ماتریس آیزنهاور")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("اولویت", fontWeight = FontWeight.Medium)
                            Box {
                                OutlinedButton(onClick = { priorityMenuExpanded = true }) {
                                    Text(priorityLabel(draft.prio, draft.showInMatrix))
                                }
                                DropdownMenu(expanded = priorityMenuExpanded, onDismissRequest = { priorityMenuExpanded = false }) {
                                    listOf("urgent" to "فوری", "high" to "مهم", "normal" to "عادی", "low" to "کم", "none" to "ندارد").forEach { (value, label) ->
                                        DropdownMenuItem(text = { Text(label) }, onClick = {
                                            priorityMenuExpanded = false
                                            onUpdateDraft(draft.copy(prio = value, showInMatrix = value != "none"))
                                        })
                                    }
                                }
                            }
                            if (draft.isDaily) Text("تکرار روزانه", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            else if (draft.repeatDays.isNotBlank()) Text("تکرار هفتگی", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                }
                Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text("تأیید و ذخیره یادآور") }
                OutlinedButton(onClick = onStartListening, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Replay, contentDescription = null)
                    Text("دوباره صحبت کن", modifier = Modifier.padding(start = 8.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun priorityLabel(prio: String, showInMatrix: Boolean): String = when {
    !showInMatrix || prio == "none" -> "ندارد"
    prio == "urgent" -> "فوری"
    prio == "high" -> "مهم"
    prio == "low" -> "کم"
    else -> "عادی"
}
