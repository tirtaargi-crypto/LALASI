package com.danrey.lalasi.ui.screens.playlist.poster

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** card height / width; includes a footer strip under the controls that holds the LALASI mark */
const val PLAYER_ASPECT = 1.6f

// glint positions of the four corners on the outline (0 = middle of the top edge, clockwise); the card is w wide, 1.6w tall, corner radius .12w
private const val GLINT_RAD = .12f
private const val GLINT_TOTAL = 2f * (1f - 2f * GLINT_RAD) + 2f * (PLAYER_ASPECT - 2f * GLINT_RAD) + 2f * 3.14159265f * GLINT_RAD
private const val GLINT_ARC = .5f * 3.14159265f * GLINT_RAD
private const val GLINT_A = .5f - GLINT_RAD
private const val GLINT_B = PLAYER_ASPECT - 2f * GLINT_RAD
private const val GLINT_C = 1f - 2f * GLINT_RAD
const val GLINT_TOP_RIGHT = (GLINT_A + GLINT_ARC / 2f) / GLINT_TOTAL
const val GLINT_BOTTOM_RIGHT = (GLINT_A + GLINT_ARC + GLINT_B + GLINT_ARC / 2f) / GLINT_TOTAL
const val GLINT_BOTTOM_LEFT = (GLINT_A + 2f * GLINT_ARC + GLINT_B + GLINT_C + GLINT_ARC / 2f) / GLINT_TOTAL
const val GLINT_TOP_LEFT = (GLINT_A + 3f * GLINT_ARC + 2f * GLINT_B + GLINT_C + GLINT_ARC / 2f) / GLINT_TOTAL

fun withAlpha(col: Int, a: Float): Int {
    val base = (col ushr 24) / 255f
    return (col and 0x00FFFFFF) or (((base * a).coerceIn(0f, 1f) * 255f).toInt() shl 24)
}

fun mixColor(a: Int, b: Int, t: Float): Int {
    val k = t.coerceIn(0f, 1f)
    fun ch(s: Int) = (((a shr s) and 255) * (1 - k) + ((b shr s) and 255) * k).toInt().coerceIn(0, 255)
    return (ch(24) shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
}

/** All vector art used by Poster Studio: stickers, the portrait music player, frames and pattern overlays. Pure Canvas drawing, no emoji or Unicode glyph icons. */
object PosterVectors {
    private fun fill(col: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = col; style = Paint.Style.FILL }
    private fun line(col: Int, w: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = col; style = Paint.Style.STROKE; strokeWidth = w; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private fun txt(col: Int, size: Float, bold: Boolean = true, face: Typeface = Typeface.SANS_SERIF) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = col; textSize = size; typeface = Typeface.create(face, if (bold) Typeface.BOLD else Typeface.NORMAL); textAlign = Paint.Align.CENTER
    }
    private fun rr(c: Canvas, l: Float, t: Float, r: Float, b: Float, rad: Float, p: Paint) = c.drawRoundRect(l, t, r, b, rad, rad, p)

    fun starPath(cx: Float, cy: Float, ro: Float, ri: Float, n: Int): Path {
        val p = Path()
        for (i in 0 until n * 2) {
            val r = if (i % 2 == 0) ro else ri
            val a = -PI / 2 + i * PI / n
            val x = cx + (r * cos(a)).toFloat(); val y = cy + (r * sin(a)).toFloat()
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close(); return p
    }

    fun sparklePath(cx: Float, cy: Float, r: Float): Path = Path().apply {
        moveTo(cx, cy - r); quadTo(cx, cy, cx + r, cy); quadTo(cx, cy, cx, cy + r); quadTo(cx, cy, cx - r, cy); quadTo(cx, cy, cx, cy - r); close()
    }

    private fun heartPath(w: Float, h: Float): Path = Path().apply {
        moveTo(w / 2, h * .92f)
        cubicTo(-w * .12f, h * .5f, w * .08f, h * .0f, w / 2, h * .3f)
        cubicTo(w * .92f, h * .0f, w * 1.12f, h * .5f, w / 2, h * .92f); close()
    }

    private fun jagged(l: Float, t: Float, r: Float, b: Float, amp: Float, step: Float, seed: Long, top: Boolean = true, bottom: Boolean = true, left: Boolean = true, right: Boolean = true): Path {
        val rnd = Random(seed); val p = Path()
        fun j() = (rnd.nextFloat() - .5f) * 2f * amp
        p.moveTo(l, t)
        var x = l; while (x < r) { x = minOf(x + step, r); p.lineTo(x, t + if (top && x < r) j() else 0f) }
        var y = t; while (y < b) { y = minOf(y + step, b); p.lineTo(r + if (right && y < b) j() else 0f, y) }
        x = r; while (x > l) { x = maxOf(x - step, l); p.lineTo(x, b + if (bottom && x > l) j() else 0f) }
        y = b; while (y > t) { y = maxOf(y - step, t); p.lineTo(l + if (left && y > t) j() else 0f, y) }
        p.close(); return p
    }

    // ------------------------------------------------------------------ icons used inside the player

    private fun skipIcon(c: Canvas, cx: Float, cy: Float, s: Float, dir: Int, p: Paint) {
        val d = dir.toFloat(); val tw = s * .55f
        for (k in 0..1) {
            val x0 = cx + d * (k - 1) * tw + d * (-tw * .0f)
            val path = Path().apply {
                moveTo(x0 - d * tw * .5f, cy - s / 2); lineTo(x0 - d * tw * .5f, cy + s / 2); lineTo(x0 + d * tw * .5f, cy); close()
            }
            c.drawPath(path, p)
        }
        // soft rounded corners
        val rim = Paint(p).apply { style = Paint.Style.STROKE; strokeWidth = s * .12f; strokeJoin = Paint.Join.ROUND }
        for (k in 0..1) {
            val x0 = cx + d * (k - 1) * tw
            val path = Path().apply {
                moveTo(x0 - d * tw * .5f, cy - s / 2); lineTo(x0 - d * tw * .5f, cy + s / 2); lineTo(x0 + d * tw * .5f, cy); close()
            }
            c.drawPath(path, rim)
        }
    }

    private fun pauseIcon(c: Canvas, cx: Float, cy: Float, s: Float, p: Paint) {
        val bw = s * .3f; val gap = s * .24f
        rr(c, cx - gap / 2 - bw, cy - s / 2, cx - gap / 2, cy + s / 2, bw * .3f, p)
        rr(c, cx + gap / 2, cy - s / 2, cx + gap / 2 + bw, cy + s / 2, bw * .3f, p)
    }

    private fun speakerIcon(c: Canvas, cx: Float, cy: Float, s: Float, p: Paint, waves: Int) {
        val body = Path().apply {
            moveTo(cx - s * .5f, cy - s * .18f); lineTo(cx - s * .22f, cy - s * .18f); lineTo(cx + s * .05f, cy - s * .42f)
            lineTo(cx + s * .05f, cy + s * .42f); lineTo(cx - s * .22f, cy + s * .18f); lineTo(cx - s * .5f, cy + s * .18f); close()
        }
        c.drawPath(body, p)
        val st = Paint(p).apply { style = Paint.Style.STROKE; strokeWidth = s * .09f; strokeCap = Paint.Cap.ROUND }
        for (i in 1..waves) {
            val r = s * (.14f + i * .15f)
            c.drawArc(RectF(cx + s * .05f - r, cy - r, cx + s * .05f + r, cy + r), -40f, 80f, false, st)
        }
    }

    private fun musicNoteIcon(c: Canvas, cx: Float, cy: Float, s: Float, p: Paint) {
        c.drawCircle(cx - s * .15f, cy + s * .3f, s * .2f, p)
        c.drawRect(cx + s * .02f, cy - s * .4f, cx + s * .1f, cy + s * .3f, p)
        val flag = Path().apply { moveTo(cx + s * .1f, cy - s * .4f); quadTo(cx + s * .4f, cy - s * .25f, cx + s * .3f, cy); quadTo(cx + s * .28f, cy - s * .18f, cx + s * .1f, cy - s * .2f); close() }
        c.drawPath(flag, p)
    }

    private fun fit(text: String, p: Paint, maxW: Float): String {
        if (p.measureText(text) <= maxW) return text
        var n = text.length
        while (n > 1 && p.measureText(text.substring(0, n) + "...") > maxW) n--
        return text.substring(0, n).trimEnd() + "..."
    }

    // ------------------------------------------------------------------ the portrait music player (matches the user reference)

    /** average colour of the artwork (32x32 sample), pushed towards a rich mid-tone so the card tint feels like Apple Music's */
    private fun artTint(b: Bitmap): Int {
        val sm = Bitmap.createScaledBitmap(b, 32, 32, true)
        val px = IntArray(32 * 32); sm.getPixels(px, 0, 32, 0, 0, 32, 32)
        if (sm !== b) sm.recycle()
        var r = 0L; var g = 0L; var bl = 0L
        for (c in px) { r += (c shr 16) and 255; g += (c shr 8) and 255; bl += c and 255 }
        val n = px.size
        val hsv = FloatArray(3)
        Color.RGBToHSV((r / n).toInt(), (g / n).toInt(), (bl / n).toInt(), hsv)
        hsv[1] = (hsv[1] * 1.3f).coerceIn(0f, 1f); hsv[2] = hsv[2].coerceIn(.4f, .9f)
        return Color.HSVToColor(hsv)
    }

    private fun starOutline(c: Canvas, cx: Float, cy: Float, s: Float, col: Int) {
        c.drawPath(starPath(cx, cy, s * .5f, s * .22f, 5), line(col, s * .11f))
    }
    private fun dotsIcon(c: Canvas, cx: Float, cy: Float, s: Float, col: Int) {
        val p = fill(col); for (i in -1..1) c.drawCircle(cx + i * s * .26f, cy, s * .065f, p)
    }
    private fun lyricsIcon(c: Canvas, cx: Float, cy: Float, s: Float, col: Int) {
        val st = line(col, s * .085f)
        c.drawRoundRect(RectF(cx - s * .5f, cy - s * .38f, cx + s * .5f, cy + s * .26f), s * .16f, s * .16f, st)
        val tail = Path().apply { moveTo(cx - s * .22f, cy + s * .26f); lineTo(cx - s * .22f, cy + s * .46f); lineTo(cx - s * .02f, cy + s * .26f) }
        c.drawPath(tail, st)
        c.drawLine(cx - s * .26f, cy - s * .15f, cx + s * .26f, cy - s * .15f, st)
        c.drawLine(cx - s * .26f, cy + s * .04f, cx + s * .1f, cy + s * .04f, st)
    }
    private fun queueIcon(c: Canvas, cx: Float, cy: Float, s: Float, col: Int) {
        val st = line(col, s * .085f); val f = fill(col)
        for (i in -1..1) {
            val y = cy + i * s * .3f
            c.drawCircle(cx - s * .42f, y, s * .06f, f)
            c.drawLine(cx - s * .2f, y, cx + s * .5f, y, st)
        }
    }

    /** round frosted-glass button (the star / ellipsis next to the title) */
    private fun glassDisc(c: Canvas, cx: Float, cy: Float, r: Float, dark: Boolean) {
        c.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(cx, cy - r, cx, cy + r, withAlpha(if (dark) Color.WHITE else Color.BLACK, if (dark) .26f else .12f), withAlpha(if (dark) Color.WHITE else Color.BLACK, if (dark) .10f else .05f), Shader.TileMode.CLAMP)
        })
        c.drawCircle(cx, cy, r - r * .03f, line(withAlpha(Color.WHITE, if (dark) .3f else .7f), r * .05f))
    }

    fun drawPlayer(c: Canvas, w: Float, h: Float, style: Int, art: Bitmap?, title: String, artist: String, progress: Float, elapsed: String, total: String, artSize: Float, logo: Bitmap? = null, glint: Boolean = true, glintPos: Float = GLINT_TOP_RIGHT, glintSize: Float = 1f, glintGlow: Float = 1f, glintLen: Float = 1f, glintTone: Float = 0f) {
        val pad = w * .08f
        val dark = style != 2
        val ink = if (dark) Color.WHITE else 0xFF1C1C1E.toInt()
        val tint = if (art != null) artTint(art) else 0xFF6C5CE7.toInt()
        val bgTop = when (style) { 1 -> 0x3CFFFFFF; 2 -> mixColor(0xFFF7F7F9.toInt(), tint, .16f); 3 -> mixColor(0xFF3A2D22.toInt(), tint, .16f); else -> 0x80303034.toInt() }
        val bgBottom = when (style) { 1 -> 0x40101012; 2 -> mixColor(0xFFD8D8E0.toInt(), tint, .26f); 3 -> mixColor(0xFF281F18.toInt(), tint, .12f); else -> 0x9A101012.toInt() }
        val rad = w * .12f
        val cardPath = Path().apply { addRoundRect(RectF(0f, 0f, w, h), rad, rad, Path.Direction.CW) }
        rr(c, 0f, 0f, w, h, rad, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, 0f, 0f, h, bgTop, bgBottom, Shader.TileMode.CLAMP) })

        // ---- glass: artwork-coloured glow, diagonal specular streak, top sheen, shaded underside (what makes it read as a 3D slab)
        c.save(); c.clipPath(cardPath)
        val glowR = w * .95f
        c.drawCircle(w * .5f, w * .45f, glowR, Paint().apply { shader = RadialGradient(w * .5f, w * .45f, glowR, intArrayOf(withAlpha(if (style <= 1) Color.WHITE else tint, if (style <= 1) .05f else if (dark) .30f else .26f), 0), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP) })
        c.drawRect(0f, 0f, w, h * .26f, Paint().apply { shader = LinearGradient(0f, 0f, 0f, h * .26f, withAlpha(Color.WHITE, if (dark) .14f else .65f), 0, Shader.TileMode.CLAMP) })
        c.drawPath(Path().apply { moveTo(w * .46f, 0f); lineTo(w * .70f, 0f); lineTo(0f, h * .42f); lineTo(0f, h * .24f); close() }, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(Color.WHITE, if (dark) .055f else .4f) })
        c.drawRect(0f, h * .66f, w, h, Paint().apply { shader = LinearGradient(0f, h * .66f, 0f, h, 0, withAlpha(Color.BLACK, if (dark) .48f else .14f), Shader.TileMode.CLAMP) })
        // the slab's thickness: the sides fall into shadow, so the card reads as a solid object and not a flat sticker
        c.drawRect(0f, 0f, w, h, Paint().apply { shader = LinearGradient(0f, 0f, w, 0f, intArrayOf(withAlpha(Color.BLACK, if (dark) .30f else .10f), 0, 0, withAlpha(Color.BLACK, if (dark) .40f else .14f)), floatArrayOf(0f, .14f, .8f, 1f), Shader.TileMode.CLAMP) })
        c.restore()

        // ---- bevel: bright top-left rim, soft dark edge at the bottom, so the edge has thickness
        val rw = w * .008f
        if (!dark) rr(c, rw, rw, w - rw, h - rw, rad, line(0x26000000, w * .004f))
        c.drawRoundRect(RectF(rw / 2, rw / 2, w - rw / 2, h - rw / 2), rad, rad, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE; strokeWidth = rw
            shader = LinearGradient(0f, 0f, w, h, intArrayOf(withAlpha(Color.WHITE, .95f), withAlpha(Color.WHITE, .3f), withAlpha(Color.WHITE, .08f), withAlpha(Color.WHITE, .42f)), floatArrayOf(0f, .3f, .7f, 1f), Shader.TileMode.CLAMP)
        })
        c.drawRoundRect(RectF(rw * 1.6f, rw * 1.6f, w - rw * 1.6f, h - rw * 1.6f), rad - rw, rad - rw, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE; strokeWidth = rw * .8f
            shader = LinearGradient(0f, 0f, 0f, h, intArrayOf(0, 0, withAlpha(Color.BLACK, if (dark) .45f else .16f)), floatArrayOf(0f, .6f, 1f), Shader.TileMode.CLAMP)
        })

        // the lower and right edge of the slab: a thin dark rim, as if the card had thickness
        c.drawRoundRect(RectF(rw * 0.2f, rw * 0.2f, w - rw * 0.2f, h - rw * 0.2f), rad, rad, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE; strokeWidth = rw * 1.3f
            shader = LinearGradient(0f, 0f, w, h, intArrayOf(0, 0, withAlpha(Color.BLACK, .55f)), floatArrayOf(0f, .55f, 1f), Shader.TileMode.CLAMP)
        })

        // ---- artwork, floating above the glass
        val area = w - 2 * pad
        // the artwork gives up a little height on the shorter card so the rows below never collide
        val side = minOf(area * artSize.coerceIn(0.5f, 1f), (h - pad * .9f - w * .61f - w * .12f).coerceAtLeast(area * .5f))
        val al = pad + (area - side) / 2f
        val artTop = pad * .9f
        val artRect = RectF(al, artTop, al + side, artTop + side)
        val ar = w * .05f
        c.drawRoundRect(artRect, ar, ar, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF000000.toInt(); setShadowLayer(w * .07f, 0f, w * .035f, 0xAA000000.toInt()) })
        if (art != null) {
            c.save()
            c.clipPath(Path().apply { addRoundRect(artRect, ar, ar, Path.Direction.CW) })
            val src = android.graphics.Rect(0, 0, art.width, art.height)
            val m = minOf(art.width, art.height)
            val crop = android.graphics.Rect((art.width - m) / 2, (art.height - m) / 2, (art.width + m) / 2, (art.height + m) / 2)
            c.drawBitmap(art, if (art.width == art.height) src else crop, artRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            c.restore()
        } else {
            c.drawRoundRect(artRect, ar, ar, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(artRect.left, artRect.top, artRect.right, artRect.bottom, 0xFF6C5CE7.toInt(), 0xFFFF8A3D.toInt(), Shader.TileMode.CLAMP) })
            musicNoteIcon(c, artRect.centerX(), artRect.centerY(), side * .35f, fill(0xCCFFFFFF.toInt()))
        }
        c.save(); c.clipPath(Path().apply { addRoundRect(artRect, ar, ar, Path.Direction.CW) })
        c.drawRect(artRect, Paint().apply { shader = LinearGradient(artRect.left, artRect.top, artRect.right, artRect.bottom, intArrayOf(withAlpha(Color.WHITE, .20f), 0, withAlpha(Color.BLACK, .12f)), floatArrayOf(0f, .5f, 1f), Shader.TileMode.CLAMP) })
        c.restore()
        c.drawRoundRect(artRect, ar, ar, line(withAlpha(Color.WHITE, .28f), w * .004f))

        // ---- vertical rhythm: blocks flow down from the artwork and share the spare height, so nothing collides at any artwork size
        val blocks = w * (.155f + .095f + .14f + .06f + .075f + .085f) // info, progress, controls, volume, bottom row, footer
        val free = h - (artTop + side) - blocks
        val gap = (free / 6f).coerceIn(w * .02f, w * .075f)
        var y = artTop + side + gap + ((free - gap * 6f) / 2f).coerceAtLeast(0f)
        // title + star / ellipsis glass buttons
        val disc = w * .047f
        val dotsX = w - pad - disc; val starX = dotsX - disc * 2f - w * .02f
        val rowY = y + w * .045f
        glassDisc(c, starX, rowY, disc, dark); starOutline(c, starX, rowY, disc * .95f, withAlpha(ink, .95f))
        glassDisc(c, dotsX, rowY, disc, dark); dotsIcon(c, dotsX, rowY, disc * 1.1f, withAlpha(ink, .95f))
        val textMax = starX - disc - w * .03f - pad
        val tp = txt(ink, w * .07f, true).apply { textAlign = Paint.Align.LEFT; setShadowLayer(w * .01f, 0f, w * .004f, 0x44000000) }
        c.drawText(fit(title, tp, textMax), pad, y + w * .06f, tp)
        val ap = txt(withAlpha(ink, .62f), w * .056f, false).apply { textAlign = Paint.Align.LEFT }
        c.drawText(fit(artist, ap, textMax), pad, y + w * .135f, ap)
        y += w * .155f + gap
        // scrubber with a knob
        val th = w * .016f; val by = y + w * .014f
        rr(c, pad, by, w - pad, by + th, th / 2, fill(withAlpha(ink, .26f)))
        val px = pad + area * progress.coerceIn(0.02f, 1f)
        rr(c, pad, by, px, by + th, th / 2, fill(withAlpha(ink, .92f)))
        c.drawCircle(px, by + th / 2, th * .95f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; setShadowLayer(w * .01f, 0f, w * .004f, 0x66000000) })
        val tm = txt(withAlpha(ink, .55f), w * .038f, true)
        tm.textAlign = Paint.Align.LEFT; c.drawText(elapsed, pad, by + th + w * .052f, tm)
        tm.textAlign = Paint.Align.RIGHT; c.drawText(total, w - pad, by + th + w * .052f, tm)
        y += w * .095f + gap
        // transport: glyphs carry a soft shadow so they sit on the glass
        val cy = y + w * .075f
        val ip = fill(ink).apply { setShadowLayer(w * .014f, 0f, w * .008f, 0x55000000) }
        skipIcon(c, w * .22f, cy, w * .09f, -1, ip)
        pauseIcon(c, w * .5f, cy, w * .12f, ip)
        skipIcon(c, w * .78f, cy, w * .09f, 1, ip)
        y += w * .14f + gap * .8f
        // volume
        val vy = y + w * .03f
        speakerIcon(c, pad + w * .02f, vy, w * .05f, fill(withAlpha(ink, .6f)), 0)
        val vx0 = pad + w * .1f; val vx1 = w - pad - w * .1f
        rr(c, vx0, vy - th / 2, vx1, vy + th / 2, th / 2, fill(withAlpha(ink, .26f)))
        rr(c, vx0, vy - th / 2, vx0 + (vx1 - vx0) * .45f, vy + th / 2, th / 2, fill(withAlpha(ink, .85f)))
        speakerIcon(c, w - pad - w * .035f, vy, w * .05f, fill(withAlpha(ink, .6f)), 2)
        y += w * .06f + gap
        // bottom row: lyrics . output . queue
        val py = y + w * .0375f
        lyricsIcon(c, pad + w * .04f, py, w * .06f, withAlpha(ink, .7f))
        queueIcon(c, w - pad - w * .04f, py, w * .06f, withAlpha(ink, .7f))
        val pillW = w * .36f; val pillH = w * .075f
        rr(c, w / 2 - pillW / 2, py - pillH / 2, w / 2 + pillW / 2, py + pillH / 2, pillH / 2, fill(withAlpha(ink, .15f)))
        rr(c, w / 2 - pillW / 2, py - pillH / 2, w / 2 + pillW / 2, py + pillH / 2, pillH / 2, line(withAlpha(Color.WHITE, if (dark) .2f else .6f), w * .003f))
        speakerIcon(c, w / 2 - pillW / 2 + pillH * .6f, py, pillH * .5f, fill(withAlpha(ink, .85f)), 1)
        c.drawText("Phone Speaker", w / 2 + pillH * .35f, py + w * .014f, txt(withAlpha(ink, .85f), w * .036f, true))

        if (glint) drawGlint(c, w, h, rad, glintPos, glintSize, glintGlow, glintLen, glintTone)

        // LALASI mark in its own footer strip, centred, in the card's ink colour at high contrast.
        val footerCy = h - w * .05f
        val markH = w * .064f
        val ink2 = withAlpha(ink, .9f)
        val wmText = txt(ink2, w * .046f, true).apply { textAlign = Paint.Align.LEFT; letterSpacing = .14f }
        val textW = wmText.measureText("LALASI")
        val textBase = footerCy + (wmText.descent() - wmText.ascent()) / 2f - wmText.descent()
        if (logo != null && logo.width > 0) {
            val markW = markH * logo.width / logo.height
            val gapX = w * .026f
            val startX = (w - (markW + gapX + textW)) / 2f
            val tintP = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { colorFilter = PorterDuffColorFilter(ink2, PorterDuff.Mode.SRC_IN) }
            c.drawBitmap(logo, null, RectF(startX, footerCy - markH / 2f, startX + markW, footerCy + markH / 2f), tintP)
            c.drawText("LALASI", startX + markW + gapX, textBase, wmText)
        } else {
            c.drawText("LALASI", (w - textW) / 2f, textBase, wmText)
        }
    }

    /**
     * The light glint on the card's rim. Its place is chosen by the user, anywhere on the outline ([pos] 0..1, clockwise from the middle
     * of the top edge; the corners are presets, see GLINT_*), its brightness by [glow], its thickness by [size], how far it streaks along
     * the rim by [len] and its colour by [tone] (-1 cool, 0 white, 1 warm). Nothing here is placed automatically.
     * The bloom is allowed outside the card silhouette; the caller supplies transparent overscan around the player bitmap.
     */
    private fun drawGlint(c: Canvas, w: Float, h: Float, rad: Float, pos: Float, size: Float, glow: Float, lenK: Float, tone: Float) {
        val k = size.coerceIn(.65f, 1.8f)
        val power = glow.coerceIn(.25f, 1.8f)
        val tn = tone.coerceIn(-1f, 1f)
        val tint = if (tn >= 0f) mixColor(Color.WHITE, 0xFFFFC98A.toInt(), tn) else mixColor(Color.WHITE, 0xFF9CC8FF.toInt(), -tn)

        // the outline, clockwise from the middle of the top edge, so a position means the same thing on every card
        val rim = Path().apply {
            moveTo(w / 2f, 0f)
            lineTo(w - rad, 0f); arcTo(RectF(w - 2 * rad, 0f, w, 2 * rad), -90f, 90f)
            lineTo(w, h - rad); arcTo(RectF(w - 2 * rad, h - 2 * rad, w, h), 0f, 90f)
            lineTo(rad, h); arcTo(RectF(0f, h - 2 * rad, 2 * rad, h), 90f, 90f)
            lineTo(0f, rad); arcTo(RectF(0f, 0f, 2 * rad, 2 * rad), 180f, 90f)
            close()
        }
        val pm = PathMeasure(rim, false)
        val total = pm.length
        if (total <= 0f) return
        val d = (((pos % 1f) + 1f) % 1f) * total
        val half = (w * .22f * lenK.coerceIn(.4f, 2f) * k.coerceAtMost(1.4f) + rad * .8f).coerceAtMost(total / 2.2f)

        val pa = FloatArray(2); val ta = FloatArray(2)
        fun at(dist: Float, p: FloatArray, t: FloatArray) { pm.getPosTan(((dist % total) + total) % total, p, t) }
        val p0 = FloatArray(2); val p1 = FloatArray(2); val tmp = FloatArray(2)
        at(d - half, p0, tmp); at(d + half, p1, tmp)
        at(d, pa, ta)
        val stops = floatArrayOf(0f, .5f, 1f)
        fun along(a: Float, col: Int = Color.WHITE) = LinearGradient(p0[0], p0[1], p1[0], p1[1],
            intArrayOf(0, withAlpha(col, a * power), 0), stops, Shader.TileMode.CLAMP)

        // the streak: the part of the outline around the position, two pieces when it runs past the start of the outline
        val seg = Path().apply {
            val s0 = d - half; val s1 = d + half
            if (s0 >= 0f && s1 <= total) pm.getSegment(s0, s1, this, true)
            else {
                val a0 = if (s0 < 0f) s0 + total else s0
                val a1 = if (s1 > total) s1 - total else s1
                val part = Path()
                pm.getSegment(a0, total, part, true); addPath(part)
                part.reset(); pm.getSegment(0f, a1, part, true); addPath(part)
            }
        }
        c.save()
        // no clip to the card: the light bloom must visibly spill past the rim
        c.drawPath(seg, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = w * .105f * k; strokeCap = Paint.Cap.ROUND
            shader = along(.78f, tint)
            maskFilter = BlurMaskFilter(w * .052f * k * power, BlurMaskFilter.Blur.NORMAL)
        })
        c.drawPath(seg, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = w * .038f * k; strokeCap = Paint.Cap.ROUND
            shader = along(.88f, tint)
            maskFilter = BlurMaskFilter(w * .018f * k * power, BlurMaskFilter.Blur.NORMAL)
        })
        c.drawPath(seg, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = w * .0085f * k; strokeCap = Paint.Cap.ROUND
            shader = along(1f)
        })

        // the bright focal point, just inside the rim, with its cross of fine rays turned to follow the outline there
        val tx = ta[0]; val ty = ta[1]
        val nx = -ty; val ny = tx // the inward side: the outline runs clockwise
        val ins = w * .0045f
        val hx = pa[0] + nx * ins; val hy = pa[1] + ny * ins
        val hot = w * .082f * k
        c.drawCircle(hx, hy, hot, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(hx, hy, hot,
                intArrayOf(withAlpha(tint, .72f * power), withAlpha(tint, .22f * power), 0),
                floatArrayOf(0f, .34f, 1f), Shader.TileMode.CLAMP)
        })
        val flare = w * .105f * k
        fun hair(dx: Float, dy: Float, len: Float, a: Float) {
            val x0 = hx - dx * len; val y0 = hy - dy * len; val x1 = hx + dx * len; val y1 = hy + dy * len
            c.drawLine(x0, y0, x1, y1, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeWidth = w * .0032f * k; strokeCap = Paint.Cap.ROUND
                shader = LinearGradient(x0, y0, x1, y1, intArrayOf(0, withAlpha(Color.WHITE, a * power), 0), stops, Shader.TileMode.CLAMP)
            })
        }
        hair(tx, ty, flare, .95f)
        hair(nx, ny, flare, .95f)
        val dg = .7071f
        hair((tx + nx) * dg, (ty + ny) * dg, flare * .5f, .48f)
        c.drawCircle(hx, hy, w * .0075f * k, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })

        // a small second sparkle further back along the rim
        at(d - half * .62f, p0, tmp)
        val sx = p0[0]; val sy = p0[1]
        val speck = w * .026f * k
        c.drawCircle(sx, sy, speck, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(sx, sy, speck, intArrayOf(withAlpha(tint, .68f * power), 0), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        })
        c.drawCircle(sx, sy, w * .0032f * k, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
        c.restore()
    }

    // ------------------------------------------------------------------ stickers (every one has its own shape)

    fun drawSticker(c: Canvas, key: String, w: Float, h: Float, col: Int, col2: Int) {
        when (key) {
            "star" -> {
                c.drawPath(starPath(w / 2, h * .53f, w * .5f, w * .22f, 5), fill(col))
                c.drawPath(starPath(w / 2, h * .53f, w * .5f, w * .22f, 5), line(col2, w * .035f))
                c.drawPath(starPath(w / 2, h * .5f, w * .26f, w * .1f, 5), fill(0x55FFFFFF))
            }
            "sparkle" -> {
                c.drawPath(sparklePath(w * .5f, h * .5f, w * .5f), fill(col))
                c.drawPath(sparklePath(w * .2f, h * .2f, w * .13f), fill(col2))
                c.drawPath(sparklePath(w * .82f, h * .8f, w * .1f), fill(col2))
            }
            "heart" -> {
                val p = heartPath(w, h)
                c.drawPath(p, fill(col)); c.drawPath(p, line(col2, w * .035f))
                c.drawArc(RectF(w * .17f, h * .16f, w * .4f, h * .4f), 200f, 70f, false, line(0x99FFFFFF.toInt(), w * .04f))
            }
            "flower" -> {
                for (i in 0 until 6) {
                    val a = i * PI / 3
                    val px = w / 2 + (cos(a) * w * .27f).toFloat(); val py = h / 2 + (sin(a) * h * .27f).toFloat()
                    c.drawCircle(px, py, w * .19f, fill(col)); c.drawCircle(px, py, w * .19f, line(0x33000000, w * .012f))
                }
                c.drawCircle(w / 2, h / 2, w * .15f, fill(col2))
                c.drawCircle(w / 2, h / 2, w * .15f, line(0x33000000, w * .012f))
            }
            "butterfly" -> {
                for (side in 0..1) {
                    c.save(); if (side == 1) c.scale(-1f, 1f, w / 2, h / 2)
                    val up = Path().apply { moveTo(w * .5f, h * .45f); cubicTo(w * .62f, h * .02f, w * 1.0f, h * -.02f, w * .98f, h * .3f); cubicTo(w * .97f, h * .5f, w * .75f, h * .55f, w * .5f, h * .5f); close() }
                    val lo = Path().apply { moveTo(w * .5f, h * .5f); cubicTo(w * .8f, h * .52f, w * .95f, h * .72f, w * .8f, h * .9f); cubicTo(w * .65f, h * 1.0f, w * .52f, h * .82f, w * .5f, h * .56f); close() }
                    c.drawPath(up, fill(col)); c.drawPath(lo, fill(mixColor(col, col2, .35f)))
                    c.drawPath(up, line(col2, w * .02f)); c.drawPath(lo, line(col2, w * .02f))
                    c.drawCircle(w * .78f, h * .24f, w * .06f, fill(0xCCFFFFFF.toInt())); c.drawCircle(w * .72f, h * .74f, w * .04f, fill(0xCCFFFFFF.toInt()))
                    c.restore()
                }
                rr(c, w * .47f, h * .3f, w * .53f, h * .86f, w * .03f, fill(col2))
                val an = line(col2, w * .016f)
                c.drawLine(w * .5f, h * .32f, w * .4f, h * .1f, an); c.drawLine(w * .5f, h * .32f, w * .6f, h * .1f, an)
            }
            "ribbon" -> {
                val tail = Path().apply { moveTo(0f, h * .28f); lineTo(w * .2f, h * .28f); lineTo(w * .2f, h * .95f); lineTo(0f, h * .95f); lineTo(w * .07f, h * .61f); close() }
                c.drawPath(tail, fill(col2)); c.save(); c.scale(-1f, 1f, w / 2, h / 2); c.drawPath(tail, fill(col2)); c.restore()
                c.drawRect(w * .12f, h * .1f, w * .88f, h * .8f, fill(col))
                c.drawRect(w * .12f, h * .1f, w * .88f, h * .8f, line(col2, w * .012f))
                c.drawLine(w * .16f, h * .2f, w * .84f, h * .2f, line(0x66FFFFFF, w * .012f))
                c.drawLine(w * .16f, h * .7f, w * .84f, h * .7f, line(0x66FFFFFF, w * .012f))
            }
            "bow" -> {
                for (side in 0..1) {
                    c.save(); if (side == 1) c.scale(-1f, 1f, w / 2, h / 2)
                    val loop = Path().apply { moveTo(w * .5f, h * .46f); cubicTo(w * .25f, h * -.06f, w * -.02f, h * .1f, w * .04f, h * .46f); cubicTo(w * -.02f, h * .84f, w * .25f, h * .92f, w * .5f, h * .5f); close() }
                    c.drawPath(loop, fill(col)); c.drawPath(loop, line(col2, w * .025f))
                    val tail = Path().apply { moveTo(w * .46f, h * .55f); lineTo(w * .28f, h * .98f); lineTo(w * .4f, h * .9f); lineTo(w * .52f, h * .6f); close() }
                    c.drawPath(tail, fill(col)); c.drawPath(tail, line(col2, w * .02f))
                    c.restore()
                }
                rr(c, w * .4f, h * .34f, w * .6f, h * .62f, w * .06f, fill(col2))
            }
            "bubbles" -> {
                val spots = listOf(Triple(.3f, .66f, .24f), Triple(.68f, .42f, .18f), Triple(.45f, .18f, .1f), Triple(.8f, .8f, .09f))
                for ((x, y, r) in spots) {
                    c.drawCircle(w * x, h * y, w * r, fill(withAlpha(col, .22f)))
                    c.drawCircle(w * x, h * y, w * r, line(withAlpha(col, .95f), w * .015f))
                    c.drawArc(RectF(w * (x - r * .7f), h * (y - r * .7f), w * (x + r * .2f), h * (y + r * .2f)), 200f, 60f, false, line(withAlpha(col2, .9f), w * .015f))
                }
            }
            "doodle" -> {
                val p = Path().apply {
                    moveTo(w * .08f, h * .25f)
                    for (i in 0 until 4) { quadTo(w * (.2f + i * .22f), h * (if (i % 2 == 0) -.05f else .55f), w * (.3f + i * .2f), h * .3f) }
                }
                c.drawPath(p, line(col, w * .05f))
                c.drawCircle(w * .2f, h * .8f, w * .05f, fill(col2)); c.drawCircle(w * .55f, h * .9f, w * .035f, fill(col))
                c.drawPath(sparklePath(w * .82f, h * .75f, w * .12f), fill(col2))
            }
            "hand_shapes" -> {
                val rnd = Random(5); val p = Path()
                for (i in 0..28) {
                    val a = i * 2 * PI / 28; val r = w * (.3f + rnd.nextFloat() * .035f)
                    val x = w * .4f + (cos(a) * r).toFloat(); val y = h * .45f + (sin(a) * r).toFloat()
                    if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                }
                c.drawPath(p, line(col, w * .035f))
                val ar = line(col2, w * .04f)
                c.drawLine(w * .62f, h * .86f, w * .92f, h * .6f, ar)
                c.drawLine(w * .92f, h * .6f, w * .8f, h * .6f, ar); c.drawLine(w * .92f, h * .6f, w * .9f, h * .72f, ar)
            }
            "deco_line" -> {
                val p = Path().apply { moveTo(w * .12f, h * .5f); var x = w * .12f; var up = true; while (x < w * .88f) { x += w * .05f; lineTo(x, if (up) h * .2f else h * .8f); up = !up } }
                c.drawPath(p, line(col, h * .08f))
                c.drawPath(starPath(w * .05f, h * .5f, h * .4f, h * .17f, 5), fill(col2)); c.drawPath(starPath(w * .95f, h * .5f, h * .4f, h * .17f, 5), fill(col2))
            }
            "ornament" -> {
                val st = line(col, w * .02f)
                for (side in 0..1) {
                    c.save(); if (side == 1) c.scale(-1f, 1f, w / 2, h / 2)
                    val p = Path().apply { moveTo(w * .46f, h * .5f); cubicTo(w * .36f, h * .1f, w * .8f, h * .0f, w * .94f, h * .38f); cubicTo(w * .82f, h * .62f, w * .62f, h * .4f, w * .7f, h * .3f) }
                    c.drawPath(p, st); c.drawCircle(w * .9f, h * .65f, w * .02f, fill(col))
                    c.restore()
                }
                val d = Path().apply { moveTo(w / 2, h * .3f); lineTo(w * .56f, h / 2); lineTo(w / 2, h * .7f); lineTo(w * .44f, h / 2); close() }
                c.drawPath(d, fill(col2))
            }
            "vinyl" -> {
                c.drawCircle(w / 2, h / 2, w / 2, fill(col2))
                for (i in 1..5) c.drawCircle(w / 2, h / 2, w * (.2f + i * .05f), line(0x14FFFFFF, w * .006f))
                c.drawArc(RectF(w * .1f, h * .1f, w * .9f, h * .9f), 210f, 40f, true, fill(0x14FFFFFF))
                c.drawArc(RectF(w * .1f, h * .1f, w * .9f, h * .9f), 30f, 40f, true, fill(0x14FFFFFF))
                c.drawCircle(w / 2, h / 2, w * .17f, fill(col)); c.drawCircle(w / 2, h / 2, w * .17f, line(0x33000000, w * .008f))
                c.drawCircle(w / 2, h / 2, w * .028f, fill(0xFF1C1C1E.toInt()))
            }
            "cassette" -> {
                rr(c, 0f, 0f, w, h, w * .06f, fill(col)); rr(c, 0f, 0f, w, h, w * .06f, line(col2, w * .02f))
                rr(c, w * .07f, h * .09f, w * .93f, h * .63f, w * .03f, fill(0xFFFFFFFF.toInt()))
                c.drawRect(w * .07f, h * .09f, w * .93f, h * .2f, fill(0xFFE24A4A.toInt()))
                c.drawText("PLAYLIST", w / 2, h * .185f, txt(0xFFFFFFFF.toInt(), h * .075f))
                rr(c, w * .2f, h * .28f, w * .8f, h * .56f, h * .07f, fill(col2))
                for (cx in listOf(.36f, .64f)) { c.drawCircle(w * cx, h * .42f, h * .075f, fill(0xFFFFFFFF.toInt())); c.drawCircle(w * cx, h * .42f, h * .03f, fill(col2))
                    for (k in 0 until 6) { val a = k * PI / 3; c.drawLine(w * cx, h * .42f, w * cx + (cos(a) * h * .075f).toFloat(), h * .42f + (sin(a) * h * .075f).toFloat(), line(col2, w * .008f)) } }
                val bot = Path().apply { moveTo(w * .16f, h * .7f); lineTo(w * .84f, h * .7f); lineTo(w * .92f, h * .98f); lineTo(w * .08f, h * .98f); close() }
                c.drawPath(bot, fill(withAlpha(col2, .45f)))
                for (cx in listOf(.28f, .72f)) c.drawCircle(w * cx, h * .84f, w * .03f, fill(col))
                for (p in listOf(.05f to .06f, .95f to .06f, .05f to .94f, .95f to .94f)) c.drawCircle(w * p.first, h * p.second, w * .015f, fill(col2))
            }
            "cd" -> {
                val ring = Path().apply { fillType = Path.FillType.EVEN_ODD; addCircle(w / 2, h / 2, w / 2, Path.Direction.CW); addCircle(w / 2, h / 2, w * .09f, Path.Direction.CCW) }
                val sg = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = SweepGradient(w / 2, h / 2, intArrayOf(0xFFF3F6FF.toInt(), col2, 0xFFFFE9F3.toInt(), col, 0xFFE3FFF4.toInt(), col2, 0xFFF3F6FF.toInt()), null) }
                c.drawPath(ring, sg); c.drawPath(ring, line(0x55000000, w * .01f))
                c.drawCircle(w / 2, h / 2, w * .24f, line(0x66FFFFFF, w * .02f)); c.drawCircle(w / 2, h / 2, w * .15f, line(0x44FFFFFF, w * .06f))
            }
            "headphones" -> {
                c.drawArc(RectF(w * .12f, h * .06f, w * .88f, h * .98f), 180f, 180f, false, line(col2, w * .1f))
                c.drawArc(RectF(w * .12f, h * .06f, w * .88f, h * .98f), 180f, 180f, false, line(col, w * .06f))
                for (side in 0..1) {
                    c.save(); if (side == 1) c.scale(-1f, 1f, w / 2, h / 2)
                    rr(c, w * .03f, h * .5f, w * .27f, h * .9f, w * .08f, fill(col2)); rr(c, w * .08f, h * .55f, w * .27f, h * .85f, w * .06f, fill(col))
                    c.restore()
                }
            }
            "earphones" -> {
                val cable = line(col, w * .022f)
                c.drawPath(Path().apply { moveTo(w * .16f, h * .2f); cubicTo(w * .12f, h * .7f, w * .4f, h * .9f, w * .56f, h * .72f) }, cable)
                c.drawPath(Path().apply { moveTo(w * .86f, h * .24f); cubicTo(w * .92f, h * .7f, w * .7f, h * .9f, w * .6f, h * .72f) }, cable)
                c.drawPath(Path().apply { moveTo(w * .58f, h * .72f); lineTo(w * .56f, h * .98f) }, cable)
                rr(c, w * .545f, h * .6f, w * .625f, h * .78f, w * .03f, fill(col)); c.drawCircle(w * .585f, h * .66f, w * .012f, fill(col2))
                for (b in listOf(.16f to .12f, .86f to .16f)) { c.drawOval(RectF(w * (b.first - .05f), h * (b.second - .08f), w * (b.first + .05f), h * (b.second + .08f)), fill(col)); c.drawCircle(w * b.first, h * b.second, w * .02f, fill(col2)) }
            }
            "notes" -> {
                val head = fill(col)
                for (p in listOf(.24f to .78f, .72f to .68f)) { c.save(); c.rotate(-20f, w * p.first, h * p.second); c.drawOval(RectF(w * (p.first - .15f), h * (p.second - .1f), w * (p.first + .15f), h * (p.second + .1f)), head); c.restore() }
                c.drawRect(w * .35f, h * .2f, w * .41f, h * .74f, head); c.drawRect(w * .83f, h * .1f, w * .89f, h * .64f, head)
                c.drawPath(Path().apply { moveTo(w * .35f, h * .2f); lineTo(w * .89f, h * .08f); lineTo(w * .89f, h * .24f); lineTo(w * .35f, h * .36f); close() }, head)
            }
            "equalizer" -> {
                val hs = listOf(.45f, .8f, .6f, 1f, .7f, .5f, .85f); val bw = w / (hs.size * 1.6f)
                val g = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, 0f, 0f, h, col, col2, Shader.TileMode.CLAMP) }
                hs.forEachIndexed { i, v -> val x = i * bw * 1.6f + bw * .1f; rr(c, x, h * (1 - v), x + bw, h, bw / 2, g) }
            }
            "waveform" -> {
                val n = 17; val st = line(col, w * .03f)
                for (i in 0 until n) { val x = w * (.05f + .9f * i / (n - 1)); val a = (.18f + .8f * kotlin.math.abs(sin(i * 1.7f) * cos(i * .6f))); c.drawLine(x, h * (.5f - a / 2), x, h * (.5f + a / 2), st) }
                c.drawLine(w * .05f, h * .5f, w * .95f, h * .5f, line(col2, w * .008f))
            }
            "speaker" -> {
                rr(c, 0f, 0f, w, h, w * .1f, fill(col)); rr(c, 0f, 0f, w, h, w * .1f, line(col2, w * .02f))
                c.drawCircle(w / 2, h * .26f, w * .13f, fill(0xFF111111.toInt())); c.drawCircle(w / 2, h * .26f, w * .13f, line(col2, w * .015f)); c.drawCircle(w / 2, h * .26f, w * .05f, fill(col2))
                c.drawCircle(w / 2, h * .66f, w * .3f, fill(0xFF111111.toInt())); c.drawCircle(w / 2, h * .66f, w * .3f, line(col2, w * .02f))
                c.drawCircle(w / 2, h * .66f, w * .19f, line(0x55FFFFFF, w * .012f)); c.drawCircle(w / 2, h * .66f, w * .08f, fill(0xFF2E2E31.toInt()))
            }
            "music_label" -> {
                rr(c, 0f, 0f, w, h, h / 2, fill(col)); rr(c, 0f, 0f, w, h, h / 2, line(col2, h * .06f))
                val hs = listOf(.35f, .7f, .5f, .85f)
                hs.forEachIndexed { i, v -> val x = h * .5f + i * h * .16f; rr(c, x, h * (.5f - v * .25f), x + h * .09f, h * (.5f + v * .25f), h * .04f, fill(col2)) }
                c.drawText("NOW PLAYING", w * .6f, h * .64f, txt(col2, h * .4f))
            }
            "retro_player" -> {
                c.drawPath(Path().apply { moveTo(w * .22f, h * .22f); cubicTo(w * .22f, -h * .1f, w * .78f, -h * .1f, w * .78f, h * .22f) }, line(col2, w * .03f))
                rr(c, 0f, h * .2f, w, h, w * .07f, fill(col)); rr(c, 0f, h * .2f, w, h, w * .07f, line(col2, w * .02f))
                for (cx in listOf(.2f, .8f)) { c.drawCircle(w * cx, h * .62f, h * .22f, fill(col2)); c.drawCircle(w * cx, h * .62f, h * .14f, line(0x55FFFFFF, w * .012f)); c.drawCircle(w * cx, h * .62f, h * .05f, fill(0xFF55555A.toInt())) }
                rr(c, w * .36f, h * .36f, w * .64f, h * .62f, w * .02f, fill(col2))
                for (cx in listOf(.45f, .55f)) c.drawCircle(w * cx, h * .49f, h * .05f, fill(0xFFEDE6D4.toInt()))
                for (i in 0 until 4) rr(c, w * (.38f + i * .06f), h * .72f, w * (.42f + i * .06f), h * .82f, w * .005f, fill(col2))
                c.drawCircle(w * .5f, h * .3f, h * .025f, fill(0xFFE24A4A.toInt()))
            }
            "music_symbols" -> {
                val st = fill(col)
                c.drawRect(w * .14f, h * .12f, w * .2f, h * .82f, st); c.drawRect(w * .3f, h * .08f, w * .36f, h * .78f, st)
                c.drawPath(Path().apply { moveTo(w * .06f, h * .4f); lineTo(w * .44f, h * .28f); lineTo(w * .44f, h * .38f); lineTo(w * .06f, h * .5f); close() }, st)
                c.drawPath(Path().apply { moveTo(w * .06f, h * .68f); lineTo(w * .44f, h * .56f); lineTo(w * .44f, h * .66f); lineTo(w * .06f, h * .78f); close() }, st)
                c.drawOval(RectF(w * .55f, h * .68f, w * .8f, h * .88f), st); c.drawRect(w * .76f, h * .2f, w * .81f, h * .78f, st)
                c.drawPath(Path().apply { moveTo(w * .81f, h * .2f); quadTo(w * 1.0f, h * .3f, w * .9f, h * .5f); quadTo(w * .92f, h * .35f, w * .81f, h * .34f); close() }, st)
            }
            "tape" -> {
                val p = Path(); val n = 7; p.moveTo(w * .04f, 0f)
                p.lineTo(w * .96f, 0f)
                for (i in 0 until n) p.lineTo(w * (if (i % 2 == 0) .99f else .93f), h * (i + 1f) / n)
                p.lineTo(w * .04f, h)
                for (i in 0 until n) p.lineTo(w * (if (i % 2 == 0) .01f else .07f), h * (1f - (i + 1f) / n))
                p.close()
                c.drawPath(p, fill(withAlpha(col, .85f)))
                var x = -h; while (x < w) { c.drawLine(x, h, x + h, 0f, line(0x26FFFFFF, w * .015f)); x += w * .06f }
            }
            "paperclip" -> {
                val p = Path().apply {
                    moveTo(w * .5f, h * .22f); lineTo(w * .5f, h * .72f); cubicTo(w * .5f, h * .88f, w * .82f, h * .88f, w * .82f, h * .72f)
                    lineTo(w * .82f, h * .18f); cubicTo(w * .82f, h * 0f, w * .18f, h * 0f, w * .18f, h * .18f)
                    lineTo(w * .18f, h * .8f); cubicTo(w * .18f, h * .98f, w * .66f, h * .98f, w * .66f, h * .8f); lineTo(w * .66f, h * .35f)
                }
                c.save(); c.translate(w * .02f, h * .006f); c.drawPath(p, line(0x33000000, w * .07f)); c.restore()
                c.drawPath(p, line(col2, w * .075f)); c.drawPath(p, line(col, w * .04f))
            }
            "torn_paper" -> {
                val p = jagged(w * .02f, h * .02f, w * .98f, h * .96f, w * .02f, w * .045f, 11, top = false, left = false)
                c.drawPath(p, fill(col)); c.drawPath(p, line(withAlpha(col2, .6f), w * .006f))
                for (i in 1..6) c.drawLine(w * .1f, h * (.12f + i * .13f), w * .9f, h * (.12f + i * .13f), line(withAlpha(col2, .35f), w * .006f))
            }
            "stamp" -> {
                val p = Path().apply { fillType = Path.FillType.EVEN_ODD; addRect(0f, 0f, w, h, Path.Direction.CW)
                    val n = 12; for (i in 0..n) { val x = w * i / n; addCircle(x, 0f, w * .022f, Path.Direction.CCW); addCircle(x, h, w * .022f, Path.Direction.CCW) }
                    val m = 10; for (i in 0..m) { val y = h * i / m; addCircle(0f, y, w * .022f, Path.Direction.CCW); addCircle(w, y, w * .022f, Path.Direction.CCW) } }
                c.drawPath(p, fill(col))
                rr(c, w * .09f, h * .1f, w * .91f, h * .9f, w * .01f, line(col2, w * .012f))
                c.drawRect(w * .13f, h * .16f, w * .87f, h * .7f, fill(withAlpha(col2, .15f)))
                c.drawPath(Path().apply { moveTo(w * .13f, h * .7f); lineTo(w * .4f, h * .35f); lineTo(w * .58f, h * .55f); lineTo(w * .7f, h * .42f); lineTo(w * .87f, h * .7f); close() }, fill(withAlpha(col2, .55f)))
                c.drawCircle(w * .72f, h * .27f, w * .05f, fill(withAlpha(col2, .7f)))
                c.drawText("LALASI", w * .32f, h * .84f, txt(col2, h * .1f)); c.drawText("25", w * .78f, h * .84f, txt(col2, h * .12f))
            }
            "newspaper" -> {
                rr(c, 0f, 0f, w, h, w * .01f, fill(col)); rr(c, 0f, 0f, w, h, w * .01f, line(withAlpha(col2, .5f), w * .008f))
                c.drawText("THE DAILY BEAT", w / 2, h * .12f, txt(col2, h * .085f, true, Typeface.SERIF))
                c.drawLine(w * .06f, h * .16f, w * .94f, h * .16f, line(col2, w * .008f)); c.drawLine(w * .06f, h * .175f, w * .94f, h * .175f, line(col2, w * .003f))
                c.drawText("MUSIC ISSUE", w * .06f, h * .32f, txt(col2, h * .1f, true, Typeface.SERIF).apply { textAlign = Paint.Align.LEFT })
                c.drawRect(w * .06f, h * .38f, w * .42f, h * .66f, fill(withAlpha(col2, .35f)))
                for (cx in listOf(.46f, .72f)) for (i in 0 until 6) c.drawRect(w * cx, h * (.38f + i * .05f), w * (cx + .22f), h * (.4f + i * .05f), fill(withAlpha(col2, .6f)))
                for (i in 0 until 5) c.drawRect(w * .06f, h * (.7f + i * .045f), w * .94f, h * (.715f + i * .045f), fill(withAlpha(col2, .5f)))
            }
            "note" -> {
                c.drawPath(Path().apply { moveTo(0f, 0f); lineTo(w * .8f, 0f); lineTo(w, h * .2f); lineTo(w, h); lineTo(0f, h); close() }, fill(col))
                c.drawPath(Path().apply { moveTo(w * .8f, 0f); lineTo(w * .8f, h * .2f); lineTo(w, h * .2f); close() }, fill(withAlpha(col2, .25f)))
                for (i in 0 until 4) { val y = h * (.32f + i * .16f); val p = Path().apply { moveTo(w * .1f, y); for (k in 0 until 8) quadTo(w * (.14f + k * .1f), y - h * .05f * (if (k % 2 == 0) 1 else -1), w * (.19f + k * .1f), y) }
                    c.drawPath(p, line(withAlpha(col2, .85f), w * .014f)) }
            }
            "film_frame" -> {
                rr(c, 0f, 0f, w, h, w * .02f, fill(col))
                val holes = 6
                for (i in 0 until holes) { val x = w * (.08f + i * .15f); rr(c, x, h * .05f, x + w * .07f, h * .15f, w * .01f, fill(col2)); rr(c, x, h * .85f, x + w * .07f, h * .95f, w * .01f, fill(col2)) }
                c.drawRect(w * .06f, h * .21f, w * .94f, h * .79f, fill(0xFF5B6372.toInt()))
                c.drawPath(Path().apply { moveTo(w * .06f, h * .79f); lineTo(w * .35f, h * .5f); lineTo(w * .55f, h * .68f); lineTo(w * .7f, h * .56f); lineTo(w * .94f, h * .79f); close() }, fill(0x55FFFFFF))
            }
            "polaroid" -> {
                c.drawRect(0f, 0f, w, h, fill(col)); c.drawRect(0f, 0f, w, h, line(col2, w * .01f))
                val ph = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, h * .07f, 0f, h * .76f, 0xFF8FA6C4.toInt(), 0xFFE4C9A4.toInt(), Shader.TileMode.CLAMP) }
                c.drawRect(w * .07f, h * .07f, w * .93f, h * .77f, ph)
                c.drawCircle(w * .68f, h * .26f, w * .08f, fill(0xCCFFF3C4.toInt()))
                c.drawPath(Path().apply { moveTo(w * .07f, h * .77f); lineTo(w * .38f, h * .42f); lineTo(w * .6f, h * .64f); lineTo(w * .74f, h * .5f); lineTo(w * .93f, h * .77f); close() }, fill(0x66334455))
            }
            "scrap_label" -> {
                rr(c, 0f, 0f, w, h, h * .12f, fill(col))
                val d = line(col2, w * .012f).apply { pathEffect = DashPathEffect(floatArrayOf(w * .04f, w * .03f), 0f) }
                rr(c, w * .05f, h * .12f, w * .95f, h * .88f, h * .08f, d)
                c.drawText("FAVS", w / 2, h * .66f, txt(col2, h * .46f, true, Typeface.SERIF))
            }
            "paper_pieces" -> {
                val cols = listOf(col, col2, mixColor(col, 0xFFFFE97A.toInt(), .6f))
                val rots = listOf(-12f, 9f, -3f); val pos = listOf(.34f to .36f, .64f to .46f, .46f to .66f)
                for (i in 0..2) { c.save(); c.rotate(rots[i], w * pos[i].first, h * pos[i].second)
                    val l = w * (pos[i].first - .26f); val t = h * (pos[i].second - .2f)
                    c.drawRect(l, t, l + w * .5f, t + h * .4f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cols[i]; setShadowLayer(w * .02f, 0f, w * .01f, 0x44000000) }); c.restore() }
            }
            "collage_deco" -> {
                c.drawCircle(w * .32f, h * .34f, w * .24f, fill(col)); c.drawPath(Path().apply { moveTo(w * .5f, h * .9f); lineTo(w * .95f, h * .9f); lineTo(w * .72f, h * .45f); close() }, fill(col2))
                c.drawCircle(w * .78f, h * .22f, w * .09f, fill(0xFF111111.toInt())); c.drawArc(RectF(w * .05f, h * .55f, w * .45f, h * .95f), 180f, 180f, true, fill(0xFFFFE97A.toInt()))
                for (i in 0 until 4) c.drawRect(w * .55f, h * (.08f + i * .045f), w * .72f, h * (.1f + i * .045f), fill(0xFF111111.toInt()))
            }
            "vintage_badge" -> {
                c.drawCircle(w / 2, h / 2, w * .49f, fill(col)); c.drawCircle(w / 2, h / 2, w * .49f, line(col2, w * .03f)); c.drawCircle(w / 2, h / 2, w * .4f, line(col2, w * .012f))
                c.drawPath(starPath(w / 2, h * .44f, w * .17f, w * .07f, 5), fill(col2))
                c.drawText("VINTAGE", w / 2, h * .72f, txt(col2, w * .11f, true, Typeface.SERIF)); c.drawText("EST. 1999", w / 2, h * .83f, txt(col2, w * .055f, false, Typeface.SERIF))
            }
            "retro_sun" -> {
                val cols = listOf(col2, mixColor(col2, col, .5f), col, 0xFFE24A4A.toInt())
                for (i in cols.indices) { val r = w * (.5f - i * .1f); c.drawArc(RectF(w / 2 - r, h - r, w / 2 + r, h + r), 180f, 180f, true, fill(cols[i])) }
                for (i in 0 until 3) c.drawRect(0f, h * (.72f + i * .08f), w, h * (.75f + i * .08f), fill(0x00000000))
            }
            "smiley" -> {
                c.drawCircle(w / 2, h / 2, w * .49f, fill(col)); c.drawCircle(w / 2, h / 2, w * .49f, line(col2, w * .03f))
                c.drawOval(RectF(w * .3f, h * .3f, w * .4f, h * .46f), fill(col2)); c.drawOval(RectF(w * .6f, h * .3f, w * .7f, h * .46f), fill(col2))
                c.drawArc(RectF(w * .25f, h * .35f, w * .75f, h * .75f), 20f, 140f, false, line(col2, w * .04f))
                c.drawCircle(w * .22f, h * .58f, w * .06f, fill(0x66FF5C8A)); c.drawCircle(w * .78f, h * .58f, w * .06f, fill(0x66FF5C8A))
            }
            "y2k_star" -> {
                val p = sparklePath(w * .45f, h * .5f, w * .45f)
                val g = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, 0f, w, h, intArrayOf(0xFFFFFFFF.toInt(), col, col2, 0xFFFFFFFF.toInt()), null, Shader.TileMode.CLAMP) }
                c.drawPath(p, g); c.drawPath(p, line(0xFF4A4F8C.toInt(), w * .015f))
                c.drawPath(sparklePath(w * .85f, h * .2f, w * .12f), g); c.drawPath(sparklePath(w * .82f, h * .85f, w * .08f), g)
            }
            "grunge_patch" -> {
                val p = jagged(w * .03f, h * .03f, w * .97f, h * .97f, w * .018f, w * .05f, 3)
                c.drawPath(p, fill(col))
                val d = line(col2, w * .012f).apply { pathEffect = DashPathEffect(floatArrayOf(w * .04f, w * .03f), 0f) }
                c.drawRect(w * .1f, h * .1f, w * .9f, h * .9f, d)
                val x = line(col2, w * .03f); c.drawLine(w * .3f, h * .3f, w * .7f, h * .7f, x); c.drawLine(w * .7f, h * .3f, w * .3f, h * .7f, x)
            }
            "minimal_circle" -> {
                c.drawCircle(w * .4f, h * .5f, w * .3f, line(col, w * .015f)); c.drawCircle(w * .4f, h * .5f, w * .05f, fill(col))
                c.drawLine(w * .7f, h * .5f, w * .97f, h * .5f, line(col, w * .015f)); c.drawCircle(w * .97f, h * .5f, w * .02f, fill(col))
            }
            "soft_cloud" -> {
                val cl = Path().apply { addCircle(w * .28f, h * .6f, w * .2f, Path.Direction.CW); addCircle(w * .5f, h * .42f, w * .26f, Path.Direction.CW); addCircle(w * .74f, h * .58f, w * .2f, Path.Direction.CW); addRoundRect(RectF(w * .1f, h * .55f, w * .9f, h * .84f), h * .14f, h * .14f, Path.Direction.CW) }
                c.drawPath(cl, fill(col)); c.drawPath(cl, line(col2, w * .02f))
            }
            "editorial_tag" -> {
                c.drawRect(0f, 0f, w, h, fill(col)); c.drawText("NO.1", w / 2, h * .5f, txt(col2, w * .3f, true, Typeface.SERIF))
                c.drawLine(w * .12f, h * .6f, w * .88f, h * .6f, line(col2, w * .015f)); c.drawText("PLAYLIST", w / 2, h * .72f, txt(col2, w * .11f, false))
                c.drawCircle(w / 2, h * .09f, w * .04f, fill(col2))
            }
            "analog_camera" -> {
                rr(c, 0f, h * .22f, w, h, w * .06f, fill(col)); rr(c, w * .1f, h * .06f, w * .45f, h * .26f, w * .03f, fill(col)); rr(c, 0f, h * .22f, w, h, w * .06f, line(col2, w * .02f))
                c.drawRect(0f, h * .4f, w, h * .55f, fill(col2)); rr(c, w * .7f, h * .1f, w * .88f, h * .22f, w * .02f, fill(0xFFB8D4E8.toInt()))
                c.drawCircle(w * .5f, h * .6f, w * .27f, fill(col2)); c.drawCircle(w * .5f, h * .6f, w * .2f, fill(0xFF55555A.toInt())); c.drawCircle(w * .5f, h * .6f, w * .12f, fill(0xFF1A2330.toInt()))
                c.drawCircle(w * .45f, h * .56f, w * .035f, fill(0x88FFFFFF.toInt()))
            }
            "film_roll" -> {
                rr(c, w * .05f, h * .12f, w * .95f, h * .92f, w * .06f, fill(col)); c.drawRect(w * .38f, 0f, w * .62f, h * .14f, fill(0xFF9AA0AA.toInt()))
                c.drawRect(w * .05f, h * .12f, w * .95f, h * .2f, fill(0xFF55555A.toInt()))
                rr(c, w * .15f, h * .34f, w * .85f, h * .76f, w * .03f, fill(0xFFFFFFFF.toInt()))
                c.drawText("ISO 400", w / 2, h * .52f, txt(col2, w * .16f)); c.drawText("35MM 36 EXP", w / 2, h * .66f, txt(col2, w * .09f, false))
                c.drawRect(w * .15f, h * .72f, w * .85f, h * .76f, fill(0xFFE24A4A.toInt()))
            }
            else -> c.drawRect(0f, 0f, w, h, fill(col))
        }
    }

    // ------------------------------------------------------------------ frames (windows over the poster)

    fun drawFrame(c: Canvas, key: String, w: Float, h: Float) {
        when (key) {
            "polaroid" -> {
                val m = w * .055f; val b = h * .2f
                val p = Path().apply { fillType = Path.FillType.EVEN_ODD; addRect(0f, 0f, w, h, Path.Direction.CW); addRect(m, m, w - m, h - b, Path.Direction.CCW) }
                c.drawPath(p, fill(0xFFFAF8F2.toInt()))
                c.drawRect(m, m, w - m, h - b, line(0x33000000, w * .004f))
                c.drawText("my playlist", w / 2, h - b * .4f, txt(0xFF444444.toInt(), b * .32f, false, Typeface.SERIF))
            }
            "film_border" -> {
                val bar = h * .075f; val dk = fill(0xFF141416.toInt())
                c.drawRect(0f, 0f, w, bar, dk); c.drawRect(0f, h - bar, w, h, dk); c.drawRect(0f, 0f, w * .02f, h, dk); c.drawRect(w * .98f, 0f, w, h, dk)
                val s = bar * .38f; var x = s
                while (x < w - s) { rr(c, x, bar * .3f, x + s, bar * .7f, s * .2f, fill(0xFFEDE6D4.toInt())); rr(c, x, h - bar * .7f, x + s, h - bar * .3f, s * .2f, fill(0xFFEDE6D4.toInt())); x += s * 2.1f }
                val t = txt(0xFFF2A81D.toInt(), bar * .22f); t.textAlign = Paint.Align.LEFT
                var i = 0; var tx = w * .06f; while (tx < w - w * .12f) { c.drawText("${14 + i}  ${14 + i}A", tx, h - bar - h * .008f, t); tx += w * .22f; i++ }
            }
            "viewfinder" -> {
                val st = line(0xFFFFFFFF.toInt(), w * .006f); val m = w * .06f; val l = w * .1f
                for (sx in 0..1) for (sy in 0..1) {
                    val x = if (sx == 0) m else w - m; val y = if (sy == 0) m else h - m; val dx = if (sx == 0) l else -l; val dy = if (sy == 0) l else -l
                    c.drawLine(x, y, x + dx, y, st); c.drawLine(x, y, x, y + dy, st)
                }
                val th = line(0x44FFFFFF, w * .003f)
                for (i in 1..2) { c.drawLine(w * i / 3f, 0f, w * i / 3f, h, th); c.drawLine(0f, h * i / 3f, w, h * i / 3f, th) }
                c.drawLine(w / 2 - w * .03f, h / 2, w / 2 + w * .03f, h / 2, st); c.drawLine(w / 2, h / 2 - w * .03f, w / 2, h / 2 + w * .03f, st)
                c.drawRect(w / 2 - w * .09f, h / 2 - w * .09f, w / 2 + w * .09f, h / 2 + w * .09f, line(0xAAFFFFFF.toInt(), w * .004f))
                c.drawCircle(m + w * .02f, m + w * .06f, w * .014f, fill(0xFFE53935.toInt()))
                c.drawText("REC", m + w * .06f, m + w * .075f, txt(0xFFFFFFFF.toInt(), w * .04f).apply { textAlign = Paint.Align.LEFT })
                rr(c, w - m - w * .1f, m + w * .04f, w - m - w * .02f, m + w * .08f, w * .008f, line(0xFFFFFFFF.toInt(), w * .004f)); c.drawRect(w - m - w * .095f, m + w * .045f, w - m - w * .045f, m + w * .075f, fill(0xFFFFFFFF.toInt()))
                c.drawText("00:12:05", w / 2, h - m - w * .02f, txt(0xFFFFFFFF.toInt(), w * .04f, false))
            }
            "vintage_cam" -> {
                val v = Paint().apply { shader = RadialGradient(w / 2, h / 2, maxOf(w, h) * .75f, intArrayOf(0x00000000, 0x00000000, 0x77000000), floatArrayOf(0f, .55f, 1f), Shader.TileMode.CLAMP) }
                c.drawRect(0f, 0f, w, h, v)
                val d = txt(0xFFFF8A2B.toInt(), w * .05f, true, Typeface.MONOSPACE).apply { textAlign = Paint.Align.RIGHT; setShadowLayer(w * .01f, 0f, 0f, 0xAAFF5A00.toInt()) }
                c.drawText("'98  11  19", w - w * .07f, h - h * .05f, d)
                c.drawPath(Path().apply { moveTo(w * .07f, h * .045f); lineTo(w * .07f, h * .075f); lineTo(w * .095f, h * .06f); close() }, fill(0xFFFF8A2B.toInt()))
                c.drawText("PLAY", w * .115f, h * .071f, txt(0xFFFF8A2B.toInt(), w * .035f, true, Typeface.MONOSPACE).apply { textAlign = Paint.Align.LEFT })
            }
            "film_burn" -> {
                val l = Paint().apply { shader = LinearGradient(0f, 0f, w * .45f, 0f, intArrayOf(0xFFFF7A18.toInt(), 0x88FF3D00.toInt(), 0x00FF3D00), floatArrayOf(0f, .4f, 1f), Shader.TileMode.CLAMP) }
                val r = Paint().apply { shader = LinearGradient(w, 0f, w * .6f, 0f, intArrayOf(0xFFFFB347.toInt(), 0x88FF5A00.toInt(), 0x00FF5A00), floatArrayOf(0f, .35f, 1f), Shader.TileMode.CLAMP) }
                c.drawRect(0f, 0f, w, h, l); c.drawRect(0f, 0f, w, h, r)
                val t = Paint().apply { shader = LinearGradient(0f, 0f, 0f, h * .3f, 0xAAFFD27A.toInt(), 0x00FFD27A, Shader.TileMode.CLAMP) }
                c.drawRect(0f, 0f, w, h * .3f, t)
            }
            "torn_edges" -> {
                val m = w * .05f
                val inner = jagged(m, m, w - m, h - m, m * .5f, w * .035f, 21)
                val p = Path().apply { fillType = Path.FillType.EVEN_ODD; addRect(0f, 0f, w, h, Path.Direction.CW); addPath(inner) }
                c.drawPath(p, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF4EFE3.toInt(); setShadowLayer(w * .01f, 0f, 0f, 0x55000000) })
            }
            "news_border" -> {
                val s1 = line(0xFF151515.toInt(), w * .005f); val s2 = line(0xFF151515.toInt(), w * .002f); val m = w * .03f
                c.drawRect(0f, 0f, w, h * .06f, fill(0xFFE9E2D0.toInt())); c.drawRect(0f, h * .94f, w, h, fill(0xFFE9E2D0.toInt()))
                c.drawRect(m, m, w - m, h - m, s1); c.drawRect(m * 1.5f, m * 1.5f, w - m * 1.5f, h - m * 1.5f, s2)
                c.drawText("THE LALASI TIMES  -  VOL. 1  -  MUSIC EDITION", w / 2, h * .04f, txt(0xFF151515.toInt(), w * .028f, true, Typeface.SERIF))
                c.drawText("ALL THE SONGS FIT TO PRINT", w / 2, h * .975f, txt(0xFF151515.toInt(), w * .026f, false, Typeface.SERIF))
            }
            "scrap_frame" -> {
                val m = w * .05f
                val p = Path().apply { fillType = Path.FillType.EVEN_ODD; addRect(0f, 0f, w, h, Path.Direction.CW); addRect(m, m, w - m, h - m, Path.Direction.CCW) }
                c.drawPath(p, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF1EBDD.toInt(); setShadowLayer(w * .012f, 0f, w * .004f, 0x44000000) })
                for (sx in 0..1) for (sy in 0..1) {
                    c.save(); c.translate(if (sx == 0) m * .4f else w - m * .4f, if (sy == 0) m * .4f else h - m * .4f); c.rotate(if (sx == sy) -38f else 38f)
                    c.drawRect(-w * .06f, -w * .018f, w * .06f, w * .018f, fill(0xCCF3D98B.toInt())); c.restore()
                }
            }
            "contact_sheet" -> {
                val g = line(0x99FFFFFF.toInt(), w * .004f)
                val cols = 3; val rows = 4; val mx = w * .04f; val top = h * .05f; val bot = h * .05f
                for (i in 0..cols) c.drawLine(mx + (w - 2 * mx) * i / cols, top, mx + (w - 2 * mx) * i / cols, h - bot, g)
                for (j in 0..rows) c.drawLine(mx, top + (h - top - bot) * j / rows, w - mx, top + (h - top - bot) * j / rows, g)
                val t = txt(0xFFF2A81D.toInt(), w * .028f, true, Typeface.MONOSPACE).apply { textAlign = Paint.Align.LEFT }
                for (j in 0 until rows) for (i in 0 until cols) c.drawText("${j * cols + i + 1}A", mx + (w - 2 * mx) * i / cols + w * .01f, top + (h - top - bot) * (j + 1) / rows - h * .008f, t)
                c.drawText("LALASI 400  CONTACT SHEET", w / 2, h * .035f, txt(0xFFFFFFFF.toInt(), w * .03f, true, Typeface.MONOSPACE))
            }
            "deco_border" -> {
                val a = line(0xFFE8D7B8.toInt(), w * .006f); val b = line(0xFFE8D7B8.toInt(), w * .003f); val m = w * .04f; val m2 = w * .056f
                c.drawRect(m, m, w - m, h - m, a); c.drawRect(m2, m2, w - m2, h - m2, b)
                val r = w * .05f
                for (sx in 0..1) for (sy in 0..1) {
                    val x = if (sx == 0) m else w - m; val y = if (sy == 0) m else h - m
                    c.drawCircle(x, y, r, fill(0xFF1C1C1E.toInt())); c.drawCircle(x, y, r, a); c.drawCircle(x, y, r * .4f, fill(0xFFE8D7B8.toInt()))
                }
                for (p in listOf(w / 2 to m, w / 2 to h - m, m to h / 2, w - m to h / 2)) c.drawPath(Path().apply { moveTo(p.first, p.second - r * .5f); lineTo(p.first + r * .5f, p.second); lineTo(p.first, p.second + r * .5f); lineTo(p.first - r * .5f, p.second); close() }, fill(0xFFE8D7B8.toInt()))
            }
            "camera_details" -> {
                val t = txt(0xFFFFFFFF.toInt(), w * .034f, true, Typeface.MONOSPACE)
                t.textAlign = Paint.Align.LEFT; c.drawText("ISO 400", w * .05f, h * .05f, t)
                t.textAlign = Paint.Align.RIGHT; c.drawText("35mm  f/2.8", w * .95f, h * .05f, t)
                t.textAlign = Paint.Align.LEFT; c.drawText("24A", w * .05f, h * .965f, t)
                t.textAlign = Paint.Align.RIGHT; c.drawText("1/125", w * .95f, h * .965f, t)
                c.drawPath(Path().apply { moveTo(w * .46f, h * .95f); lineTo(w * .46f, h * .975f); lineTo(w * .485f, h * .9625f); close() }, fill(0xFFF2A81D.toInt()))
                val tk = line(0xAAFFFFFF.toInt(), w * .003f)
                for (i in 0..20) { val x = w * (.05f + .9f * i / 20f); c.drawLine(x, h * .01f, x, h * (if (i % 5 == 0) .025f else .018f), tk) }
            }
        }
    }

    // ------------------------------------------------------------------ pattern overlays

    fun drawPattern(c: Canvas, key: String, w: Float, h: Float, col: Int, col2: Int, scale: Float) {
        val s = (w * .06f * scale).coerceAtLeast(6f)
        when (key) {
            "pattern_dots" -> { var y = s / 2; var row = 0; while (y < h + s) { var x = if (row % 2 == 0) s / 2 else s; while (x < w + s) { c.drawCircle(x, y, s * .2f, fill(col)); x += s }; y += s * .9f; row++ } }
            "pattern_grid" -> { val p = line(col, maxOf(1f, s * .04f)); var x = 0f; while (x <= w) { c.drawLine(x, 0f, x, h, p); x += s }; var y = 0f; while (y <= h) { c.drawLine(0f, y, w, y, p); y += s } }
            "pattern_stripes" -> { val p = line(col, s * .25f); var x = -h; while (x < w) { c.drawLine(x, h, x + h, 0f, p); x += s * .9f } }
            "pattern_checker" -> { var j = 0; var y = 0f; while (y < h) { var i = 0; var x = 0f; while (x < w) { if ((i + j) % 2 == 0) c.drawRect(x, y, x + s, y + s, fill(col)) else c.drawRect(x, y, x + s, y + s, fill(col2)); x += s; i++ }; y += s; j++ } }
            "pattern_stars" -> { var y = s; var row = 0; while (y < h + s) { var x = if (row % 2 == 0) s else s * 1.6f; while (x < w + s) { c.drawPath(starPath(x, y, s * .26f, s * .11f, 5), fill(col)); x += s * 1.2f }; y += s * 1.1f; row++ } }
        }
    }
}
