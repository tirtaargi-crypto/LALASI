// Theme.kt

package com.danrey.lalasi.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score

val DefaultThemeColor = Color(0xFF8E8E8E)

/**
 * LALASI uses a soft-black, dark-first palette.
 *
 * The light palette was never finished - large parts of the UI hardcode white text and dark
 * glass - so on a light-mode phone the app looked broken rather than light. Pinned here as one
 * switch instead of being decided independently at every call site.
 */
const val ForceDarkTheme = true

@Composable
fun LalasiTheme(
    darkTheme: Boolean = ForceDarkTheme,
    pureBlack: Boolean = false,
    themeStyle: com.danrey.lalasi.constants.AppThemeStyle = com.danrey.lalasi.constants.AppThemeStyle.DARK,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    // everything that draws with LalasiInk / lalasiAdapt follows this
    if (LalasiPalette.style != themeStyle) LalasiPalette.style = themeStyle

    // Dynamic (Material You) colour is deliberately not used - the app ships its own palette.
    // The branch is gone rather than dead-but-present: dynamic*ColorScheme is API 31.
    val baseColorScheme = run {

        if (darkTheme && themeStyle == com.danrey.lalasi.constants.AppThemeStyle.IOS) {
            // iOS: true black canvas, grouped rows on systemGray6, system blue accent
            darkColorScheme(
                primary = Color(0xFF0A84FF),
                onPrimary = Color.White,
                primaryContainer = Color(0xFF1C1C1E),
                onPrimaryContainer = Color.White,
                secondary = Color(0xFF8E8E93),
                onSecondary = Color.Black,
                secondaryContainer = Color(0xFF2C2C2E),
                onSecondaryContainer = Color.White,
                background = Color.Black,
                onBackground = Color.White,
                surface = Color.Black,
                onSurface = Color.White,
                surfaceVariant = Color(0xFF1C1C1E),
                onSurfaceVariant = Color(0xFFAEAEB2),
                surfaceTint = Color.Black,
            )
        } else if (darkTheme && themeStyle == com.danrey.lalasi.constants.AppThemeStyle.LIQUID_GLASS) {
            // Kaca cair: deep blue-violet, cool translucent-looking surfaces, bright icy accent
            darkColorScheme(
                primary = Color(0xFF9DB8FF),
                onPrimary = Color(0xFF0A1330),
                primaryContainer = Color(0xFF1B2547),
                onPrimaryContainer = Color(0xFFE3EAFF),
                secondary = Color(0xFFA9D6FF),
                onSecondary = Color(0xFF06182B),
                secondaryContainer = Color(0xFF16304D),
                onSecondaryContainer = Color(0xFFE3F1FF),
                background = Color.Transparent,
                onBackground = Color(0xFFF2F4FF),
                surface = Color(0xFF0B1024),
                onSurface = Color(0xFFF2F4FF),
                surfaceVariant = Color(0xFF161C38),
                onSurfaceVariant = Color(0xFFBFC6E6),
                surfaceTint = Color(0xFF0B1024),
            )
        } else if (darkTheme) {
            darkColorScheme(
                primary = Color(0xFFE7E8EB),
                onPrimary = Color.Black,
                primaryContainer = Color(0xFF25282D),
                onPrimaryContainer = Color.White,
                secondary = Color(0xFFB8BBC1),
                onSecondary = Color.Black,
                secondaryContainer = Color(0xFF1D2025),
                onSecondaryContainer = Color.White,
                background = Color(0xFF050608),
                onBackground = Color(0xFFF5F5F6),
                surface = Color(0xFF0D0F12),
                onSurface = Color(0xFFF5F5F6),
                surfaceVariant = Color(0xFF191C21),
                onSurfaceVariant = Color(0xFFC2C4C8),
                surfaceTint = Color(0xFF0D0F12)
            )
        } else {
            lightColorScheme(
                primary = Color.Black,
                onPrimary = Color.White,
                primaryContainer = Color(0xFFE0E0E0),
                onPrimaryContainer = Color.Black,
                secondary = Color(0xFF333333),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFCCCCCC),
                onSecondaryContainer = Color.Black,
                background = Color(0xFFF7F8FB),
                onBackground = Color(0xFF111216),
                surface = Color(0xFFF7F8FB),
                onSurface = Color(0xFF111216),
                surfaceVariant = Color(0xFFECEDF2),
                onSurfaceVariant = Color(0xFF3C3F46),
                surfaceTint = Color.White,
                surfaceContainerLowest = Color.White,
                surfaceContainerLow = Color(0xFFF7F8FB),
                surfaceContainer = Color(0xFFF2F3F7),
                surfaceContainerHigh = Color(0xFFECEDF2),
                surfaceContainerHighest = Color(0xFFE6E7ED),
                outline = Color(0xFF8E8E93),
                outlineVariant = Color(0xFFD1D2D8),
            )
        }
    }

    val colorScheme = remember(baseColorScheme, pureBlack, darkTheme) {
        if (darkTheme && pureBlack) {
            baseColorScheme.pureBlack(true)
        } else {
            baseColorScheme
        }
    }

    val font = InterFontFamily
    val typography = remember(font) { appTypography(font) }
    val shapes = remember(themeStyle) {
        when (themeStyle) {
            com.danrey.lalasi.constants.AppThemeStyle.IOS -> androidx.compose.material3.Shapes(
                extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                large = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            )
            com.danrey.lalasi.constants.AppThemeStyle.LIQUID_GLASS -> androidx.compose.material3.Shapes(
                extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                small = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                medium = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                large = androidx.compose.foundation.shape.RoundedCornerShape(30.dp),
                extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(36.dp),
            )
            else -> androidx.compose.material3.Shapes()
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = shapes,
    ) {
        ProvideTextStyle(
            value = typography.bodyLarge,
            content = content
        )
    }
}

fun Bitmap.extractThemeColor(): Color {
    val colorsToPopulation = Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .swatches
        .associate { it.rgb to it.population }
    val rankedColors = Score.score(colorsToPopulation)
    return Color(rankedColors.first())
}

fun Bitmap.extractGradientColors(): List<Color> {
    val extractedColors = Palette.from(this)
        .maximumColorCount(64)
        .generate()
        .swatches
        .associate { it.rgb to it.population }

    val orderedColors = Score.score(extractedColors, 2, 0xff4285f4.toInt(), true)
        .sortedByDescending { Color(it).luminance() }

    return if (orderedColors.size >= 2)
        listOf(Color(orderedColors[0]), Color(orderedColors[1]))
    else
        listOf(Color(0xFF595959), Color(0xFF0D0D0D))
}

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
