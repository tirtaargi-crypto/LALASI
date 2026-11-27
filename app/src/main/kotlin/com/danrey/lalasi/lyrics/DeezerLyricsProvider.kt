// DeezerLyricsProvider.kt

package com.danrey.lalasi.lyrics

import android.content.Context
import com.music.directlyrics.Deezer
import com.danrey.lalasi.constants.EnableDeezerKey
import com.danrey.lalasi.utils.dataStore
import com.danrey.lalasi.utils.get

object DeezerLyricsProvider : LyricsProvider {
    override val name = "Deezer"
    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableDeezerKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = Deezer.getLyrics(title, artist, duration)
}
