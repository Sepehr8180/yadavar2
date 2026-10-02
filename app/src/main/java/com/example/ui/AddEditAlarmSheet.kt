package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AlarmEntity
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.PriorityLow
import com.example.ui.theme.PriorityNormal
import com.example.ui.theme.PriorityUrgent
import com.example.util.JalaliCalendar
import com.example.util.JalaliDate
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAlarmSheet(
    editingAlarm: AlarmEntity?,
    initialDate: JalaliDate,
    defaultPriority: String = "normal",
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        title: String,
        hour: Int,
        minute: Int,
        isDaily: Boolean,
        repeatDays: String,
        jalaliDate: JalaliDate,
        prio: String,
        tag: String,
        isVibrate: Boolean,
        snoozeMinutes: Int,
        hasDate: Boolean,
        hasAlarm: Boolean
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentCal = remember { Calendar.getInstance() }
    var hour by remember {
        mutableIntStateOf(editingAlarm?.hour ?: ((currentCal.get(Calendar.HOUR_OF_DAY) + 1) % 24))
    }
    var minute by remember {
        mutableIntStateOf(editingAlarm?.minute ?: 0)
    }
    var title by remember {
        mutableStateOf(editingAlarm?.title ?: "")
    }

    // Repeat Mode: 0 = One time (date), 1 = Daily (every day), 2 = Specific days of week
    var repeatMode by remember {
        mutableIntStateOf(
            when {
                editingAlarm?.isDaily == true -> 1
                (editingAlarm?.repeatDays?.isNotEmpty() == true) -> 2
                else -> 0
            }
        )
    }

    var selectedRepeatDays by remember {
        mutableStateOf(editingAlarm?.parseRepeatDays() ?: emptySet())
    }

    var selectedJalaliDate by remember {
        mutableStateOf(editingAlarm?.toJalaliDate() ?: initialDate)
    }

    var hasDate by remember {
        mutableStateOf(editingAlarm?.hasDate ?: true)
    }

    var prio by remember {
        mutableStateOf(editingAlarm?.prio ?: defaultPriority)
    }

    var tag by remember {
        mutableStateOf(editingAlarm?.tag ?: "شخصی")
    }

    var isVibrate by remember {
        mutableStateOf(editingAlarm?.isVibrate ?: true)
    }

    var snoozeMinutes by remember {
        mutableIntStateOf(editingAlarm?.snoozeMinutes ?: 10)
    }

    var hasAlarm by remember {
        mutableStateOf(editingAlarm?.hasAlarm ?: true)
    }

    val tagsList = listOf("کار", "شخصی", "روتین", "خونه", "سلامت", "مطالعه", "تولد")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingAlarm == null) "📝 ثبت یادآور جدید" else "✏️ ویرایش یادآور",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "بستن")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Alarm mode: a reminder can live in the Eisenhower matrix without ringing.
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (hasAlarm) "زنگ گوشی فعال است" else "فقط یادآوری / ماتریس آیزنهاور",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when {
                                hasAlarm -> "در زمان تعیین‌شده صدای آلارم پخش می‌شود."
                                !hasDate -> "برای فعال کردن زنگ، ابتدا «تاریخ مشخص دارد» را روشن کنید."
                                else -> "هیچ زنگی پخش نمی‌شود و مورد در ماتریس باقی می‌ماند."
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Switch(
                        checked = hasAlarm,
                        enabled = hasDate,
                        onCheckedChange = { if (hasDate) hasAlarm = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Optional date: some matrix tasks do not have a specific date.
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (hasDate) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (hasDate) "تاریخ مشخص دارد" else "بدون تاریخ مشخص",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (hasDate) "این مورد در تقویم نمایش داده می‌شود." else "برای کارهایی که فقط می‌خواهی در ماتریس آیزنهاور بمانند.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Switch(
                        checked = hasDate,
                        onCheckedChange = { enabled ->
                            hasDate = enabled
                            if (!enabled) hasAlarm = false
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (hasAlarm) {
                // Time Selector Card with Big Digits
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "زمان زنگ آلارم",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.outline
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = JalaliCalendar.formatTime(hour, minute),
                            fontSize = 46.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Hour and Minute Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ساعت: ${JalaliCalendar.toPersianDigits(hour.toString())}", fontSize = 13.sp)
                                Slider(
                                    value = hour.toFloat(),
                                    onValueChange = { hour = it.toInt() },
                                    valueRange = 0f..23f,
                                    steps = 22,
                                    modifier = Modifier.width(140.dp)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("دقیقه: ${JalaliCalendar.toPersianDigits(minute.toString())}", fontSize = 13.sp)
                                Slider(
                                    value = minute.toFloat(),
                                    onValueChange = { minute = it.toInt() },
                                    valueRange = 0f..59f,
                                    steps = 58,
                                    modifier = Modifier.width(140.dp)
                                )
                            }
                        }

                        // Quick Time Preset Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PresetChip("۰۷:۰۰ صبح") { hour = 7; minute = 0 }
                            PresetChip("۰۸:۳۰ صبح") { hour = 8; minute = 30 }
                            PresetChip("۱۲:۳۰ ظهر") { hour = 12; minute = 30 }
                            PresetChip("۱۷:۰۰ عصر") { hour = 17; minute = 0 }
                            PresetChip("۲۲:۰۰ شب") { hour = 22; minute = 0 }
                            PresetChip("+۳۰ دقیقه") {
                                val c = Calendar.getInstance().apply { add(Calendar.MINUTE, 30) }
                                hour = c.get(Calendar.HOUR_OF_DAY)
                                minute = c.get(Calendar.MINUTE)
                            }
                        }
                    }
                }


            }

            Spacer(modifier = Modifier.height(18.dp))

            // Title TextField
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان یادآوری یا آلارم") },
                placeholder = { Text("مثلاً: بیدار باش صبحگاهی، جلسه کاری...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("alarm_title_input"),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            if (hasDate) {
                Spacer(modifier = Modifier.height(18.dp))

                // Repeat Mode Selector
                Text(
                text = "زمان‌بندی تکرار",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RepeatModeChip(
                    label = "تاریخ شمسی",
                    selected = repeatMode == 0,
                    onClick = { repeatMode = 0 },
                    modifier = Modifier.weight(1f)
                )
                RepeatModeChip(
                    label = "روزانه (هر روز)",
                    selected = repeatMode == 1,
                    onClick = { repeatMode = 1 },
                    modifier = Modifier.weight(1f)
                )
                RepeatModeChip(
                    label = "روزهای هفته",
                    selected = repeatMode == 2,
                    onClick = { repeatMode = 2 },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mode Details
            when (repeatMode) {
                0 -> {
                    // Quick Date Chips
                    val today = remember { JalaliCalendar.today() }
                    val tomorrow = remember { JalaliCalendar.addDays(today, 1) }
                    val afterTomorrow = remember { JalaliCalendar.addDays(today, 2) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DateChip(
                            label = "امروز (${JalaliCalendar.toPersianDigits(today.day.toString())} ${JalaliCalendar.MON_NAMES[today.month - 1]})",
                            selected = selectedJalaliDate == today,
                            onClick = { selectedJalaliDate = today },
                            modifier = Modifier.weight(1f)
                        )
                        DateChip(
                            label = "فردا",
                            selected = selectedJalaliDate == tomorrow,
                            onClick = { selectedJalaliDate = tomorrow },
                            modifier = Modifier.weight(1f)
                        )
                        DateChip(
                            label = "پس‌فردا",
                            selected = selectedJalaliDate == afterTomorrow,
                            onClick = { selectedJalaliDate = afterTomorrow },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                2 -> {
                    // Weekday Multi-selection Chips (0=Saturday..6=Friday)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (i in 0..6) {
                            val dowName = JalaliCalendar.DOW_SHORT[i]
                            val isSelected = selectedRepeatDays.contains(i)
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable {
                                        selectedRepeatDays = if (isSelected) {
                                            selectedRepeatDays - i
                                        } else {
                                            selectedRepeatDays + i
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dowName,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

                Spacer(modifier = Modifier.height(18.dp))
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Priority
            Text(
                text = "اولویت",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrioChip("فوری", "urgent", PriorityUrgent, prio == "urgent") { prio = "urgent" }
                PrioChip("مهم", "high", PriorityHigh, prio == "high") { prio = "high" }
                PrioChip("عادی", "normal", PriorityNormal, prio == "normal") { prio = "normal" }
                PrioChip("کم", "low", PriorityLow, prio == "low") { prio = "low" }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Tag / Category
            Text(
                text = "دسته‌بندی (تگ)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (t in tagsList) {
                    FilterChip(
                        selected = tag == t,
                        onClick = { tag = t },
                        label = { Text("#$t") },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (hasAlarm) {
                // Settings: Vibration & Snooze Duration
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Vibration,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("لرزش (ویبره گوشی)", fontSize = 14.sp)
                            }
                            Switch(
                                checked = isVibrate,
                                onCheckedChange = { isVibrate = it }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("مدت زمان اسنوز (تعویق):", fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                SnoozeChip("۵ دقیقه", 5, snoozeMinutes == 5) { snoozeMinutes = 5 }
                                SnoozeChip("۱۰ دقیقه", 10, snoozeMinutes == 10) { snoozeMinutes = 10 }
                                SnoozeChip("۱۵ دقیقه", 15, snoozeMinutes == 15) { snoozeMinutes = 15 }
                            }
                        }
                    }
                }


            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Button(
                onClick = {
                    val repeatDaysStr = if (hasDate && repeatMode == 2) {
                        selectedRepeatDays.sorted().joinToString(",")
                    } else ""

                    onSave(
                        editingAlarm?.id ?: 0L,
                        title,
                        hour,
                        minute,
                        hasDate && repeatMode == 1,
                        repeatDaysStr,
                        selectedJalaliDate,
                        prio,
                        tag,
                        isVibrate,
                        snoozeMinutes,
                        hasDate,
                        hasAlarm
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_alarm_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (editingAlarm == null) { if (hasAlarm) "تنظیم آلارم" else "ثبت یادآور" } else "ذخیره تغییرات",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun PresetChip(text: String, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun RepeatModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 10.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun DateChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun PrioChip(
    label: String,
    key: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) color else color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else color,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun SnoozeChip(
    label: String,
    minutes: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
