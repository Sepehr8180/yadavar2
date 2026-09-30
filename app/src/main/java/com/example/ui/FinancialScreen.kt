package com.example.ui

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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FinancialEntity
import com.example.ui.theme.FinancialContainer
import com.example.ui.theme.FinancialOnContainer
import com.example.ui.theme.FinancialPrimary
import com.example.util.JalaliCalendar
import com.example.util.JalaliDate

@Composable
fun FinancialScreen(
    items: List<FinancialEntity>,
    currentYear: Int,
    currentMonth: Int,
    selectedDate: JalaliDate,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (FinancialEntity) -> Unit,
    onDelete: (FinancialEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val monthName = JalaliCalendar.MON_NAMES[currentMonth - 1]
    val monthItems = remember(items, currentYear, currentMonth) {
        items.mapNotNull { item ->
            item.occurrenceInMonth(currentYear, currentMonth)?.let { occurrence -> item to occurrence }
        }
    }
    val total = monthItems.sumOf { it.first.amount }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = FinancialContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("برنامه مالی", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = FinancialOnContainer)
                            Text("اقساط و بدهی‌های ثبت‌شده در تقویم", fontSize = 12.sp, color = FinancialOnContainer.copy(alpha = 0.75f))
                        }
                        Surface(color = FinancialPrimary, shape = CircleShape) {
                            Box(modifier = Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Payments, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("مجموع اقساط این ماه", fontSize = 12.sp, color = FinancialOnContainer)
                    Text(
                        "${JalaliCalendar.formatNumber(total)} تومان",
                        fontSize = 27.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = FinancialPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${JalaliCalendar.toPersianDigits(monthItems.size.toString())} سررسید در $monthName",
                        fontSize = 12.sp,
                        color = FinancialOnContainer.copy(alpha = 0.75f)
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNextMonth) { Icon(Icons.Default.ChevronLeft, "ماه بعد") }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$monthName ${JalaliCalendar.toPersianDigits(currentYear.toString())}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("نمایش سررسیدهای این ماه", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                }
                IconButton(onClick = onPrevMonth) { Icon(Icons.Default.ChevronRight, "ماه قبل") }
            }
        }

        if (monthItems.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 22.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Payments, null, tint = FinancialPrimary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("برای این ماه قسط یا بدهی ثبت نشده است", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = onAdd, shape = RoundedCornerShape(12.dp)) { Text("ثبت اولین مورد") }
                    }
                }
            }
        } else {
            items(monthItems, key = { "financial-${it.first.id}" }) { (item, occurrence) ->
                FinancialItemCard(item, occurrence, onEdit = { onEdit(item) }, onDelete = { onDelete(item) })
            }
        }

        item {
            Spacer(modifier = Modifier.height(88.dp))
        }
    }
}

@Composable
private fun FinancialItemCard(
    item: FinancialEntity,
    occurrence: JalaliDate,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = FinancialContainer, shape = CircleShape) {
                    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Payments, null, tint = FinancialPrimary, modifier = Modifier.size(21.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "سررسید: ${JalaliCalendar.toPersianDigits(occurrence.day.toString())} ${JalaliCalendar.MON_NAMES[occurrence.month - 1]}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Text("${item.formattedAmount()} تومان", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = FinancialPrimary)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = FinancialContainer, shape = RoundedCornerShape(7.dp)) {
                    Text(
                        item.frequencyLabel(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinancialOnContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Edit, "ویرایش", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Delete, "حذف", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f), modifier = Modifier.size(18.dp))
                }
            }
            if (item.notes.isNotBlank()) {
                Text(item.notes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
