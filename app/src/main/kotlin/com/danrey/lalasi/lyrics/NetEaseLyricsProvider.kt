// NetEaseLyricsProvider.kt

package com.danrey.lalasi.lyrics

import android.content.Context
import com.music.directlyrics.NetEase
import com.danrey.lalasi.constants.EnableNetEaseKey
import com.danrey.lalasi.utils.dataStore
import com.danrey.lalasi.utils.get

object NetEaseLyricsProvider : LyricsProvider {
    override val name = "NetEase"
    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableNetEaseKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = NetEase.getLyrics(title, artist, duration)
}
