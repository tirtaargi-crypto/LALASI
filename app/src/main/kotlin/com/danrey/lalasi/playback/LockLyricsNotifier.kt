// LockLyricsNotifier.kt
//
// Follows the lyric line of the song while the phone is locked, the screen is on and a song with timed lyrics plays, and hands it to the
// accessibility overlay (LockLyricsBus -> LockLyricsAccessibilityService), which draws it over the lock screen. When the accessibility
// service is not switched on, the same line is posted as an ordinary public notification instead, so the lock screen still lists it.
// Everything is removed again the moment the phone is unlocked or the screen turns off, so nothing lingers.
//
// It runs inside the existing MusicService (one screen receiver, one loop that only exists while the lock screen is showing), uses the
// lyrics already in the database and the player's real position, and never invents a line: no timed lyrics means no notification.

package com.danrey.lalasi.playback

import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.danrey.lalasi.R
import com.danrey.lalasi.constants.LockScreenLyricsKey
import com.danrey.lalasi.db.MusicDatabase
import com.danrey.lalasi.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.danrey.lalasi.lyrics.LyricsEntry
import com.danrey.lalasi.lyrics.LyricsUtils
import com.danrey.lalasi.models.MediaMetadata
import com.danrey.lalasi.utils.dataStore
import com.danrey.lalasi.utils.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LockLyricsNotifier(
    private val context: Context,
    private val scope: CoroutineScope,
    private val database: MusicDatabase,
    private val metadata: StateFlow<MediaMetadata?>,
    private val isPlaying: () -> Boolean,
    private val positionMs: () -> Long,
) {
    private var registered = false
    private var loop: Job? = null
    private var posted = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> begin()
                Intent.ACTION_SCREEN_OFF, Intent.ACTION_USER_PRESENT -> end()
            }
        }
    }

    fun start() {
        if (registered) return
        createChannel()
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        registered = true
        // the service may start while the phone is already locked with the screen on
        begin()
    }

    fun stop() {
        if (registered) runCatching { context.unregisterReceiver(receiver) }
        registered = false
        end()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.lock_lyrics_channel), NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            },
        )
    }

    private fun lockedWithScreenOn(): Boolean {
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager ?: return false
        val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return keyguard.isKeyguardLocked && power.isInteractive
    }

    private fun begin() {
        if (!registered) return
        if (!context.dataStore.get(LockScreenLyricsKey, false)) return
        if (!lockedWithScreenOn()) return
        if (loop?.isActive == true) return
        loop = scope.launch {
            var cachedId: String? = null
            var lines: List<LyricsEntry> = emptyList()
            var offset = 0L
            var shownKey: String? = null
            while (isActive) {
                val song = metadata.value
                if (song == null || !isPlaying()) {
                    clear()
                    shownKey = null
                    delay(1000)
                    continue
                }
                if (song.id != cachedId) {
                    cachedId = song.id
                    val raw = runCatching { database.lyrics(song.id).first()?.lyrics }.getOrNull()
                    lines = timedLines(raw)
                    offset = runCatching { (database.song(song.id).first()?.song?.lyricsOffset ?: 0).toLong() }.getOrDefault(0L)
                    shownKey = null
                }
                if (lines.isEmpty()) {
                    // no timed lyrics: nothing is posted, no line is made up
                    clear()
                    shownKey = null
                    delay(1500)
                    continue
                }
                val index = LyricsUtils.findCurrentLineIndex(lines, positionMs() + offset)
                val key = "${song.id}#$index"
                // findCurrentLineIndex answers lines.size once the song is past its last line: nothing to show then
                if (index in lines.indices && key != shownKey) {
                    post(song, lines[index].text.trim(), lines.getOrNull(index + 1)?.text?.trim().orEmpty())
                    shownKey = key
                }
                delay(250)
            }
        }
    }

    private fun end() {
        loop?.cancel()
        loop = null
        clear()
    }

    // the lyrics only count when they carry timestamps, plain text cannot follow the song
    private fun timedLines(raw: String?): List<LyricsEntry> {
        if (raw.isNullOrBlank() || raw == LYRICS_NOT_FOUND) return emptyList()
        val parsed = LyricsUtils.parseLyricsCached(raw).filter { it.text.isNotBlank() }
        return if (parsed.size >= 2 && parsed.any { it.time > 0L }) parsed else emptyList()
    }

    private fun post(song: MediaMetadata, line: String, next: String) {
        if (line.isEmpty()) return
        LockLyricsBus.current.value = LockLyricsBus.Line(
            line = line,
            next = next,
            title = song.title,
            artist = song.artists.joinToString { it.name },
        )
        // the overlay is drawing it: no second copy as a notification
        if (LockLyricsBus.overlayConnected) {
            clearNotification()
            return
        }
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.lalasi_notification)
            .setContentTitle(line)
            .setContentText(next.ifEmpty { null })
            .setSubText(listOf(song.title, song.artists.joinToString { it.name }).filter { it.isNotBlank() }.joinToString(" • "))
            // public: the system lock screen shows it in full, the way the phone's own media and message rows are shown
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setOngoing(true)
            .setContentIntent(open)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            posted = true
        } // without the notification permission nothing is posted and nothing breaks
    }

    // nothing is shown any more: the overlay line and the fallback notification both go
    private fun clear() {
        LockLyricsBus.current.value = null
        clearNotification()
    }

    private fun clearNotification() {
        if (!posted) return
        runCatching { NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID) }
        posted = false
    }

    companion object {
        const val CHANNEL_ID = "lock_lyrics_01"
        const val NOTIFICATION_ID = 4711
    }
}
