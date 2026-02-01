package com.example.whatsalarm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.example.whatsalarm.databinding.ActivityMainBinding
import com.example.whatsalarm.ui.utils.ThemeHelper
import com.example.whatsalarm.ui.utils.animateClick
import com.example.whatsalarm.ui.utils.hideKeyboard
import com.google.android.material.chip.Chip
import androidx.core.content.ContextCompat;

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
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)

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

        setupThemeToggle()
        setupAlertsToggle()
        setupKeywordInput()
        setupActionButtons()
        
        // Initial data restoration
        loadStoredData()
    }

    private fun setupThemeToggle() {
        binding.switchTheme.isChecked = ThemeHelper.isDarkMode(this)
        binding.switchTheme.setOnCheckedChangeListener { _, isChecked ->
            ThemeHelper.toggleDarkMode(this, isChecked)
            // Recreate to apply theme changes immediately
            recreate()
        }
    }

    private fun setupAlertsToggle() {
        binding.switchEnable.isChecked = prefs.getBoolean("enabled", true)
        binding.switchEnable.setOnCheckedChangeListener { _, value ->
            prefs.edit().putBoolean("enabled", value).apply()
        }
    }

    private fun setupKeywordInput() {
        binding.keywordInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addKeywordFromInput()
                true
            } else false
        }

        binding.keywordInput.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_SPACE || keyCode == KeyEvent.KEYCODE_COMMA)) {
                addKeywordFromInput()
                true
            } else false
        }

        binding.keywordInput.doAfterTextChanged { text ->
            if (text?.endsWith(",") == true || text?.endsWith(" ") == true) {
                addKeywordFromInput()
            }
        }
    }

    private fun addKeywordFromInput() {
        val raw = binding.keywordInput.text.toString().trim().replace(",", "")
        if (raw.isNotEmpty()) {
            addChip(raw)
            binding.keywordInput.setText("")
            updateKeywordCount()
        }
    }

    private fun addChip(text: String) {
        // Avoid duplicates
        if (getKeywords().contains(text)) return

        val chip = Chip(this).apply {
            this.text = text
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                binding.keywordChipGroup.removeView(this)
                updateKeywordCount()
            }
            // Material 3 Styling
            setChipBackgroundColorResource(R.color.md_theme_light_surfaceVariant)
            setTextColor(ContextCompat.getColor(context, R.color.md_theme_light_onSurfaceVariant))
            setCloseIconTintResource(R.color.md_theme_light_onSurfaceVariant)
        }
        binding.keywordChipGroup.addView(chip)
    }

    private fun updateKeywordCount() {
        binding.tvActiveKeywordCount.text = "${binding.keywordChipGroup.childCount} active"
    }

    private fun setupActionButtons() {
        binding.btnOpenNotifAccess.setOnClickListener {
            it.animateClick()
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        binding.btnChooseRingtone.setOnClickListener {
            it.animateClick()
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Tone")
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, prefs.getString("alarmTone", null)?.let { Uri.parse(it) })
            }
            ringtonePicker.launch(intent)
        }

        binding.btnSettings.setOnClickListener {
            it.animateClick()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        }

        binding.saveBtn.setOnClickListener {
            val keywords = getKeywords()
            prefs.edit().putString("keywords", keywords.joinToString(",")).apply()
            it.hideKeyboard()
            Toast.makeText(this, "Configuration saved ✅", Toast.LENGTH_SHORT).show()
            binding.saveBtn.text = "Saved!"
            it.animateClick()
            binding.saveBtn.postDelayed({ binding.saveBtn.text = "Save Configuration" }, 1500)
        }
    }

    private fun loadStoredData() {
        val storedKeywords = prefs.getString("keywords", "urgent, help") ?: ""
        if (storedKeywords.isNotEmpty()) {
            setKeywords(storedKeywords.split(",").map { it.trim() })
        }
        updateKeywordCount()
    }

    // Exposed API for keyword management
    fun getKeywords(): List<String> {
        val keywords = mutableListOf<String>()
        for (i in 0 until binding.keywordChipGroup.childCount) {
            val chip = binding.keywordChipGroup.getChildAt(i) as? Chip
            chip?.let { keywords.add(it.text.toString()) }
        }
        return keywords
    }

    fun setKeywords(list: List<String>) {
        binding.keywordChipGroup.removeAllViews()
        list.forEach { addChip(it) }
        updateKeywordCount()
    }
}
