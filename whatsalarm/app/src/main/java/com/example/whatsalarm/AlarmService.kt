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
import androidx.core.app.NotificationCompat

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var lastKeyword: String? = null

    companion object {
        const val CHANNEL_ID = "whatsalarm_channel"
        const val NOTIF_ID = 1
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when (intent?.action) {
            "START_ALARM" -> {
                if (mediaPlayer != null) return START_STICKY
                lastKeyword = intent.getStringExtra("keyword")
                startForeground(NOTIF_ID, buildNotification())
                startAlarmSound()
            }

            "STOP_ALARM" -> {
                stopAlarm()
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
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        getSharedPreferences("settings", MODE_PRIVATE)
            .edit()
            .putBoolean("alarm_running", false)
            .apply()

        stopForeground(true)
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
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("WhatsAlarm is ringing")
            .setContentText("Keyword: ${lastKeyword ?: "Detected"}")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .build()
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
