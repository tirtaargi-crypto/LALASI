package com.danrey.lalasi.ui.screens.playlist.poster

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.LruCache
import androidx.core.content.res.ResourcesCompat
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.danrey.lalasi.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

data class SongInfo(val id: String, val title: String, val artist: String, val durationSec: Int, val thumb: String?)

/** Pre-rendered pixels of one layer at the current output size. */
class Content(
    val bmp: Bitmap,
    val key: String,
    val scale: Float,
    private val logicalWidth: Float? = null,
    private val logicalHeight: Float? = null
) {
    // Player bitmaps include transparent overscan for rim glow. Keep the logical
    // object bounds equal to the card itself while drawing the larger bitmap centered.
    val width get() = logicalWidth ?: bmp.width * scale
    val height get() = logicalHeight ?: bmp.height * scale
}

class Fx(val bmp: Bitmap, val ox: Int, val oy: Int)

/**
 * Loads and caches every bitmap the renderer needs. A fresh instance is created for export so preview and export never fight over memory.
 * All drawing calls happen on one worker at a time, so the cache is intentionally not recycled on eviction (avoids "recycled bitmap" crashes).
 */
class PosterAssets(private val context: Context, private val songs: Map<String, SongInfo>, cacheBytes: Int) {
    private val bitmaps = object : LruCache<String, Bitmap>(cacheBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }
    private val fxCache = object : LruCache<String, Fx>(cacheBytes / 2) {
        override fun sizeOf(key: String, value: Fx): Int = value.bmp.allocationByteCount
    }
    private val failed = HashSet<String>()
    private val typefaces = HashMap<Int, Typeface>()
    /** Official LALASI mark (white on transparent), cropped to its visible pixels so it can be tinted and sized exactly. */
    private val brandLogo: Bitmap? by lazy {
        runCatching {
            val opts = BitmapFactory.Options().apply { inScaled = false; inPreferredConfig = Bitmap.Config.ARGB_8888 }
            val full = BitmapFactory.decodeResource(context.resources, R.drawable.lalasi_official_logo, opts) ?: return@runCatching null
            val w = full.width; val h = full.height
            val px = IntArray(w * h); full.getPixels(px, 0, w, 0, 0, w, h)
            var x0 = w; var y0 = h; var x1 = -1; var y1 = -1
            for (y in 0 until h) for (x in 0 until w) if ((px[y * w + x] ushr 24) > 12) { if (x < x0) x0 = x; if (x > x1) x1 = x; if (y < y0) y0 = y; if (y > y1) y1 = y }
            if (x1 < x0 || y1 < y0) full else Bitmap.createBitmap(full, x0, y0, x1 - x0 + 1, y1 - y0 + 1)
        }.getOrNull()
    }
    val aspects = HashMap<String, Float>()

    fun song(id: String?): SongInfo? = if (id == null) null else songs[id]

    // ---------------------------------------------------------------- loading

    fun source(path: String?, maxDim: Int): Bitmap? {
        if (path.isNullOrBlank()) return null
        val key = "src:$path:$maxDim"
        bitmaps.get(key)?.let { return it }
        if (key in failed) return null
        val bmp = runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= maxDim || bounds.outHeight / (sample * 2) >= maxDim) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 }
            var b = BitmapFactory.decodeFile(path, opts) ?: return@runCatching null
            val big = max(b.width, b.height)
            if (big > maxDim) { val s = maxDim.toFloat() / big; b = Bitmap.createScaledBitmap(b, max(1, (b.width * s).roundToInt()), max(1, (b.height * s).roundToInt()), true) }
            b
        }.getOrNull()
        if (bmp != null) { bitmaps.put(key, bmp); aspects[path] = bmp.height.toFloat() / bmp.width } else failed.add(key)
        return bmp
    }

    fun art(url: String?): Bitmap? = if (url.isNullOrBlank()) null else bitmaps.get("art:$url")

    /** Downloads/loads every artwork and photo the document needs. Called before drawing so drawing itself never blocks on IO. */
    suspend fun prepare(doc: PosterDoc, W: Int, H: Int) {
        source(doc.bgUri, PosterRenderer.bgMax(W, H))
        if (doc.bgPop) source(doc.bgCutUri, 2048)
        for (l in doc.layers) {
            when (l.type) {
                LayerType.CHARACTER, LayerType.IMAGE -> source(l.uri, 2048)
                LayerType.PLAYER -> {
                    val url = songs[l.songId]?.thumb
                    if (!url.isNullOrBlank() && bitmaps.get("art:$url") == null && "art:$url" !in failed) {
                        val b = runCatching {
                            val res = context.imageLoader.execute(ImageRequest.Builder(context).data(url).allowHardware(false).build())
                            res.image?.toBitmap()
                        }.getOrNull()
                        if (b != null) bitmaps.put("art:$url", b) else failed.add("art:$url")
                    }
                }
                else -> Unit
            }
        }
    }

    fun fontFor(index: Int, bold: Boolean, italic: Boolean): Typeface {
        val style = when { bold && italic -> Typeface.BOLD_ITALIC; bold -> Typeface.BOLD; italic -> Typeface.ITALIC; else -> Typeface.NORMAL }
        val base = typefaces.getOrPut(index) {
            runCatching {
                when (index) {
                    0 -> Typeface.create("serif", Typeface.BOLD_ITALIC)
                    1 -> Typeface.create("serif", Typeface.BOLD)
                    2 -> Typeface.SERIF
                    3 -> Typeface.create("serif", Typeface.NORMAL)
                    4 -> ResourcesCompat.getFont(context, R.font.inter) ?: Typeface.SANS_SERIF
                    5 -> Typeface.SANS_SERIF
                    6 -> Typeface.create("sans-serif-condensed", Typeface.NORMAL)
                    7 -> Typeface.create("cursive", Typeface.NORMAL)
                    8 -> Typeface.create("casual", Typeface.NORMAL)
                    9 -> Typeface.create("serif-monospace", Typeface.NORMAL)
                    10 -> Typeface.create("sans-serif-black", Typeface.NORMAL)
                    11 -> Typeface.create("sans-serif-black", Typeface.NORMAL)
                    12 -> ResourcesCompat.getFont(context, R.font.bbh_bartle) ?: Typeface.DEFAULT_BOLD
                    13 -> Typeface.MONOSPACE
                    else -> Typeface.create("casual", Typeface.BOLD)
                }
            }.getOrDefault(Typeface.DEFAULT)
        }
        return Typeface.create(base, style)
    }

    // ---------------------------------------------------------------- per-layer content

    private fun bucket(px: Float): Float {
        val v = px.coerceIn(8f, 4096f)
        return 1.12f.pow((ln(v) / ln(1.12f)).roundToInt())
    }

    fun content(l: Layer, W: Int): Content? {
        val target = l.size * W
        when (l.type) {
            LayerType.PLAYER -> {
                val bw = bucket(target).roundToInt().coerceAtLeast(16); val bh = (bw * PLAYER_ASPECT).roundToInt()
                val s = songs[l.songId]
                val art = art(s?.thumb)
                // Transparent overscan is essential: without it Android clips every
                // pixel outside the player bitmap before the poster compositor sees it.
                val bleed = (bw * .14f).roundToInt().coerceAtLeast(8)
                val key = "pl-edgeglow2:${l.songId}:${l.playerStyle}:${l.artSize}:${l.progress}:${l.glint}:${l.glintCorner}:${l.glintSize}:${l.glintGlow}:$bw:${art != null}"
                val bmp = bitmaps.get(key) ?: Bitmap.createBitmap(bw + bleed * 2, bh + bleed * 2, Bitmap.Config.ARGB_8888).also {
                    val canvas = Canvas(it)
                    canvas.translate(bleed.toFloat(), bleed.toFloat())
                    val dur = s?.durationSec ?: -1
                    val total = if (dur > 0) fmt(dur) else "--:--"; val el = if (dur > 0) fmt((dur * l.progress).toInt()) else "0:00"
                    PosterVectors.drawPlayer(canvas, bw.toFloat(), bh.toFloat(), l.playerStyle, art, s?.title ?: l.name, s?.artist ?: "", l.progress, el, total, l.artSize, brandLogo, l.glint, l.glintCorner, l.glintSize, l.glintGlow)
                    bitmaps.put(key, it)
                }
                return Content(bmp, key, target / bw, target, target * PLAYER_ASPECT)
            }
            LayerType.STICKER -> {
                val def = stickerDef(l.kind) ?: return null
                val bw = bucket(target).roundToInt().coerceAtLeast(8); val bh = max(1, (bw * def.aspect).roundToInt())
                val key = "st:${l.kind}:${l.color}:${l.color2}:$bw"
                val bmp = bitmaps.get(key) ?: Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888).also {
                    PosterVectors.drawSticker(Canvas(it), l.kind, bw.toFloat(), bh.toFloat(), l.color, l.color2); bitmaps.put(key, it)
                }
                return Content(bmp, key, target / bw)
            }
            LayerType.CHARACTER, LayerType.IMAGE -> {
                val src = source(l.uri, 2048) ?: return null
                val frame = if (l.type == LayerType.IMAGE) l.kind else ""
                val bw = min(bucket(target).roundToInt().coerceAtLeast(8), src.width.coerceAtLeast(8) * 2)
                val key = "im:${l.uri}:$frame:$bw:${l.color}"
                val bmp = bitmaps.get(key) ?: framedImage(src, bw, frame, l.color).also { bitmaps.put(key, it) }
                return Content(bmp, key, target / bw)
            }
            LayerType.TEXT -> {
                if (l.text.isBlank()) return null
                val px = bucket(target).coerceAtLeast(6f)
                val key = "tx:${l.text}:${l.font}:${l.bold}:${l.italic}:${l.align}:${l.letterSpacing}:${l.lineHeight}:${l.color}:${l.textBgOn}:${l.textBg}:${px.roundToInt()}"
                val bmp = bitmaps.get(key) ?: renderText(l, px).also { bitmaps.put(key, it) }
                return Content(bmp, key, target / px)
            }
            else -> return null
        }
    }

    private val cutBounds = java.util.concurrent.ConcurrentHashMap<String, RectF>()
    /** Bounding box of the subject inside its cut-out, as fractions (0..1) of the photo; null when the cut-out can't be read. */
    fun popBounds(path: String?): RectF? {
        if (path == null) return null
        cutBounds[path]?.let { return it }
        val b = source(path, 2048) ?: return null
        var x0 = b.width; var y0 = b.height; var x1 = -1; var y1 = -1
        val step = max(2, min(b.width, b.height) / 256)
        for (y in 0 until b.height step step) for (x in 0 until b.width step step) if ((b.getPixel(x, y) ushr 24) > 40) { if (x < x0) x0 = x; if (x > x1) x1 = x; if (y < y0) y0 = y; if (y > y1) y1 = y }
        if (x1 < x0 || y1 < y0) return null
        return RectF(x0.toFloat() / b.width, y0.toFloat() / b.height, (x1 + step).toFloat() / b.width, (y1 + step).toFloat() / b.height).also { cutBounds[path] = it }
    }

    /** The background subject's cut-out, scaled to the bucket that matches how wide it is drawn (keeps shadow/outline caches small and reusable while panning). */
    fun popContent(path: String?, dw: Float): Content? {
        val src = source(path, 2048) ?: return null
        val bw = min(bucket(dw).roundToInt().coerceAtLeast(8), src.width)
        val key = "bgcut:$path:$bw"
        val bmp = bitmaps.get(key) ?: (if (src.width == bw) src else Bitmap.createScaledBitmap(src, bw, max(1, (bw.toFloat() * src.height / src.width).roundToInt()), true)).also { bitmaps.put(key, it) }
        return Content(bmp, key, dw / bmp.width)
    }

    private fun fmt(sec: Int) = "${sec / 60}:${(sec % 60).toString().padStart(2, '0')}"

    private fun framedImage(src: Bitmap, bw: Int, frame: String, tint: Int): Bitmap {
        val ph = max(1, (bw.toFloat() * src.height / src.width).roundToInt())
        val inner = if (src.width == bw) src else Bitmap.createScaledBitmap(src, bw, ph, true)
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        return when (frame) {
            "polaroid" -> {
                val m = (bw * .06f).roundToInt(); val bottom = (bw * .22f).roundToInt()
                val out = Bitmap.createBitmap(bw + 2 * m, ph + m + bottom, Bitmap.Config.ARGB_8888); val c = Canvas(out)
                c.drawRect(0f, 0f, out.width.toFloat(), out.height.toFloat(), Paint().apply { color = 0xFFFAF8F2.toInt() })
                c.drawBitmap(inner, m.toFloat(), m.toFloat(), p); out
            }
            "round" -> {
                val out = Bitmap.createBitmap(bw, ph, Bitmap.Config.ARGB_8888); val c = Canvas(out)
                c.clipPath(Path().apply { addRoundRect(RectF(0f, 0f, bw.toFloat(), ph.toFloat()), bw * .08f, bw * .08f, Path.Direction.CW) })
                c.drawBitmap(inner, 0f, 0f, p); out
            }
            "film" -> {
                val bar = (bw * .09f).roundToInt()
                val out = Bitmap.createBitmap(bw, ph + 2 * bar, Bitmap.Config.ARGB_8888); val c = Canvas(out)
                c.drawRect(0f, 0f, out.width.toFloat(), out.height.toFloat(), Paint().apply { color = 0xFF141416.toInt() })
                c.drawBitmap(inner, 0f, bar.toFloat(), p)
                val s = bar * .4f; var x = s
                while (x < bw - s) { c.drawRoundRect(x, bar * .3f, x + s, bar * .7f, s * .2f, s * .2f, Paint().apply { color = 0xFFEDE6D4.toInt() }); c.drawRoundRect(x, out.height - bar * .7f, x + s, out.height - bar * .3f, s * .2f, s * .2f, Paint().apply { color = 0xFFEDE6D4.toInt() }); x += s * 2.1f }
                out
            }
            "paper" -> {
                val m = (bw * .04f).roundToInt()
                val out = Bitmap.createBitmap(bw + 2 * m, ph + 2 * m, Bitmap.Config.ARGB_8888); val c = Canvas(out)
                c.drawRect(0f, 0f, out.width.toFloat(), out.height.toFloat(), Paint().apply { color = 0xFFF1EBDD.toInt() })
                c.drawBitmap(inner, m.toFloat(), m.toFloat(), p); out
            }
            else -> inner
        }
    }

    /** measures and draws text with letter spacing, alignment, line height and optional label background */
    private fun renderText(l: Layer, px: Float): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = l.color; textSize = px; typeface = fontFor(l.font, l.bold, l.italic); letterSpacing = l.letterSpacing
        }
        val lines = l.text.split("\n")
        val widths = lines.map { paint.measureText(it) }
        val fm = paint.fontMetrics
        val lh = (fm.descent - fm.ascent) * l.lineHeight
        val padX = if (l.textBgOn) px * .45f else px * .1f; val padY = if (l.textBgOn) px * .3f else px * .08f
        val w = ((widths.maxOrNull() ?: 1f) + 2 * padX).roundToInt().coerceAtLeast(2)
        val h = (lh * lines.size - (lh - (fm.descent - fm.ascent)) + 2 * padY).roundToInt().coerceAtLeast(2)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); val c = Canvas(bmp)
        if (l.textBgOn) c.drawRoundRect(0f, 0f, w.toFloat(), h.toFloat(), px * .3f, px * .3f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = l.textBg })
        lines.forEachIndexed { i, s ->
            val x = when (l.align) { 0 -> padX; 2 -> w - padX - widths[i]; else -> (w - widths[i]) / 2f }
            c.drawText(s, x, padY - fm.ascent + i * lh, paint)
        }
        return bmp
    }

    // ---------------------------------------------------------------- fx (shadow / outline / soft edge) derived from the real alpha shape

    fun alphaMask(l: Layer, W: Int): Bitmap? {
        val c = content(l, W) ?: return null
        val key = "mask:${c.key}"
        return bitmaps.get(key) ?: run {
            val h = max(1, (40f * c.bmp.height / c.bmp.width).toInt())
            Bitmap.createScaledBitmap(c.bmp, 40, h, true).also { bitmaps.put(key, it) }
        }
    }

    fun fx(key: String, make: () -> Fx): Fx = fxCache.get(key) ?: make().also { fxCache.put(key, it) }
}

/** Alpha-shape helpers: everything here is computed from the object's own transparency, so shadows hug hair and clothing instead of a box. */
object AlphaFx {
    fun tinted(src: Bitmap, color: Int, grow: Int): Fx {
        val g = max(0, grow)
        val out = Bitmap.createBitmap(src.width + 2 * g, src.height + 2 * g, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val p = Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN) }
        if (g == 0) { c.drawBitmap(src, 0f, 0f, p); return Fx(out, 0, 0) }
        val steps = (2 * PI * g / 2.0).toInt().coerceIn(16, 56)
        for (ring in listOf(g.toFloat(), g * .55f)) {
            for (i in 0 until steps) {
                val a = 2 * PI * i / steps
                c.drawBitmap(src, g + (cos(a) * ring).toFloat(), g + (sin(a) * ring).toFloat(), p)
            }
        }
        c.drawBitmap(src, g.toFloat(), g.toFloat(), p)
        return Fx(out, -g, -g)
    }

    fun blurredShadow(src: Bitmap, blur: Float, spread: Int): Fx {
        val base = if (spread > 0) tinted(src, Color.BLACK, spread) else Fx(src, 0, 0)
        val off = IntArray(2)
        val paint = Paint().apply { if (blur >= 0.5f) maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL) }
        val alpha = base.bmp.extractAlpha(paint, off)
        return Fx(alpha, base.ox + off[0], base.oy + off[1])
    }

    fun soften(src: Bitmap, radius: Float): Bitmap {
        if (radius < 0.5f) return src
        val out = src.copy(Bitmap.Config.ARGB_8888, true)
        val off = IntArray(2)
        val soft = src.extractAlpha(Paint().apply { maskFilter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL) }, off)
        val c = Canvas(out)
        c.drawBitmap(soft, off[0].toFloat(), off[1].toFloat(), Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) })
        return out
    }
}

class RenderResult(val boxes: Map<String, FloatArray>)

object PosterRenderer {
    fun bgMax(W: Int, H: Int): Int = max(W, H).coerceAtMost(3072).coerceAtLeast(1024)

    fun sizeFor(doc: PosterDoc, width: Int): Pair<Int, Int> {
        var w = width.coerceIn(64, 4096); var h = (w * doc.aspect).roundToInt()
        val maxPx = 16_000_000f
        if (w.toFloat() * h > maxPx) { val s = kotlin.math.sqrt(maxPx / (w.toFloat() * h)); w = (w * s).toInt(); h = (h * s).toInt() }
        return w to h
    }

    /** One renderer for both the live preview and the export, so layer order, shadows and effects always match. */
    fun render(
        target: Bitmap, doc: PosterDoc, assets: PosterAssets, transparentBg: Boolean = false, check: () -> Unit = {},
    ): RenderResult {
        target.eraseColor(Color.TRANSPARENT)
        val canvas = Canvas(target)
        val W = target.width; val H = target.height
        val boxes = HashMap<String, FloatArray>()
        if (!transparentBg) drawBackground(canvas, W, H, doc, assets)
        // background effect: drawn straight after the photo, so only the photo gets it (the popped-out subject and everything else stay clean)
        if (!transparentBg && doc.bgFx.isNotBlank()) effectDef(doc.bgFx)?.let { def ->
            val fxLayer = Layer(id = "__bgfx", type = LayerType.EFFECT, name = def.label, kind = def.key, x = def.x, y = def.y, size = def.size, intensity = doc.bgFxAmount, blend = def.blend, color = def.color, color2 = def.color2)
            val mark0 = canvas.save()
            try { drawLayer(canvas, target, W, H, doc, fxLayer, assets, HashMap()) } catch (e: Exception) { } finally { canvas.restoreToCount(mark0) }
        }
        // Depth only where it can matter: the photo's subject is drawn after the last player / text / sticker, but only inside the area those occupy,
        // so it covers them exactly where the subject overlaps them (and casts a soft shadow on them). Nothing overlaps -> the poster is just the photo.
        val behindTypes = setOf(LayerType.PLAYER, LayerType.TEXT, LayerType.STICKER)
        val ordered = doc.renderOrder()
        val popAfter = if (doc.bgPop && !transparentBg && doc.bgCutUri != null) ordered.indexOfLast { it.visible && it.type in behindTypes } else -2
        val behind = Path()
        for ((i, l) in ordered.withIndex()) {
            check()
            if (l.visible) {
                val mark = canvas.save()
                try { drawLayer(canvas, target, W, H, doc, l, assets, boxes) } catch (e: Exception) { /* a broken layer must not take the poster down */ } finally { canvas.restoreToCount(mark) }
                if (l.type in behindTypes && popAfter >= 0) boxes[l.id]?.let { addOutline(behind, l, it[0], it[1], W, H) }
            }
            if (i == popAfter) drawBgPop(canvas, W, H, doc, assets, behind)
        }
        return RenderResult(boxes)
    }

    /** where the background photo lands on the poster (shared by the photo and its cut-out so they stay pixel-aligned) */
    private fun bgRect(W: Int, H: Int, doc: PosterDoc, srcW: Int, srcH: Int): RectF {
        val cover = max(W.toFloat() / srcW, H.toFloat() / srcH) * doc.bgScale.coerceIn(1f, 5f)
        val dw = srcW * cover; val dh = srcH * cover
        val maxX = max(0f, (dw - W) / 2f); val maxY = max(0f, (dh - H) / 2f)
        val ox = (doc.bgX * W).coerceIn(-maxX, maxX); val oy = (doc.bgY * H).coerceIn(-maxY, maxY)
        return RectF((W - dw) / 2f + ox, (H - dh) / 2f + oy, (W + dw) / 2f + ox, (H + dh) / 2f + oy)
    }

    private fun bgColorFilter(doc: PosterDoc): ColorMatrixColorFilter {
        val g = max(.05f, doc.contrast * 2f.pow(doc.exposure * 1.2f)); val t = (1 - doc.contrast) * 128f + doc.brightness * 110f
        val cm = ColorMatrix().apply { setSaturation(doc.saturation); postConcat(ColorMatrix(floatArrayOf(g, 0f, 0f, 0f, t, 0f, g, 0f, 0f, t, 0f, 0f, g, 0f, t, 0f, 0f, 0f, 1f, 0f))) }
        return ColorMatrixColorFilter(cm)
    }

    /** the on-poster outline of a layer (rounded card for players), rotated and tilted exactly like the layer itself */
    private fun addOutline(path: Path, l: Layer, w: Float, h: Float, W: Int, H: Int) {
        val p = Path()
        val r = RectF(-w / 2f, -h / 2f, w / 2f, h / 2f)
        if (l.type == LayerType.PLAYER) p.addRoundRect(r, w * .12f, w * .12f, Path.Direction.CW) else p.addRect(r, Path.Direction.CW)
        val m = Matrix()
        tiltMatrix(l, w, h)?.let { m.postConcat(it) }
        m.postRotate(l.rotation); m.postTranslate(l.x * W, l.y * H)
        p.transform(m)
        path.addPath(p)
    }

    /**
     * The photo's subject in front of whatever sits behind it. Drawn only inside [behind] (feathered edge), so the subject is the same pixels as the
     * photo everywhere else; inside it, the subject hides the player/text and casts a soft contact shadow onto it. Matches the photo's grade/overlay.
     */
    private fun drawBgPop(c: Canvas, W: Int, H: Int, doc: PosterDoc, assets: PosterAssets, behind: Path) {
        if (behind.isEmpty) return
        val bg = assets.source(doc.bgUri, bgMax(W, H)) ?: return
        val rect = bgRect(W, H, doc, bg.width, bg.height)
        val nb = assets.popBounds(doc.bgCutUri) ?: return
        val subject = RectF(rect.left + nb.left * rect.width(), rect.top + nb.top * rect.height(), rect.left + nb.right * rect.width(), rect.top + nb.bottom * rect.height())
        val area = RectF(); behind.computeBounds(area, true)
        if (!RectF.intersects(area, subject)) return // the subject doesn't reach anything: no depth needed
        val content = assets.popContent(doc.bgCutUri, rect.width()) ?: return
        val di = doc.bgPopIntensity.coerceIn(0f, 1f)
        val feather = max(2f, W * .006f)
        area.inset(-feather * 4f, -feather * 4f)
        val pseudo = quickDepth(
            Layer(id = "__bgpop", type = LayerType.CHARACTER, name = "bgpop", x = rect.centerX() / W, y = rect.centerY() / H, size = rect.width() / W),
            outline = false,
        ).copy(depthIntensity = di, shadowMode = 0, shadowOpacity = .3f + .35f * di, shadowDist = .03f + .035f * di, shadowBlur = .2f + .12f * di, shadowAngle = 90f, elevation = .5f, edgeSoftness = 0f, highlight = .08f + .06f * di)
        val grade = Paint().apply { colorFilter = bgColorFilter(doc) }
        val outer = c.saveLayer(area, null)
        drawObject(c, content, pseudo, W.toFloat(), H.toFloat(), assets, grade, lift = false)
        if (doc.overlayOpacity > 0.01f) c.drawRect(area, Paint().apply { color = withAlpha(doc.overlayColor, doc.overlayOpacity); xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP) })
        // feathered mask: keep the result only where players/text/stickers are (grown by the feather so they are fully covered)
        val maskLayer = c.saveLayer(area, Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) })
        c.drawPath(behind, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.FILL_AND_STROKE; strokeWidth = feather * 2f; strokeJoin = Paint.Join.ROUND; maskFilter = BlurMaskFilter(feather, BlurMaskFilter.Blur.NORMAL) })
        c.restoreToCount(maskLayer)
        c.restoreToCount(outer)
    }

    private fun drawBackground(c: Canvas, W: Int, H: Int, doc: PosterDoc, assets: PosterAssets) {
        c.drawColor(doc.bgColor)
        val src = assets.source(doc.bgUri, bgMax(W, H))
        if (src != null) {
            val dst = bgRect(W, H, doc, src.width, src.height)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            paint.colorFilter = bgColorFilter(doc)
            var bmp = src
            if (doc.blur > 0.02f) {
                val key = "bgblur:${doc.bgUri}:${(doc.blur * 20).roundToInt()}:${W}"
                val fb = assets.fx(key) {
                    val small = if (src.width > W) Bitmap.createScaledBitmap(src, W, max(1, W * src.height / src.width), true) else src
                    Fx(PosterEffects.fastBlur(small, (2 + doc.blur * 30).toInt()), 0, 0)
                }
                bmp = fb.bmp
            }
            c.drawBitmap(bmp, null, dst, paint)
        }
        if (doc.overlayOpacity > 0.01f) c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), Paint().apply { color = withAlpha(doc.overlayColor, doc.overlayOpacity) })
    }

    private fun layerPaint(l: Layer): Paint? {
        val mode = blendModeOf(l.blend)
        if (l.opacity >= 0.995f && mode == null) return null
        return Paint().apply { alpha = (l.opacity.coerceIn(0f, 1f) * 255).toInt(); if (mode != null) xfermode = PorterDuffXfermode(mode) }
    }

    private fun drawLayer(c: Canvas, target: Bitmap, W: Int, H: Int, doc: PosterDoc, l: Layer, assets: PosterAssets, boxes: HashMap<String, FloatArray>) {
        val wf = W.toFloat(); val hf = H.toFloat()
        val lp = layerPaint(l)
        when (l.type) {
            LayerType.EFFECT -> {
                val def = effectDef(l.kind)
                val snap = if (l.kind in PosterEffects.NeedsSnapshot) target.copy(Bitmap.Config.ARGB_8888, false) else null
                // colour effects blend by intensity as well as by opacity
                val colourFx = def?.group == "Color"
                val paint = Paint().apply {
                    alpha = ((l.opacity * (if (colourFx) l.intensity else 1f)).coerceIn(0f, 1f) * 255).toInt()
                    blendModeOf(l.blend)?.let { xfermode = PorterDuffXfermode(it) }
                }
                c.saveLayer(0f, 0f, wf, hf, paint)
                PosterEffects.draw(c, l.kind, wf, hf, l, snap)
                c.restore()
                boxes[l.id] = floatArrayOf(l.size * wf, l.size * wf)
            }
            LayerType.FRAME, LayerType.OVERLAY -> {
                val isBlur = l.type == LayerType.OVERLAY && l.kind == "blur"
                val snap = if (isBlur) target.copy(Bitmap.Config.ARGB_8888, false) else null
                c.saveLayer(0f, 0f, wf, hf, lp)
                c.save(); c.translate(l.x * wf, l.y * hf); c.rotate(l.rotation)
                val s = l.size.coerceIn(.2f, 3f); c.scale(s * (if (l.flipH) -1f else 1f), s * (if (l.flipV) -1f else 1f)); c.translate(-wf / 2, -hf / 2)
                if (isBlur && snap != null) {
                    val b = PosterEffects.fastBlur(snap, (2 + l.intensity * 28).toInt()); c.save(); c.clipRect(0f, 0f, wf, hf)
                    c.setMatrix(null); c.drawBitmap(b, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG)); c.restore()
                } else if (l.type == LayerType.FRAME) PosterVectors.drawFrame(c, l.kind, wf, hf)
                else PosterEffects.drawOverlay(c, l.kind, wf, hf, l)
                c.restore(); c.restore()
                boxes[l.id] = floatArrayOf(l.size * wf, l.size * hf)
            }
            else -> {
                val content = assets.content(l, W) ?: return
                drawObject(c, content, l, wf, hf, assets, lp)
                boxes[l.id] = floatArrayOf(content.width, content.height)
            }
        }
    }

    /**
     * True perspective tilt. The object's rectangle is rotated in 3D (Y then X, CSS convention: positive tiltY sends the right edge away,
     * positive tiltX brings the bottom edge closer) and projected through a pinhole camera whose distance is proportional to the object's
     * own size, so the look is identical in the editor preview and in any export resolution. [zOff] pushes the plane away from the viewer
     * (negative = behind) and is used for the card thickness. Works in output pixels, around the object's centre.
     */
    private fun tiltMatrix(l: Layer, w: Float, h: Float, zOff: Float = 0f): Matrix? {
        if (l.tiltY == 0f && l.tiltX == 0f) return null
        val ry = Math.toRadians(l.tiltY.toDouble()); val rx = Math.toRadians(l.tiltX.toDouble())
        val cy = cos(ry).toFloat(); val sy = sin(ry).toFloat(); val cx = cos(rx).toFloat(); val sx = sin(rx).toFloat()
        val d = max(w, h * .75f).coerceAtLeast(1f) * 1.0f // camera distance: close, so even a gentle turn shows real perspective (near edge grows, far edge shrinks)
        val hw = w / 2f; val hh = h / 2f
        val src = floatArrayOf(-hw, -hh, hw, -hh, hw, hh, -hw, hh)
        val dst = FloatArray(8)
        for (i in 0 until 4) {
            val x = src[i * 2]; val y = src[i * 2 + 1]; val z = zOff
            val x1 = x * cy + z * sy; val z1 = -x * sy + z * cy
            val y2 = y * cx - z1 * sx; val z2 = y * sx + z1 * cx
            val k = d / (d - z2).coerceAtLeast(d * .2f)
            dst[i * 2] = x1 * k; dst[i * 2 + 1] = y2 * k
        }
        val m = Matrix()
        return if (m.setPolyToPoly(src, 0, dst, 0, 4)) m else null
    }

    /** 0..1: how far the object is turned away (drives face shading and card thickness) */
    private fun tiltAmount(l: Layer) = min(1f, max(kotlin.math.abs(l.tiltY), kotlin.math.abs(l.tiltX)) / 24f)

    private fun drawObject(c: Canvas, content: Content, l: Layer, W: Float, H: Float, assets: PosterAssets, lp: Paint?, lift: Boolean = true) {
        val bmp = content.bmp
        val unit = W / 1080f
        val cx = l.x * W; val cy = l.y * H
        val dw = content.width
        val tilt = tiltMatrix(l, dw, content.height)
        val elev = l.elevation.coerceIn(0f, 1f)
        val depth = l.depthStyle != 0

        // --- resolve the visual recipe. Depth styles add to the manual shadow/outline values the user can still tune.
        val di = l.depthIntensity.coerceIn(0f, 1f)
        var shadowOn = l.shadowOn; var sOp = l.shadowOpacity; var sBlur = l.shadowBlur; var sDist = l.shadowDist; var sSpread = l.shadowSpread
        var outlineOn = l.outlineOn; var oW = l.outlineWidth; var oCol = l.outlineColor
        var extrude = 0; var scaleUp = 1f; var hiAmt = 0f; var contrast = 1f; var doubleOutline = false; var extrudeColor = Color.BLACK
        if (depth) {
            shadowOn = true
            when (l.depthStyle) {
                1 -> { sDist += .08f * di; sBlur += .15f * di; scaleUp += .02f * di; hiAmt = .15f }
                2 -> { sDist += .06f * di; sBlur += .12f * di; scaleUp += .05f * di; hiAmt = l.highlight; contrast = 1f + .1f * di }
                3 -> { extrude = (4 + 10 * di).toInt(); hiAmt = l.highlight * .6f; extrudeColor = mixColor(l.shadowColor, Color.BLACK, .3f) }
                4 -> { outlineOn = true; oW = max(oW, .02f); hiAmt = max(l.highlight, .3f); sDist += .04f * di }
                5 -> { sDist += .03f * di }
                6 -> { outlineOn = true; oW = max(oW, .035f); oCol = 0xFFF8F4EA.toInt(); sBlur = .05f; sDist = .05f + .04f * di }
                7 -> { outlineOn = true; doubleOutline = true; oW = max(oW, .03f); sBlur = 0f; sDist = .06f + .05f * di; sOp = 1f }
            }
        }
        if (!lift) scaleUp = 1f // background pop-out must stay pixel-aligned with the photo
        val isPlayer = l.type == LayerType.PLAYER
        if (isPlayer) shadowOn = false // players get the automatic three-layer shadow below; nothing to configure
        sDist *= (.6f + elev * .8f); sBlur *= (.6f + elev * .8f)
        when (l.shadowMode) { 1 -> { sBlur *= 1.3f; sOp *= .8f }; 2 -> { sBlur *= .5f; sDist *= 1.4f; sOp = min(1f, sOp * 1.4f) }; 3 -> { sDist *= 1.8f; sBlur *= 1.5f; sOp *= .7f } }

        var drawSrc = bmp
        val soft = l.edgeSoftness * 14f * unit / content.scale
        val softKey = "soft:${content.key}:${(soft * 4).roundToInt()}"
        if (soft >= .5f && (l.type == LayerType.CHARACTER || l.type == LayerType.IMAGE)) drawSrc = assets.fx(softKey) { Fx(AlphaFx.soften(bmp, soft), 0, 0) }.bmp

        val px = content.scale // content pixels -> output pixels

        // Only the area this object (plus its shadow / outline / extrusion) can touch needs an offscreen layer; a full-poster layer per object was the main cause of drag lag.
        val blurPxB = sBlur * dw * .12f
        val layersB = if (l.depthStyle == 5) 3 else 1
        val kdB = 1f + (layersB - 1) * .6f
        val distB = sDist * dw * .5f
        val shadowReach = if (shadowOn && sOp > 0.01f) (if (l.shadowMode == 3 || l.depthStyle == 1) distB * kdB * 1.4f + dw * .1f else distB * kdB) + blurPxB * (1f + (layersB - 1) * .9f) * 3f + sSpread * dw * .05f * 2f else 0f
        val growB = max(1, (oW * dw / px * .5f).roundToInt()) * px * (if (doubleOutline) 2.2f else 1.1f)
        val reach = max(if (isPlayer) dw * .75f else 0f, max(shadowReach, max(if (outlineOn) growB else 0f, if (extrude > 0) dw * (.015f + .035f * di) * 1.2f else 0f)) + 6f + soft * px)
        val half = kotlin.math.hypot(dw, content.height) / 2f * scaleUp
        val bounds = if (tilt != null) RectF(0f, 0f, W, H) else RectF(cx - half - reach, cy - half - reach, cx + half + reach, cy + half + reach)
        c.saveLayer(bounds, lp)

        fun place(dx: Float, dy: Float, body: () -> Unit) {
            c.save()
            c.translate(cx + dx, cy + dy); c.rotate(l.rotation)
            if (tilt != null) c.concat(tilt)
            c.scale((if (l.flipH) -1f else 1f) * scaleUp, (if (l.flipV) -1f else 1f) * scaleUp)
            c.scale(px, px)
            body(); c.restore()
        }
        fun ang() = l.shadowAngle * PI.toFloat() / 180f

        // 0) players: automatic, physically-minded shadow. Light is above: a tight contact shadow, a key shadow and a long soft ambient shadow.
        if (isPlayer) {
            val f = .6f + elev * .8f
            val layers = floatArrayOf(.016f, .010f, .62f, .05f, .042f, .42f, .15f, .125f, .30f) // blur, drop (fractions of the card width), strength
            for (k in 0 until 3) {
                val kb = layers[k * 3] * dw / px
                val fx = assets.fx("psh:${content.key}:${kb.roundToInt()}") { AlphaFx.blurredShadow(drawSrc, max(0f, kb), 0) }
                val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { color = Color.BLACK; alpha = (layers[k * 3 + 2] * f * 255).toInt().coerceIn(0, 255) }
                place(0f, layers[k * 3 + 1] * dw * f) { c.drawBitmap(fx.bmp, fx.ox - bmp.width / 2f, fx.oy - bmp.height / 2f, paint) }
            }
        }
        // 1) shadow, offset in poster space so the light direction stays constant even when the object rotates
        if (shadowOn && sOp > 0.01f) {
            val blurPx = sBlur * dw * .12f; val dist = sDist * dw * .5f; val spreadPx = (sSpread * dw * .05f).roundToInt()
            val sdx = cos(ang()) * dist; val sdy = sin(ang()) * dist
            val layers = if (l.depthStyle == 5) 3 else 1
            for (k in 0 until layers) {
                val kb = blurPx * (1f + k * .9f) / px; val kd = (1f + k * .6f)
                val sk = "sh:${content.key}:${soft.roundToInt()}:${(kb * 2).roundToInt()}:$spreadPx"
                val fx = assets.fx(sk) { AlphaFx.blurredShadow(drawSrc, max(0f, kb), (spreadPx / px).roundToInt()) }
                val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { color = withAlpha(l.shadowColor, 1f); alpha = (sOp / (layers * .75f) * 255).toInt().coerceIn(0, 255) }
                if (l.shadowMode == 3 || l.depthStyle == 1) {
                    // floating: also a squashed contact shadow far below the object
                    place(sdx * kd * .5f, dw * .08f + sdy * kd * 1.2f) { c.scale(1f, .25f); c.drawBitmap(fx.bmp, fx.ox - bmp.width / 2f, fx.oy - bmp.height / 2f, paint) }
                } else place(sdx * kd, sdy * kd) { c.drawBitmap(fx.bmp, fx.ox - bmp.width / 2f, fx.oy - bmp.height / 2f, paint) }
            }
        }
        // 1b) objects that carry depth get a second, tight contact shadow right under their edge: it is what makes them read as standing in front of the card
        if (depth && !isPlayer && lift && l.depthStyle != 3) {
            val kb = max(1f, dw * (.006f + .008f * di) / px)
            val fx = assets.fx("cs:${content.key}:${soft.roundToInt()}:${(kb * 2).roundToInt()}") { AlphaFx.blurredShadow(drawSrc, kb, 0) }
            val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { color = withAlpha(l.shadowColor, 1f); alpha = ((.28f + .3f * di) * 255).toInt().coerceIn(0, 255) }
            val dd = dw * (.006f + .01f * di)
            place(cos(ang()) * dd, sin(ang()) * dd) { c.drawBitmap(fx.bmp, fx.ox - bmp.width / 2f, fx.oy - bmp.height / 2f, paint) }
        }
        // 1c) card thickness: when a player is turned, its edge is stacked backwards in real perspective so it reads as a solid slab, not a flat sticker
        if (isPlayer && tilt != null) {
            val amt = tiltAmount(l)
            val thick = dw * (.032f + .04f * amt)
            val n = 12
            val rr = dw * .12f
            val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
            val rect = RectF(-dw / 2f, -content.height / 2f, dw / 2f, content.height / 2f)
            for (i in n downTo 1) {
                val t = i / n.toFloat()
                val m = tiltMatrix(l, dw, content.height, -thick * t) ?: continue
                // darker the further back, with a light rim on the first layer so the edge catches the light
                val (e0, e1) = when (l.playerStyle) { 2 -> 0xFFC9C9D2.toInt() to 0xFF74747E.toInt(); 3 -> 0xFF6B5645.toInt() to 0xFF20160F.toInt(); 1 -> 0xFF8E94A6.toInt() to 0xFF2A2C36.toInt(); else -> 0xFF4A4A52.toInt() to 0xFF0C0C0E.toInt() }
                edge.color = mixColor(e0, e1, t)
                c.save(); c.translate(cx, cy); c.rotate(l.rotation); c.concat(m)
                c.drawRoundRect(rect, rr, rr, edge)
                c.restore()
            }
        }
        // 2) extrusion (Soft 3D): stacked silhouettes behind the object
        if (extrude > 0) {
            val ek = "ex:${content.key}:${l.shadowColor}"
            val sil = assets.fx(ek) { AlphaFx.tinted(drawSrc, extrudeColor, 0) }
            val n = extrude; val total = dw * (.015f + .035f * di); val dx = cos(ang()) * total / n; val dy = sin(ang()) * total / n
            for (i in n downTo 1) place(dx * i, dy * i) { c.drawBitmap(sil.bmp, -bmp.width / 2f, -bmp.height / 2f, Paint(Paint.FILTER_BITMAP_FLAG)) }
        }
        // 3) outline / sticker border from the alpha shape
        if (outlineOn) {
            val grow = max(1, (oW * dw / px * .5f).roundToInt())
            if (doubleOutline) {
                val outer = assets.fx("ol2:${content.key}:${soft.roundToInt()}:$grow") { AlphaFx.tinted(drawSrc, Color.BLACK, grow * 2) }
                place(0f, 0f) { c.drawBitmap(outer.bmp, outer.ox - bmp.width / 2f, outer.oy - bmp.height / 2f, Paint(Paint.FILTER_BITMAP_FLAG)) }
                val inner = assets.fx("ol1:${content.key}:${soft.roundToInt()}:$grow:$oCol") { AlphaFx.tinted(drawSrc, Color.WHITE, grow) }
                place(0f, 0f) { c.drawBitmap(inner.bmp, inner.ox - bmp.width / 2f, inner.oy - bmp.height / 2f, Paint(Paint.FILTER_BITMAP_FLAG)) }
            } else {
                val fx = assets.fx("ol:${content.key}:${soft.roundToInt()}:$grow:$oCol") { AlphaFx.tinted(drawSrc, oCol, grow) }
                place(0f, 0f) { c.drawBitmap(fx.bmp, fx.ox - bmp.width / 2f, fx.oy - bmp.height / 2f, Paint(Paint.FILTER_BITMAP_FLAG)) }
            }
        }
        // 4) the object itself (+ rim highlight clipped to its own alpha)
        val bodyPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        if (contrast != 1f) bodyPaint.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(1.05f); postConcat(ColorMatrix(floatArrayOf(contrast, 0f, 0f, 0f, (1 - contrast) * 128f, 0f, contrast, 0f, 0f, (1 - contrast) * 128f, 0f, 0f, contrast, 0f, (1 - contrast) * 128f, 0f, 0f, 0f, 1f, 0f))) })
        val shadeAmt = if (tilt != null && l.type != LayerType.TEXT) tiltAmount(l) else 0f
        place(0f, 0f) {
            val bw = bmp.width.toFloat(); val bh = bmp.height.toFloat()
            if (hiAmt > 0.01f || shadeAmt > 0.01f) {
                c.saveLayer(-bw / 2, -bh / 2, bw / 2, bh / 2, null)
                c.drawBitmap(drawSrc, -bw / 2, -bh / 2, bodyPaint)
                if (shadeAmt > 0.01f) {
                    // light comes from the front-left: the edge turned away falls into shade, the edge turned toward us picks up a soft sheen
                    val sh = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP) }
                    val ty = (l.tiltY / 30f).coerceIn(-1f, 1f); val tx = (l.tiltX / 30f).coerceIn(-1f, 1f)
                    if (kotlin.math.abs(ty) > .01f) {
                        val far = if (ty > 0) 1f else -1f // +tiltY: right edge recedes
                        sh.shader = LinearGradient(far * bw / 2f, 0f, -far * bw / 2f, 0f,
                            intArrayOf(withAlpha(Color.BLACK, .42f * kotlin.math.abs(ty)), withAlpha(Color.BLACK, .06f * kotlin.math.abs(ty)), withAlpha(Color.WHITE, .16f * kotlin.math.abs(ty))), floatArrayOf(0f, .55f, 1f), Shader.TileMode.CLAMP)
                        c.drawRect(-bw / 2, -bh / 2, bw / 2, bh / 2, sh)
                    }
                    if (kotlin.math.abs(tx) > .01f) {
                        val far = if (tx > 0) -1f else 1f // +tiltX: bottom edge comes closer, top recedes
                        sh.shader = LinearGradient(0f, far * bh / 2f, 0f, -far * bh / 2f,
                            intArrayOf(withAlpha(Color.BLACK, .34f * kotlin.math.abs(tx)), withAlpha(Color.BLACK, .04f * kotlin.math.abs(tx)), withAlpha(Color.WHITE, .14f * kotlin.math.abs(tx))), floatArrayOf(0f, .55f, 1f), Shader.TileMode.CLAMP)
                        c.drawRect(-bw / 2, -bh / 2, bw / 2, bh / 2, sh)
                    }
                }
            }
            if (hiAmt > 0.01f) {
                val a = ang() + PI.toFloat()
                val gx = cos(a) * bw * .5f; val gy = sin(a) * bh * .5f
                val hp = Paint().apply {
                    shader = LinearGradient(gx, gy, -gx, -gy, intArrayOf(withAlpha(Color.WHITE, hiAmt), 0, 0), floatArrayOf(0f, .45f, 1f), Shader.TileMode.CLAMP)
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
                }
                c.drawRect(-bw / 2, -bh / 2, bw / 2, bh / 2, hp)
            }
            if (hiAmt > 0.01f || shadeAmt > 0.01f) c.restore()
            else c.drawBitmap(drawSrc, -bw / 2, -bh / 2, bodyPaint)
        }
        c.restore()
    }

    /** small bitmap used by the library cards so every effect has a real preview */
    fun effectPreview(key: String, w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); val c = Canvas(bmp)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply { shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), 0xFF5B6B8C.toInt(), 0xFF2A2630.toInt(), Shader.TileMode.CLAMP) })
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE8D7B8.toInt() }
        c.drawCircle(w * .5f, h * .38f, w * .16f, p); c.drawRoundRect(w * .28f, h * .52f, w * .72f, h * 1.1f, w * .16f, w * .16f, p)
        c.drawRect(0f, h * .85f, w.toFloat(), h.toFloat(), Paint().apply { color = 0xFF3A3A42.toInt() })
        val def = effectDef(key) ?: return bmp
        val layer = Layer(id = "preview-$key", type = LayerType.EFFECT, name = key, kind = key, x = def.x, y = def.y, size = def.size, intensity = min(1f, def.intensity + .15f), color = def.color, color2 = def.color2)
        val snap = if (key in PosterEffects.NeedsSnapshot) bmp.copy(Bitmap.Config.ARGB_8888, false) else null
        val paint = Paint().apply { blendModeOf(def.blend)?.let { xfermode = PorterDuffXfermode(it) } }
        c.saveLayer(0f, 0f, w.toFloat(), h.toFloat(), paint)
        PosterEffects.draw(c, key, w.toFloat(), h.toFloat(), layer, snap)
        c.restore()
        return bmp
    }
}
