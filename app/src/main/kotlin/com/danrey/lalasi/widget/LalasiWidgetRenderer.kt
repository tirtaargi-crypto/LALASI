// LalasiWidgetRenderer.kt

package com.danrey.lalasi.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.media3.common.Player
import com.danrey.lalasi.R
import java.util.Objects
import java.util.concurrent.ConcurrentHashMap

/** Everything a widget needs to draw one moment of playback. */
data class WidgetSnapshot(
    val title: String,
    val artist: String,
    val artworkUri: String?,
    val isPlaying: Boolean,
    val isLiked: Boolean,
    val durationMs: Long,
    val positionMs: Long,
    val shuffleEnabled: Boolean,
    val repeatMode: Int,
    val lyrics: LyricsFrame?,
)

/**
 * Draws the LALASI widget collection with one shared design system: the same glass renderer, artwork treatment,
 * typography and control look for every variant. A widget is only pushed to the launcher when something it shows
 * actually changed, which keeps updates light and avoids flicker.
 */
class LalasiWidgetRenderer(private val context: Context) {

    private val glassCache = LinkedHashMap<String, Bitmap>()
    private val artCache = LinkedHashMap<String, Bitmap>()

    /** Draws one placed widget; skips the update when nothing visible changed since the last one. */
    fun update(
        appWidgetManager: AppWidgetManager,
        variant: LalasiWidgetVariant,
        widgetId: Int,
        snapshot: WidgetSnapshot,
        albumArt: Bitmap?,
    ) {
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val wDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).takeIf { it > 0 } ?: variant.defaultWidthDp
        val hDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT).takeIf { it > 0 } ?: variant.defaultHeightDp
        val style = WidgetStyle.load(context)

        val seconds = if (variant.hasProgress) snapshot.positionMs / 1000L else 0L
        val lyrics = if (variant.hasLyrics) snapshot.lyrics else null
        val signature = Objects.hash(
            snapshot.title, snapshot.artist, snapshot.isPlaying, snapshot.isLiked, snapshot.shuffleEnabled,
            snapshot.repeatMode, System.identityHashCode(albumArt), seconds, snapshot.durationMs / 1000L,
            lyrics?.previous, lyrics?.current, lyrics?.next, lyrics?.mode,
            wDp / 4, hDp / 4, style.liquidColor, style.tint, style.blur, style.look, style.shine, style.thickness, style.coverShape, style.swirl, variant.ordinal,
        )
        if (lastDrawn[widgetId] == signature) return

        appWidgetManager.updateAppWidget(widgetId, build(variant, snapshot, albumArt, wDp, hDp, style))
        lastDrawn[widgetId] = signature
    }

    private fun build(
        variant: LalasiWidgetVariant,
        snap: WidgetSnapshot,
        art: Bitmap?,
        wDp: Int,
        hDp: Int,
        style: WidgetStyle.Config,
    ): RemoteViews {
        val v = RemoteViews(context.packageName, variant.layout)
        val slots = variant.slots

        v.setImageViewBitmap(R.id.lw_glass_bg, glass(art, wDp, hDp, style, scrimFor(variant)))
        WidgetIntents.bind(context, v, variant)

        // responsive: scale type with the real size, and drop secondary pieces in an orderly way when space is short
        val scale = minOf(wDp / variant.defaultWidthDp.toFloat(), hDp / variant.defaultHeightDp.toFloat()).coerceIn(0.88f, 1.25f)
        val narrow = wDp < 200
        val short = hDp < 100

        if (Slot.ART in slots) {
            val corner = when (style.coverShape) {
                WidgetStyle.CoverShape.SQUARE -> 0.03f
                WidgetStyle.CoverShape.CIRCLE -> 0.5f
                else -> variant.artCorner
            }
            v.setImageViewBitmap(R.id.lw_art, roundedArt(art, corner))
        }
        if (Slot.INFO in slots) {
            v.setTextViewText(R.id.lw_title, snap.title)
            v.setTextViewText(R.id.lw_artist, snap.artist)
            v.setTextViewTextSize(R.id.lw_title, TypedValue.COMPLEX_UNIT_SP, variant.titleSp * scale)
            v.setTextViewTextSize(R.id.lw_artist, TypedValue.COMPLEX_UNIT_SP, variant.artistSp * scale)
        }
        if (Slot.PLAY in slots) {
            v.setImageViewResource(R.id.lw_play, if (snap.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val size = (variant.playDp * scale).coerceIn(30f, 58f)
                v.setViewLayoutWidth(R.id.lw_play, size, TypedValue.COMPLEX_UNIT_DIP)
                v.setViewLayoutHeight(R.id.lw_play, size, TypedValue.COMPLEX_UNIT_DIP)
            }
        }
        if (Slot.LIKE in slots) {
            v.setImageViewResource(
                R.id.lw_like,
                if (snap.isLiked) R.drawable.ic_widget_heart_nav else R.drawable.ic_widget_heart_outline_nav,
            )
        }
        if (Slot.SHUFFLE in slots) {
            v.setImageViewResource(R.id.lw_shuffle, if (snap.shuffleEnabled) R.drawable.ic_widget_shuffle_on else R.drawable.ic_widget_shuffle)
            v.setViewVisibility(R.id.lw_shuffle, if (narrow) View.GONE else View.VISIBLE)
        }
        if (Slot.REPEAT in slots) {
            v.setImageViewResource(
                R.id.lw_repeat,
                when (snap.repeatMode) {
                    Player.REPEAT_MODE_ONE -> R.drawable.ic_widget_repeat_one
                    Player.REPEAT_MODE_ALL -> R.drawable.ic_widget_repeat_on
                    else -> R.drawable.ic_widget_repeat
                },
            )
            v.setViewVisibility(R.id.lw_repeat, if (narrow) View.GONE else View.VISIBLE)
        }
        if (Slot.PROGRESS in slots) {
            val permille = if (snap.durationMs > 0) ((snap.positionMs * 1000L) / snap.durationMs).toInt().coerceIn(0, 1000) else 0
            v.setProgressBar(R.id.lw_progress, 1000, permille, false)
            v.setViewVisibility(R.id.lw_progress, if (short) View.GONE else View.VISIBLE)
        }
        if (Slot.TIMES in slots) {
            v.setTextViewText(R.id.lw_time_cur, formatTime(snap.positionMs))
            v.setTextViewText(R.id.lw_time_rem, "-" + formatTime((snap.durationMs - snap.positionMs).coerceAtLeast(0L)))
            v.setViewVisibility(R.id.lw_time_cur, if (short) View.GONE else View.VISIBLE)
            v.setViewVisibility(R.id.lw_time_rem, if (short) View.GONE else View.VISIBLE)
        }
        if (variant.hasLyrics) bindLyrics(v, variant, snap.lyrics, scale, hDp, style.isLight)
        // the Artwork widget keeps its own dark bottom scrim, so its white text stays white
        if (style.isLight && variant != LalasiWidgetVariant.ARTWORK) paintForLightGlass(v, slots, snap)
        return v
    }

    /** The light look has a pale pane, so the white text and white icons switch to dark ink. */
    private fun paintForLightGlass(v: RemoteViews, slots: Set<Slot>, snap: WidgetSnapshot) {
        val ink = 0xFF14161C.toInt()
        val soft = 0x99141C1C.toInt()
        if (Slot.INFO in slots) {
            v.setTextColor(R.id.lw_title, ink)
            v.setTextColor(R.id.lw_artist, soft)
        }
        if (Slot.TIMES in slots) {
            v.setTextColor(R.id.lw_time_cur, soft)
            v.setTextColor(R.id.lw_time_rem, soft)
        }
        if (Slot.PLAY in slots) v.setInt(R.id.lw_play, "setColorFilter", ink)
        if (Slot.PREV in slots) v.setInt(R.id.lw_prev, "setColorFilter", ink)
        if (Slot.NEXT in slots) v.setInt(R.id.lw_next, "setColorFilter", ink)
        if (Slot.LIKE in slots && !snap.isLiked) v.setInt(R.id.lw_like, "setColorFilter", ink)
        if (Slot.SHUFFLE in slots && !snap.shuffleEnabled) v.setInt(R.id.lw_shuffle, "setColorFilter", ink)
        if (Slot.REPEAT in slots && snap.repeatMode == Player.REPEAT_MODE_OFF) v.setInt(R.id.lw_repeat, "setColorFilter", ink)
    }

    private fun bindLyrics(v: RemoteViews, variant: LalasiWidgetVariant, frame: LyricsFrame?, scale: Float, hDp: Int, light: Boolean = false) {
        val slots = variant.slots
        val mode = frame?.mode ?: LyricsFrame.Mode.EMPTY
        val dim = if (light) 0x80141C1C.toInt() else 0x80FFFFFF.toInt()
        val bright = if (light) 0xFF14161C.toInt() else 0xFFFFFFFF.toInt()
        val plain = if (light) 0xE614161C.toInt() else 0xE6FFFFFF.toInt()
        val hideSides = hDp < 90

        val previous: String
        val current: String
        val next: String
        when (mode) {
            LyricsFrame.Mode.EMPTY -> {
                previous = ""
                current = context.getString(R.string.widget_lyrics_empty)
                next = ""
            }
            else -> {
                previous = frame?.previous.orEmpty()
                current = frame?.current.orEmpty()
                next = frame?.next.orEmpty()
            }
        }
        // plain (unsynced) lyrics: every line is shown evenly, nothing is highlighted as "current"
        val sideColor = if (mode == LyricsFrame.Mode.PLAIN) plain else dim
        val currentColor = if (mode == LyricsFrame.Mode.PLAIN) plain else bright

        v.setTextViewText(R.id.lw_lyric_cur, current)
        v.setTextColor(R.id.lw_lyric_cur, if (mode == LyricsFrame.Mode.EMPTY) dim else currentColor)
        v.setTextViewTextSize(R.id.lw_lyric_cur, TypedValue.COMPLEX_UNIT_SP, variant.lyricSp * scale)
        if (Slot.LYRIC_PREV in slots) {
            v.setTextViewText(R.id.lw_lyric_prev, previous)
            v.setTextColor(R.id.lw_lyric_prev, sideColor)
            v.setTextViewTextSize(R.id.lw_lyric_prev, TypedValue.COMPLEX_UNIT_SP, variant.lyricSideSp * scale)
            v.setViewVisibility(R.id.lw_lyric_prev, if (hideSides || previous.isEmpty()) View.GONE else View.VISIBLE)
        }
        if (Slot.LYRIC_NEXT in slots) {
            v.setTextViewText(R.id.lw_lyric_next, next)
            v.setTextColor(R.id.lw_lyric_next, sideColor)
            v.setTextViewTextSize(R.id.lw_lyric_next, TypedValue.COMPLEX_UNIT_SP, variant.lyricSideSp * scale)
            v.setViewVisibility(R.id.lw_lyric_next, if (hideSides || next.isEmpty()) View.GONE else View.VISIBLE)
        }
    }

    /** Text sits straight on the cover in every variant except Artwork, which has its own bottom scrim layer. */
    private fun scrimFor(variant: LalasiWidgetVariant): Int = when {
        variant == LalasiWidgetVariant.ARTWORK -> 0
        variant.hasLyrics -> 150
        else -> 110
    }

    /**
     * One look for the whole collection, the Artwork widget's: the sharp cover (never blurred), the same colour
     * treatment, highlight and glass edge. Only the legibility [scrim] differs per variant. The blur setting no
     * longer applies to widgets, so every widget shows the same colours for the same song.
     */
    private fun glass(art: Bitmap?, wDp: Int, hDp: Int, style: WidgetStyle.Config, scrim: Int): Bitmap {
        var scale = context.resources.displayMetrics.density * 1.0f
        // HD, but a widget bitmap travels through the launcher's IPC: keep it under ~1.6 MP
        val px = (wDp * scale) * (hDp * scale)
        if (px > 1_600_000f) scale *= kotlin.math.sqrt(1_600_000f / px)
        val w = (wDp * scale).toInt().coerceAtLeast(32)
        val h = (hDp * scale).toInt().coerceAtLeast(32)
        // the frost slider applies here now (it used to be forced to 0); the dark legibility scrim only
        // belongs on looks that put the cover under the text, never on the light pane
        val useScrim = if (style.isLight) 0 else scrim
        val key = "${System.identityHashCode(art)}|$w|$h|${style.liquidColor}|${style.tint}|${style.blur}|${style.look}|${style.shine}|${style.thickness}|${style.swirl}|$useScrim"
        return glassCache.getOrPut(key) {
            if (glassCache.size >= 8) glassCache.remove(glassCache.keys.first())
            WidgetStyle.renderGlass(art, w, h, 30f * scale, style, 1.4f * scale, useScrim)
        }
    }

    /** Square, centre-cropped, rounded artwork; cached per artwork and corner so it is never re-decoded per update. */
    private fun roundedArt(art: Bitmap?, corner: Float): Bitmap {
        val key = "${System.identityHashCode(art)}|$corner"
        artCache[key]?.let { return it }
        val source = art ?: defaultArt()
        val side = minOf(source.width, source.height)
        val out = Bitmap.createBitmap(minOf(side, MaxArtPx), minOf(side, MaxArtPx), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            val k = out.width / side.toFloat()
            setLocalMatrix(android.graphics.Matrix().apply {
                postTranslate(-(source.width - side) / 2f, -(source.height - side) / 2f)
                postScale(k, k)
            })
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { this.shader = shader }
        val r = out.width * corner
        canvas.drawRoundRect(RectF(0f, 0f, out.width.toFloat(), out.height.toFloat()), r, r, paint)
        if (artCache.size >= 6) artCache.remove(artCache.keys.first())
        artCache[key] = out
        return out
    }

    private var cachedDefaultArt: Bitmap? = null

    private fun defaultArt(): Bitmap = cachedDefaultArt ?: run {
        val drawable = context.packageManager.getApplicationIcon(context.packageName)
        val size = 256
        Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also {
            drawable.setBounds(0, 0, size, size)
            drawable.draw(Canvas(it))
            cachedDefaultArt = it
        }
    }

    private fun formatTime(ms: Long): String {
        val total = (ms / 1000L).coerceAtLeast(0L)
        return "%d:%02d".format(total / 60L, total % 60L)
    }

    companion object {
        private const val MaxArtPx = 512

        /** widget id -> signature of what is currently on that widget */
        private val lastDrawn = ConcurrentHashMap<Int, Int>()

        /** Forces the next update to really redraw these widgets (new widget, resize, launcher asked for an update). */
        fun invalidate(ids: IntArray) {
            ids.forEach { lastDrawn.remove(it) }
        }

        fun forget(ids: IntArray) = invalidate(ids)

        /** Working layout shown before anything plays: wired controls and the neutral "tap to open" text. */
        fun idleViews(context: Context, variant: LalasiWidgetVariant): RemoteViews {
            val views = RemoteViews(context.packageName, variant.layout)
            WidgetIntents.bind(context, views, variant)
            return views
        }
    }
}
