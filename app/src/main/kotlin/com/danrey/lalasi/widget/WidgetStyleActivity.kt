// WidgetStyleActivity.kt

package com.danrey.lalasi.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.danrey.lalasi.playback.MusicService

/**
 * Picks the widgets' liquid colour, blur and colour strength. Opens when a widget is placed (and from the
 * launcher's "reconfigure" on a placed one), shows a live preview, and asks the music service to redraw.
 */
class WidgetStyleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // the widget stays placed even if the screen is backed out of
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))

        val initial = WidgetStyle.load(this)
        setContent {
            // follows the app theme (Terang = a light editor), also when the editor is opened from the widget with the app closed
            val (themeStyle) = com.danrey.lalasi.utils.rememberEnumPreference(
                com.danrey.lalasi.constants.AppThemeStyleKey,
                defaultValue = com.danrey.lalasi.constants.AppThemeStyle.DARK,
            )
            if (com.danrey.lalasi.ui.theme.LalasiPalette.style != themeStyle) com.danrey.lalasi.ui.theme.LalasiPalette.style = themeStyle
            val lightEditor = com.danrey.lalasi.ui.theme.LalasiPalette.light
            MaterialTheme(colorScheme = if (lightEditor) androidx.compose.material3.lightColorScheme() else darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = if (lightEditor) Color(0xFFF7F8FB) else Color(0xFF0E1018)) {
                    WidgetStyleScreen(
                        initial = initial,
                        onApply = { config ->
                            WidgetStyle.save(this@WidgetStyleActivity, config)
                            refreshWidgets()
                            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                            finish()
                        },
                    )
                }
            }
        }
    }

    private fun refreshWidgets() {
        if (!MusicService.isRunning) return
        val intent = Intent(this, MusicService::class.java).apply {
            action = MusicWidgetReceiver.ACTION_UPDATE_WIDGET
        }
        try {
            androidx.core.content.ContextCompat.startForegroundService(this, intent)
        } catch (e: Exception) {
        }
    }
}

private val LiquidSwatches = listOf(
    0xFF7C8CFF.toInt(), 0xFFFF6FB5.toInt(), 0xFFFF9A5C.toInt(), 0xFFFFD35C.toInt(),
    0xFF5CE0A8.toInt(), 0xFF4CC9FF.toInt(), 0xFFB67CFF.toInt(), 0xFFFF5C6C.toInt(),
)

private val Looks = listOf(
    WidgetStyle.Look.GLASS to "Kaca",
    WidgetStyle.Look.CLEAR to "Kaca bening",
    WidgetStyle.Look.DARK to "Gelap",
    WidgetStyle.Look.LIGHT to "Terang",
    WidgetStyle.Look.COVER to "Warna cover",
)

private val Shapes = listOf(
    WidgetStyle.CoverShape.ROUNDED to "Membulat",
    WidgetStyle.CoverShape.SQUARE to "Kotak",
    WidgetStyle.CoverShape.CIRCLE to "Lingkaran",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WidgetStyleScreen(initial: WidgetStyle.Config, onApply: (WidgetStyle.Config) -> Unit) {
    var look by remember { mutableIntStateOf(initial.look) }
    var color by remember { mutableIntStateOf(initial.liquidColor) }
    var hue by remember { mutableFloatStateOf(0f) }
    var frost by remember { mutableFloatStateOf(initial.blur.toFloat()) }
    var tint by remember { mutableFloatStateOf(initial.tint.toFloat()) }
    var shine by remember { mutableFloatStateOf(initial.shine.toFloat()) }
    var thickness by remember { mutableFloatStateOf(initial.thickness.toFloat()) }
    var shape by remember { mutableIntStateOf(initial.coverShape) }
    var swirl by remember { mutableFloatStateOf(initial.swirl.toFloat()) }
    val config = WidgetStyle.Config(color, frost.toInt(), tint.toInt(), look, shine.toInt(), thickness.toInt(), shape, swirl.toInt())

    val density = LocalDensity.current.density
    val art = remember { WidgetStyle.sampleArt() }
    val preview = remember(config, density) {
        val scale = density * 0.5f
        WidgetStyle.renderGlass(
            art = art,
            w = (320 * scale).toInt(),
            h = (160 * scale).toInt(),
            cornerPx = 30f * scale,
            config = config,
            strokePx = 1.4f * scale,
        ).asImageBitmap()
    }
    val ink = if (config.isLight) Color(0xFF14161C) else Color.White
    val coverRadius = when (shape) {
        WidgetStyle.CoverShape.SQUARE -> 4.dp
        WidgetStyle.CoverShape.CIRCLE -> 60.dp
        else -> 18.dp
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Gaya Widget", style = MaterialTheme.typography.titleLarge, color = com.danrey.lalasi.ui.theme.LalasiInk)

        // live preview: the glass, then a cover and two lines of text on it
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(2f)) {
            Image(bitmap = preview, contentDescription = null, modifier = Modifier.fillMaxSize())
            Row(
                modifier = Modifier.fillMaxSize().padding(22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Image(
                    bitmap = remember { WidgetStyle.sampleArt().asImageBitmap() },
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.size(78.dp).clip(RoundedCornerShape(coverRadius)),
                )
                Column {
                    Text("Judul lagu", color = ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("Nama artis", color = ink.copy(alpha = 0.65f), fontSize = 13.sp)
                }
            }
        }

        Label("Gaya widget")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Looks.forEach { (id, name) -> Chip(name, look == id) { look = id } }
        }

        Label("Warna")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF))
                    .border(if (color == 0) 2.dp else 0.dp, com.danrey.lalasi.ui.theme.LalasiInk, CircleShape)
                    .clickable { color = 0 }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Dari cover", color = com.danrey.lalasi.ui.theme.LalasiInk, style = MaterialTheme.typography.labelMedium) }
            LiquidSwatches.forEach { c -> Swatch(c, selected = color == c) { color = c } }
        }
        Text("Varian warna kustom", color = com.danrey.lalasi.ui.theme.LalasiInk.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
        Slider(
            value = hue,
            onValueChange = {
                hue = it
                color = android.graphics.Color.HSVToColor(floatArrayOf(it, 0.65f, 1f))
            },
            valueRange = 0f..360f,
        )

        PercentSlider("Kilau kaca", shine) { shine = it }
        PercentSlider("Ketebalan kaca (lensa + tepi)", thickness) { thickness = it }
        PercentSlider("Pusaran latar (melar)", swirl) { swirl = it }
        PercentSlider("Frost (blur cover)", frost) { frost = it }
        PercentSlider("Intensitas warna", tint) { tint = it }

        Label("Bentuk cover")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Shapes.forEach { (id, name) -> Chip(name, shape == id) { shape = id } }
        }

        Button(onClick = { onApply(config) }, modifier = Modifier.fillMaxWidth()) {
            Text("Terapkan")
        }
        Text(
            "Kilau dan ketebalan 50% = tampilan standar. Frost 0% menampilkan cover tanpa blur. Bentuk cover berlaku di widget yang menampilkan cover.",
            color = com.danrey.lalasi.ui.theme.LalasiInk.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun Label(text: String) {
    Text(text, color = com.danrey.lalasi.ui.theme.LalasiInk.copy(alpha = 0.8f), style = MaterialTheme.typography.titleSmall)
}

@Composable
private fun PercentSlider(title: String, value: Float, onChange: (Float) -> Unit) {
    Column {
        Text("$title: ${value.toInt()}%", color = com.danrey.lalasi.ui.theme.LalasiInk.copy(alpha = 0.8f))
        Slider(value = value, onValueChange = onChange, valueRange = 0f..100f)
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(CircleShape)
            .background(if (selected) com.danrey.lalasi.ui.theme.LalasiInk else com.danrey.lalasi.ui.theme.LalasiInk.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (selected) (if (com.danrey.lalasi.ui.theme.LalasiPalette.light) Color.White else Color.Black) else com.danrey.lalasi.ui.theme.LalasiInk, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun Swatch(color: Int, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(color))
            .border(if (selected) 2.dp else 0.dp, com.danrey.lalasi.ui.theme.LalasiInk, CircleShape)
            .clickable(onClick = onClick),
    )
}
