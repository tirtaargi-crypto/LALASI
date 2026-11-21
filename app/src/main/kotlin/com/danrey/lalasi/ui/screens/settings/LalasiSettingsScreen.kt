// LalasiSettingsScreen.kt

package com.danrey.lalasi.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.danrey.lalasi.R
import com.danrey.lalasi.constants.ProfilePicUriKey
import com.danrey.lalasi.constants.UsernameKey
import com.danrey.lalasi.scrobble.ScrobbleSite
import com.danrey.lalasi.ui.component.SettingsGroup
import com.danrey.lalasi.ui.component.SettingsGroupStyle
import com.danrey.lalasi.ui.component.SettingsHighlight
import com.danrey.lalasi.ui.component.SettingsItem
import com.danrey.lalasi.ui.component.SubSettingsScaffold
import com.danrey.lalasi.ui.component.SubSettingsSearchBar
import com.danrey.lalasi.ui.component.navigateToTab
import com.danrey.lalasi.ui.screens.gesture.customisationSectionRoute
import com.danrey.lalasi.ui.screens.profile.ProfileStore
import com.danrey.lalasi.ui.screens.search.SearchAvatar
import com.danrey.lalasi.ui.screens.settings.importsync.YouTubeSyncRoute
import com.danrey.lalasi.ui.screens.settings.integrations.IntegrationsRoute
import com.danrey.lalasi.ui.screens.settings.integrations.scrobbleServiceRoute
import com.danrey.lalasi.ui.theme.InterFontFamily
import com.danrey.lalasi.utils.rememberPreference

private data class SettingsEntry(
    val title: String,
    val page: String,
    val route: String,
    val keywords: String = "",
    val titleRes: Int? = null,
    val pageRes: Int? = null,
) {
    val highlightKey: String get() = title
}

// search ids stay english so rows can find themselves, what's shown and matched follows the app's language
private val LocalizedSettingsTitles = mapOf(
    "Customisation" to R.string.settings_customisation_category,
    "Blur" to R.string.appearance_blur,
    "Progressive blur" to R.string.appearance_progressive_blur,
    "Blur quality" to R.string.appearance_blur_quality,
    "Blur strength" to R.string.appearance_blur_strength,
    "Animation" to R.string.appearance_animation,
    "Lyrics animation" to R.string.appearance_lyrics_animation,
    "Lyrics" to R.string.lalasi_set_lyrics,
    "Larp mode" to R.string.larp_mode,
    "Disable App Updates notice" to R.string.settings_updates_notice,
    "Offline mode" to R.string.settings_offline_mode,
    "Proxy" to R.string.proxy,
    "Albums from liked songs" to R.string.settings_albums_from_likes,
    "Keep playing even when closed" to R.string.playback_keep_playing,
    "Play alongside other apps" to R.string.playback_mix_other_apps,
    "Save Player's last state" to R.string.playback_save_state,
    "Local song auto metadata" to R.string.settings_local_auto_metadata,
    "Disable auto-playing YouTube video" to R.string.settings_disable_video_autoplay,
    "Translations from YouTube" to R.string.settings_youtube_translations,
    "Sync lyrics to music videos" to R.string.settings_sync_lyrics_video,
    "Don't skip on failure" to R.string.settings_dont_skip_failure,
    "Skip timeout failure" to R.string.settings_skip_timeout,
    "Skip Silence" to R.string.playback_skip_silence,
    "Audio format badge" to R.string.appearance_format_badge,
    "Show sample rate" to R.string.appearance_sample_rate,
    "Always enable Youtube background" to R.string.appearance_youtube_background,
    "Enable animated canvas" to R.string.appearance_canvas,
    "High quality bloom" to R.string.appearance_bloom,
    "Audio Pass" to R.string.audiopass_hub_title,
    "Prefer USB DAC" to R.string.playback_usb_prefer,
    "USB DAC passthrough" to R.string.playback_usb_passthrough,
    "Hi-res output" to R.string.playback_hires,
    "Hi-fi resampler" to R.string.playback_hifi,
    "Audio Lab" to R.string.settings_audio_lab_category,
    "Modules" to R.string.settings_modules_category,
    "Storage" to R.string.storage,
    "App behaviour" to R.string.settings_app_behaviour,
    "Disable description on settings" to R.string.settings_hide_descriptions,
    "Cover rounding" to R.string.customise_cover_rounding,
    "Playlist styles" to R.string.customise_playlist_styles,
    "Player styles" to R.string.customise_player_styles,
    "Home styles" to R.string.customise_home_styles,
    "Element blur" to R.string.customise_element_blur,
    "Animation speed" to R.string.customise_animation_speed,
    "Top Bar on player" to R.string.customise_top_bar,
    "Gesture" to R.string.customise_gesture,
    "Swipe right" to R.string.customise_swipe_right,
    "Swipe left" to R.string.customise_swipe_left,
    "Swipe down anywhere to close" to R.string.customise_swipe_down_anywhere,
    "Open with your finger" to R.string.customise_swipe_up_follows,
)
private val LocalizedSettingsPages = mapOf(
    "App behaviour" to R.string.settings_app_behaviour,
    "Customisation" to R.string.settings_customisation_category,
    "Lyrics" to R.string.lalasi_set_lyrics,
    "Storage" to R.string.storage,
    "Audio Lab" to R.string.settings_audio_lab_category,
    "Modules" to R.string.settings_modules_category,
    "Integrations" to R.string.integrations,
)

private val SettingsIndex = listOf(
    // Customisation
    SettingsEntry("Customisation", "Customisation", "customisation_settings", "customize cover rounding playlist player home styles gesture swipe blur animation"),
    SettingsEntry("Dynamic Island", "Customisation", customisationSectionRoute("dynamic_island"), "pill notch iphone ios top lyrics glass liquid blue black style camera kamera depan cutout poni lirik"),
    SettingsEntry("Lyrics", "Lyrics", "lyrics_settings", "lyrics display animation style bloom translation sync karaoke"),
    SettingsEntry("Cover rounding", "Customisation", customisationSectionRoute("rounding"), "square rounded corner radius px thumbnail art"),
    SettingsEntry("Playlist styles", "Customisation", "customisation_settings", "flow classic minimal header layout"),
    SettingsEntry("Player styles", "Customisation", customisationSectionRoute("player"), "flagship turntable ipod poster timeline lyrics studio customize player cover"),
    SettingsEntry("Home styles", "Customisation", "customisation_settings", "lalasi listen now hop in hero layout grid"),
    SettingsEntry("Blur", "Customisation", customisationSectionRoute("blur"), "blur glass frosted performance lag"),
    SettingsEntry("Progressive blur", "Customisation", customisationSectionRoute("blur"), "gradient fade top bar hairline flat"),
    SettingsEntry("Blur quality", "Customisation", customisationSectionRoute("blur"), "low high glass performance"),
    SettingsEntry("Blur strength", "Customisation", customisationSectionRoute("blur"), "radius px glass amount"),
    SettingsEntry("Element blur", "Customisation", customisationSectionRoute("element_blur"), "lyrics line blur cover zoom title morph menu page wipe"),
    SettingsEntry("Animation", "Customisation", customisationSectionRoute("animation"), "transition motion off reduce disable"),
    SettingsEntry("Animation speed", "Customisation", customisationSectionRoute("animation"), "faster slower duration scale motion"),
    SettingsEntry("Lyrics animation", "Lyrics", "lyrics_settings", "lyrics style 1 2 boring plain shader glow wave agsl letters"),
    SettingsEntry("Top Bar on player", "Customisation", customisationSectionRoute("top_bar"), "status bar hide clock battery player top"),
    SettingsEntry("Gesture", "Customisation", customisationSectionRoute("gesture"), "swipe slide song list left right queue library download play next playlist details"),
    SettingsEntry("Swipe right", "Customisation", customisationSectionRoute("gesture"), "slide song list queue library download play next playlist details action"),
    SettingsEntry("Swipe left", "Customisation", customisationSectionRoute("gesture"), "slide song list queue library download play next playlist details action"),
    SettingsEntry("Swipe down anywhere to close", "Customisation", customisationSectionRoute("gesture"), "player close dismiss swipe down pull gesture anywhere"),
    SettingsEntry("Open with your finger", "Customisation", customisationSectionRoute("gesture"), "player open swipe up mini player follow finger speed velocity gesture"),

    // App behaviour
    SettingsEntry("App behaviour", "App behaviour", "general_settings", "general playback player behaviour"),
    SettingsEntry("Larp mode", "App behaviour", "general_settings", "larp 67 flac 768 khz wadax atlantis dac hi-res lossless bit-perfect flex"),
    SettingsEntry("App language", "App behaviour", "general_settings", "language bahasa locale device system indonesia english translate", R.string.app_language),
    SettingsEntry("Disable App Updates notice", "App behaviour", "general_settings", "update popup notification updater"),
    SettingsEntry("Offline mode", "App behaviour", "general_settings", "play downloaded cache offline"),
    SettingsEntry("Proxy", "App behaviour", ProxySettingsRoute, "proxy socks http server vpn network region country blocked bot sign in"),
    SettingsEntry("Albums from liked songs", "App behaviour", "general_settings", "albums library liked songs old way apple music"),
    SettingsEntry("Keep playing even when closed", "App behaviour", "general_settings", "background close stop playback"),
    SettingsEntry("Play alongside other apps", "App behaviour", "general_settings", "audio focus mix pause other app video game sound together"),
    SettingsEntry("Save Player's last state", "App behaviour", "general_settings", "persistent queue restore track state"),
    SettingsEntry("Local song auto metadata", "App behaviour", "general_settings", "match tags id3 covers lyrics"),
    SettingsEntry("Disable auto-playing YouTube video", "App behaviour", "general_settings", "music video live autoplay cover"),
    SettingsEntry("Translations from YouTube", "App behaviour", "general_settings", "captions translate lyrics language"),
    SettingsEntry("Sync lyrics to music videos", "App behaviour", "general_settings", "lyrics video timing intro skit"),
    SettingsEntry("Don't skip on failure", "App behaviour", "general_settings", "fail stream error stay skip"),
    SettingsEntry("Skip timeout failure", "App behaviour", "general_settings", "skip timeout seconds failed stream"),
    SettingsEntry("Skip Silence", "App behaviour", "general_settings", "silence trim gap interval"),
    SettingsEntry("Audio format badge", "App behaviour", "general_settings", "format codec opus aac flac badge title quality"),
    SettingsEntry("Show sample rate", "App behaviour", "general_settings", "sample rate khz hz format badge quality"),
    SettingsEntry("Always enable Youtube background", "App behaviour", "general_settings", "youtube video background canvas"),
    SettingsEntry("Enable animated canvas", "App behaviour", "general_settings", "canvas animation spotify looping visual"),
    SettingsEntry("High quality bloom", "App behaviour", "general_settings", "lyrics bloom quality extra soft pass glow"),
    SettingsEntry("Disable description on settings", "App behaviour", "general_settings", "hide subtitle text rows titles only"),

    // Audio Lab
    SettingsEntry("Audio Lab", "Audio Lab", "audio_lab", "sound effects equalizer fx quality"),
    SettingsEntry("Crossfade", "Audio Lab", "crossmix_settings", "crossmix automix transition gapless blend"),
    SettingsEntry("Equalizer", "Audio Lab", "equalizer", "eq bass treble band parametric graph autoeq headphones correction preset"),
    SettingsEntry("AutoEQ", "Audio Lab", "equalizer", "headphones earbuds correction harman database model"),
    SettingsEntry("Audio Quality", "Audio Lab", "audio_quality", "bitrate stream high low lossless hi-res flac monochrome"),
    SettingsEntry("Audio Pass", "Audio Lab", "audio_pass", "dac usb output headphones bluetooth codec bit perfect exclusive"),
    SettingsEntry("Prefer USB DAC", "Audio Lab", "audio_pass", "usb dac headphones output external audio"),
    SettingsEntry("USB DAC passthrough", "Audio Lab", "audio_pass", "bit perfect bitperfect passthrough sample rate dac usb resample"),
    SettingsEntry("Hi-res output", "Audio Lab", "audio_pass", "hi res 24 bit float precision lossless flac quality"),
    SettingsEntry("Hi-fi resampler", "Audio Lab", "audio_pass", "resample sample rate 44.1 48 khz sinc src quality filter"),
    SettingsEntry("Reverb", "Audio Lab", "audio_fx", "room hall echo space fx effect"),
    SettingsEntry("Loudness", "Audio Lab", "audio_fx", "boost louder compressor limiter night mode fx"),
    SettingsEntry("Audio Normalization", "Audio Lab", "audio_fx", "even out songs loudness volume gain balance replaygain"),
    SettingsEntry("Spatial audio", "Audio Lab", "audio_fx", "crossfeed binaural hrtf virtual speakers 3d headphones"),
    SettingsEntry("Dolby Atmos", "Audio Lab", "audio_fx", "atmos spatial surround 3d immersive"),
    SettingsEntry("Playback speed", "Audio Lab", "audio_fx", "tempo pitch slowed nightcore preserve pitch"),
    SettingsEntry("Exponential volume", "Audio Lab", "audio_fx", "volume curve logarithmic limit"),
    SettingsEntry("Modules", "Modules", "modules", "apple music source module plugin extension stream subscription"),
    SettingsEntry("Apple Music", "Modules", "module/applemusic", "apple music itunes subscription stream aac"),
    SettingsEntry("Monochrome", "Modules", "module/monochrome", "monochrome lossless flac hi-res tidal"),
    SettingsEntry("Provider order", "Audio Lab", "audio_quality", "source order apple music monochrome youtube priority"),
    SettingsEntry("YouTube Music", "Audio Lab", "audio_quality", "youtube turn off disable provider only monochrome apple music"),

    // Integrations
    SettingsEntry("Last.fm", "Integrations", scrobbleServiceRoute(ScrobbleSite.LASTFM), "lastfm scrobble scrobbling history stats account sign in"),
    SettingsEntry("Libre.fm", "Integrations", scrobbleServiceRoute(ScrobbleSite.LIBREFM), "librefm scrobble scrobbling history stats account sign in"),
    SettingsEntry("Now Playing", "Integrations", scrobbleServiceRoute(ScrobbleSite.LASTFM), "scrobble now playing status profile lastfm librefm"),
    SettingsEntry("Send likes", "Integrations", scrobbleServiceRoute(ScrobbleSite.LASTFM), "love loved songs lastfm librefm scrobble"),
    SettingsEntry("YouTube Music", "Integrations", YouTubeSyncRoute, "youtube ytm sync account sign in likes library playlists history"),

    // Storage
    SettingsEntry("Storage", "Storage", "storage_settings", "space disk size cache downloads clear free junk delete"),
    SettingsEntry("Music cache limit", "Storage", "storage_settings", "cache size limit space song stream"),
    SettingsEntry("Manage downloads", "Storage", "storage_settings", "downloads delete remove biggest offline space"),
    SettingsEntry("Remove all downloads", "Storage", "storage_settings", "delete clear downloads offline space"),

    // Experimental
    SettingsEntry("Import & sync", "Experimental", "experimental_settings", "import spotify apple music deezer tidal tunemymusic csv youtube sync export backup transfer"),
    SettingsEntry("Big disc cover styles", "Experimental", "experimental_settings", "disc vinyl cover"),
    SettingsEntry("Music haptics", "Experimental", "experimental_settings", "vibration haptic"),
    SettingsEntry("Cipher", "Experimental", "experimental_settings", "player script signature refresh"),
    SettingsEntry("Playback diagnostics", "Experimental", "experimental_settings", "logs potoken stream client debug"),
    SettingsEntry("Repeat initial setup", "Experimental", "experimental_settings", "onboarding wizard setup"),
    SettingsEntry("Show where songs play from", "Experimental", "experimental_settings", "source banner toast apple music monochrome youtube"),
    SettingsEntry("Force playback", "Experimental", "experimental_settings", "fallback skip error stream failed youtube"),
)

// a search result wears the icon and colour of the page it opens, as the rows on the settings home do
private fun pageIcon(page: String): Int = when (page) {
    "Customisation" -> R.drawable.palette
    "Lyrics" -> R.drawable.lyrics
    "App behaviour" -> R.drawable.play
    "Audio Lab" -> R.drawable.equalizer
    "Modules" -> R.drawable.library_add
    "Integrations" -> R.drawable.integration
    "Storage" -> R.drawable.storage
    "Experimental" -> R.drawable.tune
    else -> R.drawable.settings
}

private fun pageTint(page: String): Color = when (page) {
    "Customisation" -> Color(0xFF5E5CE6)
    "Lyrics" -> Color(0xFFFF375F)
    "App behaviour" -> Color(0xFF30D158)
    "Audio Lab" -> Color(0xFFFF9F0A)
    "Modules" -> Color(0xFFBF5AF2)
    "Integrations" -> Color(0xFF64D2FF)
    "Experimental" -> Color(0xFFFF453A)
    else -> Color(0xFF8E8E93)
}

private val IosCard = Color(0xFF1C1C1E)
private val IosGray = Color(0xFF8E8E93)
private val IosBlue = Color(0xFF0A84FF)

private val SettingsPages = listOf(
    "Customisation", "Lyrics", "App behaviour", "Audio Lab", "Modules", "Integrations", "Storage", "Experimental",
)

// is this entry a hit for the query? the same fields the search always looked at, in the app's language too
private fun SettingsEntry.matches(q: String, context: android.content.Context): Boolean =
    title.contains(q, ignoreCase = true) ||
        (titleRes ?: LocalizedSettingsTitles[title])?.let { id -> context.getString(id).contains(q, ignoreCase = true) } == true ||
        (pageRes ?: LocalizedSettingsPages[page])?.let { id -> context.getString(id).contains(q, ignoreCase = true) } == true ||
        keywords.contains(q, ignoreCase = true) ||
        page.contains(q, ignoreCase = true)

/**
 * The settings home. It is built on the very same scaffold, search field, grouped lists and animations as every settings group page
 * (large title that folds into the bar, the search pill that turns into a button, rows that fade and fold while you type), so the
 * home and its groups move and look alike.
 */
@Composable
fun LalasiSettingsScreen(navController: NavController) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val (profilePicUri) = rememberPreference(ProfilePicUriKey, "")
    val (username) = rememberPreference(UsernameKey, "")
    var query by remember { mutableStateOf("") }
    val q = query.trim()
    val searching = q.isNotBlank()

    BackHandler(enabled = query.isNotEmpty()) {
        query = ""
        focusManager.clearFocus()
    }

    val chevron = painterResource(R.drawable.navigate_next)
    val chevronTrailing: @Composable () -> Unit = {
        Icon(painter = chevron, contentDescription = null, tint = IosGray, modifier = Modifier.size(20.dp))
    }
    val pagePainters = SettingsPages.associateWith { painterResource(pageIcon(it)) }
    val fallbackPainter = painterResource(R.drawable.settings)

    fun go(route: String) { navController.navigate(route) }

    @Composable
    fun category(titleRes: Int, subRes: Int, icon: Int, tint: Color, route: String) = SettingsItem(
        icon = painterResource(icon),
        iconTint = tint,
        title = { Text(stringResource(titleRes)) },
        descriptionText = stringResource(subRes),
        onClick = { go(route) },
        trailingContent = chevronTrailing,
    )

    // the rows of the search: every entry is built whole and only hidden by isVisible, so rows fold in and out like on the group pages
    val results = SettingsIndex.map { entry ->
        val shownTitle = (entry.titleRes ?: LocalizedSettingsTitles[entry.title])?.let { context.getString(it) } ?: entry.title
        val shownPage = (entry.pageRes ?: LocalizedSettingsPages[entry.page])?.let { context.getString(it) } ?: entry.page
        SettingsItem(
            icon = pagePainters[entry.page] ?: fallbackPainter,
            iconTint = pageTint(entry.page),
            title = { Text(shownTitle) },
            description = { Text(shownPage) },
            isVisible = searching && entry.matches(q, context),
            onClick = {
                SettingsHighlight.pending = entry.highlightKey
                go(entry.route)
            },
            trailingContent = chevronTrailing,
        )
    }
    val nothingFound = searching && results.none { it.isVisible }

    val accountName = username.ifBlank { ProfileStore.GuestName }
    val photo = profilePicUri.ifBlank { null }

    val appearance = category(R.string.lalasi_set_appearance, R.string.lalasi_set_appearance_sub, R.drawable.palette, Color(0xFF5E5CE6), "customisation_settings")
    val playback = category(R.string.lalasi_set_playback, R.string.lalasi_set_playback_sub, R.drawable.play, Color(0xFF30D158), "general_settings")
    val lyrics = category(R.string.lalasi_set_lyrics, R.string.lalasi_set_lyrics_sub, R.drawable.lyrics, Color(0xFFFF375F), "lyrics_settings")
    val audio = category(R.string.lalasi_set_audio, R.string.lalasi_set_audio_sub, R.drawable.equalizer, Color(0xFFFF9F0A), "audio_lab")
    val streaming = category(R.string.lalasi_set_streaming, R.string.lalasi_set_streaming_sub, R.drawable.cloud, IosBlue, "audio_quality")
    val storage = category(R.string.lalasi_set_storage, R.string.lalasi_set_storage_sub, R.drawable.storage, Color(0xFF8E8E93), "storage_settings")
    val integrations = category(R.string.integrations, R.string.lalasi_set_integrations_sub, R.drawable.integration, Color(0xFF64D2FF), IntegrationsRoute)
    val modules = category(R.string.settings_modules_category, R.string.lalasi_set_modules_sub, R.drawable.library_add, Color(0xFFBF5AF2), "modules")

    SubSettingsScaffold(
        title = stringResource(R.string.settings),
        onBack = { navController.navigateUp() },
        searchBar = { SubSettingsSearchBar(query = query, onQueryChange = { query = it }) },
    ) {
        // search results
        FoldingSection(visible = searching) {
            SettingsGroup(style = SettingsGroupStyle.Grouped, items = results)
        }

        FoldingSection(visible = nothingFound) {
            Text(
                text = stringResource(R.string.settings_nothing_matches, query),
                fontFamily = InterFontFamily,
                fontSize = 15.sp,
                color = IosGray,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
            )
        }

        // the profile: the one way back to the profile page (and to About, which lives only there)
        FoldingSection(visible = !searching) {
            val openProfile = {
                // back to the profile page these were opened from, not a second copy of it on top
                if (!navController.popBackStack("settings", inclusive = false)) navController.navigateToTab("settings")
            }
            Column(Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(IosCard)
                        .clickable(onClick = openProfile)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    SearchAvatar(imageUrl = photo, onClick = openProfile, size = 56.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(accountName, fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = Color.White, maxLines = 1)
                        Text("Profil", fontFamily = InterFontFamily, fontSize = 14.sp, color = IosGray, maxLines = 1)
                    }
                    Icon(painter = chevron, contentDescription = null, tint = IosGray, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(22.dp))
            }
        }

        // order follows the LALASI design; each row opens the page that really holds that setting
        FoldingSection(visible = !searching) {
            Column(Modifier.fillMaxWidth()) {
                SettingsGroup(style = SettingsGroupStyle.Grouped, items = listOf(appearance, playback, lyrics))
                Spacer(Modifier.height(22.dp))
                SettingsGroup(style = SettingsGroupStyle.Grouped, items = listOf(audio, streaming, storage))
                Spacer(Modifier.height(22.dp))
                SettingsGroup(style = SettingsGroupStyle.Grouped, items = listOf(integrations, modules))
            }
        }
        Spacer(Modifier.height(140.dp))
    }
}

// a block that folds open and shut with the same 250 ms fade and fold the group pages use for their sections
@Composable
private fun FoldingSection(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(250)) + expandVertically(tween(250)),
        exit = fadeOut(tween(200)) + shrinkVertically(tween(200)),
    ) {
        Column(Modifier.fillMaxWidth()) { content() }
    }
}

/** Kept for the developer playground: the same grouped row, as a stand-alone card. */
@Composable
internal fun CategoryBubble(
    title: String,
    iconRes: Int,
    onClick: () -> Unit,
    subtitle: String? = null,
) {
    val subtitleContent: (@Composable () -> Unit)? = if (subtitle != null) ({ Text(subtitle) }) else null
    SettingsGroup(
        style = SettingsGroupStyle.Grouped,
        items = listOf(
            SettingsItem(
                icon = painterResource(iconRes),
                iconTint = IosBlue,
                title = { Text(title) },
                description = subtitleContent,
                onClick = onClick,
                trailingContent = {
                    Icon(painter = painterResource(R.drawable.navigate_next), contentDescription = null, tint = IosGray, modifier = Modifier.size(20.dp))
                },
            ),
        ),
    )
}
