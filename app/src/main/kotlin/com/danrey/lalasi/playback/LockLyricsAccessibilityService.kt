// LockLyricsAccessibilityService.kt
//
// Draws the lyric line of the song that is playing over the lock screen. It uses an accessibility overlay window, which needs the user to
// switch "LALASI Lock Screen Lyrics" on in the system accessibility settings and nothing else (no "display over other apps").
//
// The service reads no screen content and no events: canRetrieveWindowContent is off and onAccessibilityEvent does nothing. It only draws
// what LockLyricsBus hands it, and removes the window again as soon as the bus is empty (unlocked, screen off, paused, no timed lyrics).

package com.danrey.lalasi.playback

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.danrey.lalasi.constants.LockLyricsNextLineKey
import com.danrey.lalasi.constants.LockLyricsPositionKey
import com.danrey.lalasi.constants.LockLyricsSizeKey
import com.danrey.lalasi.constants.LockLyricsTitleKey
import com.danrey.lalasi.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class LockLyricsAccessibilityService : AccessibilityService() {
    private data class Look(val position: Int, val next: Boolean, val title: Boolean, val size: Int)

    private var scope: CoroutineScope? = null
    private var windowManager: WindowManager? = null
    private var host: FrameLayout? = null
    private var lineView: TextView? = null
    private var nextView: TextView? = null
    private var titleView: TextView? = null
    private var attachedLook: Look? = null
    private var shownLine: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        LockLyricsBus.attachService(this)
        LockLyricsBus.overlayConnected = true
        windowManager = getSystemService(WINDOW_SERVICE) as? WindowManager
        val job = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = job
        val looks = applicationContext.dataStore.data.map {
            Look(
                position = (it[LockLyricsPositionKey] ?: 0).coerceIn(0, 2),
                next = it[LockLyricsNextLineKey] ?: true,
                title = it[LockLyricsTitleKey] ?: true,
                size = (it[LockLyricsSizeKey] ?: 1).coerceIn(0, 2),
            )
        }.distinctUntilChanged()
        job.launch {
            combine(LockLyricsBus.current, looks) { line, look -> line to look }.collect { (line, look) ->
                if (line == null || line.line.isBlank()) remove() else show(line, look)
            }
        }
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
        remove()
        scope?.cancel()
        scope = null
    }

    private fun dp(value: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics).toInt()

    private fun show(line: LockLyricsBus.Line, look: Look) {
        val manager = windowManager ?: return
        if (host == null || attachedLook != look) {
            remove()
            build(look)
            val height = resources.displayMetrics.heightPixels
            val top = when (look.position) {
                0 -> (height * 0.24f).toInt()
                1 -> (height * 0.46f).toInt()
                else -> (height * 0.68f).toInt()
            }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = top
            }
            val view = host ?: return
            view.alpha = 0f
            runCatching { manager.addView(view, params) }.onFailure { host = null; return }
            attachedLook = look
            view.animate().alpha(1f).setDuration(220).start()
            shownLine = null
        }
        bind(line, look)
    }

    private fun build(look: Look) {
        val lineSp = when (look.size) { 0 -> 18f; 2 -> 28f; else -> 22f }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(22f), dp(18f), dp(22f), dp(18f))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(26f).toFloat()
                setColor(Color.argb(112, 12, 12, 16))
                setStroke(dp(1f), Color.argb(46, 255, 255, 255))
            }
        }
        val title = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(Color.argb(150, 255, 255, 255))
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            letterSpacing = 0.04f
            visibility = if (look.title) View.VISIBLE else View.GONE
        }
        val current = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, lineSp)
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            maxLines = 3
            ellipsize = TextUtils.TruncateAt.END
            setLineSpacing(0f, 1.08f)
            setShadowLayer(dp(6f).toFloat(), 0f, dp(1f).toFloat(), Color.argb(120, 0, 0, 0))
        }
        val next = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, lineSp * 0.68f)
            setTextColor(Color.argb(140, 255, 255, 255))
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            gravity = Gravity.CENTER
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            visibility = if (look.next) View.VISIBLE else View.GONE
        }
        card.addView(title, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(8f) })
        card.addView(current, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        card.addView(next, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8f) })
        host = FrameLayout(this).apply {
            setPadding(dp(20f), 0, dp(20f), 0)
            addView(card, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        }
        lineView = current
        nextView = next
        titleView = title
    }

    private fun bind(line: LockLyricsBus.Line, look: Look) {
        val current = lineView ?: return
        titleView?.text = listOf(line.title, line.artist).filter { it.isNotBlank() }.joinToString(" • ")
        nextView?.text = line.next
        nextView?.visibility = if (look.next && line.next.isNotBlank()) View.VISIBLE else View.GONE
        if (shownLine == line.line) return
        val first = shownLine == null
        shownLine = line.line
        if (first) {
            current.text = line.line
            return
        }
        // the old line fades out, the new one fades in
        current.animate().cancel()
        current.animate().alpha(0f).setDuration(110).withEndAction {
            current.text = line.line
            current.animate().alpha(1f).setDuration(160).start()
        }.start()
    }

    private fun remove() {
        val view = host ?: return
        host = null
        lineView = null
        nextView = null
        titleView = null
        attachedLook = null
        shownLine = null
        runCatching { view.animate().cancel() }
        runCatching { windowManager?.removeViewImmediate(view) }
    }
}
