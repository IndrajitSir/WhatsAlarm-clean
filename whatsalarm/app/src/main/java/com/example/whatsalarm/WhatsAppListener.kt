package com.example.whatsalarm

import android.app.Notification
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat

/**
 * Notification listener that watches WhatsApp notifications and starts AlarmService
 * when a configured keyword is matched.
 *
 * This version uses AlarmService.ACTION_START_ALARM (not AlarmPlayerService) and honors
 * the "enabled" and "alarm_running" preferences.
 */
class WhatsAppListener : NotificationListenerService() {

    private val whatsappPackages = setOf("com.whatsapp", "com.whatsapp.w4b")

    override fun onListenerConnected() {
        super.onListenerConnected()
        // optional: log or notify that the listener connected
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        if (!whatsappPackages.contains(pkg)) return

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)

        // Master toggle + running guard
        if (!prefs.getBoolean("enabled", true)) return
        if (prefs.getBoolean("alarm_running", false)) return

        val notif = sbn.notification ?: return
        val extras = notif.extras

        // Extract candidate texts (several fallbacks)
        val textCandidates = mutableListOf<String>()
        extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.let { textCandidates.add(it) }
        extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.let { textCandidates.add(it) }
        extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.let { textCandidates.add(it) }
        extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()?.let { textCandidates.add(it) }

        // EXTRA_TEXT_LINES (grouped notifications)
        (extras.get(CharSequenceArrayExtraKey)?.let { it as? Array<CharSequence> })?.forEach {
            it?.toString()?.let(textCandidates::add)
        }

        // Try MessagingStyle (if present)
        val messagingText = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notif)
            ?.let { style ->
                val sb = StringBuilder()
                for (m in style.messages) {
                    sb.append(m.text).append(' ')
                }
                sb.toString().trim()
            }
        messagingText?.let { if (it.isNotBlank()) textCandidates.add(it) }

        val body = textCandidates.joinToString(" ").trim()
        if (body.isBlank()) return

        // Load keywords from prefs (comma-separated)
        val keywords = prefs.getString("keywords", "")!!
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }

        if (keywords.isEmpty()) return

        // Check for match and find the first keyword matched
        var matchedKeyword: String? = null
        loop@ for (text in textCandidates) {
            for (keyword in keywords) {
                if (text.contains(keyword, ignoreCase = true)) {
                    matchedKeyword = keyword
                    break@loop
                }
            }
        }

        // Trigger AlarmService if a keyword matched
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

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // optional: handle removal
    }

    companion object {
        // Helper key for potential EXTRA_TEXT_LINES — cannot directly refer to the hidden constant here,
        // we try to fetch it by string so it won't crash on older SDKs.
        private val CharSequenceArrayExtraKey = "android.textLines"
    }
}