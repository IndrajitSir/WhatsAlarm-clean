package com.example.whatsalarm

import android.app.Notification
import android.media.MediaPlayer
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.time.LocalTime

class WhatsAppListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        if (!prefs.getBoolean("enabled", true)) return

        if (sbn.packageName != "com.whatsapp") return

        val text = sbn.notification.extras.getString(Notification.EXTRA_TEXT)?.lowercase() ?: ""
        val group = sbn.notification.extras.getString(Notification.EXTRA_TITLE) ?: "default"

        val keywords = (prefs.getString("keywords", "") ?: "")
            .lowercase().split(",").map { it.trim() }.filter { it.isNotEmpty() }

        if (keywords.none { text.contains(it) }) return

        if (isQuietHours(
                prefs.getString("quietStart","22:00")!!,
                prefs.getString("quietEnd","07:00")!!
            )) return

        playSoundForGroup(group.lowercase())
    }

    private fun isQuietHours(start:String, end:String): Boolean {
        return try {
            val now = LocalTime.now()
            val s = LocalTime.parse(start)
            val e = LocalTime.parse(end)
            if (s <= e) now.isAfter(s) && now.isBefore(e) else now.isAfter(s) || now.isBefore(e)
        } catch (e:Exception){ false }
    }

    private fun playSoundForGroup(group:String){
        val resId = when {
            group.contains("family") -> R.raw.alarm_family
            group.contains("office") -> R.raw.alarm_office
            else -> R.raw.alarm_default
        }
        val mp = MediaPlayer.create(this,resId)
        mp.start()
    }
}
