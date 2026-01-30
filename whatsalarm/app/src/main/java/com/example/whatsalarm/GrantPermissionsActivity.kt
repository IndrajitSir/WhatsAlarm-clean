package com.example.whatsalarm

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import com.example.whatsalarm.databinding.ActivityPermissionBinding
import com.example.whatsalarm.ui.utils.animateClick
import android.content.pm.PackageManager

class GrantPermissionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNotificationPermission()
        setupPopupPermission()
        setupNotificationAccess()
    }

    private fun setupNotificationPermission() {

        binding.cardNotification.btnGrant.setOnClickListener {

            it.animateClick() 

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }


    private fun setupPopupPermission() {
        val btn = findViewById<Button>(R.id.cardPopup)
            .findViewById<Button>(R.id.btnGrant)

        btn.setOnClickListener {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun setupNotificationAccess() {
        val btn = findViewById<Button>(R.id.cardNotifAccess)
            .findViewById<Button>(R.id.btnGrant)

        btn.setOnClickListener {
            startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            )
        }
    }

    private fun isNotificationAccessGranted(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        return enabled?.contains(context.packageName) == true
    }

    private fun isOverlayGranted(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    private fun checkAndContinue() {
        if (isNotificationAccessGranted(this) &&
            Settings.canDrawOverlays(this)
        ) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
        checkAndContinue()
    }

    private fun updateUI() {
        val notifGranted = isNotificationAccessGranted(this)
        val overlayGranted = isOverlayGranted(this)

        // Card 1 – Notification permission (Android 13+)
        binding.cardNotification.statusText.text =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED
            ) "Granted ✅" else "Required"

        // Card 2 – Popup / Overlay
        binding.cardPopup.statusText.text =
            if (overlayGranted) "Granted ✅" else "Required"

        // Card 3 – Notification listener access
        binding.cardNotifAccess.statusText.text =
            if (notifGranted) "Granted ✅" else "Required"
    }
}
