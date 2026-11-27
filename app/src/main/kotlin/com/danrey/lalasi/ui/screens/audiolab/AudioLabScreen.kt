// audio lab home, set the iOS way: a LALASI hero card (purple to orange, a spectrum that moves while sound is being processed) and grouped rows with
// coloured icon tiles, each row showing its state at the right

package com.danrey.lalasi.ui.screens.audiolab

import com.danrey.lalasi.ui.theme.LalasiInk
import com.danrey.lalasi.ui.theme.lalasiAdapt

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.geometry.Size as GeoSize
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.ui.unit.em
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.danrey.lalasi.LocalPlayerConnection
import com.danrey.lalasi.R
import com.danrey.lalasi.constants.AudioQuality
import com.danrey.lalasi.constants.AudioQualityKey
import com.danrey.lalasi.constants.CrossmixMode
import com.danrey.lalasi.constants.CrossmixModeKey
import com.danrey.lalasi.constants.playable
import com.danrey.lalasi.eq.EqPresets
import com.danrey.lalasi.eq.EqSettings
import com.danrey.lalasi.playback.audio.AudioOutputs
import com.danrey.lalasi.ui.component.damageBoundary
import com.danrey.lalasi.ui.component.pressBounce
import com.danrey.lalasi.ui.theme.InterFontFamily
import com.danrey.lalasi.utils.rememberEnumPreference
import com.danrey.lalasi.utils.rememberPreference
import com.danrey.lalasi.viewmodels.AudioLabViewModel
import kotlin.math.roundToInt
import kotlin.math.sin

private val RowGroup get() = lalasiAdapt(0xFF1C1C1E)
private val Secondary = Color(0xFF8E8E93)

@Composable
fun AudioLabScreen(
    navController: NavController,
    viewModel: AudioLabViewModel = hiltViewModel(),
) {
    val eq by viewModel.eq.collectAsState()
    val presets by viewModel.presets.collectAsState()
    val reverb by viewModel.reverb.collectAsState()
    val loudness by viewModel.loudness.collectAsState()
    val spatial by viewModel.spatial.collectAsState()
    val playback by viewModel.playback.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val (quality) = rememberEnumPreference(AudioQualityKey, defaultValue = AudioQuality.AUTO)
    val order by viewModel.sources.order.collectAsState()
    val modules by viewModel.sources.entries.collectAsState()
    val youTubeOn by viewModel.sources.youTubeEnabled.collectAsState()
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.sources.refresh() }
    // whichever provider plays first sets what "quality" means
    val firstProvider = order.firstOrNull { id ->
        if (id == com.danrey.lalasi.modules.SourceModules.YouTubeId) youTubeOn
        else modules.any { it.module.info.id == id && it.enabled && it.account is com.danrey.lalasi.module.AccountState.SignedIn }
    }
    val (mode) = rememberEnumPreference(CrossmixModeKey, defaultValue = CrossmixMode.AUTO_CROSSFADE)

    val effects = activeEffects(reverb, loudness, spatial, playback, volume)

    val outputs = LocalPlayerConnection.current?.service?.audioOutputs
    val devices by remember(outputs) { outputs?.devices ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList()) }.collectAsState()
    val usbDac = devices.firstOrNull { it.kind == AudioOutputs.Kind.USB }
    val usbName = usbDac?.let { it.name.ifBlank { stringResource(R.string.audiopass_kind_usb) } }
    val passStatus = when {
        usbName != null -> usbName
        devices.any { it.isActive && it.kind == AudioOutputs.Kind.BLUETOOTH } -> stringResource(R.string.audiopass_status_bluetooth)
        else -> stringResource(R.string.audiopass_status_none)
    }

    val processing = eq.enabled || effects.isNotEmpty()
    val summary = listOfNotNull(
        if (eq.enabled) "Equalizer" else null,
        if (effects.isNotEmpty()) "${effects.size} efek" else null,
    )

    AudioLabPage(
        title = LabTitle("Lab", "Audio"),
        onBack = { navController.navigateUp() },
    ) {
        LabHero(
            active = processing,
            headline = if (processing) "Pengolahan aktif" else "Suara asli",
            detail = if (summary.isEmpty()) "Equalizer dan efek sedang mati" else summary.joinToString(" · ") + " menyala",
        )
        Spacer(Modifier.height(8.dp))

        LabSheetTitle("Suara")
        RowGroupCard {
            LabRow("Equalizer", equalizerStatus(eq, presets.associate { it.id to it.name }), GlyphSliders, Color(0xFFFF9F0A), Color(0xFFFF6B00), { navController.navigate("equalizer") })
            LabRow("Efek Audio", effectsStatus(effects), GlyphSparkle, Color(0xFFBF5AF2), Color(0xFF7D3CF0), { navController.navigate("audio_fx") }, last = true)
        }

        LabSheetTitle("Pemutaran")
        RowGroupCard {
            LabRow("Kualitas Audio", qualityName(quality, firstProvider), GlyphWave, Color(0xFF0A84FF), Color(0xFF0050D0), { navController.navigate("audio_quality") })
            LabRow(
                "Transisi",
                when (mode.playable()) {
                    CrossmixMode.OFF -> "Mati"
                    CrossmixMode.AUTO_CROSSFADE -> "Otomatis"
                    CrossmixMode.MANUAL_CROSSFADE -> "Manual"
                    CrossmixMode.CROSSMIX -> "CrossMix"
                },
                GlyphCross, Color(0xFF30D158), Color(0xFF12A53D), { navController.navigate("crossmix_settings") },
            )
            LabRow(stringResource(R.string.audiopass_hub_title), passStatus, GlyphPlug, Color(0xFFFF375F), Color(0xFFD0124A), { navController.navigate("audio_pass") }, last = true)
        }
    }
}

/** the effects status, the first names then "dan 2 lainnya" */
private fun effectsStatus(effects: List<ActiveEffect>): String {
    val names = effects.map { it.label }
    return when {
        names.isEmpty() -> "Mati"
        names.size <= 2 -> names.joinToString(", ")
        else -> names.take(2).joinToString(", ") + " dan ${names.size - 2} lainnya"
    }
}

private fun equalizerStatus(eq: EqSettings, userPresets: Map<String, String>): String {
    if (!eq.enabled) return "Mati"
    val curve = eq.presetId?.let { id -> EqPresets.BuiltIn.firstOrNull { it.id == id }?.name ?: userPresets[id] } ?: "Kustom"
    val correction = eq.correction?.takeIf { eq.correctionEnabled }?.model
    return if (correction != null) "$curve · $correction" else curve
}

private fun qualityName(q: AudioQuality, firstProvider: String?): String = when (firstProvider) {
    com.danrey.lalasi.module.monochrome.MonochromeModule.Id -> "Monochrome · Lossless"
    com.danrey.lalasi.module.applemusic.AppleMusicModule.Id -> "Apple Music · AAC"
    else -> "YouTube · " + when (q) {
        AudioQuality.LOW -> "Rendah"
        AudioQuality.MEDIUM -> "Sedang"
        AudioQuality.HIGH -> "Tinggi"
        else -> "Otomatis"
    }
}

/** the sketch's own clock: still unless it's on screen and something should move */
@Composable
private fun sketchTime(moving: () -> Boolean = { true }): androidx.compose.runtime.State<Float> {
    val onScreen = rememberOnScreen()
    val held = rememberPicturesHeld(onScreen)
    return rememberLabTime(held, moving)
}

/** LALASI's own card at the top: its purple to orange, a spectrum along the bottom that moves while sound is being processed and the song is playing */
@Composable
private fun LabHero(active: Boolean, headline: String, detail: String) {
    val time = sketchTime { active && com.danrey.lalasi.eq.audio.AudioTap.isLive }
    Box(
        Modifier
            .fillMaxWidth()
            // grows with its words (large font sizes, a long summary) instead of cutting them at a fixed 136 dp
            .heightIn(min = 136.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF4B2FB8), Color(0xFF9140D6), Color(0xFFFF7A45)))),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val bars = 28
            val gap = 4.dp.toPx()
            val bw = (size.width - gap * (bars - 1)) / bars
            val t = time.value
            for (i in 0 until bars) {
                val p = i / (bars - 1f)
                val shape = 0.5f + 0.5f * sin(p * 7f + 1f)
                val live = if (active) 0.5f + 0.5f * sin(t * 3.1f + p * 9f) * sin(t * 1.7f + p * 4f) else 0f
                val h = size.height * (0.10f + 0.40f * shape * (0.35f + 0.65f * live))
                drawRoundRect(LalasiInk.copy(alpha = 0.22f), Offset(i * (bw + gap), size.height - h), GeoSize(bw, h), CornerRadius(bw / 2f))
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 20.dp)) {
            Text("LALASI", fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.12.em, color = LalasiInk.copy(alpha = 0.7f), maxLines = 1)
            Spacer(Modifier.height(6.dp))
            Text(headline, fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.02).em, color = LalasiInk, maxLines = 2)
            Spacer(Modifier.height(4.dp))
            Text(detail, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp, letterSpacing = 0.em, color = LalasiInk.copy(alpha = 0.82f), maxLines = 3)
        }
    }
}

@Composable
private fun RowGroupCard(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(RowGroup), content = content)
}

/** one row: a coloured tile with a white glyph, the name, its state in grey and the chevron; it lights up while pressed */
@Composable
private fun LabRow(
    title: String,
    detail: String,
    glyph: Int,
    tileTop: Color,
    tileBottom: Color,
    onClick: () -> Unit,
    last: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(if (pressed) lalasiAdapt(0xFF2C2C2E) else Color.Transparent)
                .clickable(interaction, null, onClick = onClick)
                .heightIn(min = 56.dp)
                .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(Brush.verticalGradient(listOf(tileTop, tileBottom))),
                contentAlignment = Alignment.Center,
            ) { Canvas(Modifier.size(20.dp)) { drawGlyph(glyph) } }
            Spacer(Modifier.width(14.dp))
            Text(title, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = 0.em, color = LalasiInk, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            // a long state ("Monochrome · Lossless") wraps to a second line instead of being cut with "..."
            Text(
                detail, fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.em, color = Secondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 168.dp),
            )
            Spacer(Modifier.width(6.dp))
            Icon(painterResource(R.drawable.navigate_next), contentDescription = null, tint = Color(0xFF48484A), modifier = Modifier.size(20.dp))
        }
        if (!last) Box(Modifier.padding(start = 64.dp).fillMaxWidth().height(0.5.dp).background(Color(0xFF38383A)))
    }
}

private const val GlyphSliders = 0
private const val GlyphSparkle = 1
private const val GlyphWave = 2
private const val GlyphCross = 3
private const val GlyphPlug = 4

/** the white glyphs on the tiles, drawn at the tile's size so they stay crisp */
private fun DrawScope.drawGlyph(kind: Int) {
    val s = size.minDimension
    val white = LalasiInk
    val stroke = 1.8.dp.toPx()
    when (kind) {
        GlyphSliders -> {
            val knobs = floatArrayOf(0.66f, 0.34f, 0.55f)
            for (i in 0..2) {
                val x = s * (0.2f + 0.3f * i)
                drawLine(white, Offset(x, s * 0.08f), Offset(x, s * 0.92f), stroke, StrokeCap.Round)
                drawCircle(white, s * 0.13f, Offset(x, s * knobs[i]))
            }
        }
        GlyphSparkle -> {
            val p = Path().apply {
                moveTo(s * 0.5f, s * 0.04f)
                quadraticBezierTo(s * 0.56f, s * 0.44f, s * 0.96f, s * 0.5f)
                quadraticBezierTo(s * 0.56f, s * 0.56f, s * 0.5f, s * 0.96f)
                quadraticBezierTo(s * 0.44f, s * 0.56f, s * 0.04f, s * 0.5f)
                quadraticBezierTo(s * 0.44f, s * 0.44f, s * 0.5f, s * 0.04f)
                close()
            }
            drawPath(p, white)
        }
        GlyphWave -> {
            val heights = floatArrayOf(0.3f, 0.62f, 0.95f, 0.52f, 0.26f)
            for (i in 0..4) {
                val x = s * (0.1f + 0.2f * i)
                val h = s * heights[i]
                drawLine(white, Offset(x, s / 2f - h / 2f), Offset(x, s / 2f + h / 2f), stroke * 1.3f, StrokeCap.Round)
            }
        }
        GlyphCross -> {
            val a = Path().apply { moveTo(s * 0.04f, s * 0.14f); cubicTo(s * 0.5f, s * 0.14f, s * 0.5f, s * 0.86f, s * 0.96f, s * 0.86f) }
            val b = Path().apply { moveTo(s * 0.04f, s * 0.86f); cubicTo(s * 0.5f, s * 0.86f, s * 0.5f, s * 0.14f, s * 0.96f, s * 0.14f) }
            drawPath(a, white, style = Stroke(stroke, cap = StrokeCap.Round))
            drawPath(b, white.copy(alpha = 0.7f), style = Stroke(stroke, cap = StrokeCap.Round))
        }
        else -> {
            val mid = s / 2f
            drawRoundRect(white, Offset(0f, mid - s * 0.17f), GeoSize(s * 0.2f, s * 0.34f), CornerRadius(1.5.dp.toPx()), style = Stroke(stroke))
            drawRoundRect(white, Offset(s * 0.2f, mid - s * 0.28f), GeoSize(s * 0.4f, s * 0.56f), CornerRadius(3.dp.toPx()))
            drawLine(white, Offset(s * 0.6f, mid), Offset(s, mid), stroke, StrokeCap.Round)
        }
    }
}
