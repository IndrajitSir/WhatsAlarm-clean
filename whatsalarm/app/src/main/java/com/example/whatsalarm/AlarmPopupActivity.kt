package com.example.whatsalarm

import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.TextView
import com.google.android.material.button.MaterialButton

class AlarmPopupActivity : Activity() {

    companion object {
        const val EXTRA_KEYWORD = "keyword"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            try {
                val km = getSystemService(KeyguardManager::class.java)
                km?.requestDismissKeyguard(this, null)
            } catch (_: Exception) { }
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        setContentView(R.layout.activity_alarm_popup)

        val keyword = intent.getStringExtra(EXTRA_KEYWORD) ?: "Keyword"
        findViewById<TextView>(R.id.tvKeyword).text = "Keyword detected: $keyword"

        findViewById<MaterialButton>(R.id.btnStopAlarm).setOnClickListener {
            val stopIntent = Intent(this, AlarmService::class.java).apply {
                action = AlarmService.ACTION_STOP_ALARM
            }
            stopService(stopIntent)
            finish()
        }

        findViewById<TextView>(R.id.tvKeyword).setOnClickListener {
            finish()
        }
    }
}