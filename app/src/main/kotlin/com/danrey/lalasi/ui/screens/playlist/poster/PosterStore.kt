package com.danrey.lalasi.ui.screens.playlist.poster

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/** Disk helpers: importing gallery images, drafts, and export. */
object PosterStore {
    private fun assetsDir(context: Context) = File(context.filesDir, "poster-assets").apply { mkdirs() }
    private fun draftFile(context: Context) = File(context.filesDir, "poster-draft-v2.json")

    /**
     * Copies a gallery image into app storage (EXIF-rotated, capped at maxDim) so the project keeps working after the picker permission expires.
     * Images with transparency stay PNG.
     */
    fun importImage(context: Context, uri: Uri, maxDim: Int = 2560): File? = runCatching {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxDim || bounds.outHeight / (sample * 2) >= maxDim) sample *= 2
        var bmp = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) } ?: return null
        val orientation = resolver.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) } ?: ExifInterface.ORIENTATION_NORMAL
        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
        }
        val big = max(bmp.width, bmp.height)
        if (big > maxDim) { val s = maxDim.toFloat() / big; m.postScale(s, s) }
        if (!m.isIdentity) bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        val png = bounds.outMimeType == "image/png" || bmp.hasAlpha() && bounds.outMimeType == "image/webp"
        val file = File(assetsDir(context), "img-${System.currentTimeMillis()}.${if (png) "png" else "jpg"}")
        FileOutputStream(file).use { bmp.compress(if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, 96, it) }
        file
    }.getOrNull()

    fun saveBitmapPng(context: Context, bmp: Bitmap, prefix: String = "cutout"): File {
        val file = File(assetsDir(context), "$prefix-${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }

    fun saveDraft(context: Context, doc: PosterDoc, playlistName: String) {
        runCatching { draftFile(context).writeText(JSONObject().put("playlist", playlistName).put("doc", doc.toJson()).toString()) }
    }

    fun loadDraft(context: Context): PosterDoc? = runCatching {
        val f = draftFile(context); if (!f.exists()) return null
        docFromJson(JSONObject(f.readText()).getJSONObject("doc"))
    }.getOrNull()

    fun hasDraft(context: Context) = draftFile(context).exists()

    /** Writes the bitmap to Pictures/LALASI (MediaStore on Android 10+, app folder before that). Returns the content/file uri string. */
    fun export(context: Context, bmp: Bitmap, png: Boolean, quality: Int = 95): Uri {
        val ext = if (png) "png" else "jpg"
        val name = "LALASI-Poster-${System.currentTimeMillis()}.$ext"
        val format = if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, if (png) "image/png" else "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/LALASI")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("Cannot create image file")
            context.contentResolver.openOutputStream(uri)?.use { bmp.compress(format, quality, it) } ?: error("Cannot write image")
            return uri
        }
        val dir = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES) ?: context.filesDir, "LALASI").apply { mkdirs() }
        val file = File(dir, name)
        FileOutputStream(file).use { bmp.compress(format, quality, it) }
        return Uri.fromFile(file)
    }
}
