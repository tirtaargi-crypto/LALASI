// DynamicIsland.kt
//
// An iPhone-style Dynamic Island. It is drawn OUTSIDE the app by the overlay window (IslandOverlayController); inside the app only the
// settings page shows a preview of it (DynamicIslandPreview). Two sizes of the same pill:
//  - normal (collapsed): cover on the left, the line being sung in the middle, moving bars on the right. There is
//    NO song title here: with no lyric line the pill rests at the size of a real Dynamic Island (cover | camera | bars).
//    The pill grows just wide enough for the line; a very long line is shrunk first and then scrolls, so a lyric
//    is never cut off with "...".
//  - camera mode: the line is split at a word boundary and flows AROUND the front camera, one half on each side of it,
//    so no letter is ever hidden behind the lens. The pill stays centred on the camera.
//  - two shapes: ISLAND (the floating pill) and NOTCH (flat top flush with the top edge of the screen, rounded bottom).
//  - open: tap it. A small player (cover, title, artist, seek bar, previous / play / next) with the lyrics
//    running next to it, every line wrapped in full, the current one lit word by word.
//  - minimise again with the handle at the top, a swipe up, or the player's own back button.
// Three looks: default (solid black), liquid glass (the song's artwork, a little blurred, under a dark glass tint, with the same
// rim as the nav bar) and blue.

package com.danrey.lalasi.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.danrey.lalasi.LocalPlayerConnection
import com.danrey.lalasi.R
import com.danrey.lalasi.constants.DynamicIslandEnabledKey
import com.danrey.lalasi.constants.DynamicIslandOffsetDefault
import com.danrey.lalasi.constants.DynamicIslandShape
import com.danrey.lalasi.constants.DynamicIslandStyle
import com.danrey.lalasi.constants.DynamicIslandStyleKey
import com.danrey.lalasi.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.danrey.lalasi.lyrics.LyricsEntry
import com.danrey.lalasi.lyrics.LyricsUtils
import com.danrey.lalasi.ui.theme.InterFontFamily
import com.danrey.lalasi.ui.utils.stableStatusBars
import com.danrey.lalasi.utils.rememberEnumPreference
import com.danrey.lalasi.utils.rememberPreference
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.danrey.lalasi.viewmodels.LyricsScreenViewModel
import kotlinx.coroutines.delay
import kotlin.math.abs

private val IslandBlue = Color(0xFF0A84FF)
private val CollapsedWidth = 168.dp
private val CollapsedHeight = 36.dp
private val ExpandedHeight = 216.dp

/** The overlay window's height while the card is open: the card plus a little room. */
val ExpandedWindowHeight = ExpandedHeight + 8.dp

/**
 * While the pill is minimised the overlay window has ONE fixed size: as wide as the pill can ever get and a hair taller than it.
 * The pill is centred inside it, so a line change that makes the pill wider or narrower only moves pixels inside the window; the
 * window itself is never resized to follow the pill (a wrap-content window re-lays out a frame late, which is what made the
 * pill flick to the left when it was minimised).
 */
fun islandClosedWindowWidth(fullWidth: Dp): Dp = (fullWidth - 20.dp).coerceAtMost(440.dp)

/**
 * The open card is a little narrower than the window, so the window keeps the SAME width open and closed: opening and minimising
 * only change its height (anchored at the top), never its width, and a window whose width changes is what made the pill jump sideways.
 * The few dp of margin are the room the elastic overshoot needs.
 */
fun islandCardWidth(fullWidth: Dp): Dp = islandClosedWindowWidth(fullWidth) - 12.dp
val ClosedWindowHeight = CollapsedHeight + 4.dp
private val ExpandedRadius = 34.dp
private val IslandMinTop = 4.dp

/**
 * Where the island sits against the status bar. The status bar swallows every touch inside it, so the island must not
 * go up into it far: a small NEGATIVE value tucks the very top of the pill a few dp under the bar, which puts it
 * right beside the front camera, while the rest of the pill (cover, line, bars, buttons) stays fully tappable.
 * The overlay uses the same value. Raise it (towards 0 or positive) to move the island down; never below about -4.dp.
 */
val IslandBelowBarGap = (-3).dp

// what the normal pill spends on things that are not the lyric: padding, cover, gaps, bars
private val CollapsedChrome = 78.dp

// the line in the minimised pill is always this size: it never grows or shrinks from one line to the next (a long line scrolls instead)
private const val PillLyricSp = 12f

// a real Dynamic Island rests at about 126 x 37; a notch is a little wider
private val CompactIslandWidth = 148.dp
private val CompactNotchWidth = 172.dp
// the notch hangs from the very top edge: it is the status bar tall plus this lip, which is the part below the bar that can be tapped
private val NotchLip = 10.dp
private val NotchRadius = 16.dp
/** the camera gap when the phone does not tell us its cutout */
val DefaultCameraGap = 28.dp

/** The overlay window's height while minimised, for either shape. */
fun islandClosedWindowHeight(shape: DynamicIslandShape, statusBar: Dp): Dp =
    (if (shape == DynamicIslandShape.NOTCH) statusBar + NotchLip else CollapsedHeight) + 4.dp

/** The overlay window's height while the card is open, for either shape (the notch card also covers the status bar). */
fun islandExpandedWindowHeight(shape: DynamicIslandShape, statusBar: Dp): Dp =
    (if (shape == DynamicIslandShape.NOTCH) ExpandedHeight + statusBar else ExpandedHeight) + 8.dp

/** The switches from Settings > Dynamic Island that change what the pill shows. */
data class IslandOptions(
    val pillLyrics: Boolean = true,
    val cardLyrics: Boolean = true,
    val wordLit: Boolean = true,
    val swipe: Boolean = true,
)

// 0..1 eased, so a face fades in and out without a visible start or stop
private fun smoothStep(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private data class Parsed(val lines: List<LyricsEntry>, val synced: Boolean)
private data class LyricFit(val sp: Float, val width: Dp, val marquee: Boolean)
private data class CameraFit(val left: String, val right: String, val sp: Float, val half: Dp, val marquee: Boolean)

/**
 * The pill itself, shared by the in-app island and the overlay window: it knows nothing about where it is drawn.
 * [fullWidth] is the screen's width; open, the pill takes all of it but a small margin.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DynamicIslandPill(
    metadata: com.danrey.lalasi.models.MediaMetadata,
    isPlaying: Boolean,
    lyricsRaw: String?,
    offsetMs: Int,
    positionNow: () -> Long,
    durationMs: () -> Long,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    style: DynamicIslandStyle,
    fullWidth: Dp,
    options: IslandOptions = IslandOptions(),
    /** ISLAND floats as a pill, NOTCH hangs from the top edge of the screen */
    shapeStyle: DynamicIslandShape = DynamicIslandShape.ISLAND,
    /** the status bar's height; the notch is this tall (plus a lip) and its open card starts below it */
    statusBar: Dp = 24.dp,
    /** the gap left for the front camera; 0 turns camera mode off and the line stays in one piece */
    cameraGap: Dp = 0.dp,
    modifier: Modifier = Modifier,
    /** when set, the caller owns open / closed (the overlay needs it to close on a tap outside the pill) */
    expandedState: Boolean? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    /** when set, a tap on the artwork of the open player card opens the app (and closes the card) */
    onOpenApp: (() -> Unit)? = null,
) {
    var innerExpanded by remember { mutableStateOf(false) }
    val expanded = expandedState ?: innerExpanded
    fun setExpanded(value: Boolean) {
        innerExpanded = value
        onExpandedChange?.invoke(value)
    }
    // a new song closes the island again, like the phone's
    LaunchedEffect(metadata.id) { setExpanded(false) }

    val parsed = remember(lyricsRaw) {
        val raw = lyricsRaw
        if (raw.isNullOrBlank() || raw == LYRICS_NOT_FOUND) Parsed(emptyList(), false) else {
            val timed = LyricsUtils.parseLyricsCached(raw).filter { it.text.isNotBlank() }
            if (timed.isNotEmpty()) Parsed(timed, true)
            else Parsed(LyricsUtils.parsePlainLyrics(raw).filter { it.text.isNotBlank() }, false)
        }
    }

    // where the song is: followed in both sizes, so the line keeps moving in the small pill too
    var rawPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    LaunchedEffect(metadata.id, isPlaying, expanded) {
        do {
            rawPosition = positionNow().coerceAtLeast(0L)
            duration = durationMs().coerceAtLeast(0L)
            delay(if (expanded) 80 else 140)
        } while (isPlaying || expanded)
    }
    val position = rawPosition + offsetMs
    val current = if (parsed.synced) parsed.lines.indexOfLast { it.time <= position } else -1

    // --- the normal pill: as wide as its line needs, never cut ---
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val cardW = islandCardWidth(fullWidth)
    val maxCollapsed = cardW
    // the resting pill keeps ONE width while a song has lyrics (it never follows the line), so a new line can never make it jump
    val restW = (maxCollapsed * 0.80f).coerceAtLeast(CompactIslandWidth + 40.dp).coerceAtMost(maxCollapsed)
    val textRoom = restW - CollapsedChrome
    fun widthOf(text: String, sp: Float): Dp = with(density) {
        measurer.measure(
            text = text,
            style = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = sp.sp),
            maxLines = 1,
            softWrap = false,
        ).size.width.toDp()
    }
    fun fitFor(text: String): LyricFit {
        val w = widthOf(text, PillLyricSp)
        return if (w <= textRoom) LyricFit(PillLyricSp, w, false) else LyricFit(PillLyricSp, textRoom, true)
    }
    // the shape's own sizes
    val notch = shapeStyle == DynamicIslandShape.NOTCH
    val cameraOn = cameraGap > 0.dp
    val closedH = if (notch) statusBar + NotchLip else CollapsedHeight
    val topInset = if (notch) statusBar else 0.dp
    val openH = ExpandedHeight + topInset
    val closedRadius = if (notch) minOf(NotchRadius, closedH / 2) else closedH / 2
    // the face (cover, line, bars) sits in the band of the camera: the whole pill for the island, the status bar for the notch
    val faceH = if (notch) statusBar else CollapsedHeight
    val compactMin = maxOf(
        if (notch) CompactNotchWidth else CompactIslandWidth,
        if (cameraOn) CollapsedChrome + cameraGap + 16.dp else 0.dp,
    )

    // the title is not shown in the minimised pill any more: only the line being sung, or nothing
    val title = metadata.title
    val collapsedText = (if (options.pillLyrics && current >= 0) parsed.lines[current].text else "").replace('\n', ' ').trim()

    // camera mode: both halves get the same room so the camera stays at the pill's centre
    val halfRoom = (restW - CollapsedChrome - cameraGap) / 2
    fun splitAround(text: String): Pair<String, String> {
        val words = text.split(' ').filter { it.isNotEmpty() }
        if (words.size >= 2) {
            var best = 1
            var bestDiff = Float.MAX_VALUE
            for (k in 1 until words.size) {
                val l = widthOf(words.subList(0, k).joinToString(" "), PillLyricSp).value
                val r = widthOf(words.subList(k, words.size).joinToString(" "), PillLyricSp).value
                val d = abs(l - r)
                if (d < bestDiff) { bestDiff = d; best = k }
            }
            return words.subList(0, best).joinToString(" ") to words.subList(best, words.size).joinToString(" ")
        }
        val one = words.firstOrNull() ?: return "" to ""
        if (widthOf(one, PillLyricSp) <= halfRoom) return one to ""
        var mid = one.length / 2
        if (mid in 1 until one.length && Character.isLowSurrogate(one[mid])) mid++
        return one.substring(0, mid) to one.substring(mid)
    }
    fun cameraFitFor(text: String): CameraFit {
        if (text.isEmpty()) return CameraFit("", "", PillLyricSp, 0.dp, false)
        val (l, r) = splitAround(text)
        val w = maxOf(widthOf(l, PillLyricSp), widthOf(r, PillLyricSp))
        return if (w <= halfRoom) CameraFit(l, r, PillLyricSp, w, false) else CameraFit(l, r, PillLyricSp, halfRoom, true)
    }
    val hasPillLyrics = options.pillLyrics && parsed.lines.isNotEmpty()
    val collapsedTarget = (if (hasPillLyrics) restW else compactMin).coerceIn(minOf(compactMin, maxCollapsed), maxCollapsed)
    val collapsedW by animateDpAsState(collapsedTarget, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow), label = "collapsedW")

    // ONE progress value drives width, height and corner radius together, so the shape is always one clean
    // rounded pill / rounded card: never a circle next to a box. The radius can never pass half the height.
    // Elastic and nothing else: opening and minimising are the same single spring, a touch firmer on the way in, so the pill
    // stretches and settles like rubber. No slide, no scale of the contents, no jump of the window.
    val progress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = if (expanded) {
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 420f)
        } else {
            // minimising settles without rebound: nothing swings back and forth at the end
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 460f)
        },
        label = "island",
    )
    val p = progress
    // the rubber may pass either end a little, never enough to leave the window or squash the pill flat
    val shapeP = p.coerceIn(0f, 1f)
    val width = collapsedW + (cardW - collapsedW) * shapeP
    val height = closedH + (openH - closedH) * shapeP
    val radius = minOf(closedRadius + (ExpandedRadius - closedRadius) * shapeP.coerceIn(0f, 1f), height / 2)
    // the notch is flat on top (flush with the screen edge) and round only at the bottom
    val shape = if (notch) RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = radius, bottomEnd = radius) else RoundedCornerShape(radius)

    Box(
        modifier
            .width(width)
            .height(height)
            .islandSurface(style, shape, radius, notch)
            // swipe down opens, swipe up minimises
            .pointerInput(expanded, options.swipe) {
                if (!options.swipe) return@pointerInput
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { total = 0f },
                    onDragCancel = { total = 0f },
                ) { _, dy ->
                    total += dy
                    if (expanded && total < -24.dp.toPx()) { setExpanded(false); total = 0f }
                    else if (!expanded && total > 16.dp.toPx()) { setExpanded(true); total = 0f }
                }
            }
            .clickable(enabled = !expanded, interactionSource = remember { MutableInteractionSource() }, indication = null) { setExpanded(true) },
    ) {
        if (style == DynamicIslandStyle.LIQUID_GLASS) LiquidGlassBackdrop(metadata.thumbnailUrl, Modifier.fillMaxSize())
        // smooth cross-fade between the two faces; each keeps its own final size so nothing reflows while the shape moves
        val openAlpha = smoothStep((p - 0.35f) / 0.5f)
        val closedAlpha = smoothStep((0.5f - p) / 0.4f)
        if (p > 0.25f) {
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .requiredWidth(cardW)
                    .requiredHeight(openH)
                    // only a cross-fade: the card keeps its full size and is simply uncovered / covered by the elastic pill
                    .graphicsLayer { alpha = openAlpha },
            ) {
                // the notch's card starts under the status bar
                if (topInset > 0.dp) Spacer(Modifier.height(topInset))
                // the handle: tap it to minimise
                Box(
                    Modifier.fillMaxWidth().height(22.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { setExpanded(false) },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.width(36.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.35f)))
                }
                Row(Modifier.weight(1f).fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
                    PlayerColumn(
                        cover = metadata.thumbnailUrl,
                        title = title,
                        artist = metadata.artists.joinToString { it.name },
                        playing = isPlaying,
                        positionMs = rawPosition,
                        durationMs = duration,
                        onSeek = onSeek,
                        onPrevious = onPrevious,
                        onPlayPause = onPlayPause,
                        onNext = onNext,
                        onCoverClick = onOpenApp?.let { open -> { setExpanded(false); open() } },
                        modifier = (if (options.cardLyrics) Modifier.weight(0.46f) else Modifier.weight(1f)).fillMaxHeight(),
                    )
                    if (options.cardLyrics) {
                        Spacer(Modifier.width(14.dp))
                        LyricsPanel(parsed, current, position, options.wordLit, Modifier.weight(0.54f).fillMaxHeight())
                    }
                }
            }
        }
        if (p < 0.6f) {
            Row(
                Modifier
                    .align(if (notch) Alignment.TopCenter else Alignment.Center)
                    .requiredSize(collapsedW, faceH)
                    .graphicsLayer { alpha = closedAlpha }
                    // camera mode keeps both sides equal (38 dp each) so the gap in the middle sits exactly on the camera
                    .padding(start = 6.dp, end = if (cameraOn) 10.5.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Cover(metadata.thumbnailUrl, 24.dp)
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f).clipToBounds(), contentAlignment = Alignment.Center) {
                    AnimatedContent(
                        targetState = collapsedText,
                        transitionSpec = {
                            fadeIn(tween(240, easing = FastOutSlowInEasing)) togetherWith fadeOut(tween(160))
                        },
                        label = "collapsedLine",
                    ) { line ->
                        if (cameraOn) {
                            // left half | camera | right half
                            val f = remember(line, cameraGap, restW) { cameraFitFor(line) }
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.weight(1f).clipToBounds(), contentAlignment = Alignment.CenterEnd) {
                                    CameraHalf(f.left, f.sp, f.marquee, TextAlign.End)
                                }
                                Spacer(Modifier.width(cameraGap))
                                Box(Modifier.weight(1f).clipToBounds(), contentAlignment = Alignment.CenterStart) {
                                    CameraHalf(f.right, f.sp, f.marquee, TextAlign.Start)
                                }
                            }
                        } else {
                            val fit = fitFor(line)
                            Text(
                                text = line,
                                color = Color.White,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = fit.sp.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                                textAlign = TextAlign.Center,
                                modifier = if (fit.marquee) Modifier.fillMaxWidth().basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 300, velocity = 30.dp)
                                else Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Bars(isPlaying)
            }
        }
    }
}

// one side of the line in camera mode; a half that is still too long at the smallest size scrolls instead of being cut
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CameraHalf(text: String, sp: Float, marquee: Boolean, align: TextAlign) {
    Text(
        text = text,
        color = Color.White,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = sp.sp,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        textAlign = align,
        modifier = if (marquee) Modifier.fillMaxWidth().basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 300, velocity = 30.dp)
        else Modifier,
    )
}

// the three looks. Default is solid black, blue is solid blue; liquid glass is a dark glass base, the blurred artwork on top
// of it (LiquidGlassBackdrop) and the nav bar's own rim over all of it.
@Composable
private fun Modifier.islandSurface(style: DynamicIslandStyle, shape: androidx.compose.ui.graphics.Shape, radius: androidx.compose.ui.unit.Dp, notch: Boolean = false): Modifier = when (style) {
    DynamicIslandStyle.DEFAULT -> this.clip(shape).background(Color.Black)
    DynamicIslandStyle.BLUE -> this.clip(shape).background(IslandBlue)
    DynamicIslandStyle.LIQUID_GLASS -> {
        // The glass is the material only: a flat dark base here, the blurred artwork in LiquidGlassBackdrop on top of it, and
        // the very rim the nav bar wears (the white edge light plus the rainbow film) drawn over everything. No gradient sheen.
        val bubble = rememberIridescentBubble()
        val phase = iridescencePhaseState()
        this
            .clip(shape)
            .background(GlassChromeColor)
            .drawWithContent {
                drawContent()
                val r = radius.toPx()
                if (notch) {
                    // flat top: the outline is drawn taller than the notch so its rounded top corners fall above the edge, outside the clip
                    val tall = Size(size.width, size.height + r)
                    drawChromeOutline(Offset(0f, -r), tall, r)
                    drawIridescentRim(bubble, Offset(0f, -r), tall, r, phase.value, 0.34f)
                } else {
                    drawChromeOutline(Offset.Zero, size, r)
                    drawIridescentRim(bubble, Offset.Zero, size, r, phase.value, 0.34f)
                }
            }
    }
}

/**
 * Behind the liquid glass island: the song's artwork, only a little blurred (so it still reads as the cover's colours),
 * under one flat dark tint that keeps the white text readable. Without an artwork it falls back to a few soft colour blobs.
 */
@Composable
private fun LiquidGlassBackdrop(cover: String?, modifier: Modifier = Modifier) {
    Box(modifier) {
        if (cover != null) {
            AsyncImage(
                model = cover,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    // a little bigger than the island, so the blur has no pale edge
                    .graphicsLayer { scaleX = 1.25f; scaleY = 1.25f }
                    .blur(16.dp, BlurredEdgeTreatment.Rectangle),
            )
        } else {
            Box(Modifier.fillMaxSize().blur(16.dp, BlurredEdgeTreatment.Rectangle)) {
                Box(Modifier.fillMaxSize().background(Color(0xFF45557A)))
                Box(Modifier.align(Alignment.TopStart).offset(10.dp, (-10).dp).size(70.dp).clip(CircleShape).background(Color(0xFFB467C9)))
                Box(Modifier.align(Alignment.BottomEnd).offset((-10).dp, 14.dp).size(90.dp).clip(CircleShape).background(Color(0xFF3F8CF0)))
            }
        }
        Box(Modifier.fillMaxSize().background(GlassChromeColor.copy(alpha = 0.40f)))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlayerColumn(
    cover: String?,
    title: String,
    artist: String,
    playing: Boolean,
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onCoverClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Cover(cover, 50.dp, corner = 12.dp, onClick = onCoverClick)
            Bars(playing)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            title, color = Color.White, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.fillMaxWidth().basicMarquee(iterations = Int.MAX_VALUE),
        )
        Text(
            artist, color = Color.White.copy(alpha = 0.65f), fontFamily = InterFontFamily, fontSize = 12.sp,
            maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.fillMaxWidth().basicMarquee(iterations = Int.MAX_VALUE),
        )
        Spacer(Modifier.weight(1f))
        SeekBar(positionMs, durationMs, onSeek)
        Spacer(Modifier.height(2.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
            ControlButton(R.drawable.skip_previous, 26.dp, onPrevious)
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(Color.White)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onPlayPause),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(if (playing) R.drawable.pause else R.drawable.play), contentDescription = null, tint = Color.Black, modifier = Modifier.size(22.dp))
            }
            ControlButton(R.drawable.skip_next, 26.dp, onNext)
        }
    }
}

@Composable
private fun ControlButton(icon: Int, size: Dp, onClick: () -> Unit) {
    Box(
        Modifier.size(38.dp).clip(CircleShape).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(size))
    }
}

private fun formatTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

@Composable
private fun SeekBar(positionMs: Long, durationMs: Long, onSeek: (Long) -> Unit) {
    var drag by remember { mutableStateOf<Float?>(null) }
    val frac = drag ?: if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val shown = if (durationMs > 0) (frac * durationMs).toLong() else 0L
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier.fillMaxWidth().height(16.dp)
                .pointerInput(durationMs) {
                    detectTapGestures { o -> if (durationMs > 0) onSeek((o.x / size.width * durationMs).toLong().coerceIn(0L, durationMs)) }
                }
                .pointerInput(durationMs) {
                    detectHorizontalDragGestures(
                        onDragStart = { o -> drag = (o.x / size.width).coerceIn(0f, 1f) },
                        onDragEnd = {
                            drag?.let { if (durationMs > 0) onSeek((it * durationMs).toLong()) }
                            drag = null
                        },
                        onDragCancel = { drag = null },
                    ) { change, _ ->
                        change.consume()
                        drag = (change.position.x / size.width).coerceIn(0f, 1f)
                    }
                }
                .drawBehind {
                    val h = 4.dp.toPx()
                    val y = (size.height - h) / 2
                    drawRoundRect(Color.White.copy(alpha = 0.25f), Offset(0f, y), Size(size.width, h), CornerRadius(h / 2))
                    drawRoundRect(Color.White, Offset(0f, y), Size((size.width * frac).coerceAtLeast(h), h), CornerRadius(h / 2))
                },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(shown), color = Color.White.copy(alpha = 0.6f), fontFamily = InterFontFamily, fontSize = 10.sp, maxLines = 1)
            Text("-" + formatTime((durationMs - shown).coerceAtLeast(0L)), color = Color.White.copy(alpha = 0.6f), fontFamily = InterFontFamily, fontSize = 10.sp, maxLines = 1)
        }
    }
}

// the lyrics next to the player: every line wrapped in full (nothing is cut), the one being sung is big and
// bright and lit word by word, the others dim and smaller, and the list glides to keep the line in view
@Composable
private fun LyricsPanel(parsed: Parsed, current: Int, position: Long, wordLit: Boolean, modifier: Modifier = Modifier) {
    if (parsed.lines.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.CenterStart) {
            Text(
                stringResource(R.string.island_no_lyrics), color = Color.White.copy(alpha = 0.6f), fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp,
            )
        }
        return
    }
    val state = rememberLazyListState()
    LaunchedEffect(current, parsed.lines.size) {
        if (parsed.synced && current >= 0) {
            val viewport = state.layoutInfo.viewportSize.height
            state.animateScrollToItem(current, -(viewport * 0.22f).toInt())
        }
    }
    LazyColumn(
        state = state,
        modifier = modifier.fadeEdges(),
        contentPadding = PaddingValues(top = 26.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(parsed.lines) { i, line ->
            val active = parsed.synced && i == current
            val target = when {
                !parsed.synced -> 0.85f
                active -> 1f
                abs(i - current) == 1 -> 0.45f
                else -> 0.3f
            }
            val alpha by animateFloatAsState(target, tween(320), label = "lineAlpha")
            val scale by animateFloatAsState(if (active || !parsed.synced) 1f else 0.9f, tween(320), label = "lineScale")
            val words = line.words
            val lit = active && wordLit && !words.isNullOrEmpty()
            val text = if (lit && words != null) litWords(line.text, words, position) else AnnotatedString(line.text)
            Text(
                text = text,
                color = Color.White.copy(alpha = if (lit) 0.42f else alpha),
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                modifier = Modifier.fillMaxWidth().graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0f, 0.5f)
                },
            )
        }
    }
}

// the line with each word already sung at full brightness and the one being sung fading in
private fun litWords(text: String, words: List<com.danrey.lalasi.lyrics.WordTimestamp>, positionMs: Long): AnnotatedString {
    val spans = ArrayList<AnnotatedString.Range<SpanStyle>>()
    var cursor = 0
    for (w in words) {
        val piece = w.text.trim()
        if (piece.isEmpty()) continue
        val at = text.indexOf(piece, cursor)
        if (at < 0) continue
        val start = w.startTime * 1000
        val end = w.endTime * 1000
        val f = ((positionMs - start) / (end - start).coerceAtLeast(1.0)).coerceIn(0.0, 1.0).toFloat()
        spans += AnnotatedString.Range(SpanStyle(color = Color.White.copy(alpha = 0.42f + 0.58f * f)), at, at + piece.length)
        cursor = at + piece.length
    }
    return AnnotatedString(text, spanStyles = spans)
}

// soft fade at the top and bottom of the lyrics so lines glide in and out instead of being cut by a hard edge
private fun Modifier.fadeEdges(): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(
            Brush.verticalGradient(0f to Color.Transparent, 0.14f to Color.Black, 0.86f to Color.Black, 1f to Color.Transparent),
            blendMode = BlendMode.DstIn,
        )
    }

@Composable
private fun Cover(url: String?, size: androidx.compose.ui.unit.Dp, corner: androidx.compose.ui.unit.Dp? = null, onClick: (() -> Unit)? = null) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(size).clip(if (corner == null) CircleShape else RoundedCornerShape(corner)).background(Color.White.copy(alpha = 0.12f))
            .then(if (onClick != null) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick) else Modifier),
    )
}

// four bars that dance while the song plays and rest low when it is paused
@Composable
private fun Bars(playing: Boolean, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "bars")
    val heights = listOf(0.55f to 380, 1f to 520, 0.7f to 440, 0.9f to 600).mapIndexed { i, (peak, ms) ->
        infinite.animateFloat(
            initialValue = 0.25f,
            targetValue = peak,
            animationSpec = infiniteRepeatable(tween(ms, easing = LinearEasing), RepeatMode.Reverse),
            label = "bar$i",
        )
    }
    Row(modifier.height(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.5.dp)) {
        heights.forEach { h ->
            val f = if (playing) h.value else 0.25f
            Box(Modifier.width(3.dp).height(16.dp * f).clip(CircleShape).background(Color.White))
        }
    }
}


/**
 * What the island looks like, for the Dynamic Island page in Settings: the small pill and the open card, in the chosen
 * look. Nothing here plays or moves; the real island is the overlay outside the app.
 */
@Composable
fun DynamicIslandPreview(
    style: DynamicIslandStyle,
    modifier: Modifier = Modifier,
    coverUrl: String? = null,
    shapeStyle: DynamicIslandShape = DynamicIslandShape.ISLAND,
) {
    val notch = shapeStyle == DynamicIslandShape.NOTCH
    val smallH = if (notch) 30.dp else CollapsedHeight
    val smallShape = if (notch) RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = NotchRadius, bottomEnd = NotchRadius) else RoundedCornerShape(smallH / 2)
    val smallRadius = if (notch) NotchRadius else smallH / 2
    val bigShape = if (notch) RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = ExpandedRadius, bottomEnd = ExpandedRadius) else RoundedCornerShape(ExpandedRadius)
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF3B4F73), Color(0xFF121826))))
            .padding(horizontal = 14.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // the small island: cover, the camera in the middle, bars. No title.
        Box(Modifier.width(if (notch) CompactNotchWidth else CompactIslandWidth).height(smallH).islandSurface(style, smallShape, smallRadius, notch)) {
            if (style == DynamicIslandStyle.LIQUID_GLASS) LiquidGlassBackdrop(coverUrl, Modifier.fillMaxSize())
            Row(Modifier.fillMaxSize().padding(start = 6.dp, end = 10.5.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(24.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)))
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(9.dp).clip(CircleShape).background(Color.Black).border(0.5.dp, Color.White.copy(alpha = 0.3f), CircleShape))
                }
                Bars(true)
            }
        }
        // the open card
        Box(Modifier.fillMaxWidth().height(ExpandedHeight).islandSurface(style, bigShape, ExpandedRadius, notch)) {
            if (style == DynamicIslandStyle.LIQUID_GLASS) LiquidGlassBackdrop(coverUrl, Modifier.fillMaxSize())
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().height(22.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.width(36.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.35f)))
                }
                Column(Modifier.weight(1f).fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 14.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.18f)))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Song title", color = Color.White, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
                            Text("Artist", color = Color.White.copy(alpha = 0.65f), fontFamily = InterFontFamily, fontSize = 12.sp, maxLines = 1)
                        }
                        Bars(true)
                    }
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.fillMaxWidth().height(16.dp).drawBehind {
                        val h = 4.dp.toPx()
                        val y = (size.height - h) / 2
                        drawRoundRect(Color.White.copy(alpha = 0.25f), Offset(0f, y), Size(size.width, h), CornerRadius(h / 2))
                        drawRoundRect(Color.White, Offset(0f, y), Size(size.width * 0.38f, h), CornerRadius(h / 2))
                    })
                    Spacer(Modifier.height(2.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                        ControlButton(R.drawable.skip_previous, 26.dp) {}
                        Box(Modifier.size(38.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                            Icon(painterResource(R.drawable.pause), contentDescription = null, tint = Color.Black, modifier = Modifier.size(22.dp))
                        }
                        ControlButton(R.drawable.skip_next, 26.dp) {}
                    }
                }
            }
        }
    }
}


// ---------- where the island may sit: anywhere on the screen, never off it ----------

/** The real screen size in px (not the window's), the same for the overlay and for Settings. */
fun islandScreenPx(context: android.content.Context): Pair<Int, Int> {
    val wm = context.getSystemService(android.content.Context.WINDOW_SERVICE) as android.view.WindowManager
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        val b = wm.maximumWindowMetrics.bounds
        b.width() to b.height()
    } else {
        val dm = android.util.DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(dm)
        dm.widthPixels to dm.heightPixels
    }
}

/** The status bar's height in px. */
fun islandStatusBarPx(context: android.content.Context): Int {
    val density = context.resources.displayMetrics.density
    val id = context.resources.getIdentifier("status_bar_height", "dimen", "android")
    return if (id > 0) context.resources.getDimensionPixelSize(id) else (24 * density).toInt()
}

/**
 * The offsets (dp) that keep the island on the screen. For the island the offset counts from the bottom of the status bar; for the
 * notch it counts from the top edge of the screen, with [DynamicIslandOffsetDefault] meaning flush with it (so the one stepper in
 * Settings reads the same for both). The bottom end leaves room for the open card to grow downwards without leaving the screen.
 */
fun islandOffsetRangeDp(context: android.content.Context, shape: DynamicIslandShape = DynamicIslandShape.ISLAND): IntRange {
    val density = context.resources.displayMetrics.density
    val bar = islandStatusBarPx(context) / density
    val screenH = islandScreenPx(context).second / density
    val expanded = islandExpandedWindowHeight(shape, bar.dp).value
    return if (shape == DynamicIslandShape.NOTCH) {
        val min = DynamicIslandOffsetDefault
        val max = kotlin.math.floor(screenH - expanded).toInt() + DynamicIslandOffsetDefault
        min..maxOf(min, max)
    } else {
        val min = kotlin.math.ceil(-bar).toInt()
        val max = kotlin.math.floor(screenH - bar - expanded).toInt()
        min..maxOf(min, max)
    }
}

/** Top of the overlay window in px for a shape and an offset, kept on the screen (the open card too). The overlay and Settings share it. */
fun islandWindowTopPx(context: android.content.Context, shape: DynamicIslandShape, offsetDp: Int): Int {
    val density = context.resources.displayMetrics.density
    val screenH = islandScreenPx(context).second
    val bar = islandStatusBarPx(context)
    val expandedPx = (islandExpandedWindowHeight(shape, (bar / density).dp).value * density).toInt()
    val lowest = (screenH - expandedPx).coerceAtLeast(0)
    val y = if (shape == DynamicIslandShape.NOTCH) {
        ((offsetDp - DynamicIslandOffsetDefault).coerceAtLeast(0) * density).toInt()
    } else {
        bar + (offsetDp * density).toInt()
    }
    return y.coerceIn(0, lowest)
}

/**
 * The width of the gap left for the front camera. A manual value (>= 0, set in Settings) wins; otherwise it is read from the display
 * cutout (Android 11+), plus a little air, and falls back to [DefaultCameraGap] when the phone does not report one.
 */
fun islandCameraGapDp(context: android.content.Context, manualDp: Int): Dp {
    if (manualDp >= 0) return manualDp.dp
    val detected = runCatching {
        val density = context.resources.displayMetrics.density
        val screenW = islandScreenPx(context).first
        val rects: List<android.graphics.Rect>? = when {
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S ->
                (context.getSystemService(android.content.Context.DISPLAY_SERVICE) as android.hardware.display.DisplayManager)
                    .getDisplay(android.view.Display.DEFAULT_DISPLAY)?.cutout?.boundingRects
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R ->
                (context.getSystemService(android.content.Context.WINDOW_SERVICE) as android.view.WindowManager)
                    .currentWindowMetrics.windowInsets.displayCutout?.boundingRects
            else -> null
        }
        // the cutout that touches the top edge near the middle of the screen is the camera
        rects?.filter { it.top <= 2 && it.exactCenterX() in (screenW * 0.3f)..(screenW * 0.7f) }
            ?.maxOfOrNull { it.width() }
            ?.let { it / density }
    }.getOrNull()
    return if (detected != null && detected > 4f) (detected + 6f).coerceIn(14f, 90f).dp else DefaultCameraGap
}

/**
 * A little phone in Settings that shows where the island sits (it is drawn outside the app, so this is the only way to see it
 * there): the status bar, the camera and the island at the chosen offset, in the chosen look. The red part, when there is one,
 * is under the status bar and cannot be tapped.
 */
@Composable
fun DynamicIslandPositionPreview(
    style: DynamicIslandStyle,
    offsetDp: Int,
    statusBarDp: Float,
    screenWidthDp: Float,
    screenHeightDp: Float,
    coverUrl: String?,
    modifier: Modifier = Modifier,
    shapeStyle: DynamicIslandShape = DynamicIslandShape.ISLAND,
    cameraGapDp: Float = 0f,
) {
    val notch = shapeStyle == DynamicIslandShape.NOTCH
    val mockWidth = 150.dp
    val scale = mockWidth.value / screenWidthDp.coerceAtLeast(1f)
    val mockHeight = (screenHeightDp * scale).dp
    val frame = RoundedCornerShape(22.dp)
    val barH = (statusBarDp * scale).dp
    val pillWdp = if (notch) CompactNotchWidth.value else CompactIslandWidth.value
    val pillHdp = if (notch) statusBarDp + NotchLip.value else CollapsedHeight.value
    val pillW = (pillWdp * scale).dp
    val pillH = (pillHdp * scale).dp
    val topDp = if (notch) (offsetDp - DynamicIslandOffsetDefault).coerceAtLeast(0).toFloat() else statusBarDp + offsetDp
    val pillTop = (topDp * scale).dp
    val pillShape = if (notch) RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = (NotchRadius.value * scale).dp, bottomEnd = (NotchRadius.value * scale).dp)
    else RoundedCornerShape(pillH / 2)
    Box(
        modifier.width(mockWidth).height(mockHeight).clip(frame).background(Color(0xFF0F131C)).border(1.5.dp, Color.White.copy(alpha = 0.35f), frame),
    ) {
        // the status bar and the camera
        Box(Modifier.fillMaxWidth().height(barH).background(Color.White.copy(alpha = 0.08f)))
        Box(
            Modifier.align(Alignment.TopCenter).padding(top = (barH / 2 - 3.5.dp).coerceAtLeast(0.dp)).size(7.dp)
                .clip(CircleShape).background(Color.Black).border(0.5.dp, Color.White.copy(alpha = 0.3f), CircleShape),
        )
        // the island or the notch
        Box(
            Modifier.align(Alignment.TopCenter).offset(y = pillTop).width(pillW).height(pillH)
                .islandSurface(style, pillShape, if (notch) (NotchRadius.value * scale).dp else pillH / 2, notch),
        ) {
            if (style == DynamicIslandStyle.LIQUID_GLASS) LiquidGlassBackdrop(coverUrl, Modifier.fillMaxSize())
            // the face sits in the band of the camera: the notch's is the status bar's
            Row(
                Modifier.fillMaxWidth().height(if (notch) barH else pillH).padding(horizontal = pillH * 0.22f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size((pillHdp.coerceAtMost(if (notch) statusBarDp else pillHdp) * 0.66f * scale).dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)))
                // the gap for the camera: the line is split around it
                Spacer(Modifier.weight(1f))
                if (cameraGapDp > 0f) Spacer(Modifier.width((cameraGapDp * scale).dp))
                Spacer(Modifier.weight(1f))
                Box(Modifier.width(3.dp).height(5.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.7f)))
            }
            // the part under the status bar: it takes the touches, so it cannot be tapped
            val untappableDp = if (notch) minOf(statusBarDp - topDp, pillHdp).coerceAtLeast(0f)
            else if (offsetDp < 0) minOf(-offsetDp, CollapsedHeight.value.toInt()).toFloat() else 0f
            if (untappableDp > 0f) {
                Box(
                    Modifier.align(Alignment.TopCenter).fillMaxWidth().height((untappableDp * scale).dp)
                        .background(Color(0xFFFF3B30).copy(alpha = 0.55f)),
                )
            }
        }
    }
}
