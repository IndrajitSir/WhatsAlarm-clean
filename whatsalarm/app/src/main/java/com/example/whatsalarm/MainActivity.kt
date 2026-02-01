package com.example.whatsalarm

import android.Manifest
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import com.example.whatsalarm.databinding.ActivityMainBinding
import androidx.appcompat.app.AppCompatDelegate
import android.view.animation.AnimationUtils
import com.example.whatsalarm.R
import com.example.whatsalarm.ui.utils.animateClick
import com.example.whatsalarm.ui.utils.hideKeyboard
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResult

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val prefs by lazy { getSharedPreferences("settings", MODE_PRIVATE) }

    private val ringtonePicker: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                uri?.let {
                    prefs.edit().putString("alarmTone", it.toString()).apply()
                    Toast.makeText(this, "Ringtone updated ✅", Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 200)
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
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        binding.root.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in))

        // Enable switch
        binding.switchEnable.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
        binding.switchEnable.isChecked = prefs.getBoolean("enabled", true)
        binding.switchEnable.setOnCheckedChangeListener { _, value ->
            prefs.edit().putBoolean("enabled", value).apply()
        }

        // Initial UI values
        binding.keywords.setText(prefs.getString("keywords", "good morning"))

        // Save keywords
        binding.saveBtn.setOnClickListener {
            prefs.edit().putString("keywords", binding.keywords.text.toString()).apply()
            it.hideKeyboard()
            Toast.makeText(this, "Keywords saved ✅", Toast.LENGTH_SHORT).show()
            binding.saveBtn.text = "Saved!"
            it.animateClick()
            binding.saveBtn.postDelayed({ binding.saveBtn.text = "Save" }, 1500)
        }

        // Open notification access
        binding.btnOpenNotifAccess.setOnClickListener {
            it.animateClick()
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        // Choose ringtone (Activity Result API)
        binding.btnChooseRingtone.setOnClickListener {
            it.animateClick()
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Tone")
                putExtra(
                    RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                    prefs.getString("alarmTone", null)?.let { Uri.parse(it) }
                )
            }
            ringtonePicker.launch(intent)
        }

        // Open app settings
        binding.btnSettings.setOnClickListener {
            it.animateClick()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200 && (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
            // Optionally show a Toast here
        }
    }
}
