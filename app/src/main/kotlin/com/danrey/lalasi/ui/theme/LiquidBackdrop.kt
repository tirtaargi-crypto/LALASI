// LiquidBackdrop.kt
//
// Kaca cair's canvas: a deep midnight gradient with a few large, slowly drifting light blooms (cool blue, teal,
// soft white). Everything glass in the app (panels turn into frosted white, see lalasiAdapt) sits on this, so the
// glass has something to refract. Drawn in one layer, three animated floats, no bitmaps.

package com.danrey.lalasi.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun LiquidBackdrop(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "liquidBackdrop")
    val a by t.animateFloat(0f, 1f, infiniteRepeatable(tween(23000, easing = LinearEasing), RepeatMode.Reverse), label = "a")
    val b by t.animateFloat(0f, 1f, infiniteRepeatable(tween(31000, easing = LinearEasing), RepeatMode.Reverse), label = "b")
    val c by t.animateFloat(0f, 1f, infiniteRepeatable(tween(41000, easing = LinearEasing), RepeatMode.Reverse), label = "c")
    Box(modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF050914), Color(0xFF0A1330), Color(0xFF07101F))))
            val w = size.width
            val h = size.height
            fun bloom(cx: Float, cy: Float, r: Float, color: Color) = drawCircle(
                brush = Brush.radialGradient(listOf(color, Color.Transparent), center = Offset(cx, cy), radius = r),
                radius = r, center = Offset(cx, cy),
            )
            bloom(w * (0.12f + 0.55f * a), h * (0.14f + 0.20f * b), w * 0.95f, Color(0x993E7BFF))
            bloom(w * (0.90f - 0.55f * b), h * (0.52f + 0.20f * c), w * 0.85f, Color(0x801FB6C9))
            bloom(w * (0.25f + 0.50f * c), h * (0.92f - 0.22f * a), w * 0.80f, Color(0x66A9C4FF))
        }
    }
}
