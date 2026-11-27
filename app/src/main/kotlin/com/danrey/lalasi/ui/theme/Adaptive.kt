// Adaptive.kt
//
// One switch for the light theme. [LalasiPalette.light] is set by LalasiTheme (themeStyle == LIGHT).
// The colours below read that snapshot state, so they work in composables, in draw blocks and in plain
// code alike, and repaint by themselves when the theme changes.
//
//  - LalasiInk: what text and icons are drawn with. White on the dark themes, near-black on Terang.
//    (A white overlay such as LalasiInk.copy(alpha = .08f) becomes a dark one on a light surface.)
//  - lalasiAdapt(hex): for a hard-coded neutral dark or light colour. Dark themes get the colour as
//    written; Terang gets its mirror, so a dark panel becomes a light one and light grey text dark.

package com.danrey.lalasi.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

object LalasiPalette {
    /** the chosen app theme; every adaptive colour below reads it, so a change repaints by itself */
    var style by mutableStateOf(com.danrey.lalasi.constants.AppThemeStyle.DARK)
    /** the light theme (Terang) was removed: kept as a constant so the adaptive colours below compile unchanged */
    val light: Boolean get() = false
    val glass: Boolean get() = style == com.danrey.lalasi.constants.AppThemeStyle.LIQUID_GLASS
    val ios: Boolean get() = style == com.danrey.lalasi.constants.AppThemeStyle.IOS
}

private val InkLight = Color(0xFF111216)

val LalasiInk: Color get() = if (LalasiPalette.light) InkLight else Color.White

private fun mirror(c: Color): Color {
    // flip brightness, keep it a touch off pure white/black so panels read as paper, not as a void
    fun f(v: Float) = 0.985f - v * 0.955f
    return Color(f(c.red).coerceIn(0f, 1f), f(c.green).coerceIn(0f, 1f), f(c.blue).coerceIn(0f, 1f), c.alpha)
}

fun lalasiAdapt(argb: Long): Color = lalasiAdapt(Color(argb))

fun lalasiAdapt(argb: Int): Color = lalasiAdapt(Color(argb))

private fun lalasiAdapt(c: Color): Color {
    if (LalasiPalette.light) return mirror(c)
    val mx = maxOf(c.red, c.green, c.blue)
    val mn = minOf(c.red, c.green, c.blue)
    val neutral = mx - mn < 0.09f && c.alpha > 0.9f
    if (!neutral) return c
    val avg = (c.red + c.green + c.blue) / 3f
    if (LalasiPalette.glass) {
        // Kaca cair: panels become frosted glass over the aurora backdrop; the deepest tones become deep navy
        return when {
            avg < 0.07f -> Color(0xFF0A1024)
            avg < 0.30f -> Color.White.copy(alpha = 0.07f + avg * 0.16f)
            else -> c
        }
    }
    if (LalasiPalette.ios) {
        // iOS: pure black canvas, grouped rows on systemGray6 / 5
        return when {
            avg < 0.07f -> Color.Black
            avg < 0.30f -> Color(0xFF1C1C1E)
            else -> c
        }
    }
    return c
}

/** the page canvas where the dark themes use pure black; paper on Terang */
val LalasiBlack: Color get() = if (LalasiPalette.light) Color(0xFFF7F8FB) else Color.Black
