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

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Notification Permission card
        binding.cardNotification.apply {
            title.text = "Notification Permission"
            desc.text = "For instant alerts & ringtones"
            icon.setImageResource(R.drawable.ic_notification)
            btnGrant.setOnClickListener { view ->
                view.animateClick()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissions(
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        REQ_NOTIFICATION
                    )
                }
            }
        }

        // Popup / Overlay card
        binding.cardPopup.apply {
            title.text = "Overlay Permission"
            desc.text = "To show alarm popup on screen"
            icon.setImageResource(R.drawable.ic_popup)
            btnGrant.setOnClickListener { view ->
                view.animateClick()
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }

        // Notification Access card
        binding.cardNotifAccess.apply {
            title.text = "Notification Access"
            desc.text = "To detect incoming messages"
            icon.setImageResource(R.drawable.ic_access)
            btnGrant.setOnClickListener { view ->
                view.animateClick()
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
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
        refreshStates()
    }

    private fun refreshStates() {
        // 1. Notification Permission
        val notifPermissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

        updateCardState(binding.cardNotification, notifPermissionGranted)

        // 2. Overlay Permission
        val overlayGranted = isOverlayGranted(this)
        updateCardState(binding.cardPopup, overlayGranted)

        // 3. Notification Access
        val notifAccessGranted = isNotificationAccessGranted(this)
        updateCardState(binding.cardNotifAccess, notifAccessGranted)

        // Auto-navigate if all granted
        if (notifPermissionGranted && overlayGranted && notifAccessGranted) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun updateCardState(itemBinding: com.example.whatsalarm.databinding.ItemPermissionBinding, isGranted: Boolean) {
        itemBinding.statusText.text = if (isGranted) "Granted ✅" else "Required"
        itemBinding.statusText.setTextColor(
            ContextCompat.getColor(this, if (isGranted) R.color.accent else R.color.warning)
        )
        itemBinding.btnGrant.isEnabled = !isGranted
        itemBinding.btnGrant.alpha = if (isGranted) 0.5f else 1.0f
    }
}
