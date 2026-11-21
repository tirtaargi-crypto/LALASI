// LalasiWidgetVariants.kt

package com.danrey.lalasi.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import com.danrey.lalasi.MainActivity
import com.danrey.lalasi.R
import com.danrey.lalasi.playback.MusicService

/** The pieces a widget layout can contain. A variant only ever touches the slots it really has. */
enum class Slot { ART, INFO, PLAY, PREV, NEXT, PROGRESS, TIMES, SHUFFLE, REPEAT, LIKE, LYRIC_PREV, LYRIC_CUR, LYRIC_NEXT }

/**
 * One real home-screen widget per entry: its own provider (see AndroidManifest), layout, picker preview and size.
 * Typography values are the sp sizes written in the layout; the renderer scales them with the real widget size.
 */
enum class LalasiWidgetVariant(
    val receiver: Class<out AppWidgetProvider>,
    @LayoutRes val layout: Int,
    val slots: Set<Slot>,
    val defaultWidthDp: Int,
    val defaultHeightDp: Int,
    val titleSp: Float,
    val artistSp: Float,
    val lyricSp: Float,
    val lyricSideSp: Float,
    val artCorner: Float,
    val playDp: Int,
) {
    COMPACT(
        LalasiCompactWidgetReceiver::class.java, R.layout.widget_lw_compact,
        setOf(Slot.ART, Slot.INFO, Slot.PLAY, Slot.NEXT), 180, 70, 13f, 11f, 0f, 0f, 0.22f, 34,
    ),
    SQUARE(
        LalasiSquareWidgetReceiver::class.java, R.layout.widget_lw_square,
        setOf(Slot.ART, Slot.INFO, Slot.PROGRESS, Slot.PREV, Slot.PLAY, Slot.NEXT), 110, 110, 13f, 11f, 0f, 0f, 0.22f, 38,
    ),
    WIDE(
        LalasiWideWidgetReceiver::class.java, R.layout.widget_lw_wide,
        setOf(Slot.ART, Slot.INFO, Slot.PREV, Slot.PLAY, Slot.NEXT), 250, 70, 14f, 12f, 0f, 0f, 0.22f, 40,
    ),
    EXPANDED(
        LalasiExpandedWidgetReceiver::class.java, R.layout.widget_lw_expanded,
        setOf(Slot.ART, Slot.INFO, Slot.PROGRESS, Slot.TIMES, Slot.PREV, Slot.PLAY, Slot.NEXT, Slot.SHUFFLE, Slot.REPEAT, Slot.LIKE),
        250, 110, 15f, 12f, 0f, 0f, 0.18f, 40,
    ),
    LARGE(
        LalasiLargeSquareWidgetReceiver::class.java, R.layout.widget_lw_large,
        setOf(Slot.ART, Slot.INFO, Slot.PROGRESS, Slot.TIMES, Slot.PREV, Slot.PLAY, Slot.NEXT, Slot.SHUFFLE, Slot.REPEAT, Slot.LIKE),
        250, 250, 17f, 13f, 0f, 0f, 0.12f, 46,
    ),
    LYRICS(
        LalasiLyricsWidgetReceiver::class.java, R.layout.widget_lw_lyrics,
        setOf(Slot.ART, Slot.INFO, Slot.PREV, Slot.PLAY, Slot.NEXT, Slot.LYRIC_PREV, Slot.LYRIC_CUR, Slot.LYRIC_NEXT),
        250, 180, 12f, 11f, 21f, 13f, 0.22f, 38,
    ),
    COMPACT_LYRICS(
        LalasiCompactLyricsWidgetReceiver::class.java, R.layout.widget_lw_clyrics,
        setOf(Slot.INFO, Slot.PLAY, Slot.LYRIC_CUR, Slot.LYRIC_NEXT), 180, 110, 11f, 10f, 16f, 11f, 0f, 30,
    ),
    WIDE_LYRICS(
        LalasiWideLyricsWidgetReceiver::class.java, R.layout.widget_lw_wlyrics,
        setOf(Slot.ART, Slot.PLAY, Slot.NEXT, Slot.LYRIC_PREV, Slot.LYRIC_CUR, Slot.LYRIC_NEXT), 250, 110, 0f, 0f, 16f, 11f, 0.18f, 38,
    ),
    MINIMAL(
        LalasiMinimalWidgetReceiver::class.java, R.layout.widget_lw_minimal,
        setOf(Slot.INFO, Slot.PLAY, Slot.NEXT), 180, 70, 14f, 12f, 0f, 0f, 0f, 36,
    ),
    ARTWORK(
        LalasiArtworkWidgetReceiver::class.java, R.layout.widget_lw_artwork,
        setOf(Slot.ART, Slot.INFO, Slot.PLAY), 110, 110, 13f, 11f, 0f, 0f, 0.2f, 36,
    );

    val hasLyrics: Boolean get() = Slot.LYRIC_CUR in slots
    val hasProgress: Boolean get() = Slot.PROGRESS in slots
}

/** PendingIntents shared by every widget in the collection. They all go through the existing playback actions. */
object WidgetIntents {
    private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    fun openApp(context: Context): PendingIntent =
        PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), FLAGS)

    fun playback(context: Context, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, MusicWidgetReceiver::class.java).apply { this.action = action },
            FLAGS,
        )

    /** Wires every control the variant has to the existing playback actions; nothing decorative. */
    fun bind(context: Context, views: RemoteViews, variant: LalasiWidgetVariant) {
        val slots = variant.slots
        views.setOnClickPendingIntent(android.R.id.background, openApp(context))
        if (Slot.ART in slots) views.setOnClickPendingIntent(R.id.lw_art, openApp(context))
        if (Slot.PLAY in slots) views.setOnClickPendingIntent(R.id.lw_play, playback(context, MusicWidgetReceiver.ACTION_PLAY_PAUSE, 1))
        if (Slot.PREV in slots) views.setOnClickPendingIntent(R.id.lw_prev, playback(context, MusicWidgetReceiver.ACTION_PREVIOUS, 7))
        if (Slot.NEXT in slots) views.setOnClickPendingIntent(R.id.lw_next, playback(context, MusicWidgetReceiver.ACTION_NEXT, 6))
        if (Slot.LIKE in slots) views.setOnClickPendingIntent(R.id.lw_like, playback(context, MusicWidgetReceiver.ACTION_LIKE, 2))
        if (Slot.SHUFFLE in slots) views.setOnClickPendingIntent(R.id.lw_shuffle, playback(context, MusicWidgetReceiver.ACTION_SHUFFLE, 8))
        if (Slot.REPEAT in slots) views.setOnClickPendingIntent(R.id.lw_repeat, playback(context, MusicWidgetReceiver.ACTION_REPEAT, 9))
    }
}

/** Base provider: asks the running service to redraw, and keeps a working layout when nothing plays yet. */
abstract class LalasiWidgetReceiver(private val variant: LalasiWidgetVariant) : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        LalasiWidgetRenderer.invalidate(appWidgetIds)
        if (MusicService.isRunning) {
            requestRefresh(context)
        } else {
            // Not playing: the layout still opens LALASI and its controls are wired.
            appWidgetIds.forEach { id ->
                appWidgetManager.updateAppWidget(id, LalasiWidgetRenderer.idleViews(context, variant))
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        LalasiWidgetRenderer.invalidate(intArrayOf(appWidgetId))
        if (MusicService.isRunning) {
            requestRefresh(context)
        } else {
            appWidgetManager.updateAppWidget(appWidgetId, LalasiWidgetRenderer.idleViews(context, variant))
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        LalasiWidgetRenderer.forget(appWidgetIds)
    }

    private fun requestRefresh(context: Context) {
        val intent = Intent(context, MusicService::class.java).apply { action = MusicWidgetReceiver.ACTION_UPDATE_WIDGET }
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (_: Exception) {
            // the service cannot start right now; the next playback update redraws the widget
        }
    }
}

class LalasiCompactWidgetReceiver : LalasiWidgetReceiver(LalasiWidgetVariant.COMPACT)
class LalasiSquareWidgetReceiver : LalasiWidgetReceiver(LalasiWidgetVariant.SQUARE)
class LalasiWideWidgetReceiver : LalasiWidgetReceiver(LalasiWidgetVariant.WIDE)
class LalasiExpandedWidgetReceiver : LalasiWidgetReceiver(LalasiWidgetVariant.EXPANDED)
class LalasiLargeSquareWidgetReceiver : LalasiWidgetReceiver(LalasiWidgetVariant.LARGE)
class LalasiLyricsWidgetReceiver : LalasiWidgetReceiver(LalasiWidgetVariant.LYRICS)
class LalasiCompactLyricsWidgetReceiver : LalasiWidgetReceiver(LalasiWidgetVariant.COMPACT_LYRICS)
class LalasiWideLyricsWidgetReceiver : LalasiWidgetReceiver(LalasiWidgetVariant.WIDE_LYRICS)
class LalasiMinimalWidgetReceiver : LalasiWidgetReceiver(LalasiWidgetVariant.MINIMAL)
class LalasiArtworkWidgetReceiver : LalasiWidgetReceiver(LalasiWidgetVariant.ARTWORK)
