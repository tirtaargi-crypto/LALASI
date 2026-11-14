package com.danrey.lalasi.ui.launch

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.danrey.lalasi.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** Brand-first launch layer. Uses the full transparent LALASI wordmark, never the launcher icon. */
private const val ExitMillis = 360
private const val MaxHoldMillis = 8_000L
private val RevealEasing = CubicBezierEasing(0.18f, 0.82f, 0.22f, 1f)

@Composable
fun LalasiLoadingOverlay(state: LaunchIntroState) {
    if (!state.plays) return
    var finished by remember { mutableStateOf(false) }
    if (finished) return

    val reveal = remember { Animatable(0f) }
    val line = remember { Animatable(0f) }
    val exit = remember { Animatable(1f) }
    val progress = remember { Animatable(0.035f) }

    LaunchedEffect(state) {
        snapshotFlow {
            ((if (state.homeComposed) 0.42f else 0.10f) + 0.58f * state.flight.value)
                .coerceIn(0f, 1f)
        }.collectLatest { target ->
            progress.animateTo(target, tween(260, easing = LinearOutSlowInEasing))
        }
    }

    LaunchedEffect(Unit) {
        reveal.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = 190f))
        line.animateTo(1f, tween(420, easing = LinearEasing))
        withTimeoutOrNull(MaxHoldMillis) { snapshotFlow { state.done }.first { it } }
        progress.animateTo(1f, tween(180, easing = LinearEasing))
        exit.animateTo(0f, tween(ExitMillis, easing = RevealEasing))
        finished = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = exit.value }
            .drawBehind { drawRect(Color(0xFF050505)) }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // Fine circular hairlines and a progress arc add depth without expensive full-screen blur.
        Box(
            modifier = Modifier
                .size(286.dp)
                .graphicsLayer { alpha = reveal.value * 0.52f }
                .drawBehind {
                    val stroke = 0.7.dp.toPx()
                    val diameter = size.minDimension * 0.94f
                    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                    drawCircle(Color.White.copy(alpha = 0.055f), radius = diameter / 2f, style = Stroke(stroke))
                    drawArc(
                        color = Color.White.copy(alpha = 0.48f),
                        startAngle = -90f,
                        sweepAngle = 360f * progress.value,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = 1.15.dp.toPx()),
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.07f),
                        radius = diameter * 0.405f,
                        style = Stroke(stroke),
                    )
                },
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.lalasi_official_logo),
                contentDescription = "LALASI",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(width = 252.dp, height = 168.dp)
                    .graphicsLayer {
                        val t = reveal.value.coerceIn(0f, 1f)
                        alpha = (t * 1.35f).coerceAtMost(1f)
                        val scale = 0.84f + 0.16f * t
                        scaleX = scale
                        scaleY = scale
                        translationY = (1f - t) * 16.dp.toPx()
                    },
            )
            Spacer(Modifier.height(34.dp))
            Box(
                modifier = Modifier
                    .width(92.dp)
                    .height(1.5.dp)
                    .graphicsLayer { alpha = line.value }
                    .drawBehind {
                        drawRect(Color.White.copy(alpha = 0.12f))
                        drawRect(Color.White.copy(alpha = 0.88f), size = Size(size.width * progress.value, size.height))
                    },
            )
        }
    }
}
