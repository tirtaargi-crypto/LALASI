// AuroraPlayerStyle.kt
//
// "Aurora": a glass player. The cover floats as a big rounded card with a soft glow in its own colours over a blurred, slowly drifting
// wash of the same cover, the title and artist sit under it, the lyrics run as free lines with no card around them (the line being sung bright and sharp, the others dimmer and softer by distance, gliding
// as the song goes; in landscape, and when the player is turned, they fill the right half like a media player), and a thin
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.CircleShape
import androidx.media3.common.Player
import com.danrey.lalasi.R
import com.danrey.lalasi.ui.player.flagship.AnimatedPlayGlyph
import com.danrey.lalasi.ui.player.flagship.AnimatedSkipGlyph
import com.danrey.lalasi.ui.player.flagship.SkipCycleState
import kotlinx.coroutines.launch
import androidx.compose.runtime.State
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import kotlinx.coroutines.flow.collectLatest
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

    // the line being sung; only the lyric list reads it, so only the list recomposes when the line changes, and not on every frame
    val lineIndex: State<Int> = remember(lyrics.lines, lyricsClock) {
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
        if (landscape && lyrics.lines.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 28.dp + insets.start, end = 28.dp + insets.end, top = 22.dp, bottom = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                Column(Modifier.weight(0.8f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        AuroraCover(carousel, track.artwork, tones, minOf(maxWidth, maxHeight))
                    }
                    Spacer(Modifier.height(14.dp))
                    AuroraDetails(carousel, track, lyrics, r, landscape = true)
                }
                Column(Modifier.weight(1.2f).fillMaxHeight().padding(end = menuButton + 14.dp)) {
                    AuroraLyrics(lyrics.lines, lineIndex, r.sp(0.085f, 22f, 36f), 0.4f, preview, Modifier.weight(1f).fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    AuroraProgress(position, duration, preview)
                    AuroraTimes(position, duration)
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TransportCircles(tones = tones, size = circle, spacing = 14.dp) }
                }
            }
        } else if (landscape) {
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
                    Spacer(Modifier.height(14.dp))
                    AuroraProgress(position, duration, preview)
                    AuroraTimes(position, duration)
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TransportCircles(tones = tones, size = circle, spacing = 14.dp) }
                }
            }
        } else {
            // portrait, the way iOS lays out Now Playing: a big cover that settles back while paused, the words left-aligned with the
            // lyrics button beside them, a thin scrubber, then plain big transport glyphs (no circles)
            val sidePad = r.dp(0.075f, 24.dp, 34.dp)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = sidePad + insets.start, end = sidePad + insets.end, top = insets.top + 12.dp, bottom = insets.bottom + 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // the room the ••• button takes at the top right
                Spacer(Modifier.height(menuButton + 6.dp))
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    AuroraCover(carousel, track.artwork, tones, minOf(maxWidth, maxHeight), breathe = !preview, corner = 16.dp)
                }
                Spacer(Modifier.height(22.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { AuroraDetails(carousel, track, lyrics, r, landscape = false) }
                    actions.onLyrics?.let { openLyrics ->
                        Spacer(Modifier.size(12.dp))
                        StyleCircle(icon = R.drawable.lyrics, tones = tones, size = 40.dp, onClick = openLyrics)
                    }
                }
                if (lyrics.lines.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    // a tap on the lines opens the whole lyrics page, the flagship's own
                    AuroraLyrics(lyrics.lines, lineIndex, r.sp(0.056f, 18f, 26f), 0.5f, preview, Modifier.fillMaxWidth().height(r.dp(0.36f, 108.dp, 160.dp)), onOpen = actions.onLyrics)
                }
                Spacer(Modifier.height(10.dp))
                AuroraProgress(position, duration, preview)
                AuroraTimes(position, duration)
                Spacer(Modifier.height(6.dp))
                AuroraTransport(size = r.dp(0.17f, 58.dp, 78.dp), preview = preview)
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
                val c = Offset(size.width * (0.50f + 0.22f * (1f - phase)), size.height * (0.46f + 0.16f * phase))
                val radius = size.maxDimension * 0.62f
                drawCircle(Brush.radialGradient(listOf(tones.played.copy(alpha = 0.60f), Color.Transparent), a, radius), radius, a)
                drawCircle(Brush.radialGradient(listOf(tones.remaining.copy(alpha = 0.55f), Color.Transparent), b, radius), radius, b)
                drawCircle(Brush.radialGradient(listOf(tones.played.copy(alpha = 0.28f), Color.Transparent), c, radius * 0.8f), radius * 0.8f, c)
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
    /** portrait: the cover settles back a little while paused and springs out when it plays, as in iOS */
    breathe: Boolean = false,
    corner: Dp = 20.dp,
) {
    val shape = RoundedCornerShape(corner)
    val connection = LocalPlayerConnection.current
    val playing = if (breathe && connection != null) connection.uiState.transportState.collectAsState().value.isPlaying else true
    val scale by animateFloatAsState(
        targetValue = if (playing) 1f else 0.86f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 260f),
        label = "auroraCoverScale",
    )
    CarouselPictures(
        carousel = carousel,
        fallbackUrl = artwork,
        modifier = Modifier
            .size(side)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(30.dp, shape, ambientColor = Color.Black, spotColor = tones.played.copy(alpha = 0.85f))
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
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = song.track.title,
                    color = Color.White,
                    fontSize = r.sp(0.062f, 20f, 28f),
                    lineHeight = r.sp(0.078f, 25f, 34f),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
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
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * The lyrics as one column of free lines, no card and no frame. The line being sung is bright and sharp; the others fall back by how far they are
 * from it (dimmer, a little smaller, softer: the blur needs Android 12, before that they are only dimmer). Each change glides: the column
 * scrolls so the sung line settles on [anchor] (a fraction of the height), a seek jumps there at once, and the ends fade out. Tapping a line
 * jumps the song to it. [fontSize] is the sung line's size.
 */
@Composable
private fun AuroraLyrics(
    lines: List<com.danrey.lalasi.lyrics.LyricsEntry>,
    index: State<Int>,
    fontSize: androidx.compose.ui.unit.TextUnit,
    anchor: Float,
    preview: Boolean,
    modifier: Modifier = Modifier,
    /** set: a tap on any line opens this (the lyrics page) instead of seeking to the line */
    onOpen: (() -> Unit)? = null,
) {
    if (lines.isEmpty()) return
    val state = rememberLazyListState()
    val connection = LocalPlayerConnection.current
    BoxWithConstraints(modifier) {
        val above = maxHeight * anchor
        val below = maxHeight * (1f - anchor)
        // follow the sung line; a newer line cancels a glide still running, so it never lags behind
        LaunchedEffect(lines, anchor) {
            var first = true
            snapshotFlow { index.value }.collectLatest { i ->
                glideTo(state, i, anchor, animate = !first && !preview)
                first = false
            }
        }
        LazyColumn(
            state = state,
            userScrollEnabled = false,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        Brush.verticalGradient(0f to Color.Transparent, 0.14f to Color.Black, 0.80f to Color.Black, 1f to Color.Transparent),
                        blendMode = BlendMode.DstIn,
                    )
                },
        ) {
            item(key = "above") { Spacer(Modifier.height(above)) }
            items(lines.size, key = { it }) { i ->
                AuroraLyricLine(
                    text = lines[i].text.trim(),
                    line = i,
                    index = index,
                    fontSize = fontSize,
                    onClick = if (preview) null else (onOpen ?: { connection?.player?.seekTo(lines[i].time); Unit }),
                )
            }
            item(key = "below") { Spacer(Modifier.height(below)) }
        }
    }
}

@Composable
private fun AuroraLyricLine(text: String, line: Int, index: State<Int>, fontSize: androidx.compose.ui.unit.TextUnit, onClick: (() -> Unit)?) {
    // how far this line is from the sung one; read here, so a change recomposes the few lines on screen and nothing else
    val distance = kotlin.math.abs(line - index.value).coerceAtMost(3)
    val spec = androidx.compose.animation.core.tween<Float>(420, easing = FastOutSlowInEasing)
    val alpha by animateFloatAsState(when (distance) { 0 -> 1f; 1 -> 0.55f; 2 -> 0.36f; else -> 0.24f }, spec, label = "lyricAlpha")
    val scale by animateFloatAsState(when (distance) { 0 -> 1f; 1 -> 0.92f; else -> 0.86f }, spec, label = "lyricScale")
    val blur by animateDpAsState(
        when (distance) { 0 -> 0.dp; 1 -> 1.6.dp; 2 -> 3.2.dp; else -> 5.dp },
        androidx.compose.animation.core.tween(420, easing = FastOutSlowInEasing), label = "lyricBlur",
    )
    Text(
        text = text,
        color = Color.White,
        fontSize = fontSize,
        lineHeight = fontSize * 1.22f,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Start,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = onClick) else Modifier)
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0.5f)
            }
            .then(if (blur.value > 0.05f) Modifier.blur(blur, BlurredEdgeTreatment.Unbounded) else Modifier)
            .padding(vertical = 7.dp),
    )
}

/** scrolls so [line] sits on [anchor] of the height: a glide, or at once when it is far off screen or [animate] is false */
internal suspend fun glideTo(state: LazyListState, line: Int, anchor: Float, animate: Boolean) {
    if (line < 0) return
    val target = line + 1 // item 0 is the space above the first line
    try {
        fun gap(): Float? {
            val info = state.layoutInfo
            val item = info.visibleItemsInfo.firstOrNull { it.index == target } ?: return null
            return item.offset + item.size / 2f - info.viewportSize.height * anchor
        }
        val near = gap()
        if (near == null) {
            // not on screen (a seek, or the first show): put it there, wait a frame for the layout, then settle it exactly
            state.scrollToItem(target)
            androidx.compose.runtime.withFrameNanos { }
            gap()?.let { state.scrollBy(it) }
            return
        }
        if (animate && kotlin.math.abs(near) > 1f) {
            state.animateScrollBy(near, androidx.compose.animation.core.tween(520, easing = FastOutSlowInEasing))
        } else {
            state.scrollBy(near)
        }
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        // a layout that is not ready yet must never take the player down; the next line settles it
    }
}

/** a thin seek bar: tap to jump, drag to scrub (the song only moves when the finger lifts) */
@Composable
internal fun AuroraProgress(position: LivePosition, duration: Long, preview: Boolean) {
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
        val trackHeight = (if (dragging != null) 10.dp else 5.dp).toPx()
        val cy = size.height / 2f
        val corner = CornerRadius(trackHeight / 2f)
        drawRoundRect(Color.White.copy(alpha = 0.22f), Offset(0f, cy - trackHeight / 2f), Size(size.width, trackHeight), corner)
        drawRoundRect(Color.White, Offset(0f, cy - trackHeight / 2f), Size(size.width * fraction, trackHeight), corner)
    }
}

@Composable
internal fun AuroraTimes(position: LivePosition, duration: Long) {
    val style = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        PlayedTime(position, Color.White.copy(alpha = 0.7f), style, Modifier)
        Text(makeTimeString(duration), color = Color.White.copy(alpha = 0.7f), style = style, maxLines = 1, softWrap = false)
    }
}

/** a transport glyph with no circle: it presses in with a spring and a faint round highlight, the way iOS buttons answer a finger */
@Composable
private fun AuroraGlyphButton(
    size: Dp,
    enabled: Boolean = true,
    onClick: () -> Unit,
    glyph: @Composable (Color) -> Unit,
) {
    val press = remember { Animatable(1f) }
    val glow = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val interactive = LocalStyleInteractive.current
    val currentOnClick by rememberUpdatedState(onClick)
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = press.value
                scaleY = press.value
                alpha = if (enabled) 1f else 0.35f
            }
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f * glow.value))
            .pointerInput(enabled, interactive) {
                if (!enabled) return@pointerInput
                whileStyleInteractive(interactive) {
                    detectTapGestures(
                        onPress = {
                            scope.launch { press.animateTo(0.80f, spring(dampingRatio = 0.6f, stiffness = 900f)) }
                            scope.launch { glow.animateTo(1f, tween(90)) }
                            tryAwaitRelease()
                            scope.launch { press.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 420f)) }
                            scope.launch { glow.animateTo(0f, tween(260)) }
                        },
                        onTap = { currentOnClick() },
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) { glyph(Color.White) }
}

/** previous, play / pause, next: big plain glyphs spread across the width */
@Composable
internal fun AuroraTransport(size: Dp, preview: Boolean) {
    val connection = LocalPlayerConnection.current
    if (connection == null || preview) {
        // a selector card shows the row, it doesn't play
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.size(size)); Spacer(Modifier.size(size)); Spacer(Modifier.size(size))
        }
        return
    }
    val transport by connection.uiState.transportState.collectAsState()
    val scope = rememberCoroutineScope()
    val previous = remember { SkipCycleState() }
    val next = remember { SkipCycleState() }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        AuroraGlyphButton(size = size, enabled = transport.canSkipPrevious, onClick = {
            scope.launch { previous.step() }
            connection.seekToPrevious()
        }) { tint -> AnimatedSkipGlyph(cycle = previous, backwards = true, color = tint, width = size * 0.56f) }
        AuroraGlyphButton(size = size * 1.15f, onClick = {
            if (transport.playbackState == Player.STATE_ENDED) {
                connection.player.seekTo(0, 0)
                connection.player.playWhenReady = true
            } else {
                connection.togglePlayPause()
            }
        }) { tint -> AnimatedPlayGlyph(playing = transport.isPlaying, color = tint, height = size * 0.62f) }
        AuroraGlyphButton(size = size, enabled = transport.canSkipNext, onClick = {
            scope.launch { next.step() }
            connection.seekToNext()
        }) { tint -> AnimatedSkipGlyph(cycle = next, backwards = false, color = tint, width = size * 0.56f) }
    }
}
