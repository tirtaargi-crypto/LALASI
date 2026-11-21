package com.danrey.lalasi.ui.screens.update

import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.danrey.lalasi.ui.component.BlurEffectCache
import com.danrey.lalasi.ui.component.damageBoundary
import com.danrey.lalasi.ui.component.rememberElementBlurAvailable

private const val DiscScrollZoom = 0.42f
private const val DiscMaxBlurRadius = 28f

@RequiresApi(Build.VERSION_CODES.S)
private fun discBlurEffect(radius: Float): RenderEffect? =
    BlurEffectCache.get(radius, Shader.TileMode.DECAL)

/**
 * A record turning behind the app icon: a black disc with fine grooves (a few wider gaps between "tracks"),
 * and two soft reflections sweeping round it the way light slides over vinyl. The grooves are drawn once;
 * only the reflection layer rotates. As the sheet scrolls it zooms, blurs and fades away like the old ring did.
 * The icon tile in front of it acts as the record's label.
 */
@Composable
fun AboutVinyl(
    modifier: Modifier = Modifier,
    scrollProgress: () -> Float = { 0f },
    radius: Dp = 124.dp,
    turnMillis: Int = 14_000,
) {
    val canBlur = rememberElementBlurAvailable()
    val latestProgress = rememberUpdatedState(scrollProgress)
    val spinning by remember { derivedStateOf { latestProgress.value() < 0.99f } }
    val turn = if (spinning) {
        rememberInfiniteTransition(label = "aboutVinyl").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(turnMillis, easing = LinearEasing), RepeatMode.Restart),
            label = "aboutVinylTurn",
        ).value
    } else {
        0f
    }

    Box(
        modifier
            .size(radius * 2)
            .graphicsLayer {
                val p = latestProgress.value().coerceIn(0f, 1f)
                val eased = p * p * (3f - 2f * p)
                val scale = 1f + DiscScrollZoom * eased
                scaleX = scale
                scaleY = scale
                translationY = -p * 72.dp.toPx()
                val fade = 1f - eased
                alpha = fade
                renderEffect = if (canBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && p > 0.005f && fade > 0.01f) {
                    discBlurEffect(DiscMaxBlurRadius * eased)
                } else {
                    null
                }
            },
    ) {
        // the disc and its grooves: still, drawn once
        Box(
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    val r = radius.toPx()
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val inner = r * 0.40f
                    val outer = r * 0.965f
                    val hair = 0.7.dp.toPx()
                    val gap = 2.6.dp.toPx()
                    onDrawBehind {
                        drawCircle(Color(0xFF0B0B0D), radius = r, center = c)
                        var rad = inner + gap
                        var i = 0
                        while (rad < outer) {
                            // a fixed pseudo-random shade per groove, so the surface looks cut rather than printed
                            val h = (((i * 2654435761L) ushr 7) and 0xFFL).toInt() / 255f
                            drawCircle(Color.White.copy(alpha = 0.035f + 0.05f * h), radius = rad, center = c, style = Stroke(hair))
                            // every so often a wider smooth gap, where one track ends and the next begins
                            rad += gap * (if (i % 23 == 22) 3.2f else 1f)
                            i++
                        }
                        drawCircle(Color(0xFF141418), radius = inner, center = c)
                        drawCircle(Color.White.copy(alpha = 0.12f), radius = inner, center = c, style = Stroke(hair))
                        drawCircle(Color.White.copy(alpha = 0.10f), radius = r - hair / 2f, center = c, style = Stroke(hair))
                    }
                },
        )
        // the light sliding over it: the only thing that turns
        Box(
            Modifier
                .fillMaxSize()
                .damageBoundary()
                .graphicsLayer { rotationZ = turn }
                .drawWithCache {
                    val r = radius.toPx()
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val inner = r * 0.40f
                    val outer = r * 0.965f
                    val none = Color.White.copy(alpha = 0f)
                    val brush = Brush.sweepGradient(
                        0f to none,
                        0.05f to none,
                        0.12f to Color.White.copy(alpha = 0.20f),
                        0.20f to none,
                        0.50f to none,
                        0.57f to none,
                        0.63f to Color.White.copy(alpha = 0.13f),
                        0.70f to none,
                        1f to none,
                        center = c,
                    )
                    onDrawBehind {
                        drawCircle(brush, radius = (inner + outer) / 2f, center = c, style = Stroke(outer - inner))
                    }
                },
        )
    }
}
