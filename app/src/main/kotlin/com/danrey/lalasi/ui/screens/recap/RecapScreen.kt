// RecapScreen.kt
//
// Recap lagu seperti Spotify Wrapped: halaman-halaman penuh layar yang berganti sendiri (ketuk kanan / kiri
// untuk maju / mundur), dengan batang kemajuan di atas. Datanya dari RecapRepository (tabel event), jadi
// tidak ada data baru yang disimpan. Dibuka dari kartu streak (Lihat recap) dan dari menu Profil.
//
// route "recap"      -> bulan ini (atau bulan lalu / tahun ini kalau bulan ini masih kosong)
// route "recap_year" -> tahun ini

package com.danrey.lalasi.ui.screens.recap

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.danrey.lalasi.LocalDatabase
import com.danrey.lalasi.ui.theme.InterFontFamily
import com.danrey.lalasi.ui.utils.stableSystemBars
import java.time.DayOfWeek
import java.time.LocalDate

private const val PageMillis = 6000

private class ClassicRecapPalette(val top: Color, val bottom: Color, val accent: Color)

// one palette per page, cycling
private val Palettes = listOf(
    ClassicRecapPalette(Color(0xFF5B21B6), Color(0xFF1E1B4B), Color(0xFFF0ABFC)),
    ClassicRecapPalette(Color(0xFF047857), Color(0xFF052E2B), Color(0xFFA7F3D0)),
    ClassicRecapPalette(Color(0xFFC2410C), Color(0xFF431407), Color(0xFFFDBA74)),
    ClassicRecapPalette(Color(0xFF1D4ED8), Color(0xFF0B1B4D), Color(0xFFBFDBFE)),
    ClassicRecapPalette(Color(0xFFBE185D), Color(0xFF3B0A24), Color(0xFFFBCFE8)),
    ClassicRecapPalette(Color(0xFF0F766E), Color(0xFF042F2E), Color(0xFF99F6E4)),
    ClassicRecapPalette(Color(0xFF7C3AED), Color(0xFF1E1146), Color(0xFFDDD6FE)),
)

private fun big(size: Int, color: Color = Color.White) = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.ExtraBold,
    fontSize = size.sp,
    lineHeight = (size * 1.08f).sp,
    letterSpacing = (-0.03f * size).sp,
    color = color,
)

private fun body(size: Int, color: Color = Color.White.copy(alpha = 0.78f), weight: FontWeight = FontWeight.Medium) = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = (size * 1.3f).sp,
    color = color,
)

private sealed interface RecapPage {
    object Intro : RecapPage
    object Total : RecapPage
    object HeroArtist : RecapPage
    object TopArtists : RecapPage
    object HeroSong : RecapPage
    object TopSongs : RecapPage
    object When : RecapPage
    object Outro : RecapPage
}

@Composable
fun RecapScreen(navController: NavController, yearly: Boolean = false) {
    val db = LocalDatabase.current
    val today = remember { LocalDate.now() }
    var stats by remember { mutableStateOf<ListeningStats?>(null) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(yearly) {
        loaded = false
        stats = if (yearly) {
            RecapRepository.load(db, StatsRange.yearOf(today)).takeIf { !it.isEmpty }
        } else {
            // force = true: show whatever has been played, even before the 3rd or the 10th song
            (RecapRepository.recapFor(db, today, force = true) as? RecapAvailability.Ready)?.stats
        }
        loaded = true
    }

    val topInset = WindowInsets.stableSystemBars.asPaddingValues().calculateTopPadding()
    val s = stats
    if (s == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Palettes[0].top, Palettes[0].bottom))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (!loaded) "Menyiapkan recap..." else "Belum ada recap.\nPutar beberapa lagu dulu, lalu kembali ke sini.",
                style = body(16, Color.White),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(32.dp),
            )
            CloseButton(Modifier.align(Alignment.TopEnd).padding(top = topInset + 12.dp, end = 16.dp)) { navController.navigateUp() }
        }
        return
    }

    val pages = remember(s) {
        buildList {
            add(RecapPage.Intro)
            add(RecapPage.Total)
            if (s.topArtists.isNotEmpty()) { add(RecapPage.HeroArtist); add(RecapPage.TopArtists) }
            if (s.topSongs.isNotEmpty()) { add(RecapPage.HeroSong); add(RecapPage.TopSongs) }
            add(RecapPage.When)
            add(RecapPage.Outro)
        }
    }
    var index by remember(s) { mutableIntStateOf(0) }
    val progress = remember(s) { Animatable(0f) }

    // the bar of the current page fills over PageMillis; when full, the next page opens (the last one stays)
    LaunchedEffect(index, s) {
        progress.snapTo(0f)
        if (index < pages.lastIndex) {
            progress.animateTo(1f, tween(PageMillis, easing = LinearEasing))
            index += 1
        } else {
            progress.snapTo(1f)
        }
    }

    val palette = Palettes[index % Palettes.size]
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(palette.top, palette.bottom)))
            .pointerInput(pages.size) {
                detectTapGestures { offset ->
                    if (offset.x < size.width * 0.33f) index = (index - 1).coerceAtLeast(0)
                    else if (index < pages.lastIndex) index += 1
                }
            },
    ) {
        AnimatedContent(
            targetState = index,
            transitionSpec = { fadeIn(tween(320)) togetherWith fadeOut(tween(220)) },
            modifier = Modifier.fillMaxSize(),
            label = "recapPage",
        ) { i ->
            Column(
                modifier = Modifier.fillMaxSize().padding(start = 28.dp, end = 28.dp, top = topInset + 64.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                when (pages[i]) {
                    RecapPage.Intro -> IntroPage(s, yearly, palette)
                    RecapPage.Total -> TotalPage(s, palette)
                    RecapPage.HeroArtist -> HeroArtistPage(s.topArtists.first(), palette)
                    RecapPage.TopArtists -> TopArtistsPage(s.topArtists, palette)
                    RecapPage.HeroSong -> HeroSongPage(s.topSongs.first(), palette)
                    RecapPage.TopSongs -> TopSongsPage(s.topSongs, palette)
                    RecapPage.When -> WhenPage(s, palette)
                    RecapPage.Outro -> OutroPage(s, palette) { navController.navigateUp() }
                }
            }
        }

        // progress: one segment per page
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = topInset + 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            pages.indices.forEach { i ->
                Box(Modifier.weight(1f).height(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.28f))) {
                    val fill = when {
                        i < index -> 1f
                        i == index -> progress.value
                        else -> 0f
                    }
                    Box(Modifier.fillMaxHeight().fillMaxWidth(fill.coerceIn(0f, 1f)).background(Color.White))
                }
            }
        }
        CloseButton(Modifier.align(Alignment.TopEnd).padding(top = topInset + 22.dp, end = 16.dp)) { navController.navigateUp() }
    }
}

@Composable
private fun CloseButton(modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier.size(34.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.28f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text("✕", style = body(14, Color.White, FontWeight.Bold)) }
}

@Composable
private fun Cover(url: String?, size: Int, circle: Boolean = false) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(size.dp)
            .clip(if (circle) CircleShape else RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.14f)),
    )
}

@Composable
private fun IntroPage(s: ListeningStats, yearly: Boolean, p: ClassicRecapPalette) {
    Text(if (yearly) "Recap tahunmu" else "Recap bulanmu", style = body(16, p.accent, FontWeight.SemiBold))
    Spacer(Modifier.height(10.dp))
    Text(s.range.label, style = big(64))
    Spacer(Modifier.height(14.dp))
    Text("Inilah lagu-lagu yang menemani ${if (yearly) "tahunmu" else "bulanmu"}. Ketuk untuk lanjut.", style = body(17))
}

@Composable
private fun TotalPage(s: ListeningStats, p: ClassicRecapPalette) {
    Text("Kamu mendengarkan", style = body(17))
    Spacer(Modifier.height(8.dp))
    Text(formatHoursMinutes(s.totalMs), style = big(58, p.accent))
    Spacer(Modifier.height(22.dp))
    Text("${s.plays} kali putar", style = big(26))
    Spacer(Modifier.height(6.dp))
    Text("dari ${s.songCount} lagu berbeda, ${s.artistCount} artis, ${s.albumCount} album.", style = body(16))
}

@Composable
private fun HeroArtistPage(a: RecapArtist, p: ClassicRecapPalette) {
    Text("Artis nomor satumu", style = body(17))
    Spacer(Modifier.height(20.dp))
    Cover(a.thumbnailUrl, 200, circle = true)
    Spacer(Modifier.height(22.dp))
    Text(a.name, style = big(38, p.accent), maxLines = 2, overflow = TextOverflow.Ellipsis)
    Spacer(Modifier.height(8.dp))
    Text("${formatHoursMinutes(a.playTimeMs)} dan ${a.plays} kali putar.", style = body(16))
    a.topSong?.let {
        Spacer(Modifier.height(6.dp))
        Text("Paling sering: ${it.title}", style = body(15), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun TopArtistsPage(artists: List<RecapArtist>, p: ClassicRecapPalette) {
    Text("Artis teratasmu", style = big(32))
    Spacer(Modifier.height(18.dp))
    artists.take(5).forEachIndexed { i, a ->
        RankRow(i + 1, a.thumbnailUrl, a.name, formatHoursMinutes(a.playTimeMs), circle = true, p = p)
    }
}

@Composable
private fun HeroSongPage(song: RecapSong, p: ClassicRecapPalette) {
    Text("Lagu nomor satumu", style = body(17))
    Spacer(Modifier.height(20.dp))
    Cover(song.thumbnailUrl, 220)
    Spacer(Modifier.height(22.dp))
    Text(song.title, style = big(32, p.accent), maxLines = 3, overflow = TextOverflow.Ellipsis)
    Spacer(Modifier.height(6.dp))
    Text(song.artistName, style = body(17, Color.White))
    Spacer(Modifier.height(10.dp))
    Text("Diputar ${song.plays} kali, total ${formatHoursMinutes(song.playTimeMs)}.", style = body(15))
}

@Composable
private fun TopSongsPage(songs: List<RecapSong>, p: ClassicRecapPalette) {
    Text("Lagu teratasmu", style = big(32))
    Spacer(Modifier.height(18.dp))
    songs.take(5).forEachIndexed { i, s ->
        RankRow(i + 1, s.thumbnailUrl, s.title, "${s.artistName} · ${s.plays}x", circle = false, p = p)
    }
}

@Composable
private fun RankRow(rank: Int, image: String?, title: String, subtitle: String, circle: Boolean, p: ClassicRecapPalette) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("$rank", style = big(24, p.accent), modifier = Modifier.width(34.dp))
        Cover(image, 52, circle)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = body(16, Color.White, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = body(13), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun WhenPage(s: ListeningStats, p: ClassicRecapPalette) {
    val dayIndex = s.weekdayMs.indices.maxByOrNull { s.weekdayMs[it] } ?: 0
    val peakHour = s.hourMs.indices.maxByOrNull { s.hourMs[it] } ?: 0
    val day = DayOfWeek.of(dayIndex + 1).displayName()
    val partOfDay = when (timeOfDayName(peakHour)) {
        "mornings" -> "pagi"
        "afternoons" -> "siang"
        "evenings" -> "sore dan malam"
        else -> "tengah malam"
    }
    Text("Kamu paling sering mendengarkan pada", style = body(17))
    Spacer(Modifier.height(10.dp))
    Text(day, style = big(50, p.accent))
    Spacer(Modifier.height(4.dp))
    Text(formatHoursMinutes(s.weekdayMs.getOrElse(dayIndex) { 0L }), style = body(16))
    Spacer(Modifier.height(26.dp))
    Text("Jam puncakmu", style = body(17))
    Spacer(Modifier.height(6.dp))
    Text("%02d:00 ($partOfDay)".format(peakHour), style = big(34))
}

@Composable
private fun OutroPage(s: ListeningStats, p: ClassicRecapPalette, onClose: () -> Unit) {
    Text("Itu recap ${s.range.label}-mu", style = big(36))
    Spacer(Modifier.height(12.dp))
    Text(
        "${formatHoursMinutes(s.totalMs)} musik, ${s.songCount} lagu. Terus dengarkan untuk menjaga streak apimu.",
        style = body(17),
    )
    Spacer(Modifier.height(30.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White)
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center,
    ) { Text("Selesai", style = body(16, Color.Black, FontWeight.Bold)) }
}
