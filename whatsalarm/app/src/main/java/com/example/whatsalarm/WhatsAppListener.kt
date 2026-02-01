package com.example.whatsalarm

import android.app.Notification
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat

class WhatsAppListener : NotificationListenerService() {

    private val whatsappPackages = setOf("com.whatsapp", "com.whatsapp.w4b")

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        if (!whatsappPackages.contains(pkg)) return

        val prefs = applicationContext.getSharedPreferences("settings", MODE_PRIVATE)
        if (!prefs.getBoolean("enabled", true)) return
        if (prefs.getBoolean("alarm_running", false)) return

        val notif = sbn.notification ?: return
        val extras = notif.extras

        val textCandidates = mutableListOf<String>()
        extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.let(textCandidates::add)
        extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.let(textCandidates::add)
        extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.let(textCandidates::add)
        extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()?.let(textCandidates::add)

        (extras.get(CharSequenceArrayExtraKey) as? Array<CharSequence>)?.forEach {
            it?.toString()?.let(textCandidates::add)
        }

        NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notif)?.let { style ->
            val sb = StringBuilder()
            for (m in style.messages) {
                sb.append(m.text).append(' ')
            }
            val messagingText = sb.toString().trim()
            if (messagingText.isNotBlank()) textCandidates.add(messagingText)
        }

        val body = textCandidates.joinToString(" ").trim()
        if (body.isBlank()) return

        val keywords = prefs.getString("keywords", "")!!
            .split(",", " ")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }

        if (keywords.isEmpty()) return

        val matchedKeyword = textCandidates.firstOrNull { text ->
            keywords.any { keyword -> text.contains(keyword, ignoreCase = true) }
        }

        matchedKeyword?.let { kw ->
            val intent = Intent(this, AlarmService::class.java).apply {
                action = AlarmService.ACTION_START_ALARM
                putExtra(AlarmService.EXTRA_KEYWORD, kw)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    companion object {
        private const val CharSequenceArrayExtraKey = "android.textLines"
    }
}
