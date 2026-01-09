package com.example.whatsalarm

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.os.Build

class WhatsAppListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {

        // Only react to WhatsApp notifications
        val pkg = sbn.packageName ?: return
        if (!pkg.contains("com.whatsapp")) return

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)

        // Master toggle
        if (!prefs.getBoolean("enabled", true)) return
        if (prefs.getBoolean("alarm_running", false)) return
        // Pull *all* possible text from the notification
        val extras = sbn.notification.extras
        val texts = mutableListOf<String>()

        extras.getCharSequence("android.text")?.let { texts.add(it.toString().lowercase()) }
        extras.getCharSequence("android.bigText")?.let { texts.add(it.toString().lowercase()) }
        extras.getCharSequence("android.title")?.let { texts.add(it.toString().lowercase()) }

        if (texts.isEmpty()) return

        // Keywords
        val keywords = prefs.getString("keywords", "")!!
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }

        if (keywords.isEmpty()) return

        // Check for match and find the first keyword matched
        var matchedKeyword: String? = null
        loop@ for (text in texts) {
            for (keyword in keywords) {
                if (text.contains(keyword)) {
                    matchedKeyword = keyword
                    break@loop
                }
            }
        }

        // Trigger alarm service if a keyword matched
        matchedKeyword?.let {
            val intent = Intent(this, AlarmService::class.java)
            intent.action = "START_ALARM"
            intent.putExtra("keyword", it)  // pass matched keyword

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }
}
