package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AddBirthdaySheet
import com.example.ui.AddEditAlarmSheet
import com.example.ui.AiSettingsDialog
import com.example.ui.AlarmListScreen
import com.example.ui.BirthdayScreen
import com.example.ui.CalendarView
import com.example.ui.CompletedListScreen
import com.example.ui.EisenhowerMatrixScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.util.JalaliCalendar
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme(darkTheme = false) {
                val context = LocalContext.current

                // Request Permissions on Android 13+ (Notifications and Audio Recording for Voice Commands)
                val permissionsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val notifGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: true
                    val micGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: true
                    if (!notifGranted) {
                        Toast.makeText(
                            context,
                            "برای پخش به موقع آلارم‌ها، اجازه نمایش اعلان نیاز است",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                LaunchedEffect(Unit) {
                    val permsToRequest = mutableListOf(Manifest.permission.RECORD_AUDIO)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permissionsLauncher.launch(permsToRequest.toTypedArray())
                }

                // Force RTL for Persian Interface
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    YadavarMainScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YadavarMainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val allAlarms by viewModel.allAlarms.collectAsStateWithLifecycle()
    val allBirthdays by viewModel.allBirthdays.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val calendarYear by viewModel.calendarYear.collectAsStateWithLifecycle()
    val calendarMonth by viewModel.calendarMonth.collectAsStateWithLifecycle()
    val filterPrio by viewModel.filterPrio.collectAsStateWithLifecycle()
    val filterTag by viewModel.filterTag.collectAsStateWithLifecycle()
    val showAddEditSheet by viewModel.showAddEditSheet.collectAsStateWithLifecycle()
    val editingAlarm by viewModel.editingAlarm.collectAsStateWithLifecycle()
    val showAddBirthdaySheet by viewModel.showAddBirthdaySheet.collectAsStateWithLifecycle()
    val showAiSettingsDialog by viewModel.showAiSettingsDialog.collectAsStateWithLifecycle()
    val isAiProcessing by viewModel.isAiProcessing.collectAsStateWithLifecycle()
    val customApiKey by viewModel.customApiKey.collectAsStateWithLifecycle()
    val initialQuadrant by viewModel.initialQuadrant.collectAsStateWithLifecycle()

    val activeCount = remember(allAlarms) {
        allAlarms.count { it.isEnabled && !it.isDone }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "یادآور",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "آلارم گوشی",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    // AI Settings Button
                    IconButton(
                        onClick = { viewModel.openAiSettings() },
                        modifier = Modifier.testTag("topbar_ai_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "تنظیمات هوش مصنوعی",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    // Quick Test Alarm Trigger button in TopBar
                    IconButton(
                        onClick = {
                            viewModel.testAlarmTrigger()
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("🔔 زنگ آلارم گوشی تست شد!")
                            }
                        },
                        modifier = Modifier.testTag("topbar_test_alarm_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "تست زنگ آلارم",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                // Tab 0: Alarms
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Default.Alarm else Icons.Outlined.Alarm,
                            contentDescription = "آلارم‌ها"
                        )
                    },
                    label = { Text("آلارم‌ها", fontSize = 11.sp) }
                )

                // Tab 1: Solar Calendar
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 1) Icons.Default.CalendarMonth else Icons.Outlined.CalendarMonth,
                            contentDescription = "تقویم"
                        )
                    },
                    label = { Text("تقویم", fontSize = 11.sp) }
                )

                // Tab 2: Eisenhower Matrix
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.setSelectedTab(2) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 2) Icons.Default.GridView else Icons.Outlined.GridView,
                            contentDescription = "آیزنهاور"
                        )
                    },
                    label = { Text("آیزنهاور", fontSize = 11.sp) }
                )

                // Tab 3: Birthdays
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { viewModel.setSelectedTab(3) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 3) Icons.Default.Cake else Icons.Outlined.Cake,
                            contentDescription = "تولدها"
                        )
                    },
                    label = { Text("تولدها", fontSize = 11.sp) }
                )

                // Tab 4: Completed
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { viewModel.setSelectedTab(4) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 4) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                            contentDescription = "انجام شده"
                        )
                    },
                    label = { Text("سوابق", fontSize = 11.sp) }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab != 3) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.openAddAlarm() },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_alarm_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "آلارم جدید")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "آلارم جدید", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            } else {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.openAddBirthday() },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.testTag("add_birthday_fab")
                ) {
                    Icon(imageVector = Icons.Default.Cake, contentDescription = "ثبت تولد")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "ثبت تولد", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_transition"
            ) { tab ->
                when (tab) {
                    0 -> AlarmListScreen(
                        alarms = allAlarms,
                        selectedPrio = filterPrio,
                        selectedTag = filterTag,
                        isAiProcessing = isAiProcessing,
                        onAiSubmit = { text ->
                            viewModel.processVoiceOrTextWithAi(text) { resultMsg ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(resultMsg)
                                }
                            }
                        },
                        onOpenAiSettings = { viewModel.openAiSettings() },
                        onSelectPrio = { viewModel.setFilterPrio(it) },
                        onSelectTag = { viewModel.setFilterTag(it) },
                        onToggleEnabled = { viewModel.toggleAlarmEnabled(it) },
                        onToggleDone = { viewModel.toggleAlarmDone(it) },
                        onEdit = { viewModel.openEditAlarm(it) },
                        onDelete = { viewModel.deleteAlarm(it) },
                        onTestAlarm = { alarm ->
                            viewModel.testAlarmTrigger(alarm.title, alarm.prio, alarm.tag)
                        },
                        onTestAlarmNow = {
                            viewModel.testAlarmTrigger()
                        },
                        onAddNewAlarm = { viewModel.openAddAlarm() }
                    )
                    1 -> CalendarView(
                        currentYear = calendarYear,
                        currentMonth = calendarMonth,
                        selectedDate = selectedDate,
                        alarms = allAlarms,
                        birthdays = allBirthdays,
                        onSelectDate = { viewModel.setSelectedDate(it) },
                        onPrevMonth = { viewModel.prevMonth() },
                        onNextMonth = { viewModel.nextMonth() },
                        onToday = { viewModel.setSelectedDate(JalaliCalendar.today()) },
                        onAddAlarmForDate = { date ->
                            viewModel.setSelectedDate(date)
                            viewModel.openAddAlarm()
                        },
                        onToggleAlarmEnabled = { viewModel.toggleAlarmEnabled(it) },
                        onToggleAlarmDone = { viewModel.toggleAlarmDone(it) },
                        onEditAlarm = { viewModel.openEditAlarm(it) },
                        onDeleteAlarm = { viewModel.deleteAlarm(it) },
                        onTestAlarm = { alarm ->
                            viewModel.testAlarmTrigger(alarm.title, alarm.prio, alarm.tag)
                        }
                    )
                    2 -> EisenhowerMatrixScreen(
                        alarms = allAlarms,
                        onToggleDone = { viewModel.toggleAlarmDone(it) },
                        onChangePriority = { alarm, newPrio ->
                            viewModel.updateAlarmPriority(alarm, newPrio)
                        },
                        onDeleteAlarm = { viewModel.deleteAlarm(it) },
                        onAddInQuadrant = { prio ->
                            viewModel.openAddAlarm(defaultPrio = prio)
                        }
                    )
                    3 -> BirthdayScreen(
                        birthdays = allBirthdays,
                        onAddBirthdayClick = { viewModel.openAddBirthday() },
                        onDeleteBirthday = { viewModel.deleteBirthday(it) }
                    )
                    4 -> CompletedListScreen(
                        alarms = allAlarms,
                        onToggleDone = { viewModel.toggleAlarmDone(it) },
                        onDelete = { viewModel.deleteAlarm(it) }
                    )
                }
            }
        }
    }

    // Add / Edit Alarm Bottom Sheet
    if (showAddEditSheet) {
        AddEditAlarmSheet(
            editingAlarm = editingAlarm,
            initialDate = selectedDate,
            defaultPriority = initialQuadrant,
            onDismiss = { viewModel.closeAddEditSheet() },
            onSave = { id, title, hour, minute, isDaily, repeatDays, jalaliDate, prio, tag, isVibrate, snoozeMinutes ->
                viewModel.saveAlarm(
                    id = id,
                    title = title,
                    hour = hour,
                    minute = minute,
                    isDaily = isDaily,
                    repeatDays = repeatDays,
                    jalaliDate = jalaliDate,
                    prio = prio,
                    tag = tag,
                    isVibrate = isVibrate,
                    snoozeMinutes = snoozeMinutes
                )
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("آلارم «$title» تنظیم شد")
                }
            }
        )
    }

    // Add Birthday Bottom Sheet
    if (showAddBirthdaySheet) {
        AddBirthdaySheet(
            onDismiss = { viewModel.closeAddBirthday() },
            onSave = { name, day, month, year, hasAlarm, notes ->
                viewModel.saveBirthday(name, day, month, year, hasAlarm, notes)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("تولد «$name» در تقویم ثبت شد 🎂")
                }
            }
        )
    }

    // AI Settings Dialog
    if (showAiSettingsDialog) {
        AiSettingsDialog(
            currentApiKey = customApiKey,
            onDismiss = { viewModel.closeAiSettings() },
            onSaveKey = { newKey ->
                viewModel.saveCustomApiKey(newKey)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("تنظیمات هوش مصنوعی ذخیره شد ✨")
                }
            }
        )
    }
}
