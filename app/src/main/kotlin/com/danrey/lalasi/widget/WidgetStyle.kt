// WidgetStyle.kt

package com.danrey.lalasi.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader

/**
 * Look of the home-screen widgets: the "liquid" colour, how blurred the glass is and how strongly the colour
 * washes through it. Saved by [WidgetStyleActivity], read by the widget manager and by that screen's preview.
 */
object WidgetStyle {
    const val PREFS = "lalasi_widget_style"
    private const val KEY_COLOR = "liquid_color"
    private const val KEY_BLUR = "blur"
    private const val KEY_TINT = "tint"

    /** [liquidColor] 0 = automatic, taken from the album art. [blur] and [tint] are 0..100. */
    data class Config(val liquidColor: Int = 0, val blur: Int = 60, val tint: Int = 45)

    fun load(context: Context): Config {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Config(p.getInt(KEY_COLOR, 0), p.getInt(KEY_BLUR, 60), p.getInt(KEY_TINT, 45))
    }

    fun save(context: Context, config: Config) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_COLOR, config.liquidColor)
            .putInt(KEY_BLUR, config.blur.coerceIn(0, 100))
            .putInt(KEY_TINT, config.tint.coerceIn(0, 100))
            .apply()
    }

    private fun white(alpha: Int) = Color.argb(alpha.coerceIn(0, 255), 255, 255, 255)

    private fun withAlpha(color: Int, alpha: Int) =
        Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

    /** A small colourful picture so the preview has something to blur. */
    fun sampleArt(): Bitmap {
        val bmp = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(
            0f, 0f, 96f, 96f,
            intArrayOf(Color.rgb(255, 120, 90), Color.rgb(120, 90, 255), Color.rgb(40, 190, 255)),
            null, Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, 96f, 96f, paint)
        paint.shader = null
        paint.color = Color.argb(150, 255, 230, 120)
        canvas.drawCircle(30f, 64f, 22f, paint)
        return bmp
    }

    /**
     * The widget's glass: the album art centre-cropped and blurred, a liquid colour washing through it from two
     * corners, a diagonal sheen and a bright specular edge, all clipped to the rounded rectangle.
     * Sized in px by the caller; the widget stretches it to fit.
     */
    fun renderGlass(art: Bitmap?, w: Int, h: Int, cornerPx: Float, config: Config, strokePx: Float): Bitmap {
        val width = w.coerceAtLeast(16)
        val height = h.coerceAtLeast(16)
        val wf = width.toFloat()
        val hf = height.toFloat()
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.clipPath(Path().apply { addRoundRect(RectF(0f, 0f, wf, hf), cornerPx, cornerPx, Path.Direction.CW) })

        var liquid = config.liquidColor
        if (art != null) {
            val blurred = blurCover(art, width, height, config.blur)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(1.35f) })
            }
            canvas.drawBitmap(blurred, null, RectF(0f, 0f, wf, hf), paint)
            if (liquid == 0) liquid = averageColor(blurred)
            blurred.recycle()
        } else {
            canvas.drawColor(Color.rgb(27, 32, 48))
            if (liquid == 0) liquid = Color.rgb(108, 123, 255)
        }

        val strength = config.tint.coerceIn(0, 100) / 100f
        // a flat veil of the liquid colour, then two soft washes, the second shifted in hue
        canvas.drawColor(withAlpha(liquid, (strength * 0.45f * 255).toInt()))
        val hsv = FloatArray(3)
        Color.colorToHSV(liquid, hsv)
        val sat = hsv[1].coerceAtLeast(0.5f)
        val c1 = Color.HSVToColor(floatArrayOf(hsv[0], sat, 1f))
        val c2 = Color.HSVToColor(floatArrayOf((hsv[0] + 50f) % 360f, sat, 1f))
        val radius = maxOf(wf, hf) * 0.8f
        val wash = Paint(Paint.ANTI_ALIAS_FLAG)
        wash.shader = RadialGradient(
            wf * 0.12f, hf * 0.05f, radius,
            intArrayOf(withAlpha(c1, (strength * 150).toInt()), withAlpha(c1, 0)), null, Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, wf, hf, wash)
        wash.shader = RadialGradient(
            wf * 0.92f, hf * 1.0f, radius,
            intArrayOf(withAlpha(c2, (strength * 125).toInt()), withAlpha(c2, 0)), null, Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, wf, hf, wash)

        // sheen: bright from the top-left, clear in the middle, a little light back from the bottom-right
        val sheen = Paint(Paint.ANTI_ALIAS_FLAG)
        sheen.shader = LinearGradient(
            0f, 0f, wf, hf,
            intArrayOf(white(95), white(10), white(48)), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, wf, hf, sheen)

        // specular edge
        val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokePx
            shader = LinearGradient(
                0f, 0f, wf, hf,
                intArrayOf(white(240), white(45), white(125)), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP,
            )
        }
        val half = strokePx / 2f
        canvas.drawRoundRect(RectF(half, half, wf - half, hf - half), cornerPx - half, cornerPx - half, edge)
        val inner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokePx * 0.5f
            color = white(36)
        }
        val gap = strokePx * 1.6f
        canvas.drawRoundRect(RectF(gap, gap, wf - gap, hf - gap), cornerPx - gap, cornerPx - gap, inner)
        return out
    }

    /** Centre-crops [art] to the target's aspect, shrinks it a long way, then grows it back smoothly. */
    private fun blurCover(art: Bitmap, w: Int, h: Int, blur: Int): Bitmap {
        val targetAspect = w.toFloat() / h
        val srcAspect = art.width.toFloat() / art.height
        val src = if (srcAspect > targetAspect) {
            val nw = (art.height * targetAspect).toInt().coerceIn(1, art.width)
            Rect((art.width - nw) / 2, 0, (art.width + nw) / 2, art.height)
        } else {
            val nh = (art.width / targetAspect).toInt().coerceIn(1, art.height)
            Rect(0, (art.height - nh) / 2, art.width, (art.height + nh) / 2)
        }
        val factor = 4f + blur.coerceIn(0, 100) / 100f * 20f
        val tw = (w / factor).toInt().coerceAtLeast(6)
        val th = (h / factor).toInt().coerceAtLeast(6)

        var cur = Bitmap.createBitmap(art, src.left, src.top, src.width(), src.height())
        while (cur.width / 2 > tw * 2 && cur.height / 2 > th * 2) {
            val next = Bitmap.createScaledBitmap(cur, cur.width / 2, cur.height / 2, true)
            if (cur !== art) cur.recycle()
            cur = next
        }
        var small = Bitmap.createScaledBitmap(cur, tw, th, true)
        if (cur !== art && cur !== small) cur.recycle()
        // grow back in doubling steps: one big bilinear jump would show the blocks
        while (small.width * 2 < w && small.height * 2 < h) {
            val next = Bitmap.createScaledBitmap(small, small.width * 2, small.height * 2, true)
            small.recycle()
            small = next
        }
        val up = Bitmap.createScaledBitmap(small, w, h, true)
        if (up !== small) small.recycle()
        return up
    }

    private fun averageColor(bitmap: Bitmap): Int {
        val one = Bitmap.createScaledBitmap(bitmap, 1, 1, true)
        val px = one.getPixel(0, 0)
        if (one !== bitmap) one.recycle()
        return Color.rgb(Color.red(px), Color.green(px), Color.blue(px))
    }
}
