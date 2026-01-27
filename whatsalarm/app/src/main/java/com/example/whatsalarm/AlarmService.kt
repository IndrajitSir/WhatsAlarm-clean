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
import android.util.Log
import androidx.core.app.NotificationCompat
import android.app.Service

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var lastKeyword: String? = null

    companion object {
        private const val TAG = "AlarmService"
        const val CHANNEL_ID = "whatsalarm_channel"
        const val NOTIF_ID = 1

        const val ACTION_START_ALARM = "START_ALARM"
        const val ACTION_STOP_ALARM = "com.example.whatsalarm.ACTION_STOP"
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
        try {
            when (intent?.action) {
                ACTION_START_ALARM, "START_ALARM" -> {
                    if (mediaPlayer != null) return START_STICKY
                    lastKeyword = intent.getStringExtra(EXTRA_KEYWORD)
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
        } catch (t: Throwable) {
            Log.e(TAG, "Unhandled error in onStartCommand", t)
            getSharedPreferences("settings", MODE_PRIVATE)
                .edit()
                .putBoolean("alarm_running", false)
                .apply()
            try { stopForeground(true) } catch (_: Exception) {}
            stopSelf()
        }
        return START_STICKY
    }

    private fun startAlarmSound() {
        if (mediaPlayer != null) return

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val uri: Uri = prefs.getString("alarmTone", null)?.let {
            Uri.parse(it)
        } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        try {
            mediaPlayer = MediaPlayer().apply {
                setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "MediaPlayer error what=$what extra=$extra")
                    try { mp.stop() } catch (_: Exception) {}
                    try { mp.reset() } catch (_: Exception) {}
                    try { mp.release() } catch (_: Exception) {}
                    mediaPlayer = null
                    getSharedPreferences("settings", MODE_PRIVATE)
                        .edit()
                        .putBoolean("alarm_running", false)
                        .apply()
                    try { stopForeground(true) } catch (_: Exception) {}
                    stopSelf()
                    true
                }

                try {
                    setDataSource(applicationContext, uri)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = true
                    setOnPreparedListener { it.start() }
                    prepareAsync()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to prepare MediaPlayer for uri=$uri", e)
                    try { reset() } catch (_: Exception) {}
                    try { release() } catch (_: Exception) {}
                    mediaPlayer = null
                    getSharedPreferences("settings", MODE_PRIVATE)
                        .edit()
                        .putBoolean("alarm_running", false)
                        .apply()
                    try { stopForeground(true) } catch (_: Exception) {}
                    stopSelf()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start alarm sound", e)
            mediaPlayer = null
            getSharedPreferences("settings", MODE_PRIVATE)
                .edit()
                .putBoolean("alarm_running", false)
                .apply()
            try { stopForeground(true) } catch (_: Exception) {}
            stopSelf()
        }
    }

    private fun stopAlarm() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    try { it.stop() } catch (_: Exception) {}
                }
                try { it.reset() } catch (_: Exception) {}
                try { it.release() } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping media player", e)
        } finally {
            mediaPlayer = null
        }

        getSharedPreferences("settings", MODE_PRIVATE)
            .edit()
            .putBoolean("alarm_running", false)
            .apply()

        try { stopForeground(true) } catch (e: Exception) {
            Log.w(TAG, "stopForeground failed", e)
        }
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

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)

        // Stop and Snooze actions (broadcast intents)
        val stopIntent = Intent(this, AlarmActionReceiver::class.java).apply { action = ACTION_STOP_ALARM }
        val snoozeIntent = Intent(this, AlarmActionReceiver::class.java).apply {
            action = ACTION_SNOOZE_ALARM
            putExtra(EXTRA_SNOOZE_MINUTES, DEFAULT_SNOOZE_MINUTES)
        }

        val stopPending = PendingIntent.getBroadcast(this, 1, stopIntent, flags)
        val snoozePending = PendingIntent.getBroadcast(this, 2, snoozeIntent, flags)

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
            .setDefaults(Notification.DEFAULT_ALL)
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

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                am.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule snooze", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                "WhatsAlarm Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarm notifications for WhatsAlarm"
                setSound(alarmSound, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }


    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }
}