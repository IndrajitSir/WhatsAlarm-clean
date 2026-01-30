package com.example.whatsalarm

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import android.os.Build
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import com.example.whatsalarm.databinding.ActivityMainBinding
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import android.view.animation.AnimationUtils
import com.example.whatsalarm.R
import com.example.whatsalarm.ui.utils.animateClick
import com.example.whatsalarm.ui.utils.hideKeyboard
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val prefs by lazy { getSharedPreferences("settings", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    200
                )
            }
        }

            // FIRST LAUNCH CHECK
        val firstLaunch = prefs.getBoolean("first_launch", true)
        if (firstLaunch) {
            prefs.edit().putBoolean("first_launch", false).apply()
            startActivity(Intent(this, GrantPermissionsActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        val isDark = prefs.getBoolean("dark_mode", false)
        binding.switchTheme.isChecked = isDark

        AppCompatDelegate.setDefaultNightMode(
            if (isDark)
                AppCompatDelegate.MODE_NIGHT_YES
            else
                AppCompatDelegate.MODE_NIGHT_NO
        )

        binding.root.startAnimation(
            AnimationUtils.loadAnimation(this, R.anim.fade_in)
        )

        // ----- ENABLE SWITCH -----
        binding.switchEnable.performHapticFeedback(
            android.view.HapticFeedbackConstants.KEYBOARD_TAP
        )
        
        binding.switchEnable.isChecked = prefs.getBoolean("enabled", true)

        binding.switchEnable.setOnCheckedChangeListener { _, value ->
            prefs.edit().putBoolean("enabled", value).apply()
        }

        // ----- INITIAL UI VALUES -----
        binding.keywords.setText(prefs.getString("keywords", "good morning"))

        // ----- SAVE KEYWORDS -----
        binding.saveBtn.setOnClickListener {
            prefs.edit().putString("keywords", binding.keywords.text.toString()).apply()

            // hide keyboard
            it.hideKeyboard()

            Toast.makeText(this,"Keywords saved ✅", Toast.LENGTH_SHORT).show()
            binding.saveBtn.text = "Saved!"
            it.animateClick()
            binding.saveBtn.postDelayed({ binding.saveBtn.text = "Save" },1500)
        }

        // ----- OPEN NOTIFICATION ACCESS -----
        binding.btnOpenNotifAccess.setOnClickListener {
            it.animateClick()
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        // ----- CHOOSE RINGTONE -----
        binding.btnChooseRingtone.setOnClickListener {
            it.animateClick()
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Tone")
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                    prefs.getString("alarmTone", null)?.let { Uri.parse(it) })
            }
            startActivityForResult(intent, 101)
        }

        // ----- OPEN SETTINGS -----
        binding.btnSettings.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS))
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(this, Settings::class.java)
                startActivity(intent)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 101 && resultCode == RESULT_OK) {
            val uri: Uri? = data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            uri?.let {
                prefs.edit().putString("alarmTone", it.toString()).apply()
                Toast.makeText(this, "Ringtone updated ✅", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult( requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200 && (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
            // Toast.makeText( this, "Notification permission denied Popup won't work",Toast.LENGTH_LONG).show()
        }
    }
}
