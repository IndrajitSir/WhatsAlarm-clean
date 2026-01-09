package com.example.whatsalarm

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.whatsalarm.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val prefs by lazy { getSharedPreferences("settings", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.switchEnable.isChecked = prefs.getBoolean("enabled", true)
        binding.keywords.setText(prefs.getString("keywords", "good morning,meeting,urgent"))
        binding.btnQuietStart.text = prefs.getString("quietStart","22:00")
        binding.btnQuietEnd.text = prefs.getString("quietEnd","07:00")

        binding.switchEnable.setOnCheckedChangeListener { _, v ->
            prefs.edit().putBoolean("enabled", v).apply()
        }

        binding.saveBtn.setOnClickListener {
            prefs.edit().putString("keywords", binding.keywords.text.toString()).apply()
        }

        binding.btnQuietStart.setOnClickListener { pickTime("quietStart") }
        binding.btnQuietEnd.setOnClickListener { pickTime("quietEnd") }

        binding.btnOpenNotifAccess.setOnClickListener {
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }
    }

    private fun pickTime(key:String){
        val cal = Calendar.getInstance()
        TimePickerDialog(this,{_,h,m->
            val t = "%02d:%02d".format(h,m)
            prefs.edit().putString(key,t).apply()
            if(key=="quietStart") binding.btnQuietStart.text=t else binding.btnQuietEnd.text=t
        },cal.get(Calendar.HOUR_OF_DAY),cal.get(Calendar.MINUTE),true).show()
    }
}
