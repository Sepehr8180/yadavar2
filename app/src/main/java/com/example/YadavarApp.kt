package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import com.example.alarm.AlarmScheduler
import com.example.alarm.AlarmService
import com.example.data.AlarmRepository
import com.example.data.YadavarDatabase

class YadavarApp : Application() {

    lateinit var database: YadavarDatabase
        private set
    lateinit var repository: AlarmRepository
        private set
    lateinit var alarmScheduler: AlarmScheduler
        private set

    override fun onCreate() {
        super.onCreate()
        database = YadavarDatabase.getInstance(this)
        repository = AlarmRepository(database.alarmDao(), database.birthdayDao())
        alarmScheduler = AlarmScheduler(this)

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmChannel = NotificationChannel(
                AlarmService.CHANNEL_ID,
                "زنگ و بیدارباش یادآور",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "نمایش تمام صفحه و پخش زنگ آلارم برای یادآوری‌ها"
                enableVibration(true)
                enableLights(true)
                val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                setSound(alarmSound, audioAttributes)
            }

            val reminderChannel = NotificationChannel(
                "yadavar_reminder_channel",
                "اعلان‌های عمومی یادآور",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "اعلان زمان‌بندی یادآوری‌های روزمره و تولدها"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(alarmChannel)
            manager.createNotificationChannel(reminderChannel)
        }
    }
}
