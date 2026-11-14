package com.danrey.lalasi.utils

import androidx.media3.common.MediaMetadata
import java.text.Normalizer
import java.util.Locale

/**
 * Conservative discovery filter for clearly pornographic/adult-entertainment metadata.
 * It deliberately does not treat ordinary explicit-language labels, Japanese language,
 * anime, or generic words such as "adult" as sufficient evidence on their own.
 */
object ContentSafety {
    fun isSafe(metadata: com.danrey.lalasi.models.MediaMetadata): Boolean = isSafe(metadata.title, metadata.artists.joinToString(" ") { it.name }, metadata.album?.title, null)
    private val strongSignals = listOf(
        Regex("\\bjav\\b", RegexOption.IGNORE_CASE),
        Regex("\\bjapanese adult video\\b", RegexOption.IGNORE_CASE),
        Regex("\\badult video soundtrack\\b", RegexOption.IGNORE_CASE),
        Regex("\\bporn(?:ography|ographic)?\\b", RegexOption.IGNORE_CASE),
        Regex("\\bhentai\\b", RegexOption.IGNORE_CASE),
        Regex("\\bxxx\\b", RegexOption.IGNORE_CASE),
        Regex("\\bsex tape\\b", RegexOption.IGNORE_CASE),
        Regex("\\badult film soundtrack\\b", RegexOption.IGNORE_CASE),
        Regex("\\berotic audio\\b", RegexOption.IGNORE_CASE),
        Regex("\\buncensored adult\\b", RegexOption.IGNORE_CASE),
    )

    private val explicitArtworkSignals = listOf(
        "pornographic artwork", "explicit sexual artwork", "adult video cover",
        "jav cover", "hentai cover", "xxx cover"
    )

    private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT)
        .replace(Regex("[\\p{Punct}\\p{Z}]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    fun isSafe(title: String?, artist: String?, album: String?, description: String? = null): Boolean {
        val text = normalize(listOfNotNull(title, artist, album, description).joinToString(" "))
        if (text.isBlank()) return true
        if (explicitArtworkSignals.any { text.contains(normalize(it)) }) return false
        // Contextual phrase matching avoids blocking ordinary Japanese music or explicit lyrics.
        return strongSignals.none { it.containsMatchIn(" $text ") }
    }

    fun isSafe(metadata: MediaMetadata): Boolean = isSafe(
        title = metadata.title?.toString(),
        artist = metadata.artist?.toString(),
        album = metadata.albumTitle?.toString(),
        description = metadata.description?.toString(),
    )
}
