package com.example.whatsalarm

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.app.PendingIntent
import android.app.AlarmManager
import androidx.core.app.NotificationCompat

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var lastKeyword: String? = null

    companion object {
        const val CHANNEL_ID = "whatsalarm_channel"
        const val NOTIF_ID = 1

        const val ACTION_START_ALARM = "START_ALARM"
        const val ACTION_STOP_ALARM = "STOP_ALARM"
        const val ACTION_SNOOZE_ALARM = "SNOOZE_ALARM"

        const val EXTRA_KEYWORD = "keyword"
        const val EXTRA_SNOOZE_MINUTES = "snooze_minutes"
        const val DEFAULT_SNOOZE_MINUTES = 10
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_ALARM, "START_ALARM" -> {
                if (mediaPlayer != null) return START_STICKY
                lastKeyword = intent.getStringExtra(EXTRA_KEYWORD)
                // mark alarm running
                getSharedPreferences("settings", MODE_PRIVATE)
                    .edit()
                    .putBoolean("alarm_running", true)
                    .apply()
                startForeground(NOTIF_ID, buildNotification())
                startAlarmSound()
            }

            ACTION_STOP_ALARM, "STOP_ALARM" -> {
                stopAlarm()
                stopSelf()
            }

            ACTION_SNOOZE_ALARM, "SNOOZE_ALARM" -> {
                val mins = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, DEFAULT_SNOOZE_MINUTES)
                stopAlarm()
                scheduleSnooze(mins)
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun startAlarmSound() {
        if (mediaPlayer != null) return

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val uri: Uri = prefs.getString("alarmTone", null)?.let {
            Uri.parse(it)
        } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        mediaPlayer = MediaPlayer().apply {
            setDataSource(applicationContext, uri)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            isLooping = true
            setOnPreparedListener { start() }
            prepareAsync()
        }
    }

    private fun stopAlarm() {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {}
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null

        getSharedPreferences("settings", MODE_PRIVATE)
            .edit()
            .putBoolean("alarm_running", false)
            .apply()

        try { stopForeground(true) } catch (_: Exception) {}
    }

    private fun buildNotification(): Notification {

        val popupIntent = Intent(this, AlarmPopupActivity::class.java).apply {
            putExtra(AlarmPopupActivity.EXTRA_KEYWORD, lastKeyword)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            0,
            popupIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        // Broadcast PendingIntents for Stop & Snooze (so they work from lock screen / while sleeping)
        val stopIntent = Intent(this, AlarmActionReceiver::class.java).apply { action = ACTION_STOP_ALARM }
        val snoozeIntent = Intent(this, AlarmActionReceiver::class.java).apply {
            action = ACTION_SNOOZE_ALARM
            putExtra(EXTRA_SNOOZE_MINUTES, DEFAULT_SNOOZE_MINUTES)
        }

        val stopPending = PendingIntent.getBroadcast(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val snoozePending = PendingIntent.getBroadcast(
            this,
            2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("WhatsAlarm is ringing")
            .setContentText("Keyword: ${lastKeyword ?: "Detected"}")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPending)
            .addAction(android.R.drawable.ic_media_pause, "Snooze", snoozePending)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun scheduleSnooze(minutes: Int) {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + minutes * 60_000L

        val playIntent = Intent(this, AlarmService::class.java).apply { action = ACTION_START_ALARM }
        val pending = PendingIntent.getService(
            this,
            3,
            playIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "WhatsAlarm Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarm notifications for WhatsAlarm"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }
}