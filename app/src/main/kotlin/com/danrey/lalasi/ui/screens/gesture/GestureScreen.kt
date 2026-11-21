// GestureScreen.kt

package com.danrey.lalasi.ui.screens.gesture

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.danrey.lalasi.LocalPlayerAwareWindowInsets
import com.danrey.lalasi.LocalPlayerConnection
import com.danrey.lalasi.R
import com.danrey.lalasi.constants.PlayerStyle
import com.danrey.lalasi.constants.PlayerStyleKey
import com.danrey.lalasi.constants.SwipeLeftActionKey
import com.danrey.lalasi.constants.SwipeRightActionKey
import com.danrey.lalasi.constants.ThumbnailCornerRadiusKey
import com.danrey.lalasi.ui.component.DefaultSwipeLeft
import com.danrey.lalasi.ui.component.DefaultSwipeRight
import com.danrey.lalasi.ui.component.HomeCardCornerRadius
import com.danrey.lalasi.ui.component.LocalMenuState
import com.danrey.lalasi.ui.component.LocalPlayerBottomSheetState
import com.danrey.lalasi.ui.component.SheetIcon
import com.danrey.lalasi.ui.component.SheetOptionRow
import com.danrey.lalasi.ui.component.SongSwipeAction
import com.danrey.lalasi.ui.component.bleedHorizontally
import com.danrey.lalasi.ui.screens.audiolab.LabBackButton
import com.danrey.lalasi.ui.screens.audiolab.LabCard
import com.danrey.lalasi.ui.screens.audiolab.LabColors
import com.danrey.lalasi.ui.theme.InterFontFamily
import com.danrey.lalasi.constants.PlayerSwipeDownAnywhereKey
import com.danrey.lalasi.constants.PlayerSwipeUpFollowsKey
import com.danrey.lalasi.ui.screens.audiolab.LabToggleRow
import com.danrey.lalasi.utils.rememberEnumPreference
import com.danrey.lalasi.utils.rememberPreference
import com.danrey.lalasi.ui.utils.stableStatusBars
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import com.danrey.lalasi.ui.component.LocalSettingsScroll
import com.danrey.lalasi.ui.component.SettingsHighlight
import com.danrey.lalasi.ui.component.SettingsPlacement
import com.danrey.lalasi.ui.component.SettingsScroll
import com.danrey.lalasi.ui.component.reveal
import com.danrey.lalasi.ui.component.SubSettingsSearchBar
import com.danrey.lalasi.ui.component.SubSettingsScaffold
import com.danrey.lalasi.ui.component.SettingsGroup
import com.danrey.lalasi.ui.component.SettingsGroupStyle
import com.danrey.lalasi.ui.component.SettingsItem
import com.danrey.lalasi.ui.screens.settings.BottomBarHighlightKey
import com.danrey.lalasi.ui.screens.settings.TopBarHighlightKey
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.filter

private val CardPadding = 20.dp

@Composable
fun GestureScreen(navController: NavController) = CustomisationScreen(navController)

// the customize page's parts in order, with what a search or a settings result can find each one by
private enum class CustomiseSection(@StringRes val titleRes: Int, val keys: List<String>, val words: String) {
    ICONS(R.string.customise_icons, listOf("Icons", "App icon"), "icon icons app launcher home screen lalasi outline anime material you white black dark transparent"),
    ROUNDING(R.string.customise_cover_rounding, listOf("Cover rounding"), "cover rounding corner radius square rounded art"),
    DYNAMIC_ISLAND(R.string.customise_dynamic_island, listOf("Dynamic Island"), "dynamic island pill notch iphone ios top lyrics lirik glass liquid blue black"),
    PLAYER(R.string.customise_player_styles, listOf("Player styles"), "player styles flagship turntable ipod poster timeline lyrics editor customize"),
    BLUR(R.string.appearance_blur, listOf("Blur", "Progressive blur", "Blur quality", "Blur strength"), "blur glass frosted progressive flat quality low high strength px"),
    ELEMENT_BLUR(R.string.customise_element_blur, listOf("Element blur"), "element blur lyrics lines cover zoom titles menus page swipes"),
    ANIMATION(R.string.appearance_animation, listOf("Animation", "Animation speed"), "animation speed motion transitions off faster slower"),
    TOP_BAR(R.string.customise_top_bar, listOf(TopBarHighlightKey), "top bar status bar player clock battery icons hide"),
    BOTTOM_BAR(R.string.customise_bottom_bar, listOf(BottomBarHighlightKey), "bottom bar navigation bar nav buttons gesture handle back home recents player hide"),
    GESTURE(R.string.customise_gesture, listOf("Gesture", "Swipe right", "Swipe left", "Swipe down anywhere to close"), "gesture swipe left right slide songs player close down");

    // the page this section opens on: customisation_detail/<route>
    val route: String get() = name.lowercase()

    // launcher icons only switch on android 8+, where launchers draw adaptive icons
    val available: Boolean get() = this != ICONS || android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O

    // every word typed has to show up in the section's english words, its names, or its title in the app's language
    fun matches(query: String, shownTitle: String): Boolean {
        val words = query.trim().lowercase().split(' ').filter { it.isNotBlank() }
        if (words.isEmpty()) return true
        val haystack = (this.words + " " + keys.joinToString(" ") + " " + shownTitle).lowercase()
        return words.all { it in haystack }
    }
}

// the sections as iOS rows: which icon and tint each one wears in the list
private fun CustomiseSection.rowIcon(): Int = when (this) {
    CustomiseSection.ICONS -> R.drawable.palette
    CustomiseSection.ROUNDING -> R.drawable.crop
    CustomiseSection.PLAYER -> R.drawable.play
    CustomiseSection.DYNAMIC_ISLAND -> R.drawable.lyrics
    CustomiseSection.BLUR -> R.drawable.image
    CustomiseSection.ELEMENT_BLUR -> R.drawable.tune
    CustomiseSection.ANIMATION -> R.drawable.speed
    CustomiseSection.TOP_BAR -> R.drawable.dock_to_top
    CustomiseSection.BOTTOM_BAR -> R.drawable.dock_to_top
    CustomiseSection.GESTURE -> R.drawable.swipe
}

// three inset groups, like iOS Settings: what the app looks like, how it moves, how the player is handled
private val CustomiseGroups: List<List<CustomiseSection>> = listOf(
    listOf(CustomiseSection.ICONS, CustomiseSection.ROUNDING, CustomiseSection.PLAYER, CustomiseSection.DYNAMIC_ISLAND),
    listOf(CustomiseSection.BLUR, CustomiseSection.ELEMENT_BLUR, CustomiseSection.ANIMATION),
    listOf(CustomiseSection.TOP_BAR, CustomiseSection.BOTTOM_BAR, CustomiseSection.GESTURE),
)

const val CustomisationDetailRoute = "customisation_detail"
const val CustomisationDetailPattern = "$CustomisationDetailRoute/{section}"

// the page a section's route name opens, for the settings search and the related links
fun customisationSectionRoute(section: String) = "$CustomisationDetailRoute/$section"

// settings > customize: an iOS list, one row per section. Lyrics has its own page now (lyrics_settings).
@Composable
fun CustomisationScreen(navController: NavController) {
    var query by remember { mutableStateOf("") }
    val titles = CustomiseSection.entries.associateWith { stringResource(it.titleRes) }
    val chevron = painterResource(R.drawable.navigate_next)

    SubSettingsScaffold(
        title = stringResource(R.string.customise_title),
        onBack = { navController.navigateUp() },
        searchBar = { SubSettingsSearchBar(query = query, onQueryChange = { query = it }) },
    ) {
        var any = false
        CustomiseGroups.forEach { group ->
            val rows = group.filter { it.available && it.matches(query, titles.getValue(it)) }
            if (rows.isEmpty()) return@forEach
            any = true
            SettingsGroup(
                style = SettingsGroupStyle.Grouped,
                items = rows.map { section ->
                    SettingsItem(
                        icon = painterResource(section.rowIcon()),
                        title = { Text(titles.getValue(section)) },
                        highlightKey = section.keys.first(),
                        searchText = titles.getValue(section),
                        onClick = { navController.navigate(customisationSectionRoute(section.route)) },
                        trailingContent = {
                            Icon(painter = chevron, contentDescription = null, tint = Color(0xFF8E8E93), modifier = Modifier.size(20.dp))
                        },
                    )
                },
            )
            Spacer(Modifier.height(18.dp))
        }
        val lockLyricsTitle = stringResource(R.string.lock_lyrics_title)
        if (query.isBlank() || lockLyricsTitle.contains(query, ignoreCase = true)) {
            any = true
            SettingsGroup(
                style = SettingsGroupStyle.Grouped,
                items = listOf(
                    SettingsItem(
                        icon = painterResource(R.drawable.lyrics),
                        title = { Text(lockLyricsTitle) },
                        highlightKey = "Lock Screen Lyrics",
                        searchText = lockLyricsTitle,
                        onClick = { navController.navigate(com.danrey.lalasi.ui.screens.settings.LockLyricsSettingsRoute) },
                        trailingContent = {
                            Icon(painter = chevron, contentDescription = null, tint = Color(0xFF8E8E93), modifier = Modifier.size(20.dp))
                        },
                    ),
                ),
            )
            Spacer(Modifier.height(18.dp))
        }
        if (!any) {
            Text(
                text = stringResource(R.string.customise_nothing_matches, query),
                fontFamily = InterFontFamily,
                fontSize = 15.sp,
                color = Color(0xFF8E8E93),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
            )
        }
        Spacer(Modifier.height(140.dp))
    }
}

// one section on a page of its own, inside the same iOS scaffold (back chevron, large title that collapses)
@Composable
fun CustomisationDetailScreen(navController: NavController, sectionRoute: String?) {
    val section = CustomiseSection.entries.firstOrNull { it.route == sectionRoute && it.available }
    if (section == null) {
        LaunchedEffect(Unit) { navController.navigateUp() }
        return
    }
    SubSettingsScaffold(
        title = stringResource(section.titleRes),
        onBack = { navController.navigateUp() },
        backLabel = stringResource(R.string.customise_title),
    ) {
        Box(Modifier.highlightTarget(section.keys)) {
            CustomiseSectionContent(section)
        }
        Spacer(Modifier.height(140.dp))
    }
}

@Composable
private fun CustomiseSectionContent(section: CustomiseSection) {
    // the scaffold's own page margin, so a swipeable pager can run edge to edge
    val pagerBleed = Modifier.bleedHorizontally(16.dp)
    when (section) {
        CustomiseSection.ICONS -> IconsSection()
        CustomiseSection.ROUNDING -> {
            val (coverRadiusPx, onCoverRadiusChange) = rememberPreference(ThumbnailCornerRadiusKey, defaultValue = DefaultCoverRadius)
            CoverRoundingSection(
                radiusPx = coverRadiusPx,
                onRadiusChange = { next ->
                    onCoverRadiusChange(next)
                    HomeCardCornerRadius = next.dp
                },
            )
        }
        CustomiseSection.PLAYER -> PlayerSection()
        CustomiseSection.DYNAMIC_ISLAND -> DynamicIslandSection()
        CustomiseSection.BLUR -> BlurSection()
        CustomiseSection.ELEMENT_BLUR -> ElementBlurSection()
        CustomiseSection.ANIMATION -> AnimationSection()
        CustomiseSection.TOP_BAR -> TopBarSection()
        CustomiseSection.BOTTOM_BAR -> BottomBarSection()
        CustomiseSection.GESTURE -> GestureSection()
    }
}

// the player has its own editor, so its card opens that on the player itself
@Composable
private fun PlayerSection() {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val playerSheetState = LocalPlayerBottomSheetState.current
    val playerStyle by rememberEnumPreference(PlayerStyleKey, defaultValue = PlayerStyle.FLAGSHIP)
    val playFirst = stringResource(R.string.customise_play_first)
    PlayerStyleCard(
        selected = playerStyle,
        onOpenEditor = {
            val hasMedia = playerConnection?.player?.currentMediaItem != null
            if (hasMedia) {
                playerSheetState?.expandSoft()
                playerConnection?.requestPlayerStudio()
            } else {
                Toast.makeText(context, playFirst, Toast.LENGTH_SHORT).show()
            }
        },
    )
}

@Composable
private fun GestureSection() {
    val (right, setRight) = rememberEnumPreference(SwipeRightActionKey, defaultValue = DefaultSwipeRight)
    val (left, setLeft) = rememberEnumPreference(SwipeLeftActionKey, defaultValue = DefaultSwipeLeft)
    Column {
        Text(
            text = stringResource(R.string.customise_gesture),
            style = TextStyle(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                letterSpacing = (-0.05).em,
            ),
            color = Color.White,
        )
        Spacer(Modifier.height(14.dp))
        SwipeCard(
            title = stringResource(R.string.customise_swipe_right),
            subtitle = stringResource(R.string.customise_swipe_right_desc),
            toRight = true,
            action = right,
            onPick = setRight,
        )
        Spacer(Modifier.height(14.dp))
        SwipeCard(
            title = stringResource(R.string.customise_swipe_left),
            subtitle = stringResource(R.string.customise_swipe_left_desc),
            toRight = false,
            action = left,
            onPick = setLeft,
        )
        Spacer(Modifier.height(14.dp))
        PlayerGestureCard()
    }
}

// a section a search result or a related row points at scrolls into view and its outline flashes once
@Composable
private fun Modifier.highlightTarget(keys: List<String>): Modifier {
    val requester = remember { BringIntoViewRequester() }
    val flash = remember { Animatable(0f) }
    val settingsScroll = LocalSettingsScroll.current
    val density = LocalDensity.current
    val placement = remember { SettingsPlacement() }
    LaunchedEffect(keys) {
        // watched, not keyed on: clearing the request restarted the effect and cut it off before it scrolled
        snapshotFlow { SettingsHighlight.pending }.filter { it in keys }.collect {
            SettingsHighlight.pending = null
            // let the page finish opening first, or it scrolls while still sliding in
            delay(320)
            val target = placement.coords
            if (settingsScroll != null && target != null) settingsScroll.reveal(target, density) else requester.bringIntoView()
            flash.animateTo(1f, tween(220))
            flash.animateTo(0f, tween(900))
        }
    }
    return this
        .bringIntoViewRequester(requester)
        .onPlaced { placement.coords = it }
        .drawWithContent {
            drawContent()
            val a = flash.value
            if (a > 0f) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.9f * a),
                    cornerRadius = CornerRadius(22.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }
}

@Composable
private fun SwipeCard(
    title: String,
    subtitle: String,
    toRight: Boolean,
    action: SongSwipeAction,
    onPick: (SongSwipeAction) -> Unit,
) {
    val sheets = LocalMenuState.current
    LabCard(padding = CardPadding) {
        Text(
            title,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            letterSpacing = (-0.02).em,
            color = LabColors.Primary,
        )
        Text(
            subtitle,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = LabColors.Secondary,
            modifier = Modifier.padding(top = 2.dp),
        )
        Spacer(Modifier.height(20.dp))
        // the demo runs edge to edge, like the mock: the row slides off the card's side
        SwipePreview(action, toRight, Modifier.bleedHorizontally(CardPadding))
        Spacer(Modifier.height(20.dp))
        ActionDropdown(action) {
            sheets.show {
                ActionPicker(selected = action) { picked ->
                    onPick(picked)
                    sheets.dismiss()
                }
            }
        }
    }
}

@Composable
private fun ActionDropdown(action: SongSwipeAction, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(LabColors.Row)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
    ) {
        ActionOptionBadge(action = action)
        Crossfade(targetState = action, animationSpec = tween(180), modifier = Modifier.weight(1f), label = "swipeAction") { shown ->
            Text(
                shown.title,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                letterSpacing = (-0.01).em,
                color = LabColors.Primary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Icon(
            painter = painterResource(R.drawable.expand_more),
            contentDescription = null,
            tint = LabColors.Secondary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ActionPicker(selected: SongSwipeAction, onPick: (SongSwipeAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
        SongSwipeAction.entries.forEach { option ->
            SheetOptionRow(
                title = option.title,
                onClick = { onPick(option) },
                leading = { ActionOptionBadge(action = option) },
                trailing = {
                    if (option == selected) SheetIcon(R.drawable.check, size = 18.dp)
                },
            )
        }
    }
}

// colored circle badge so each gesture option shows its pill color in the selector and sheet
@Composable
private fun ActionOptionBadge(action: SongSwipeAction) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(action.color.copy(alpha = 0.22f)),
    ) {
        Icon(
            painter = painterResource(action.icon),
            contentDescription = null,
            tint = action.color,
            modifier = Modifier.size(16.dp),
        )
    }
}

/**
 * The open player's own gestures. Out of the box only its top strip and its cover close it with a
 * swipe down, so a crooked swipe over the controls can't throw it away; this brings back closing
 * it from anywhere for those who'd rather not reach for the top of a tall phone (#121).
 */
@Composable
private fun PlayerGestureCard() {
    val (anywhere, setAnywhere) = rememberPreference(PlayerSwipeDownAnywhereKey, defaultValue = false)
    val (follows, setFollows) = rememberPreference(PlayerSwipeUpFollowsKey, defaultValue = true)
    LabCard(padding = CardPadding) {
        Text(
            stringResource(R.string.customise_player_gestures),
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            letterSpacing = (-0.02).em,
            color = LabColors.Primary,
        )
        Text(
            stringResource(R.string.customise_player_gestures_desc),
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = LabColors.Secondary,
            modifier = Modifier.padding(top = 2.dp),
        )
        Spacer(Modifier.height(14.dp))
        LabToggleRow(
            title = stringResource(R.string.customise_swipe_down_anywhere),
            subtitle = stringResource(R.string.customise_swipe_down_anywhere_desc),
            checked = anywhere,
            onCheckedChange = setAnywhere,
        )
        LabToggleRow(
            title = stringResource(R.string.customise_swipe_up_follows),
            subtitle = stringResource(R.string.customise_swipe_up_follows_desc),
            checked = follows,
            onCheckedChange = setFollows,
        )
    }
}


// Dynamic Island: on/off, then the three looks as an iOS list with a tick on the chosen one
@Composable
private fun DynamicIslandSection() {
    val (enabled, setEnabled) = rememberPreference(com.danrey.lalasi.constants.DynamicIslandEnabledKey, defaultValue = false)
    val (style, setStyle) = rememberEnumPreference(com.danrey.lalasi.constants.DynamicIslandStyleKey, defaultValue = com.danrey.lalasi.constants.DynamicIslandStyle.DEFAULT)
    val (pillLyrics, setPillLyrics) = rememberPreference(com.danrey.lalasi.constants.DynamicIslandPillLyricsKey, defaultValue = true)
    val (cardLyrics, setCardLyrics) = rememberPreference(com.danrey.lalasi.constants.DynamicIslandCardLyricsKey, defaultValue = true)
    val (wordLit, setWordLit) = rememberPreference(com.danrey.lalasi.constants.DynamicIslandWordLitKey, defaultValue = true)
    val (swipe, setSwipe) = rememberPreference(com.danrey.lalasi.constants.DynamicIslandSwipeKey, defaultValue = true)
    val (shapeStyle, setShapeStyle) = rememberEnumPreference(com.danrey.lalasi.constants.DynamicIslandShapeKey, defaultValue = com.danrey.lalasi.constants.DynamicIslandShape.ISLAND)
    val (cameraOn, setCameraOn) = rememberPreference(com.danrey.lalasi.constants.DynamicIslandCameraKey, defaultValue = true)
    val (gapSaved, setGap) = rememberPreference(com.danrey.lalasi.constants.DynamicIslandCameraGapKey, defaultValue = com.danrey.lalasi.constants.DynamicIslandCameraGapAuto)
    val tick = painterResource(R.drawable.check)
    val context = LocalContext.current
    // "over other apps" is granted in system settings, so it is read again every time this page comes back
    var overlayAllowed by remember { mutableStateOf(canDrawOverApps(context)) }
    // accessibility access is granted on a system page too, and read again the same way
    var accessAllowed by remember { mutableStateOf(com.danrey.lalasi.playback.LockLyricsBus.isAccessGranted(context)) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                overlayAllowed = canDrawOverApps(context)
                accessAllowed = com.danrey.lalasi.playback.LockLyricsBus.isAccessGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val looks = listOf(
        Triple(com.danrey.lalasi.constants.DynamicIslandStyle.DEFAULT, R.string.dynamic_island_style_default, Color.Black),
        Triple(com.danrey.lalasi.constants.DynamicIslandStyle.LIQUID_GLASS, R.string.dynamic_island_style_glass, Color(0xFF3A3F4A)),
        Triple(com.danrey.lalasi.constants.DynamicIslandStyle.BLUE, R.string.dynamic_island_style_blue, Color(0xFF0A84FF)),
    )
    Column {
        // the island only lives outside the app; here is how it looks, in the chosen style
        val previewCover = LocalPlayerConnection.current?.mediaMetadata?.collectAsState()?.value?.thumbnailUrl
        com.danrey.lalasi.ui.component.DynamicIslandPreview(style, Modifier.padding(horizontal = 16.dp), previewCover, shapeStyle)
        Spacer(Modifier.height(18.dp))
        SettingsGroup(
            style = SettingsGroupStyle.Grouped,
            items = listOf(
                SettingsItem(
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.dynamic_island_enable)) },
                    descriptionText = stringResource(R.string.dynamic_island_enable_desc),
                    highlightKey = "Dynamic Island",
                    onClick = {
                        setEnabled(!enabled)
                        if (!enabled && !overlayAllowed && !accessAllowed) openAccessSettings(context)
                    },
                    trailingContent = {
                        com.danrey.lalasi.ui.component.AppSwitch(
                            checked = enabled,
                            onCheckedChange = {
                                setEnabled(it)
                                if (it && !overlayAllowed && !accessAllowed) openAccessSettings(context)
                            },
                        )
                    },
                ),
                SettingsItem(
                    icon = painterResource(R.drawable.tune),
                    title = { Text(stringResource(R.string.dynamic_island_access)) },
                    descriptionText = stringResource(R.string.dynamic_island_access_desc),
                    enabled = enabled,
                    onClick = { openAccessSettings(context) },
                    trailingContent = {
                        Text(
                            stringResource(if (accessAllowed) R.string.dynamic_island_on else R.string.dynamic_island_off),
                            color = if (accessAllowed) Color(0xFF30D158) else Color(0xFF8E8E93),
                            fontSize = 15.sp,
                        )
                    },
                ),
                SettingsItem(
                    icon = painterResource(R.drawable.image),
                    title = { Text(stringResource(R.string.dynamic_island_overlay)) },
                    descriptionText = stringResource(R.string.dynamic_island_overlay_desc),
                    enabled = enabled,
                    onClick = { openOverlaySettings(context) },
                    trailingContent = {
                        Text(
                            stringResource(if (overlayAllowed) R.string.dynamic_island_on else R.string.dynamic_island_off),
                            color = if (overlayAllowed) Color(0xFF30D158) else Color(0xFF8E8E93),
                            fontSize = 15.sp,
                        )
                    },
                ),
            ),
        )
        // a phone with its own island keeps it as it is; LALASI's stays hidden unless asked for
        val systemIsland = com.danrey.lalasi.utils.SystemIsland.name
        if (systemIsland != null) {
            val (force, setForce) = rememberPreference(com.danrey.lalasi.constants.DynamicIslandForceKey, defaultValue = false)
            Text(
                text = stringResource(R.string.dynamic_island_system_note, systemIsland),
                fontFamily = InterFontFamily,
                fontSize = 13.sp,
                color = Color(0xFF8E8E93),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            SettingsGroup(
                style = SettingsGroupStyle.Grouped,
                items = listOf(
                    SettingsItem(
                        title = { Text(stringResource(R.string.dynamic_island_force)) },
                        enabled = enabled,
                        onClick = { setForce(!force) },
                        trailingContent = { com.danrey.lalasi.ui.component.AppSwitch(checked = force, onCheckedChange = setForce, enabled = enabled) },
                    ),
                ),
            )
        }
        Spacer(Modifier.height(18.dp))
        // island or notch
        SettingsGroup(
            title = stringResource(R.string.dynamic_island_group_shape),
            style = SettingsGroupStyle.Grouped,
            items = listOf(
                com.danrey.lalasi.constants.DynamicIslandShape.ISLAND to R.string.dynamic_island_shape_island,
                com.danrey.lalasi.constants.DynamicIslandShape.NOTCH to R.string.dynamic_island_shape_notch,
            ).map { (value, titleRes) ->
                SettingsItem(
                    title = { Text(stringResource(titleRes)) },
                    descriptionText = stringResource(
                        if (value == com.danrey.lalasi.constants.DynamicIslandShape.ISLAND) R.string.dynamic_island_shape_island_desc
                        else R.string.dynamic_island_shape_notch_desc,
                    ),
                    enabled = enabled,
                    onClick = { setShapeStyle(value) },
                    trailingContent = {
                        if (shapeStyle == value) Icon(painter = tick, contentDescription = null, tint = Color(0xFF0A84FF), modifier = Modifier.size(20.dp))
                    },
                )
            },
        )
        Spacer(Modifier.height(18.dp))
        SettingsGroup(
            title = stringResource(R.string.dynamic_island_group_lyrics),
            style = SettingsGroupStyle.Grouped,
            items = listOf(
                SettingsItem(
                    title = { Text(stringResource(R.string.dynamic_island_pill_lyrics)) },
                    enabled = enabled,
                    onClick = { setPillLyrics(!pillLyrics) },
                    trailingContent = { com.danrey.lalasi.ui.component.AppSwitch(checked = pillLyrics, onCheckedChange = setPillLyrics, enabled = enabled) },
                ),
                SettingsItem(
                    title = { Text(stringResource(R.string.dynamic_island_card_lyrics)) },
                    enabled = enabled,
                    onClick = { setCardLyrics(!cardLyrics) },
                    trailingContent = { com.danrey.lalasi.ui.component.AppSwitch(checked = cardLyrics, onCheckedChange = setCardLyrics, enabled = enabled) },
                ),
                SettingsItem(
                    title = { Text(stringResource(R.string.dynamic_island_word_lit)) },
                    enabled = enabled && (pillLyrics || cardLyrics),
                    onClick = { setWordLit(!wordLit) },
                    trailingContent = { com.danrey.lalasi.ui.component.AppSwitch(checked = wordLit, onCheckedChange = setWordLit, enabled = enabled && (pillLyrics || cardLyrics)) },
                ),
            ),
        )
        Spacer(Modifier.height(18.dp))
        // the line flows around the front camera
        val autoGap = gapSaved < 0
        SettingsGroup(
            title = stringResource(R.string.dynamic_island_group_camera),
            style = SettingsGroupStyle.Grouped,
            items = listOf(
                SettingsItem(
                    title = { Text(stringResource(R.string.dynamic_island_camera)) },
                    descriptionText = stringResource(R.string.dynamic_island_camera_desc),
                    enabled = enabled,
                    onClick = { setCameraOn(!cameraOn) },
                    trailingContent = { com.danrey.lalasi.ui.component.AppSwitch(checked = cameraOn, onCheckedChange = setCameraOn, enabled = enabled) },
                ),
                SettingsItem(
                    title = { Text(stringResource(R.string.dynamic_island_camera_auto)) },
                    descriptionText = stringResource(R.string.dynamic_island_camera_auto_desc),
                    enabled = enabled && cameraOn,
                    onClick = {
                        // leaving auto starts the manual value from what the phone reports
                        if (autoGap) setGap(com.danrey.lalasi.ui.component.islandCameraGapDp(context, -1).value.toInt()) else setGap(com.danrey.lalasi.constants.DynamicIslandCameraGapAuto)
                    },
                    trailingContent = {
                        com.danrey.lalasi.ui.component.AppSwitch(
                            checked = autoGap,
                            onCheckedChange = {
                                if (it) setGap(com.danrey.lalasi.constants.DynamicIslandCameraGapAuto)
                                else setGap(com.danrey.lalasi.ui.component.islandCameraGapDp(context, -1).value.toInt())
                            },
                            enabled = enabled && cameraOn,
                        )
                    },
                ),
                SettingsItem(
                    title = { Text(stringResource(R.string.dynamic_island_camera_gap)) },
                    descriptionText = stringResource(R.string.dynamic_island_camera_gap_desc),
                    enabled = enabled && cameraOn && !autoGap,
                    trailingContent = {
                        IslandOffsetStepper(
                            value = if (autoGap) com.danrey.lalasi.ui.component.islandCameraGapDp(context, -1).value.toInt() else gapSaved,
                            enabled = enabled && cameraOn && !autoGap,
                            signed = false,
                            onChange = { setGap(it.coerceIn(8, 120)) },
                        )
                    },
                ),
            ),
        )
        Spacer(Modifier.height(18.dp))
        SettingsGroup(
            title = stringResource(R.string.dynamic_island_group_gesture),
            style = SettingsGroupStyle.Grouped,
            items = listOf(
                SettingsItem(
                    title = { Text(stringResource(R.string.dynamic_island_swipe)) },
                    enabled = enabled,
                    onClick = { setSwipe(!swipe) },
                    trailingContent = { com.danrey.lalasi.ui.component.AppSwitch(checked = swipe, onCheckedChange = setSwipe, enabled = enabled) },
                ),
            ),
        )
        Spacer(Modifier.height(18.dp))
        val (savedOffset, setOffset) = rememberPreference(com.danrey.lalasi.constants.DynamicIslandOffsetKey, defaultValue = com.danrey.lalasi.constants.DynamicIslandOffsetDefault)
        // kept on the screen: from the very top down to where the open card still fits
        val offsetRange = remember(context, shapeStyle) { com.danrey.lalasi.ui.component.islandOffsetRangeDp(context, shapeStyle) }
        val offset = savedOffset.coerceIn(offsetRange.first, offsetRange.last)
        val screenDensity = context.resources.displayMetrics.density
        val (screenW, screenH) = remember(context) { com.danrey.lalasi.ui.component.islandScreenPx(context) }
        // this is where the island sits: it is drawn outside the app, so the page shows it on a little phone
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            com.danrey.lalasi.ui.component.DynamicIslandPositionPreview(
                style = style,
                offsetDp = offset,
                statusBarDp = com.danrey.lalasi.ui.component.islandStatusBarPx(context) / screenDensity,
                screenWidthDp = screenW / screenDensity,
                screenHeightDp = screenH / screenDensity,
                coverUrl = previewCover,
                shapeStyle = shapeStyle,
                cameraGapDp = if (cameraOn) com.danrey.lalasi.ui.component.islandCameraGapDp(context, gapSaved).value else 0f,
            )
        }
        Text(
            text = stringResource(R.string.dynamic_island_position_hint),
            fontFamily = InterFontFamily,
            fontSize = 13.sp,
            color = Color(0xFF8E8E93),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
        )
        SettingsGroup(
            title = stringResource(R.string.dynamic_island_group_position),
            style = SettingsGroupStyle.Grouped,
            items = listOf(
                SettingsItem(
                    title = { Text(stringResource(R.string.dynamic_island_position)) },
                    descriptionText = stringResource(R.string.dynamic_island_position_desc),
                    enabled = enabled,
                    trailingContent = {
                        IslandOffsetStepper(
                            value = offset,
                            enabled = enabled,
                            onChange = { setOffset(it.coerceIn(offsetRange.first, offsetRange.last)) },
                        )
                    },
                ),
                SettingsItem(
                    title = { Text(stringResource(R.string.dynamic_island_position_reset)) },
                    enabled = enabled && offset != com.danrey.lalasi.constants.DynamicIslandOffsetDefault,
                    onClick = { setOffset(com.danrey.lalasi.constants.DynamicIslandOffsetDefault) },
                ),
            ),
        )
        Spacer(Modifier.height(18.dp))
        SettingsGroup(
            title = stringResource(R.string.dynamic_island_style),
            style = SettingsGroupStyle.Grouped,
            items = looks.map { (value, titleRes, swatch) ->
                SettingsItem(
                    title = { Text(stringResource(titleRes)) },
                    enabled = enabled,
                    onClick = { setStyle(value) },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(width = 44.dp, height = 18.dp).clip(CircleShape).background(swatch).border(0.5.dp, Color.White.copy(alpha = 0.3f), CircleShape))
                            if (style == value) {
                                Spacer(Modifier.width(10.dp))
                                Icon(painter = tick, contentDescription = null, tint = Color(0xFF0A84FF), modifier = Modifier.size(20.dp))
                            }
                        }
                    },
                )
            },
        )
    }
}


// minus / value / plus: one dp per tap, and holding a button keeps moving it (the page keeps it on the screen).
@Composable
private fun IslandOffsetStepper(value: Int, enabled: Boolean, onChange: (Int) -> Unit, signed: Boolean = true) {
    val ink = if (enabled) Color(0xFF0A84FF) else Color(0xFF8E8E93)
    val current by androidx.compose.runtime.rememberUpdatedState(value)
    val change by androidx.compose.runtime.rememberUpdatedState(onChange)
    @Composable
    fun StepButton(label: String, step: Int) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f))
                .pointerInput(enabled, step) {
                    if (!enabled) return@pointerInput
                    detectTapGestures(
                        onPress = {
                            change(current + step)
                            coroutineScope {
                                val repeat = launch {
                                    delay(400)
                                    var n = 0
                                    while (true) {
                                        // the longer it is held, the bigger the jump
                                        change(current + step * (if (n < 15) 1 else if (n < 40) 3 else 8))
                                        n++
                                        delay(45)
                                    }
                                }
                                tryAwaitRelease()
                                repeat.cancel()
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) { Text(label, color = ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold) }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepButton("−", -1)
        Text(
            text = (if (signed && value > 0) "+$value" else "$value") + " dp",
            color = Color(0xFF8E8E93),
            fontFamily = InterFontFamily,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 64.dp),
        )
        StepButton("+", 1)
    }
}

// the system page where the LALASI accessibility service is switched on (it draws the island with no overlay permission)
private fun openAccessSettings(context: android.content.Context) {
    runCatching { context.startActivity(com.danrey.lalasi.playback.LockLyricsBus.accessSettingsIntent()) }
}

private fun canDrawOverApps(context: android.content.Context) =
    android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M || android.provider.Settings.canDrawOverlays(context)

// the page where "display over other apps" is switched on for LALASI
private fun openOverlaySettings(context: android.content.Context) {
    runCatching {
        context.startActivity(
            android.content.Intent(
                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:${context.packageName}"),
            ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
