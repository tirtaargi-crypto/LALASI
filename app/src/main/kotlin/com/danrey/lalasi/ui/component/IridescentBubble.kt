// IridescentBubble.kt

package com.danrey.lalasi.ui.component

import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.translate
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/*
 * The LALASI glass bubble: a rainbow soap-bubble made of real glass, not a blur or a gradient fill.
 *
 *  - thin-film interference: a sweep of Cyan / Electric Blue / Violet / Lavender / Magenta / Pearl / Icy Silver that
 *    turns slowly round the bubble, drawn as several bands of different width and phase, so the colours slide over
 *    each other the way a film of different thickness does
 *  - refraction: light bent round to the opposite side of the rim, plus the magnified icon under the bubble
 *  - chrome silver rim, glossy cap, a specular streak and a second small reflection that drift with the phase
 *  - the middle stays clear; all the colour sits on the edge, thicker toward the rim
 *
 * Performance: the two sweep shaders, the silver shader, the paints and the matrices are built once per bubble and only
 * their local matrix moves per frame. A frame allocates nothing. The phase is a plain float state read only in draw.
 */

private fun argb(v: Long): Int = v.toInt()

/** Spectral dispersion like light split by glass: blue, cyan, pearl, green, yellow, red, magenta, silver, and round to blue (seamless) */
private val IriColors = intArrayOf(
    argb(0xFF3D7BFF), // blue
    argb(0xFF22E0F0), // cyan
    argb(0xFFF4F8FF), // pearl
    argb(0xFF39E58C), // green
    argb(0xFFFFE066), // yellow
    argb(0xFFFF4D5E), // red
    argb(0xFFE05CFF), // magenta
    argb(0xFFCBD5E1), // icy silver
    argb(0xFF3D7BFF),
)

/** Muted spectrum for every rainbow rim: soft blue, cyan, mint, straw, rose, orchid and round to blue (closed, no seam) */
private val MutedRimColors = intArrayOf(
    argb(0xFF8FA8E8), // soft blue
    argb(0xFF7FCBE0), // soft cyan
    argb(0xFF93D9B0), // mint
    argb(0xFFE0D08A), // straw
    argb(0xFFE49A9A), // rose
    argb(0xFFCFA0E0), // orchid
    argb(0xFF8FA8E8), // = first colour: closed ring, no seam
)

private val Obsidian = Color(0xFF07080C)
private val Pearl = Color(0xFFF8F5FF)
private val IcySilver = Color(0xFFCBD5E1)

/** degrees per second the rainbow turns: slow, so it flows rather than spins */
private const val IriDegreesPerSecond = 22f

/** the phase is advanced at about this rate: the colour moves slowly, so 30 updates a second look the same as 60 */
private const val IriStepNanos = 32_000_000L

/** one phase for the whole app, so every glass surface carries the same colours in the same place at the same time */
private val SharedPhase = mutableFloatStateOf(0f)

/**
 * The turning rainbow's phase in degrees (0..360), shared by every glass surface. While any caller is [active] it is
 * advanced from the frame clock (quantised, so several callers write the very same value and nothing is redrawn twice).
 * Read it ONLY in a draw lambda (`phase.value`) so a change redraws and never recomposes anything.
 */
@Composable
fun rememberIridescencePhase(active: Boolean = true): State<Float> {
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        while (true) {
            val now = withFrameNanos { it }
            val step = now / IriStepNanos
            SharedPhase.floatValue = ((step * IriStepNanos) / 1_000_000_000.0 * IriDegreesPerSecond % 360.0).toFloat()
        }
    }
    return SharedPhase
}

/** the shared phase without driving it: for the small glass buttons, which turn whenever the nav bar's loop runs */
fun iridescencePhaseState(): State<Float> = SharedPhase

/** one colour stop of a soft rim gradient: [pos] 0..1 across the bubble, colour [rgb], opacity [alpha] */
private class RimStop(val pos: Float, val rgb: Int, val alpha: Float)

/** Everything a bubble needs that must not be rebuilt per frame. One per bar / player, `remember`ed. */
class IridescentBubble {
    private val sweepA = SweepGradient(0f, 0f, IriColors, null)
    private val sweepB = SweepGradient(0f, 0f, IriColors, null)
    private val mutedSweep = SweepGradient(0f, 0f, MutedRimColors, null)
    private val silver = LinearGradient(
        0f, 0f, 0f, 1f,
        intArrayOf(argb(0xFFFFFFFF), argb(0x88E8EEF6), argb(0x3DCBD5E1), argb(0xA6CBD5E1)),
        floatArrayOf(0f, 0.30f, 0.68f, 1f),
        Shader.TileMode.CLAMP,
    )
    private val matrix = Matrix()
    private val rect = RectF()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    /** a stroked rounded rect painted by one of the turning sweeps */
    private fun ring(
        canvas: android.graphics.Canvas,
        shader: Shader,
        cx: Float, cy: Float, degrees: Float,
        l: Float, t: Float, r: Float, b: Float,
        radius: Float, strokeWidth: Float, alpha: Float,
    ) {
        if (alpha <= 0.004f || r - l <= strokeWidth || b - t <= strokeWidth) return
        matrix.setRotate(degrees)
        matrix.postTranslate(cx, cy)
        shader.setLocalMatrix(matrix)
        paint.shader = shader
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        paint.alpha = (alpha * 255f).toInt().coerceIn(0, 255)
        val half = strokeWidth / 2f
        rect.set(l + half, t + half, r - half, b - half)
        val rr = (radius - half).coerceAtLeast(0f)
        canvas.drawRoundRect(rect, rr, rr, paint)
    }

    private fun silverRing(
        canvas: android.graphics.Canvas,
        t: Float, h: Float, l: Float, r: Float, b: Float,
        radius: Float, strokeWidth: Float, alpha: Float,
    ) {
        matrix.setScale(1f, h)
        matrix.postTranslate(0f, t)
        silver.setLocalMatrix(matrix)
        paint.shader = silver
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        paint.alpha = (alpha * 255f).toInt().coerceIn(0, 255)
        val half = strokeWidth / 2f
        rect.set(l + half, t + half, r - half, b - half)
        val rr = (radius - half).coerceAtLeast(0f)
        canvas.drawRoundRect(rect, rr, rr, paint)
    }

    private fun wash(
        canvas: android.graphics.Canvas,
        cx: Float, cy: Float, degrees: Float,
        l: Float, t: Float, r: Float, b: Float, radius: Float, alpha: Float,
    ) {
        matrix.setRotate(degrees)
        matrix.postTranslate(cx, cy)
        sweepB.setLocalMatrix(matrix)
        paint.shader = sweepB
        paint.style = Paint.Style.FILL
        paint.alpha = (alpha * 255f).toInt().coerceIn(0, 255)
        rect.set(l, t, r, b)
        canvas.drawRoundRect(rect, radius, radius, paint)
    }

    private fun arc(
        canvas: android.graphics.Canvas,
        shader: Shader,
        cx: Float, cy: Float, degrees: Float,
        l: Float, t: Float, r: Float, b: Float,
        start: Float, sweep: Float, strokeWidth: Float, alpha: Float,
    ) {
        matrix.setRotate(degrees)
        matrix.postTranslate(cx, cy)
        shader.setLocalMatrix(matrix)
        paint.shader = shader
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        paint.strokeCap = Paint.Cap.ROUND
        paint.alpha = (alpha * 255f).toInt().coerceIn(0, 255)
        rect.set(l, t, r, b)
        canvas.drawArc(rect, start, sweep, false, paint)
        paint.strokeCap = Paint.Cap.BUTT
    }

    private val rrPath = Path()
    private val shapePath = Path()

    private fun withAlpha(rgb: Int, a: Float): Int = ((a * 255f).toInt().coerceIn(0, 255) shl 24) or (rgb and 0x00FFFFFF)

    // variant bit 0: the bubble's left end is the bar's end (colour keeps going round it); bit 1: the same on the right
    private fun fadeShader(rgb: IntArray, positions: FloatArray, variant: Int): LinearGradient {
        val endL = if (variant and 1 != 0) 1f else 0f
        val endR = if (variant and 2 != 0) 1f else 0f
        val colors = IntArray(rgb.size) { withAlpha(rgb[it], 1f) }
        colors[0] = withAlpha(rgb[0], endL)
        colors[colors.size - 1] = withAlpha(rgb[rgb.size - 1], endR)
        return LinearGradient(0f, 0f, 1f, 0f, colors, positions, Shader.TileMode.CLAMP)
    }

    // the reference's lens edge: a green ring on the outside, inside it red on the left and blue on the right
    private val greenRing = Array(4) { fadeShader(intArrayOf(argb(0xFF3FE05A), argb(0xFF3FE05A), argb(0xFF3FE05A), argb(0xFF3FE05A)), floatArrayOf(0f, 0.22f, 0.78f, 1f), it) }
    private val redBlueRing = Array(4) {
        fadeShader(
            intArrayOf(argb(0xFFE53935), argb(0xFFE53935), argb(0x00882BC8), argb(0xFF2B4BFF), argb(0xFF2B4BFF)),
            floatArrayOf(0f, 0.30f, 0.50f, 0.70f, 1f),
            it,
        )
    }

    private fun band(
        c: android.graphics.Canvas, path: android.graphics.Path, shader: Shader?, color: Int,
        l: Float, t: Float, r: Float, b: Float, hh: Float, visible: Float, alpha: Float,
    ) {
        val w = (r - l).coerceAtLeast(1f)
        if (shader != null) {
            matrix.setScale(w, 1f)
            matrix.postTranslate(l, 0f)
            shader.setLocalMatrix(matrix)
            paint.shader = shader
        } else {
            paint.shader = null
            paint.color = color
        }
        paint.strokeWidth = visible * 2f
        paint.alpha = (alpha * 255f).toInt().coerceIn(0, 255)
        c.save()
        c.clipRect(l - 6f, t - 6f, r + 6f, t + hh)
        c.drawPath(path, paint)
        c.restore()
        c.save()
        c.clipRect(l - 6f, b - hh, r + 6f, b + 6f)
        c.drawPath(path, paint)
        c.restore()
    }

    private val rimTopCStops = arrayOf(
        RimStop(0.00f, argb(0xFFE0883A), 0.00f),
        RimStop(0.10f, argb(0xFFE0883A), 0.50f),
        RimStop(0.30f, argb(0xFFE0883A), 0.38f),
        RimStop(0.45f, argb(0xFFE0883A), 0.00f),
        RimStop(0.55f, argb(0xFF3AC8D8), 0.00f),
        RimStop(0.72f, argb(0xFF3AC8D8), 0.38f),
        RimStop(0.90f, argb(0xFF3AC8D8), 0.50f),
        RimStop(1.00f, argb(0xFF3AC8D8), 0.00f),
    )
    private val rimBotCStops = arrayOf(
        RimStop(0.00f, argb(0xFF3AC8D8), 0.00f),
        RimStop(0.10f, argb(0xFF3AC8D8), 0.50f),
        RimStop(0.30f, argb(0xFF3AC8D8), 0.38f),
        RimStop(0.45f, argb(0xFF3AC8D8), 0.00f),
        RimStop(0.55f, argb(0xFFE68C40), 0.00f),
        RimStop(0.72f, argb(0xFFE68C40), 0.38f),
        RimStop(0.90f, argb(0xFFE68C40), 0.50f),
        RimStop(1.00f, argb(0xFFE68C40), 0.00f),
    )
    private val rimTopBStops = arrayOf(
        RimStop(0.00f, argb(0xFFE02028), 0.00f),
        RimStop(0.08f, argb(0xFFE02028), 0.69f),
        RimStop(0.20f, argb(0xFFE02028), 0.78f),
        RimStop(0.30f, argb(0xFFE02028), 0.62f),
        RimStop(0.45f, argb(0xFFE02028), 0.00f),
        RimStop(0.55f, argb(0xFF2028F0), 0.00f),
        RimStop(0.70f, argb(0xFF2028F0), 0.62f),
        RimStop(0.82f, argb(0xFF2028F0), 0.78f),
        RimStop(0.95f, argb(0xFF2028F0), 0.62f),
        RimStop(1.00f, argb(0xFF2028F0), 0.25f),
    )
    private val rimBotBStops = arrayOf(
        RimStop(0.00f, argb(0xFF2028F0), 0.25f),
        RimStop(0.05f, argb(0xFF2028F0), 0.62f),
        RimStop(0.18f, argb(0xFF2028F0), 0.78f),
        RimStop(0.30f, argb(0xFF2028F0), 0.62f),
        RimStop(0.45f, argb(0xFF2028F0), 0.00f),
        RimStop(0.55f, argb(0xFFE02028), 0.00f),
        RimStop(0.70f, argb(0xFFE02028), 0.62f),
        RimStop(0.82f, argb(0xFFE02028), 0.78f),
        RimStop(0.92f, argb(0xFFE02028), 0.69f),
        RimStop(1.00f, argb(0xFFE02028), 0.00f),
    )
    private val rimTopAStops = arrayOf(
        RimStop(0.00f, argb(0xFFB0B228), 1.00f),
        RimStop(0.10f, argb(0xFF5AD02E), 1.00f),
        RimStop(0.22f, argb(0xFF3CE05A), 0.78f),
        RimStop(0.40f, argb(0xFF3CE05A), 0.00f),
        RimStop(0.60f, argb(0xFF22D890), 0.00f),
        RimStop(0.78f, argb(0xFF22D890), 0.78f),
        RimStop(0.90f, argb(0xFF22D890), 1.00f),
        RimStop(1.00f, argb(0xFF22A8B0), 1.00f),
    )
    private val rimBotAStops = arrayOf(
        RimStop(0.00f, argb(0xFF22A8B0), 1.00f),
        RimStop(0.10f, argb(0xFF22D890), 1.00f),
        RimStop(0.22f, argb(0xFF3CE05A), 0.78f),
        RimStop(0.40f, argb(0xFF3CE05A), 0.00f),
        RimStop(0.60f, argb(0xFF5AD02E), 0.00f),
        RimStop(0.78f, argb(0xFF5AD02E), 0.78f),
        RimStop(0.90f, argb(0xFF5AD02E), 1.00f),
        RimStop(1.00f, argb(0xFFE68C40), 1.00f),
    )

    private fun rimShader(stops: Array<RimStop>): LinearGradient {
        val colors = IntArray(stops.size) { withAlpha(stops[it].rgb, stops[it].alpha) }
        val pos = FloatArray(stops.size) { stops[it].pos }
        return LinearGradient(0f, 0f, 1f, 0f, colors, pos, Shader.TileMode.CLAMP)
    }

    // top edge: red at the left, blue at the right; bottom edge is the mirror (blue left, red right), like a real lens.
    // the outer ring (A) runs olive/green to teal, the fringe (C) is orange to cyan. The centre of the bubble stays clear.
    private val rimTopC = rimShader(rimTopCStops)
    private val rimTopB = rimShader(rimTopBStops)
    private val rimTopA = rimShader(rimTopAStops)
    private val rimBotC = rimShader(rimBotCStops)
    private val rimBotB = rimShader(rimBotBStops)
    private val rimBotA = rimShader(rimBotAStops)

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dstIn = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN)
    private val maskCache = HashMap<Int, LinearGradient>()

    /** vertical fade mask: opaque on the top and bottom edge, a cosine fall-off to nothing at [hf] of the height */
    private fun maskFor(hf: Float): LinearGradient {
        val key = (hf * 50f).toInt()
        maskCache[key]?.let { return it }
        if (maskCache.size > 32) maskCache.clear()
        val q = key / 50f
        val n = 7
        val colors = IntArray(n * 2)
        val pos = FloatArray(n * 2)
        for (i in 0 until n) {
            val k = i / (n - 1).toFloat()
            val a = 0.5f * (1f + kotlin.math.cos((PI * k).toFloat()))
            val c = ((a * 255f).toInt().coerceIn(0, 255) shl 24) or 0x00FFFFFF
            colors[i] = c
            pos[i] = k * q
            colors[n * 2 - 1 - i] = c
            pos[n * 2 - 1 - i] = 1f - k * q
        }
        return LinearGradient(0f, 0f, 0f, 1f, colors, pos, Shader.TileMode.CLAMP).also { maskCache[key] = it }
    }

    /** horizontal ramp 0 -> 1 (smooth), used to keep the end-of-bar highlight on its own end only */
    private val endRamp = LinearGradient(
        0f, 0f, 1f, 0f,
        intArrayOf(0x00FFFFFF, 0x0AFFFFFF, 0x33FFFFFF, 0x8CFFFFFF.toInt(), 0xE0FFFFFF.toInt(), 0xFFFFFFFF.toInt()),
        floatArrayOf(0f, 0.2f, 0.4f, 0.6f, 0.8f, 1f),
        Shader.TileMode.CLAMP,
    )

    /** a few stacked strokes of shrinking width: the inner edge of the band fades out instead of stopping hard */
    private fun rimPasses(
        c: android.graphics.Canvas, path: android.graphics.Path, v: Float, n: Int, each: Float, alpha: Float,
    ) {
        for (i in 0 until n) {
            paint.strokeWidth = v * (1f - i * (0.72f / n)) * 2f
            paint.alpha = (each * alpha * 255f).toInt().coerceIn(0, 255)
            c.drawPath(path, paint)
        }
    }

    private fun rimShaderPasses(
        c: android.graphics.Canvas, path: android.graphics.Path, shader: Shader,
        l: Float, r: Float, v: Float, n: Int, each: Float, alpha: Float,
    ) {
        matrix.setScale((r - l).coerceAtLeast(1f), 1f)
        matrix.postTranslate(l, 0f)
        shader.setLocalMatrix(matrix)
        paint.shader = shader
        rimPasses(c, path, v, n, each, alpha)
    }

    private fun rimHalf(
        c: android.graphics.Canvas, path: android.graphics.Path, l: Float, r: Float, hh: Float, alpha: Float,
        sC: Shader, sB: Shader, sA: Shader,
    ) {
        paint.style = Paint.Style.STROKE
        // soft dark core under the colour, so the colour reads as light bent through glass
        paint.shader = null
        paint.color = argb(0xFF000000)
        rimPasses(c, path, hh, 4, 0.376f, alpha)
        rimShaderPasses(c, path, sC, l, r, hh * 0.68f, 4, 0.405f, alpha)
        rimShaderPasses(c, path, sB, l, r, hh * 0.55f, 4, 0.58f, alpha)
        rimShaderPasses(c, path, sA, l, r, hh * 0.17f, 3, 0.78f, alpha)
    }

    /**
     * One layer of the soft chromatic edge, [hf] = how deep (share of the height) the colour reaches. With [endSide]
     * -1 / +1 the layer is also cut to its own end of the bar by a horizontal ramp, so it is ONE highlight, not two.
     */
    private fun rimLayer(
        c: android.graphics.Canvas, path: android.graphics.Path,
        l: Float, t: Float, r: Float, b: Float, hfIn: Float, px: Float, alpha: Float,
        endSide: Int, layerAlpha: Float,
    ) {
        val h = b - t
        val hf = ((hfIn.coerceIn(0.06f, 0.46f)) * 50f).toInt() / 50f
        val hh = h * hf
        val pad = 4f * px
        val layer = if (layerAlpha >= 0.995f) {
            c.saveLayer(l - pad, t - pad, r + pad, b + pad, null)
        } else {
            c.saveLayerAlpha(l - pad, t - pad, r + pad, b + pad, (layerAlpha * 255f).toInt().coerceIn(0, 255))
        }
        val mid = (t + b) / 2f
        c.save()
        c.clipPath(path)
        c.save()
        c.clipRect(l - pad, t - pad, r + pad, mid)
        rimHalf(c, path, l, r, hh, alpha, rimTopC, rimTopB, rimTopA)
        c.restore()
        c.save()
        c.clipRect(l - pad, mid, r + pad, b + pad)
        rimHalf(c, path, l, r, hh, alpha, rimBotC, rimBotB, rimBotA)
        c.restore()
        c.restore()
        fillPaint.xfermode = dstIn
        val m = maskFor(hf)
        matrix.setScale(1f, h)
        matrix.postTranslate(0f, t)
        m.setLocalMatrix(matrix)
        fillPaint.shader = m
        c.drawRect(l - pad, t - pad, r + pad, b + pad, fillPaint)
        if (endSide != 0) {
            val ew = (h * 0.75f).coerceAtMost((r - l) * 0.6f)
            if (endSide > 0) {
                matrix.setScale(ew, 1f)
                matrix.postTranslate(r - ew, 0f)
            } else {
                matrix.setScale(-ew, 1f)
                matrix.postTranslate(l + ew, 0f)
            }
            endRamp.setLocalMatrix(matrix)
            fillPaint.shader = endRamp
            c.drawRect(l - pad, t - pad, r + pad, b + pad, fillPaint)
        }
        fillPaint.xfermode = null
        fillPaint.shader = null
        c.restoreToCount(layer)
    }

    /**
     * The soft chromatic lens edge: colour only on the top and bottom edge, strongest near the two ends and clear in the
     * middle; top and bottom are mirrored (red-left/blue-right above, blue-left/red-right below). Every band is feathered
     * and fades out smoothly with depth, so nothing ends in a hard line. [frac] is how deep the colour reaches in the
     * middle of the bar. [endL] / [endR] (0..1) is how close the bubble is to that end of the bar: one extra highlight
     * grows on THAT end only, reaching down the bar's curve like a C (left) or a reversed C (right); it fades in and out
     * smoothly as the bubble moves, so it is a highlight that follows the end, not a shape that snaps on.
     */
    private fun rainbowEdge(
        c: android.graphics.Canvas, path: android.graphics.Path,
        l: Float, t: Float, r: Float, b: Float, frac: Float, px: Float, alpha: Float,
        endL: Float, endR: Float,
    ) {
        if (b - t <= 2f || r - l <= 2f || alpha <= 0.004f) return
        rimLayer(c, path, l, t, r, b, frac, px, alpha, 0, 1f)
        if (endL > 0.02f) rimLayer(c, path, l, t, r, b, frac + (0.46f - frac) * endL, px, alpha, -1, endL)
        if (endR > 0.02f) rimLayer(c, path, l, t, r, b, frac + (0.46f - frac) * endR, px, alpha, 1, endR)
    }

    private fun silverPath(
        c: android.graphics.Canvas, path: android.graphics.Path, t: Float, h: Float, strokeWidth: Float, alpha: Float,
    ) {
        matrix.setScale(1f, h)
        matrix.postTranslate(0f, t)
        silver.setLocalMatrix(matrix)
        paint.shader = silver
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        paint.alpha = (alpha * 255f).toInt().coerceIn(0, 255)
        c.drawPath(path, paint)
    }

    /**
     * The whole bubble inside [topLeft]/[size]. [intensity] 0..1 fades it in and out; [phase] is
     * [rememberIridescencePhase]; [resting] makes it a touch calmer (the lit tab's indicator).
     * [bounds] is the bar's own outline: the bubble is cut to it, so near the bar's ends its top and bottom edge (and the
     * colour on them) bend down round the bar's curve instead of poking out straight.
     */
    internal fun draw(
        scope: DrawScope, topLeft: Offset, size: Size, phase: Float, intensity: Float,
        resting: Boolean = false, bounds: Path? = null, endLeft: Boolean = false, endRight: Boolean = false,
        endLeftAmt: Float = if (endLeft) 1f else 0f, endRightAmt: Float = if (endRight) 1f else 0f,
    ) {
        val k = intensity.coerceIn(0f, 1f)
        if (k <= 0.004f || size.width <= 4f || size.height <= 4f) return
        with(scope) {
            val rad = (phase * PI / 180.0).toFloat()
            // the bubble breathes a little: elastic, never quite the same shape twice
            val breatheX = 1f + 0.010f * sin(rad * 3f)
            val breatheY = 1f - 0.008f * (0.5f + 0.5f * sin(rad * 4f + 0.7f))
            val w = size.width * breatheX
            val h = size.height * breatheY
            val l = topLeft.x + (size.width - w) / 2f
            val t = topLeft.y + (size.height - h) / 2f
            val r = l + w
            val b = t + h
            val radius = h / 2f
            val calm = if (resting) 0.78f else 1f
            val px = 1.dp.toPx()

            rrPath.reset()
            // the reference's ends are true half circles (corner radius = half the height); measured on both reference
            // photos the outline is a pill, not a wider-than-tall oval
            val rx = minOf(radius, w / 2f)
            rrPath.addRoundRect(RoundRect(l, t, r, b, rx, radius))
            val shape: Path = if (bounds != null && shapePath.op(rrPath, bounds, PathOperation.Intersect)) shapePath else rrPath
            val sh = shape.asAndroidPath()

            // clear glass: a barely-there body, the middle stays see-through
            drawPath(shape, Color.White.copy(alpha = 0.15f * k * calm))

            drawIntoCanvas { canvas ->
                val c = canvas.nativeCanvas
                rainbowEdge(c, sh, l, t, r, b, 0.18f, px, k * calm, endLeftAmt, endRightAmt)
                // chrome silver edge: bright on top, dim through the middle, brighter again at the bottom lip
                silverPath(c, sh, t, h, 1.6f * px, 0.95f * k)
            }
        }
    }

    /**
     * The navbar bubble's look on any outline (Home's press bubble): frosted translucent body, the lens edge (dark core,
     * green ring outside, red-to-blue ring inside) on the top and bottom edge that runs on round both ends, and the thin
     * chrome edge. [path] is the bubble's outline in the scope's coordinates, [l]/[t]/[r]/[b] its box, [intensity] 0..1,
     * [thin] scales the colour bands (small buttons less than cards). One instance per surface: its shaders keep their
     * matrix, so two bubbles never repaint each other's colours.
     */
    internal fun drawLens(
        scope: DrawScope, path: Path, l: Float, t: Float, r: Float, b: Float,
        intensity: Float, thin: Float, bodyAlpha: Float,
    ) {
        val k = intensity.coerceIn(0f, 1f)
        if (k <= 0.004f || r - l <= 8f || b - t <= 8f) return
        with(scope) {
            val px = 1.dp.toPx() * thin
            // the colour runs down the ends for about the same length on every bubble, tall card or small chip
            val wrap = (26.dp.toPx() / (b - t)).coerceIn(0.12f, 0.45f)
            val sh = path.asAndroidPath()
            drawPath(path, Color.White.copy(alpha = bodyAlpha * k))
            drawIntoCanvas { canvas ->
                val c = canvas.nativeCanvas
                rainbowEdge(c, sh, l, t, r, b, wrap, px, k, 0f, 0f)
                silverPath(c, sh, t, b - t, 1.4.dp.toPx(), 0.95f * k)
            }
        }
    }

    /**
     * One closed 360-degree rim: a single muted SweepGradient (first colour = last colour, no seam) mapped onto the shape's
     * own width and height, so the colour follows the real perimeter of a pill, circle or card alike. The stroke is drawn
     * at twice [widthPx] under a clip of the shape, so only the inside half shows and no corner is ever cut off.
     * [glow] (0..1) adds a faint wider film under it. Every rainbow rim in the app goes through here.
     */
    private fun ring360(
        scope: DrawScope, l: Float, t: Float, r: Float, b: Float, radius: Float,
        degrees: Float, widthPx: Float, alpha: Float, glow: Float,
    ) {
        val w = r - l
        val h = b - t
        if (alpha <= 0.004f || w <= 4f || h <= 4f) return
        val rr = radius.coerceIn(0f, minOf(w, h) / 2f)
        rrPath.reset()
        rrPath.addRoundRect(RoundRect(l, t, r, b, rr, rr))
        val native = rrPath.asAndroidPath()
        matrix.setRotate(degrees)
        matrix.postScale(w / 2f, h / 2f)
        matrix.postTranslate((l + r) / 2f, (t + b) / 2f)
        mutedSweep.setLocalMatrix(matrix)
        paint.shader = mutedSweep
        paint.style = Paint.Style.STROKE
        with(scope) {
            drawIntoCanvas { canvas ->
                val c = canvas.nativeCanvas
                c.save()
                c.clipPath(native)
                if (glow > 0f) {
                    paint.strokeWidth = widthPx * 5f
                    paint.alpha = (alpha * glow * 255f).toInt().coerceIn(0, 255)
                    c.drawPath(native, paint)
                }
                paint.strokeWidth = widthPx * 2f
                paint.alpha = (alpha * 255f).toInt().coerceIn(0, 255)
                c.drawPath(native, paint)
                c.restore()
            }
        }
    }

    /** A thin, crisp 360-degree rim for small glass buttons and chips (same muted spectrum, same phase as every other rim). */
    internal fun drawEdge(scope: DrawScope, topLeft: Offset, size: Size, cornerRadius: Float, alpha: Float, thin: Float = 0.5f) {
        val k = alpha.coerceIn(0f, 1f)
        if (k <= 0.004f || size.width <= 8f || size.height <= 8f) return
        val px = with(scope) { 1.dp.toPx() }
        ring360(
            scope, topLeft.x, topLeft.y, topLeft.x + size.width, topLeft.y + size.height, cornerRadius,
            SharedPhase.value, px * (0.8f + thin), 0.38f * k, 0f,
        )
    }

    /**
     * The glass film round a rounded rect (nav bar at rest, mini player, glass buttons): ONE closed 360-degree ring,
     * evenly lit all the way round with no gap, soft muted colours, turning slowly with [phase]. [alpha] fades it.
     */
    internal fun drawRim(scope: DrawScope, topLeft: Offset, size: Size, cornerRadius: Float, phase: Float, alpha: Float) {
        val k = alpha.coerceIn(0f, 1f)
        if (k <= 0.004f || size.width <= 4f || size.height <= 4f) return
        val px = with(scope) { 1.dp.toPx() }
        ring360(
            scope, topLeft.x, topLeft.y, topLeft.x + size.width, topLeft.y + size.height, cornerRadius,
            phase, 1.5f * px, 0.8f * k, 0.18f,
        )
    }
}

@Composable
fun rememberIridescentBubble(): IridescentBubble = remember { IridescentBubble() }

/** public entry points, so other files need not touch the class's internals */
fun DrawScope.drawIridescentBubble(
    bubble: IridescentBubble, topLeft: Offset, size: Size, phase: Float, intensity: Float = 1f, resting: Boolean = false,
    bounds: Path? = null, endLeft: Boolean = false, endRight: Boolean = false,
    endLeftAmt: Float = if (endLeft) 1f else 0f, endRightAmt: Float = if (endRight) 1f else 0f,
) = bubble.draw(this, topLeft, size, phase, intensity, resting, bounds, endLeft, endRight, endLeftAmt, endRightAmt)

fun DrawScope.drawIridescentEdge(
    bubble: IridescentBubble, topLeft: Offset, size: Size, cornerRadius: Float, alpha: Float = 1f, thin: Float = 0.5f,
) = bubble.drawEdge(this, topLeft, size, cornerRadius, alpha, thin)

fun DrawScope.drawIridescentRim(
    bubble: IridescentBubble, topLeft: Offset, size: Size, cornerRadius: Float, phase: Float, alpha: Float = 1f,
) = bubble.drawRim(this, topLeft, size, cornerRadius, phase, alpha)

/**
 * The rainbow rim on top of whatever this modifier is on: for the glass buttons and pills that are not the nav bar,
 * so they all carry the same film. [cornerFraction] is the corner radius as a share of the height (0.5 = a pill).
 */
fun Modifier.iridescentRim(cornerFraction: Float = 0.5f, alpha: Float = 1f): Modifier = composed {
    val bubble = rememberIridescentBubble()
    val phase = iridescencePhaseState()
    drawWithContent {
        drawContent()
        val radius = minOf(size.width, size.height) * cornerFraction.coerceIn(0f, 0.5f) * (if (cornerFraction >= 0.5f) 1f else 2f)
        drawIridescentRim(bubble, Offset.Zero, size, radius, phase.value, alpha * 0.8f)
        drawIridescentEdge(bubble, Offset.Zero, size, radius, alpha * 0.9f)
    }
}


/* ------------------------------------------------------------------------------------------------
 * Liquid glass rim (Home): ONE closed outline, ONE seamless sweep.
 *
 * Why the old rim looked cut up: it painted two separate bands on the top and bottom edge only (each clipped to a
 * strip) and a sweep of saturated blue / cyan / green / yellow / red. The sides had nothing, so the colour read as
 * isolated streaks, and the neon palette read as an RGB border.
 *
 * Here the whole shape outline is stroked in a single draw call, so there are no segments and no joins to leave gaps.
 * The colour comes from a SweepGradient whose first and last colour are identical (closed ring) and whose local matrix
 * maps the shape's own width/height onto the unit circle, so the angle follows the actual perimeter of a pill, a
 * circle or a card alike (a plain sweep on a wide pill would bunch every colour into the short sides).
 * The stroke is drawn at twice the width under a clip of the shape itself: only the INSIDE half shows, so the rim can
 * never be cut off by the component's own bounds, and corners stay complete at any radius.
 * ---------------------------------------------------------------------------------------------- */

/** frosted white / smoky silver with three very pale tints (ice, peach, mint); alpha carries most of the variation */
private val GlassRimColors = intArrayOf(
    argb(0x73D6DCE8), // right: silver
    argb(0x66C6D3F0), // bottom-right: ice
    argb(0x40E4E7EE), // bottom: dimmest
    argb(0x5CECD9D4), // bottom-left: peach
    argb(0x8CEFF1F6), // left: frosted
    argb(0xF2FFFFFF), // top-left: the light (specular)
    argb(0xA6E2E6F0), // top: silver
    argb(0x52CDE4DA), // top-right: mint
    argb(0x73D6DCE8), // = first colour: closed ring, no seam
)
private val GlassRimStops = floatArrayOf(0f, 0.125f, 0.25f, 0.375f, 0.5f, 0.625f, 0.75f, 0.875f, 1f)

/** the angle (degrees, 0 = 3 o'clock, clockwise) at which [GlassRimColors] is brightest; the light is turned from here */
internal const val GlassRimLightAngle = 225f

private val CalmGlassFill = Color(0xFF0B0D12).copy(alpha = 0.46f)

/** a shape's outline as a Path, moved by [offset]; the one place Home turns a Shape into geometry */
internal fun outlineToPath(outline: Outline, offset: Offset = Offset.Zero): Path = Path().apply {
    when (outline) {
        is Outline.Rectangle -> addRect(outline.rect.translate(offset))
        is Outline.Rounded -> addRoundRect(outline.roundRect.translate(offset))
        is Outline.Generic -> addPath(outline.path, offset)
    }
}

/**
 * Paints the glass rim of one surface. One instance per surface, built lazily (nothing is allocated for a card that is
 * never touched). The shader's matrix only changes when the size or the light direction changes, never per frame, so a
 * recorded frame can never be repainted with another frame's matrix.
 */
internal class GlassRimPainter {
    private var sweep: SweepGradient? = null
    private val matrix = Matrix()
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = argb(0xFFFFFFFF)
    }
    private var keyCx = Float.NaN
    private var keyCy = Float.NaN
    private var keyW = Float.NaN
    private var keyH = Float.NaN
    private var keyRot = Float.NaN

    /**
     * [path] is the closed outline in the scope's coordinates; [cx]/[cy]/[w]/[h] describe the box it fills;
     * [rimWidthPx] is the visible (inner) width; [lightDegrees] turns the bright spot round the rim; [alpha] 0..1.
     */
    fun draw(
        scope: DrawScope, path: Path,
        cx: Float, cy: Float, w: Float, h: Float,
        rimWidthPx: Float, lightDegrees: Float, alpha: Float,
    ) {
        val a = alpha.coerceIn(0f, 1f)
        if (a <= 0.01f || w <= 2f || h <= 2f) return
        val shader = sweep ?: SweepGradient(0f, 0f, GlassRimColors, GlassRimStops).also { sweep = it }
        if (cx != keyCx || cy != keyCy || w != keyW || h != keyH || lightDegrees != keyRot) {
            // shader space is the unit circle: turn the light, stretch it to the shape, move it to the shape's centre
            matrix.setRotate(lightDegrees)
            matrix.postScale(w / 2f, h / 2f)
            matrix.postTranslate(cx, cy)
            shader.setLocalMatrix(matrix)
            rimPaint.shader = shader
            keyCx = cx; keyCy = cy; keyW = w; keyH = h; keyRot = lightDegrees
        }
        val native = path.asAndroidPath()
        scope.drawIntoCanvas { canvas ->
            val c = canvas.nativeCanvas
            c.save()
            c.clipPath(native)
            // soft inner falloff, so the edge reads as light bent into the glass rather than a drawn line
            linePaint.strokeWidth = rimWidthPx * 7f
            linePaint.alpha = (a * 0.07f * 255f).toInt().coerceIn(0, 255)
            c.drawPath(native, linePaint)
            // the film: coloured by the sweep, inside half only (stroke is doubled and clipped)
            rimPaint.strokeWidth = rimWidthPx * 2f
            rimPaint.alpha = (a * 255f).toInt().coerceIn(0, 255)
            c.drawPath(native, rimPaint)
            // crisp hairline right on the edge
            linePaint.strokeWidth = rimWidthPx * 0.9f
            linePaint.alpha = (a * 0.20f * 255f).toInt().coerceIn(0, 255)
            c.drawPath(native, linePaint)
            c.restore()
        }
    }
}

/**
 * A calm, static glass surface for Home's small interactive pieces (chips, the playing row, round buttons): smoky
 * translucent body, a faint sheen, and the closed silver rim. Draws once per size, never animates, so it costs nothing
 * while idle. Not used by the nav bar, the mini player or other screens.
 */
fun Modifier.calmGlassSurface(shape: Shape, rimAlpha: Float = 0.55f, rimWidth: Dp = 1.2.dp): Modifier =
    this.clip(shape).drawWithCache {
        val path = outlineToPath(shape.createOutline(size, layoutDirection, this))
        val painter = GlassRimPainter()
        val rimPx = rimWidth.toPx()
        val sheen = Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.08f),
            0.5f to Color.White.copy(alpha = 0.02f),
            1f to Color.White.copy(alpha = 0.05f),
        )
        onDrawWithContent {
            drawPath(path, CalmGlassFill)
            drawPath(path, sheen)
            drawContent()
            painter.draw(this, path, size.width / 2f, size.height / 2f, size.width, size.height, rimPx, 0f, rimAlpha)
        }
    }

/** a one-pixel-ish silver edge on artwork and tiles: bright on the light side, nearly gone opposite; static */
fun Modifier.glassHairline(shape: Shape, width: Dp = 0.8.dp, alpha: Float = 1f): Modifier =
    this.drawWithCache {
        val path = outlineToPath(shape.createOutline(size, layoutDirection, this))
        val strokePx = width.toPx() * 2f
        val brush = Brush.linearGradient(
            0f to Color.White.copy(alpha = 0.30f),
            0.45f to Color.White.copy(alpha = 0.04f),
            0.75f to Color.White.copy(alpha = 0.03f),
            1f to Color.White.copy(alpha = 0.14f),
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        )
        onDrawWithContent {
            drawContent()
            // doubled stroke under the shape's own clip: the edge is complete at every corner
            clipPath(path) { drawPath(path, brush, alpha = alpha, style = Stroke(strokePx)) }
        }
    }
