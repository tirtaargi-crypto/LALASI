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
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0E1018)) {
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

@Composable
private fun WidgetStyleScreen(initial: WidgetStyle.Config, onApply: (WidgetStyle.Config) -> Unit) {
    var color by remember { mutableIntStateOf(initial.liquidColor) }
    var hue by remember { mutableFloatStateOf(0f) }
    var blur by remember { mutableFloatStateOf(initial.blur.toFloat()) }
    var tint by remember { mutableFloatStateOf(initial.tint.toFloat()) }
    val config = WidgetStyle.Config(color, blur.toInt(), tint.toInt())

    val density = LocalDensity.current.density
    val preview = remember(config, density) {
        val scale = density * 0.5f
        WidgetStyle.renderGlass(
            art = WidgetStyle.sampleArt(),
            w = (320 * scale).toInt(),
            h = (160 * scale).toInt(),
            cornerPx = 30f * scale,
            config = config,
            strokePx = 1.4f * scale,
        ).asImageBitmap()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Warna Liquid Widget", style = MaterialTheme.typography.titleLarge, color = Color.White)
        Image(
            bitmap = preview,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().aspectRatio(2f),
        )

        Text("Warna", color = Color(0xCCFFFFFF))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF))
                    .border(if (color == 0) 2.dp else 0.dp, Color.White, CircleShape)
                    .clickable { color = 0 },
                contentAlignment = Alignment.Center,
            ) { Text("Auto", color = Color.White, style = MaterialTheme.typography.labelSmall) }
            LiquidSwatches.take(5).forEach { c ->
                Swatch(c, selected = color == c) { color = c }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LiquidSwatches.drop(5).forEach { c ->
                Swatch(c, selected = color == c) { color = c }
            }
        }

        Text("Warna kustom", color = Color(0xCCFFFFFF))
        Slider(
            value = hue,
            onValueChange = {
                hue = it
                color = android.graphics.Color.HSVToColor(floatArrayOf(it, 0.65f, 1f))
            },
            valueRange = 0f..360f,
        )

        Text("Blur", color = Color(0xCCFFFFFF))
        Slider(value = blur, onValueChange = { blur = it }, valueRange = 0f..100f)

        Text("Intensitas warna", color = Color(0xCCFFFFFF))
        Slider(value = tint, onValueChange = { tint = it }, valueRange = 0f..100f)

        Button(onClick = { onApply(config) }, modifier = Modifier.fillMaxWidth()) {
            Text("Terapkan")
        }
        Text(
            "Auto mengambil warna dari sampul lagu. Blur dibuat dari sampul lagu yang sedang diputar.",
            color = Color(0x99FFFFFF),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun Swatch(color: Int, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(color))
            .border(if (selected) 2.dp else 0.dp, Color.White, CircleShape)
            .clickable(onClick = onClick),
    )
}
