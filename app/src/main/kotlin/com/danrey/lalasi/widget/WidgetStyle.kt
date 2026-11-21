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
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
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
    data class Config(val liquidColor: Int = 0, val blur: Int = 52, val tint: Int = 32)

    fun load(context: Context): Config {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Config(p.getInt(KEY_COLOR, 0), p.getInt(KEY_BLUR, 52), p.getInt(KEY_TINT, 32))
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
    fun renderGlass(art: Bitmap?, w: Int, h: Int, cornerPx: Float, config: Config, strokePx: Float, scrim: Int = 0): Bitmap {
        val width = w.coerceAtLeast(16)
        val height = h.coerceAtLeast(16)
        val wf = width.toFloat()
        val hf = height.toFloat()
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val radius = cornerPx.coerceAtMost(minOf(wf, hf) * 0.22f)
        canvas.clipPath(Path().apply {
            addRoundRect(RectF(0f, 0f, wf, hf), radius, radius, Path.Direction.CW)
        })

        var liquid = config.liquidColor
        if (art != null) {
            // The artwork supplies colour and depth; the glass treatment above it stays optically clear.
            val blurred = blurCover(art, width, height, config.blur)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(1.12f) })
            }
            canvas.drawBitmap(blurred, null, RectF(0f, 0f, wf, hf), paint)
            if (liquid == 0) liquid = vibrantColor(art)
            blurred.recycle()
        } else {
            canvas.drawColor(Color.rgb(24, 28, 40))
            if (liquid == 0) liquid = Color.rgb(108, 123, 255)
        }

        // Legibility scrim for widgets that put text straight on the sharp cover (the Artwork widget has its own
        // bottom scrim layer, so it passes 0): an even dim plus a stronger lower half, same hue as the cover's shadows.
        if (scrim > 0) {
            canvas.drawRect(0f, 0f, wf, hf, Paint().apply { color = Color.argb((scrim * 0.55f).toInt().coerceIn(0, 255), 0, 0, 0) })
            canvas.drawRect(0f, 0f, wf, hf, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, 0f, hf,
                    intArrayOf(Color.argb(0, 0, 0, 0), Color.argb((scrim * 0.5f).toInt().coerceIn(0, 255), 0, 0, 0), Color.argb(scrim.coerceIn(0, 255), 0, 0, 0)),
                    floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP,
                )
            })
        }

        val tintStrength = config.tint.coerceIn(0, 100) / 100f
        val hsv = FloatArray(3)
        Color.colorToHSV(liquid, hsv)
        val vivid = Color.HSVToColor(floatArrayOf(
            hsv[0], hsv[1].coerceIn(0.35f, 0.9f), 1f
        ))

        // No grey veil over the cover: the glass keeps the artwork's own colours, the way the Artwork widget
        // shows them, so every widget reads as the same colour for the same song.

        // A little album-art colour transmission keeps each widget tied to its current track.
        if (tintStrength > 0f) {
            val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = withAlpha(vivid, (tintStrength * 36f).toInt())
            }
            canvas.drawRect(0f, 0f, wf, hf, tintPaint)
        }

        // Natural depth at the lower edge, kept subtle so details and artwork colours remain visible.
        val depth = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, hf,
                intArrayOf(Color.argb(0, 0, 0, 0), Color.argb(18, 0, 0, 0), Color.argb(58, 0, 0, 0)),
                floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, wf, hf, depth)

        // Cinematic artwork darkening shared by every widget: a gentle overall dim (artwork stays clear
        // behind the glass) plus a soft vignette. Together with the gradient above it keeps text readable.
        canvas.drawRect(0f, 0f, wf, hf, Paint().apply { color = Color.argb(40, 0, 0, 0) })
        val vignette = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                wf / 2f, hf / 2f, maxOf(wf, hf) * 0.78f,
                intArrayOf(Color.argb(0, 0, 0, 0), Color.argb(0, 0, 0, 0), Color.argb(52, 0, 0, 0)),
                floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, wf, hf, vignette)

        // Liquid-glass refraction: the cover is bent along the rim like a thick lens, with the colours split apart.
        refractBezel(out, radius)

        // Liquid colour reflections: localised and luminous, not a full-surface colour wash.
        val screen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        }
        val glowRadius = maxOf(wf, hf) * 0.62f
        fun liquidReflection(cx: Float, cy: Float, r: Float, color: Int, alpha: Int) {
            screen.shader = RadialGradient(
                cx, cy, r,
                intArrayOf(withAlpha(color, alpha), withAlpha(color, 0)),
                null, Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, wf, hf, screen)
        }
        liquidReflection(wf * 0.08f, hf * 0.02f, glowRadius, vivid, (tintStrength * 72f).toInt())
        val secondary = Color.HSVToColor(floatArrayOf((hsv[0] + 38f) % 360f, 0.48f, 1f))
        liquidReflection(wf * 0.96f, hf * 0.98f, glowRadius * 0.72f, secondary, (tintStrength * 58f).toInt())
        screen.shader = null
        screen.xfermode = null

        // Clear-glass sheen: a narrow reflection band, with a transparent centre.
        val sheen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, wf * 0.9f, hf,
                intArrayOf(white(76), white(18), white(2), white(30)),
                floatArrayOf(0f, 0.22f, 0.62f, 1f), Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, wf, hf, sheen)

        // A curved, liquid-looking highlight near the upper surface.
        val topGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, wf, 0f,
                intArrayOf(white(0), white(142), white(62), white(0)),
                floatArrayOf(0f, 0.24f, 0.72f, 1f), Shader.TileMode.CLAMP
            )
            style = Paint.Style.STROKE
            strokeWidth = maxOf(1f, strokePx * 1.8f)
        }
        val inset = maxOf(1f, strokePx)
        canvas.drawRoundRect(
            RectF(inset, inset, wf - inset, hf - inset),
            radius - inset, radius - inset, topGlow
        )

        // Fine polished perimeter and a delicate inner contour, like a transparent glass cover.
        val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = maxOf(1f, strokePx)
            shader = LinearGradient(
                0f, 0f, wf, hf,
                intArrayOf(white(238), white(34), white(30), white(150)),
                floatArrayOf(0f, 0.30f, 0.70f, 1f), Shader.TileMode.CLAMP
            )
        }
        val half = edge.strokeWidth / 2f
        canvas.drawRoundRect(
            RectF(half, half, wf - half, hf - half),
            (radius - half).coerceAtLeast(0f), (radius - half).coerceAtLeast(0f), edge
        )

        val inner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = maxOf(0.7f, strokePx * 0.45f)
            color = white(42)
        }
        val gap = strokePx * 1.7f
        if (wf > gap * 2 && hf > gap * 2) {
            canvas.drawRoundRect(
                RectF(gap, gap, wf - gap, hf - gap),
                (radius - gap).coerceAtLeast(0f), (radius - gap).coerceAtLeast(0f), inner
            )
        }

        // iOS 26 glass is clear and slightly lifted: a thin even veil of light over the whole pane
        canvas.drawRect(0f, 0f, wf, hf, Paint().apply { color = white(14) })
        // a soft bloom under the bright edge, so the specular line looks lit from inside the glass
        val bloom = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = maxOf(2f, strokePx * 3.4f)
            maskFilter = android.graphics.BlurMaskFilter(maxOf(2f, strokePx * 2.4f), android.graphics.BlurMaskFilter.Blur.NORMAL)
            shader = LinearGradient(
                0f, 0f, wf, hf,
                intArrayOf(white(120), white(0), white(0), white(70)),
                floatArrayOf(0f, 0.3f, 0.7f, 1f), Shader.TileMode.CLAMP
            )
        }
        val bi = strokePx * 1.6f
        canvas.drawRoundRect(RectF(bi, bi, wf - bi, hf - bi), (radius - bi).coerceAtLeast(0f), (radius - bi).coerceAtLeast(0f), bloom)

        // Chromatic fringe on the rim, as light splits at a glass edge: cool blue at the top-left, warm pink at the
        // bottom-right, both faint, sitting just inside the white edge line.
        val fringeInset = strokePx * 2.6f
        if (wf > fringeInset * 2 && hf > fringeInset * 2) {
            val fringe = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = maxOf(1f, strokePx * 1.1f)
                shader = LinearGradient(
                    0f, 0f, wf, hf,
                    intArrayOf(
                        Color.argb(72, 130, 214, 255), Color.argb(50, 176, 150, 255), Color.argb(0, 255, 255, 255),
                        Color.argb(0, 255, 255, 255), Color.argb(46, 255, 156, 206), Color.argb(58, 255, 206, 140),
                    ),
                    floatArrayOf(0f, 0.16f, 0.36f, 0.64f, 0.84f, 1f), Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(
                RectF(fringeInset, fringeInset, wf - fringeInset, hf - fringeInset),
                (radius - fringeInset).coerceAtLeast(0f), (radius - fringeInset).coerceAtLeast(0f), fringe
            )
        }
        return out
    }

    /**
     * Refracts the rim of the glass. Inside a band along the edge every pixel shows content taken from further in
     * (the nearer the edge, the further), so the picture stretches and curls at the border the way it does under a
     * thick glass lens. Red, green and blue are taken from slightly different distances, which splits the colours
     * (chromatic aberration). The interior is untouched. Runs once per artwork/size, on the CPU, then is cached.
     */
    private fun refractBezel(bmp: Bitmap, radius: Float) {
        val w = bmp.width
        val h = bmp.height
        if (w < 24 || h < 24) return
        val src = IntArray(w * h)
        bmp.getPixels(src, 0, w, 0, 0, w, h)
        val dst = src.copyOf()
        val bezel = (minOf(w, h) * 0.21f).coerceAtLeast(7f)
        val reach = 0.47f * bezel
        val hx = w / 2f
        val hy = h / 2f
        val rx = hx - radius
        val ry = hy - radius
        val maxX = (w - 1).toFloat()
        val maxY = (h - 1).toFloat()

        // one colour channel, bilinear, at (fx, fy); channel = 16 red, 8 green, 0 blue
        fun chan(fx: Float, fy: Float, shift: Int): Float {
            val x = (fx - 0.5f).coerceIn(0f, maxX)
            val y = (fy - 0.5f).coerceIn(0f, maxY)
            val x0 = x.toInt()
            val y0 = y.toInt()
            val x1 = minOf(x0 + 1, w - 1)
            val y1 = minOf(y0 + 1, h - 1)
            val tx = x - x0
            val ty = y - y0
            val a = (src[y0 * w + x0] shr shift) and 0xFF
            val b = (src[y0 * w + x1] shr shift) and 0xFF
            val c = (src[y1 * w + x0] shr shift) and 0xFF
            val d = (src[y1 * w + x1] shr shift) and 0xFF
            return (a + (b - a) * tx) * (1f - ty) + (c + (d - c) * tx) * ty
        }

        for (y in 0 until h) {
            val py = y + 0.5f - hy
            val sy = if (py < 0f) -1f else 1f
            for (x in 0 until w) {
                val px = x + 0.5f - hx
                val sx = if (px < 0f) -1f else 1f
                val qx = kotlin.math.abs(px) - rx
                val qy = kotlin.math.abs(py) - ry
                val ox = maxOf(qx, 0f)
                val oy = maxOf(qy, 0f)
                val outer = kotlin.math.sqrt(ox * ox + oy * oy)
                val inside = radius - (outer + minOf(maxOf(qx, qy), 0f))   // distance in from the edge
                if (inside <= 0f || inside >= bezel) continue
                // inward direction: toward the corner circle's centre in a corner, straight in along a side
                val nx: Float
                val ny: Float
                if (outer > 0f) {
                    nx = -(ox / outer) * sx
                    ny = -(oy / outer) * sy
                } else if (qx > qy) {
                    nx = -sx
                    ny = 0f
                } else {
                    nx = 0f
                    ny = -sy
                }
                val t = 1f - inside / bezel
                val shift = reach * t * t
                val fx = x + 0.5f
                val fy = y + 0.5f
                val sr = shift * 1.07f
                val sb = shift * 0.93f
                val r = chan(fx + nx * sr, fy + ny * sr, 16)
                val g = chan(fx + nx * shift, fy + ny * shift, 8)
                val b = chan(fx + nx * sb, fy + ny * sb, 0)
                val alpha = src[y * w + x] ushr 24   // keep the anti-aliased rim of the rounded shape
                dst[y * w + x] = (alpha shl 24) or (r.toInt().coerceIn(0, 255) shl 16) or
                    (g.toInt().coerceIn(0, 255) shl 8) or b.toInt().coerceIn(0, 255)
            }
        }
        bmp.setPixels(dst, 0, w, 0, 0, w, h)
    }

    /**
     * Centre-crops artwork and applies a multi-pass separable box blur at useful resolution.
     * The previous tiny-thumbnail upscale produced visible square pixels on large widget surfaces.
     * This keeps enough source detail for a smooth optical-frosted-glass impression while remaining
     * inexpensive: the result is generated only when artwork/size/style changes and is cached by caller.
     */
    private fun blurCover(art: Bitmap, w: Int, h: Int, blur: Int): Bitmap {
        val targetAspect = w.toFloat() / h.coerceAtLeast(1)
        val srcAspect = art.width.toFloat() / art.height.coerceAtLeast(1)
        val src = if (srcAspect > targetAspect) {
            val nw = (art.height * targetAspect).toInt().coerceIn(1, art.width)
            Rect((art.width - nw) / 2, 0, (art.width + nw) / 2, art.height)
        } else {
            val nh = (art.width / targetAspect).toInt().coerceIn(1, art.height)
            Rect(0, (art.height - nh) / 2, art.width, (art.height + nh) / 2)
        }
        val base = Bitmap.createBitmap(art, src.left, src.top, src.width(), src.height())
        val scaled = Bitmap.createScaledBitmap(base, w, h, true)
        if (base !== art && base !== scaled && !base.isRecycled) base.recycle()

        // Blur 0 means no blur: the cover is only centre-cropped and scaled, exactly as the artwork widget shows it.
        // (the caller recycles what this returns, so never hand back the artwork bitmap itself)
        if (blur <= 0) return if (scaled === art) Bitmap.createBitmap(scaled) else scaled

        val pixels = IntArray(w * h)
        scaled.getPixels(pixels, 0, w, 0, 0, w, h)
        if (scaled !== art) scaled.recycle()

        // Box passes approximate a soft Gaussian without the blocky thumbnail look. The radius grows steadily
        // from the first step of the slider (1 px) to its end, relative to the widget's shorter side.
        val shortSide = minOf(w, h).toFloat()
        val userFactor = blur.coerceIn(1, 100) / 100f
        val radius = (shortSide * userFactor * 0.07f).toInt().coerceIn(1, 14)
        boxBlur(pixels, w, h, radius, horizontal = true)
        boxBlur(pixels, w, h, radius, horizontal = false)
        boxBlur(pixels, w, h, radius, horizontal = true)
        boxBlur(pixels, w, h, radius, horizontal = false)

        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also {
            it.setPixels(pixels, 0, w, 0, 0, w, h)
        }
    }

    /** Sliding-window box blur; O(width * height), with no per-pixel object allocations. */
    private fun boxBlur(pixels: IntArray, width: Int, height: Int, radius: Int, horizontal: Boolean) {
        val output = IntArray(pixels.size)
        val lineCount = if (horizontal) height else width
        val lineLength = if (horizontal) width else height
        val window = radius * 2 + 1
        for (line in 0 until lineCount) {
            var a = 0L; var r = 0L; var g = 0L; var b = 0L
            fun index(pos: Int): Int = if (horizontal) line * width + pos else pos * width + line
            for (k in -radius..radius) {
                val c = pixels[index(k.coerceIn(0, lineLength - 1))]
                a += Color.alpha(c); r += Color.red(c); g += Color.green(c); b += Color.blue(c)
            }
            for (pos in 0 until lineLength) {
                output[index(pos)] = Color.argb((a / window).toInt(), (r / window).toInt(), (g / window).toInt(), (b / window).toInt())
                val remove = pixels[index((pos - radius).coerceIn(0, lineLength - 1))]
                val add = pixels[index((pos + radius + 1).coerceIn(0, lineLength - 1))]
                a += Color.alpha(add) - Color.alpha(remove)
                r += Color.red(add) - Color.red(remove)
                g += Color.green(add) - Color.green(remove)
                b += Color.blue(add) - Color.blue(remove)
            }
        }
        System.arraycopy(output, 0, pixels, 0, pixels.size)
    }

    /**
     * The colour the picture is "about": the most saturated, reasonably bright hue family, pushed to a
     * vivid version of itself. A plain average of a photo is mud; this stays a colour. Grey art falls back to its average.
     */
    private fun vibrantColor(bitmap: Bitmap): Int {
        val n = 24
        val small = Bitmap.createScaledBitmap(bitmap, n, n, true)
        val px = IntArray(n * n)
        small.getPixels(px, 0, n, 0, 0, n, n)
        if (small !== bitmap) small.recycle()
        val weight = FloatArray(12)
        val hueSum = FloatArray(12)
        val satSum = FloatArray(12)
        val valSum = FloatArray(12)
        val hsv = FloatArray(3)
        for (p in px) {
            Color.colorToHSV(p, hsv)
            if (hsv[2] < 0.18f || hsv[1] < 0.12f) continue
            val w = hsv[1] * (0.3f + hsv[2])
            val bin = (hsv[0] / 30f).toInt().coerceIn(0, 11)
            weight[bin] += w
            hueSum[bin] += hsv[0] * w
            satSum[bin] += hsv[1] * w
            valSum[bin] += hsv[2] * w
        }
        var best = -1
        for (i in 0 until 12) if (best < 0 || weight[i] > weight[best]) best = i
        if (best < 0 || weight[best] < 0.6f) return averageColor(bitmap)
        val w = weight[best]
        return Color.HSVToColor(
            floatArrayOf(
                hueSum[best] / w,
                (satSum[best] / w * 1.15f).coerceIn(0.55f, 0.95f),
                (valSum[best] / w * 1.2f).coerceIn(0.78f, 1f),
            ),
        )
    }

    private fun averageColor(bitmap: Bitmap): Int {
        val one = Bitmap.createScaledBitmap(bitmap, 1, 1, true)
        val px = one.getPixel(0, 0)
        if (one !== bitmap) one.recycle()
        return Color.rgb(Color.red(px), Color.green(px), Color.blue(px))
    }
}
