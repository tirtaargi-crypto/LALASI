package com.danrey.lalasi.ui.component

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

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
    val bitmap = remember(context) {
        runCatching {
            context.assets.open("lalasi_official_logo.png").use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
        )
    }
}
