package com.example.whatsalarm

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.whatsalarm.databinding.ActivityPermissionBinding
import com.example.whatsalarm.ui.utils.animateClick

class GrantPermissionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionBinding

    private val REQ_NOTIFICATION = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNotificationPermission()
        setupPopupPermission()
        setupNotificationAccess()
    }

    private fun setupNotificationPermission() {

        binding.cardNotification.btnGrant.setOnClickListener { view ->
            view.animateClick()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQ_NOTIFICATION
                )
            }
        }
    }

    private fun setupPopupPermission() {

        binding.cardPopup.btnGrant.setOnClickListener { view ->
            view.animateClick()

            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun setupNotificationAccess() {

        binding.cardNotifAccess.btnGrant.setOnClickListener { view ->
            view.animateClick()

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

    override fun onResume() {
        super.onResume()
        updateUI()
        checkAndContinue()
    }

    private fun updateUI() {

        // -------- Notification permission
        val notifPermissionGranted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED

        binding.cardNotification.statusText.text =
            if (notifPermissionGranted) "Granted ✅" else "Required"

        binding.cardNotification.btnGrant.isEnabled =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notifPermissionGranted


        // -------- Overlay permission
        val overlayGranted = isOverlayGranted(this)

        binding.cardPopup.statusText.text =
            if (overlayGranted) "Granted ✅" else "Required"

        binding.cardPopup.btnGrant.isEnabled = !overlayGranted


        // -------- Notification listener
        val notifAccessGranted = isNotificationAccessGranted(this)

        binding.cardNotifAccess.statusText.text =
            if (notifAccessGranted) "Granted ✅" else "Required"

        binding.cardNotifAccess.btnGrant.isEnabled = !notifAccessGranted
    }

    private fun checkAndContinue() {

        val notifPermissionGranted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED

        val overlayGranted = isOverlayGranted(this)
        val notifAccessGranted = isNotificationAccessGranted(this)

        if (notifPermissionGranted && overlayGranted && notifAccessGranted) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
