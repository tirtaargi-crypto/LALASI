// ThemeSection.kt
//
// Pengaturan > Tampilan > Tema: iOS, Gelap, Terang, Kaca cair. The choice is one enum in the DataStore
// (AppThemeStyleKey); MainActivity reads it and LalasiTheme turns it into a colour scheme.
// Also holds the "Gaya widget" card, which opens the widget editor.

package com.danrey.lalasi.ui.screens.gesture

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danrey.lalasi.constants.AppThemeStyle
import com.danrey.lalasi.constants.AppThemeStyleKey
import com.danrey.lalasi.ui.theme.InterFontFamily
import com.danrey.lalasi.ui.theme.LalasiInk
import com.danrey.lalasi.ui.theme.ThemeTransition
import com.danrey.lalasi.ui.player.findActivity
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

import com.danrey.lalasi.utils.rememberEnumPreference
import com.danrey.lalasi.widget.WidgetStyleActivity

private data class ThemeOption(val style: AppThemeStyle, val label: String, val hint: String)

private val ThemeOptions = listOf(
    ThemeOption(AppThemeStyle.IOS, "iOS", "Hitam pekat, daftar berkelompok, biru sistem"),
    ThemeOption(AppThemeStyle.DARK, "Gelap", "Tampilan bawaan LALASI"),
    ThemeOption(AppThemeStyle.LIQUID_GLASS, "Kaca cair", "Panel kaca buram di atas cahaya bergerak"),
)

@Composable
internal fun ThemeSection() {
    val (selected, setSelected) = rememberEnumPreference(AppThemeStyleKey, defaultValue = AppThemeStyle.DARK)
    val activity = LocalContext.current.findActivity()
    // the light theme (Terang) was removed: a saved choice of it becomes Gelap
    androidx.compose.runtime.LaunchedEffect(selected) { if (selected == AppThemeStyle.LIGHT) setSelected(AppThemeStyle.DARK) }
    val pick: (AppThemeStyle) -> Unit = { style ->
        if (style != selected) ThemeTransition.run(activity, style) { setSelected(style) }
    }
    Column {
        Text(
            "Tema",
            style = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp),
            color = LalasiInk,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Berlaku langsung, tanpa buka ulang aplikasi.",
            style = TextStyle(fontFamily = InterFontFamily, fontSize = 13.sp),
            color = Color(0xFF8E8E93),
        )
        Spacer(Modifier.height(14.dp))
        ThemeOptions.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                pair.forEach { option ->
                    ThemeCard(option, selected == option.style, Modifier.weight(1f)) { pick(option.style) }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ThemeCard(option: ThemeOption, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val ring = Color(0xFF0A84FF)
    Column(
        modifier = modifier
            .clip(shape)
            .background(LalasiInk.copy(alpha = 0.06f))
            .border(if (selected) 2.dp else 1.dp, if (selected) ring else LalasiInk.copy(alpha = 0.12f), shape)
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        ThemeMock(option.style, Modifier.fillMaxWidth().aspectRatio(0.82f))
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                option.label,
                style = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                color = LalasiInk,
                modifier = Modifier.weight(1f),
            )
            if (selected) Box(Modifier.size(10.dp).clip(CircleShape).background(ring))
        }
        Text(
            option.hint,
            style = TextStyle(fontFamily = InterFontFamily, fontSize = 11.sp, lineHeight = 14.sp),
            color = Color(0xFF8E8E93),
        )
    }
}

/** a tiny phone screen drawn in that theme's own look: what each one really does to the app */
@Composable
private fun ThemeMock(style: AppThemeStyle, modifier: Modifier) {
    Canvas(modifier.clip(RoundedCornerShape(14.dp))) {
        val w = size.width
        val h = size.height
        val pad = w * 0.07f
        fun rr(x: Float, y: Float, rw: Float, rh: Float, r: Float, c: Color) =
            drawRoundRect(c, Offset(x, y), Size(rw, rh), CornerRadius(r, r))
        fun line(y: Float, lw: Float, c: Color) = rr(pad, y, lw, h * 0.028f, h, c)
        when (style) {
            AppThemeStyle.IOS -> {
                drawRect(Color.Black)
                line(h * 0.07f, w * 0.40f, Color.White)                         // large title
                // grouped inset list: one card, hairline separators, blue chevrons
                val top = h * 0.18f; val rowH = h * 0.12f
                rr(pad, top, w - pad * 2, rowH * 3, w * 0.07f, Color(0xFF1C1C1E))
                repeat(3) { i ->
                    val y = top + rowH * i
                    rr(pad * 1.6f, y + rowH * 0.28f, w * 0.12f, w * 0.12f, w * 0.03f, listOf(Color(0xFF0A84FF), Color(0xFF30D158), Color(0xFFFF9F0A))[i])
                    rr(pad * 1.6f + w * 0.2f, y + rowH * 0.42f, w * 0.34f, h * 0.026f, h, Color.White.copy(alpha = 0.85f))
                    rr(w - pad * 2.4f, y + rowH * 0.38f, w * 0.03f, h * 0.035f, h, Color(0xFF636366))
                    if (i < 2) rr(pad * 1.6f + w * 0.2f, y + rowH - 1f, w - pad * 2 - pad * 1.6f - w * 0.2f, 1.2f, 0f, Color(0xFF38383A))
                }
                // tab bar: flat, hairline on top, blue active
                rr(0f, h * 0.88f, w, 1.2f, 0f, Color(0xFF38383A))
                repeat(4) { i -> drawCircle(if (i == 0) Color(0xFF0A84FF) else Color(0xFF636366), w * 0.028f, Offset(w * (0.2f + 0.2f * i), h * 0.945f)) }
            }
            AppThemeStyle.DARK -> {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF090B0E), Color(0xFF050608))))
                line(h * 0.07f, w * 0.40f, Color(0xFFE7E8EB))
                repeat(3) { i -> rr(pad, h * 0.18f + h * 0.14f * i, w - pad * 2, h * 0.11f, w * 0.04f, Color(0xFF191C21)) }
                rr(pad, h * 0.78f, w - pad * 2, h * 0.08f, h, Color(0xFF25282D))
                drawCircle(Color(0xFFE7E8EB), h * 0.022f, Offset(pad * 2f, h * 0.82f))
            }
            AppThemeStyle.LIGHT -> {
                drawRect(Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF2F3F7))))
                line(h * 0.07f, w * 0.40f, Color(0xFF111216))
                repeat(3) { i -> rr(pad, h * 0.18f + h * 0.14f * i, w - pad * 2, h * 0.11f, w * 0.04f, Color(0xFFE9EAEF)) }
                rr(pad, h * 0.78f, w - pad * 2, h * 0.08f, h, Color(0xFFFFFFFF))
                drawCircle(Color(0xFF111216), h * 0.022f, Offset(pad * 2f, h * 0.82f))
            }
            AppThemeStyle.LIQUID_GLASS -> {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF050914), Color(0xFF0A1330), Color(0xFF07101F))))
                // the moving light behind the glass
                drawCircle(Brush.radialGradient(listOf(Color(0x883E7BFF), Color.Transparent), Offset(w * 0.2f, h * 0.2f), w * 0.8f), w * 0.8f, Offset(w * 0.2f, h * 0.2f))
                drawCircle(Brush.radialGradient(listOf(Color(0x661FB6C9), Color.Transparent), Offset(w * 0.9f, h * 0.62f), w * 0.7f), w * 0.7f, Offset(w * 0.9f, h * 0.62f))
                line(h * 0.07f, w * 0.40f, Color.White.copy(alpha = 0.92f))
                val edge = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.75f), Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.35f)), Offset.Zero, Offset(w, h))
                repeat(3) { i ->
                    val y = h * 0.18f + h * 0.14f * i
                    val r = w * 0.08f
                    rr(pad, y, w - pad * 2, h * 0.11f, r, Color.White.copy(alpha = 0.10f))
                    rr(pad, y, w - pad * 2, h * 0.055f, r, Color.White.copy(alpha = 0.05f))
                    drawRoundRect(edge, Offset(pad, y), Size(w - pad * 2, h * 0.11f), CornerRadius(r, r), style = androidx.compose.ui.graphics.drawscope.Stroke(1.4f))
                }
                // floating glass pill nav
                val ny = h * 0.80f
                rr(pad * 1.5f, ny, w - pad * 3, h * 0.10f, h, Color.White.copy(alpha = 0.12f))
                drawRoundRect(edge, Offset(pad * 1.5f, ny), Size(w - pad * 3, h * 0.10f), CornerRadius(h, h), style = androidx.compose.ui.graphics.drawscope.Stroke(1.4f))
                rr(w * 0.18f, ny + h * 0.012f, w * 0.26f, h * 0.076f, h, Color.White.copy(alpha = 0.22f))
            }
        }
    }
}

/** Gaya widget: the editor lives in its own activity (live preview + sliders); this card opens it. */
@Composable
internal fun WidgetStyleCard() {
    val context = LocalContext.current
    Column {
        Text(
            "Gaya widget",
            style = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp),
            color = LalasiInk,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Kaca, kaca bening, gelap, terang, warna cover. Atur warna, kilau, ketebalan kaca, frost, dan bentuk cover.",
            style = TextStyle(fontFamily = InterFontFamily, fontSize = 13.sp, lineHeight = 18.sp),
            color = Color(0xFF8E8E93),
        )
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(25.dp))
                .background(Color(0xFF0A84FF))
                .clickable { context.startActivity(Intent(context, WidgetStyleActivity::class.java)) },
            contentAlignment = Alignment.Center,
        ) {
            Text("Buka editor widget", style = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp), color = Color.White)
        }
    }
}
