package com.danrey.lalasi.ui.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * LALASI design tokens. One place for the numbers the redesign uses, so screens agree.
 * Type: the app's existing Inter scale is kept; only these sizes are used for new surfaces.
 */
object LalasiTokens {
    // spacing scale
    val Space4 = 4.dp
    val Space8 = 8.dp
    val Space12 = 12.dp
    val Space16 = 16.dp
    val Space20 = 20.dp
    val Space24 = 24.dp

    // corner radius
    val RadiusChip = 24.dp
    val RadiusCard = 22.dp

    // minimum touch target
    val TouchTarget = 48.dp

    // surface opacity of the light glass
    const val GlassFillAlpha = 0.08f
    const val GlassBorderAlpha = 0.13f
    const val GlassHighlightAlpha = 0.055f

    // LALASI Soft Black palette
    val Background = Color(0xFF050608)
    val Surface = Color(0xFF0D0F12)
    val ElevatedSurface = Color(0xFF171A1F)
    val FrostedSilver = Color(0xFFE7E8EB)
    val MutedSilver = Color(0xFF9A9EA6)
}

/**
 * A light glass surface for small interactive elements (chips, selected rows).
 * It uses a low-opacity neutral body and a fine reflective outline; the floating navigation and
 * mini player use the full backdrop-sampling blur path for heavier frosted glass.
 */
fun Modifier.lalasiGlassSurface(shape: Shape, cornerRadiusFraction: Float = 0.5f): Modifier =
    this
        .clip(shape)
        .drawWithContent {
            // Neutral translucent glass: a single material tint, never a decorative gradient.
            drawRect(Color.White.copy(alpha = LalasiTokens.GlassFillAlpha * 0.78f))
            // Restrained specular edge, matching the floating chrome.
            drawRoundRect(
                color = Color.White.copy(alpha = 0.045f),
                topLeft = Offset(1f, 1f),
                size = androidx.compose.ui.geometry.Size((size.width - 2f).coerceAtLeast(0f), (size.height - 2f).coerceAtLeast(0f)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height * cornerRadiusFraction),
                style = androidx.compose.ui.graphics.drawscope.Stroke(0.75f),
            )
            drawContent()
            drawChromeOutline(
                topLeft = Offset.Zero,
                size = size,
                cornerRadius = size.height * cornerRadiusFraction,
            )
        }
