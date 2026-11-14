// FrostedGlass.kt

package com.danrey.lalasi.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.dp

/*
 * Floating navigation and mini-player chrome: a transparent neutral tint over the live backdrop,
 * restrained double hairline and a soft shadow. Both surfaces share these tokens.
 */

/** How much of the chrome colour covers the blurred app. */
private const val FrostTintAlpha = 0.30f

/** A fine physical-pixel edge; the secondary inset line creates a restrained glass highlight. */
private const val ChromeOutlineWidthPx = 1.25f
private val ChromeOutlineColor = Color.White.copy(alpha = 0.34f)
private val ChromeOutlineStroke = Stroke(ChromeOutlineWidthPx)

/** Lifts the chrome off a light page without looking like a dark halo on a black one. */
val ChromeShadow = Shadow(radius = 10.dp, color = Color.Black.copy(alpha = 0.22f), spread = 1.dp)

/** The lit tab's pill in the nav bar, and the round press behind the mini player's buttons. */
val ChromeHighlightColor = Color.White.copy(alpha = 0.075f)

/**
 * A faint grain over the glass. A dark blur only has a few shades to work with, so without it the
 * glass shows bands; this breaks them up and is too faint to see as grain.
 */
private const val FrostGrainAlpha = 0.035f
private const val FrostGrainTileSize = 64

/** A small tile of grain, half lighter and half darker pixels, repeated over the glass. Made once. */
private val FrostGrainBrush: ShaderBrush by lazy {
    val random = java.util.Random(0x6A55)
    val pixels = IntArray(FrostGrainTileSize * FrostGrainTileSize) {
        val alpha = random.nextInt(256) shl 24
        if (random.nextBoolean()) alpha or 0xFFFFFF else alpha
    }
    val tile = android.graphics.Bitmap.createBitmap(
        pixels, FrostGrainTileSize, FrostGrainTileSize, android.graphics.Bitmap.Config.ARGB_8888,
    )
    ShaderBrush(ImageShader(tile.asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
}

/**
 * The frosted glass behind the nav bar and the mini player.
 *
 * The blur reads a margin of the app around the glass (see `bleed` on [GlassPillBackground]). It
 * used to see only what was right under the glass and repeat the edge row past it, so anything
 * bright touching an edge was smeared across the whole bar: streaks while scrolling, and a flash
 * of glow each time a cover or a title crossed in. The parent has to clip it to its shape.
 */
@Composable
fun FrostedChromeGlass(
    state: GlassState,
    pureBlack: Boolean,
    accentWash: (() -> Color)?,
    modifier: Modifier = Modifier,
) {
    val color = if (pureBlack) Color.Black else GlassChromeColor
    // the grain only matters on the finer, high quality glass
    val grain = LocalBlurQuality.current == com.danrey.lalasi.constants.BlurQuality.HIGH
    val sheenScale = if (pureBlack) 0.6f else 1f
    Box(
        modifier = modifier.drawWithCache {
            // a light, still see-through sheen over the blur: brighter glass, not a milky fill
            val sheen = Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.20f * sheenScale),
                0.45f to Color.White.copy(alpha = 0.07f * sheenScale),
                1f to Color.White.copy(alpha = 0.13f * sheenScale),
            )
            onDrawWithContent {
                drawContent()
                drawRect(brush = sheen)
                if (grain) drawRect(brush = FrostGrainBrush, alpha = FrostGrainAlpha)
            }
        }
    ) {
        GlassPillBackground(
            state = state,
            blurRadius = { GlassChromeBlurRadius },
            tint = color.copy(alpha = FrostTintAlpha),
            foundationColor = color,
            accentWash = accentWash,
            // Only reaches the margin's outer edge now, too far out to show.
            tileMode = android.graphics.Shader.TileMode.CLAMP,
            bleed = true,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * The chrome's hairline round a rounded rect, kept just inside it so the parent's clip can't
 * shave half of it off. [alpha] fades it, for the mini player as it opens.
 */
fun DrawScope.drawChromeOutline(
    topLeft: Offset,
    size: Size,
    cornerRadius: Float,
    alpha: Float = 1f,
) {
    val half = ChromeOutlineWidthPx / 2f
    val inset = Offset(topLeft.x + half, topLeft.y + half)
    val innerSize = Size(size.width - ChromeOutlineWidthPx, size.height - ChromeOutlineWidthPx)
    val innerRadius = CornerRadius((cornerRadius - half).coerceAtLeast(0f))
    drawRoundRect(
        color = ChromeOutlineColor,
        topLeft = inset,
        size = innerSize,
        cornerRadius = innerRadius,
        style = ChromeOutlineStroke,
        alpha = alpha,
    )
    // Specular edge: the top run and the top-left corner catch the light, the bottom-right bounces a little back.
    val r = innerRadius.x
    val w = innerSize.width
    val h = innerSize.height
    val spec = Stroke(ChromeOutlineWidthPx * 1.4f)
    if (w > 2f * r && r > 0f) {
        drawLine(
            color = Color.White.copy(alpha = 0.70f * alpha),
            start = Offset(inset.x + r, inset.y),
            end = Offset(inset.x + w - r, inset.y),
            strokeWidth = spec.width,
        )
        drawLine(
            color = Color.White.copy(alpha = 0.32f * alpha),
            start = Offset(inset.x + r, inset.y + h),
            end = Offset(inset.x + w - r, inset.y + h),
            strokeWidth = ChromeOutlineWidthPx,
        )
    }
    if (r > 0f) {
        drawArc(
            color = Color.White.copy(alpha = 0.70f * alpha),
            startAngle = 180f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(inset.x, inset.y),
            size = Size(r * 2f, r * 2f),
            style = spec,
        )
        drawArc(
            color = Color.White.copy(alpha = 0.40f * alpha),
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(inset.x + w - r * 2f, inset.y + h - r * 2f),
            size = Size(r * 2f, r * 2f),
            style = Stroke(ChromeOutlineWidthPx),
        )
    }
    // A second, faint inner edge gives the glass a real cut-glass boundary without a glow.
    drawRoundRect(
        color = Color.White.copy(alpha = 0.10f),
        topLeft = Offset(inset.x + 1.5f, inset.y + 1.5f),
        size = Size((innerSize.width - 3f).coerceAtLeast(0f), (innerSize.height - 3f).coerceAtLeast(0f)),
        cornerRadius = CornerRadius((innerRadius.x - 1.5f).coerceAtLeast(0f)),
        style = Stroke(0.75f),
        alpha = alpha,
    )
}
