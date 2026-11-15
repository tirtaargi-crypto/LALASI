package com.danrey.lalasi.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentSafetyTest {
    @Test fun blocksClearlyPornographicMetadata() {
        assertFalse(ContentSafety.isSafe("JAV soundtrack", "Studio", "Original soundtrack"))
        assertFalse(ContentSafety.isSafe("Hentai theme", "Artist", "Album"))
        assertFalse(ContentSafety.isSafe("Track", "Artist", "Pornographic album"))
    }

    @Test fun keepsOrdinaryJapaneseAndAnimeMusic() {
        assertTrue(ContentSafety.isSafe("Anime opening", "Japanese artist", "J-pop collection"))
        assertTrue(ContentSafety.isSafe("Sakura", "Japanese singer", "Instrumental album"))
    }

    @Test fun explicitLanguageAloneIsNotPornography() {
        assertTrue(ContentSafety.isSafe("Explicit lyrics", "Artist", "Album"))
        assertTrue(ContentSafety.isSafe("Adult contemporary", "Artist", "Album"))
    }
}
