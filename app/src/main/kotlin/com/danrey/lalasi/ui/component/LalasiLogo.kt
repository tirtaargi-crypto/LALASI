package com.danrey.lalasi.ui.component

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

/**
 * The brand mark, decoded once per process. App.onCreate warms it on Dispatchers.IO, so the first
 * Compose frame (loading overlay, Home) finds it ready instead of decoding a PNG on the main thread,
 * and Home being rebuilt never decodes it again. If a composition gets here first it waits for the
 * one decode in flight (or does it itself) - never twice. Same file, same pixels as before.
 */
object LalasiLogoCache {
    @Volatile
    private var cached: ImageBitmap? = null

    fun load(context: Context): ImageBitmap? {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val decoded = runCatching {
                context.applicationContext.assets.open("lalasi_official_logo.png").use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }.getOrNull()
            cached = decoded
            return decoded
        }
    }
}

/**
 * Loads the brand mark from app assets instead of Android drawable resource resolution.
 * This avoids crashes on devices/build variants where the merged drawable resolves to
 * an XML bitmap wrapper that Compose painterResource cannot decode.
 */
@Composable
fun LalasiLogo(
    modifier: Modifier = Modifier,
    contentDescription: String? = "LALASI",
    contentScale: ContentScale = ContentScale.Fit,
) {
    val context = LocalContext.current
    val bitmap = remember(context) { LalasiLogoCache.load(context) }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
        )
    }
}
