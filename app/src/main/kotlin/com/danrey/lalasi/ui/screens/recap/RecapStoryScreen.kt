package com.danrey.lalasi.ui.screens.recap

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.danrey.lalasi.ui.theme.InterFontFamily
import com.danrey.lalasi.LocalDatabase
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxHeight as fillHeight
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow as Overflow
import com.danrey.lalasi.ui.component.IslandGlow
import com.danrey.lalasi.ui.component.rememberIslandGlow
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

// Recap as a story, the way Spotify's Wrapped plays: full-screen slides, bars on top that fill one by one,
// a tap on the right side goes on, a tap on the left side goes back, holding pauses. Static gradients and big type only,
// so nothing but the thin progress bars redraws while it plays.

private const val SlideMillis = 6500

private enum class StorySlide { Intro, Time, TopSong, Songs, TopArtist, Artists, Rhythm, Outro }

private val StoryInk = Color.White

@Composable
fun RecapStoryScreen(navController: NavController, kind: String, year: Int, month: Int) {
    val database = LocalDatabase.current
    val range = remember(kind, year, month) {
        if (kind == "year") StatsRange(StatsRange.Kind.Year, year) else StatsRange(StatsRange.Kind.Month, year, month)
    }
    var stats by remember { mutableStateOf<ListeningStats?>(null) }
    LaunchedEffect(range) { stats = RecapRepository.load(database, range) }
    val close: () -> Unit = { navController.navigateUp() }
    val s = stats
    androidx.compose.material3.ProvideTextStyle(androidx.compose.ui.text.TextStyle(fontFamily = InterFontFamily)) {
        Box(Modifier.fillMaxSize().background(Color(0xFF0B0B0D))) {
            when {
                s == null -> androidx.compose.material3.CircularProgressIndicator(
                    color = Color.White, strokeWidth = 3.dp, modifier = Modifier.align(Alignment.Center).size(32.dp),
                )
                s.isEmpty -> Column(
                    Modifier.fillMaxSize().statusBarsPadding().padding(28.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("Belum ada lagu diputar di ${range.label}.", color = StoryInk, fontSize = 32.sp, lineHeight = 36.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp)
                    Spacer(Modifier.height(10.dp))
                    Text("Putar beberapa lagu, lalu Recap-mu muncul di sini.", color = StoryInk.copy(alpha = 0.7f), fontSize = 16.sp)
                    Spacer(Modifier.height(28.dp))
                    DoneButton(close)
                }
                else -> StoryPlayer(s, range, close)
            }
        }
    }
}

@Composable
private fun StoryPlayer(s: ListeningStats, range: StatsRange, onClose: () -> Unit) {
    val slides = remember(s) {
        buildList {
            add(StorySlide.Intro); add(StorySlide.Time)
            if (s.topSongs.isNotEmpty()) { add(StorySlide.TopSong); add(StorySlide.Songs) }
            if (s.topArtists.isNotEmpty()) { add(StorySlide.TopArtist); add(StorySlide.Artists) }
            add(StorySlide.Rhythm); add(StorySlide.Outro)
        }
    }
    var page by rememberSaveable { mutableIntStateOf(0) }
    var held by remember { mutableStateOf(false) }
    val progress = remember(page) { Animatable(0f) }

    // one slide plays for a few seconds, then the next; holding the screen pauses it
    LaunchedEffect(page, held) {
        if (!held && page < slides.lastIndex) {
            val left = ((1f - progress.value) * SlideMillis).toInt().coerceAtLeast(1)
            progress.animateTo(1f, tween(left, easing = LinearEasing))
            page += 1
        } else if (page == slides.lastIndex) {
            progress.snapTo(1f)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(slides.size) {
                detectTapGestures(
                    onPress = {
                        held = true
                        tryAwaitRelease()
                        held = false
                    },
                    onLongPress = { },
                    onTap = { at ->
                        if (at.x < size.width * 0.33f) { if (page > 0) page -= 1 }
                        else if (page < slides.lastIndex) page += 1
                    },
                )
            },
    ) {
        AnimatedContent(
            targetState = page,
            transitionSpec = { fadeIn(tween(320)) togetherWith fadeOut(tween(240)) },
            label = "story",
            modifier = Modifier.fillMaxSize(),
        ) { index ->
            Box(Modifier.fillMaxSize()) {
                SlideBody(slides[index.coerceIn(0, slides.lastIndex)], s, range, onClose)
            }
        }

        // the bars on top: done slides full, the current one filling, the rest empty
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 10.dp),
        ) {
            slides.indices.forEach { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(2.5.dp)
                        .background(Color.White.copy(alpha = 0.26f))
                        .drawBehind {
                            val f = if (i < page) 1f else if (i == page) progress.value else 0f
                            if (f > 0f) drawRect(Color.White, size = Size(size.width * f, size.height))
                        },
                )
            }
        }
        // close
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 20.dp, end = 10.dp)
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onClose),
        ) {
            Canvas(Modifier.size(14.dp)) {
                val w = 2.dp.toPx()
                drawLine(Color.White, Offset(0f, 0f), Offset(size.width, size.height), w, StrokeCap.Round)
                drawLine(Color.White, Offset(size.width, 0f), Offset(0f, size.height), w, StrokeCap.Round)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// pieces
// ---------------------------------------------------------------------------------------------------------------------

private fun storyDark(c: Color, k: Float) = Color(c.red * k, c.green * k, c.blue * k, 1f)

/**
 * The slide's ground: the cover itself, enlarged and blurred (so the slide is made of the song's own colours, not of a
 * preset gradient), pushed down by a dark scrim for the text, with a slow zoom. No cover: the glow colours alone.
 */
@Composable
private fun StoryBackdrop(url: String?, glow: IslandGlow, scrim: Float = 0.58f) {
    val zoom = remember { Animatable(0f) }
    LaunchedEffect(Unit) { zoom.animateTo(1f, tween(SlideMillis + 1500, easing = LinearEasing)) }
    Box(Modifier.fillMaxSize().background(storyDark(glow.c, 0.22f))) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { val z = 1.35f + 0.12f * zoom.value; scaleX = z; scaleY = z }
                    .blur(44.dp, BlurredEdgeTreatment.Rectangle),
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = scrim * 0.75f),
                    0.5f to Color.Black.copy(alpha = scrim * 0.55f),
                    1f to Color.Black.copy(alpha = scrim + 0.2f),
                ),
            ),
        )
    }
}

/** an item that slides up and fades in a moment after the slide appears; the delay staggers the items */
@Composable
private fun StoryRise(order: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(order * 80L)
        a.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
    }
    Box(modifier.graphicsLayer { alpha = a.value; translationY = (1f - a.value) * 26.dp.toPx() }) { content() }
}

@Composable
private fun StoryKicker(text: String) {
    Text(text.uppercase(), color = StoryInk.copy(alpha = 0.72f), fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
}

@Composable
private fun StorySlideColumn(arrangement: Arrangement.Vertical = Arrangement.Center, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 26.dp, vertical = 58.dp),
        verticalArrangement = arrangement,
        content = content,
    )
}

@Composable
private fun StoryCover(url: String?, glow: IslandGlow, size: androidx.compose.ui.unit.Dp, shape: Shape = RoundedCornerShape(6.dp)) {
    Box(Modifier.size(size).clip(shape).background(glow.a.copy(alpha = 0.28f))) {
        if (url != null) AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun StoryStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, color = StoryInk, fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp, maxLines = 1)
        Text(label, color = StoryInk.copy(alpha = 0.62f), fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
    }
}

/** flat bars, one per value; the highest one lit in the cover's colour, the rest quiet */
@Composable
private fun StoryBars(values: List<Long>, highlight: Color, height: androidx.compose.ui.unit.Dp, gap: androidx.compose.ui.unit.Dp) {
    val peak = values.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    val top = values.indices.maxByOrNull { values[it] } ?: 0
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, tween(900, delayMillis = 250, easing = FastOutSlowInEasing)) }
    Canvas(Modifier.fillMaxWidth().height(height)) {
        val g = gap.toPx()
        val bw = (size.width - g * (values.size - 1)) / values.size
        values.forEachIndexed { i, v ->
            val bh = (size.height * (v.toFloat() / peak)).coerceAtLeast(2.dp.toPx()) * grow.value
            drawRect(
                if (i == top) highlight else Color.White.copy(alpha = 0.28f),
                topLeft = Offset(i * (bw + g), size.height - bh), size = Size(bw, bh),
            )
        }
    }
}

@Composable
private fun SlideBody(slide: StorySlide, s: ListeningStats, range: StatsRange, onClose: () -> Unit) {
    val topSong = s.topSongs.firstOrNull()
    val topArtist = s.topArtists.firstOrNull()
    val songGlow = rememberIslandGlow(topSong?.thumbnailUrl).value
    val artistGlow = rememberIslandGlow(topArtist?.thumbnailUrl ?: topSong?.thumbnailUrl).value
    val hours = formatHoursDecimal(s.totalMs)
    val isMonth = range.kind == StatsRange.Kind.Month

    when (slide) {
        StorySlide.Intro -> {
            // a wall of the period's covers, dimmed; the title sits at the bottom
            val covers = remember(s) { s.topSongs.mapNotNull { it.thumbnailUrl }.distinct().take(12) }
            Box(Modifier.fillMaxSize().background(storyDark(songGlow.c, 0.2f))) {
                Column(Modifier.fillMaxSize()) {
                    for (r in 0 until 4) {
                        Row(Modifier.weight(1f).fillMaxWidth()) {
                            for (c in 0 until 3) {
                                val url = covers.getOrNull(r * 3 + c)
                                Box(Modifier.weight(1f).fillHeight().background(songGlow.a.copy(alpha = 0.18f))) {
                                    if (url != null) AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }
                }
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.55f), 0.45f to Color.Black.copy(alpha = 0.68f), 1f to Color.Black.copy(alpha = 0.96f))))
            }
            StorySlideColumn(Arrangement.Bottom) {
                StoryRise(0) { StoryKicker("Recap musikmu") }
                Spacer(Modifier.height(8.dp))
                val big = if (isMonth) range.monthName.replaceFirstChar { it.uppercase() } else range.year.toString()
                StoryRise(1) {
                    Text(big, color = StoryInk, fontSize = if (big.length > 7) 60.sp else 96.sp, lineHeight = if (big.length > 7) 60.sp else 92.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-4).sp, maxLines = 1)
                }
                if (isMonth) StoryRise(2) { Text(range.year.toString(), color = StoryInk.copy(alpha = 0.7f), fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp) }
                Spacer(Modifier.height(22.dp))
                StoryRise(3) { Text("$hours jam  ·  ${s.songCount} lagu  ·  ${s.artistCount} artis", color = StoryInk.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
            }
        }

        StorySlide.Time -> {
            StoryBackdrop(topSong?.thumbnailUrl, songGlow)
            StorySlideColumn {
                StoryRise(0) { StoryKicker(if (isMonth) "Bulan ini kamu mendengarkan" else "Tahun ini kamu mendengarkan") }
                StoryRise(1) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(hours, color = StoryInk, fontSize = 116.sp, lineHeight = 108.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-6).sp, maxLines = 1)
                        Spacer(Modifier.width(8.dp))
                        Text("jam", color = StoryInk, fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))
                    }
                }
                Spacer(Modifier.height(26.dp))
                StoryRise(2) {
                    Column {
                        StoryBars(s.hourMs, songGlow.a, 76.dp, 3.dp)
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            listOf("00", "06", "12", "18", "23").forEach { Text(it, color = StoryInk.copy(alpha = 0.55f), fontSize = 11.sp, fontWeight = FontWeight.Medium) }
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
                StoryRise(3) {
                    Row(Modifier.fillMaxWidth()) {
                        StoryStat("${s.songCount}", "lagu", Modifier.weight(1f))
                        StoryStat("${s.artistCount}", "artis", Modifier.weight(1f))
                        StoryStat("${s.plays}", "putaran", Modifier.weight(1f))
                    }
                }
            }
        }

        StorySlide.TopSong -> if (topSong != null) {
            StoryBackdrop(topSong.thumbnailUrl, songGlow)
            StorySlideColumn {
                StoryRise(0) { StoryKicker("Lagu nomor 1") }
                Spacer(Modifier.height(18.dp))
                StoryRise(1) { StoryCover(topSong.thumbnailUrl, songGlow, 280.dp, RoundedCornerShape(8.dp)) }
                Spacer(Modifier.height(24.dp))
                StoryRise(2) {
                    Text(topSong.title, color = StoryInk, fontSize = 34.sp, lineHeight = 36.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.2).sp, maxLines = 2, overflow = Overflow.Ellipsis)
                }
                Spacer(Modifier.height(4.dp))
                StoryRise(3) { Text(topSong.artistName, color = StoryInk.copy(alpha = 0.85f), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = Overflow.Ellipsis) }
                Spacer(Modifier.height(18.dp))
                StoryRise(4) {
                    Row(Modifier.fillMaxWidth()) {
                        StoryStat("${topSong.plays}", "putaran", Modifier.weight(1f))
                        StoryStat(shortDuration(topSong.playTimeMs), "waktu dengar", Modifier.weight(1f))
                    }
                }
            }
        }

        StorySlide.Songs -> {
            StoryBackdrop(topSong?.thumbnailUrl, songGlow, scrim = 0.66f)
            StorySlideColumn {
                StoryRise(0) { StoryKicker("Top 5 lagu") }
                Spacer(Modifier.height(20.dp))
                s.topSongs.take(5).forEachIndexed { i, song ->
                    StoryRise(1 + i) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Text("${i + 1}", color = if (i == 0) songGlow.a else StoryInk.copy(alpha = 0.6f), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp, modifier = Modifier.width(34.dp))
                            StoryCover(song.thumbnailUrl, songGlow, 58.dp, RoundedCornerShape(5.dp))
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(song.title, color = StoryInk, fontSize = 17.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = Overflow.Ellipsis)
                                Text(song.artistName, color = StoryInk.copy(alpha = 0.65f), fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = Overflow.Ellipsis)
                            }
                            Spacer(Modifier.width(10.dp))
                            Text("${song.plays}x", color = StoryInk.copy(alpha = 0.78f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        StorySlide.TopArtist -> if (topArtist != null) {
            StoryBackdrop(topArtist.thumbnailUrl ?: topSong?.thumbnailUrl, artistGlow)
            StorySlideColumn {
                StoryRise(0) { StoryKicker("Artis nomor 1") }
                Spacer(Modifier.height(18.dp))
                StoryRise(1) { StoryCover(topArtist.thumbnailUrl, artistGlow, 250.dp, CircleShape) }
                Spacer(Modifier.height(26.dp))
                StoryRise(2) {
                    Text(topArtist.name, color = StoryInk, fontSize = 46.sp, lineHeight = 46.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-2).sp, maxLines = 2, overflow = Overflow.Ellipsis)
                }
                Spacer(Modifier.height(18.dp))
                StoryRise(3) {
                    Row(Modifier.fillMaxWidth()) {
                        StoryStat(shortDuration(topArtist.playTimeMs), "waktu dengar", Modifier.weight(1f))
                        StoryStat("${topArtist.plays}", "putaran", Modifier.weight(1f))
                    }
                }
                topArtist.topSong?.let { song ->
                    Spacer(Modifier.height(20.dp))
                    StoryRise(4) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StoryCover(song.thumbnailUrl, artistGlow, 44.dp, RoundedCornerShape(5.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Paling sering diputar", color = StoryInk.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Text(song.title, color = StoryInk, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = Overflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }

        StorySlide.Artists -> {
            StoryBackdrop(topArtist?.thumbnailUrl ?: topSong?.thumbnailUrl, artistGlow, scrim = 0.66f)
            StorySlideColumn {
                StoryRise(0) { StoryKicker("Top 5 artis") }
                Spacer(Modifier.height(20.dp))
                s.topArtists.take(5).forEachIndexed { i, artist ->
                    StoryRise(1 + i) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Text("${i + 1}", color = if (i == 0) artistGlow.a else StoryInk.copy(alpha = 0.6f), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp, modifier = Modifier.width(34.dp))
                            StoryCover(artist.thumbnailUrl, artistGlow, 58.dp, CircleShape)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(artist.name, color = StoryInk, fontSize = 17.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = Overflow.Ellipsis)
                                Text("${artist.plays} putaran", color = StoryInk.copy(alpha = 0.65f), fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium)
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(shortDuration(artist.playTimeMs), color = StoryInk.copy(alpha = 0.78f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        StorySlide.Rhythm -> {
            StoryBackdrop(topSong?.thumbnailUrl, songGlow, scrim = 0.64f)
            val day = s.weekdayMs.indices.maxByOrNull { s.weekdayMs[it] } ?: 0
            val hour = s.hourMs.indices.maxByOrNull { s.hourMs[it] } ?: 0
            StorySlideColumn {
                StoryRise(0) { StoryKicker("Hari favoritmu") }
                StoryRise(1) {
                    Text(
                        DayOfWeek.of(day + 1).getDisplayName(JavaTextStyle.FULL, Locale.getDefault()),
                        color = StoryInk, fontSize = 58.sp, lineHeight = 58.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-2.5).sp, maxLines = 1,
                    )
                }
                Spacer(Modifier.height(16.dp))
                StoryRise(2) {
                    Column {
                        StoryBars(s.weekdayMs, songGlow.a, 88.dp, 8.dp)
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth()) {
                            for (d in 1..7) {
                                Text(
                                    DayOfWeek.of(d).getDisplayName(JavaTextStyle.NARROW, Locale.getDefault()),
                                    color = StoryInk.copy(alpha = if (d == day + 1) 0.95f else 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(38.dp))
                StoryRise(3) { StoryKicker("Jam favoritmu") }
                StoryRise(4) {
                    Text("%02d.00".format(hour), color = StoryInk, fontSize = 76.sp, lineHeight = 76.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-3.5).sp, maxLines = 1)
                }
                StoryRise(5) { Text("total ${formatHoursDecimal(s.hourMs[hour])} jam di jam ini", color = StoryInk.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.Medium) }
            }
        }

        StorySlide.Outro -> {
            StoryBackdrop(topSong?.thumbnailUrl, songGlow, scrim = 0.7f)
            StorySlideColumn {
                // the top covers, overlapping in a row
                StoryRise(0) {
                    Row {
                        s.topSongs.take(5).forEachIndexed { i, song ->
                            Box(Modifier.offset(x = (-26 * i).dp)) { StoryCover(song.thumbnailUrl, songGlow, 96.dp, RoundedCornerShape(6.dp)) }
                        }
                    }
                }
                Spacer(Modifier.height(30.dp))
                StoryRise(1) { StoryKicker("Itu dia") }
                StoryRise(2) {
                    Text("Recap ${range.label}", color = StoryInk, fontSize = 48.sp, lineHeight = 48.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-2).sp)
                }
                StoryRise(2) { Text("kamu", color = StoryInk, fontSize = 48.sp, lineHeight = 48.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-2).sp) }
                Spacer(Modifier.height(18.dp))
                StoryRise(3) {
                    Row(Modifier.fillMaxWidth()) {
                        StoryStat(hours, "jam", Modifier.weight(1f))
                        StoryStat("${s.songCount}", "lagu", Modifier.weight(1f))
                        StoryStat("${s.artistCount}", "artis", Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(32.dp))
                StoryRise(4) { DoneButton(onClose) }
            }
        }
    }
}

@Composable
private fun DoneButton(onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.height(48.dp).clip(RoundedCornerShape(10.dp)).background(Color.White).clickable(onClick = onClick).padding(horizontal = 34.dp),
    ) {
        Text("Selesai", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}
