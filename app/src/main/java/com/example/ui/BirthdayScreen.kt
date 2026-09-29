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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BirthdayEntity
import com.example.util.JalaliCalendar
import com.example.util.JalaliDate

@Composable
fun BirthdayScreen(
    birthdays: List<BirthdayEntity>,
    onAddBirthdayClick: () -> Unit,
    onDeleteBirthday: (BirthdayEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { JalaliCalendar.today() }

    // Sort birthdays by days left to upcoming occurrence
    val sortedBirthdays = remember(birthdays, today) {
        birthdays.sortedBy { b ->
            b.nextOccurrenceInfo(today).first
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Header Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.secondary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cake,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "یادآور تولدها و مناسبت‌ها",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${JalaliCalendar.toPersianDigits(birthdays.size.toString())} مناسبت ثبت شده",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    Button(
                        onClick = onAddBirthdayClick,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("add_bday_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ثبت تولد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Empty state
        if (sortedBirthdays.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cake,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "هنوز هیچ تولدی ثبت نکرده‌اید",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "با ثبت روز تولد دوستان و خانواده، آلارم بیدارباش صبحگاهی یادآوری دریافت کنید.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onAddBirthdayClick,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("افزودن اولین تولد")
                        }
                    }
                }
            }
        } else {
            items(sortedBirthdays, key = { it.id }) { bday ->
                val (daysLeft, turningAge, nextDate) = bday.nextOccurrenceInfo(today)

                BirthdayCard(
                    birthday = bday,
                    daysLeft = daysLeft,
                    turningAge = turningAge,
                    onDelete = { onDeleteBirthday(bday) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(84.dp))
        }
    }
}

@Composable
fun BirthdayCard(
    birthday: BirthdayEntity,
    daysLeft: Int,
    turningAge: Int?,
    onDelete: () -> Unit
) {
    val isToday = daysLeft == 0
    val isTomorrow = daysLeft == 1

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isToday) Color(0xFFFFF4E5) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isToday) 3.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            if (isToday) Color(0xFFFF9800) else MaterialTheme.colorScheme.secondaryContainer,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isToday) "🎉" else "🎂",
                        fontSize = 22.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = birthday.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (turningAge != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${JalaliCalendar.toPersianDigits(turningAge.toString())} ساله",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "تاریخ تولد: ${birthday.formattedDate()}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    if (birthday.notes.isNotBlank()) {
                        Text(
                            text = birthday.notes,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // Days left badge & delete button
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Surface(
                    color = when {
                        isToday -> Color(0xFFE65100)
                        isTomorrow -> Color(0xFFF57C00)
                        daysLeft <= 7 -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = when {
                            isToday -> "امروز تولدشه! 🎂"
                            isTomorrow -> "فردا"
                            else -> "${JalaliCalendar.toPersianDigits(daysLeft.toString())} روز دیگر"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (daysLeft <= 7) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف تولد",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBirthdaySheet(
    onDismiss: () -> Unit,
    onSave: (name: String, day: Int, month: Int, year: Int?, hasAlarm: Boolean, notes: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf("") }
    var day by remember { mutableIntStateOf(1) }
    var month by remember { mutableIntStateOf(1) }
    var yearStr by remember { mutableStateOf("") }
    var hasAlarm by remember { mutableStateOf(true) }
    var notes by remember { mutableStateOf("") }
    var monthMenuExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎂 افزودن تولد و مناسبت جدید",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "بستن")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("نام شخص (مثلاً: مامان، سارا، علی...)") },
                placeholder = { Text("نام دوست یا خانواده") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bday_name_input"),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Day and Month Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Day input (1..31)
                OutlinedTextField(
                    value = if (day > 0) day.toString() else "",
                    onValueChange = {
                        val d = it.toIntOrNull()
                        if (d != null && d in 1..31) day = d
                        else if (it.isBlank()) day = 0
                    },
                    label = { Text("روز (۱-۳۱)") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("bday_day_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                // Month selection
                ExposedDropdownMenuBox(
                    expanded = monthMenuExpanded,
                    onExpandedChange = { monthMenuExpanded = it },
                    modifier = Modifier.weight(1.5f)
                ) {
                    OutlinedTextField(
                        value = JalaliCalendar.MON_NAMES.getOrElse(month - 1) { "" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("ماه") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = monthMenuExpanded,
                        onDismissRequest = { monthMenuExpanded = false }
                    ) {
                        JalaliCalendar.MON_NAMES.forEachIndexed { index, mName ->
                            DropdownMenuItem(
                                text = { Text(mName) },
                                onClick = {
                                    month = index + 1
                                    monthMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Optional Year
            OutlinedTextField(
                value = yearStr,
                onValueChange = { yearStr = it },
                label = { Text("سال تولد (اختیاری جهت محاسبه سن، مثلاً ۱۳۷۲)") },
                placeholder = { Text("مثلاً ۱۳۷۰") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bday_year_input"),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Notes / Gift ideas
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("یادداشت یا ایده هدیه (اختیاری)") },
                placeholder = { Text("مثلاً: خرید کتاب یا ساعت...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Alarm toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "زنگ آلارم صبحگاهی (ساعت ۰۹:۰۰)",
                        fontSize = 14.sp
                    )
                }

                Switch(
                    checked = hasAlarm,
                    onCheckedChange = { hasAlarm = it }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (name.isNotBlank() && day in 1..31) {
                        onSave(
                            name.trim(),
                            day,
                            month,
                            yearStr.trim().toIntOrNull(),
                            hasAlarm,
                            notes.trim()
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_bday_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(imageVector = Icons.Default.Cake, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ثبت تولد در تقویم",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
