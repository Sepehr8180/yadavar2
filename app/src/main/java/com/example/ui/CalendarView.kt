package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AlarmEntity
import com.example.data.BirthdayEntity
import com.example.util.JalaliCalendar
import com.example.util.JalaliDate

@Composable
fun CalendarView(
    currentYear: Int,
    currentMonth: Int,
    selectedDate: JalaliDate,
    alarms: List<AlarmEntity>,
    birthdays: List<BirthdayEntity> = emptyList(),
    onSelectDate: (JalaliDate) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onAddAlarmForDate: (JalaliDate) -> Unit,
    onToggleAlarmEnabled: (AlarmEntity) -> Unit,
    onToggleAlarmDone: (AlarmEntity) -> Unit,
    onEditAlarm: (AlarmEntity) -> Unit,
    onDeleteAlarm: (AlarmEntity) -> Unit,
    onTestAlarm: (AlarmEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { JalaliCalendar.today() }
    val monthName = JalaliCalendar.MON_NAMES.getOrElse(currentMonth - 1) { "" }
    val daysInMonth = remember(currentYear, currentMonth) {
        JalaliCalendar.monthLength(currentYear, currentMonth)
    }
    // Day of week of the 1st day of month (0 = Saturday, ..., 6 = Friday)
    val firstDow = remember(currentYear, currentMonth) {
        JalaliCalendar.dayOfWeek(currentYear, currentMonth, 1)
    }

    // Set of days in this month that have alarms
    val alarmDays = remember(alarms, currentYear, currentMonth) {
        val set = mutableSetOf<Int>()
        for (a in alarms) {
            if (a.isDaily) {
                for (d in 1..daysInMonth) set.add(d)
            } else if (a.repeatDays.isNotEmpty()) {
                val repDays = a.parseRepeatDays()
                for (d in 1..daysInMonth) {
                    val dow = JalaliCalendar.dayOfWeek(currentYear, currentMonth, d)
                    if (dow in repDays) set.add(d)
                }
            } else if (a.jalaliYear == currentYear && a.jalaliMonth == currentMonth) {
                set.add(a.jalaliDay)
            }
        }
        set
    }

    // Map of days in this month that have birthdays
    val birthdayDaysMap = remember(birthdays, currentMonth) {
        birthdays.filter { it.month == currentMonth }
            .groupBy { it.day }
    }

    // Filter alarms for selected date
    val alarmsForSelectedDate = remember(alarms, selectedDate) {
        val selectedDow = JalaliCalendar.dayOfWeek(selectedDate.year, selectedDate.month, selectedDate.day)
        alarms.filter { a ->
            a.isDaily ||
            a.parseRepeatDays().contains(selectedDow) ||
            (a.jalaliYear == selectedDate.year && a.jalaliMonth == selectedDate.month && a.jalaliDay == selectedDate.day)
        }
    }

    // Birthdays on selected date
    val birthdaysForSelectedDate = remember(birthdays, selectedDate) {
        birthdays.filter { it.month == selectedDate.month && it.day == selectedDate.day }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Calendar Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Header Month & Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$monthName ${JalaliCalendar.toPersianDigits(currentYear.toString())}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = onToday,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(34.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                            ) {
                                Text("امروز", fontSize = 12.sp)
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(
                                onClick = onPrevMonth,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "ماه قبل"
                                )
                            }

                            IconButton(
                                onClick = onNextMonth,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = "ماه بعد"
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Weekday names
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (dow in JalaliCalendar.DOW_SHORT) {
                            Text(
                                text = dow,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (dow == "ج") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(38.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Calendar Days Grid (6 rows x 7 cols)
                    val rows = 6

                    for (r in 0 until rows) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            for (c in 0 until 7) {
                                val cellIndex = r * 7 + c
                                val dayNumber = cellIndex - firstDow + 1

                                if (dayNumber in 1..daysInMonth) {
                                    val isToday = currentYear == today.year && currentMonth == today.month && dayNumber == today.day
                                    val isSelected = currentYear == selectedDate.year && currentMonth == selectedDate.month && dayNumber == selectedDate.day
                                    val hasAlarm = alarmDays.contains(dayNumber)
                                    val hasBirthday = birthdayDaysMap.containsKey(dayNumber)
                                    val isFriday = c == 6

                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                                    hasBirthday -> Color(0xFFFFF3E0) // Warm festive birthday tint
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .then(
                                                if (hasBirthday && !isSelected) {
                                                    Modifier.border(1.5.dp, Color(0xFFFF9800), CircleShape)
                                                } else Modifier
                                            )
                                            .clickable {
                                                onSelectDate(JalaliDate(currentYear, currentMonth, dayNumber))
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            // Day Number
                                            Text(
                                                text = JalaliCalendar.toPersianDigits(dayNumber.toString()),
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected || isToday || hasBirthday) FontWeight.Bold else FontWeight.Normal,
                                                color = when {
                                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                                    hasBirthday -> Color(0xFFE65100)
                                                    isFriday -> MaterialTheme.colorScheme.primary
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                }
                                            )

                                            // Distinct Indicator row (Birthday Cake / Alarm dot)
                                            Row(
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (hasBirthday) {
                                                    Text(
                                                        text = "🎂",
                                                        fontSize = 9.sp,
                                                        lineHeight = 9.sp
                                                    )
                                                }
                                                if (hasAlarm) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                                else MaterialTheme.colorScheme.secondary
                                                            )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Empty padding cell
                                    Box(modifier = Modifier.size(40.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Legend: Shows Birthday & Alarm indicators guide
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🎂 تولدها (رنگ نارنجی و کیک)", fontSize = 11.sp, color = Color(0xFFE65100), fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.width(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "آلارم‌ها و یادآورها", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }

        // Distinct Birthday Card for Selected Date (Celebration banner)
        if (birthdaysForSelectedDate.isNotEmpty()) {
            item {
                for (bday in birthdaysForSelectedDate) {
                    val age = bday.year?.let { selectedDate.year - it }
                    val ageText = if (age != null) " (امسال $age ساله می‌شود)" else ""

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E5)),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFB74D))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🎂", fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "تولد ${bday.name}$ageText 🎉",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB25000)
                                )
                                if (bday.notes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = bday.notes,
                                        fontSize = 12.sp,
                                        color = Color(0xFF8D4004)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section header for selected date
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = selectedDate.formatted(includeDayName = true),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${JalaliCalendar.toPersianDigits(alarmsForSelectedDate.size.toString())} آلارم تنظیم شده",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Button(
                    onClick = { onAddAlarmForDate(selectedDate) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_alarm_for_date_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "افزودن آلارم",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("آلارم جدید", fontSize = 13.sp)
                }
            }
        }

        // Alarms for selected date list
        if (alarmsForSelectedDate.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "برای این روز یادآوری یا آلارمی وجود ندارد",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(alarmsForSelectedDate, key = { it.id }) { alarm ->
                AlarmCard(
                    alarm = alarm,
                    onToggleEnabled = { onToggleAlarmEnabled(alarm) },
                    onToggleDone = { onToggleAlarmDone(alarm) },
                    onEdit = { onEditAlarm(alarm) },
                    onDelete = { onDeleteAlarm(alarm) },
                    onTestAlarm = { onTestAlarm(alarm) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
