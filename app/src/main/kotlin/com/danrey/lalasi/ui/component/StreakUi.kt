// StreakUi.kt

package com.danrey.lalasi.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import com.danrey.lalasi.ui.theme.lalasiAdapt
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.danrey.lalasi.R
import com.danrey.lalasi.ui.theme.LalasiInk
import com.danrey.lalasi.utils.StreakManager
import com.danrey.lalasi.utils.StreakState
import java.time.LocalDate
import java.time.format.TextStyle as DayTextStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

private val FlameOrange = Color(0xFFFF7A1A)
private val FlameYellow = Color(0xFFFFC53D)
private val SoftEase = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/**
 * The streak flame. Lit = the vector flame, gently breathing (a scale on a graphics layer, read in the
 * layer block only, so it never recomposes). Unlit = the grey flame, still.
 */
/** how the flame looks at a given streak: it changes on every odd day and grows richer the longer it runs */
internal class FlameLook(val hue: Float, val scale: Float, val embers: Int, val sideFlames: Boolean, val halo: Boolean, val name: String)

internal fun flameLook(days: Int): FlameLook {
    val odd = ((days + 1) / 2).coerceIn(0, 60)           // 1 for days 1-2, 2 for days 3-4 ...: a new step on every odd day
    val hue = -odd * 2.3f                                // orange slides through red and magenta towards violet-blue
    val scale = when {
        days >= 100 -> 1.46f
        days >= 60 -> 1.36f
        days >= 30 -> 1.24f
        days >= 10 -> 1.12f
        else -> 1f
    }
    val embers = when { days >= 60 -> 9; days >= 30 -> 7; days >= 10 -> 6; else -> 4 }
    val name = when {
        days >= 100 -> "Api legendaris"
        days >= 60 -> "Api langka"
        days >= 30 -> "Api berubah warna"
        days >= 10 -> "Api membesar"
        days >= 3 -> "Api stabil"
        days >= 1 -> "Baru menyala"
        else -> "Belum menyala"
    }
    return FlameLook(hue, scale, embers, sideFlames = days >= 30, halo = days >= 60, name = name)
}

/** the name of the flame tier, for the streak page */
fun flameLookName(days: Int): String = flameLook(days).name

private fun hueShift(c: Color, deg: Float): Color {
    if (deg == 0f) return c
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(c.toArgb(), hsv)
    hsv[0] = ((hsv[0] + deg) % 360f + 360f) % 360f
    return Color(android.graphics.Color.HSVToColor(hsv)).copy(alpha = c.alpha)
}

/** a hue rotation as a colour matrix, so the flame artwork itself changes colour (not just its glow) */
private fun hueFilter(deg: Float): androidx.compose.ui.graphics.ColorFilter? {
    if (deg == 0f) return null
    val r = Math.toRadians(deg.toDouble())
    val c = cos(r).toFloat()
    val sn = sin(r).toFloat()
    val lr = 0.213f; val lg = 0.715f; val lb = 0.072f
    val m = floatArrayOf(
        lr + c * (1 - lr) + sn * (-lr), lg + c * (-lg) + sn * (-lg), lb + c * (-lb) + sn * (1 - lb), 0f, 0f,
        lr + c * (-lr) + sn * 0.143f, lg + c * (1 - lg) + sn * 0.140f, lb + c * (-lb) + sn * (-0.283f), 0f, 0f,
        lr + c * (-lr) + sn * (-(1 - lr)), lg + c * (-lg) + sn * lg, lb + c * (1 - lb) + sn * lb, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )
    return androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix(m))
}

@Composable
fun StreakFlame(lit: Boolean, modifier: Modifier = Modifier, days: Int = 0, tiered: Boolean = false) {
    if (!lit) {
        Image(
            painter = painterResource(R.drawable.ic_streak_flame_off),
            contentDescription = null,
            modifier = modifier,
        )
        return
    }
    val look = remember(days) { flameLook(days) }
    val filter = remember(look.hue) { hueFilter(look.hue) }
    val orange = remember(look.hue) { hueShift(FlameOrange, look.hue) }
    val yellow = remember(look.hue) { hueShift(FlameYellow, look.hue * 0.6f) }
    val transition = rememberInfiniteTransition(label = "flame")
    // two clocks: a quick uneven flicker (the flame itself) and a slow one that lifts the embers
    val flick = transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(760, easing = androidx.compose.animation.core.FastOutSlowInEasing), RepeatMode.Reverse),
        label = "flameFlick",
    )
    val slow = transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "flameSlow",
    )
    val grow = if (tiered) look.scale else 1f
    Box(
        modifier.graphicsLayer {
            scaleX = grow
            scaleY = grow
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.94f)
        },
    ) {
        // the light the flame throws: a wide bloom that spills sideways (onto the number next to it) and a hot
        // core at its foot, both breathing with the flicker. Drawn, not blurred, so it costs almost nothing.
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val pulse = 0.82f + 0.18f * flick.value
            val bloomC = Offset(w * 0.60f, h * 0.60f)
            val bloomR = h * 1.9f * pulse
            drawCircle(
                Brush.radialGradient(
                    0f to orange.copy(alpha = 0.46f * pulse),
                    0.45f to orange.copy(alpha = 0.14f),
                    1f to Color.Transparent,
                    center = bloomC, radius = bloomR,
                ),
                radius = bloomR, center = bloomC,
            )
            val coreC = Offset(w * 0.5f, h * 0.72f)
            val coreR = h * 0.6f * pulse
            drawCircle(
                Brush.radialGradient(listOf(yellow.copy(alpha = 0.55f * pulse), Color.Transparent), center = coreC, radius = coreR),
                radius = coreR, center = coreC,
            )
            if (look.halo) {
                // a thin ring of light turning slowly round the flame (60 days and up)
                val c = Offset(w * 0.5f, h * 0.55f)
                rotate(slow.value * 360f, c) {
                    drawCircle(
                        Brush.sweepGradient(listOf(Color.Transparent, yellow.copy(alpha = 0.85f), Color.Transparent, Color.Transparent), center = c),
                        radius = h * 0.66f, center = c, style = Stroke(h * 0.018f, cap = StrokeCap.Round),
                    )
                }
            }
        }
        if (tiered && look.sideFlames) {
            // 30 days and up: two small flames beside the big one
            for (side in intArrayOf(-1, 1)) {
                Image(
                    painter = painterResource(R.drawable.ic_streak_flame),
                    contentDescription = null,
                    colorFilter = hueFilter(look.hue - 25f),
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        val e = if (side < 0) flick.value else 1f - flick.value
                        scaleX = 0.5f
                        scaleY = 0.5f + 0.06f * e
                        translationX = side * size.width * 0.40f
                        rotationZ = side * (14f + 3f * e)
                        alpha = 0.9f
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.94f)
                    },
                )
            }
        }
        Image(
            painter = painterResource(R.drawable.ic_streak_flame),
            contentDescription = null,
            colorFilter = filter,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                val e = flick.value
                // tall and thin, then short and wide, with a slight lean: a flame never holds still
                scaleY = 1f + 0.075f * e
                scaleX = 1f - 0.04f * e
                rotationZ = (e - 0.5f) * 3.2f
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.94f)
            },
        )
        // sparks rising off the tip and fading out
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val n = look.embers
            for (i in 0 until n) {
                val t = (slow.value + i / n.toFloat()) % 1f
                val x = w * (0.5f + 0.20f * sin(t * 2f * PI.toFloat() * 1.5f + i * 1.7f))
                val y = h * (0.74f - 0.90f * t)
                val fadeIn = if (t < 0.12f) t / 0.12f else 1f
                drawCircle(
                    hueShift(Color(0xFFFFD98A), look.hue * 0.5f).copy(alpha = (1f - t) * fadeIn * 0.95f),
                    radius = h * 0.032f * (1f - 0.55f * t) * (0.8f + 0.4f * ((i % 3) / 2f)),
                    center = Offset(x, y),
                )
            }
        }
    }
}

/**
 * Home's streak: one rounded bar. The flame lights the left end of it (its colour follows the flame's tier), then the
 * number with "hari", a thin line toward the next milestone, and on the right a ring for today (a check once today is done).
 * Tap it and it opens the streak page; the three dots (or a long press) open the Recap. Calm glass with the silver rim,
 * like the Home chips; nothing in it animates except the flame itself, so it costs nothing while idle.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun StreakCard(
    state: State<StreakState>,
    modifier: Modifier = Modifier,
    scrollProgress: Float = 0f,
    onOpen: () -> Unit,
    onOpenRecap: () -> Unit,
    onOpenRecapYear: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenDetail: () -> Unit,
) {
    val s = state.value
    var menu by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(24.dp)
    val accent = remember(s.current) {
        val hue = ((25f + flameLook(s.current).hue) % 360f + 360f) % 360f
        Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(hue, 0.95f, 0.56f)))
    }
    val lit = s.doneToday
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = 1f - scrollProgress.coerceIn(0f, 1f) * 0.22f }
            .height(72.dp)
            .clip(shape)
            .background(com.danrey.lalasi.ui.theme.lalasiAdapt(0xFF1C1C1E))
            .drawBehind {
                // the flame's light on the left end of the bar
                drawRect(
                    Brush.radialGradient(
                        0f to accent.copy(alpha = if (lit) 0.30f else 0.12f),
                        1f to Color.Transparent,
                        center = Offset(size.height * 0.62f, size.height * 0.5f),
                        radius = size.width * 0.55f,
                    ),
                )
            }
            .calmGlassSurface(shape)
            .combinedClickable(onClick = onOpen, onLongClick = { menu = true })
            .padding(start = 14.dp, end = 4.dp),
    ) {
        StreakFlame(lit = lit, modifier = Modifier.size(40.dp), days = s.current)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            // number and word in ONE text, so they share a baseline
            Text(
                text = androidx.compose.ui.text.buildAnnotatedString {
                    withStyle(androidx.compose.ui.text.SpanStyle(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.8).sp, color = LalasiInk)) { append(s.current.toString()) }
                    withStyle(androidx.compose.ui.text.SpanStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = LalasiInk.copy(alpha = 0.62f))) { append("  hari beruntun") }
                },
                lineHeight = 32.sp,
                maxLines = 1,
            )
            Spacer(Modifier.height(4.dp))
            MilestoneBar(s)
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).clickable { menu = true },
        ) {
            val dot = LalasiInk.copy(alpha = 0.6f)
            Canvas(Modifier.size(width = 18.dp, height = 4.dp)) {
                val r = size.height / 2f
                for (i in 0..2) drawCircle(dot, r, Offset(r + i * (size.width - 2 * r) / 2f, r))
            }
            androidx.compose.material3.DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                // one entry: the Recap page is the listening-stats page (month / year buttons inside it)
                androidx.compose.material3.DropdownMenuItem(text = { Text("Recap") }, onClick = { menu = false; onOpenStats() })
            }
        }
    }
}

/** how far along the way to the next milestone (3, 10, 30, 100 ...) */
@Composable
private fun MilestoneBar(s: StreakState) {
    val next = StreakManager.nextMilestone(s.current).coerceAtLeast(1)
    val progress = (s.current / next.toFloat()).coerceIn(0f, 1f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(LalasiInk.copy(alpha = 0.10f))
                .drawBehind {
                    if (progress > 0f) {
                        drawRoundRect(
                            Brush.horizontalGradient(listOf(FlameYellow, FlameOrange)),
                            size = Size(size.width * progress, size.height),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f),
                        )
                    }
                },
        )
        Spacer(Modifier.width(10.dp))
        Text(text = "${(next - s.current).coerceAtLeast(0)} hari lagi", color = LalasiInk.copy(alpha = 0.55f), fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/**
 * The milestone celebration (3, 10, 30, 100, then every 100 days). One progress value drives everything
 * and is read only in draw / graphics-layer blocks, so the whole animation recomposes nothing:
 * a glow blooms, the flame pops in on a spring, sparks fly out, then the text rises.
 */
@Composable
fun StreakCelebration(days: Int, onDismiss: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val burst = remember { Animatable(0f) }   // sparks + glow, 0 -> 1
    val pop = remember { Animatable(0f) }     // flame scale, spring
    val textIn = remember { Animatable(0f) }  // text rise, 0 -> 1
    val screenIn = remember { Animatable(0f) }

    LaunchedEffect(days) {
        screenIn.animateTo(1f, tween(220, easing = SoftEase))
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        // all three start together from the same frame; they only differ in timing
        kotlinx.coroutines.coroutineScope {
            launch { pop.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessLow)) }
            launch { burst.animateTo(1f, tween(1100, easing = SoftEase)) }
            launch {
                kotlinx.coroutines.delay(260)
                textIn.animateTo(1f, tween(520, easing = SoftEase))
            }
        }
    }

    val title = stringResource(R.string.streak_celebrate_title, days)
    val message = stringResource(
        when {
            days >= 100 && days != 100 -> R.string.streak_celebrate_many
            days == 100 -> R.string.streak_celebrate_100
            days == 30 -> R.string.streak_celebrate_30
            days == 10 -> R.string.streak_celebrate_10
            else -> R.string.streak_celebrate_3
        },
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = screenIn.value }
                .background(Color.Black.copy(alpha = 0.86f))
                .clickable(interactionSource = null, indication = null, onClick = onDismiss)
                .semantics { contentDescription = title },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 36.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(260.dp)) {
                    // glow + sparks: one canvas, geometry and brushes made once
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithCache {
                                val glow = Brush.radialGradient(
                                    listOf(FlameOrange.copy(alpha = 0.55f), Color.Transparent),
                                    center = Offset(size.width / 2f, size.height / 2f),
                                    radius = size.minDimension / 2f,
                                )
                                val count = 18
                                val angles = FloatArray(count) { i -> (i * 2f * PI.toFloat() / count) + (if (i % 2 == 0) 0.07f else -0.05f) }
                                val reach = FloatArray(count) { i -> if (i % 3 == 0) 1f else if (i % 3 == 1) 0.78f else 0.9f }
                                val dot = 4.dp.toPx()
                                onDrawBehind {
                                    val t = burst.value
                                    if (t <= 0f) return@onDrawBehind
                                    // the glow swells then settles low
                                    val glowAlpha = (if (t < 0.35f) t / 0.35f else 1f - 0.45f * ((t - 0.35f) / 0.65f)).coerceIn(0f, 1f)
                                    drawCircle(glow, radius = size.minDimension / 2f * (0.7f + 0.3f * t), alpha = glowAlpha)
                                    val fly = 1f - (1f - t) * (1f - t) * (1f - t)
                                    val fade = (1f - ((t - 0.45f) / 0.55f)).coerceIn(0f, 1f)
                                    if (fade > 0f) {
                                        val base = size.minDimension * 0.30f
                                        val span = size.minDimension * 0.20f
                                        for (i in 0 until count) {
                                            val d = base + span * reach[i] * fly
                                            val c = Offset(center.x + cos(angles[i]) * d, center.y + sin(angles[i]) * d)
                                            drawCircle(
                                                color = if (i % 2 == 0) FlameYellow else FlameOrange,
                                                radius = dot * (1f - 0.55f * t) * (if (i % 3 == 0) 1.2f else 0.85f),
                                                center = c,
                                                alpha = fade,
                                            )
                                        }
                                    }
                                }
                            },
                    )
                    Image(
                        painter = painterResource(R.drawable.ic_streak_flame),
                        contentDescription = null,
                        modifier = Modifier
                            .size(150.dp)
                            .graphicsLayer {
                                val p = pop.value
                                scaleX = p
                                scaleY = p
                                alpha = (p * 3f).coerceIn(0f, 1f)
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.9f)
                            },
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer {
                        val e = textIn.value
                        alpha = e
                        translationY = (1f - e) * 24.dp.toPx()
                    },
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = message,
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(28.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .height(52.dp)
                            .fillMaxWidth(0.7f)
                            .clip(RoundedCornerShape(26.dp))
                            .background(Brush.horizontalGradient(listOf(FlameYellow, FlameOrange)))
                            .clickable(onClick = onDismiss),
                    ) {
                        Text(
                            text = stringResource(R.string.streak_continue),
                            color = Color(0xFF2A1200),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
