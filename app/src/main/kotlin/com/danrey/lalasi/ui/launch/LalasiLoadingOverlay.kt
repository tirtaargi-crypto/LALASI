package com.danrey.lalasi.ui.launch

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.danrey.lalasi.R
import com.danrey.lalasi.ui.component.LalasiLogoCache
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/*
 * LALASI loading screen.
 *
 *  1. The logo stands exactly where the system splash (Android 12+: lalasi_splash_logo) draws it, so the
 *     hand-off between the two shows no step, no blink and never two logos.
 *  2. It stays still while Home is built behind it (nothing animates on a busy main thread, so nothing can stutter).
 *  3. Once the frames are quiet the word LALASI is written, stroke by stroke, by a pen: every stroke is one Path
 *     cut by distance with PathMeasure. Per frame that is a segment copy and a couple of draw calls.
 *  4. A short hold, then the opacity comes down: the logo and the word dissolve first, the black follows a beat
 *     later, so Home shows through without a flash. Every fade is per element (no full-screen layer, no blur).
 *
 * No gradients, no blur, no RenderEffect. The animation clocks are read only in draw / graphicsLayer lambdas, so
 * nothing recomposes while it plays. Composed only on a fresh launch (LaunchIntroState.plays).
 */

/** the logo is shown at least this long before the pen starts, counted from this screen's first frame */
private const val MinHoldMillis = 0L

// the word starts being written on the very first frame; ~1.1 s from first frame to Home when Home is quick
private const val WriteMillis = 640

// the finished word stays at least this long (a beat to read it), even when Home is already built
private const val MinHoldAfterWriteMillis = 160L
private const val FadeOutMillis = 240

/** how long the loading screen waits for Home to be built before it moves on anyway (a deadlock guard only) */
private const val HomeSettleMaxMillis = 900L
private const val MinSettleFrames = 4
private const val QuietFramesNeeded = 3

/** a frame later than this (a 60 Hz frame is 16.7 ms) means the main thread was busy */
private const val OnTimeFrameNanos = 24_000_000L

/** the system splash draws the logo at this share of the full-size composition: keep the two in step */
private const val SplashScale = 0.6f

// the splash vector puts the logo's centre 13.2 dp above the screen centre (pivot y 130.8 of 288); same here
private const val LogoLiftDp = 13.2f

private val WriteEase = CubicBezierEasing(0.45f, 0f, 0.25f, 1f)
private val FadeEase = CubicBezierEasing(0.33f, 0f, 0.2f, 1f)
private val AppearEase = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

// the word is laid out in units where a letter is 100 tall; this is its total width (the I is a bare stroke)
private const val WordUnitsWide = 468f

/** Pure black, the same as the system splash window (see colors.xml). */
private val LaunchBackground = Color(0xFF000000)

/** Suspends until the main thread is quiet: several frames in a row arrive on time. Never longer than [maxMillis]. */
private suspend fun awaitQuietFrames(maxMillis: Long) {
    withTimeoutOrNull(maxMillis) {
        var last = 0L
        var frames = 0
        var onTime = 0
        while (frames < MinSettleFrames || onTime < QuietFramesNeeded) {
            val now = withFrameNanos { it }
            if (last != 0L) {
                frames++
                if (now - last <= OnTimeFrameNanos) onTime++ else onTime = 0
            }
            last = now
        }
    }
}

/** One letter's pen: coordinates are in letter units (100 tall), placed at [ox] and scaled by [u]. */
private class Pen(private val x0: Float, private val u: Float, private val ox: Float) {
    val path = Path()
    fun m(x: Float, y: Float) = path.moveTo(x0 + (ox + x) * u, y * u)
    fun l(x: Float, y: Float) = path.lineTo(x0 + (ox + x) * u, y * u)
    fun c(a: Float, b: Float, c: Float, d: Float, e: Float, f: Float) =
        path.cubicTo(x0 + (ox + a) * u, b * u, x0 + (ox + c) * u, d * u, x0 + (ox + e) * u, f * u)
}

private inline fun stroke(x0: Float, u: Float, ox: Float, build: Pen.() -> Unit): Path =
    Pen(x0, u, ox).apply(build).path

/** L A L A S I as pen strokes, in the order a hand writes them; letter height 100 units * [u], starting at [x0]. */
private fun wordmarkStrokes(x0: Float, u: Float): List<Path> {
    val strokes = ArrayList<Path>(8)
    // letter origins, in units: L 0, A 84, L 190, A 274, S 380, I 468
    strokes += stroke(x0, u, 0f) { m(6f, 4f); l(6f, 96f); l(62f, 96f) }
    strokes += stroke(x0, u, 84f) { m(0f, 96f); l(42f, 4f); l(84f, 96f) }
    strokes += stroke(x0, u, 84f) { m(16f, 68f); l(68f, 68f) }
    strokes += stroke(x0, u, 190f) { m(6f, 4f); l(6f, 96f); l(62f, 96f) }
    strokes += stroke(x0, u, 274f) { m(0f, 96f); l(42f, 4f); l(84f, 96f) }
    strokes += stroke(x0, u, 274f) { m(16f, 68f); l(68f, 68f) }
    strokes += stroke(x0, u, 380f) {
        m(62f, 22f)
        c(54f, 6f, 10f, 2f, 8f, 28f)
        c(6f, 52f, 62f, 46f, 60f, 74f)
        c(58f, 98f, 14f, 98f, 2f, 78f)
    }
    strokes += stroke(x0, u, 468f) { m(0f, 4f); l(0f, 96f) }
    return strokes
}

@Composable
fun LalasiLoadingOverlay(state: LaunchIntroState) {
    if (!state.plays || state.done) return

    var finished by remember { mutableStateOf(false) }
    if (finished) return

    // the same PNG as R.drawable.lalasi_official_logo, decoded once per process (warmed in App.onCreate)
    val logo = LalasiLogoCache.load(LocalContext.current)

    // 0 -> 1 while the pen writes; 0 -> 1 while the opacity comes down
    val write = remember { Animatable(0f) }
    val exit = remember { Animatable(0f) }
    // Android 12+: the splash already shows the logo, so it is there from the first frame. Android 11 and below
    // start from a plain black window, so the logo eases in instead of popping.
    val appear = remember { Animatable(if (android.os.Build.VERSION.SDK_INT >= 31) 1f else 0f) }

    LaunchedEffect(state) {
        // two frames: the splash (which already shows the same logo) is only released once this screen has
        // really been drawn, so there is never a black frame between the two
        withFrameNanos { }
        LaunchGate.onOverlayDrawn()
        if (appear.value < 1f) launch { appear.animateTo(1f, tween(220, easing = AppearEase)) }

        // 1. The word is written straight away. Home is still its light skeleton at this point, so nothing
        //    heavy competes with the pen: every frame of the stroke is cheap and lands on time.
        state.rise.snapTo(1f)
        state.landed = true
        write.animateTo(1f, tween(WriteMillis, easing = WriteEase))

        // 2. Only now is Home built, behind the finished (standing still) word: a long frame here is
        //    a short wait on a still picture, never a stutter in a moving one.
        val builtAt = android.os.SystemClock.uptimeMillis()
        LaunchGate.releaseHome()
        awaitQuietFrames(HomeSettleMaxMillis)
        LaunchGate.onHomeReady()
        val held = android.os.SystemClock.uptimeMillis() - builtAt
        if (held < MinHoldAfterWriteMillis) delay(MinHoldAfterWriteMillis - held)

        // 3. Fade out over a Home that has stopped working.
        exit.animateTo(1f, tween(FadeOutMillis, easing = FadeEase))
        state.loadingReleased = true
        finished = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // the black trails the logo: it only starts to clear once the logo is already half gone
            .drawBehind {
                val e = ((exit.value - 0.35f) / 0.65f).coerceIn(0f, 1f)
                drawRect(LaunchBackground, alpha = 1f - e)
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // the logo: same size and place as the system splash's
        Image(
            painter = if (logo != null) BitmapPainter(logo) else painterResource(R.drawable.lalasi_official_logo),
            contentDescription = "LALASI",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .offset(y = -LogoLiftDp.dp)
                .size(width = 220.dp * SplashScale, height = 147.dp * SplashScale)
                .graphicsLayer {
                    val e = (exit.value / 0.7f).coerceIn(0f, 1f)
                    alpha = appear.value * (1f - e)
                },
        )
        // the word, written: every stroke is cut from one pen path by distance, and a small pen tip leads it.
        // Paths and measures are built once; a frame only moves the cut.
        Box(
            modifier = Modifier
                // just under the logo (its bottom edge is 30.8 dp below the centre)
                .offset(y = (-LogoLiftDp + 147f * SplashScale / 2f + 22f).dp)
                .size(width = 150.dp, height = 26.dp)
                .semantics { contentDescription = "LALASI" }
                .graphicsLayer { alpha = 1f - (exit.value / 0.7f).coerceIn(0f, 1f) }
                .drawWithCache {
                    val u = size.height / 100f
                    val strokes = wordmarkStrokes((size.width - WordUnitsWide * u) / 2f, u)
                    val measures = strokes.map { PathMeasure().apply { setPath(it, false) } }
                    val lengths = FloatArray(strokes.size) { measures[it].length }
                    val starts = FloatArray(strokes.size)
                    var total = 0f
                    for (i in strokes.indices) {
                        starts[i] = total
                        total += lengths[i]
                    }
                    val partial = Path()
                    val pen = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    val ink = Color.White.copy(alpha = 0.94f)
                    val tipHalo = 5.dp.toPx()
                    val tipDot = 1.8.dp.toPx()
                    onDrawBehind {
                        val p = write.value
                        if (p <= 0f) return@onDrawBehind
                        val d = p * total
                        var tip: Offset? = null
                        for (i in strokes.indices) {
                            val at = d - starts[i]
                            if (at >= lengths[i]) {
                                drawPath(strokes[i], ink, style = pen)
                            } else {
                                if (at > 0f) {
                                    partial.reset()
                                    measures[i].getSegment(0f, at, partial, true)
                                    drawPath(partial, ink, style = pen)
                                    tip = measures[i].getPosition(at)
                                }
                                break
                            }
                        }
                        if (tip != null && p < 1f) {
                            // two flat discs, no gradient: a soft halo and a bright dot
                            val fade = 1f - ((p - 0.94f) / 0.06f).coerceIn(0f, 1f)
                            drawCircle(Color.White, radius = tipHalo, center = tip, alpha = 0.16f * fade)
                            drawCircle(Color.White, radius = tipDot, center = tip, alpha = 0.9f * fade)
                        }
                    }
                },
        )
    }
}
