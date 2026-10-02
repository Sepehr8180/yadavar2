package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.AlarmEntity
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.PriorityLow
import com.example.ui.theme.PriorityNormal
import com.example.ui.theme.PriorityUrgent
import com.example.util.JalaliCalendar
import kotlin.math.roundToInt

@Composable
fun EisenhowerMatrixScreen(
    alarms: List<AlarmEntity>,
    onToggleDone: (AlarmEntity) -> Unit,
    onChangePriority: (AlarmEntity, String) -> Unit,
    onDeleteAlarm: (AlarmEntity) -> Unit,
    onAddInQuadrant: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTasks = remember(alarms) { alarms.filter { !it.isDone } }

    val urgentTasks = remember(activeTasks) { activeTasks.filter { it.prio == "urgent" } }
    val highTasks = remember(activeTasks) { activeTasks.filter { it.prio == "high" } }
    val normalTasks = remember(activeTasks) { activeTasks.filter { it.prio == "normal" } }
    val lowTasks = remember(activeTasks) { activeTasks.filter { it.prio == "low" } }

    // Quadrant Bounds for Drag and Drop detection
    val quadrantBounds = remember { mutableStateMapOf<String, Rect>() }

    // Drag and Drop state
    var draggedTask by remember { mutableStateOf<AlarmEntity?>(null) }
    var dragGlobalPosition by remember { mutableStateOf(Offset.Zero) }
    var hoveredQuadrant by remember { mutableStateOf<String?>(null) }

    fun findHoveredQuadrant(globalPos: Offset): String? {
        for ((prio, rect) in quadrantBounds) {
            if (rect.contains(globalPos)) {
                return prio
            }
        }
        return null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Instructions banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ماتریس آیزنهاور (تقسیم ۴ بخشی)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // The 2x2 Grid forming a "+" (Cross) with equal sizes
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Half: Urgent & Important (urgent) | Not Urgent & Important (high)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Top-Right: Urgent & Important
                        QuadrantBox(
                            title = "فوری و مهم",
                            subtitle = "اقدام فوری",
                            tasks = urgentTasks,
                            prioKey = "urgent",
                            accentColor = PriorityUrgent,
                            isHovered = hoveredQuadrant == "urgent" && draggedTask?.prio != "urgent",
                            onPositioned = { rect -> quadrantBounds["urgent"] = rect },
                            onToggleDone = onToggleDone,
                            onChangePriority = onChangePriority,
                            onDelete = onDeleteAlarm,
                            onAddTask = { onAddInQuadrant("urgent") },
                            onStartDrag = { task, pos ->
                                draggedTask = task
                                dragGlobalPosition = pos
                                hoveredQuadrant = findHoveredQuadrant(pos)
                            },
                            onDrag = { pos ->
                                dragGlobalPosition = pos
                                hoveredQuadrant = findHoveredQuadrant(pos)
                            },
                            onEndDrag = {
                                val target = hoveredQuadrant
                                if (target != null && draggedTask != null && target != draggedTask!!.prio) {
                                    onChangePriority(draggedTask!!, target)
                                }
                                draggedTask = null
                                hoveredQuadrant = null
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // Top-Left: Not Urgent but Important
                        QuadrantBox(
                            title = "غیرفوری و مهم",
                            subtitle = "برنامه‌ریزی و رشد",
                            tasks = highTasks,
                            prioKey = "high",
                            accentColor = PriorityHigh,
                            isHovered = hoveredQuadrant == "high" && draggedTask?.prio != "high",
                            onPositioned = { rect -> quadrantBounds["high"] = rect },
                            onToggleDone = onToggleDone,
                            onChangePriority = onChangePriority,
                            onDelete = onDeleteAlarm,
                            onAddTask = { onAddInQuadrant("high") },
                            onStartDrag = { task, pos ->
                                draggedTask = task
                                dragGlobalPosition = pos
                                hoveredQuadrant = findHoveredQuadrant(pos)
                            },
                            onDrag = { pos ->
                                dragGlobalPosition = pos
                                hoveredQuadrant = findHoveredQuadrant(pos)
                            },
                            onEndDrag = {
                                val target = hoveredQuadrant
                                if (target != null && draggedTask != null && target != draggedTask!!.prio) {
                                    onChangePriority(draggedTask!!, target)
                                }
                                draggedTask = null
                                hoveredQuadrant = null
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Bottom Half: Urgent but Not Important (normal) | Not Urgent & Not Important (low)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Bottom-Right: Urgent but Not Important
                        QuadrantBox(
                            title = "فوری و کم‌اهمیت",
                            subtitle = "تفویض یا سریع",
                            tasks = normalTasks,
                            prioKey = "normal",
                            accentColor = PriorityNormal,
                            isHovered = hoveredQuadrant == "normal" && draggedTask?.prio != "normal",
                            onPositioned = { rect -> quadrantBounds["normal"] = rect },
                            onToggleDone = onToggleDone,
                            onChangePriority = onChangePriority,
                            onDelete = onDeleteAlarm,
                            onAddTask = { onAddInQuadrant("normal") },
                            onStartDrag = { task, pos ->
                                draggedTask = task
                                dragGlobalPosition = pos
                                hoveredQuadrant = findHoveredQuadrant(pos)
                            },
                            onDrag = { pos ->
                                dragGlobalPosition = pos
                                hoveredQuadrant = findHoveredQuadrant(pos)
                            },
                            onEndDrag = {
                                val target = hoveredQuadrant
                                if (target != null && draggedTask != null && target != draggedTask!!.prio) {
                                    onChangePriority(draggedTask!!, target)
                                }
                                draggedTask = null
                                hoveredQuadrant = null
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // Bottom-Left: Not Urgent & Not Important
                        QuadrantBox(
                            title = "غیرفوری و غیرمهم",
                            subtitle = "حذف / تفریح",
                            tasks = lowTasks,
                            prioKey = "low",
                            accentColor = PriorityLow,
                            isHovered = hoveredQuadrant == "low" && draggedTask?.prio != "low",
                            onPositioned = { rect -> quadrantBounds["low"] = rect },
                            onToggleDone = onToggleDone,
                            onChangePriority = onChangePriority,
                            onDelete = onDeleteAlarm,
                            onAddTask = { onAddInQuadrant("low") },
                            onStartDrag = { task, pos ->
                                draggedTask = task
                                dragGlobalPosition = pos
                                hoveredQuadrant = findHoveredQuadrant(pos)
                            },
                            onDrag = { pos ->
                                dragGlobalPosition = pos
                                hoveredQuadrant = findHoveredQuadrant(pos)
                            },
                            onEndDrag = {
                                val target = hoveredQuadrant
                                if (target != null && draggedTask != null && target != draggedTask!!.prio) {
                                    onChangePriority(draggedTask!!, target)
                                }
                                draggedTask = null
                                hoveredQuadrant = null
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Center Plus Badge Icon at exact intersection
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Floating drag visual shadow
        if (draggedTask != null) {
            Surface(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (dragGlobalPosition.x - 120).roundToInt(),
                            y = (dragGlobalPosition.y - 60).roundToInt()
                        )
                    }
                    .size(width = 180.dp, height = 50.dp)
                    .zIndex(100f)
                    .shadow(12.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = draggedTask!!.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun QuadrantBox(
    title: String,
    subtitle: String,
    tasks: List<AlarmEntity>,
    prioKey: String,
    accentColor: Color,
    isHovered: Boolean,
    onPositioned: (Rect) -> Unit,
    onToggleDone: (AlarmEntity) -> Unit,
    onChangePriority: (AlarmEntity, String) -> Unit,
    onDelete: (AlarmEntity) -> Unit,
    onAddTask: () -> Unit,
    onStartDrag: (AlarmEntity, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onEndDrag: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxHeight()
            .onGloballyPositioned { coords ->
                onPositioned(coords.boundsInRoot())
            }
            .border(
                width = if (isHovered) 3.dp else 1.dp,
                color = if (isHovered) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHovered) accentColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHovered) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(accentColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = accentColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = JalaliCalendar.toPersianDigits(tasks.size.toString()),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    IconButton(
                        onClick = onAddTask,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "افزودن به این بخش",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Task list in this quadrant
            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isHovered) "👇 اینجا رها کنید" else "خالی (برای جابجایی بکشید اینجا)",
                        fontSize = 10.sp,
                        color = if (isHovered) accentColor else MaterialTheme.colorScheme.outline,
                        fontWeight = if (isHovered) FontWeight.Bold else FontWeight.Normal
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        DraggableMatrixItem(
                            task = task,
                            accentColor = accentColor,
                            onToggleDone = { onToggleDone(task) },
                            onChangePriority = { newPrio -> onChangePriority(task, newPrio) },
                            onDelete = { onDelete(task) },
                            onStartDrag = onStartDrag,
                            onDrag = onDrag,
                            onEndDrag = onEndDrag
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DraggableMatrixItem(
    task: AlarmEntity,
    accentColor: Color,
    onToggleDone: () -> Unit,
    onChangePriority: (String) -> Unit,
    onDelete: () -> Unit,
    onStartDrag: (AlarmEntity, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onEndDrag: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var itemOffsetInRoot by remember { mutableStateOf(Offset.Zero) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                itemOffsetInRoot = coords.boundsInRoot().topLeft
            }
            .pointerInput(task) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { localOffset ->
                        onStartDrag(task, itemOffsetInRoot + localOffset)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        itemOffsetInRoot += dragAmount
                        onDrag(itemOffsetInRoot)
                    },
                    onDragEnd = {
                        onEndDrag()
                    },
                    onDragCancel = {
                        onEndDrag()
                    }
                )
            },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onToggleDone,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RadioButtonUnchecked,
                        contentDescription = "انجام شد",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = task.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "انتقال",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(14.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("انتقال به ۱. فوری و مهم", fontSize = 12.sp) },
                        onClick = {
                            onChangePriority("urgent")
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("انتقال به ۲. غیرفوری و مهم", fontSize = 12.sp) },
                        onClick = {
                            onChangePriority("high")
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("انتقال به ۳. فوری و کم‌اهمیت", fontSize = 12.sp) },
                        onClick = {
                            onChangePriority("normal")
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("انتقال به ۴. غیرفوری و غیرمهم", fontSize = 12.sp) },
                        onClick = {
                            onChangePriority("low")
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("حذف تسک", fontSize = 12.sp, color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            onDelete()
                            menuExpanded = false
                        }
                    )
                }
            }
        }
    }
}
