// StreakScreen.kt
//
// What the streak bar opens. From the top: the "Recap kamu <year>" menu, the streak itself (a flame that changes with
// the day count, and an info button), then the playlists and the listening history under it.

package com.danrey.lalasi.ui.screens.streak

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.danrey.lalasi.LocalDatabase
import com.danrey.lalasi.LocalPlayerAwareWindowInsets
import com.danrey.lalasi.LocalPlayerConnection
import com.danrey.lalasi.R
import com.danrey.lalasi.models.toMediaMetadata
import com.danrey.lalasi.playback.queues.YouTubeQueue
import com.danrey.lalasi.ui.component.PlaylistThumbnail
import com.danrey.lalasi.ui.component.StreakFlame
import com.danrey.lalasi.ui.component.SubSettingsScaffold
import com.danrey.lalasi.ui.component.flameLook
import com.danrey.lalasi.ui.component.flameLookName
import com.danrey.lalasi.ui.component.calmGlassSurface
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import com.danrey.lalasi.ui.theme.LalasiInk
import com.danrey.lalasi.ui.theme.lalasiAdapt
import com.danrey.lalasi.ui.utils.resize
import com.danrey.lalasi.utils.StreakManager
import com.danrey.lalasi.utils.rememberStreak
import java.time.LocalDate

private val Panel get() = lalasiAdapt(0xFF1C1C1E)
private val PanelEdge get() = LalasiInk.copy(alpha = if (com.danrey.lalasi.ui.theme.LalasiPalette.glass) 0.20f else 0.09f)

@Composable
fun StreakScreen(navController: NavController) {
    val streak = rememberStreak()
    val s = streak.value
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current
    val playlists by database.playlistsByUpdatedDateAsc().collectAsState(initial = emptyList())
    val history by database.recentEvents(12).collectAsState(initial = emptyList())
    var showInfo by remember { mutableStateOf(false) }
    val bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()

    SubSettingsScaffold(title = "Streak", onBack = { navController.navigateUp() }) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            // 1. the recap menu (one entry: the month / year choice is inside the Recap page)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Panel)
                    .calmGlassSurface(RoundedCornerShape(22.dp))
                    .clickable { navController.navigate("stats") }
                    .padding(horizontal = 16.dp),
            ) {
                androidx.compose.material3.Icon(
                    painter = painterResource(R.drawable.trending_up),
                    contentDescription = null,
                    tint = LalasiInk,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    text = "Recap kamu ${LocalDate.now().year}",
                    color = LalasiInk,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                androidx.compose.material3.Icon(
                    painter = painterResource(R.drawable.navigate_next),
                    contentDescription = null,
                    tint = LalasiInk.copy(alpha = 0.5f),
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(Modifier.height(14.dp))

            // 2. the streak: the flame in its own light (the colour follows the flame's tier), the number, the tier, and a line for today
            val accent = remember(s.current) { flameAccent(s.current) }
            val heroShape = RoundedCornerShape(30.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(heroShape)
                    .background(Panel)
                    .drawBehind {
                        // the flame's light, soft and wide behind it; dimmer while the flame waits for today
                        val glowAlpha = if (s.doneToday) 0.34f else 0.16f
                        drawRect(
                            Brush.radialGradient(
                                0f to accent.copy(alpha = glowAlpha),
                                0.55f to accent.copy(alpha = glowAlpha * 0.35f),
                                1f to Color.Transparent,
                                center = Offset(size.width / 2f, size.height * 0.30f),
                                radius = size.width * 0.75f,
                            ),
                        )
                    }
                    .calmGlassSurface(heroShape),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 24.dp, start = 18.dp, end = 18.dp),
                ) {
                    Box(Modifier.height(150.dp).width(120.dp), contentAlignment = Alignment.Center) {
                        StreakFlame(lit = s.doneToday, days = s.current, tiered = true, modifier = Modifier.size(104.dp))
                    }
                    Text(
                        text = s.current.toString(),
                        color = LalasiInk,
                        fontSize = 76.sp,
                        lineHeight = 78.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-3).sp,
                        maxLines = 1,
                    )
                    Text(
                        text = "hari beruntun",
                        color = LalasiInk.copy(alpha = 0.66f),
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = flameLookName(s.current),
                        color = accent,
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = when {
                            s.current == 0 -> "Dengarkan musik hari ini untuk menyalakan api."
                            !s.doneToday -> "Dengarkan musik hari ini supaya apimu tidak padam."
                            else -> "${s.daysToNext} hari lagi menuju ${StreakManager.nextMilestone(s.current)} hari."
                        },
                        color = LalasiInk.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(20.dp))
                    MilestonePath(current = s.current, accent = accent)
                }
                // the info button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(36.dp)
                        .calmGlassSurface(CircleShape)
                        .clickable { showInfo = true },
                ) {
                    androidx.compose.material3.Icon(
                        painter = painterResource(R.drawable.info),
                        contentDescription = "Info streak",
                        tint = LalasiInk.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // two small facts under it
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StreakFact("Rekor terbaik", "${maxOf(s.best, s.current)} hari", Modifier.weight(1f))
                StreakFact(
                    "Hari ini",
                    if (s.doneToday) "Sudah dengar" else "${(s.todayProgress * 100).toInt().coerceIn(0, 99)}%",
                    Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(26.dp))

            // 3. playlists
            SectionHeader("Playlist") { navController.navigate("library/playlists") }
            Spacer(Modifier.height(10.dp))
            val shown = playlists.asReversed().take(10)
            if (shown.isEmpty()) {
                Text("Belum ada playlist.", color = LalasiInk.copy(alpha = 0.5f), fontSize = 13.sp)
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    shown.forEach { playlist ->
                        Column(
                            modifier = Modifier
                                .width(104.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    if (playlist.id == "liked") navController.navigate("auto_playlist/liked")
                                    else navController.navigate("local_playlist/${playlist.id}")
                                },
                        ) {
                            PlaylistThumbnail(
                                thumbnails = playlist.thumbnails,
                                size = 104.dp,
                                shape = RoundedCornerShape(16.dp),
                                placeHolder = {
                                    androidx.compose.material3.Icon(
                                        painter = painterResource(R.drawable.queue_music),
                                        contentDescription = null,
                                        tint = LalasiInk.copy(alpha = 0.6f),
                                        modifier = Modifier.size(32.dp),
                                    )
                                },
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = playlist.title,
                                color = LalasiInk,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(26.dp))

            // 4. listening history
            SectionHeader("Riwayat diputar") { navController.navigate("history") }
            Spacer(Modifier.height(6.dp))
            if (history.isEmpty()) {
                Text("Belum ada lagu yang diputar.", color = LalasiInk.copy(alpha = 0.5f), fontSize = 13.sp)
            }
            history.forEach { item ->
                val song = item.song
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { playerConnection?.playQueue(YouTubeQueue.radio(song.toMediaMetadata())) }
                        .padding(vertical = 6.dp),
                ) {
                    AsyncImage(
                        model = song.song.thumbnailUrl?.resize(240, 240),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(50.dp).clip(RoundedCornerShape(10.dp)).background(Panel),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = song.song.title,
                            color = LalasiInk,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = song.artists.joinToString(", ") { it.name },
                            color = LalasiInk.copy(alpha = 0.55f),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(Modifier.height(bottom + 120.dp))
        }
    }

    if (showInfo) StreakInfoDialog(current = s.current, onDismiss = { showInfo = false })
}

/** the flame's own colour at this streak (orange, sliding toward red and violet as the days go on), for the light behind it */
private fun flameAccent(days: Int): Color {
    val hue = ((25f + flameLook(days).hue) % 360f + 360f) % 360f
    return Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(hue, 0.95f, 0.56f)))
}

/** 0 - 3 - 10 - 30 - 100: how far along the road of milestones this streak is; static, so it costs nothing */
@Composable
private fun MilestonePath(current: Int, accent: Color) {
    val marks = intArrayOf(0, 3, 10, 30, 100)
    val pos = floatArrayOf(0f, 0.125f, 0.375f, 0.625f, 0.875f)
    var k = 0
    for (i in 0 until 4) if (current >= marks[i]) k = i
    val reach = if (current >= 100) pos[4] else {
        val f = (current - marks[k]).toFloat() / (marks[k + 1] - marks[k])
        pos[k] + (pos[k + 1] - pos[k]) * f
    }
    val ink = LalasiInk
    val bed = Panel
    Column(Modifier.fillMaxWidth()) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(22.dp)) {
            val cy = size.height / 2f
            val track = 4.dp.toPx()
            drawLine(ink.copy(alpha = 0.12f), Offset(0f, cy), Offset(size.width * pos[4], cy), track, StrokeCap.Round)
            if (reach > 0f) {
                drawLine(
                    Brush.horizontalGradient(listOf(accent.copy(alpha = 0.55f), accent), startX = 0f, endX = size.width * reach),
                    Offset(0f, cy), Offset(size.width * reach, cy), track, StrokeCap.Round,
                )
            }
            for (i in 1..4) {
                val reached = current >= marks[i]
                val c = Offset(size.width * pos[i], cy)
                drawCircle(if (reached) accent else bed, 8.dp.toPx(), c)
                if (reached) {
                    drawCircle(Color.White.copy(alpha = 0.92f), 3.dp.toPx(), c)
                } else {
                    drawCircle(ink.copy(alpha = 0.28f), 8.dp.toPx(), c, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            for (i in 1..4) {
                Text(
                    text = "${marks[i]} hari",
                    color = ink.copy(alpha = if (current >= marks[i]) 0.85f else 0.45f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StreakFact(label: String, value: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Panel)
            .calmGlassSurface(shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(label, color = LalasiInk.copy(alpha = 0.55f), fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text(value, color = LalasiInk, fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp, maxLines = 1)
    }
}

private class InfoTier(val days: Int, val range: String, val text: String)

private val InfoTiers = listOf(
    InfoTier(1, "1–9 hari", "Api kecil. Warnanya bergeser sedikit tiap hari ganjil."),
    InfoTier(10, "10 hari", "Api membesar."),
    InfoTier(30, "30 hari", "Warna berubah dan muncul dua api kecil di sisinya."),
    InfoTier(60, "60 hari", "Cincin cahaya mengelilingi api."),
    InfoTier(100, "100 hari", "Api terbesar. Tingkat tertinggi."),
)

/** The "Tentang streak" sheet with pictures: every flame tier drawn as it looks. */
@Composable
private fun StreakInfoDialog(current: Int, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 18.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(lalasiAdapt(0xFF1C1C1E))
                .border(BorderStroke(1.dp, PanelEdge), RoundedCornerShape(28.dp))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 22.dp),
        ) {
            Text("Tentang streak", color = LalasiInk, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Streak bertambah satu setiap hari kamu mendengarkan musik minimal ${StreakManager.MinListenMillisPerDay / 60_000L} menit. Kalau satu hari terlewat, apinya padam dan mulai dari awal.",
                color = LalasiInk.copy(alpha = 0.68f), fontSize = 13.sp, lineHeight = 18.sp,
            )
            Spacer(Modifier.height(16.dp))

            Spacer(Modifier.height(18.dp))
            Text("Api ikut berkembang", color = LalasiInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            InfoTiers.forEach { tier ->
                val reached = current >= tier.days
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(LalasiInk.copy(alpha = if (reached) 0.10f else 0.05f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Box(Modifier.size(width = 76.dp, height = 84.dp), contentAlignment = Alignment.Center) {
                        StreakFlame(lit = true, days = tier.days, tiered = true, modifier = Modifier.size(50.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tier.range, color = LalasiInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(tier.text, color = LalasiInk.copy(alpha = 0.62f), fontSize = 12.5.sp, lineHeight = 16.sp)
                    }
                    if (reached) {
                        Text("✓", color = Color(0xFFFF9F1C), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                "Mengerti",
                color = LalasiInk,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(LalasiInk.copy(alpha = 0.10f))
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 13.dp),
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick),
    ) {
        Text(
            text = title,
            color = LalasiInk,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        androidx.compose.material3.Icon(
            painter = painterResource(R.drawable.navigate_next),
            contentDescription = null,
            tint = LalasiInk.copy(alpha = 0.5f),
            modifier = Modifier.size(22.dp),
        )
    }
}
