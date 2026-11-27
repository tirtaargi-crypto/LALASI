// about app, upstream credits and license sheets pushed from the version sheet or the lalasi page

package com.danrey.lalasi.ui.screens.update

import com.danrey.lalasi.ui.theme.LalasiInk
import com.danrey.lalasi.ui.theme.lalasiAdapt

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.danrey.lalasi.BuildConfig
import com.danrey.lalasi.R
import com.danrey.lalasi.core.updater.GithubOwner
import com.danrey.lalasi.core.updater.GithubRepo
import com.danrey.lalasi.core.updater.GithubRepoUrl
import com.danrey.lalasi.ui.component.PopupSheetState
import com.danrey.lalasi.ui.component.pressBounce
import com.danrey.lalasi.ui.player.menu.MenuRowSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

@Immutable
private data class LeadDevEntry(
    val name: String,
    val githubUser: String,
    val devSpaceIndex: Int,
    @DrawableRes val fallbackAvatar: Int,
)

@Immutable
private data class CreditEntry(
    val name: String,
    val subtitle: String,
    val githubUser: String,
    val url: String,
)

@Immutable
private data class LicenseEntry(
    val tag: String,
    val title: String,
    val subtitle: String,
    val url: String,
)

@Immutable
private data class ContributorEntry(
    val login: String,
    val avatarUrl: String,
    val profileUrl: String,
)

private val LeadDevs = listOf(
    LeadDevEntry(
        name = "Roo",
        githubUser = "realidkroo",
        devSpaceIndex = 0,
        fallbackAvatar = R.drawable.frame_51_3,
    ),
    LeadDevEntry(
        name = "key",
        githubUser = "realkeichi",
        devSpaceIndex = 1,
        fallbackAvatar = R.drawable.dev_keycii,
    ),
)

private val BaseCredits = listOf(
    CreditEntry(
        name = "vivizzz007",
        subtitle = "Vivimusic dev, The base of this app",
        githubUser = "vivizzz007",
        url = "https://github.com/vivizzz007/vivi-music",
    ),
    CreditEntry(
        name = "Mostafa Alagamy",
        subtitle = "Metrolist dev, The base of vivmusic too",
        githubUser = "mostafaalagamy",
        url = "https://github.com/mostafaalagamy/Metrolist",
    ),
)

private val MoreCredits = listOf(
    CreditEntry(
        name = "maxrave-dev",
        subtitle = "SimpMusic · lyrics API provider",
        githubUser = "maxrave-dev",
        url = "https://github.com/maxrave-dev/SimpMusic",
    ),
    CreditEntry(
        name = "Rukamori",
        subtitle = "ArchiveTune · design & architecture inspiration",
        githubUser = "rukamori",
        url = "https://github.com/rukamori/ArchiveTune",
    ),
    CreditEntry(
        name = "fast4x",
        subtitle = "RiMusic · YouTube Music InnerTube service wrapper",
        githubUser = "fast4x",
        url = "https://github.com/fast4x/RiMusic",
    ),
    CreditEntry(
        name = "tranxuanthang",
        subtitle = "LRCLIB · synchronized LRC lyrics provider",
        githubUser = "tranxuanthang",
        url = "https://github.com/tranxuanthang/lrclib",
    ),
    CreditEntry(
        name = "monochrome-music",
        subtitle = "Monochrome Music · lossless FLAC streams",
        githubUser = "monochrome-music",
        url = "https://github.com/monochrome-music/monochrome",
    ),
    CreditEntry(
        name = "Jaakko Pasanen",
        subtitle = "AutoEq · headphone equalizer database",
        githubUser = "jaakkopasanen",
        url = "https://github.com/jaakkopasanen/AutoEq",
    ),
    CreditEntry(
        name = "Ibra Al Tabian",
        subtitle = "LyricsPlus · open source lyrics server",
        githubUser = "ibratabian17",
        url = "https://github.com/ibratabian17/LyricsPlus",
    ),
    CreditEntry(
        name = "Boidushya",
        subtitle = "Better Lyrics API · synced syllable lyrics",
        githubUser = "boidushya",
        url = "https://github.com/better-lyrics/api",
    ),
)

private val Licenses = listOf(
    LicenseEntry(
        tag = "GPL-3.0",
        title = "Lalasi Music · GNU General Public License v3.0",
        subtitle = "Free and open source software, source code on GitHub",
        url = "$GithubRepoUrl/blob/main/LICENSE",
    ),
    LicenseEntry(
        tag = "GPL-3.0",
        title = "ViviMusic · GNU General Public License v3.0",
        subtitle = "by vivizzz007 · base project this app was forked from",
        url = "https://github.com/vivizzz007/vivi-music/blob/main/LICENSE",
    ),
    LicenseEntry(
        tag = "GPL-3.0",
        title = "Metrolist · GNU General Public License v3.0",
        subtitle = "by Mostafa Alagamy · base framework",
        url = "https://github.com/mostafaalagamy/Metrolist/blob/main/LICENSE",
    ),
    LicenseEntry(
        tag = "GPL-3.0",
        title = "SimpMusic · GNU General Public License v3.0",
        subtitle = "by maxrave-dev · lyrics provider integration",
        url = "https://github.com/maxrave-dev/SimpMusic/blob/dev/LICENSE",
    ),
    LicenseEntry(
        tag = "GPL-3.0",
        title = "ArchiveTune · GNU General Public License v3.0",
        subtitle = "by Rukamori · design and architectural inspiration",
        url = "https://github.com/rukamori/ArchiveTune/blob/main/LICENSE",
    ),
    LicenseEntry(
        tag = "GPL-3.0",
        title = "RiMusic · GNU General Public License v3.0",
        subtitle = "by fast4x · InnerTube API wrapper",
        url = "https://github.com/fast4x/RiMusic/blob/master/LICENSE",
    ),
    LicenseEntry(
        tag = "MIT",
        title = "LRCLIB · MIT License",
        subtitle = "by tranxuanthang · synchronized lyrics database",
        url = "https://github.com/tranxuanthang/lrclib/blob/main/LICENSE",
    ),
    LicenseEntry(
        tag = "MIT",
        title = "AutoEq · MIT License",
        subtitle = "by Jaakko Pasanen · headphone correction profiles",
        url = "https://github.com/jaakkopasanen/AutoEq/blob/master/LICENSE",
    ),
)

//  this guy promote the app
private val ExtraContributors = listOf(
    ContributorEntry(
        login = "swtstvn",
        avatarUrl = githubAvatarUrl("swtstvn"),
        profileUrl = "https://github.com/swtstvn",
    ),
)

private val FallbackContributors = listOf(
    ContributorEntry(
        login = "realidkroo",
        avatarUrl = githubAvatarUrl("realidkroo"),
        profileUrl = "https://github.com/realidkroo",
    ),
    ContributorEntry(
        login = "realkeichi",
        avatarUrl = githubAvatarUrl("realkeichi"),
        profileUrl = "https://github.com/realkeichi",
    ),
) + ExtraContributors

// kept in memory for the session so reopening the about sheet never hits github twice
@Volatile
private var cachedContributors: List<ContributorEntry>? = null

// github redirects username.png straight to the user's current avatar without counting against the api rate limit
private fun githubAvatarUrl(githubUser: String): String =
    "https://github.com/$githubUser.png?size=160"

private suspend fun fetchContributors(): List<ContributorEntry> {
    cachedContributors?.let { return it }
    return withContext(Dispatchers.IO) {
        runCatching {
            val url = URL("https://api.github.com/repos/$GithubOwner/$GithubRepo/contributors?per_page=30")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "lalasi-android")
            }
            try {
                val arr = JSONArray(conn.inputStream.bufferedReader().use { it.readText() })
                buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val type = obj.optString("type", "User")
                        if (type.equals("Bot", ignoreCase = true)) continue
                        val login = obj.optString("login").takeIf { it.isNotBlank() } ?: continue
                        if (login.endsWith("[bot]", ignoreCase = true)) continue
                        val avatar = obj.optString("avatar_url").ifBlank { githubAvatarUrl(login) }
                        val html = obj.optString("html_url").ifBlank { "https://github.com/$login" }
                        add(ContributorEntry(login = login, avatarUrl = avatar, profileUrl = html))
                    }
                }.ifEmpty { FallbackContributors }
                    .plus(ExtraContributors)
                    .distinctBy { it.login.lowercase() }
                    .also { cachedContributors = it }
            } finally {
                conn.disconnect()
            }
        }.getOrDefault(FallbackContributors)
    }
}

fun PopupSheetState.showAboutSheet(onOpenDevSpace: (Int) -> Unit = {}) {
    val popup = this
    show(buttonBar = { SheetPrimaryButton(text = "Done", onClick = popup::dismiss) }) {
        AboutSheetContent(popup = popup, onOpenDevSpace = onOpenDevSpace)
    }
}

private fun PopupSheetState.showCreditsSheet() {
    val popup = this
    show(buttonBar = { SheetPrimaryButton(text = "Done", onClick = popup::dismiss) }) {
        CreditsSheetContent()
    }
}


// serif with a gothic edge, matches the banner's engraved look; OFL licensed (Cardo)
private val BannerFont = FontFamily(
    Font(R.font.cardo_bold, FontWeight.Bold),
    Font(R.font.cardo_italic, FontWeight.Normal, FontStyle.Italic),
)

private const val BannerW = 1080f
private const val BannerH = 607f

// where the eye glow bitmap sits inside the banner artwork (pixels of the 1080x607 source)
private const val GlowX = 370f
private const val GlowY = 135f
private const val GlowW = 300f
private const val GlowH = 115f

private val DevInstagram = "devreyw_"
private val DevTiktok = "danhanzyr"

@Composable
private fun AboutBanner(modifier: Modifier = Modifier) {
    val glow = ImageBitmap.imageResource(R.drawable.lalasi_banner_eye_glow)
    // one light on/off cycle: ignite fast, hold, then fade; only the draw phase reads it, so nothing recomposes
    val transition = rememberInfiniteTransition(label = "bannerEyes")
    val pulse = transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3200
                0f at 0 using CubicBezierEasing(0.2f, 0f, 0f, 1f)
                1f at 700 using LinearEasing
                0.82f at 1100 using LinearEasing
                1f at 1500 using CubicBezierEasing(0.4f, 0f, 0.6f, 1f)
                0.05f at 2700 using LinearEasing
                0f at 3200
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "eyePulse",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(BannerW / BannerH)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF8E1114))
            .drawWithContent {
                drawContent()
                val a = pulse.value
                if (a > 0.01f) {
                    val sx = size.width / BannerW
                    val sy = size.height / BannerH
                    drawImage(
                        image = glow,
                        dstOffset = IntOffset((GlowX * sx).toInt(), (GlowY * sy).toInt()),
                        dstSize = IntSize((GlowW * sx).toInt().coerceAtLeast(1), (GlowH * sy).toInt().coerceAtLeast(1)),
                        alpha = a,
                        blendMode = BlendMode.Plus,
                    )
                }
            },
    ) {
        Image(
            painter = painterResource(R.drawable.lalasi_about_banner),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // bottom-left scrim so the signature stays readable on the dark silhouette
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent),
                        start = Offset(0f, Float.POSITIVE_INFINITY),
                        end = Offset(Float.POSITIVE_INFINITY, 0f),
                    ),
                ),
        )
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontFamily = BannerFont, fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = 3.sp, color = lalasiAdapt(0xFFF3E6DF))) {
                    append("LALASI")
                }
                withStyle(SpanStyle(fontFamily = BannerFont, fontStyle = FontStyle.Italic, fontSize = 14.sp, letterSpacing = 1.sp, color = Color(0xFFE9A9A0))) {
                    append("  by DanRey")
                }
            },
            style = TextStyle(shadow = Shadow(color = Color.Black.copy(alpha = 0.7f), offset = Offset(0f, 2f), blurRadius = 8f)),
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 12.dp),
        )
    }
}

@Composable
private fun ColumnScope.AboutSheetContent(
    popup: PopupSheetState,
    onOpenDevSpace: (Int) -> Unit,
) {
    val installed = shortVersion(BuildConfig.VERSION_NAME)
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        AboutBanner()
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Music & Audio player",
            fontSize = 15.sp,
            color = Color(0xFF8E8E93),
        )
        Spacer(Modifier.height(20.dp))

        // one inset grouped card, like iOS Settings > General > About
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(lalasiAdapt(0xFF1C1C1E)),
        ) {
            AboutValueRow(label = "Versi", value = installed)
            Box(Modifier.fillMaxWidth().padding(start = 16.dp).height(0.5.dp).background(LalasiInk.copy(alpha = 0.12f)))
            AboutValueRow(label = "Aplikasi", value = "LALASI")
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "Developer",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF8E8E93),
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, bottom = 6.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(lalasiAdapt(0xFF1C1C1E)),
        ) {
            // developer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.lalasi_anime_dark),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(44.dp).clip(CircleShape),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(text = "DanRey", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = "Developer · LALASI", fontSize = 13.sp, color = Color(0xFF8E8E93))
                }
            }
            Box(Modifier.fillMaxWidth().padding(start = 16.dp).height(0.5.dp).background(LalasiInk.copy(alpha = 0.12f)))
            SocialRow(
                platform = "Instagram",
                handle = "@$DevInstagram",
                icon = R.drawable.ic_social_instagram,
                tile = Brush.linearGradient(listOf(Color(0xFFFEDA75), Color(0xFFFA7E1E), Color(0xFFD62976), Color(0xFF962FBF), Color(0xFF4F5BD5))),
                onClick = { context.openUrl("https://www.instagram.com/$DevInstagram") },
            )
            Box(Modifier.fillMaxWidth().padding(start = 16.dp).height(0.5.dp).background(LalasiInk.copy(alpha = 0.12f)))
            SocialRow(
                platform = "TikTok",
                handle = "@$DevTiktok",
                icon = R.drawable.ic_social_tiktok,
                tile = Brush.linearGradient(listOf(lalasiAdapt(0xFF111111), lalasiAdapt(0xFF111111))),
                tiktokSplit = true,
                onClick = { context.openUrl("https://www.tiktok.com/@$DevTiktok") },
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SocialRow(
    platform: String,
    handle: String,
    @DrawableRes icon: Int,
    tile: Brush,
    onClick: () -> Unit,
    tiktokSplit: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .pressBounce(interaction, pressedScale = 0.98f)
            .fillMaxWidth()
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(tile)
                .border(0.5.dp, LalasiInk.copy(alpha = 0.14f), RoundedCornerShape(9.dp)),
        ) {
            if (tiktokSplit) {
                // the brand's cyan/red split, drawn as two offset copies under the white glyph
                Icon(painterResource(icon), null, tint = Color(0xFF25F4EE), modifier = Modifier.size(19.dp).offset(x = (-1).dp, y = (-1).dp))
                Icon(painterResource(icon), null, tint = Color(0xFFFE2C55), modifier = Modifier.size(19.dp).offset(x = 1.dp, y = 1.dp))
            }
            Icon(painterResource(icon), contentDescription = platform, tint = LalasiInk, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(text = platform, fontSize = 17.sp)
            Text(text = handle, fontSize = 13.sp, color = Color(0xFF8E8E93), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(text = "›", fontSize = 22.sp, color = Color(0xFF8E8E93))
    }
}

@Composable
private fun AboutValueRow(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = label,
            fontSize = 17.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            fontSize = 17.sp,
            color = Color(0xFF8E8E93),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ColumnScope.CreditsSheetContent() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Credits",
            fontSize = 36.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1.4).sp,
        )
        Spacer(Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MenuRowSurface)
                .padding(vertical = 6.dp),
        ) {
            (BaseCredits + MoreCredits).forEach { credit ->
                PersonRow(
                    name = credit.name,
                    subtitle = credit.subtitle,
                    avatarUrl = githubAvatarUrl(credit.githubUser),
                    showArrow = true,
                    onClick = { context.openUrl(credit.url) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ColumnScope.LicenseSheetContent() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = "License",
            fontSize = 36.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1.4).sp,
        )
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Licenses.forEach { entry ->
                LicenseCard(
                    entry = entry,
                    onClick = { context.openUrl(entry.url) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.3).sp,
        color = LalasiInk.copy(alpha = 0.55f),
    )
}

@Composable
private fun AboutPillRow(text: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .pressBounce(interaction, pressedScale = 0.97f)
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MenuRowSurface)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp),
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AboutCardButton(text: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .pressBounce(interaction, pressedScale = 0.97f)
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MenuRowSurface)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp),
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PersonRow(
    name: String,
    subtitle: String?,
    avatarUrl: String,
    showArrow: Boolean,
    onClick: () -> Unit,
    @DrawableRes fallbackRes: Int? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .pressBounce(interaction, pressedScale = 0.98f)
            .fillMaxWidth()
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        GithubAvatar(
            url = avatarUrl,
            size = 44.dp,
            fallbackRes = fallbackRes,
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.2).sp,
                    color = LalasiInk.copy(alpha = 0.65f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (showArrow) {
            Spacer(Modifier.width(10.dp))
            ArrowBadge()
        }
    }
}

@Composable
private fun AndMoreRow(
    avatars: List<String>,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val circleSize = 22.dp
    val step = 13.dp
    val stackWidth = circleSize + step * (avatars.size - 1).coerceAtLeast(0)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .pressBounce(interaction, pressedScale = 0.97f)
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MenuRowSurface)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp),
    ) {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier.width(stackWidth).height(circleSize),
        ) {
            avatars.forEachIndexed { index, url ->
                GithubAvatar(
                    url = url,
                    size = circleSize,
                    modifier = Modifier
                        .offset(x = step * index)
                        .border(1.5.dp, MenuRowSurface, CircleShape),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = "And more \u2192",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp,
        )
    }
}

@Composable
private fun LicenseCard(
    entry: LicenseEntry,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .pressBounce(interaction, pressedScale = 0.98f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MenuRowSurface)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.tag,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp,
                color = LalasiInk.copy(alpha = 0.55f),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = entry.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = entry.subtitle,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.2).sp,
                color = LalasiInk.copy(alpha = 0.55f),
            )
        }
        Spacer(Modifier.width(12.dp))
        ArrowBadge()
    }
}

@Composable
private fun ArrowBadge() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(LalasiInk.copy(alpha = 0.16f)),
    ) {
        Icon(
            painter = painterResource(R.drawable.arrow_forward),
            contentDescription = null,
            tint = LalasiInk.copy(alpha = 0.85f),
            modifier = Modifier.size(12.dp),
        )
    }
}

@Composable
private fun GithubAvatar(
    url: String,
    size: Dp,
    modifier: Modifier = Modifier,
    @DrawableRes fallbackRes: Int? = null,
) {
    val context = LocalContext.current
    val request = remember(url) {
        ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
            .build()
    }
    val fallback = fallbackRes?.let { painterResource(it) }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(lalasiAdapt(0xFFD9D9D9)),
    ) {
        AsyncImage(
            model = request,
            contentDescription = null,
            placeholder = fallback,
            error = fallback,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
