// QQMusicLyricsProvider.kt

package com.danrey.lalasi.lyrics

import android.content.Context
import com.music.directlyrics.QQMusic
import com.danrey.lalasi.constants.EnableQQMusicKey
import com.danrey.lalasi.utils.dataStore
import com.danrey.lalasi.utils.get

object QQMusicLyricsProvider : LyricsProvider {
    override val name = "QQMusic"
    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableQQMusicKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = QQMusic.getLyrics(title, artist, duration)
}
