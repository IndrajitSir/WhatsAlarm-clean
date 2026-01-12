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
        const val ACTION_PLAY_ALARM = "com.example.whatsalarm.ACTION_PLAY_ALARM"
        const val ACTION_STOP_ALARM = "com.example.whatsalarm.ACTION_STOP_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.example.whatsalarm.ACTION_SNOOZE_ALARM"
        const val EXTRA_NOTIF_TITLE = "extra_notif_title"
        const val EXTRA_NOTIF_TEXT = "extra_notif_text"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
        const val DEFAULT_SNOOZE_MINUTES = 10
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun pendingFlags(): Int {
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        // Use immutable on modern platforms where appropriate
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags = flags or PendingIntent.FLAG_IMMUTABLE
        return flags
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_ALARM -> {
                // Start foreground and play
                val title = intent.getStringExtra(EXTRA_NOTIF_TITLE) ?: "WhatsApp alarm"
                val text = intent.getStringExtra(EXTRA_NOTIF_TEXT) ?: ""
                startForegroundWithNotification(title, text)
                playAlarm()
            }

            ACTION_STOP_ALARM -> {
                stopAlarm()
                stopSelf()
            }

            ACTION_SNOOZE_ALARM -> {
                // Stop playback now, schedule a restart for snooze duration
                stopAlarm()
                scheduleSnooze(intent.getIntExtra(EXTRA_SNOOZE_MINUTES, DEFAULT_SNOOZE_MINUTES))
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startAlarmSound() {
        if (mediaPlayer != null) return

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val uri: Uri = prefs.getString("alarmTone", null)?.let {
            Uri.parse(it)
        } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        mediaPlayer = MediaPlayer().apply {
            setDataSource(this@AlarmPlayerService, alarmUri)
            setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            )
            isLooping = true
            setOnPreparedListener { it.start() }
            prepareAsync()
        }
    }

    private fun stopAlarm() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        mediaPlayer?.let {
            if (it.isPlaying) {
                try { it.stop() } catch (ignored: Exception) {}
            }
            try { it.reset() } catch (ignored: Exception) {}
            try { it.release() } catch (ignored: Exception) {}
        }
        mediaPlayer = null
        try { stopForeground(true) } catch (ignored: Exception) {}
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

    private fun startForegroundWithNotification(title: String, text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Alarm Player", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Plays alarm sound when a keyword is matched"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
            }
            nm.createNotificationChannel(channel)
        }

        // Create notification actions that point to the BroadcastReceiver so they work while sleeping.
        val stopIntent = Intent(this, AlarmActionReceiver::class.java).apply { action = ACTION_STOP_ALARM }
        val stopPending = PendingIntent.getBroadcast(this, 0, stopIntent, pendingFlags())

        val snoozeIntent = Intent(this, AlarmActionReceiver::class.java).apply {
            action = ACTION_SNOOZE_ALARM
            putExtra(EXTRA_SNOOZE_MINUTES, DEFAULT_SNOOZE_MINUTES)
        }
        val snoozePending = PendingIntent.getBroadcast(this, 1, snoozeIntent, pendingFlags())

        val notif = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPending)
            .addAction(android.R.drawable.ic_media_pause, "Snooze", snoozePending)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        startForeground(NOTIFICATION_ID, notif)
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

    private fun scheduleSnooze(minutes: Int) {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + minutes * 60_000L

        val playIntent = Intent(this, AlarmPlayerService::class.java).apply { action = ACTION_PLAY_ALARM }
        val pending = PendingIntent.getService(this, 2, playIntent, pendingFlags())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }
}
