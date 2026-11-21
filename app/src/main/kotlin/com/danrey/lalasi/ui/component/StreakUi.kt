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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.danrey.lalasi.R
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
private val CardFill = Color.White.copy(alpha = 0.08f)
private val SoftEase = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/**
 * The streak flame. Lit = the vector flame, gently breathing (a scale on a graphics layer, read in the
 * layer block only, so it never recomposes). Unlit = the grey flame, still.
 */
@Composable
fun StreakFlame(lit: Boolean, modifier: Modifier = Modifier) {
    if (!lit) {
        Image(
            painter = painterResource(R.drawable.ic_streak_flame_off),
            contentDescription = null,
            modifier = modifier,
        )
        return
    }
    val transition = rememberInfiniteTransition(label = "flame")
    val breathe = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Reverse),
        label = "flameBreathe",
    )
    Image(
        painter = painterResource(R.drawable.ic_streak_flame),
        contentDescription = null,
        modifier = modifier.graphicsLayer {
            val e = breathe.value
            val s = 1f + 0.05f * e
            scaleX = s
            scaleY = s
            // the flame grows from its base, like a real one
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.92f)
        },
    )
}

/**
 * Home's streak card: the flame, the number, one short line, and the last seven days.
 * Tapping it does nothing heavy; it exists to be glanced at.
 */
@Composable
fun StreakCard(state: State<StreakState>, modifier: Modifier = Modifier, scrollProgress: Float = 0f, onClick: (() -> Unit)? = null) {
    val s = state.value
    val title = when {
        s.current > 0 -> stringResource(R.string.streak_days, s.current)
        else -> stringResource(R.string.streak_none_title)
    }
    val subtitle = when {
        s.current == 0 -> stringResource(R.string.streak_none_hint)
        !s.doneToday -> stringResource(R.string.streak_keep_alive)
        else -> stringResource(R.string.streak_next, s.daysToNext, StreakManager.nextMilestone(s.current))
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = 1f - scrollProgress.coerceIn(0f, 1f) * 0.22f }
            .blur((scrollProgress.coerceIn(0f, 1f) * 5f).dp)
            .lalasiGlassSurface(RoundedCornerShape(22.dp), cornerRadiusFraction = 0.24f)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StreakFlame(lit = s.doneToday, modifier = Modifier.size(52.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    maxLines = 2,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        StreakWeekRow(s)
    }
}

/** the last seven days ending today: lit flame-coloured dots for days that counted, a progress ring for today */
@Composable
private fun StreakWeekRow(s: StreakState) {
    val locale = Locale.getDefault()
    val labels = remember(s.today, locale) {
        (6 downTo 0).map { back ->
            LocalDate.ofEpochDay(s.today - back).dayOfWeek.getDisplayName(DayTextStyle.NARROW, locale)
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        for (i in 0 until 7) {
            val day = s.today - (6 - i)
            val counted = day in s.listenedDays
            val isToday = i == 6
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = labels[i],
                    color = Color.White.copy(alpha = if (isToday) 0.9f else 0.45f),
                    fontSize = 11.sp,
                    fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                )
                Spacer(Modifier.height(5.dp))
                val progress = if (isToday && !counted) s.todayProgress else 0f
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .drawBehind {
                            val r = size.minDimension / 2f
                            when {
                                counted -> drawCircle(
                                    brush = Brush.verticalGradient(listOf(FlameYellow, FlameOrange)),
                                    radius = r,
                                )
                                else -> {
                                    drawCircle(Color.White.copy(alpha = 0.10f), radius = r)
                                    if (progress > 0f) {
                                        val w = 2.5.dp.toPx()
                                        drawArc(
                                            color = FlameOrange,
                                            startAngle = -90f,
                                            sweepAngle = 360f * progress,
                                            useCenter = false,
                                            topLeft = Offset(w / 2f, w / 2f),
                                            size = Size(size.width - w, size.height - w),
                                            style = Stroke(width = w, cap = StrokeCap.Round),
                                        )
                                    }
                                }
                            }
                        },
                )
            }
        }
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
