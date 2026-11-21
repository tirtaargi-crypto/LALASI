// WidgetLyricsSource.kt

package com.danrey.lalasi.widget

import com.danrey.lalasi.db.MusicDatabase
import com.danrey.lalasi.db.entities.LyricsEntity
import com.danrey.lalasi.lyrics.LyricsEntry
import com.danrey.lalasi.lyrics.LyricsUtils
import kotlinx.coroutines.flow.first

/** What the lyric widgets show for the playing song right now. */
data class LyricsFrame(
    val previous: String,
    val current: String,
    val next: String,
    val mode: Mode,
    /** milliseconds until the shown line changes; null when nothing will change (plain or missing lyrics) */
    val changeInMs: Long?,
) {
    enum class Mode { SYNCED, PLAIN, EMPTY }
}

/**
 * Reads the lyrics LALASI already stored for the song and picks the line to show, using the song's own
 * timestamps. Unsynced lyrics are shown as plain lines; nothing is ever timed that the source did not time.
 */
class WidgetLyricsSource(private val database: MusicDatabase) {

    private class Loaded(
        val songId: String,
        val lines: List<LyricsEntry>,
        val synced: Boolean,
        val loadedAt: Long,
    )

    private var loaded: Loaded? = null

    suspend fun frame(songId: String?, positionMs: Long, offsetMs: Int): LyricsFrame {
        if (songId == null) return EMPTY
        val data = load(songId)
        if (data.lines.isEmpty()) return EMPTY

        val lines = data.lines
        if (!data.synced) {
            // honest fallback: the first lines as written, no highlight movement
            return LyricsFrame(
                previous = lines.getOrNull(0)?.text.orEmpty(),
                current = lines.getOrNull(1)?.text ?: lines[0].text,
                next = lines.getOrNull(2)?.text.orEmpty(),
                mode = LyricsFrame.Mode.PLAIN,
                changeInMs = null,
            )
        }

        val position = positionMs + offsetMs
        val index = LyricsUtils.findCurrentLineIndex(lines, position)
        val previous = lines.getOrNull(index - 1)?.text.orEmpty()
        val current = when {
            index < 0 || index >= lines.size -> NOTE
            else -> lines[index].text
        }
        val next = lines.getOrNull(index + 1)?.text.orEmpty()
        val changeAt = lines.getOrNull(index + 1)?.time
        val changeIn = changeAt?.let { (it - LyricsUtils.LineLeadInMs - position).coerceAtLeast(0L) }
        return LyricsFrame(previous, current, next, LyricsFrame.Mode.SYNCED, changeIn)
    }

    private suspend fun load(songId: String): Loaded {
        val cached = loaded
        val now = System.currentTimeMillis()
        if (cached != null && cached.songId == songId &&
            (cached.lines.isNotEmpty() || now - cached.loadedAt < RetryEmptyMs)
        ) return cached

        val raw = try {
            database.lyrics(songId).first()?.lyrics
        } catch (_: Exception) {
            null
        }
        val parsed = if (raw.isNullOrBlank() || raw == LyricsEntity.LYRICS_NOT_FOUND) null else parse(raw)
        return Loaded(songId, parsed?.first.orEmpty(), parsed?.second ?: false, now).also { loaded = it }
    }

    private fun parse(raw: String): Pair<List<LyricsEntry>, Boolean>? = try {
        val timed = LyricsUtils.parseLyricsCached(raw).filter { !it.isBackground && it.text.isNotBlank() }
        if (timed.isNotEmpty() && !LyricsUtils.isUnsynced(timed)) {
            timed to true
        } else {
            val plain = LyricsUtils.parsePlainLyrics(raw).filter { !it.isBackground && it.text.isNotBlank() }
            if (plain.isNotEmpty()) plain to false else null
        }
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val NOTE = "♪"
        const val RetryEmptyMs = 5_000L
        val EMPTY = LyricsFrame("", "", "", LyricsFrame.Mode.EMPTY, null)
    }
}
