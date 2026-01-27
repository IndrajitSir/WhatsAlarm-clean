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

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ----- INITIAL UI VALUES -----
        binding.switchEnable.isChecked = prefs.getBoolean("enabled", true)
        binding.keywords.setText(prefs.getString("keywords", "good morning"))

        // ----- TOGGLE ON/OFF -----
        binding.switchEnable.setOnCheckedChangeListener { _, value ->
            prefs.edit().putBoolean("enabled", value).apply()
        }

        // ----- SAVE KEYWORDS -----
        binding.saveBtn.setOnClickListener {
            prefs.edit().putString("keywords", binding.keywords.text.toString()).apply()
            Toast.makeText(this,"Keywords saved ✅", Toast.LENGTH_SHORT).show()
            binding.saveBtn.text = "Saved!"
            binding.saveBtn.postDelayed({ binding.saveBtn.text = "Save" },1500)
        }

        // ----- OPEN NOTIFICATION ACCESS -----
        binding.btnOpenNotifAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        // ----- CHOOSE RINGTONE -----
        binding.btnChooseRingtone.setOnClickListener {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Tone")
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                    prefs.getString("alarmTone", null)?.let { Uri.parse(it) })
            }
            startActivityForResult(intent, 101)
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
            Toast.makeText( this, "Notification permission denied Popup won't work",Toast.LENGTH_LONG).show()
        }
    }
}
