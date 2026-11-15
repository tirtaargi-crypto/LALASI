package com.danrey.lalasi.ui.launch

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.danrey.lalasi.R
import kotlinx.coroutines.delay

/** Lightweight brand-only startup overlay. Keep drawing work limited to one cached image layer. */
private const val LogoEnterMillis = 420
private const val HoldMillis = 260L
private const val ExitMillis = 240
private val LogoEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

@Composable
fun LalasiLoadingOverlay(state: LaunchIntroState) {
    if (!state.plays) return
    var finished by remember { mutableStateOf(false) }
    if (finished) return

    val progress = remember { Animatable(0f) }
    val exit = remember { Animatable(1f) }

    LaunchedEffect(state) {
        progress.animateTo(1f, tween(LogoEnterMillis, easing = LogoEasing))
        delay(HoldMillis)
        // Release the launch transition only after the official LALASI wordmark is visible.
        state.loadingReleased = true
        exit.animateTo(0f, tween(ExitMillis, easing = LinearOutSlowInEasing))
        finished = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = exit.value }
            .drawBehind { drawRect(Color(0xFF050505)) }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // The official LALASI logo is the only mark shown on the loading screen.
        Image(
            painter = painterResource(R.drawable.lalasi_official_logo),
            contentDescription = "LALASI",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(width = 252.dp, height = 168.dp)
                .graphicsLayer {
                    val t = progress.value.coerceIn(0f, 1f)
                    alpha = t
                    val scale = 0.96f + 0.04f * t
                    scaleX = scale
                    scaleY = scale
                },
        )
    }
}
