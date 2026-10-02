package com.example

import android.Manifest
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
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Payments
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
import com.example.ui.FinancialScreen
import com.example.ui.FinancialSheet
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
                val permissionsLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val notifGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: true
                    if (!notifGranted) {
                        Toast.makeText(
                            this,
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

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    YadavarMainScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YadavarMainScreen(viewModel: MainViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val allAlarms by viewModel.allAlarms.collectAsStateWithLifecycle()
    val allBirthdays by viewModel.allBirthdays.collectAsStateWithLifecycle()
    val allFinancialItems by viewModel.allFinancialItems.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val calendarYear by viewModel.calendarYear.collectAsStateWithLifecycle()
    val calendarMonth by viewModel.calendarMonth.collectAsStateWithLifecycle()
    val filterPrio by viewModel.filterPrio.collectAsStateWithLifecycle()
    val filterTag by viewModel.filterTag.collectAsStateWithLifecycle()
    val showAddEditSheet by viewModel.showAddEditSheet.collectAsStateWithLifecycle()
    val editingAlarm by viewModel.editingAlarm.collectAsStateWithLifecycle()
    val showAddBirthdaySheet by viewModel.showAddBirthdaySheet.collectAsStateWithLifecycle()
    val showFinancialSheet by viewModel.showFinancialSheet.collectAsStateWithLifecycle()
    val editingFinancial by viewModel.editingFinancial.collectAsStateWithLifecycle()
    val showAiSettingsDialog by viewModel.showAiSettingsDialog.collectAsStateWithLifecycle()
    val isAiProcessing by viewModel.isAiProcessing.collectAsStateWithLifecycle()
    val customApiKey by viewModel.customApiKey.collectAsStateWithLifecycle()
    val initialQuadrant by viewModel.initialQuadrant.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "یادآور",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
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

                    // The old phone-alarm test button is replaced by Birthday access.
                    IconButton(
                        onClick = { viewModel.openBirthdays() },
                        modifier = Modifier.testTag("topbar_birthday_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cake,
                            contentDescription = "تولدها",
                            tint = MaterialTheme.colorScheme.secondary
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
                NavigationBarItem(
                    selected = selectedTab == MainViewModel.TAB_ALARMS,
                    onClick = { viewModel.setSelectedTab(MainViewModel.TAB_ALARMS) },
                    icon = {
                        Icon(
                            if (selectedTab == MainViewModel.TAB_ALARMS) Icons.Default.Alarm else Icons.Outlined.Alarm,
                            "آلارم‌ها"
                        )
                    },
                    label = { Text("آلارم‌ها", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == MainViewModel.TAB_CALENDAR,
                    onClick = { viewModel.setSelectedTab(MainViewModel.TAB_CALENDAR) },
                    icon = {
                        Icon(
                            if (selectedTab == MainViewModel.TAB_CALENDAR) Icons.Default.CalendarMonth else Icons.Outlined.CalendarMonth,
                            "تقویم"
                        )
                    },
                    label = { Text("تقویم", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == MainViewModel.TAB_MATRIX,
                    onClick = { viewModel.setSelectedTab(MainViewModel.TAB_MATRIX) },
                    icon = {
                        Icon(
                            if (selectedTab == MainViewModel.TAB_MATRIX) Icons.Default.GridView else Icons.Outlined.GridView,
                            "آیزنهاور"
                        )
                    },
                    label = { Text("آیزنهاور", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == MainViewModel.TAB_FINANCIAL,
                    onClick = { viewModel.setSelectedTab(MainViewModel.TAB_FINANCIAL) },
                    icon = {
                        Icon(
                            if (selectedTab == MainViewModel.TAB_FINANCIAL) Icons.Default.Payments else Icons.Outlined.Payments,
                            "مالی"
                        )
                    },
                    label = { Text("مالی", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == MainViewModel.TAB_COMPLETED,
                    onClick = { viewModel.setSelectedTab(MainViewModel.TAB_COMPLETED) },
                    icon = {
                        Icon(
                            if (selectedTab == MainViewModel.TAB_COMPLETED) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                            "سوابق"
                        )
                    },
                    label = { Text("سوابق", fontSize = 11.sp) }
                )
            }
        },
        floatingActionButton = {
            when (selectedTab) {
                MainViewModel.TAB_ALARMS -> ExtendedFloatingActionButton(
                    onClick = { viewModel.openAddAlarm() },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_alarm_fab")
                ) {
                    Icon(Icons.Default.Add, "یادآور جدید")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("یادآور جدید", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                MainViewModel.TAB_BIRTHDAYS -> ExtendedFloatingActionButton(
                    onClick = { viewModel.openAddBirthday() },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.testTag("add_birthday_fab")
                ) {
                    Icon(Icons.Default.Cake, "ثبت تولد")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ثبت تولد", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                MainViewModel.TAB_FINANCIAL -> ExtendedFloatingActionButton(
                    onClick = { viewModel.openAddFinancial() },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_financial_fab")
                ) {
                    Icon(Icons.Default.Add, "قسط جدید")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ثبت قسط / بدهی", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_transition"
            ) { tab ->
                when (tab) {
                    MainViewModel.TAB_ALARMS -> AlarmListScreen(
                        alarms = allAlarms,
                        selectedPrio = filterPrio,
                        selectedTag = filterTag,
                        isAiProcessing = isAiProcessing,
                        onAiSubmit = { text ->
                            viewModel.processVoiceOrTextWithAi(text) { resultMsg ->
                                coroutineScope.launch { snackbarHostState.showSnackbar(resultMsg) }
                            }
                        },
                        onOpenAiSettings = { viewModel.openAiSettings() },
                        onSelectPrio = { viewModel.setFilterPrio(it) },
                        onSelectTag = { viewModel.setFilterTag(it) },
                        onToggleEnabled = { viewModel.toggleAlarmEnabled(it) },
                        onToggleDone = { viewModel.toggleAlarmDone(it) },
                        onEdit = { viewModel.openEditAlarm(it) },
                        onDelete = { viewModel.deleteAlarm(it) },
                        onAddNewAlarm = { viewModel.openAddAlarm() }
                    )

                    MainViewModel.TAB_CALENDAR -> CalendarView(
                        currentYear = calendarYear,
                        currentMonth = calendarMonth,
                        selectedDate = selectedDate,
                        alarms = allAlarms,
                        birthdays = allBirthdays,
                        financialItems = allFinancialItems,
                        onSelectDate = viewModel::setSelectedDate,
                        onPrevMonth = viewModel::prevMonth,
                        onNextMonth = viewModel::nextMonth,
                        onToday = { viewModel.setSelectedDate(JalaliCalendar.today()) },
                        onAddAlarmForDate = { date ->
                            viewModel.setSelectedDate(date)
                            viewModel.openAddAlarm()
                        },
                        onToggleAlarmEnabled = viewModel::toggleAlarmEnabled,
                        onToggleAlarmDone = viewModel::toggleAlarmDone,
                        onEditAlarm = viewModel::openEditAlarm,
                        onDeleteAlarm = viewModel::deleteAlarm
                    )

                    MainViewModel.TAB_MATRIX -> EisenhowerMatrixScreen(
                        alarms = allAlarms,
                        onToggleDone = viewModel::toggleAlarmDone,
                        onChangePriority = viewModel::updateAlarmPriority,
                        onDeleteAlarm = viewModel::deleteAlarm,
                        onAddInQuadrant = { prio -> viewModel.openAddAlarm(prio) }
                    )

                    MainViewModel.TAB_FINANCIAL -> FinancialScreen(
                        items = allFinancialItems,
                        currentYear = calendarYear,
                        currentMonth = calendarMonth,
                        selectedDate = selectedDate,
                        onPrevMonth = viewModel::prevMonth,
                        onNextMonth = viewModel::nextMonth,
                        onAdd = { viewModel.openAddFinancial() },
                        onEdit = viewModel::openEditFinancial,
                        onDelete = viewModel::deleteFinancial,
                        onTogglePaid = viewModel::toggleFinancialPaid
                    )

                    MainViewModel.TAB_COMPLETED -> CompletedListScreen(
                        alarms = allAlarms,
                        onToggleDone = viewModel::toggleAlarmDone,
                        onDelete = viewModel::deleteAlarm
                    )

                    MainViewModel.TAB_BIRTHDAYS -> BirthdayScreen(
                        birthdays = allBirthdays,
                        onAddBirthdayClick = viewModel::openAddBirthday,
                        onDeleteBirthday = viewModel::deleteBirthday
                    )
                }
            }
        }
    }

    if (showAddEditSheet) {
        AddEditAlarmSheet(
            editingAlarm = editingAlarm,
            initialDate = selectedDate,
            defaultPriority = initialQuadrant,
            onDismiss = viewModel::closeAddEditSheet,
            onSave = { id, title, hour, minute, isDaily, repeatDays, jalaliDate, hasDate, prio, tag, isVibrate, snoozeMinutes, hasAlarm ->
                viewModel.saveAlarm(
                    id, title, hour, minute, isDaily, repeatDays, jalaliDate, hasDate,
                    prio, tag, isVibrate, snoozeMinutes, hasAlarm
                )
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        if (hasAlarm) "آلارم «$title» تنظیم شد" else "یادآور «$title» در ماتریس آیزنهاور ثبت شد"
                    )
                }
            }
        )
    }

    if (showAddBirthdaySheet) {
        AddBirthdaySheet(
            onDismiss = viewModel::closeAddBirthday,
            onSave = { name, day, month, year, hasAlarm, notes ->
                viewModel.saveBirthday(name, day, month, year, hasAlarm, notes)
                coroutineScope.launch { snackbarHostState.showSnackbar("تولد «$name» در تقویم ثبت شد 🎂") }
            }
        )
    }

    if (showFinancialSheet) {
        FinancialSheet(
            editingItem = editingFinancial,
            initialDate = selectedDate,
            onDismiss = viewModel::closeFinancialSheet,
            onSave = { id, title, amount, date, isMonthly, endDate, notes ->
                viewModel.saveFinancial(id, title, amount, date, isMonthly, endDate, notes)
                coroutineScope.launch { snackbarHostState.showSnackbar("«$title» در برنامه مالی ثبت شد") }
            }
        )
    }

    if (showAiSettingsDialog) {
        AiSettingsDialog(
            currentApiKey = customApiKey,
            onDismiss = viewModel::closeAiSettings,
            onSaveKey = { newKey ->
                viewModel.saveCustomApiKey(newKey)
                coroutineScope.launch { snackbarHostState.showSnackbar("تنظیمات هوش مصنوعی ذخیره شد ✨") }
            }
        )
    }
}
