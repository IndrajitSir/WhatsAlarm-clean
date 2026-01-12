package com.example.whatsalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager

/**
 * Receives notification action intents (STOP / SNOOZE).
 * Acquires a short wake lock and forwards to AlarmService so actions work while device is sleeping/locked.
 */
class AlarmActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return

        // Acquire short wake lock to ensure service start completes
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WhatsAlarm:AlarmActionWakeLock")
        wl.setReferenceCounted(false)
        wl.acquire(30_000L) // 30s max

        try {
            val svcIntent = Intent(context, AlarmService::class.java).apply {
                this.action = action
                putExtras(intent) // forward any extras (e.g. snooze minutes)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(svcIntent)
            } else {
                context.startService(svcIntent)
            }
        } finally {
            // Release immediately; the wake lock has a timeout to guard against leaks.
            try { wl.release() } catch (_: Exception) {}
        }
    }
}