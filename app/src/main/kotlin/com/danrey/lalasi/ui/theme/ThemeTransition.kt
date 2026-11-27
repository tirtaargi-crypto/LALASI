// ThemeTransition.kt
//
// Changing the theme must show at once, with no restart and no flash. Right before the new theme is written, the
// current screen is copied (PixelCopy) into a plain ImageView laid over the window; the app recomposes under it in
// the new theme, then the copy fades away. The person sees one soft cross-fade instead of a blink.

package com.danrey.lalasi.ui.theme

import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView

object ThemeTransition {
    private const val FadeMs = 360L
    private const val SettleMs = 220L
    private const val MaxWaitMs = 900L

    /** Runs [change] (the write of the new theme) hidden behind a cross-fade from the current picture. */
    fun run(activity: Activity?, target: com.danrey.lalasi.constants.AppThemeStyle, change: () -> Unit) {
        val window = activity?.window
        val content = activity?.findViewById<ViewGroup>(android.R.id.content)
        if (window == null || content == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O || content.width <= 0 || content.height <= 0) {
            change(); return
        }
        val decor = window.decorView
        val bmp = try {
            Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
        } catch (_: Throwable) { change(); return }
        val main = Handler(Looper.getMainLooper())
        try {
            PixelCopy.request(window, bmp, { result ->
                if (result != PixelCopy.SUCCESS) { bmp.recycle(); change(); return@request }
                val cover = ImageView(activity).apply {
                    setImageBitmap(bmp)
                    scaleType = ImageView.ScaleType.FIT_XY
                    isClickable = false
                    isFocusable = false
                    elevation = 10_000f
                }
                content.addView(cover, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                change()
                // the preference is written asynchronously: wait until the palette really is the new one, give the
                // recomposition a moment to finish under the picture, and only then fade it away
                val t0 = android.os.SystemClock.uptimeMillis()
                fun fade() = cover.animate().alpha(0f).setStartDelay(SettleMs).setDuration(FadeMs)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .withEndAction { content.removeView(cover); bmp.recycle() }
                    .start()
                lateinit var poll: Runnable
                poll = Runnable {
                    if (LalasiPalette.style == target || android.os.SystemClock.uptimeMillis() - t0 > MaxWaitMs) fade()
                    else cover.postOnAnimation(poll)
                }
                cover.postOnAnimation(poll)
            }, main)
        } catch (_: Throwable) {
            bmp.recycle(); change()
        }
    }
}
