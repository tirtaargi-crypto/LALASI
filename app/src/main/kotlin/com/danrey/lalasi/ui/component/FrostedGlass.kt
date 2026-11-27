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
private val ChromeOutlineColor = Color.White.copy(alpha = 0.30f)
private val ChromeOutlineStroke = Stroke(ChromeOutlineWidthPx)

/** Lifts the chrome off a light page without looking like a dark halo on a black one. */
val ChromeShadow: Shadow get() =
    // Terang: a soft, wide, faint shadow (the bar floats on white); the dark themes keep the tighter, darker one
    if (com.danrey.lalasi.ui.theme.LalasiPalette.light) Shadow(radius = 18.dp, color = Color(0xFF1B2A4A).copy(alpha = 0.09f), spread = 0.dp)
    else Shadow(radius = 10.dp, color = Color.Black.copy(alpha = 0.22f), spread = 1.dp)

/** The lit tab's pill in the nav bar, and the round press behind the mini player's buttons. */
val ChromeHighlightColor = Color.White.copy(alpha = 0.12f)

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
                0f to Color.White.copy(alpha = 0.11f * sheenScale),
                0.40f to Color.White.copy(alpha = 0.045f * sheenScale),
                0.78f to Color.White.copy(alpha = 0.02f * sheenScale),
                1f to Color.White.copy(alpha = 0.05f * sheenScale),
            )
            // the glass is thick at its rim: a faint inner shade just inside the edge, so the middle reads clear
            val rimShade = Brush.radialGradient(
                0.62f to Color.Transparent,
                1f to Color.Black.copy(alpha = (if (com.danrey.lalasi.ui.theme.LalasiPalette.light) 0.03f else 0.16f) * sheenScale),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = maxOf(size.width, size.height) * 0.62f,
            )
            onDrawWithContent {
                drawContent()
                drawRect(brush = rimShade)
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
    if (com.danrey.lalasi.ui.theme.LalasiPalette.ios) {
        // iOS: no lit glass edge, just the system's hairline
        drawRoundRect(
            color = Color(0xFF545458).copy(alpha = 0.55f * alpha),
            topLeft = Offset(topLeft.x + 0.4f, topLeft.y + 0.4f),
            size = Size((size.width - 0.8f).coerceAtLeast(0f), (size.height - 0.8f).coerceAtLeast(0f)),
            cornerRadius = CornerRadius((cornerRadius - 0.4f).coerceAtLeast(0f)),
            style = Stroke(0.8f),
        )
        return
    }
    val half = ChromeOutlineWidthPx / 2f
    val inset = Offset(topLeft.x + half, topLeft.y + half)
    val innerSize = Size(size.width - ChromeOutlineWidthPx, size.height - ChromeOutlineWidthPx)
    if (innerSize.width <= 2f || innerSize.height <= 2f) return
    val innerRadius = CornerRadius((cornerRadius - half).coerceAtLeast(0f))
    val x1 = inset.x + innerSize.width
    val y1 = inset.y + innerSize.height

    // iOS 26 edge light: the light comes from the top-left, so that corner burns bright and the edge fades out
    // along the sides; a softer bounce lights the bottom-right. Same gradient for the glow and the line.
    val rim = Brush.linearGradient(
        0f to Color.White.copy(alpha = 0.72f),
        0.14f to Color.White.copy(alpha = 0.40f),
        0.40f to Color.White.copy(alpha = 0.11f),
        0.62f to Color.White.copy(alpha = 0.11f),
        0.86f to Color.White.copy(alpha = 0.42f),
        1f to Color.White.copy(alpha = 0.78f),
        start = inset,
        end = Offset(x1, y1),
    )
    // a soft bloom under the line, as if the edge were lit from inside the glass
    drawRoundRect(
        brush = rim,
        topLeft = inset,
        size = innerSize,
        cornerRadius = innerRadius,
        style = Stroke(ChromeOutlineWidthPx * 3f),
        alpha = 0.26f * alpha,
    )
    drawRoundRect(
        brush = rim,
        topLeft = inset,
        size = innerSize,
        cornerRadius = innerRadius,
        style = Stroke(ChromeOutlineWidthPx * 1.1f),
        alpha = alpha,
    )
    // a trace of the rainbow at the edge: light splitting as it leaves the glass, kept very faint
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(inset.x + 1.1f, inset.y + 1.1f),
        size = Size((innerSize.width - 2.2f).coerceAtLeast(0f), (innerSize.height - 2.2f).coerceAtLeast(0f)),
        cornerRadius = CornerRadius((innerRadius.x - 1.1f).coerceAtLeast(0f)),
        style = Stroke(0.8f),
        alpha = 0.12f * alpha,
    )
    drawRoundRect(
        color = Color(0xFFFFA8DC),
        topLeft = Offset(inset.x + 1.9f, inset.y + 1.9f),
        size = Size((innerSize.width - 3.8f).coerceAtLeast(0f), (innerSize.height - 3.8f).coerceAtLeast(0f)),
        cornerRadius = CornerRadius((innerRadius.x - 1.9f).coerceAtLeast(0f)),
        style = Stroke(0.7f),
        alpha = 0.09f * alpha,
    )
    // the shine that runs along the top edge, tapering away at both ends
    val r = innerRadius.x
    val run = innerSize.width - 2f * r
    if (run > 24f) {
        val from = inset.x + r + run * 0.04f
        val to = inset.x + r + run * 0.62f
        drawLine(
            brush = Brush.horizontalGradient(
                0f to Color.Transparent,
                0.35f to Color.White.copy(alpha = 0.70f * alpha),
                1f to Color.Transparent,
                startX = from,
                endX = to,
            ),
            start = Offset(from, inset.y + 1.3f),
            end = Offset(to, inset.y + 1.3f),
            strokeWidth = 1.6f,
        )
    }
    // the cut-glass inner edge
    drawRoundRect(
        color = Color.White.copy(alpha = 0.07f),
        topLeft = Offset(inset.x + 3f, inset.y + 3f),
        size = Size((innerSize.width - 6f).coerceAtLeast(0f), (innerSize.height - 6f).coerceAtLeast(0f)),
        cornerRadius = CornerRadius((innerRadius.x - 3f).coerceAtLeast(0f)),
        style = Stroke(0.7f),
        alpha = alpha,
    )
}
