package com.example.whatsalarm

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat

class WhatsAppListener : NotificationListenerService() {

    private val whatsappPackages = setOf("com.whatsapp", "com.whatsapp.w4b")

    override fun onListenerConnected() {
        super.onListenerConnected()
        // optional: log or notify that the listener connected
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        if (!whatsappPackages.contains(pkg)) return

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
                // join recent messages
                val sb = StringBuilder()
                for (m in style.messages) {
                    sb.append(m.text).append(' ')
                }
                sb.toString().trim()
            }
        messagingText?.let { if (it.isNotBlank()) textCandidates.add(it) }

        // Final body to check: join with spaces
        val body = textCandidates.joinToString(" ").trim()
        if (body.isBlank()) return

        // Keyword matching: simple contains; replace with regex if you want word boundaries etc.
        val keywords = listOf("alarm me", "ring me", "urgent", "wake me") // update to your keywords
        val match = keywords.any { keyword -> body.contains(keyword, ignoreCase = true) }

        if (match) {
            // Start the alarm player service (foreground). Include small context.
            val intent = Intent(this, AlarmPlayerService::class.java).apply {
                action = AlarmPlayerService.ACTION_PLAY_ALARM
                putExtra(AlarmPlayerService.EXTRA_NOTIF_TITLE, extras.getCharSequence(Notification.EXTRA_TITLE)?.toString())
                putExtra(AlarmPlayerService.EXTRA_NOTIF_TEXT, body)
                // Put other metadata if needed (package, time, etc.)
            }
            // Use startForegroundService on O+ to ensure it starts.
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