package com.example.whatsalarm

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import android.os.Build

class AlarmPopupActivity : Activity() {

    companion object {
        const val EXTRA_KEYWORD = "keyword"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make the Activity appear as a dialog
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        setContentView(R.layout.activity_alarm_popup)

        val keyword = intent.getStringExtra(EXTRA_KEYWORD) ?: "Keyword"
        findViewById<TextView>(R.id.tvKeyword).text = "Keyword detected: $keyword"

        // Stop Alarm button
        findViewById<MaterialButton>(R.id.btnStopAlarm).setOnClickListener {
                val stopIntent = Intent(this, AlarmService::class.java).apply {
                action = "STOP_ALARM"
            }

            startService(stopIntent)
            finish()
        }

        // Optional: Close the dialog when keyword text is clicked
        findViewById<TextView>(R.id.tvKeyword).setOnClickListener {
            finish()
        }
    }
}
