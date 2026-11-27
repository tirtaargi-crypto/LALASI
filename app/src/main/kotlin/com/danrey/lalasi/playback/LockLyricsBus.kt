// LockLyricsBus.kt
//
// Where the accessibility service is registered, so the Dynamic Island can draw through it (lock screen lyrics were removed; the name stays
// so nothing that points at it breaks). Nothing here is stored or sent.

package com.danrey.lalasi.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.lang.ref.WeakReference

object LockLyricsBus {
    private val connectedFlow = MutableStateFlow(false)
    private var serviceRef: WeakReference<android.accessibilityservice.AccessibilityService>? = null

    /** Follows the accessibility service: true while it is connected and able to draw an overlay (the Dynamic Island). */
    val connected: StateFlow<Boolean> get() = connectedFlow

    /** True while the accessibility service is connected and able to draw the overlay. */
    var overlayConnected: Boolean
        get() = connectedFlow.value
        set(value) { connectedFlow.value = value }

    /** The connected service; the Dynamic Island draws through it when "display over other apps" is not granted. */
    fun attachService(service: android.accessibilityservice.AccessibilityService?) {
        serviceRef = service?.let { WeakReference(it) }
    }

    /** The connected accessibility service, or null. */
    fun service(): android.accessibilityservice.AccessibilityService? = serviceRef?.get()

    /** The window manager and context of the connected service, which is what a TYPE_ACCESSIBILITY_OVERLAY window must be added with. */
    fun serviceWindow(): Pair<Context, WindowManager>? {
        val svc = service() ?: return null
        val wm = runCatching { svc.getSystemService(Context.WINDOW_SERVICE) as? WindowManager }.getOrNull() ?: return null
        return svc to wm
    }

    /** Whether the user switched the accessibility service on in the system settings. */
    fun isAccessGranted(context: Context): Boolean {
        val enabled = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        }.getOrNull().orEmpty()
        if (enabled.isBlank()) return false
        val component = ComponentName(context, LockLyricsAccessibilityService::class.java)
        return enabled.split(':').any {
            it.equals(component.flattenToString(), ignoreCase = true) ||
                it.equals(component.flattenToShortString(), ignoreCase = true)
        }
    }

    fun accessSettingsIntent(): Intent =
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
