package com.example.whatsalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager

class AlarmActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WhatsAlarm:AlarmActionWakeLock")
        wl.setReferenceCounted(false)
        wl.acquire(30_000L) // 30 seconds 

        try {
            val svcIntent = Intent(context, AlarmService::class.java).apply {
                this.action = action
                putExtras(intent) 
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(svcIntent)
            } else {
                context.startService(svcIntent)
            }
        } finally {
            try { wl.release() } catch (_: Exception) {}
        }
    }
}