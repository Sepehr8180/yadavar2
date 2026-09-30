package com.example.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Payments
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
import com.example.data.FinancialEntity
import com.example.ui.theme.FinancialContainer
import com.example.ui.theme.FinancialOnContainer
import com.example.ui.theme.FinancialPrimary
import com.example.util.JalaliCalendar
import com.example.util.JalaliDate

@Composable
fun CalendarView(
    currentYear: Int,
    currentMonth: Int,
    selectedDate: JalaliDate,
    alarms: List<AlarmEntity>,
    birthdays: List<BirthdayEntity> = emptyList(),
    financialItems: List<FinancialEntity> = emptyList(),
    onSelectDate: (JalaliDate) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onAddAlarmForDate: (JalaliDate) -> Unit,
    onToggleAlarmEnabled: (AlarmEntity) -> Unit,
    onToggleAlarmDone: (AlarmEntity) -> Unit,
    onEditAlarm: (AlarmEntity) -> Unit,
    onDeleteAlarm: (AlarmEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { JalaliCalendar.today() }
    val monthName = JalaliCalendar.MON_NAMES.getOrElse(currentMonth - 1) { "" }
    val daysInMonth = remember(currentYear, currentMonth) {
        JalaliCalendar.monthLength(currentYear, currentMonth)
    }
    val firstDow = remember(currentYear, currentMonth) {
        JalaliCalendar.dayOfWeek(currentYear, currentMonth, 1)
    }

    val alarmDays = remember(alarms, currentYear, currentMonth) {
        buildSet {
            alarms.forEach { a ->
                when {
                    a.isDaily -> for (d in 1..daysInMonth) add(d)
                    a.repeatDays.isNotEmpty() -> {
                        val days = a.parseRepeatDays()
                        for (d in 1..daysInMonth) {
                            if (JalaliCalendar.dayOfWeek(currentYear, currentMonth, d) in days) add(d)
                        }
                    }
                    a.jalaliYear == currentYear && a.jalaliMonth == currentMonth -> add(a.jalaliDay)
                }
            }
        }
    }

    val financeDays = remember(financialItems, currentYear, currentMonth) {
        financialItems.mapNotNull { it.occurrenceInMonth(currentYear, currentMonth)?.day }.toSet()
    }

    val birthdayDaysMap = remember(birthdays, currentMonth) {
        birthdays.filter { it.month == currentMonth }.groupBy { it.day }
    }

    val selectedDow = JalaliCalendar.dayOfWeek(selectedDate.year, selectedDate.month, selectedDate.day)
    val alarmsForSelectedDate = remember(alarms, selectedDate) {
        alarms.filter { a ->
            a.isDaily || a.parseRepeatDays().contains(selectedDow) ||
                (a.jalaliYear == selectedDate.year && a.jalaliMonth == selectedDate.month && a.jalaliDay == selectedDate.day)
        }
    }

    val birthdaysForSelectedDate = remember(birthdays, selectedDate) {
        birthdays.filter { it.month == selectedDate.month && it.day == selectedDate.day }
    }

    val financialForSelectedDate = remember(financialItems, selectedDate) {
        financialItems.filter { it.occursOn(selectedDate) }
    }

    val monthFinancialTotal = remember(financialItems, currentYear, currentMonth) {
        financialItems.sumOf { item -> if (item.occurrenceInMonth(currentYear, currentMonth) != null) item.amount else 0L }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$monthName ${JalaliCalendar.toPersianDigits(currentYear.toString())}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = onToday,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(34.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                            ) { Text("امروز", fontSize = 12.sp) }
                            IconButton(onClick = onPrevMonth, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.ChevronRight, "ماه قبل")
                            }
                            IconButton(onClick = onNextMonth, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.ChevronLeft, "ماه بعد")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        JalaliCalendar.DOW_SHORT.forEachIndexed { index, dow ->
                            Text(
                                text = dow,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (index == 6) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(38.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    repeat(6) { r ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            repeat(7) { c ->
                                val cellIndex = r * 7 + c
                                val dayNumber = cellIndex - firstDow + 1
                                if (dayNumber in 1..daysInMonth) {
                                    val cellDate = JalaliDate(currentYear, currentMonth, dayNumber)
                                    val isToday = cellDate == today
                                    val isSelected = cellDate == selectedDate
                                    val hasAlarm = dayNumber in alarmDays
                                    val hasFinance = dayNumber in financeDays
                                    val hasBirthday = birthdayDaysMap.containsKey(dayNumber)

                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .clickable { onSelectDate(cellDate) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = JalaliCalendar.toPersianDigits(dayNumber.toString()),
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected || isToday || hasBirthday || hasFinance) FontWeight.Bold else FontWeight.Normal,
                                                color = when {
                                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                                    hasFinance -> FinancialPrimary
                                                    hasBirthday -> Color(0xFFE65100)
                                                    c == 6 -> MaterialTheme.colorScheme.primary
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (hasBirthday) Text("🎂", fontSize = 8.sp, lineHeight = 8.sp)
                                                if (hasAlarm) {
                                                    Box(
                                                        Modifier.size(4.dp).clip(CircleShape).background(
                                                            if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary
                                                        )
                                                    )
                                                }
                                                if (hasFinance) {
                                                    Box(
                                                        Modifier.size(4.dp).clip(CircleShape).background(
                                                            if (isSelected) MaterialTheme.colorScheme.onPrimary else FinancialPrimary
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else Box(modifier = Modifier.size(42.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎂 تولد", fontSize = 10.sp, color = Color(0xFFE65100))
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("یادآور", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(Modifier.size(5.dp).clip(CircleShape).background(FinancialPrimary))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("قسط / بدهی", fontSize = 10.sp, color = FinancialPrimary)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FinancialContainer)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Payments, null, tint = FinancialPrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("مجموع اقساط این ماه", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FinancialOnContainer)
                            Text("بر اساس سررسیدهای ${monthName}", fontSize = 11.sp, color = FinancialOnContainer.copy(alpha = 0.75f))
                        }
                    }
                    Text(
                        "${JalaliCalendar.formatNumber(monthFinancialTotal)} تومان",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = FinancialPrimary
                    )
                }
            }
        }

        if (birthdaysForSelectedDate.isNotEmpty()) {
            items(birthdaysForSelectedDate) { bday ->
                val age = bday.year?.let { selectedDate.year - it }
                val ageText = if (age != null) " (امسال $age ساله می‌شود)" else ""
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E5))
                ) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🎂", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("تولد ${bday.name}$ageText 🎉", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB25000))
                            if (bday.notes.isNotBlank()) Text(bday.notes, fontSize = 12.sp, color = Color(0xFF8D4004))
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(selectedDate.formatted(includeDayName = true), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${JalaliCalendar.toPersianDigits(alarmsForSelectedDate.size.toString())} یادآور · ${JalaliCalendar.toPersianDigits(financialForSelectedDate.size.toString())} مورد مالی",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Button(
                    onClick = { onAddAlarmForDate(selectedDate) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_alarm_for_date_button")
                ) {
                    Icon(Icons.Default.Add, "افزودن یادآور", modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("یادآور", fontSize = 12.sp)
                }
            }
        }

        if (financialForSelectedDate.isNotEmpty()) {
            items(financialForSelectedDate, key = { "finance-${it.id}" }) { item ->
                FinancialCalendarCard(item)
            }
        }

        if (alarmsForSelectedDate.isEmpty() && financialForSelectedDate.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CalendarToday, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("برای این روز موردی ثبت نشده است", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (alarmsForSelectedDate.isNotEmpty()) {
            items(alarmsForSelectedDate, key = { "alarm-${it.id}" }) { alarm ->
                AlarmCard(
                    alarm = alarm,
                    onToggleEnabled = { onToggleAlarmEnabled(alarm) },
                    onToggleDone = { onToggleAlarmDone(alarm) },
                    onEdit = { onEditAlarm(alarm) },
                    onDelete = { onDeleteAlarm(alarm) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun FinancialCalendarCard(item: FinancialEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FinancialContainer),
        border = androidx.compose.foundation.BorderStroke(1.dp, FinancialPrimary.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier.size(38.dp).background(FinancialPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Payments, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(item.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FinancialOnContainer)
                    Text(
                        if (item.isMonthly) "قسط ماهانه" else "پرداخت یک‌باره",
                        fontSize = 11.sp,
                        color = FinancialOnContainer.copy(alpha = 0.75f)
                    )
                }
            }
            Text("${item.formattedAmount()} تومان", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = FinancialPrimary)
        }
    }
}
