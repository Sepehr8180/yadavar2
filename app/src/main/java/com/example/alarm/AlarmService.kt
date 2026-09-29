package com.example.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.util.JalaliCalendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AlarmService : Service() {

    companion object {
        const val TAG = "AlarmService"
        const val CHANNEL_ID = "yadavar_alarm_channel"
        const val NOTIFICATION_ID = 9999

        const val ACTION_START_RINGING = "com.example.yadavar.ACTION_START_RINGING"
        const val ACTION_STOP_RINGING = "com.example.yadavar.ACTION_STOP_RINGING"

        var isCurrentlyRinging: Boolean = false
            private set
        var currentAlarmTitle: String = ""
            private set
        var currentAlarmTag: String = ""
            private set
        var currentAlarmPrio: String = ""
            private set
        var currentAlarmId: Long = -1L
            private set
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Yadavar:AlarmWakeLock"
        ).apply {
            setReferenceCounted(false)
        }

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_STOP_RINGING) {
            stopAlarm()
            stopSelf()
            return START_NOT_STICKY
        }

        val alarmId = intent?.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L) ?: -1L
        val title = intent?.getStringExtra(AlarmScheduler.EXTRA_ALARM_TITLE) ?: "زمان یادآوری!"
        val tag = intent?.getStringExtra(AlarmScheduler.EXTRA_ALARM_TAG) ?: "یادآور"
        val prio = intent?.getStringExtra(AlarmScheduler.EXTRA_ALARM_PRIO) ?: "high"

        currentAlarmId = alarmId
        currentAlarmTitle = title
        currentAlarmTag = tag
        currentAlarmPrio = prio
        isCurrentlyRinging = true

        wakeLock?.acquire(10 * 60 * 1000L) // 10 minutes max

        startForeground(NOTIFICATION_ID, buildAlarmNotification(alarmId, title, tag, prio))
        startRingingSoundAndVibration()

        // Auto-dismiss after 10 minutes to save battery
        serviceScope.launch {
            delay(10 * 60 * 1000L)
            if (isCurrentlyRinging) {
                stopAlarm()
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun startRingingSoundAndVibration() {
        try {
            var alarmUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmService, alarmUri!!)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing alarm sound", e)
        }

        // Vibrate
        try {
            val pattern = longArrayOf(0, 600, 400, 600, 400, 800)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting vibrator", e)
        }
    }

    private fun buildAlarmNotification(alarmId: Long, title: String, tag: String, prio: String): Notification {
        // Full screen ringing activity intent
        val fullScreenIntent = Intent(this, AlarmRingingActivity::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_ALARM_TITLE, title)
            putExtra(AlarmScheduler.EXTRA_ALARM_TAG, tag)
            putExtra(AlarmScheduler.EXTRA_ALARM_PRIO, prio)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            alarmId.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss action
        val dismissIntent = Intent(this, AlarmActionReceiver::class.java).apply {
            action = AlarmActionReceiver.ACTION_DISMISS
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this,
            (alarmId + 200000).toInt(),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze action
        val snoozeIntent = Intent(this, AlarmActionReceiver::class.java).apply {
            action = AlarmActionReceiver.ACTION_SNOOZE
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_ALARM_TITLE, title)
            putExtra(AlarmScheduler.EXTRA_ALARM_TAG, tag)
            putExtra(AlarmScheduler.EXTRA_ALARM_PRIO, prio)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this,
            (alarmId + 300000).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val currentTime = JalaliCalendar.formatTime(
            java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY),
            java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE)
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.yadavar_alarm_icon_1790692296560)
            .setContentTitle("⏰ $title ($currentTime)")
            .setContentText("دسته: $tag · زنگ آلارم گوشی در حال پخش است")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(0, "🛑 قطع آلارم", dismissPendingIntent)
            .addAction(0, "⏱ تعویق ۱۰ دقیقه", snoozePendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "آلارم و زنگ یادآور",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "پخش صدای آلارم تمام صفحه و بیدارباش"
                enableVibration(true)
                setSound(null, null) // Handled manually by MediaPlayer for loop control
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun stopAlarm() {
        isCurrentlyRinging = false
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player", e)
        }

        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping vibrator", e)
        }

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing wake lock", e)
        }
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }
}
