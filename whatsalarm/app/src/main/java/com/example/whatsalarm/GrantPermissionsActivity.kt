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
import com.example.whatsalarm.ui.utils.ThemeHelper

class GrantPermissionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionBinding
    private val REQ_NOTIFICATION = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Notification Permission card
        binding.cardNotification.apply {
            btnGrant.setOnClickListener { view ->
                view.animateClick()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIFICATION)
                }
            }
        }

        // Popup / Overlay card
        binding.cardPopup.apply {
            btnGrant.setOnClickListener { view ->
                view.animateClick()
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
            }
        }

        // Notification Access card
        binding.cardNotifAccess.apply {
            btnGrant.setOnClickListener { view ->
                view.animateClick()
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }

        // Continue Button
        binding.btnContinue.setOnClickListener {
            it.animateClick()
            navigateToMain()
        }
    }

    private fun isNotificationAccessGranted(context: Context): Boolean {
        val packageName = context.packageName
        val listeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        if (listeners.isNullOrEmpty()) return false
        return listeners.split(":").any { it.startsWith("$packageName/") || it == packageName }
    }

    private fun isOverlayGranted(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    override fun onResume() {
        super.onResume()
        refreshStates()
    }

    private fun refreshStates() {
        val notifPermissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        updateCardState(binding.cardNotification, notifPermissionGranted, "Notification Permission", "For instant alerts & ringtones", R.drawable.ic_notification)

        val overlayGranted = isOverlayGranted(this)
        updateCardState(binding.cardPopup, overlayGranted, "Overlay Permission", "To show alarm popup on screen", R.drawable.ic_popup)

        val notifAccessGranted = isNotificationAccessGranted(this)
        updateCardState(binding.cardNotifAccess, notifAccessGranted, "Notification Access", "To detect incoming messages", R.drawable.ic_access)

        val allGranted = notifPermissionGranted && overlayGranted && notifAccessGranted
        binding.btnContinue.visibility = if (allGranted) android.view.View.VISIBLE else android.view.View.GONE
        
        if (allGranted) {
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        if (isFinishing) return
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun updateCardState(
        itemBinding: com.example.whatsalarm.databinding.ItemPermissionBinding, 
        isGranted: Boolean,
        titleStr: String,
        descStr: String,
        iconRes: Int
    ) {
        itemBinding.title.text = titleStr
        itemBinding.desc.text = descStr
        itemBinding.icon.setImageResource(iconRes)
        itemBinding.statusText.text = if (isGranted) "Granted ✅" else "Required"
        itemBinding.statusText.setTextColor(
            ContextCompat.getColor(this, if (isGranted) R.color.md_theme_light_primary else R.color.md_theme_light_error)
        )
        itemBinding.btnGrant.isEnabled = !isGranted
        itemBinding.btnGrant.alpha = if (isGranted) 0.5f else 1.0f
    }
}
