package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FinancialEntity
import com.example.ui.theme.FinancialContainer
import com.example.ui.theme.FinancialOnContainer
import com.example.ui.theme.FinancialPrimary
import com.example.util.JalaliCalendar
import com.example.util.JalaliDate

private fun normalizeDigits(value: String): String {
    val arabic = "٠١٢٣٤٥٦٧٨٩"
    val persian = "۰۱۲۳۴۵۶۷۸۹"
    return value.map { ch ->
        when {
            ch in persian -> ('0'.code + persian.indexOf(ch)).toChar()
            ch in arabic -> ('0'.code + arabic.indexOf(ch)).toChar()
            else -> ch
        }
    }.joinToString("")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialSheet(
    editingItem: FinancialEntity?,
    initialDate: JalaliDate,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        title: String,
        amount: Long,
        date: JalaliDate,
        isMonthly: Boolean,
        endDate: JalaliDate?,
        notes: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(editingItem?.title ?: "") }
    var amountText by remember { mutableStateOf(editingItem?.amount?.toString() ?: "") }
    var yearText by remember { mutableStateOf((editingItem?.jalaliYear ?: initialDate.year).toString()) }
    var monthText by remember { mutableStateOf((editingItem?.jalaliMonth ?: initialDate.month).toString()) }
    var dayText by remember { mutableStateOf((editingItem?.jalaliDay ?: initialDate.day).toString()) }
    var isMonthly by remember { mutableStateOf(editingItem?.isMonthly ?: false) }
    var hasEndDate by remember { mutableStateOf(editingItem?.endDate() != null) }
    var endYearText by remember { mutableStateOf(editingItem?.endJalaliYear?.toString() ?: "") }
    var endMonthText by remember { mutableStateOf(editingItem?.endJalaliMonth?.toString() ?: "") }
    var endDayText by remember { mutableStateOf(editingItem?.endJalaliDay?.toString() ?: "") }
    var notes by remember { mutableStateOf(editingItem?.notes ?: "") }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun parseDate(y: String, m: String, d: String): JalaliDate? {
        val year = normalizedDigits(y).toIntOrNull()
        val month = normalizedDigits(m).toIntOrNull()
        val day = normalizedDigits(d).toIntOrNull()
        if (year == null || month == null || day == null || month !in 1..12) return null
        if (day !in 1..JalaliCalendar.monthLength(year, month)) return null
        return JalaliDate(year, month, day)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Payments, null, tint = FinancialPrimary, modifier = Modifier.padding(end = 8.dp))
                    Text(
                        text = if (editingItem == null) "ثبت قسط / بدهی" else "ویرایش مورد مالی",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "بستن") }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان") },
                placeholder = { Text("مثلاً: قسط لپ‌تاپ، بدهی به علی...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = normalizedDigits(it).filter(Char::isDigit) },
                label = { Text("مبلغ (تومان)") },
                placeholder = { Text("مثلاً ۵۰۰۰۰۰۰") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))
            Text("تاریخ سررسید اول", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            DateFields(yearText, monthText, dayText, onYear = { yearText = it }, onMonth = { monthText = it }, onDay = { dayText = it })

            Spacer(modifier = Modifier.height(14.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = FinancialContainer),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("پرداخت تکرارشونده", fontWeight = FontWeight.Bold, color = FinancialOnContainer)
                        Text(
                            if (isMonthly) "این مبلغ هر ماه در تقویم تکرار می‌شود." else "فقط یک‌بار در تاریخ واردشده نمایش داده می‌شود.",
                            fontSize = 12.sp,
                            color = FinancialOnContainer.copy(alpha = 0.75f)
                        )
                    }
                    Switch(checked = isMonthly, onCheckedChange = { isMonthly = it })
                }
            }

            if (isMonthly) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("تاریخ پایان", fontWeight = FontWeight.Bold)
                                Text("می‌توانی پایان قسط را مشخص کنی؛ در غیر این صورت ادامه‌دار می‌ماند.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }
                            Switch(checked = hasEndDate, onCheckedChange = { hasEndDate = it })
                        }
                        if (hasEndDate) {
                            Spacer(modifier = Modifier.height(10.dp))
                            DateFields(
                                endYearText, endMonthText, endDayText,
                                onYear = { endYearText = it },
                                onMonth = { endMonthText = it },
                                onDay = { endDayText = it }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("توضیحات (اختیاری)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(14.dp)
            )

            errorText?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = {
                    val amount = normalizedDigits(amountText).toLongOrNull() ?: 0L
                    val startDate = parseDate(yearText, monthText, dayText)
                    val endDate = if (isMonthly && hasEndDate) parseDate(endYearText, endMonthText, endDayText) else null
                    errorText = when {
                        title.isBlank() -> "عنوان را وارد کن."
                        amount <= 0L -> "مبلغ باید بیشتر از صفر باشد."
                        startDate == null -> "تاریخ سررسید معتبر نیست."
                        isMonthly && hasEndDate && endDate == null -> "تاریخ پایان معتبر نیست."
                        isMonthly && endDate != null && endDate < startDate -> "تاریخ پایان نمی‌تواند قبل از شروع باشد."
                        else -> null
                    }
                    if (errorText == null && startDate != null) {
                        onSave(
                            editingItem?.id ?: 0L,
                            title,
                            amount,
                            startDate,
                            isMonthly,
                            endDate,
                            notes
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FinancialPrimary)
            ) {
                Icon(Icons.Default.Payments, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (editingItem == null) "ثبت مورد مالی" else "ذخیره تغییرات", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun DateFields(
    year: String,
    month: String,
    day: String,
    onYear: (String) -> Unit,
    onMonth: (String) -> Unit,
    onDay: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = year,
            onValueChange = { onYear(normalizeDigits(it).filter(Char::isDigit).take(4)) },
            label = { Text("سال") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1.2f),
            singleLine = true
        )
        OutlinedTextField(
            value = month,
            onValueChange = { onMonth(normalizeDigits(it).filter(Char::isDigit).take(2)) },
            label = { Text("ماه") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(0.9f),
            singleLine = true
        )
        OutlinedTextField(
            value = day,
            onValueChange = { onDay(normalizeDigits(it).filter(Char::isDigit).take(2)) },
            label = { Text("روز") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(0.9f),
            singleLine = true
        )
    }
}
