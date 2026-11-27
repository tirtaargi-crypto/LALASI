package com.danrey.lalasi.ui.screens.playlist

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteOrder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Subject cut-out. Order of attempts:
 *  1. ML Kit selfie/person segmentation (on-device, model bundled).
 *  2. If ML Kit throws (e.g. release/R8 build where its internals are missing) or finds no subject,
 *     a built-in contrast based segmenter that works for characters/objects on a plain-ish background.
 * Returns a new ARGB bitmap whose alpha is the subject mask. Never throws for "ML Kit failed"; only if both paths fail.
 */
suspend fun segmentSubject(source: Bitmap, context: Context? = null): Bitmap {
    val ml = try {
        segmentWithMlKit(source, context)
    } catch (e: CancellationException) {
        throw e
    } catch (t: Throwable) {
        null
    }
    if (ml != null) return ml
    return segmentByContrast(source)
}

/** Result of the person model, or null when it failed or found (almost) nothing/everything. */
private suspend fun segmentWithMlKit(source: Bitmap, context: Context?): Bitmap? {
    // Make sure ML Kit's own context exists even if its init provider was stripped or ran late.
    if (context != null) runCatching {
        Class.forName("com.google.mlkit.common.sdkinternal.MlKitContext")
            .getMethod("initializeIfNeeded", Context::class.java).invoke(null, context.applicationContext)
    }
    val options = SelfieSegmenterOptions.Builder()
        .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
        .enableRawSizeMask()
        .build()
    val client = Segmentation.getClient(options)
    try {
        val mask = suspendCancellableCoroutine<com.google.mlkit.vision.segmentation.SegmentationMask> { cont ->
            client.process(InputImage.fromBitmap(source, 0))
                .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
                .addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
        }
        val probs = mask.buffer.order(ByteOrder.nativeOrder()).asFloatBuffer()
        val mw = mask.width; val mh = mask.height
        if (mw < 2 || mh < 2) return null
        val w = source.width; val h = source.height
        // work at <= 720 px: sample the 256 px model mask bilinearly, then snap it to the photo's real edges with a guided filter
        val k = min(1f, 720f / max(w, h))
        val sw = max(8, (w * k).toInt()); val sh = max(8, (h * k).toInt())
        val prob = FloatArray(sw * sh)
        for (y in 0 until sh) {
            val fy = y.toFloat() * (mh - 1) / (sh - 1).coerceAtLeast(1)
            val y0 = fy.toInt().coerceIn(0, mh - 1); val y1 = (y0 + 1).coerceAtMost(mh - 1); val ty = fy - y0
            for (x in 0 until sw) {
                val fx = x.toFloat() * (mw - 1) / (sw - 1).coerceAtLeast(1)
                val x0 = fx.toInt().coerceIn(0, mw - 1); val x1 = (x0 + 1).coerceAtMost(mw - 1); val tx = fx - x0
                prob[y * sw + x] = (probs.get(y0 * mw + x0) * (1 - tx) + probs.get(y0 * mw + x1) * tx) * (1 - ty) +
                    (probs.get(y1 * mw + x0) * (1 - tx) + probs.get(y1 * mw + x1) * tx) * ty
            }
        }
        val refined = guidedFilter(lumaSmall(source, sw, sh), prob, sw, sh, 6, 0.0025f)
        var covered = 0
        for (v in refined) if (v > 0.5f) covered++
        val frac = covered.toFloat() / refined.size
        if (frac < 0.015f || frac > 0.97f) return null
        return applyAlpha(source, refined, sw, sh, 0.38f, 0.72f)
    } finally {
        runCatching { client.close() }
    }
}

/**
 * Fallback: estimates the backdrop colour from the image border, marks pixels that differ from it,
 * keeps the connected region that sits near the centre, closes small holes and feathers the edge.
 * Approximate by design; the brush in the Cutout editor is for the last bit of polish.
 */
fun segmentByContrast(source: Bitmap): Bitmap {
    val w = source.width; val h = source.height
    val maxSide = 320
    val s = min(1f, maxSide.toFloat() / max(w, h))
    val sw = max(8, (w * s).toInt()); val sh = max(8, (h * s).toInt())
    val small = Bitmap.createScaledBitmap(source, sw, sh, true)
    val px = IntArray(sw * sh); small.getPixels(px, 0, sw, 0, 0, sw, sh)
    if (small !== source) small.recycle()

    // border statistics (3 px ring)
    var n = 0; var mr = 0.0; var mg = 0.0; var mb = 0.0
    fun isBorder(x: Int, y: Int) = x < 3 || y < 3 || x >= sw - 3 || y >= sh - 3
    for (y in 0 until sh) for (x in 0 until sw) if (isBorder(x, y)) {
        val c = px[y * sw + x]; mr += (c shr 16) and 255; mg += (c shr 8) and 255; mb += c and 255; n++
    }
    mr /= n; mg /= n; mb /= n
    var vr = 0.0
    for (y in 0 until sh) for (x in 0 until sw) if (isBorder(x, y)) {
        val c = px[y * sw + x]
        val dr = ((c shr 16) and 255) - mr; val dg = ((c shr 8) and 255) - mg; val db = (c and 255) - mb
        vr += dr * dr + dg * dg + db * db
    }
    val sd = sqrt(vr / n / 3.0)
    val thr = max(28.0, sd * 2.6)

    val diff = BooleanArray(sw * sh)
    for (i in px.indices) {
        val c = px[i]
        val dr = ((c shr 16) and 255) - mr; val dg = ((c shr 8) and 255) - mg; val db = (c and 255) - mb
        diff[i] = sqrt((dr * dr + dg * dg + db * db) / 3.0) > thr
    }
    // flood fill from the centre area over "different" pixels; pick the biggest component near the centre
    val label = IntArray(sw * sh); var best = 0; var bestSize = 0; var next = 0
    val stack = IntArray(sw * sh)
    val cx0 = sw / 4; val cx1 = sw * 3 / 4; val cy0 = sh / 6; val cy1 = sh * 5 / 6
    for (sy in cy0 until cy1) for (sx in cx0 until cx1) {
        val si = sy * sw + sx
        if (!diff[si] || label[si] != 0) continue
        next++; var sp = 0; stack[sp++] = si; label[si] = next; var size = 0
        while (sp > 0) {
            val i = stack[--sp]; size++
            val x = i % sw; val y = i / sw
            if (x > 0 && diff[i - 1] && label[i - 1] == 0) { label[i - 1] = next; stack[sp++] = i - 1 }
            if (x < sw - 1 && diff[i + 1] && label[i + 1] == 0) { label[i + 1] = next; stack[sp++] = i + 1 }
            if (y > 0 && diff[i - sw] && label[i - sw] == 0) { label[i - sw] = next; stack[sp++] = i - sw }
            if (y < sh - 1 && diff[i + sw] && label[i + sw] == 0) { label[i + sw] = next; stack[sp++] = i + sw }
        }
        if (size > bestSize) { bestSize = size; best = next }
    }
    if (best == 0 || bestSize < sw * sh * 0.01f) error("Tidak ada subjek yang terdeteksi. Gunakan kuas untuk memilih area secara manual.")
    var mask = FloatArray(sw * sh) { if (label[it] == best) 1f else 0f }
    // close holes: dilate then erode (2 px), then feather with a box blur
    fun dilate(src: FloatArray, r: Int): FloatArray {
        val o = FloatArray(src.size)
        for (y in 0 until sh) for (x in 0 until sw) {
            var m = 0f
            for (dy in -r..r) { val yy = y + dy; if (yy < 0 || yy >= sh) continue
                for (dx in -r..r) { val xx = x + dx; if (xx < 0 || xx >= sw) continue; if (src[yy * sw + xx] > m) m = src[yy * sw + xx] } }
            o[y * sw + x] = m
        }
        return o
    }
    fun erode(src: FloatArray, r: Int): FloatArray {
        val o = FloatArray(src.size)
        for (y in 0 until sh) for (x in 0 until sw) {
            var m = 1f
            for (dy in -r..r) { val yy = (y + dy).coerceIn(0, sh - 1)
                for (dx in -r..r) { val xx = (x + dx).coerceIn(0, sw - 1); if (src[yy * sw + xx] < m) m = src[yy * sw + xx] } }
            o[y * sw + x] = m
        }
        return o
    }
    mask = erode(dilate(mask, 2), 2)
    fun blur(src: FloatArray): FloatArray {
        val o = FloatArray(src.size)
        for (y in 0 until sh) for (x in 0 until sw) {
            var a = 0f; var c = 0
            for (dy in -1..1) for (dx in -1..1) { val xx = x + dx; val yy = y + dy; if (xx in 0 until sw && yy in 0 until sh) { a += src[yy * sw + xx]; c++ } }
            o[y * sw + x] = a / c
        }
        return o
    }
    mask = blur(blur(mask))

    val luma = FloatArray(px.size) { val c = px[it]; (0.299f * ((c shr 16) and 255) + 0.587f * ((c shr 8) and 255) + 0.114f * (c and 255)) / 255f }
    return applyAlpha(source, guidedFilter(luma, mask, sw, sh, 3, 0.003f), sw, sh, 0.3f, 0.7f)
}

// ---------------------------------------------------------------- mask refinement helpers

private fun lumaSmall(source: Bitmap, sw: Int, sh: Int): FloatArray {
    val sm = Bitmap.createScaledBitmap(source, sw, sh, true)
    val px = IntArray(sw * sh); sm.getPixels(px, 0, sw, 0, 0, sw, sh)
    if (sm !== source) sm.recycle()
    return FloatArray(px.size) { val c = px[it]; (0.299f * ((c shr 16) and 255) + 0.587f * ((c shr 8) and 255) + 0.114f * (c and 255)) / 255f }
}

/** mean over a (2r+1) square, edge-clamped; separable running sums, O(n) */
private fun boxMean(src: FloatArray, w: Int, h: Int, r: Int): FloatArray {
    val tmp = FloatArray(w * h); val out = FloatArray(w * h)
    for (y in 0 until h) {
        val base = y * w; var sum = 0f
        for (x in 0..min(r, w - 1)) sum += src[base + x]
        for (x in 0 until w) {
            tmp[base + x] = sum / (min(w - 1, x + r) - max(0, x - r) + 1)
            if (x + 1 + r < w) sum += src[base + x + 1 + r]
            if (x - r >= 0) sum -= src[base + x - r]
        }
    }
    for (x in 0 until w) {
        var sum = 0f
        for (y in 0..min(r, h - 1)) sum += tmp[y * w + x]
        for (y in 0 until h) {
            out[y * w + x] = sum / (min(h - 1, y + r) - max(0, y - r) + 1)
            if (y + 1 + r < h) sum += tmp[(y + 1 + r) * w + x]
            if (y - r >= 0) sum -= tmp[(y - r) * w + x]
        }
    }
    return out
}

/** Guided filter (He et al.): makes the soft mask `p` follow the edges of the photo `guide`, which is what cleans up hair and clothing outlines. */
private fun guidedFilter(guide: FloatArray, p: FloatArray, w: Int, h: Int, r: Int, eps: Float): FloatArray {
    val n = guide.size
    val mI = boxMean(guide, w, h, r); val mp = boxMean(p, w, h, r)
    val mIp = boxMean(FloatArray(n) { guide[it] * p[it] }, w, h, r)
    val mII = boxMean(FloatArray(n) { guide[it] * guide[it] }, w, h, r)
    val a = FloatArray(n); val b = FloatArray(n)
    for (i in 0 until n) {
        a[i] = (mIp[i] - mI[i] * mp[i]) / (mII[i] - mI[i] * mI[i] + eps)
        b[i] = mp[i] - a[i] * mI[i]
    }
    val ma = boxMean(a, w, h, r); val mb = boxMean(b, w, h, r)
    return FloatArray(n) { (ma[it] * guide[it] + mb[it]).coerceIn(0f, 1f) }
}

/** Writes a small alpha map onto the full-size photo (bilinear), with a narrow smoothstep so edges are crisp but not jagged. */
private fun applyAlpha(source: Bitmap, a: FloatArray, sw: Int, sh: Int, lo: Float, hi: Float): Bitmap {
    val w = source.width; val h = source.height
    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val row = IntArray(w)
    for (y in 0 until h) {
        source.getPixels(row, 0, w, 0, y, w, 1)
        val fy = y.toFloat() * (sh - 1) / (h - 1).coerceAtLeast(1); val y0 = fy.toInt().coerceIn(0, sh - 1); val y1 = min(y0 + 1, sh - 1); val ty = fy - y0
        for (x in 0 until w) {
            val fx = x.toFloat() * (sw - 1) / (w - 1).coerceAtLeast(1); val x0 = fx.toInt().coerceIn(0, sw - 1); val x1 = min(x0 + 1, sw - 1); val tx = fx - x0
            val p = (a[y0 * sw + x0] * (1 - tx) + a[y0 * sw + x1] * tx) * (1 - ty) + (a[y1 * sw + x0] * (1 - tx) + a[y1 * sw + x1] * tx) * ty
            val t = ((p - lo) / (hi - lo)).coerceIn(0f, 1f)
            row[x] = (row[x] and 0x00ffffff) or ((t * t * (3f - 2f * t) * 255f).toInt() shl 24)
        }
        out.setPixels(row, 0, w, 0, y, w, 1)
    }
    return out
}

/** Kept for compatibility with older callers: segments a stored image and writes a transparent PNG into app storage. */
suspend fun removePosterBackground(context: Context, source: Uri): Uri {
    val bitmap = context.contentResolver.openInputStream(source)?.use { android.graphics.BitmapFactory.decodeStream(it) }
        ?: error("Gambar tidak dapat dibuka")
    val out = segmentSubject(bitmap, context)
    val folder = File(context.filesDir, "poster-assets").apply { mkdirs() }
    val target = File(folder, "character-${System.currentTimeMillis()}.png")
    FileOutputStream(target).use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
    out.recycle(); bitmap.recycle()
    return Uri.fromFile(target)
}
