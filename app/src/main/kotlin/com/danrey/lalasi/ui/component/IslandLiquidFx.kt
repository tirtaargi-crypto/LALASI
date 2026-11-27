// IslandLiquidFx.kt
//
// What the "Kaca cair" Dynamic Island adds when it is pressed open:
//  - a "melar" (stretched) 3D backdrop: the blurred cover is pulled sideways like warm rubber, flows slowly, and is lit as
//    a thick dome (soft diffuse + a specular glint). AGSL, API 33+; below that the plain blurred cover stays.
//  - two 3D ribbons (pita) down the left and right inner edge: soft glowing tubes (a wide halo, a body, a bright inner
//    core and a flowing highlight) in the colours of the cover, so they follow the artwork like the iPhone island.
// Both are driven by the island's own open progress, so they swell and fade with the same spring as the card.

package com.danrey.lalasi.ui.component

import android.content.Context
import android.os.Build
import android.util.LruCache
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.danrey.lalasi.ui.utils.resize
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

// ---------------------------------------------------------------------------------------------------------------------
// the colours the ribbons glow with: taken from the cover, kept bright
// ---------------------------------------------------------------------------------------------------------------------

/** [a] lights the ribbon's front face and its glow, [b] its back face */
internal class IslandGlow(
    val a: Color,
    val b: Color,
    /** a third accent (a deeper or neighbouring hue) for the slow colour slide along the ribbon and the rim */
    val c: Color = b,
    /** 0..1, how colourful the cover is: wider, stronger, more spread glow */
    val energy: Float = 0.6f,
    /** 0..1, how light the cover is: light covers get a pearly core, dark ones pure neon */
    val brightness: Float = 0.5f,
    /** from the cover itself: gives each album its own curves, drift and phase */
    val seed: Int = 0,
    /** the cover has two clearly different hues: the left ribbon takes one, the right the other */
    val split: Boolean = false,
    /** 18 numbers: rgb of the cover's blurred colour at the top, middle and bottom of the left edge, then of the right edge */
    val edge: FloatArray? = null,
)

private val NeutralGlow = IslandGlow(Color(0xFFBFD6FF), Color(0xFFEAF2FF))

private object IslandGlowCache {
    val cache = LruCache<String, IslandGlow>(48)

    suspend fun extract(context: Context, url: String): IslandGlow? {
        cache.get(url)?.let { return it }
        return try {
            withContext(Dispatchers.IO) {
                val request = ImageRequest.Builder(context).data(url).size(96, 96).allowHardware(false).build()
                val bitmap = context.imageLoader.execute(request).image?.toBitmap() ?: return@withContext null
                val glow = withContext(Dispatchers.Default) {
                    val palette = Palette.from(bitmap).maximumColorCount(16).generate()
                    // the cover's own dominant colour first (so a pink cover gives pink ribbons), then the vibrant ones
                    val first = palette.dominantSwatch?.takeIf { it.hsl[1] > 0.22f && it.hsl[2] in 0.12f..0.88f }
                        ?: palette.vibrantSwatch ?: palette.lightVibrantSwatch ?: palette.mutedSwatch
                        ?: palette.swatches.maxByOrNull { it.population } ?: return@withContext null
                    val second = (palette.lightVibrantSwatch ?: palette.darkVibrantSwatch ?: palette.lightMutedSwatch)
                        ?.takeIf { it.rgb != first.rgb }
                    val a = vivid(first.rgb, 0.62f, 0f)
                    // no second colour in the cover: the same hue, turned a little and lighter
                    val b = if (second != null) vivid(second.rgb, 0.70f, 0f) else vivid(first.rgb, 0.72f, 34f)
                    // a third accent: a deeper swatch of the cover, else the first hue turned the other way
                    val third = (palette.darkVibrantSwatch ?: palette.mutedSwatch ?: palette.darkMutedSwatch)
                        ?.takeIf { it.rgb != first.rgb && it.rgb != second?.rgb }
                    val c = if (third != null) vivid(third.rgb, 0.56f, 0f) else vivid(first.rgb, 0.58f, -28f)
                    // the cover's character: how colourful, how light, and how far apart its two main hues are
                    var pop = 0f; var sat = 0f; var lit = 0f
                    palette.swatches.forEach { sw -> pop += sw.population; sat += sw.population * sw.hsl[1]; lit += sw.population * sw.hsl[2] }
                    val energy = if (pop > 0f) (sat / pop).coerceIn(0.15f, 1f) else 0.6f
                    val brightness = if (pop > 0f) (lit / pop).coerceIn(0f, 1f) else 0.5f
                    val hueGap = second?.let { val d = kotlin.math.abs(it.hsl[0] - first.hsl[0]); minOf(d, 360f - d) } ?: 0f
                    IslandGlow(a, b, c, energy, brightness, url.hashCode(), split = second != null && hueGap > 55f, edge = edgeColors(bitmap))
                } ?: return@withContext null
                cache.put(url, glow)
                glow
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }
}

/** a colour for soft glowing light: keeps the cover's hue, a gentle saturation and a mid lightness (never neon, never muddy) */
private fun softGlow(argb: Int): Int {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(argb, hsl)
    if (hsl[1] >= 0.10f) {
        hsl[1] = (hsl[1] * 1.2f).coerceIn(0.45f, 0.95f)
        hsl[2] = hsl[2].coerceIn(0.46f, 0.66f)
    } else {
        hsl[2] = hsl[2].coerceIn(0.58f, 0.80f)
    }
    return ColorUtils.HSLToColor(hsl)
}

/** the cover's own colours next to the island's left and right edges (top / middle / bottom of the part that shows) */
private fun edgeColors(bmp: android.graphics.Bitmap): FloatArray {
    val w = bmp.width
    val h = bmp.height
    val out = FloatArray(18)
    val bands = floatArrayOf(0.30f, 0.43f, 0.57f, 0.70f)
    for (side in 0..1) {
        val x0 = if (side == 0) 0 else (w * 0.82f).toInt()
        val x1 = if (side == 0) (w * 0.18f).toInt().coerceAtLeast(1) else w
        for (k in 0..2) {
            val y0 = (h * bands[k]).toInt()
            val y1 = (h * bands[k + 1]).toInt().coerceAtLeast(y0 + 1)
            var r = 0L; var g = 0L; var b = 0L; var n = 0
            for (y in y0 until y1) for (x in x0 until x1) {
                val px = bmp.getPixel(x.coerceIn(0, w - 1), y.coerceIn(0, h - 1))
                r += android.graphics.Color.red(px); g += android.graphics.Color.green(px); b += android.graphics.Color.blue(px); n++
            }
            if (n == 0) n = 1
            val c = softGlow(android.graphics.Color.rgb((r / n).toInt(), (g / n).toInt(), (b / n).toInt()))
            val o = side * 9 + k * 3
            out[o] = android.graphics.Color.red(c) / 255f
            out[o + 1] = android.graphics.Color.green(c) / 255f
            out[o + 2] = android.graphics.Color.blue(c) / 255f
        }
    }
    return out
}

/** full colour at a glowing lightness, so it reads as light, not as paint */
private fun vivid(argb: Int, lightness: Float, hueTurn: Float): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(argb, hsl)
    hsl[0] = (hsl[0] + hueTurn) % 360f
    // a grey cover (black and white, silver) keeps a silver accent instead of getting an invented hue
    if (hsl[1] >= 0.12f) hsl[1] = hsl[1].coerceAtLeast(0.58f).coerceAtMost(1f)
    hsl[2] = lightness
    return Color(ColorUtils.HSLToColor(hsl))
}

@Composable
internal fun rememberIslandGlow(url: String?): State<IslandGlow> {
    val context = LocalContext.current
    // ONE state for the whole life of the island: when the song changes it keeps showing the old cover's glow until the
    // new one is read, then the accents morph to it (rememberIslandAccents). It never drops to the neutral blue in between.
    val state = remember { mutableStateOf(url?.let { IslandGlowCache.cache.get(it) } ?: NeutralGlow) }
    LaunchedEffect(url) {
        if (url == null) {
            state.value = NeutralGlow
            return@LaunchedEffect
        }
        val cached = IslandGlowCache.cache.get(url)
        if (cached != null) state.value = cached
        else IslandGlowCache.extract(context, url)?.let { state.value = it }
    }
    return state
}

/** every number the accents are drawn from, as plain numbers, so two covers' accents can be blended into each other */
internal class GlowParams(
    val a: Color, val b: Color, val c: Color,
    val energy: Float, val brightness: Float, val split: Float,
    /** per side (left, right): bulge, wave, phase offset, flow frequency: [bulgeL, bulgeR, waveL, waveR, phaseL, phaseR, flowL, flowR] */
    val shape: FloatArray,
    /** see IslandGlow.edge */
    val edge: FloatArray,
)

private fun IslandGlow.toParams(): GlowParams {
    val l = seed
    val r = seed + 7919
    return GlowParams(
        a, b, c, energy, brightness, if (split) 1f else 0f,
        floatArrayOf(
            2f + 6f * seeded(l, 1), 2f + 6f * seeded(r, 1),
            3f + 3.2f * seeded(l, 2), 3f + 3.2f * seeded(r, 2),
            seeded(l, 4) * 6.28f, seeded(r, 4) * 6.28f,
            1.2f + 0.6f * seeded(l, 5), 1.2f + 0.6f * seeded(r, 5),
        ),
        edge ?: arrayOf(a, c, b, b, c, a).flatMap { listOf(it.red, it.green, it.blue) }.toFloatArray(),
    )
}

private fun lerpF(a: Float, b: Float, t: Float) = a + (b - a) * t
private fun lerpC(a: Color, b: Color, t: Float) = Color(lerpF(a.red, b.red, t), lerpF(a.green, b.green, t), lerpF(a.blue, b.blue, t), 1f)

private fun lerpParams(x: GlowParams, y: GlowParams, t: Float) = GlowParams(
    lerpC(x.a, y.a, t), lerpC(x.b, y.b, t), lerpC(x.c, y.c, t),
    lerpF(x.energy, y.energy, t), lerpF(x.brightness, y.brightness, t), lerpF(x.split, y.split, t),
    FloatArray(x.shape.size) { lerpF(x.shape[it], y.shape[it], t) },
    FloatArray(x.edge.size) { lerpF(x.edge[it], y.edge[it], t) },
)

/**
 * The accents of the current cover, morphing: when the song changes, the colours, the glow strength, the light and even
 * the curves of both ribbons and the rim slide from the old cover's to the new one's over ~0.9 s (ease in-out), so
 * nothing blinks or jumps. The result is read while drawing, so a morph only redraws, it never recomposes.
 */
@Composable
internal fun rememberIslandAccents(cover: String?): State<GlowParams> {
    val raw = rememberIslandGlow(cover).value
    val target = remember(raw) {
        IslandGlow(vividRibbon(raw.a), vividRibbon(raw.b), vividRibbon(raw.c), raw.energy, raw.brightness, raw.seed, raw.split, raw.edge).toParams()
    }
    val shown = remember { mutableStateOf(target) }
    val progress = remember { androidx.compose.animation.core.Animatable(1f) }
    var from by remember { mutableStateOf(target) }
    var to by remember { mutableStateOf(target) }
    LaunchedEffect(target) {
        if (target === to) return@LaunchedEffect
        from = shown.value
        to = target
        progress.snapTo(0f)
        progress.animateTo(1f, androidx.compose.animation.core.tween(900, easing = androidx.compose.animation.core.FastOutSlowInEasing)) {
            shown.value = lerpParams(from, to, value)
        }
        shown.value = to
    }
    return shown
}

// ---------------------------------------------------------------------------------------------------------------------
// the cover, blurred once: a smooth gaussian-like bitmap the stretch shader can warp without ever showing blocks
// ---------------------------------------------------------------------------------------------------------------------

private object IslandBlurCache {
    // tiny bitmaps (128 x 128, 64 KB each): 24 of them is ~1.5 MB
    val cache = LruCache<String, ImageBitmap>(10)

    suspend fun load(context: Context, url: String): ImageBitmap? {
        cache.get(url)?.let { return it }
        return try {
            withContext(Dispatchers.IO) {
                val request = ImageRequest.Builder(context).data(url.resize(768, 768)).size(640, 640).allowHardware(false).build()
                val source = context.imageLoader.execute(request).image?.toBitmap() ?: return@withContext null
                val out = withContext(Dispatchers.Default) { softBlur(source) }
                cache.put(url, out)
                out
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }
}

private const val BlurSide = 512

/** the cover at 320 x 320 (sharp enough to never look pixelated on the wide island), three light box-blur passes (a gaussian to within a hair): smooth, and still readable as the cover */
private fun softBlur(source: android.graphics.Bitmap): ImageBitmap {
    val small = android.graphics.Bitmap.createScaledBitmap(source, BlurSide, BlurSide, true)
    val n = BlurSide * BlurSide
    val px = IntArray(n)
    small.getPixels(px, 0, BlurSide, 0, 0, BlurSide, BlurSide)
    val r = IntArray(n) { (px[it] shr 16) and 0xFF }
    val g = IntArray(n) { (px[it] shr 8) and 0xFF }
    val b = IntArray(n) { px[it] and 0xFF }
    val tmp = IntArray(n)
    repeat(2) {
        boxBlur(r, tmp, BlurSide, BlurSide, 4)
        boxBlur(g, tmp, BlurSide, BlurSide, 4)
        boxBlur(b, tmp, BlurSide, BlurSide, 4)
    }
    for (i in 0 until n) px[i] = (0xFF shl 24) or (r[i].coerceIn(0, 255) shl 16) or (g[i].coerceIn(0, 255) shl 8) or b[i].coerceIn(0, 255)
    val out = android.graphics.Bitmap.createBitmap(BlurSide, BlurSide, android.graphics.Bitmap.Config.ARGB_8888)
    out.setPixels(px, 0, BlurSide, 0, 0, BlurSide, BlurSide)
    if (small !== source) small.recycle()
    return out.asImageBitmap()
}

private fun boxBlur(ch: IntArray, tmp: IntArray, w: Int, h: Int, rad: Int) {
    val div = 2 * rad + 1
    for (y in 0 until h) {
        val row = y * w
        var sum = 0
        for (i in -rad..rad) sum += ch[row + i.coerceIn(0, w - 1)]
        for (x in 0 until w) {
            tmp[row + x] = sum / div
            sum += ch[row + (x + rad + 1).coerceAtMost(w - 1)] - ch[row + (x - rad).coerceAtLeast(0)]
        }
    }
    for (x in 0 until w) {
        var sum = 0
        for (i in -rad..rad) sum += tmp[i.coerceIn(0, h - 1) * w + x]
        for (y in 0 until h) {
            ch[y * w + x] = sum / div
            sum += tmp[(y + rad + 1).coerceAtMost(h - 1) * w + x] - tmp[(y - rad).coerceAtLeast(0) * w + x]
        }
    }
}

/** the blurred cover of [url]; while a new song's cover is being read it keeps showing the old one (then the island cross-fades) */
@Composable
internal fun rememberIslandCoverBlur(url: String?): State<ImageBitmap?> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(url?.let { IslandBlurCache.cache.get(it) }) }
    LaunchedEffect(url) {
        if (url == null) {
            state.value = null
            return@LaunchedEffect
        }
        val cached = IslandBlurCache.cache.get(url)
        if (cached != null) state.value = cached
        else IslandBlurCache.load(context, url)?.let { state.value = it }
    }
    return state
}

/** seconds, ticking once per frame while [active]; unlike an infinite transition it keeps running when the system's animation scale is 0 / power saver */
@Composable
private fun rememberFlowTime(active: Boolean): State<Float> {
    val t = remember { mutableStateOf(0f) }
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            t.value = t.value + (now - last) / 1_000_000_000f
            last = now
        }
    }
    return t
}

// ---------------------------------------------------------------------------------------------------------------------
// the "melar" 3D backdrop

// ---------------------------------------------------------------------------------------------------------------------

private const val MelarAgsl = """
uniform shader image;
uniform float2 resolution;
uniform float amt;

half4 main(float2 fc) {
    float2 uv = fc / resolution;
    float ax = abs(uv.x - 0.5) * 2.0;

    // STATIC liquid stretch: the cover is pulled sideways toward both edges, like glass drawn out. No time, no waves,
    // no ripples: it only grows with the island's open progress (amt) and then stays still.
    // sx - 0.5 = u * (1 - k * (2u)^2): monotonic while k < 1/3. (The old k up to 0.55 folded over near the edges and ghosted.)
    // At k = 0.30 the picture is stretched ~10x horizontally at the very edge: long soft streaks, like the reference.
    float pull = 0.30 * amt * ax * ax;
    float sx = 0.5 + (uv.x - 0.5) * (1.0 - pull);
    float2 st = float2(sx, uv.y);

    // a very short horizontal smear toward the nearest edge (smooth, 5 close taps, keeps the cover sharp)
    float side = (uv.x < 0.5) ? -1.0 : 1.0;
    float smear = (0.002 + 0.010 * amt) * ax * ax;
    float2 dir = float2(side * smear, 0.0);
    float2 lo = float2(0.0);
    float2 hi = float2(1.0);
    half4 tex = image.eval(clamp(st, lo, hi) * resolution) * 0.30;
    tex += image.eval(clamp(st + dir * 0.5, lo, hi) * resolution) * 0.24;
    tex += image.eval(clamp(st + dir * 1.0, lo, hi) * resolution) * 0.16;
    tex += image.eval(clamp(st - dir * 0.5, lo, hi) * resolution) * 0.20;
    tex += image.eval(clamp(st - dir * 1.0, lo, hi) * resolution) * 0.10;

    float3 col = float3(tex.rgb);
    float lum = dot(col, float3(0.299, 0.587, 0.114));
    col = mix(float3(lum), col, 1.0 + 0.20 * amt);
    // one frame-stable dither step (+-0.5/255): hides 8-bit banding without visible grain
    float dither = fract(sin(dot(fc, float2(12.9898, 78.233))) * 43758.5453) - 0.5;
    col += dither / 255.0;
    return half4(half3(col), tex.a);
}
"""

@androidx.annotation.RequiresApi(33)
private fun Modifier.melarEffect(shader: Any, amt: () -> Float): Modifier = this.graphicsLayer {
    val a = amt().coerceIn(0f, 1f)
    // closed: the plain blurred cover, no shader work at all
    if (a <= 0.001f) {
        renderEffect = null
        return@graphicsLayer
    }
    shader.setAgslUniform("resolution", size.width, size.height)
    shader.setAgslUniform("amt", a)
    renderEffect = agslRenderEffect(shader, "image").asComposeRenderEffect()
    clip = true
}

/**
 * Applies the stretched, lit look to whatever this is on (the blurred cover). [amt] is the island's open progress (0..1);
 * [active] keeps the slow flow running only while the card is open or moving. A no-op below API 33 or if the shader
 * cannot be built.
 */
@Composable
internal fun Modifier.melarBackdrop(active: Boolean, amt: () -> Float): Modifier {
    if (Build.VERSION.SDK_INT < 33) {
        // no AGSL here: a transform-only stretch (GPU friendly, no extra pass) so older phones still get the pulled-cover feel
        return this.graphicsLayer {
            val a = amt().coerceIn(0f, 1f)
            scaleX = 1f + 0.07f * a
            scaleY = 1f + 0.02f * a
            clip = true
        }
    }
    val shader = remember { runCatching { createAgslShader(MelarAgsl) }.getOrNull() } ?: return this
    return this.melarEffect(shader, amt)
}

// ---------------------------------------------------------------------------------------------------------------------
// the 3D ribbons
// ---------------------------------------------------------------------------------------------------------------------

private fun smooth01(x: Float): Float {
    val c = x.coerceIn(0f, 1f)
    return c * c * (3f - 2f * c)
}

/**
 * Two twisting satin ribbons, one down each edge of the open island, drawn over the backdrop and under the controls.
 * [progress] is the island's open progress; they grow in from the top as it opens and fade as it closes.
 */
@Composable
internal fun IslandRibbons(accents: State<GlowParams>, active: Boolean, progress: () -> Float, modifier: Modifier = Modifier) {
    // the accents are STILL (no time-driven motion): the shape stays fixed, only its growth with the island and the colour morph on a song change animate
    // one soft, blurred pass (gaussian glow, no crisp outline); on API < 31 the blur is a no-op and the layers stay soft anyway
    Canvas(modifier.blur(5.dp).graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }) {
        val p = smooth01((progress() - 0.2f) / 0.6f)
        if (p <= 0.01f || size.width <= 0f || size.height <= 0f) return@Canvas
        val t = 0.6f   // fixed phase: a static, organic shape
        val glow = accents.value   // read here, while drawing: a colour morph redraws, it does not recompose
        drawRibbon(leftSide = true, p = p, t = t, glow = glow)
        drawRibbon(leftSide = false, p = p, t = t, glow = glow)
    }
}

private const val RibbonSegments = 72

private fun vividRibbon(c: Color): Color {
    val hsl = FloatArray(3)
    androidx.core.graphics.ColorUtils.colorToHSL(c.toArgb(), hsl)
    if (hsl[1] < 0.14f) {
        // grey cover: a bright silver neon, no invented colour
        hsl[2] = hsl[2].coerceIn(0.66f, 0.84f)
    } else {
        hsl[1] = hsl[1].coerceAtLeast(0.74f)
        hsl[2] = hsl[2].coerceIn(0.52f, 0.64f)
    }
    return Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))
}

private fun Color.withA(a: Float) = copy(alpha = a.coerceIn(0f, 1f))

private fun Color.toward(white: Float) = Color(
    red = red + (1f - red) * white,
    green = green + (1f - green) * white,
    blue = blue + (1f - blue) * white,
    alpha = 1f,
)

private fun mix(a: Color, b: Color, f: Float) = Color(
    red = a.red + (b.red - a.red) * f,
    green = a.green + (b.green - a.green) * f,
    blue = a.blue + (b.blue - a.blue) * f,
    alpha = 1f,
)

/** a stable 0..1 number from the cover's seed, one per aspect of the shape, so each album curves its own way */
private fun seeded(seed: Int, k: Int): Float = (((seed * 1103515245L + k * 2654435761L) ushr 7) % 1000L).toInt() / 1000f

/**
 * One organic "bean" of soft light on each inner wall of the open island, like the reference: a rounded, kidney-like blob
 * with a saturated body, a lighter milky core and a glow spreading toward the middle. It is alive: the outline swells and
 * pinches in slow waves, the waist travels up and down, the whole bean sways and a bright patch slides through it.
 * Colours are the cover's own blurred colours at that edge (top / middle / bottom). The caller gaussian-blurs the whole
 * pass lightly (5dp), so the edges are soft but the shape still reads.
 */
private fun DrawScope.drawRibbon(leftSide: Boolean, p: Float, t: Float, glow: GlowParams) {
    val w = size.width
    val h = size.height
    val dir = if (leftSide) 1f else -1f
    val side = if (leftSide) 0 else 1
    val energy = glow.energy
    val ph = glow.shape[4 + side]
    val phase = t * (1.7f + 0.5f * energy) + ph
    val widthScale = 0.9f + 0.4f * energy
    // sits against the wall and just crosses the rim
    val baseX = if (leftSide) 9.dp.toPx() else w - 9.dp.toPx()
    val sway = sin(phase * 0.55f) * h * 0.04f
    val top = h * 0.10f + sway
    val bottom = h * 0.92f + sway
    val len = (bottom - top) * p
    if (len <= 4.dp.toPx()) return
    val cornerR = minOf(h / 2f, 34.dp.toPx())
    fun wallInset(y: Float): Float {
        val ry = if (y < cornerR) cornerR - y else if (y > h - cornerR) y - (h - cornerR) else 0f
        return if (ry <= 0f) 0f else cornerR - kotlin.math.sqrt((cornerR * cornerR - ry * ry).coerceAtLeast(0f))
    }

    val e = glow.edge
    val o = side * 9
    fun ec(k: Int) = Color(e[o + k * 3], e[o + k * 3 + 1], e[o + k * 3 + 2], 1f)
    val cTop = ec(0)
    val cMid = ec(1)
    val cBot = ec(2)
    val strength = (1.0f - 0.18f * glow.brightness) * p
    fun vg(alpha: Float) = Brush.verticalGradient(
        0f to cTop.withA(alpha * strength), 0.5f to cMid.withA(alpha * strength), 1f to cBot.withA(alpha * strength),
        startY = top, endY = top + len,
    )

    val n = RibbonSegments
    val xs = FloatArray(n + 1)
    val ys = FloatArray(n + 1)
    val hw = FloatArray(n + 1)
    val drift = (3.5f + 3f * energy).dp.toPx()
    val wave = glow.shape[2 + side]
    val baseHalf = 12.dp.toPx() * widthScale * (1f + 0.16f * sin(phase * 0.8f))
    val wc = 0.5f + 0.14f * sin(phase * 0.6f + ph)          // the waist travels up and down
    for (i in 0..n) {
        val f = i / n.toFloat()
        val y = top + len * f
        xs[i] = baseX + dir * (wallInset(y) + drift * sin(f * wave + phase * 0.9f) + 2.5.dp.toPx() * sin(PI.toFloat() * f))
        ys[i] = y
        val bean = sin(PI.toFloat() * f).coerceAtLeast(0f).pow(0.55f)   // round ends
        val waist = 1f - 0.28f * kotlin.math.exp(-(((f - wc) / 0.16f) * ((f - wc) / 0.16f)))
        val swell = 1f + 0.30f * sin(f * 4.2f + phase * 1.3f + ph * 0.5f) + 0.16f * sin(f * 8.5f - phase * 1.9f)
        hw[i] = baseHalf * (0.22f + 0.78f * bean) * waist * swell.coerceAtLeast(0.45f)
    }
    // quadratic curves through the midpoints of neighbouring points: a smooth edge, not a row of straight facets
    fun Path.curveThrough(px: (Int) -> Float, from: Int, to: Int, first: Boolean) {
        val step = if (to >= from) 1 else -1
        if (first) moveTo(px(from), ys[from]) else lineTo(px(from), ys[from])
        var i = from + step
        while (i != to) {
            val nx = i + step
            quadraticBezierTo(px(i), ys[i], (px(i) + px(nx)) / 2f, (ys[i] + ys[nx]) / 2f)
            i = nx
        }
        lineTo(px(to), ys[to])
    }
    fun band(scale: Float): Path = Path().apply {
        curveThrough({ xs[it] - hw[it] * scale }, 0, n, true)
        curveThrough({ xs[it] + hw[it] * scale }, n, 0, false)
        close()
    }

    // soft glow spreading from the bean into the glass, in the cover's colour at that edge
    val spread = w * (0.20f + 0.14f * energy)
    val haloX0 = if (leftSide) 0f else w - spread
    drawRect(
        Brush.horizontalGradient(
            if (leftSide) listOf(cMid.withA(0.34f * strength), cMid.withA(0.08f * strength), Color.Transparent)
            else listOf(Color.Transparent, cMid.withA(0.08f * strength), cMid.withA(0.34f * strength)),
            startX = haloX0, endX = haloX0 + spread,
        ),
        topLeft = Offset(haloX0, top), size = Size(spread, len),
    )
    // outer soft rim of the bean, then the saturated body (cover colours top to bottom)
    drawPath(band(1.28f), vg(0.22f))
    drawPath(band(1.05f), vg(0.34f))
    drawPath(band(0.80f), vg(0.40f))
    // lighter, milky core
    drawPath(band(0.50f), cMid.toward(0.28f + 0.30f * glow.brightness).withA(0.50f * strength))
    drawPath(band(0.24f), cMid.toward(0.55f + 0.25f * glow.brightness).withA(0.45f * strength))
    // a bright patch sliding up and down the bean: light moving through it
    val pos = 0.5f + 0.36f * sin(phase * 0.6f + 1.3f)
    val idx = (pos * n).toInt().coerceIn(0, n)
    val patchC = Offset(xs[idx], ys[idx])
    val patchR = (hw[idx] * 2.0f).coerceAtLeast(10.dp.toPx())
    drawCircle(
        Brush.radialGradient(
            listOf(cMid.toward(0.6f).withA(0.55f * strength), cMid.withA(0.18f * strength), Color.Transparent),
            center = patchC, radius = patchR,
        ),
        radius = patchR, center = patchC,
    )
}
