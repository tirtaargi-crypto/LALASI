package com.danrey.lalasi.ui.screens.playlist.poster

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Non-destructive effect renderers. Every effect is a layer that is re-drawn on each render; nothing is baked into the user's photos. */
object PosterEffects {
    /** Effects that read what is already on the canvas (colour grading, blur, split ...). */
    val NeedsSnapshot = setOf(
        "bloom", "motion_blur", "monochrome", "duotone", "vintage_fade", "rgb_split", "chromatic",
        "color_grading", "selective_color", "soft_contrast", "film_color", "warm_tone", "cool_tone",
    )

    private val noiseGray: Bitmap by lazy { makeNoise(false) }
    private val noiseColor: Bitmap by lazy { makeNoise(true) }

    private fun makeNoise(color: Boolean): Bitmap {
        val s = 256; val px = IntArray(s * s); val r = Random(42)
        for (i in px.indices) {
            val v = r.nextInt(256)
            px[i] = if (color) Color.rgb((v + r.nextInt(50) - 25).coerceIn(0, 255), v, (v + r.nextInt(50) - 25).coerceIn(0, 255)) else Color.rgb(v, v, v)
        }
        return Bitmap.createBitmap(px, s, s, Bitmap.Config.ARGB_8888)
    }

    private fun fill(col: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = col }
    private fun stroke(col: Int, w: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = col; style = Paint.Style.STROKE; strokeWidth = w; strokeCap = Paint.Cap.ROUND }

    private fun glow(c: Canvas, cx: Float, cy: Float, r: Float, col: Int, a: Float) {
        if (r <= 0f || a <= 0f) return
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(cx, cy, r, intArrayOf(withAlpha(col, a), withAlpha(col, a * .35f), withAlpha(col, 0f)), floatArrayOf(0f, .4f, 1f), Shader.TileMode.CLAMP) }
        c.drawCircle(cx, cy, r, p)
    }

    private fun noiseFill(c: Canvas, w: Float, h: Float, color: Boolean, alpha: Float, scale: Float) {
        val sh = BitmapShader(if (color) noiseColor else noiseGray, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        sh.setLocalMatrix(Matrix().apply { setScale(scale, scale) })
        c.drawRect(0f, 0f, w, h, Paint().apply { shader = sh; this.alpha = (alpha.coerceIn(0f, 1f) * 255).toInt() })
    }

    private fun contrastM(k: Float, off: Float = 0f) = ColorMatrix(floatArrayOf(k, 0f, 0f, 0f, (1 - k) * 128f + off, 0f, k, 0f, 0f, (1 - k) * 128f + off, 0f, 0f, k, 0f, (1 - k) * 128f + off, 0f, 0f, 0f, 1f, 0f))
    private fun gainM(r: Float, g: Float, b: Float, or: Float = 0f, og: Float = 0f, ob: Float = 0f) = ColorMatrix(floatArrayOf(r, 0f, 0f, 0f, or, 0f, g, 0f, 0f, og, 0f, 0f, b, 0f, ob, 0f, 0f, 0f, 1f, 0f))

    private fun drawFiltered(c: Canvas, snap: Bitmap, m: ColorMatrix) {
        c.drawBitmap(snap, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter = ColorMatrixColorFilter(m) })
    }

    /** Cheap, smooth blur: repeated halving then doubling with bilinear filtering. */
    fun fastBlur(src: Bitmap, factor: Int): Bitmap {
        if (factor <= 1) return src
        var cur = src; var f = factor
        while (f > 1) { val nw = max(1, cur.width / 2); val nh = max(1, cur.height / 2); cur = Bitmap.createScaledBitmap(cur, nw, nh, true); f /= 2 }
        var out = cur
        while (out.width < src.width) { out = Bitmap.createScaledBitmap(out, min(src.width, out.width * 2), min(src.height, out.height * 2), true) }
        if (out.width != src.width || out.height != src.height) out = Bitmap.createScaledBitmap(out, src.width, src.height, true)
        return out
    }

    private fun selective(snap: Bitmap, hue: Float): Bitmap {
        val out = snap.copy(Bitmap.Config.ARGB_8888, true)
        val w = out.width; val h = out.height; val strip = 96
        val buf = IntArray(w * strip); val hsv = FloatArray(3)
        var y = 0
        while (y < h) {
            val rows = min(strip, h - y)
            out.getPixels(buf, 0, w, 0, y, w, rows)
            for (i in 0 until w * rows) {
                Color.colorToHSV(buf[i], hsv)
                var d = abs(hsv[0] - hue); if (d > 180f) d = 360f - d
                val keep = (1f - ((d - 25f) / 25f)).coerceIn(0f, 1f)
                hsv[1] *= keep
                buf[i] = Color.HSVToColor(buf[i] ushr 24, hsv)
            }
            out.setPixels(buf, 0, w, 0, y, w, rows)
            y += rows
        }
        return out
    }

    fun draw(c: Canvas, key: String, w: Float, h: Float, l: Layer, snap: Bitmap?) {
        val sc = w / 1080f
        val rnd = Random((l.id.hashCode() * 31 + key.hashCode()).toLong())
        val cx = l.x * w; val cy = l.y * h
        val R = l.size * w * 1.2f
        val a = l.intensity.coerceIn(0f, 1f)
        val col = l.color
        when (key) {
            // ------------------------------------------------ lighting
            "lens_flare" -> {
                glow(c, cx, cy, R * .9f, col, a * .9f)
                val dx = w / 2 - cx; val dy = h / 2 - cy
                val cols = intArrayOf(0xFFFFD27A.toInt(), 0xFF7AD2FF.toInt(), 0xFFFF7AC8.toInt(), 0xFF9AFF7A.toInt(), 0xFFFFFFFF.toInt())
                val ts = floatArrayOf(.35f, .6f, .85f, 1.15f, 1.5f)
                for (i in ts.indices) {
                    val x = cx + dx * ts[i]; val y = cy + dy * ts[i]; val r = R * .1f * (1f + (i % 3) * .6f)
                    c.drawCircle(x, y, r, fill(withAlpha(cols[i], a * .28f))); c.drawCircle(x, y, r, stroke(withAlpha(cols[i], a * .5f), 2f * sc))
                }
                val sp = Paint().apply { shader = LinearGradient(0f, 0f, w, 0f, intArrayOf(0, withAlpha(Color.WHITE, a * .7f), 0), floatArrayOf(0f, .5f, 1f), Shader.TileMode.CLAMP) }
                c.save(); c.translate(0f, cy); c.drawRect(0f, -R * .012f, w, R * .012f, sp); c.restore()
            }
            "light_leak" -> {
                glow(c, cx, cy, R * 1.1f, col, a)
                glow(c, cx + R * .5f, cy + R * .6f, R * .9f, 0xFFFF3D7F.toInt(), a * .7f)
                glow(c, cx, cy - R * .5f, R * .7f, 0xFFFFE066.toInt(), a * .6f)
            }
            "soft_glow" -> {
                glow(c, cx, cy, R * 1.3f, col, a * .85f)
                c.drawRect(0f, 0f, w, h, fill(withAlpha(col, a * .12f)))
            }
            "bloom" -> if (snap != null) {
                val bright = Bitmap.createBitmap(snap.width, snap.height, Bitmap.Config.ARGB_8888)
                Canvas(bright).drawBitmap(snap, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(2.2f, 0f, 0f, 0f, -250f, 0f, 2.2f, 0f, 0f, -250f, 0f, 0f, 2.2f, 0f, -250f, 0f, 0f, 0f, 1f, 0f))) })
                val b = fastBlur(bright, 16)
                c.drawBitmap(b, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG).apply { alpha = (min(1f, a * 1.4f) * 255).toInt() })
            }
            "spotlight" -> {
                val p = Paint().apply { shader = RadialGradient(cx, cy, max(R, 1f), intArrayOf(0x00000000, withAlpha(Color.BLACK, a * .5f), withAlpha(Color.BLACK, a * .9f)), floatArrayOf(0f, .55f, 1f), Shader.TileMode.CLAMP) }
                c.drawRect(0f, 0f, w, h, p)
                glow(c, cx, cy, R * .8f, col, a * .22f)
            }
            "film_flash" -> {
                c.drawRect(0f, 0f, w, h, fill(withAlpha(col, a * .45f)))
                glow(c, cx, cy, max(w, h) * .6f, Color.WHITE, a * .8f)
                val p = Paint().apply { shader = LinearGradient(0f, 0f, 0f, h, withAlpha(col, a * .5f), 0, Shader.TileMode.CLAMP) }
                c.drawRect(0f, 0f, w, h, p)
            }
            "sun_flare" -> {
                glow(c, cx, cy, R * 1.1f, col, a)
                c.save(); c.translate(cx, cy); c.rotate(l.rotation)
                for (i in 0 until 12) {
                    c.save(); c.rotate(i * 30f)
                    val ray = Path().apply { moveTo(0f, 0f); lineTo(R * 1.6f, -R * .02f); lineTo(R * 1.6f, R * .02f); close() }
                    c.drawPath(ray, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, 0f, R * 1.6f, 0f, withAlpha(Color.WHITE, a * .5f), 0, Shader.TileMode.CLAMP) })
                    c.restore()
                }
                c.restore()
                c.drawCircle(cx, cy, R * .5f, stroke(withAlpha(col, a * .4f), 3f * sc))
            }
            "light_streak" -> {
                c.save(); c.translate(cx, cy); c.rotate(l.rotation); c.scale(1f, .035f)
                c.drawCircle(0f, 0f, R * 1.8f, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(0f, 0f, R * 1.8f, intArrayOf(withAlpha(Color.WHITE, a), withAlpha(col, a * .6f), 0), floatArrayOf(0f, .35f, 1f), Shader.TileMode.CLAMP) })
                c.restore()
            }
            "prism_light" -> {
                c.save(); c.translate(cx, cy); c.rotate(-30f + l.rotation)
                val cols = intArrayOf(0, withAlpha(0xFFFF4D4D.toInt(), a * .6f), withAlpha(0xFFFFD84D.toInt(), a * .6f), withAlpha(0xFF4DFF88.toInt(), a * .6f), withAlpha(0xFF4DB8FF.toInt(), a * .6f), withAlpha(0xFFB04DFF.toInt(), a * .6f), 0)
                val p = Paint().apply { shader = LinearGradient(0f, -R, 0f, R, cols, null, Shader.TileMode.CLAMP) }
                c.drawRect(-w * 1.5f, -R, w * 1.5f, R, p); c.restore()
            }
            "rgb_light" -> {
                glow(c, cx - R * .6f, cy, R, 0xFFFF2D55.toInt(), a * .8f)
                glow(c, cx + R * .6f, cy, R, 0xFF2D6BFF.toInt(), a * .8f)
                glow(c, cx, cy - R * .5f, R * .7f, 0xFF2DFF9A.toInt(), a * .5f)
            }
            "holographic" -> {
                val cols = intArrayOf(0xFFFF9AD5.toInt(), 0xFF9AF0FF.toInt(), 0xFFFFF39A.toInt(), 0xFFC49AFF.toInt(), 0xFFFF9AD5.toInt())
                c.drawRect(0f, 0f, w, h, Paint().apply { shader = SweepGradient(cx, cy, cols, null); alpha = (a * 200).toInt() })
                c.drawRect(0f, 0f, w, h, Paint().apply { shader = LinearGradient(0f, 0f, w, h, intArrayOf(withAlpha(Color.WHITE, a * .4f), 0, withAlpha(Color.WHITE, a * .4f)), null, Shader.TileMode.CLAMP) })
            }
            "camera_flash" -> {
                glow(c, cx, cy, R * 1.2f, Color.WHITE, a)
                c.save(); c.translate(cx, cy)
                for (i in 0 until 4) { c.save(); c.rotate(i * 90f + 45f * (i % 2)); c.drawRect(-R * 1.4f, -R * .012f, R * 1.4f, R * .012f, Paint().apply { shader = LinearGradient(-R * 1.4f, 0f, R * 1.4f, 0f, intArrayOf(0, withAlpha(Color.WHITE, a), 0), floatArrayOf(0f, .5f, 1f), Shader.TileMode.CLAMP) }); c.restore() }
                c.restore()
            }
            // ------------------------------------------------ atmosphere
            "fog", "smoke" -> {
                val n = (6 + a * 8).toInt(); val tint = if (key == "fog") Color.WHITE else col
                for (i in 0 until n) {
                    val x = rnd.nextFloat() * w; val y = h * (.45f + rnd.nextFloat() * .55f)
                    c.save(); c.translate(x, y); c.scale(1f, if (key == "fog") .55f else .9f)
                    glow(c, 0f, 0f, w * (.2f + rnd.nextFloat() * .25f) * (l.size + .4f), tint, a * (if (key == "fog") .3f else .22f))
                    c.restore()
                }
            }
            "dust_particles" -> repeat((60 + a * 220).toInt()) { c.drawCircle(rnd.nextFloat() * w, rnd.nextFloat() * h, (0.7f + rnd.nextFloat() * 1.8f) * sc * (l.size + .5f), fill(withAlpha(Color.WHITE, (.25f + rnd.nextFloat() * .5f) * a))) }
            "floating_particles" -> repeat((20 + a * 60).toInt()) { val x = rnd.nextFloat() * w; val y = rnd.nextFloat() * h; val r = (2f + rnd.nextFloat() * 7f) * sc * (l.size + .5f); glow(c, x, y, r * 2.2f, 0xFFFFF3D8.toInt(), a * .7f); c.drawCircle(x, y, r * .5f, fill(withAlpha(Color.WHITE, a))) }
            "bokeh" -> repeat((10 + a * 22).toInt()) {
                val x = rnd.nextFloat() * w; val y = rnd.nextFloat() * h; val r = w * (.02f + rnd.nextFloat() * .05f) * (l.size + .5f)
                val cc = intArrayOf(0xFFFFE0A0.toInt(), 0xFFFFB0C8.toInt(), 0xFFA0D8FF.toInt(), 0xFFFFFFFF.toInt())[rnd.nextInt(4)]
                c.drawCircle(x, y, r, fill(withAlpha(cc, a * .22f))); c.drawCircle(x, y, r, stroke(withAlpha(cc, a * .45f), 2f * sc))
            }
            "sparkles" -> repeat((10 + a * 26).toInt()) { val r = w * (.008f + rnd.nextFloat() * .022f) * (l.size + .5f); c.drawPath(PosterVectors.sparklePath(rnd.nextFloat() * w, rnd.nextFloat() * h, r), fill(withAlpha(if (rnd.nextBoolean()) col else 0xFFFFE9A8.toInt(), a))) }
            "star_particles" -> repeat((14 + a * 36).toInt()) { val r = w * (.004f + rnd.nextFloat() * .012f) * (l.size + .5f); c.drawPath(PosterVectors.starPath(rnd.nextFloat() * w, rnd.nextFloat() * h, r, r * .45f, 5), fill(withAlpha(col, a * (.5f + rnd.nextFloat() * .5f)))) }
            "rain" -> {
                val ang = (12f + l.rotation) * PI.toFloat() / 180f
                val p = stroke(withAlpha(Color.WHITE, a * .45f), 1.6f * sc)
                repeat((120 + a * 260).toInt()) { val x = rnd.nextFloat() * w * 1.2f; val y = rnd.nextFloat() * h; val len = h * (.03f + rnd.nextFloat() * .05f); c.drawLine(x, y, x - sin(ang) * len, y + cos(ang) * len, p) }
            }
            "snow" -> repeat((60 + a * 200).toInt()) { val r = (1.5f + rnd.nextFloat() * 4.5f) * sc * (l.size + .5f); c.drawCircle(rnd.nextFloat() * w, rnd.nextFloat() * h, r, fill(withAlpha(Color.WHITE, (.45f + rnd.nextFloat() * .5f) * a))) }
            "soft_haze" -> {
                c.drawRect(0f, 0f, w, h, Paint().apply { shader = LinearGradient(0f, h, 0f, h * .35f, withAlpha(Color.WHITE, a * .65f), 0, Shader.TileMode.CLAMP) })
                c.drawRect(0f, 0f, w, h, fill(withAlpha(Color.WHITE, a * .12f)))
            }
            "motion_blur" -> if (snap != null) {
                val n = 10; val dist = a * w * .05f; val ang = l.rotation * PI.toFloat() / 180f
                for (k in 0..n) {
                    val t = -1f + 2f * k / n
                    c.drawBitmap(snap, cos(ang) * dist * t, sin(ang) * dist * t, Paint(Paint.FILTER_BITMAP_FLAG).apply { alpha = (255f / (k + 1)).toInt().coerceIn(1, 255) })
                }
            }
            // ------------------------------------------------ texture
            "grain" -> noiseFill(c, w, h, false, .35f + a * .65f, sc * (.6f + l.size))
            "vintage_noise" -> { noiseFill(c, w, h, true, .35f + a * .6f, sc * (.7f + l.size)); c.drawRect(0f, 0f, w, h, fill(withAlpha(0xFFB58A4A.toInt(), a * .18f))) }
            "paper_texture" -> paper(c, w, h, rnd, a, sc, 0xFFF2EBDA.toInt(), false)
            "old_paper" -> paper(c, w, h, rnd, a, sc, 0xFFD9B98A.toInt(), true)
            "dust_texture" -> {
                repeat((200 + a * 500).toInt()) { c.drawCircle(rnd.nextFloat() * w, rnd.nextFloat() * h, (0.5f + rnd.nextFloat() * 1.8f) * sc, fill(withAlpha(if (rnd.nextInt(3) == 0) Color.BLACK else Color.WHITE, (.3f + rnd.nextFloat() * .6f) * a))) }
                repeat((6 + a * 14).toInt()) { val x = rnd.nextFloat() * w; val y = rnd.nextFloat() * h; val p = Path().apply { moveTo(x, y); quadTo(x + rnd.nextFloat() * 40f * sc - 20f * sc, y + rnd.nextFloat() * 40f * sc, x + rnd.nextFloat() * 70f * sc - 35f * sc, y + rnd.nextFloat() * 60f * sc) }; c.drawPath(p, stroke(withAlpha(Color.WHITE, a * .6f), 1f * sc)) }
            }
            "scratches" -> repeat((12 + a * 30).toInt()) { val x = rnd.nextFloat() * w; val y = rnd.nextFloat() * h; val len = h * (.1f + rnd.nextFloat() * .6f); c.drawLine(x, y, x + (rnd.nextFloat() - .5f) * 8f * sc, y + len, stroke(withAlpha(Color.WHITE, (.25f + rnd.nextFloat() * .5f) * a), (.6f + rnd.nextFloat() * 1.2f) * sc)) }
            "halftone" -> {
                val sp = 16f * sc * (l.size + .4f); var y = 0f; var row = 0
                while (y < h + sp) { var x = if (row % 2 == 0) 0f else sp / 2; while (x < w + sp) { val g = 1f - y / h * .6f; c.drawCircle(x, y, sp * .42f * g * a.coerceAtLeast(.2f), fill(Color.BLACK)); x += sp }; y += sp * .87f; row++ }
            }
            "newspaper" -> {
                c.drawRect(0f, 0f, w, h, fill(withAlpha(0xFFE6DFCC.toInt(), .9f)))
                val sp = 7f * sc; var y = 0f; var row = 0
                while (y < h + sp) { var x = if (row % 2 == 0) 0f else sp / 2; while (x < w + sp) { c.drawCircle(x, y, sp * .22f * (.5f + a * .5f), fill(withAlpha(Color.BLACK, .55f))); x += sp }; y += sp * .87f; row++ }
                var ty = h * .06f; while (ty < h) { c.drawRect(w * .05f, ty, w * (.5f + rnd.nextFloat() * .45f), ty + 3f * sc, fill(withAlpha(Color.BLACK, .22f * a))); ty += 11f * sc }
            }
            "photocopy" -> {
                val sh = BitmapShader(noiseGray, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                sh.setLocalMatrix(Matrix().apply { setScale(sc * (.8f + l.size), sc * (.8f + l.size)) })
                // dark noise pixels become black toner, bright ones stay transparent
                val k = 3f + a * 4f
                val toner = ColorMatrix(floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, -k, 0f, 0f, 0f, k * 140f))
                c.drawRect(0f, 0f, w, h, Paint().apply { shader = sh; colorFilter = ColorMatrixColorFilter(toner) })
                val e = Paint().apply { shader = RadialGradient(w / 2, h / 2, max(w, h) * .75f, intArrayOf(0, 0, withAlpha(Color.BLACK, a * .8f)), floatArrayOf(0f, .6f, 1f), Shader.TileMode.CLAMP) }
                c.drawRect(0f, 0f, w, h, e)
            }
            "vhs" -> {
                var y = 0f; while (y < h) { c.drawRect(0f, y, w, y + 1.5f * sc, fill(withAlpha(Color.BLACK, .18f * a))); y += 4f * sc }
                repeat((2 + a * 4).toInt()) {
                    val by = rnd.nextFloat() * h; val bh = (6f + rnd.nextFloat() * 18f) * sc
                    c.drawRect(0f, by, w, by + bh, fill(withAlpha(0xFFFF2D55.toInt(), .12f * a))); c.drawRect(0f, by + bh * .5f, w, by + bh * 1.2f, fill(withAlpha(0xFF2DD4FF.toInt(), .1f * a)))
                }
                c.save(); c.clipRect(0f, h * .92f, w, h); noiseFill(c, w, h, true, .9f * a, sc * 1.2f); c.restore()
                c.drawRect(0f, 0f, w, h, fill(withAlpha(0xFF2D3A8C.toInt(), .06f * a)))
            }
            "analog_film" -> {
                noiseFill(c, w, h, false, .3f + a * .4f, sc)
                val v = Paint().apply { shader = RadialGradient(w / 2, h / 2, max(w, h) * .75f, intArrayOf(0, 0, withAlpha(0xFF6B2A00.toInt(), a * .7f)), floatArrayOf(0f, .55f, 1f), Shader.TileMode.CLAMP) }
                c.drawRect(0f, 0f, w, h, v)
                repeat((8 + a * 14).toInt()) { val x = rnd.nextFloat() * w; c.drawLine(x, 0f, x + (rnd.nextFloat() - .5f) * 6f * sc, h * (.2f + rnd.nextFloat() * .7f), stroke(withAlpha(Color.WHITE, .3f * a), 1f * sc)) }
            }
            "grunge" -> {
                repeat((14 + a * 22).toInt()) { val x = rnd.nextFloat() * w; val y = rnd.nextFloat() * h; glow(c, x, y, w * (.04f + rnd.nextFloat() * .14f), Color.BLACK, .25f * a) }
                noiseFill(c, w, h, false, .5f * a, sc * 1.4f)
                val e = Paint().apply { shader = RadialGradient(w / 2, h / 2, max(w, h) * .72f, intArrayOf(0, 0, withAlpha(Color.BLACK, a * .85f)), floatArrayOf(0f, .5f, 1f), Shader.TileMode.CLAMP) }
                c.drawRect(0f, 0f, w, h, e)
            }
            // ------------------------------------------------ colour (operate on what is below)
            "monochrome" -> if (snap != null) drawFiltered(c, snap, ColorMatrix().apply { setSaturation(0f); postConcat(contrastM(1.1f)) })
            "duotone" -> if (snap != null) {
                val c1 = l.color; val c2 = l.color2
                val dr = (Color.red(c2) - Color.red(c1)) / 255f; val dg = (Color.green(c2) - Color.green(c1)) / 255f; val db = (Color.blue(c2) - Color.blue(c1)) / 255f
                drawFiltered(c, snap, ColorMatrix(floatArrayOf(
                    dr * .213f, dr * .715f, dr * .072f, 0f, Color.red(c1).toFloat(),
                    dg * .213f, dg * .715f, dg * .072f, 0f, Color.green(c1).toFloat(),
                    db * .213f, db * .715f, db * .072f, 0f, Color.blue(c1).toFloat(),
                    0f, 0f, 0f, 1f, 0f)))
            }
            "vintage_fade" -> if (snap != null) drawFiltered(c, snap, ColorMatrix().apply { setSaturation(.78f); postConcat(contrastM(.86f, 22f)); postConcat(gainM(1.05f, 1f, .92f, 6f, 2f, 0f)) })
            "color_leak" -> {
                c.drawRect(0f, 0f, w, h, Paint().apply { shader = LinearGradient(0f, 0f, w * .5f, 0f, intArrayOf(withAlpha(col, a), withAlpha(0xFFFF9A3D.toInt(), a * .5f), 0), floatArrayOf(0f, .4f, 1f), Shader.TileMode.CLAMP) })
                c.drawRect(0f, 0f, w, h, Paint().apply { shader = LinearGradient(w, h, w * .6f, h * .6f, intArrayOf(withAlpha(0xFF8A4DFF.toInt(), a * .8f), 0), null, Shader.TileMode.CLAMP) })
            }
            "rgb_split" -> if (snap != null) {
                c.drawColor(Color.BLACK)
                val d = a * w * .02f
                val add = PorterDuffXfermode(PorterDuff.Mode.ADD)
                val chans = listOf(
                    floatArrayOf(1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f) to -d,
                    floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f) to 0f,
                    floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f) to d,
                )
                for ((m, dx) in chans) c.drawBitmap(snap, dx, 0f, Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter = ColorMatrixColorFilter(ColorMatrix(m)); xfermode = add })
            }
            "chromatic" -> if (snap != null) {
                c.drawColor(Color.BLACK)
                val k = a * .025f; val add = PorterDuffXfermode(PorterDuff.Mode.ADD)
                val chans = listOf(
                    floatArrayOf(1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f) to 1f + k,
                    floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f) to 1f,
                    floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f) to 1f - k,
                )
                for ((m, s) in chans) { c.save(); c.scale(s, s, w / 2, h / 2); c.drawBitmap(snap, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter = ColorMatrixColorFilter(ColorMatrix(m)); xfermode = add }); c.restore() }
            }
            "color_grading" -> if (snap != null) drawFiltered(c, snap, ColorMatrix().apply { postConcat(contrastM(1.08f)); postConcat(gainM(1.08f, 1f, .9f, 4f, 0f, 10f)) })
            "selective_color" -> if (snap != null) {
                val hsv = FloatArray(3); Color.colorToHSV(l.color, hsv)
                val out = selective(snap, hsv[0]); c.drawBitmap(out, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
            }
            "soft_contrast" -> if (snap != null) drawFiltered(c, snap, ColorMatrix().apply { postConcat(contrastM(.9f, 8f)) })
            "film_color" -> if (snap != null) drawFiltered(c, snap, ColorMatrix().apply { setSaturation(.9f); postConcat(contrastM(1.04f)); postConcat(gainM(1.04f, 1.0f, .9f, 6f, 2f, 4f)) })
            "warm_tone" -> if (snap != null) drawFiltered(c, snap, gainM(1.12f, 1.02f, .86f))
            "cool_tone" -> if (snap != null) drawFiltered(c, snap, gainM(.88f, 1.0f, 1.14f))
        }
    }

    private fun paper(c: Canvas, w: Float, h: Float, rnd: Random, a: Float, sc: Float, base: Int, aged: Boolean) {
        c.drawRect(0f, 0f, w, h, fill(withAlpha(base, .55f + a * .45f)))
        noiseFill(c, w, h, false, .12f + a * .18f, sc * 1.6f)
        repeat((40 + a * 120).toInt()) {
            val x = rnd.nextFloat() * w; val y = rnd.nextFloat() * h
            c.drawLine(x, y, x + (rnd.nextFloat() - .5f) * 30f * sc, y + (rnd.nextFloat() - .5f) * 30f * sc, stroke(withAlpha(if (aged) 0xFF6B4A22.toInt() else 0xFFB8AE98.toInt(), .18f), .8f * sc))
        }
        if (aged) {
            val e = Paint().apply { shader = RadialGradient(w / 2, h / 2, max(w, h) * .72f, intArrayOf(0, withAlpha(0xFF7A4A1C.toInt(), a * .25f), withAlpha(0xFF4A2A0C.toInt(), a * .85f)), floatArrayOf(.3f, .7f, 1f), Shader.TileMode.CLAMP) }
            c.drawRect(0f, 0f, w, h, e)
        }
    }

    /** Overlay layers (secondary decoration): textures, patterns, colour, light. Blur is handled by the renderer because it needs the backdrop. */
    fun drawOverlay(c: Canvas, key: String, w: Float, h: Float, l: Layer) {
        val a = l.intensity.coerceIn(0f, 1f)
        when (key) {
            "paper_tex" -> draw(c, "paper_texture", w, h, l.copy(intensity = a), null)
            "film_tex" -> { draw(c, "grain", w, h, l.copy(intensity = a), null); draw(c, "scratches", w, h, l.copy(intensity = a * .8f), null) }
            "vintage_tex" -> draw(c, "old_paper", w, h, l.copy(intensity = a), null)
            "pattern_dots", "pattern_grid", "pattern_stripes", "pattern_checker", "pattern_stars" -> PosterVectors.drawPattern(c, key, w, h, withAlpha(l.color, 1f), withAlpha(l.color2, 1f), l.size + .4f)
            "color_solid" -> c.drawRect(0f, 0f, w, h, fill(l.color))
            "color_gradient" -> c.drawRect(0f, 0f, w, h, Paint().apply { shader = LinearGradient(0f, 0f, 0f, h, l.color, l.color2, Shader.TileMode.CLAMP) })
            "light" -> {
                c.drawRect(0f, 0f, w, h, Paint().apply { shader = LinearGradient(0f, 0f, w, h, withAlpha(Color.WHITE, a), 0, Shader.TileMode.CLAMP) })
                glow(c, w * .15f, h * .1f, w * .9f, 0xFFFFF3DC.toInt(), a * .8f)
            }
        }
    }
}
