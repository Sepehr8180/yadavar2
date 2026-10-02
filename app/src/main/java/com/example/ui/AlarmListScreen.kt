package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AlarmEntity
import com.example.util.JalaliCalendar

@Composable
fun AlarmListScreen(
    alarms: List<AlarmEntity>,
    selectedPrio: String?,
    selectedTag: String?,
    isAiProcessing: Boolean,
    onAiSubmit: (String) -> Unit,
    onOpenAiSettings: () -> Unit,
    onSelectPrio: (String?) -> Unit,
    onSelectTag: (String?) -> Unit,
    onToggleEnabled: (AlarmEntity) -> Unit,
    onToggleDone: (AlarmEntity) -> Unit,
    onEdit: (AlarmEntity) -> Unit,
    onDelete: (AlarmEntity) -> Unit,
    onAddNewAlarm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredAlarms = remember(alarms, selectedPrio, selectedTag) {
        alarms.filter { alarm ->
            alarm.hasAlarm &&
            !alarm.isDone &&
            (selectedPrio == null || alarm.prio == selectedPrio) &&
            (selectedTag == null || alarm.tag == selectedTag)
        }
    }

    val allTags = remember(alarms) {
        listOf("کار", "شخصی", "روتین", "خونه", "سلامت", "مطالعه")
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // AI Voice & Smart Text Input Bar
        item {
            AiQuickAddBar(
                isAiProcessing = isAiProcessing,
                onAiSubmit = onAiSubmit,
                onOpenAiSettings = onOpenAiSettings,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Filter chips row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedPrio == null && selectedTag == null,
                    onClick = {
                        onSelectPrio(null)
                        onSelectTag(null)
                    },
                    label = { Text("همه موارد") },
                    shape = RoundedCornerShape(10.dp)
                )

                FilterChip(
                    selected = selectedPrio == "urgent",
                    onClick = { onSelectPrio(if (selectedPrio == "urgent") null else "urgent") },
                    label = { Text("فوری") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFFDAD8)
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                FilterChip(
                    selected = selectedPrio == "high",
                    onClick = { onSelectPrio(if (selectedPrio == "high") null else "high") },
                    label = { Text("مهم") },
                    shape = RoundedCornerShape(10.dp)
                )

                for (t in allTags) {
                    FilterChip(
                        selected = selectedTag == t,
                        onClick = { onSelectTag(if (selectedTag == t) null else t) },
                        label = { Text("#$t") },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        // Alarms count banner
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "لیست یادآورها (${JalaliCalendar.toPersianDigits(filteredAlarms.size.toString())})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = JalaliCalendar.today().formatted(includeDayName = true),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // Alarms list
        if (filteredAlarms.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
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
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "هیچ یادآوری با این فیلتر وجود ندارد",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onAddNewAlarm,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("افزودن اولین یادآور")
                        }
                    }
                }
            }
        } else {
            items(filteredAlarms, key = { it.id }) { alarm ->
                AlarmCard(
                    alarm = alarm,
                    onToggleEnabled = { onToggleEnabled(alarm) },
                    onToggleDone = { onToggleDone(alarm) },
                    onEdit = { onEdit(alarm) },
                    onDelete = { onDelete(alarm) },
                                    )
            }
        }

        item {
            Spacer(modifier = Modifier.height(84.dp))
        }
    }
}
