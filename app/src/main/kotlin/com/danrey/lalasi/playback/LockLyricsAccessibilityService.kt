// LockLyricsAccessibilityService.kt
//
// The accessibility service the Dynamic Island draws through when "display over other apps" is not granted (see IslandOverlayController).
// The lock screen lyrics are gone: this service draws nothing by itself, reads no screen content and no events (canRetrieveWindowContent is
// off and onAccessibilityEvent does nothing). It only lets the island add its own window. The class keeps its old name so the system keeps the
// switch the user already turned on.

package com.danrey.lalasi.playback

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class LockLyricsAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        LockLyricsBus.attachService(this)
        LockLyricsBus.overlayConnected = true
    }

    // the service reads nothing from the screen
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        shutDown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        shutDown()
        super.onDestroy()
    }

    private fun shutDown() {
        LockLyricsBus.attachService(null)
        LockLyricsBus.overlayConnected = false
    }
}
