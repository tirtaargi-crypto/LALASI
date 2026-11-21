// AuroraPlayerStyle.kt
//
// "Aurora": a glass player. The cover floats as a big rounded card with a soft glow in its own colours over a blurred, slowly drifting
// wash of the same cover, the title and artist sit under it, the line of lyrics that is being sung rides in a frosted pill, and a thin
// seek bar (tap or drag) and the three transport circles close it off. It is one more look in the player selector: no other style is
// touched, and it is built from the same parts as the others (StyleFrame, the song carousel, TransportCircles, the ••• menu).

package com.danrey.lalasi.ui.player.styles

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.danrey.lalasi.LocalPlayerConnection
import com.danrey.lalasi.constants.PlayerStyle
import com.danrey.lalasi.lyrics.LyricsUtils
import com.danrey.lalasi.ui.player.CarouselCrossfade
import com.danrey.lalasi.ui.player.CarouselPictures
import com.danrey.lalasi.utils.makeTimeString

private val AuroraGlass = Color.White.copy(alpha = 0.10f)
private val AuroraGlassEdge = Color.White.copy(alpha = 0.16f)

@Composable
internal fun AuroraPlayerStyle(actions: StyleActions, modifier: Modifier, preview: Boolean) {
    val tones = rememberStyleTones()
    val track = rememberStyleTrack()
    val r = rememberResponsive()
    val position = rememberLivePosition(animated = !preview)
    val lyricsClock = position.withLyricsOffset()
    val duration = rememberDurationMs()
    val lyrics = rememberStyleLyrics(readings = !preview)
    val carousel = rememberStyleCarousel(preview)
    val circle = r.dp(0.15f, 50.dp, 64.dp)
    val menuButton = r.dp(0.078f, 28.dp, 40.dp)

    // the line being sung; only the index is read here, so the screen recomposes when the line changes and not on every frame
    val lineIndex by remember(lyrics.lines, lyricsClock) {
        derivedStateOf { if (lyrics.lines.isEmpty()) -1 else LyricsUtils.findCurrentLineIndex(lyrics.lines, lyricsClock.positionMs()) }
    }

    StyleFrame(
        PlayerStyle.AURORA, preview, modifier, tones,
        background = {
            // the cover, blurred and stretched, crossfading along the row of songs; a scrim keeps the words readable on any cover
            CarouselCrossfade(carousel = carousel, fallbackUrl = track.artwork, modifier = Modifier.fillMaxSize()) { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { scaleX = 1.45f; scaleY = 1.45f }
                        .blur(56.dp),
                )
            }
            AuroraDrift(tones, preview)
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.30f),
                            0.55f to Color.Black.copy(alpha = 0.52f),
                            1f to Color.Black.copy(alpha = 0.82f),
                        ),
                    ),
            )
        },
    ) { landscape, menuOpen, setMenuOpen, rotated, setRotated ->
        val insets = styleInsets(landscape)
        if (landscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 28.dp + insets.start, end = 28.dp + insets.end, top = 22.dp, bottom = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                BoxWithConstraints(Modifier.weight(0.9f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    AuroraCover(carousel, track.artwork, tones, minOf(maxWidth, maxHeight))
                }
                Column(Modifier.weight(1.1f).fillMaxHeight().padding(end = menuButton + 14.dp), verticalArrangement = Arrangement.Center) {
                    AuroraDetails(carousel, track, lyrics, r, landscape = true)
                    AuroraLine(lyrics.lines, lineIndex, r)
                    Spacer(Modifier.height(14.dp))
                    AuroraProgress(position, duration, preview)
                    AuroraTimes(position, duration)
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TransportCircles(tones = tones, size = circle, spacing = 14.dp) }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 26.dp + insets.start, end = 26.dp + insets.end, top = insets.top + 20.dp, bottom = insets.bottom + 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // the room the ••• button takes at the top right
                Spacer(Modifier.height(menuButton + 8.dp))
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    AuroraCover(carousel, track.artwork, tones, minOf(maxWidth, maxHeight) * 0.94f)
                }
                Spacer(Modifier.height(20.dp))
                AuroraDetails(carousel, track, lyrics, r, landscape = false)
                AuroraLine(lyrics.lines, lineIndex, r)
                Spacer(Modifier.height(14.dp))
                AuroraProgress(position, duration, preview)
                AuroraTimes(position, duration)
                Spacer(Modifier.height(14.dp))
                TransportCircles(tones = tones, size = circle, spacing = 18.dp)
            }
        }
        StyleMenu(
            open = menuOpen, onOpenChange = setMenuOpen, tones = tones, actions = actions,
            rotated = rotated, onRotate = setRotated, atBottom = false,
            end = 22.dp + insets.end, edge = if (landscape) 18.dp else insets.top + 20.dp,
            buttonSize = menuButton,
        )
    }
}

/** two soft colour pools drifting under the blurred cover, so the backdrop is never quite still; a selector card keeps them put */
@Composable
private fun BoxScope.AuroraDrift(tones: StyleTones, preview: Boolean) {
    val transition = rememberInfiniteTransition(label = "auroraDrift")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "auroraDriftT",
    )
    Box(
        Modifier
            .matchParentSize()
            .drawBehind {
                val phase = if (preview) 0.5f else t
                val a = Offset(size.width * (0.15f + 0.55f * phase), size.height * (0.18f + 0.12f * phase))
                val b = Offset(size.width * (0.85f - 0.5f * phase), size.height * (0.72f - 0.14f * phase))
                val radius = size.maxDimension * 0.62f
                drawCircle(Brush.radialGradient(listOf(tones.played.copy(alpha = 0.55f), Color.Transparent), a, radius), radius, a)
                drawCircle(Brush.radialGradient(listOf(Color(0xFF5E5CE6).copy(alpha = 0.30f), Color.Transparent), b, radius), radius, b)
            },
    )
}

/** the cover as a glass card: a glow in its own colours under it, a hairline round it, the row of songs sliding inside */
@Composable
private fun AuroraCover(
    carousel: com.danrey.lalasi.ui.player.SongCarousel?,
    artwork: String?,
    tones: StyleTones,
    side: Dp,
) {
    val shape = RoundedCornerShape(30.dp)
    CarouselPictures(
        carousel = carousel,
        fallbackUrl = artwork,
        modifier = Modifier
            .size(side)
            .shadow(34.dp, shape, ambientColor = tones.played, spotColor = tones.played)
            .clip(shape)
            .background(AuroraGlass)
            .border(1.dp, AuroraGlassEdge, shape),
    ) { url, pictureModifier ->
        AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = pictureModifier)
    }
}

@Composable
private fun AuroraDetails(
    carousel: com.danrey.lalasi.ui.player.SongCarousel?,
    track: StyleTrack,
    lyrics: StyleLyrics,
    r: Responsive,
    landscape: Boolean,
) {
    Box(Modifier.fillMaxWidth()) {
        StyleSongs(carousel, track, lyrics) { song, slotModifier ->
            Column(
                modifier = Modifier.fillMaxWidth().then(slotModifier),
                horizontalAlignment = if (landscape) Alignment.Start else Alignment.CenterHorizontally,
            ) {
                Text(
                    text = song.track.title,
                    color = Color.White,
                    fontSize = r.sp(0.062f, 20f, 28f),
                    lineHeight = r.sp(0.078f, 25f, 34f),
                    fontWeight = FontWeight.Bold,
                    textAlign = if (landscape) TextAlign.Start else TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = song.track.artist,
                    color = Color.White.copy(alpha = 0.66f),
                    fontSize = r.sp(0.042f, 14f, 18f),
                    lineHeight = r.sp(0.056f, 19f, 24f),
                    fontWeight = FontWeight.Medium,
                    textAlign = if (landscape) TextAlign.Start else TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** the line being sung, in a frosted pill; a song without timed lyrics shows no pill at all */
@Composable
private fun AuroraLine(lines: List<com.danrey.lalasi.lyrics.LyricsEntry>, index: Int, r: Responsive) {
    if (lines.isEmpty()) return
    Spacer(Modifier.height(14.dp))
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(shape)
            .background(AuroraGlass)
            .border(1.dp, AuroraGlassEdge, shape)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = index,
            transitionSpec = {
                (fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 3 }) togetherWith
                    (fadeOut(tween(160)) + slideOutVertically(tween(160)) { -it / 3 })
            },
            label = "auroraLine",
        ) { i ->
            Text(
                text = lines.getOrNull(i)?.text?.trim().orEmpty(),
                color = Color.White,
                fontSize = r.sp(0.048f, 16f, 21f),
                lineHeight = r.sp(0.062f, 21f, 27f),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** a thin seek bar: tap to jump, drag to scrub (the song only moves when the finger lifts) */
@Composable
private fun AuroraProgress(position: LivePosition, duration: Long, preview: Boolean) {
    val connection = LocalPlayerConnection.current
    var dragging by remember { mutableStateOf<Float?>(null) }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(preview, duration) {
                if (preview) return@pointerInput
                detectTapGestures { offset ->
                    val f = (offset.x / size.width).coerceIn(0f, 1f)
                    connection?.player?.seekTo((f * duration).toLong())
                }
            }
            .pointerInput(preview, duration) {
                if (preview) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { dragging = (it.x / size.width).coerceIn(0f, 1f) },
                    onDragEnd = {
                        dragging?.let { f -> connection?.player?.seekTo((f * duration).toLong()) }
                        dragging = null
                    },
                    onDragCancel = { dragging = null },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        dragging = (change.position.x / size.width).coerceIn(0f, 1f)
                    },
                )
            },
    ) {
        val fraction = dragging ?: (position.positionMs().toFloat() / duration).coerceIn(0f, 1f)
        val trackHeight = 5.dp.toPx()
        val cy = size.height / 2f
        val corner = CornerRadius(trackHeight / 2f)
        drawRoundRect(Color.White.copy(alpha = 0.22f), Offset(0f, cy - trackHeight / 2f), Size(size.width, trackHeight), corner)
        drawRoundRect(Color.White, Offset(0f, cy - trackHeight / 2f), Size(size.width * fraction, trackHeight), corner)
        drawCircle(Color.White, if (dragging != null) 9.dp.toPx() else 6.dp.toPx(), Offset(size.width * fraction, cy))
    }
}

@Composable
private fun AuroraTimes(position: LivePosition, duration: Long) {
    val style = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        PlayedTime(position, Color.White.copy(alpha = 0.7f), style, Modifier)
        Text(makeTimeString(duration), color = Color.White.copy(alpha = 0.7f), style = style, maxLines = 1, softWrap = false)
    }
}
