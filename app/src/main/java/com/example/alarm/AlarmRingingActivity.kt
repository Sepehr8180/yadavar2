package com.example.alarm

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import com.example.util.JalaliCalendar
import kotlinx.coroutines.delay
import java.util.Calendar

class AlarmRingingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Turn screen on and show over lock screen like a true phone alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_TITLE) ?: "زمان یادآوری!"
        val tag = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_TAG) ?: "یادآور"
        val prio = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_PRIO) ?: "high"

        setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AlarmRingingScreen(
                        title = title,
                        tag = tag,
                        prio = prio,
                        onDismiss = {
                            dismissAlarm()
                        },
                        onSnooze = {
                            snoozeAlarm(alarmId, title, prio, tag)
                        }
                    )
                }
            }
        }
    }

    private fun dismissAlarm() {
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP_RINGING
        }
        startService(stopIntent)
        finish()
    }

    private fun snoozeAlarm(alarmId: Long, title: String, prio: String, tag: String) {
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP_RINGING
        }
        startService(stopIntent)

        val scheduler = AlarmScheduler(this)
        scheduler.scheduleSnooze(alarmId, title, prio, tag, snoozeMinutes = 10)
        finish()
    }
}

@Composable
fun AlarmRingingScreen(
    title: String,
    tag: String,
    prio: String,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    var currentTimeString by remember { mutableStateOf("") }
    var currentJalaliDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val cal = Calendar.getInstance()
            val h = cal.get(Calendar.HOUR_OF_DAY)
            val m = cal.get(Calendar.MINUTE)
            val s = cal.get(Calendar.SECOND)
            currentTimeString = JalaliCalendar.toPersianDigits(String.format("%02d:%02d:%02d", h, m, s))
            currentJalaliDate = JalaliCalendar.today().formatted(includeDayName = true)
            delay(1000L)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val prioColor = when (prio) {
        "urgent" -> Color(0xFFD32F2F)
        "high" -> Color(0xFFE65100)
        "low" -> Color(0xFF43A047)
        else -> Color(0xFF0288D1)
    }

    val prioLabel = when (prio) {
        "urgent" -> "فوری"
        "high" -> "مهم"
        "low" -> "کم"
        else -> "عادی"
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF140D0D) // Deep rich alarm background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF261010),
                            Color(0xFF120808),
                            Color(0xFF080404)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top section: Persian date & status
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 32.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x33FFB3B3)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "⏰ یادآور گوشی در حال زنگ زدن",
                            color = Color(0xFFFFB3B3),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    Text(
                        text = currentJalaliDate,
                        color = Color(0xFFE0D0C8),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                // Middle section: Pulsing bell & Clock & Reminder details
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    // Pulsing Bell Icon with glowing rings
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(160.dp)
                    ) {
                        // Outer pulse ring
                        Box(
                            modifier = Modifier
                                .size(150.dp)
                                .scale(pulseScale)
                                .border(2.dp, Color(0x44FF5252), CircleShape)
                        )
                        // Inner circle
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color(0xFFB71C1C), Color(0xFF6B0E0E))
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = "زنگ آلارم",
                                tint = Color.White,
                                modifier = Modifier.size(54.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Big Clock Display
                    Text(
                        text = currentTimeString.ifEmpty { "۰۰:۰۰:۰۰" },
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Alarm title card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF221616)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = title,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFF0EC),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = prioColor.copy(alpha = 0.25f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "اولویت $prioLabel",
                                        color = prioColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0x33FFFFFF)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "#$tag",
                                        color = Color(0xFFDDD0CB),
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom section: Large Dismiss and Snooze buttons
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Big Stop / Dismiss Button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .testTag("dismiss_alarm_button"),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.AlarmOff,
                            contentDescription = "قطع آلارم",
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "قطع آلارم",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Snooze Button
                    OutlinedButton(
                        onClick = onSnooze,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("snooze_alarm_button"),
                        shape = RoundedCornerShape(18.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(
                                listOf(Color(0xFFFFA000), Color(0xFFFFB300))
                            )
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFFB300)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Snooze,
                            contentDescription = "تعویق",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تعویق به مدت ۱۰ دقیقه (اسنوز)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
