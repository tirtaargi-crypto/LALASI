// MetrolistWidgetManager.kt

package com.danrey.lalasi.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.Bundle
import android.widget.RemoteViews
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import com.danrey.lalasi.MainActivity
import com.danrey.lalasi.R
import com.danrey.lalasi.db.MusicDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LalasiWidgetManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase
) {
    private val imageLoader by lazy {
        ImageLoader.Builder(context)
            .crossfade(false)
            .build()
    }

    private var cachedArtworkUri: String? = null
    private var cachedAlbumArt: Bitmap? = null
    private var cachedCircularAlbumArt: Bitmap? = null

    /** One update at a time: they share the cached artwork, and they no longer run on one thread. */
    private val updating = kotlinx.coroutines.sync.Mutex()

    /**
     * Redraws every placed widget. Runs off the main thread: the system calls and bitmap work here
     * used to run on it every 200ms while music played, taking frames from whatever was on screen.
     * Does nothing at all, not even load the artwork, when no widget is placed.
     */
    suspend fun updateWidgets(
        title: String,
        artist: String,
        artworkUri: String?,
        isPlaying: Boolean,
        isLiked: Boolean,
        duration: Long = 0,
        currentPosition: Long = 0,
        songId: String? = null,
        shuffleEnabled: Boolean = false,
        repeatMode: Int = 0,
        lyricsOffsetMs: Int = 0,
    ) = withContext(Dispatchers.Default) {
        updating.withLock {
            drawWidgets(
                title, artist, artworkUri, isPlaying, isLiked, duration, currentPosition,
                songId, shuffleEnabled, repeatMode, lyricsOffsetMs,
            )
        }
    }

    private val lalasiRenderer by lazy { LalasiWidgetRenderer(context) }
    private val lyricsSource by lazy { WidgetLyricsSource(database) }

    /**
     * How long the playback loop may wait before the next redraw so a lyric line changes on time.
     * Long.MAX_VALUE when no live-lyrics widget needs it; the loop keeps its normal one-second pace then.
     */
    @Volatile
    var refreshHintMs: Long = Long.MAX_VALUE
        private set

    private suspend fun drawWidgets(
        title: String,
        artist: String,
        artworkUri: String?,
        isPlaying: Boolean,
        isLiked: Boolean,
        duration: Long,
        currentPosition: Long,
        songId: String?,
        shuffleEnabled: Boolean,
        repeatMode: Int,
        lyricsOffsetMs: Int,
    ) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, MusicWidgetReceiver::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
        val turntableComponentName = ComponentName(context, TurntableWidgetReceiver::class.java)
        val turntableWidgetIds = appWidgetManager.getAppWidgetIds(turntableComponentName)
        // the LALASI widget collection: only variants that are really placed on a home screen
        val lalasiPlaced = LalasiWidgetVariant.values()
            .map { it to appWidgetManager.getAppWidgetIds(ComponentName(context, it.receiver)) }
            .filter { it.second.isNotEmpty() }
        if (widgetIds.isEmpty() && turntableWidgetIds.isEmpty() && lalasiPlaced.isEmpty()) {
            refreshHintMs = Long.MAX_VALUE
            return
        }

        val albumArt: Bitmap?
        val circularAlbumArt: Bitmap?

        if (artworkUri != null && artworkUri == cachedArtworkUri && cachedAlbumArt != null) {
            albumArt = cachedAlbumArt
            circularAlbumArt = cachedCircularAlbumArt
        } else {
            albumArt = artworkUri?.let { loadAlbumArt(it, 640) }
            circularAlbumArt = albumArt?.let { getCircularBitmap(it) }

            cachedArtworkUri = artworkUri
            cachedAlbumArt = albumArt
            cachedCircularAlbumArt = circularAlbumArt
        }

        if (widgetIds.isNotEmpty()) {
            widgetIds.forEach { widgetId ->
                val options = appWidgetManager.getAppWidgetOptions(widgetId)
                val views = createRemoteViewsForSize(
                    options,
                    title,
                    artist,
                    albumArt,
                    isPlaying,
                    isLiked,
                    duration,
                    currentPosition
                )
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }

        if (turntableWidgetIds.isNotEmpty()) {
            val turntableViews = createTurntableRemoteViews(
                circularAlbumArt,
                isPlaying,
                isLiked
            )
            turntableWidgetIds.forEach { widgetId ->
                appWidgetManager.updateAppWidget(widgetId, turntableViews)
            }
        }

        if (lalasiPlaced.isNotEmpty()) {
            // lyrics are only read when a lyric widget is placed
            val lyricsFrame = if (lalasiPlaced.any { it.first.hasLyrics }) {
                lyricsSource.frame(songId, currentPosition, lyricsOffsetMs)
            } else null
            val snapshot = WidgetSnapshot(
                title = title,
                artist = artist,
                artworkUri = artworkUri,
                isPlaying = isPlaying,
                isLiked = isLiked,
                durationMs = duration,
                positionMs = currentPosition,
                shuffleEnabled = shuffleEnabled,
                repeatMode = repeatMode,
                lyrics = lyricsFrame,
            )
            lalasiPlaced.forEach { (variant, ids) ->
                ids.forEach { id -> lalasiRenderer.update(appWidgetManager, variant, id, snapshot, albumArt) }
            }
            // the next lyric change, so the service wakes at the right moment instead of polling faster
            refreshHintMs = lyricsFrame?.changeInMs ?: Long.MAX_VALUE
        } else {
            refreshHintMs = Long.MAX_VALUE
        }
    }

    private fun createRemoteViewsForSize(
        options: Bundle,
        title: String,
        artist: String,
        albumArt: Bitmap?,
        isPlaying: Boolean,
        isLiked: Boolean,
        duration: Long,
        currentPosition: Long
    ): RemoteViews {
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)

        // what the widget is really given, for drawing its glass at the right shape
        val wDp = if (minWidth > 0) minWidth else 250
        val hDp = if (minHeight > 0) minHeight else 110

        return when {
            minWidth < 180 -> {

                createCompactSquareRemoteViews(title, artist, albumArt, isPlaying).also {
                    applyGlass(it, albumArt, wDp, hDp)
                }
            }
            minWidth >= 180 && minHeight < 100 -> {

                createCompactWideRemoteViews(title, artist, albumArt, isPlaying, isLiked).also {
                    applyGlass(it, albumArt, wDp, hDp)
                }
            }
            else -> {

                createRemoteViews(title, artist, albumArt, isPlaying, isLiked, duration, currentPosition).also {
                    applyGlass(it, albumArt, wDp, hDp)
                }
            }
        }
    }

    /** Latest few rendered glasses: the widgets redraw often and the blur only changes with art, size or style. */
    private val glassCache = LinkedHashMap<String, Bitmap>()

    private fun applyGlass(views: RemoteViews, albumArt: Bitmap?, wDp: Int, hDp: Int) {
        val config = WidgetStyle.load(context)
        val density = context.resources.displayMetrics.density
        var scale = density
        val px = (wDp * scale) * (hDp * scale)
        if (px > 1_600_000f) scale *= kotlin.math.sqrt(1_600_000f / px)
        val w = (wDp * scale).toInt().coerceAtLeast(32)
        val h = (hDp * scale).toInt().coerceAtLeast(32)
        val key = "${System.identityHashCode(albumArt)}|$w|$h|${config.liquidColor}|${config.blur}|${config.tint}|${config.look}|${config.shine}|${config.thickness}|${config.swirl}"
        val glass = glassCache.getOrPut(key) {
            if (glassCache.size >= 6) glassCache.remove(glassCache.keys.first())
            WidgetStyle.renderGlass(albumArt, w, h, 30f * scale, config, 1.4f * scale)
        }
        views.setImageViewBitmap(R.id.widget_glass_bg, glass)
        if (config.isLight) {
            views.setTextColor(R.id.widget_song_title, 0xFF14161C.toInt())
            views.setTextColor(R.id.widget_artist_name, 0x99141C1C.toInt())
        }
    }

    private fun createRemoteViews(
        title: String,
        artist: String,
        albumArt: Bitmap?,
        isPlaying: Boolean,
        isLiked: Boolean,
        duration: Long = 0,
        currentPosition: Long = 0
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_music_player)

        views.setTextViewText(R.id.widget_song_title, title)
        views.setTextViewText(R.id.widget_artist_name, artist)

        if (albumArt != null) {
            val roundedAlbumArt = getRoundedCornerBitmap(albumArt, 0.22f)
            views.setImageViewBitmap(R.id.widget_album_art, roundedAlbumArt)
        } else {
            views.setImageViewBitmap(R.id.widget_album_art, getRoundedDefaultIcon(0.22f))
        }

        val playPauseIcon = if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
        views.setImageViewResource(R.id.widget_play_pause, playPauseIcon)

        val likeIcon = if (isLiked) R.drawable.ic_widget_heart_nav else R.drawable.ic_widget_heart_outline_nav
        views.setImageViewResource(R.id.widget_like_button, likeIcon)

        if (duration > 0) {
            val level = ((currentPosition.toDouble() / duration.toDouble()) * 10000).toInt()
            views.setInt(R.id.widget_progress_fill, "setImageLevel", level)
        } else {
            views.setInt(R.id.widget_progress_fill, "setImageLevel", 0)
        }

        views.setTextViewText(R.id.widget_time_current, formatWidgetTime(currentPosition))
        views.setTextViewText(
            R.id.widget_time_remaining,
            "-" + formatWidgetTime((duration - currentPosition).coerceAtLeast(0L))
        )

        views.setOnClickPendingIntent(R.id.widget_album_art, getOpenAppIntent())
        views.setOnClickPendingIntent(R.id.widget_play_pause_container, getPlayPauseIntent())
        views.setOnClickPendingIntent(R.id.widget_like_button, getLikeIntent())
        views.setOnClickPendingIntent(R.id.widget_prev_button, getPreviousIntent())
        views.setOnClickPendingIntent(R.id.widget_next_button, getNextIntent())

        return views
    }

    private suspend fun loadAlbumArt(artworkUri: String, size: Int = 200): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(artworkUri)
                    .size(size, size)
                    .allowHardware(false)
                    .crossfade(300)
                    .build()
                val result = imageLoader.execute(request)
                result.image?.toBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun getRoundedCornerBitmap(bitmap: Bitmap, cornerRadius: Float): Bitmap {

        val fullSize = minOf(bitmap.width, bitmap.height)
        val xOffset = (bitmap.width - fullSize) / 2
        val yOffset = (bitmap.height - fullSize) / 2
        val cropped = Bitmap.createBitmap(bitmap, xOffset, yOffset, fullSize, fullSize)
        // a widget never needs more than this; smaller bitmaps keep the update light
        val size = minOf(fullSize, 640)
        val squareBitmap = if (size < fullSize) Bitmap.createScaledBitmap(cropped, size, size, true) else cropped
        if (cropped !== squareBitmap && cropped !== bitmap) cropped.recycle()
        // 1 or less means a share of the side, so the corners look the same at any size
        val radius = if (cornerRadius <= 1f) size * cornerRadius else cornerRadius

        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            shader = BitmapShader(squareBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawRoundRect(rect, radius, radius, paint)

        if (squareBitmap != bitmap) {
            squareBitmap.recycle()
        }

        return output
    }

    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)

        val xOffset = (bitmap.width - size) / 2
        val yOffset = (bitmap.height - size) / 2
        val squareBitmap = Bitmap.createBitmap(bitmap, xOffset, yOffset, size, size)

        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            shader = BitmapShader(squareBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, paint)

        if (squareBitmap != bitmap) {
            squareBitmap.recycle()
        }
        return output
    }

    private fun createCompactSquareRemoteViews(
        title: String,
        artist: String,
        albumArt: Bitmap?,
        isPlaying: Boolean
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_compact_square)

        views.setTextViewText(R.id.widget_compact_song_title, title)
        views.setTextViewText(R.id.widget_compact_artist_name, artist)

        if (albumArt != null) {
            val roundedAlbumArt = getRoundedCornerBitmap(albumArt, 0.22f)
            views.setImageViewBitmap(R.id.widget_compact_album_art, roundedAlbumArt)
        } else {
            views.setImageViewBitmap(R.id.widget_compact_album_art, getRoundedDefaultIcon(0.22f))
        }

        val playPauseIcon = if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
        views.setImageViewResource(R.id.widget_compact_play_pause, playPauseIcon)

        views.setOnClickPendingIntent(R.id.widget_compact_album_art, getOpenAppIntent())
        views.setOnClickPendingIntent(R.id.widget_compact_play_container, getPlayPauseIntent())
        views.setOnClickPendingIntent(R.id.widget_compact_prev, getPreviousIntent())
        views.setOnClickPendingIntent(R.id.widget_compact_next, getNextIntent())

        return views
    }

    private fun createCompactWideRemoteViews(
        title: String,
        artist: String,
        albumArt: Bitmap?,
        isPlaying: Boolean,
        isLiked: Boolean
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_compact_wide)

        views.setTextViewText(R.id.widget_wide_song_title, title)
        views.setTextViewText(R.id.widget_wide_artist_name, artist)

        if (albumArt != null) {
            val roundedAlbumArt = getRoundedCornerBitmap(albumArt, 0.22f)
            views.setImageViewBitmap(R.id.widget_wide_album_art, roundedAlbumArt)
        } else {

            views.setImageViewBitmap(R.id.widget_wide_album_art, getRoundedDefaultIcon(0.22f))
        }

        val playPauseIcon = if (isPlaying) R.drawable.ic_widget_pause_low else R.drawable.ic_widget_play_low
        views.setImageViewResource(R.id.widget_wide_play_pause, playPauseIcon)

        val likeIcon = if (isLiked) R.drawable.ic_widget_heart_nav else R.drawable.ic_widget_heart_outline_nav
        views.setImageViewResource(R.id.widget_wide_like_button, likeIcon)

        views.setOnClickPendingIntent(R.id.widget_wide_album_art, getOpenAppIntent())
        views.setOnClickPendingIntent(R.id.widget_wide_play_container, getPlayPauseIntent())
        views.setOnClickPendingIntent(R.id.widget_wide_like_button, getLikeIntent())

        return views
    }

    private fun createTurntableRemoteViews(
        circularAlbumArt: Bitmap?,
        isPlaying: Boolean,
        isLiked: Boolean
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_turntable)

        if (circularAlbumArt != null) {
            views.setImageViewBitmap(R.id.widget_turntable_album_art, circularAlbumArt)
        } else {

            views.setImageViewBitmap(R.id.widget_turntable_album_art, getCircularDefaultIcon())
        }

        val playPauseIcon = if (isPlaying) R.drawable.ic_widget_pause_secondary else R.drawable.ic_widget_play_secondary
        views.setImageViewResource(R.id.widget_turntable_play_pause, playPauseIcon)

        views.setOnClickPendingIntent(R.id.widget_turntable_album_art, getOpenAppIntent())
        views.setOnClickPendingIntent(R.id.widget_turntable_play_container, getTurntablePlayPauseIntent())
        views.setOnClickPendingIntent(R.id.widget_turntable_prev_button, getTurntablePreviousIntent())
        views.setOnClickPendingIntent(R.id.widget_turntable_next_button, getTurntableNextIntent())

        return views
    }

    private fun getCircularDefaultIcon(): Bitmap {

        val drawable = context.packageManager.getApplicationIcon(context.packageName)
        val size = 300
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return getCircularBitmap(bitmap)
    }

    private fun getRoundedDefaultIcon(cornerRadius: Float): Bitmap {

        val drawable = context.packageManager.getApplicationIcon(context.packageName)
        val size = 300
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return getRoundedCornerBitmap(bitmap, cornerRadius)
    }

    private fun getOpenAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getPlayPauseIntent(): PendingIntent {
        val intent = Intent(context, MusicWidgetReceiver::class.java).apply {
            action = MusicWidgetReceiver.ACTION_PLAY_PAUSE
        }
        return PendingIntent.getBroadcast(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getLikeIntent(): PendingIntent {
        val intent = Intent(context, MusicWidgetReceiver::class.java).apply {
            action = MusicWidgetReceiver.ACTION_LIKE
        }
        return PendingIntent.getBroadcast(
            context,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getTurntablePlayPauseIntent(): PendingIntent {
        val intent = Intent(context, TurntableWidgetReceiver::class.java).apply {
            action = TurntableWidgetReceiver.ACTION_TURNTABLE_PLAY_PAUSE
        }
        return PendingIntent.getBroadcast(
            context,
            3,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getTurntableNextIntent(): PendingIntent {
        val intent = Intent(context, TurntableWidgetReceiver::class.java).apply {
            action = TurntableWidgetReceiver.ACTION_TURNTABLE_NEXT
        }
        return PendingIntent.getBroadcast(
            context,
            4,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getTurntablePreviousIntent(): PendingIntent {
        val intent = Intent(context, TurntableWidgetReceiver::class.java).apply {
            action = TurntableWidgetReceiver.ACTION_TURNTABLE_PREVIOUS
        }
        return PendingIntent.getBroadcast(
            context,
            5,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun formatWidgetTime(ms: Long): String {
        val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
        return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
    }

    private fun getNextIntent(): PendingIntent {
        val intent = Intent(context, MusicWidgetReceiver::class.java).apply {
            action = MusicWidgetReceiver.ACTION_NEXT
        }
        return PendingIntent.getBroadcast(
            context,
            6,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getPreviousIntent(): PendingIntent {
        val intent = Intent(context, MusicWidgetReceiver::class.java).apply {
            action = MusicWidgetReceiver.ACTION_PREVIOUS
        }
        return PendingIntent.getBroadcast(
            context,
            7,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
