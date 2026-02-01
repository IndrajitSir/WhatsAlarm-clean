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

        // Continue Button
        binding.btnContinue.setOnClickListener {
            it.animateClick()
            navigateToMain()
        }
    }

    private fun isNotificationAccessGranted(context: Context): Boolean {
        val packageName = context.packageName
        val listeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        if (listeners.isNullOrEmpty()) return false
        
        return listeners.split(":").any { 
            it.startsWith("$packageName/") || it == packageName 
        }
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

        updateCardState(binding.cardNotification, notifPermissionGranted, "Notification Permission", "For instant alerts & ringtones", R.drawable.ic_notification)

        // 2. Overlay Permission
        val overlayGranted = isOverlayGranted(this)
        updateCardState(binding.cardPopup, overlayGranted, "Overlay Permission", "To show alarm popup on screen", R.drawable.ic_popup)

        // 3. Notification Access
        val notifAccessGranted = isNotificationAccessGranted(this)
        updateCardState(binding.cardNotifAccess, notifAccessGranted, "Notification Access", "To detect incoming messages", R.drawable.ic_access)

        // Show/Hide Continue button & Auto-navigate
        val allGranted = notifPermissionGranted && overlayGranted && notifAccessGranted
        
        // Best Practice Tip: We show the button if all are granted as a fallback,
        // but auto-navigation is usually preferred for a "wow" experience.
        binding.btnContinue.visibility = if (allGranted) android.view.View.VISIBLE else android.view.View.GONE
        
        if (allGranted) {
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        // Prevent multiple simultaneous navigations
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
            ContextCompat.getColor(this, if (isGranted) R.color.accent else R.color.warning)
        )
        itemBinding.btnGrant.isEnabled = !isGranted
        itemBinding.btnGrant.alpha = if (isGranted) 0.5f else 1.0f
    }
}

/**
 * BEST PRACTICES FOR <include> + <merge> WITH VIEWBINDING:
 * 
 * 1. Avoid <merge> as the root of included layouts IF you want to use the generated 
 *    Binding class (e.g., ItemPermissionBinding) directly from the parent binding.
 * 
 * 2. If you MUST use <merge>:
 *    - The parent <include> tag MUST specify android:layout_width and android:layout_height
 *      for ConstraintLayout to respect its constraints.
 *    - You have to manually bind the views: 
 *      val itemBinding = ItemPermissionBinding.bind(binding.cardNotification.root)
 * 
 * 3. Recommendation: Use a root container (e.g., LinearLayout or ConstraintLayout) 
 *    in your item_permission.xml. This makes ViewBinding child references work out-of-the-box.
 * 
 * 4. ConstraintLayout Rule: When using <include>, you MUST set width and height on the
 *    <include> tag itself, or its constraints will be ignored.
 */
