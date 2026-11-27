// LyricsCodingPlayerStyle.kt
//
// "Lyrics Coding": the sung lyrics typed out as code in a dark editor. Every line of the song is wrapped in a statement of the chosen
// language (print("..."), const x = "...", SELECT "..." AS x, ...) and coloured like a syntax theme; the line being sung is typed
// character by character with a caret, the lines already sung stay above it and fade (and blur) with distance, the lines to come
// are not there yet. A camera (slide, cinema, 3D, focus, zoom) moves the lines, a few text effects (blank, error, sensor, glitch,
// neon, terminal) dress them, and every third line a VS Code style suggestion popup opens over the word being typed. After the last
// line the whole page tilts back into a 3D finale.
//
// The lyrics come from the player (the app's own lyrics source and offset), the controls are the ones Aurora uses, so this file holds only
// what is new. Its choices (language, colours, camera, effect, size) are kept in preferences and changed from the </> button.

package com.danrey.lalasi.ui.player.styles

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danrey.lalasi.LocalPlayerConnection
import com.danrey.lalasi.constants.LyricsCodingCameraKey
import com.danrey.lalasi.constants.LyricsCodingFxKey
import com.danrey.lalasi.constants.LyricsCodingLangKey
import com.danrey.lalasi.constants.LyricsCodingSizeKey
import com.danrey.lalasi.constants.LyricsCodingThemeKey
import com.danrey.lalasi.constants.PlayerStyle
import com.danrey.lalasi.lyrics.LyricsEntry
import com.danrey.lalasi.lyrics.LyricsUtils
import com.danrey.lalasi.utils.rememberEnumPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.exp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// ---- choices ------------------------------------------------------------------------------

internal enum class CodeLang(val label: String, val ext: String) {
    PY("Python", "py"), JS("JavaScript", "js"), CSS("CSS", "css"), RS("Rust", "rs"), SQL("SQL", "sql"), GO("Go", "go"),
}

internal enum class CodeTheme(val label: String) {
    MONOKAI("Monokai"), DRACULA("Dracula"), NORD("Nord"), GRUVBOX("Gruvbox"), MONO("Mono"),
}

internal enum class CodeCamera(val label: String) {
    SLIDE("Geser"), CINEMA("Sinema"), DEEP("3D Dynamic"), FOCUS("Fokus"), ZOOM("Zoom"),
}

internal enum class CodeFx(val label: String) {
    NORMAL("Normal"), BLANK("Blank"), ERROR("Error"), SENSOR("Sensor"), GLITCH("Glitch"), NEON("Neon"), TERMINAL("Terminal"),
}

internal enum class CodeSize(val label: String, val scale: Float) {
    SMALL("Kecil", 0.86f), MEDIUM("Sedang", 1f), LARGE("Besar", 1.2f),
}

// ---- colours ------------------------------------------------------------------------------

/** the syntax colours of one theme: k keyword, f function, s string, v variable, n number, c comment, o operator, t type, p plain */
@Immutable
internal data class CodePalette(
    val bg: Color,
    val k: Color,
    val f: Color,
    val s: Color,
    val v: Color,
    val n: Color,
    val c: Color,
    val o: Color,
    val t: Color,
    val p: Color,
    val dim: Color,
    val cur: Color,
    val accent: Color,
) {
    fun of(cls: Char): Color = when (cls) {
        'k' -> k
        'f' -> f
        's' -> s
        'v' -> v
        'n' -> n
        'c' -> c
        'o' -> o
        't' -> t
        else -> p
    }
}

private fun hx(rgb: Long) = Color(0xFF000000L or rgb)

private fun codePalette(theme: CodeTheme, fx: CodeFx): CodePalette {
    val base = when (theme) {
        CodeTheme.MONOKAI -> CodePalette(
            bg = hx(0x0D0D0F), k = hx(0xF92672), f = hx(0xA6E22E), s = hx(0xE6DB74), v = hx(0xF8F8F2), n = hx(0xAE81FF),
            c = hx(0x75715E), o = hx(0xF92672), t = hx(0x66D9EF), p = hx(0xA59F85), dim = hx(0x4A4A4A), cur = hx(0xF8F8F2), accent = hx(0x4FC1FF),
        )
        CodeTheme.DRACULA -> CodePalette(
            bg = hx(0x14151C), k = hx(0xFF79C6), f = hx(0x50FA7B), s = hx(0xF1FA8C), v = hx(0xF8F8F2), n = hx(0xBD93F9),
            c = hx(0x6272A4), o = hx(0xFF79C6), t = hx(0x8BE9FD), p = hx(0x9AA0C3), dim = hx(0x3D4056), cur = hx(0xF8F8F2), accent = hx(0xBD93F9),
        )
        CodeTheme.NORD -> CodePalette(
            bg = hx(0x0E1219), k = hx(0x81A1C1), f = hx(0x88C0D0), s = hx(0xA3BE8C), v = hx(0xD8DEE9), n = hx(0xB48EAD),
            c = hx(0x4C566A), o = hx(0x81A1C1), t = hx(0x8FBCBB), p = hx(0x7B88A1), dim = hx(0x2F3846), cur = hx(0xD8DEE9), accent = hx(0x88C0D0),
        )
        CodeTheme.GRUVBOX -> CodePalette(
            bg = hx(0x151412), k = hx(0xFB4934), f = hx(0xB8BB26), s = hx(0xFABD2F), v = hx(0xEBDBB2), n = hx(0xD3869B),
            c = hx(0x7C6F64), o = hx(0xFE8019), t = hx(0x83A598), p = hx(0xA89984), dim = hx(0x40392F), cur = hx(0xEBDBB2), accent = hx(0xFABD2F),
        )
        CodeTheme.MONO -> CodePalette(
            bg = hx(0x000000), k = hx(0xFFFFFF), f = hx(0xD6D6D6), s = hx(0x9A9A9A), v = hx(0xFFFFFF), n = hx(0xBDBDBD),
            c = hx(0x555555), o = hx(0xFFFFFF), t = hx(0xD6D6D6), p = hx(0x777777), dim = hx(0x3A3A3A), cur = hx(0xF8F8F2), accent = hx(0xDDDDDD),
        )
    }
    return when (fx) {
        CodeFx.ERROR -> base.copy(
            k = hx(0xFF5252), f = hx(0xFF8A80), s = hx(0xFF6E6E), v = hx(0xFFB4B4), n = hx(0xFF9E80), c = hx(0xA05A5A),
            o = hx(0xFF5252), t = hx(0xFF7B7B), p = hx(0xC98080), dim = hx(0x4A2A2A), cur = hx(0xFF5252),
        )
        CodeFx.TERMINAL -> base.copy(
            bg = hx(0x020A04), k = hx(0x39FF6A), f = hx(0x39FF6A), s = hx(0x8DFFA8), v = hx(0xB6FFC8), n = hx(0x39FF6A), c = hx(0x1F7A38),
            o = hx(0x39FF6A), t = hx(0x8DFFA8), p = hx(0x2FBF55), dim = hx(0x14502A), cur = hx(0x39FF6A),
        )
        else -> base
    }
}

// ---- the lyrics as code -------------------------------------------------------------------

// @S is the lyric as a string literal, @I a name made from its first words, @T the lyric itself
private val CodeTemplates: Map<CodeLang, List<String>> = mapOf(
    CodeLang.PY to listOf(
        "<v>@I</v> <o>=</o> <s>@S</s>",
        "<f>print</f>(<s>@S</s>)",
        "<k>await</k> <f>sing</f>(<s>@S</s>)",
        "<c># @T</c>",
        "<k>yield</k> <s>@S</s>",
    ),
    CodeLang.JS to listOf(
        "<k>const</k> <v>@I</v> <o>=</o> <s>@S</s>;",
        "<v>console</v>.<f>log</f>(<s>@S</s>);",
        "<k>await</k> <f>sing</f>(<s>@S</s>);",
        "<c>// @T</c>",
        "<k>return</k> <s>@S</s>;",
    ),
    CodeLang.CSS to listOf(
        "<v>.@I</v> { <k>content</k>: <s>@S</s>; }",
        "<v>::after</v> { <k>content</k>: <s>@S</s>; }",
        "<k>--@I</k>: <s>@S</s>;",
        "<c>/* @T */</c>",
        "<v>:root</v> { <k>--line</k>: <s>@S</s>; }",
    ),
    CodeLang.RS to listOf(
        "<k>let</k> <v>@I</v> <o>=</o> <s>@S</s>;",
        "<f>println!</f>(<s>@S</s>);",
        "<v>sky</v>.<f>compile</f>(<s>@S</s>);",
        "<c>// @T</c>",
        "<k>return</k> <t>Some</t>(<s>@S</s>);",
    ),
    CodeLang.SQL to listOf(
        "<k>SELECT</k> <s>@S</s> <k>AS</k> <v>@I</v>;",
        "<k>INSERT INTO</k> <v>lyrics</v> <k>VALUES</k> (<s>@S</s>);",
        "<c>-- @T</c>",
        "<k>UPDATE</k> <v>heart</v> <k>SET</k> <v>say</v> <o>=</o> <s>@S</s>;",
        "<k>WHERE</k> <v>night</v> <o>=</o> <s>@S</s>;",
    ),
    CodeLang.GO to listOf(
        "<v>@I</v> <o>:=</o> <s>@S</s>",
        "<v>fmt</v>.<f>Println</f>(<s>@S</s>)",
        "<c>// @T</c>",
        "<k>return</k> <s>@S</s>",
        "<k>go</k> <f>sing</f>(<s>@S</s>)",
    ),
)

// words the suggestion popup knows besides the lyrics: keywords first, then functions
private val CodeKeywords: Map<CodeLang, Pair<String, String>> = mapOf(
    CodeLang.PY to ("await yield def class import return lambda async while with" to "print sing range len enumerate sleep open"),
    CodeLang.JS to ("await const return async function let class import export" to "console sing setTimeout Promise fetch map filter"),
    CodeLang.CSS to ("content color display margin padding transform animation background opacity position" to ""),
    CodeLang.RS to ("let fn match impl mut return loop async" to "println sing compile Some None Option unwrap clone"),
    CodeLang.SQL to ("SELECT INSERT UPDATE WHERE FROM VALUES SET JOIN LIMIT" to "COUNT MAX MIN COALESCE"),
    CodeLang.GO to ("func return defer range chan select package import" to "fmt Println Sprintf sing make append"),
)

private val CodeSegRegex = Regex("<(\\w)>([\\s\\S]*?)</\\1>|([^<]+)")
private val CodeVarRegex = Regex("@([SIT])")
private val CodeWordRegex = Regex("[\\p{L}\\p{N}_]+")
private val CodeSpaceRegex = Regex("\\s+")
private val CodeIdentRegex = Regex("[^A-Za-z0-9_]")

/** one lyric as its coloured pieces; [tokStart] is where the suggestion popup opens on this line, -1 when it doesn't */
private class CodeLineData(val segs: List<Pair<Char, String>>, val full: String, val tokStart: Int)

private fun codeSegments(i: Int, text: String, lang: CodeLang): List<Pair<Char, String>> {
    val ident = text.split(CodeSpaceRegex).take(3).joinToString("_").replace(CodeIdentRegex, "").ifEmpty { "line" }
    val literal = "\"" + text.replace('"', '\'') + "\""
    val templates = CodeTemplates.getValue(lang)
    val out = ArrayList<Pair<Char, String>>()
    for (m in CodeSegRegex.findAll(templates[i % templates.size])) {
        val cls = m.groups[1]?.value?.firstOrNull() ?: 'p'
        val body = m.groups[2]?.value ?: m.groups[3]?.value ?: ""
        out.add(cls to body.replace(CodeVarRegex) { mr ->
            when (mr.groupValues[1]) {
                "S" -> literal
                "I" -> ident
                else -> text
            }
        })
    }
    return out
}

/** the word of this line's text that the popup opens on: the longest one, well into the string, after its second word */
private fun codePickToken(segs: List<Pair<Char, String>>, full: String): Int {
    var used = 0
    var a = -1
    var b = -1
    for ((cls, piece) in segs) {
        if (a < 0 && (cls == 's' || cls == 'c')) {
            val quote = if (cls == 's') 1 else 0
            a = used + quote
            b = used + piece.length - quote
        }
        used += piece.length
    }
    var words = 0
    var bestStart = -1
    var bestLen = 0
    for (m in CodeWordRegex.findAll(full)) {
        val start = m.range.first
        if (start < a || start >= b) continue
        if (m.value.length >= 4 && words >= 2 && (start - a) >= (b - a) * 0.4f && m.value.length > bestLen) {
            bestStart = start
            bestLen = m.value.length
        }
        words++
    }
    return bestStart
}

private fun buildCodeLines(lines: List<LyricsEntry>, lang: CodeLang): List<CodeLineData> =
    lines.mapIndexed { i, entry ->
        val segs = codeSegments(i, entry.text.trim(), lang)
        val full = segs.joinToString("") { it.second }
        CodeLineData(segs, full, if (i >= 2 && i % 3 == 2) codePickToken(segs, full) else -1)
    }

/** what a half-typed line shows: the typed part coloured, the rest there but invisible, so the line never changes its shape while it is typed */
private fun codeAnnotated(data: CodeLineData, typed: Int, palette: CodePalette, now: Boolean, fx: CodeFx): AnnotatedString =
    buildAnnotatedString {
        var used = 0
        val glow = now && fx != CodeFx.TERMINAL
        for ((cls, piece) in data.segs) {
            val take = (typed - used).coerceIn(0, piece.length)
            if (take > 0) {
                val base = palette.of(cls)
                val masked = fx == CodeFx.SENSOR && !now && (cls == 's' || cls == 'c')
                withStyle(
                    SpanStyle(
                        color = if (masked) Color.Transparent else base,
                        background = if (masked) palette.p else Color.Unspecified,
                        fontStyle = if (cls == 'c') FontStyle.Italic else null,
                        shadow = if (glow) {
                            Shadow(base.copy(alpha = if (fx == CodeFx.NEON) 0.95f else 0.55f), Offset.Zero, if (fx == CodeFx.NEON) 34f else 14f)
                        } else if (fx == CodeFx.GLITCH && now) {
                            Shadow(Color(0xBFFF005A), Offset(3f, 0f), 0f)
                        } else null,
                    ),
                ) { append(piece.substring(0, take)) }
            }
            if (take < piece.length) withStyle(SpanStyle(color = Color.Transparent)) { append(piece.substring(take)) }
            used += piece.length
        }
    }

/** a count of typed characters never ends between the two halves of an emoji */
private fun codeSafeCut(full: String, n: Int): Int =
    if (n in 1 until full.length && full[n - 1].isHighSurrogate()) n + 1 else n

// ---- the camera ---------------------------------------------------------------------------

private class CodePose(val tx: Float, val scale: Float, val ry: Float)

/** where a line sits, [d] lines above the sung one */
private fun codeLinePose(mode: CodeCamera, d: Int, i: Int, finale: Boolean): CodePose {
    if (finale) return CodePose(0f, if (d <= 0) 1.04f else 1100f / (1100f + min(d, 10) * 48f), 0f)
    if (d <= 0) return CodePose(0f, 1f, 0f)
    val k = min(d, 6).toFloat()
    return when (mode) {
        CodeCamera.SLIDE -> CodePose(-k * 4f, 1f, 0f)
        CodeCamera.CINEMA -> CodePose(-k * 7f, 1100f / (1100f + k * 65f), -k * 0.8f)
        CodeCamera.DEEP -> {
            val curve = sin((i + 1) * 0.32f)
            CodePose(curve * k * 5f - k * 2f, 1100f / (1100f + k * 72f), curve * 3f - k * 0.65f)
        }
        CodeCamera.ZOOM -> CodePose(0f, max(0.8f, 1f - k * 0.04f), 0f)
        CodeCamera.FOCUS -> CodePose(0f, 1f - min(k, 3f) * 0.015f, 0f)
    }
}

/** how the whole page is turned: rotationY, rotationX, scale */
private fun codePagePose(mode: CodeCamera): FloatArray = when (mode) {
    CodeCamera.CINEMA -> floatArrayOf(-8f, 3f, 1.02f)
    CodeCamera.DEEP -> floatArrayOf(-10f, 2.5f, 1.015f)
    CodeCamera.ZOOM -> floatArrayOf(0f, 0f, 1.06f)
    else -> floatArrayOf(0f, 0f, 1f)
}

/** the moving camera on top of a mode's fixed pose: drift in dp, turns in degrees, scale 1 = no zoom */
private class CodeCam(val tx: Float, val ty: Float, val scale: Float, val ry: Float, val rx: Float, val rz: Float)

/**
 * The camera's motion for [phase] (0..1, one full loop of its slow drift) and [punch] (1 when a new line starts, easing to 0):
 * every line pushes the camera in a little and lets it settle back, and in between it keeps breathing in and out and drifting.
 */
private fun codeCameraMotion(mode: CodeCamera, phase: Float, punch: Float): CodeCam {
    val w = (phase * 2.0 * PI).toFloat()
    val s1 = sin(w)
    val c1 = cos(w)
    val s2 = sin(2f * w + 1f)
    val breath = 0.5f - 0.5f * cos(w)
    return when (mode) {
        CodeCamera.SLIDE -> CodeCam(s1 * 8f, s2 * 2f, 1f + 0.012f * s2 + 0.02f * punch, 0f, 0f, 0f)
        CodeCamera.CINEMA -> CodeCam(s1 * 11f - punch * 5f, c1 * 3f, 1f + 0.045f * breath + 0.035f * punch, s1 * 1.8f, s2 * 0.8f, 0f)
        CodeCamera.DEEP -> CodeCam(s1 * 13f, c1 * 5f, 1f + 0.03f * sin(2f * w) + 0.045f * punch, s1 * 5.5f - punch * 2f, c1 * 2.6f + punch * 1.5f, s2 * 0.9f)
        CodeCamera.FOCUS -> CodeCam(0f, s2 * 1.5f, 1f + 0.075f * punch + 0.01f * s1, 0f, 0f, 0f)
        CodeCamera.ZOOM -> CodeCam(s1 * 4f, c1 * 3f, 1.02f + 0.08f * (0.5f - 0.5f * cos(2f * w)) + 0.09f * punch, 0f, 0f, 0f)
    }
}

/** the slow loop of the camera's drift; a still card (a selector preview, or no lyrics to follow) has none, so nothing runs */
@Composable
private fun rememberCodeCameraPhase(running: Boolean): State<Float> {
    if (!running) return remember { mutableFloatStateOf(0f) }
    val transition = rememberInfiniteTransition(label = "codeCamera")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(18_000, easing = LinearEasing)),
        label = "codeCameraPhase",
    )
}

// ---- the suggestion popup -----------------------------------------------------------------

private class CodeIsRow(val word: String, val kind: Char)

private class CodeIsToken(val start: Int, val pre: String, val word: String, val done: Boolean)

private class CodeIsPopup(
    val rows: List<CodeIsRow>,
    val prefixLen: Int,
    val word: String,
    val kind: Char,
    val lineNo: Int,
    val x: Float,
    val caretTop: Float,
    val caretBottom: Float,
)

private fun codeIsWordChar(c: Char) = c.isLetterOrDigit() || c == '_'

/** the word being typed when [n] characters are out, with the part typed so far */
private fun codeTokenAt(full: String, n: Int): CodeIsToken? {
    if (n <= 0 || n > full.length || !codeIsWordChar(full[n - 1])) return null
    var a = n - 1
    while (a > 0 && codeIsWordChar(full[a - 1])) a--
    var e = n
    while (e < full.length && codeIsWordChar(full[e])) e++
    if (e - a < 3) return null
    return CodeIsToken(a, full.substring(a, n), full.substring(a, e), n >= e)
}

private fun codeKindAt(segs: List<Pair<Char, String>>, pos: Int): Char {
    var used = 0
    for ((cls, piece) in segs) {
        if (pos < used + piece.length) {
            return when (cls) {
                'k' -> 'k'
                'f' -> 'f'
                's', 'c' -> 'w'
                else -> 'v'
            }
        }
        used += piece.length
    }
    return 'v'
}

private fun codePool(lines: List<LyricsEntry>, lang: CodeLang): List<CodeIsRow> {
    val seen = LinkedHashMap<String, CodeIsRow>()
    fun add(word: String, kind: Char) {
        val key = word.lowercase()
        if (word.length >= 3 && !seen.containsKey(key)) seen[key] = CodeIsRow(word, kind)
    }
    val (keywords, functions) = CodeKeywords.getValue(lang)
    keywords.split(' ').forEach { if (it.isNotEmpty()) add(it, 'k') }
    functions.split(' ').forEach { if (it.isNotEmpty()) add(it, 'f') }
    for (entry in lines) for (m in CodeWordRegex.findAll(entry.text)) add(m.value, 'w')
    return seen.values.toList()
}

private fun codeSuggestions(tk: CodeIsToken, kind: Char, pool: List<CodeIsRow>): List<CodeIsRow> {
    val prefix = tk.pre.lowercase()
    val whole = tk.word.lowercase()
    val rows = ArrayList<CodeIsRow>()
    rows.add(CodeIsRow(tk.word, kind))
    for (e in pool) {
        if (rows.size >= 7) break
        val w = e.word.lowercase()
        if (w != whole && w.startsWith(prefix)) rows.add(e)
    }
    if (rows.size < 4) {
        for (e in pool) {
            if (rows.size >= 5) break
            val w = e.word.lowercase()
            if (w != whole && !w.startsWith(prefix) && w.contains(prefix)) rows.add(e)
        }
    }
    return rows
}

@Composable
private fun CodeIntelliSenseLayer(popup: State<CodeIsPopup?>, palette: CodePalette, stageWidth: Dp, stageHeight: Dp) {
    val p = popup.value ?: return
    val shape = RoundedCornerShape(5.dp)
    val width = min(300f, stageWidth.value - 16f).dp
    val height = min(230f, p.rows.size * 25f + 6f + 28f).dp
    Column(
        Modifier
            .offset {
                val w = width.roundToPx()
                val h = height.roundToPx()
                val m = 8.dp.roundToPx()
                val gap = 3.dp.roundToPx()
                val maxY = stageHeight.roundToPx()
                var y = p.caretBottom.roundToInt() + gap
                if (y + h > maxY - m) y = p.caretTop.roundToInt() - h - gap
                val x = p.x.roundToInt().coerceIn(m, max(m, stageWidth.roundToPx() - w - m))
                IntOffset(x, y.coerceIn(m, max(m, maxY - h - m)))
            }
            .width(width)
            .clip(shape)
            .background(Color(0xFA1E1E1E))
            .border(1.dp, Color.White.copy(alpha = 0.13f), shape),
    ) {
        Column(Modifier.padding(vertical = 3.dp)) {
            p.rows.forEachIndexed { index, row ->
                val selected = index == 0
                val typedPrefix = p.word.take(p.prefixLen).lowercase()
                val at = row.word.lowercase().indexOf(typedPrefix).coerceIn(0, row.word.length)
                val name = buildAnnotatedString {
                    append(row.word.substring(0, at))
                    withStyle(SpanStyle(color = if (selected) Color.White else palette.accent, fontWeight = FontWeight.SemiBold)) {
                        append(row.word.substring(at, min(row.word.length, at + p.prefixLen)))
                    }
                    append(row.word.substring(min(row.word.length, at + p.prefixLen)))
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(25.dp)
                        .background(if (selected) Color(0xFF094771) else Color.Transparent)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                        when (row.kind) {
                            'f' -> Box(Modifier.size(10.dp).background(Color(0xFFB180D7), RoundedCornerShape(2.dp)))
                            'v' -> Box(Modifier.size(10.dp).background(Color(0xFF75BEFF), RoundedCornerShape(2.dp)))
                            'k' -> Text("≡", color = Color(0xFFC5C5C5), fontSize = 13.sp, maxLines = 1)
                            else -> Text("ab", color = Color(0xFFC5C5C5), fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                    Text(
                        text = name,
                        color = if (selected) Color.White else Color(0xFFCCCCCC),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = when (row.kind) { 'f' -> "method"; 'v' -> "variable"; 'k' -> "keyword"; else -> "lyric" },
                        color = if (selected) Color(0xFFD7EAF7) else Color(0xFF999999),
                        fontSize = 11.sp,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.10f)))
        val detail = buildAnnotatedString {
            val label = when (p.kind) { 'f' -> "(method)"; 'k' -> "(keyword)"; 'v' -> "(variable)"; else -> "(lirik)" }
            withStyle(SpanStyle(color = Color(0xFF4FC1FF))) { append(label) }
            append(" ")
            append(
                when (p.kind) {
                    'f' -> "${p.word}(…): void"
                    'k' -> p.word
                    'v' -> "${p.word}: str"
                    else -> "${p.word}: string · baris ${p.lineNo}"
                },
            )
        }
        Text(
            text = detail,
            color = Color(0xFFAAAAAA),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
        )
    }
}

// ---- small clocks -------------------------------------------------------------------------

/** the caret's on / off, twice a second; it stays on while a line is typed */
@Composable
private fun rememberCodeBlink(running: Boolean): State<Boolean> {
    val on = remember { mutableStateOf(true) }
    LaunchedEffect(running) {
        on.value = true
        if (!running) return@LaunchedEffect
        while (true) {
            delay(600)
            on.value = !on.value
        }
    }
    return on
}

/** the sideways jolt of the error and glitch effects: still for most of 2.8 s, then four quick steps */
@Composable
private fun rememberCodeJolt(running: Boolean): State<Float> {
    val shift = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(running) {
        shift.floatValue = 0f
        if (!running) return@LaunchedEffect
        while (true) {
            delay(2570)
            for (v in floatArrayOf(-4f, 4f, -2f, 0f)) {
                shift.floatValue = v
                delay(56)
            }
        }
    }
    return shift
}

private val CodeSampleLines: List<LyricsEntry> = listOf(
    "midnight hums inside the screen",
    "cursor blinking like a heartbeat",
    "every line i type is you",
).mapIndexed { i, text -> LyricsEntry(i * 4000L, text) }

private val CodeEmptyLines: List<LyricsEntry> = listOf(LyricsEntry(0L, "belum ada lirik untuk lagu ini"))

// ---- the style ----------------------------------------------------------------------------

@Composable
internal fun LyricsCodingPlayerStyle(actions: StyleActions, modifier: Modifier, preview: Boolean) {
    val tones = rememberStyleTones()
    val track = rememberStyleTrack()
    val r = rememberResponsive()
    val position = rememberLivePosition(animated = !preview)
    val clock = position.withLyricsOffset()
    val duration = rememberDurationMs()
    val lyrics = rememberStyleLyrics(readings = false, load = !preview)
    // a song's lyrics are being looked up: a loading page instead of "no lyrics"; after 15 s without an answer it gives up and says there are none
    var gaveUp by remember(track) { mutableStateOf(false) }
    LaunchedEffect(track, lyrics.loading) {
        gaveUp = false
        if (lyrics.loading) {
            delay(15_000)
            gaveUp = true
        }
    }
    val loading = !preview && lyrics.loading && !gaveUp

    val (lang, setLang) = rememberEnumPreference(LyricsCodingLangKey, CodeLang.PY)
    val (theme, setTheme) = rememberEnumPreference(LyricsCodingThemeKey, CodeTheme.MONOKAI)
    val (camera, setCamera) = rememberEnumPreference(LyricsCodingCameraKey, CodeCamera.SLIDE)
    val (fx, setFx) = rememberEnumPreference(LyricsCodingFxKey, CodeFx.NORMAL)
    val (textSize, setTextSize) = rememberEnumPreference(LyricsCodingSizeKey, CodeSize.MEDIUM)
    var settingsOpen by remember { mutableStateOf(false) }

    val palette = remember(theme, fx) { codePalette(theme, fx) }

    // no synced lyrics: one comment line says so; a selector card with none shows three sample lines
    val hasLyrics = lyrics.lines.isNotEmpty()
    val lines = remember(lyrics.lines, preview) {
        when {
            hasLyrics -> lyrics.lines
            preview -> CodeSampleLines
            else -> CodeEmptyLines
        }
    }
    val fixedIndex = when {
        preview -> min(2, lines.lastIndex)
        !hasLyrics -> 0
        else -> -1
    }
    val codeLines = remember(lines, lang) { buildCodeLines(lines, lang) }
    val pool = remember(lines, lang) { codePool(lines, lang) }

    // the line being sung and how much of it is typed; only the lines read them, so the page is not rebuilt on every frame
    val lineIndex: State<Int> = remember(lines, clock, fixedIndex) {
        derivedStateOf {
            if (fixedIndex >= 0) fixedIndex
            else LyricsUtils.findCurrentLineIndex(lines, clock.positionMs()).coerceAtMost(lines.lastIndex)
        }
    }
    val typed: State<Int> = remember(lines, codeLines, clock, lineIndex, fixedIndex) {
        derivedStateOf {
            val idx = lineIndex.value
            val data = codeLines.getOrNull(idx)
            val entry = lines.getOrNull(idx)
            if (data == null || entry == null) 0
            else {
                val total = data.full.length
                if (fixedIndex >= 0) total
                else {
                    val endMs = lines.getOrNull(idx + 1)?.time ?: (entry.time + 5000L)
                    val span = (endMs - entry.time).toFloat()
                    val typingMs = max(350f, min(span * 0.92f, total / 12f * 1000f))
                    val done = ((clock.positionMs() - entry.time) / typingMs).coerceIn(0f, 1f)
                    codeSafeCut(data.full, (done * total).toInt())
                }
            }
        }
    }
    // the last line is typed: the page tilts back
    val finale: State<Boolean> = remember(lines, codeLines, lineIndex, typed, fixedIndex) {
        derivedStateOf {
            val idx = lineIndex.value
            fixedIndex < 0 && idx >= 0 && idx == lines.lastIndex && typed.value >= (codeLines.getOrNull(idx)?.full?.length ?: Int.MAX_VALUE)
        }
    }

    val fileName = remember(track.title, lang) {
        track.title.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifEmpty { "lyrics" } + "." + lang.ext
    }

    StyleFrame(
        PlayerStyle.LYRICS_CODING, preview, modifier, tones,
        background = {
            Box(Modifier.fillMaxSize().background(palette.bg))
            if (fx == CodeFx.TERMINAL) {
                // the terminal's scan lines
                Box(
                    Modifier.fillMaxSize().drawBehind {
                        val step = 3.dp.toPx()
                        val thin = 1.dp.toPx()
                        var y = 0f
                        while (y < size.height) {
                            drawRect(Color.Black.copy(alpha = 0.22f), Offset(0f, y), Size(size.width, thin))
                            y += step
                        }
                    },
                )
            }
        },
    ) { landscape, menuOpen, setMenuOpen, rotated, setRotated ->
        val insets = styleInsets(landscape)
        val side = r.dp(0.06f, 18.dp, 40.dp)
        val menuButton = r.dp(0.078f, 28.dp, 40.dp)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = side + insets.start, end = side + insets.end, top = insets.top + 12.dp, bottom = insets.bottom + 14.dp),
        ) {
            // the file's name, as the tab of an editor, with the button for the settings beside the ••• menu's room
            Row(
                Modifier.fillMaxWidth().heightIn(min = menuButton).padding(end = menuButton + 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = fileName,
                    color = palette.p,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (!preview) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "</>",
                        color = if (settingsOpen) palette.s else palette.p,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, if (settingsOpen) palette.s else palette.dim, RoundedCornerShape(8.dp))
                            .clickable(remember { MutableInteractionSource() }, null) { settingsOpen = !settingsOpen }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
            if (loading) {
                CodeLoading(
                    palette = palette,
                    title = track.title,
                    fontSize = r.sp(0.05f, 14f, 21f) * textSize.scale,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            } else CodeStage(
                lines = lines,
                codeLines = codeLines,
                pool = pool,
                index = lineIndex,
                typed = typed,
                finale = finale,
                palette = palette,
                fx = fx,
                camera = camera,
                fontSize = r.sp(0.05f, 14f, 21f) * textSize.scale,
                fixed = fixedIndex >= 0,
                preview = preview,
                seekable = hasLyrics && !preview,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            AuroraProgress(position, duration, preview)
            AuroraTimes(position, duration)
            Spacer(Modifier.height(4.dp))
            AuroraTransport(size = r.dp(0.15f, 52.dp, 70.dp), preview = preview)
        }
        if (settingsOpen && !preview) {
            // a tap beside the sheet closes it
            Box(Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null) { settingsOpen = false })
        }
        AnimatedVisibility(
            visible = settingsOpen && !preview,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            CodeSettingsSheet(
                palette = palette,
                lang = lang, onLang = setLang,
                theme = theme, onTheme = setTheme,
                camera = camera, onCamera = setCamera,
                fx = fx, onFx = setFx,
                textSize = textSize, onTextSize = setTextSize,
                bottomInset = insets.bottom,
                onClose = { settingsOpen = false },
            )
        }
        StyleMenu(
            open = menuOpen, onOpenChange = setMenuOpen, tones = tones, actions = actions,
            rotated = rotated, onRotate = setRotated, atBottom = false,
            end = 22.dp + insets.end, edge = if (landscape) 18.dp else insets.top + 20.dp,
            buttonSize = menuButton,
        )
    }
}

// ---- the settings sheet -------------------------------------------------------------------

@Composable
private fun CodeSettingsSheet(
    palette: CodePalette,
    lang: CodeLang, onLang: (CodeLang) -> Unit,
    theme: CodeTheme, onTheme: (CodeTheme) -> Unit,
    camera: CodeCamera, onCamera: (CodeCamera) -> Unit,
    fx: CodeFx, onFx: (CodeFx) -> Unit,
    textSize: CodeSize, onTextSize: (CodeSize) -> Unit,
    bottomInset: Dp,
    onClose: () -> Unit,
) {
    val shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.bg)
            .border(1.dp, palette.dim, shape)
            // a tap on the sheet itself must not reach the scrim under it
            .clickable(remember { MutableInteractionSource() }, null) { }
            .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = bottomInset + 14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Menu", color = palette.cur, fontFamily = FontFamily.Monospace, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
                "Tutup",
                color = palette.p,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                modifier = Modifier
                    .clickable(remember { MutableInteractionSource() }, null) { onClose() }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
            )
        }
        CodeOptionRow("Bahasa", CodeLang.entries, lang, { it.label }, onLang, palette)
        CodeOptionRow("Warna", CodeTheme.entries, theme, { it.label }, onTheme, palette)
        CodeOptionRow("Kamera", CodeCamera.entries, camera, { it.label }, onCamera, palette)
        CodeOptionRow("Efek", CodeFx.entries, fx, { it.label }, onFx, palette)
        CodeOptionRow("Ukuran", CodeSize.entries, textSize, { it.label }, onTextSize, palette)
    }
}

@Composable
private fun <T> CodeOptionRow(
    label: String,
    options: List<T>,
    selected: T,
    name: (T) -> String,
    onSelect: (T) -> Unit,
    palette: CodePalette,
) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = palette.p, fontFamily = FontFamily.Monospace, fontSize = 12.sp, maxLines = 1, modifier = Modifier.width(64.dp))
        Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                val on = option == selected
                Text(
                    text = name(option),
                    color = if (on) palette.s else palette.p,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, if (on) palette.s else palette.dim, RoundedCornerShape(8.dp))
                        .clickable(remember { MutableInteractionSource() }, null) { onSelect(option) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

// ---- the loading page ---------------------------------------------------------------------

/** shown while a song's lyrics are being looked up, as an editor waiting on a call: the call being typed, a caret, and a bar that fills and starts again */
@Composable
private fun CodeLoading(palette: CodePalette, title: String, fontSize: TextUnit, modifier: Modifier = Modifier) {
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(260)
            step++
        }
    }
    val barCells = 14
    val filled = step % (barCells + 1)
    val dots = ".".repeat(step % 4)
    val name = title.replace('"', '\'').trim().ifEmpty { "lyrics" }.let { if (it.length > 26) it.take(25) + "…" else it }
    val caretOn = step % 2 == 0
    val call = buildAnnotatedString {
        withStyle(SpanStyle(color = palette.k)) { append("await ") }
        withStyle(SpanStyle(color = palette.f)) { append("loadLyrics") }
        withStyle(SpanStyle(color = palette.p)) { append("(") }
        withStyle(SpanStyle(color = palette.s)) { append("\"$name\"") }
        withStyle(SpanStyle(color = palette.p)) { append(")") }
        withStyle(SpanStyle(color = if (caretOn) palette.cur else Color.Transparent)) { append(" ▌") }
    }
    val bar = buildAnnotatedString {
        withStyle(SpanStyle(color = palette.p)) { append("[") }
        withStyle(SpanStyle(color = palette.s)) { append("█".repeat(filled)) }
        withStyle(SpanStyle(color = palette.dim)) { append("░".repeat(barCells - filled)) }
        withStyle(SpanStyle(color = palette.p)) { append("]") }
    }
    Box(modifier, contentAlignment = Alignment.CenterStart) {
        Column(verticalArrangement = Arrangement.spacedBy((fontSize.value * 0.5f).dp)) {
            Text(
                text = "// memuat lirik$dots",
                color = palette.c,
                fontFamily = FontFamily.Monospace,
                fontStyle = FontStyle.Italic,
                fontSize = fontSize,
                maxLines = 1,
            )
            Text(text = call, fontFamily = FontFamily.Monospace, fontSize = fontSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = bar, fontFamily = FontFamily.Monospace, fontSize = fontSize, maxLines = 1, softWrap = false)
        }
    }
}

// ---- the page of code ---------------------------------------------------------------------

@Composable
private fun CodeStage(
    lines: List<LyricsEntry>,
    codeLines: List<CodeLineData>,
    pool: List<CodeIsRow>,
    index: State<Int>,
    typed: State<Int>,
    finale: State<Boolean>,
    palette: CodePalette,
    fx: CodeFx,
    camera: CodeCamera,
    fontSize: TextUnit,
    fixed: Boolean,
    preview: Boolean,
    seekable: Boolean,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val connection = LocalPlayerConnection.current
    val seekTo: (Int) -> Unit = { line -> connection?.player?.seekTo(lines[line].time) }
    val blink = rememberCodeBlink(running = !fixed)
    val jolt = rememberCodeJolt(running = !fixed && (fx == CodeFx.ERROR || fx == CodeFx.GLITCH))
    val popup = remember { mutableStateOf<CodeIsPopup?>(null) }
    val stageRef = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val anchor = 0.46f

    val finaleNow = finale.value
    val pagePose = codePagePose(camera)
    val spec = tween<Float>(if (finaleNow) 1400 else 700, easing = FastOutSlowInEasing)
    val motionOn = !fixed
    val camPhase = rememberCodeCameraPhase(motionOn)
    val motionAmount = animateFloatAsState(if (finaleNow) 0f else 1f, tween(900, easing = FastOutSlowInEasing), label = "codeMotion")
    // each new line pushes the camera in, then it eases back out
    val punch = remember { Animatable(0f) }
    LaunchedEffect(motionOn) {
        if (!motionOn) return@LaunchedEffect
        var first = true
        snapshotFlow { index.value }.collectLatest {
            if (first) {
                first = false
            } else {
                punch.snapTo(1f)
                punch.animateTo(0f, tween(1300, easing = FastOutSlowInEasing))
            }
        }
    }
    val rotY by animateFloatAsState(if (finaleNow) 14f else pagePose[0], spec, label = "codeRotY")
    val rotX by animateFloatAsState(if (finaleNow) 12f else pagePose[1], spec, label = "codeRotX")
    val pageScale by animateFloatAsState(if (finaleNow) 0.8f else pagePose[2], spec, label = "codeScale")

    BoxWithConstraints(modifier.onGloballyPositioned { stageRef[0] = it }) {
        val above = maxHeight * anchor
        val below = maxHeight * (1f - anchor)
        // follow the sung line; a newer line cancels a glide still running, so it never lags behind
        LaunchedEffect(lines, anchor) {
            var first = true
            snapshotFlow { index.value }.collectLatest { i ->
                glideTo(listState, i, anchor, animate = !first && !preview)
                first = false
            }
        }
        LazyColumn(
            state = listState,
            userScrollEnabled = false,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    transformOrigin = TransformOrigin(0f, 0.45f)
                    // the camera's own motion rides on the mode's fixed pose; read here, in the layer, so it never recomposes the page
                    val cam = if (motionOn) codeCameraMotion(camera, camPhase.value, punch.value) else null
                    val amount = motionAmount.value
                    val zoom = if (cam != null) 1f + (cam.scale - 1f) * amount else 1f
                    translationX = if (cam != null) cam.tx * amount * density else 0f
                    translationY = if (cam != null) cam.ty * amount * density else 0f
                    rotationY = rotY + if (cam != null) cam.ry * amount else 0f
                    rotationX = rotX + if (cam != null) cam.rx * amount else 0f
                    rotationZ = if (cam != null) cam.rz * amount else 0f
                    scaleX = pageScale * zoom
                    scaleY = pageScale * zoom
                    cameraDistance = 14f * density
                }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        Brush.verticalGradient(0f to Color.Transparent, 0.14f to Color.Black, 0.80f to Color.Black, 1f to Color.Transparent),
                        blendMode = BlendMode.DstIn,
                    )
                },
        ) {
            item(key = "above") { Spacer(Modifier.height(above)) }
            items(lines.size, key = { it }) { i ->
                val data = codeLines.getOrNull(i)
                if (data != null) {
                    CodeLineRow(
                        i = i,
                        data = data,
                        index = index,
                        typed = typed,
                        finale = finale,
                        blink = blink,
                        jolt = jolt,
                        palette = palette,
                        fx = fx,
                        camera = camera,
                        fontSize = fontSize,
                        pool = pool,
                        showPopup = !fixed,
                        stageRef = stageRef,
                        popup = popup,
                        onSeek = if (seekable) seekTo else null,
                    )
                }
            }
            item(key = "below") { Spacer(Modifier.height(below)) }
        }
        CodeIntelliSenseLayer(popup, palette, maxWidth, maxHeight)
    }
}

@Composable
private fun CodeLineRow(
    i: Int,
    data: CodeLineData,
    index: State<Int>,
    typed: State<Int>,
    finale: State<Boolean>,
    blink: State<Boolean>,
    jolt: State<Float>,
    palette: CodePalette,
    fx: CodeFx,
    camera: CodeCamera,
    fontSize: TextUnit,
    pool: List<CodeIsRow>,
    showPopup: Boolean,
    stageRef: Array<LayoutCoordinates?>,
    popup: MutableState<CodeIsPopup?>,
    onSeek: ((Int) -> Unit)?,
) {
    val density = LocalDensity.current
    // how far this line is above the sung one (negative: still to come); read here, so a change recomposes the few lines on screen
    val d = index.value - i
    val active = d == 0
    val fin = finale.value
    val full = data.full.length
    val n = when {
        d > 0 -> full
        active -> typed.value
        else -> 0
    }

    val alphaTarget = when {
        d < 0 -> 0f
        fin -> if (active) 1f else max(0.28f, 0.92f - d * 0.085f)
        fx == CodeFx.BLANK && d > 0 -> 0f
        active -> 0.94f
        d > 8 -> 0f
        else -> max(0.18f, 0.76f * exp(-d * 0.19f))
    }
    val blurTarget = if (fin || d <= 0 || d > 8) 0f else min(0.5f + (d - 1) * 0.4f, 2.5f)
    val pose = codeLinePose(camera, d, i, fin)
    val glide = tween<Float>(700, easing = FastOutSlowInEasing)
    val alpha by animateFloatAsState(alphaTarget, tween(800, easing = FastOutSlowInEasing), label = "codeAlpha")
    val scale by animateFloatAsState(pose.scale, glide, label = "codeLineScale")
    val shift by animateFloatAsState(pose.tx, glide, label = "codeLineShift")
    val turn by animateFloatAsState(pose.ry, glide, label = "codeLineTurn")
    val blur by animateFloatAsState(blurTarget, tween(700, easing = FastOutSlowInEasing), label = "codeLineBlur")

    // the suggestion popup: this line's word, while it is typed and a moment after
    val tk = if (active && showPopup && !fin && data.tokStart >= 0) codeTokenAt(data.full, n)?.takeIf { it.start == data.tokStart } else null
    var holdDone by remember(active, data) { mutableStateOf(false) }
    val reported = remember { booleanArrayOf(false) }
    val layoutRef = remember { arrayOfNulls<TextLayoutResult>(1) }
    val coordsRef = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val tkNow by rememberUpdatedState(tk)
    val holdNow by rememberUpdatedState(holdDone)
    val nNow by rememberUpdatedState(n)
    val tkDone = tk?.done == true

    fun report() {
        val token = tkNow
        val res = layoutRef[0]
        val coords = coordsRef[0]
        val stage = stageRef[0]
        if (token == null || res == null || coords == null || stage == null || !coords.isAttached || !stage.isAttached || (token.done && holdNow)) {
            if (reported[0]) {
                popup.value = null
                reported[0] = false
            }
            return
        }
        val rect = res.getCursorRect(nNow.coerceIn(0, res.layoutInput.text.length))
        val bottom = stage.localPositionOf(coords, Offset(rect.left, rect.bottom))
        val top = stage.localPositionOf(coords, Offset(rect.left, rect.top))
        val kind = codeKindAt(data.segs, token.start)
        popup.value = CodeIsPopup(codeSuggestions(token, kind, pool), token.pre.length, token.word, kind, i + 1, bottom.x, top.y, bottom.y)
        reported[0] = true
    }

    LaunchedEffect(tkDone, active) {
        if (tkDone) {
            delay(1600)
            holdDone = true
            if (reported[0]) {
                popup.value = null
                reported[0] = false
            }
        }
    }
    DisposableEffect(active) {
        onDispose {
            if (reported[0]) {
                popup.value = null
                reported[0] = false
            }
        }
    }

    val text = remember(data, n, palette, active, fx) { codeAnnotated(data, n, palette, active, fx) }
    val vertical = with(density) { (fontSize * 0.3f).toDp() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onSeek != null) Modifier.clickable(remember { MutableInteractionSource() }, null) { onSeek(i) } else Modifier)
            .graphicsLayer {
                this.alpha = alpha
                translationX = shift.dp.toPx() + (if (active) jolt.value.dp.toPx() else 0f)
                scaleX = scale
                scaleY = scale
                rotationY = turn
                transformOrigin = TransformOrigin(0f, 0.5f)
                cameraDistance = 14f * this.density
            }
            .then(if (blur > 0.05f) Modifier.blur(blur.dp, BlurredEdgeTreatment.Unbounded) else Modifier)
            .drawBehind {
                if (active && fx == CodeFx.ERROR) {
                    drawRect(Brush.horizontalGradient(listOf(Color(0x24FF3C3C), Color.Transparent)))
                    drawRect(Color(0xFFFF4040), size = Size(3.dp.toPx(), size.height))
                }
            }
            .padding(vertical = vertical),
    ) {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontSize = fontSize,
            lineHeight = fontSize * 1.45f,
            color = palette.p,
            onTextLayout = { res ->
                layoutRef[0] = res
                report()
            },
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned {
                    coordsRef[0] = it
                    if (active) report()
                }
                .drawWithContent {
                    drawContent()
                    // the caret sits after the last typed character; it blinks once the line is done
                    val res = layoutRef[0]
                    if (active && !fin && res != null && (n < full || blink.value)) {
                        val rect = res.getCursorRect(n.coerceIn(0, res.layoutInput.text.length))
                        drawRect(palette.cur, Offset(rect.left, rect.top), Size(2.dp.toPx(), rect.height))
                    }
                },
        )
    }
}
