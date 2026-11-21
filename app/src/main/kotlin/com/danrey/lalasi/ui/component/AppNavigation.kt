// AppNavigation.kt

package com.danrey.lalasi.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.danrey.lalasi.ui.screens.Screens
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.size
import com.danrey.lalasi.LocalGlassState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.geometry.CornerRadius as BubbleCornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke as BubbleStroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext

@Immutable
private data class NavItemState(
    val isSelected: Boolean,
    val iconRes: Int
)

/**
 * Navigates to a top-level tab exactly the way the navigation bar does.
 *
 * Anything that jumps to a tab from inside another tab MUST use this rather than a plain
 * `navigate(route)`, and the reason is subtle enough to be worth spelling out.
 *
 * A plain `navigate("settings")` from the profile menu pushes Settings on top of the *Home* tab's
 * stack, because that is where you were standing. Tapping Home in the bar then runs
 * `popUpTo(startDestination) { saveState = true }`, which sweeps that pushed Settings entry into
 * the state saved *for Home* — and `restoreState = true` on the very same call immediately puts it
 * back. The result is that tapping Home lands you on Settings, which is precisely the bug this
 * fixes, and it looks like the bar is ignoring you when in fact it is faithfully restoring a stack
 * that Settings should never have been part of.
 *
 * Going through here instead means a tab is always entered *as* a tab, so it never becomes a child
 * of whichever tab happened to be on screen when you left.
 */
fun NavController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * a tap on a tab. another tab: switch to it, back on the page you left it on. the tab you're in:
 * from one of its pages, back to the tab's own page (as every tab bar does); already there, the
 * page scrolls to its top.
 */
fun NavController.onTabTapped(screen: Screens, isSelectedTab: Boolean, scrollToTop: () -> Unit) {
    if (!isSelectedTab) {
        navigateToTab(screen.route)
        return
    }
    if (currentDestination?.route == screen.route) {
        currentBackStackEntry?.savedStateHandle?.set("scrollToTop", true)
        scrollToTop()
        return
    }
    if (!popBackStack(screen.route, inclusive = false)) navigateToTab(screen.route)
}

@Stable
private fun isRouteSelected(currentRoute: String?, screenRoute: String, navigationItems: List<Screens>): Boolean {
    if (currentRoute == null) return false
    if (currentRoute == screenRoute) return true
    return navigationItems.any { it.route == screenRoute } &&
           currentRoute.startsWith("$screenRoute/")
}

private val RouteOwners: List<Pair<String, String>> = listOf(
    "customisation_detail/" to "settings",
    "search/" to "search_input",
    "genre/" to "search_input",
    "youtube_browse/" to "search_input",
    "browse/" to "search_input",
    "library/" to "library",
    "cipher_settings" to "settings",
    "playback_diagnostics" to "settings",
    "lalasi_settings" to "settings",
    "general_settings" to "settings",
    "experimental_settings" to "settings",
    "developer_space" to "settings",
    "developer_playground" to "settings",
    "storage_settings" to "settings",
    "import_sync" to "settings",
    "import_providers" to "settings",
    "import_tunemymusic" to "settings",
    "import_progress" to "settings",
    "youtube_sync" to "settings",
    "youtube_login" to "settings",
    "account_import/" to "settings",
    "equalizer" to "settings",
)

private fun owningTabRoute(currentRoute: String?, navigationItems: List<Screens>): String? {
    if (currentRoute == null) return null
    navigationItems.firstOrNull { isRouteSelected(currentRoute, it.route, navigationItems) }
        ?.let { return it.route }
    RouteOwners.firstOrNull { (prefix, _) ->
        currentRoute == prefix || currentRoute.startsWith(prefix)
    }?.let { return it.second }

    // Catch-all so RouteOwners does not have to stay exhaustive: every settings sub-screen is
    // owned by the settings tab without anyone having to list it. Only a fallback now; the back
    // stack decides the tab whenever it has a tab page in it (see owningTab).
    if (currentRoute.endsWith("_settings") || currentRoute.startsWith("settings")) return "settings"
    return null
}

/**
 * the tab the page on screen belongs to: the nearest tab page under it in the back stack.
 *
 * the bar used to remember the last tab it could name from the current route alone, and keep it
 * for routes no tab claims (albums, playlists, artists). that went wrong two ways: switching back
 * to a tab restores the page you left it on, so coming back to the Library's album kept Home lit;
 * and the remembered value was dropped whenever the bar left the screen (the player's full view,
 * the recap), so it came back as Home. the back stack always knows: a tab is entered with
 * [navigateToTab], so every page sits above the tab page it was opened from.
 */
fun owningTab(backStack: List<NavBackStackEntry>, navigationItems: List<Screens>): String {
    val tabRoutes = navigationItems.mapTo(HashSet()) { it.route }
    backStack.asReversed().firstOrNull { it.destination.route in tabRoutes }
        ?.destination?.route
        ?.let { return it }
    // a stack without a tab page under it (a deep link): go by the route, else the first tab
    return owningTabRoute(backStack.lastOrNull()?.destination?.route, navigationItems)
        ?: navigationItems.firstOrNull()?.route.orEmpty()
}

/**
 * which tab each page belongs to, kept by entry so a page that has already left the stack (the
 * one a tab switch saved away) can still be placed. the tab slide reads this: an album open in
 * the Library slides like the Library, not like a page no tab owns.
 */
class TabOwnership(private val navigationItems: List<Screens>) {
    private val tabRoutes = navigationItems.map { it.route }
    private val owners = HashMap<String, String>()

    fun update(backStack: List<NavBackStackEntry>) {
        var owner: String? = null
        backStack.forEach { entry ->
            val route = entry.destination.route
            if (route != null && route in tabRoutes) owner = route
            owner?.let { owners[entry.id] = it }
        }
        // saved tab stacks keep their entries' ids, so only trim when it has clearly grown stale
        if (owners.size > 400) {
            val live = backStack.mapTo(HashSet()) { it.id }
            owners.keys.retainAll(live)
        }
    }

    /** the tab's position in the bar, or -1 when nothing places it */
    fun indexOf(entry: NavBackStackEntry, backStack: List<NavBackStackEntry>): Int {
        update(backStack)
        val owner = owners[entry.id] ?: entry.destination.route?.takeIf { it in tabRoutes }
            ?: owningTabRoute(entry.destination.route, navigationItems)
        return tabRoutes.indexOf(owner)
    }
}

@Composable
fun AppNavigationRail(
    navigationItems: List<Screens>,
    currentRoute: String?,
    /** from [owningTab]: the tab the page on screen belongs to */
    selectedTabRoute: String,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    /** a second quick tap on Search: straight into typing */
    onSearchDoubleTap: (() -> Unit)? = null
) {
    val containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
    val viewConfiguration = LocalViewConfiguration.current

    NavigationRail(
        modifier = modifier,
        containerColor = containerColor
    ) {
        Spacer(modifier = Modifier.weight(1f))

        navigationItems.forEach { screen ->
            val isSelected = screen.route == selectedTabRoute
            val iconRes = remember(isSelected, screen) {
                if (isSelected) screen.iconIdActive else screen.iconIdInactive
            }

            val lastTap = remember { longArrayOf(0L) }

            NavigationRailItem(
                selected = isSelected,
                onClick = {
                    val doubleTap = onSearchDoubleTap.takeIf { screen == Screens.Search }
                    tabTapped(lastTap, viewConfiguration.doubleTapTimeoutMillis, doubleTap) {
                        onItemClick(screen, isSelected)
                    }
                },
                icon = {
                    Icon(
                        painter = coil3.compose.rememberAsyncImagePainter(model = iconRes),
                        contentDescription = stringResource(screen.titleId),
                        modifier = Modifier.size(28.dp)
                    )
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

/**
 * How far the capsule has shrunk for the page's scrolling, 0 (full size) to 1 (smallest).
 *
 * It is driven by the scroll the pages actually consumed (read after the child has scrolled, so it
 * never takes any of it and cannot fight a page's own scrolling): scrolling down shrinks the
 * capsule in step with the finger, scrolling up grows it back. No timers, and the bar's layout
 * size never changes, so it cannot move the mini player or the content.
 */
@Stable
class NavBarScrollState(private val travelPx: Float) : NestedScrollConnection {
    /** 0 = whole bar, 1 = collapsed beside the mini player; animated, never tracking the finger pixel by pixel */
    var collapse by mutableFloatStateOf(0f)
        private set

    /** how fast the collapse is moving right now (per second); drives the bar's motion blur, 0 when still */
    var motion by mutableFloatStateOf(0f)
        private set

    /** the bar only collapses beside a mini player; with none showing, scrolling leaves it whole */
    var canCollapse: Boolean = true

    /** runs the snap animation; set by the host */
    var scope: kotlinx.coroutines.CoroutineScope? = null

    private var target = 0f
    private var job: kotlinx.coroutines.Job? = null

    // scroll gathered in the current direction; a little of it is enough to flip the bar
    private var pending = 0f
    private var pendingDirection = 0

    // about 14dp of scrolling (travelPx is 220dp)
    private val triggerPx = travelPx * 0.065f

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        val dy = consumed.y
        if (!canCollapse || dy == 0f) return Offset.Zero
        // content moving up (reading further down) is a negative delta
        val direction = if (dy < 0f) 1 else -1
        if (direction != pendingDirection) {
            pendingDirection = direction
            pending = 0f
        }
        pending += kotlin.math.abs(dy)
        if (pending >= triggerPx) {
            pending = 0f
            val next = if (direction > 0) 1f else 0f
            if (next != target) snapTo(next)
        }
        return Offset.Zero
    }

    /** go all the way to [value] at once, with a spring, instead of following the scroll */
    private fun snapTo(value: Float) {
        target = value
        job?.cancel()
        val host = scope
        if (host == null) {
            collapse = value
            return
        }
        job = host.launch {
            androidx.compose.animation.core.animate(
                initialValue = collapse,
                targetValue = value,
                animationSpec = NavScrollElasticSpring,
            ) { v, velocity ->
                collapse = v
                motion = kotlin.math.abs(velocity)
            }
            motion = 0f
        }
    }

    /** a tab change brings the bar back to full size */
    fun reset() {
        job?.cancel()
        target = 0f
        pending = 0f
        collapse = 0f
        motion = 0f
    }

    /** the bar grows back with a spring instead of jumping (tab change, tap on the Home circle) */
    suspend fun expandAnimated() {
        job?.cancel()
        target = 0f
        pending = 0f
        if (collapse == 0f) return
        androidx.compose.animation.core.animate(
            initialValue = collapse,
            targetValue = 0f,
            animationSpec = NavScrollElasticSpring,
        ) { v, velocity ->
            collapse = v
            motion = kotlin.math.abs(velocity)
        }
        motion = 0f
    }
}

/**
 * The bar's and the mini player's snap: an under-damped spring, so it overshoots its target a little and
 * settles back. The layout always uses the clamped value (see [navCollapseEase]); the overshoot is shown
 * only in the draw phase (squash and stretch), so the bounce never costs a relayout.
 */
val NavScrollElasticSpring = spring<Float>(dampingRatio = 0.56f, stiffness = 400f)

/** How far the scroll spring is past 0..1 right now: positive beyond collapsed, negative beyond whole. */
fun navCollapseOvershoot(raw: Float): Float = raw - raw.coerceIn(0f, 1f)

/**
 * The mini player's follow of the bar: the clamped ease plus a share of the spring's overshoot, so it
 * rides the same bounce. [follow] is how much of the overshoot it takes (the move is in px or dp of its own).
 */
fun navCollapseElastic(raw: Float, follow: Float): Float = navCollapseEase(raw) + navCollapseOvershoot(raw) * follow

/** One easing for the bar and the mini player, so the two always agree on how far they have moved. */
fun navCollapseEase(c: Float): Float {
    val x = c.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

/**
 * How far the mini player's left edge moves right when the bar is fully collapsed: the bar's
 * margin, the Home circle and the gap between, less the 24dp the mini player already sits in from the edge.
 * Read in layout and draw by the mini player (see BottomSheetPlayer); provided by MainActivity.
 */
val MiniPlayerCollapsedShift = 22.dp + 60.dp + 8.dp - 14.dp

/**
 * How far down (dp) the mini player drops when the bar is fully collapsed, so it sits level with the
 * Home circle instead of floating above the bar: the 80dp the bar reserves, less the 8dp under the pill.
 */
val MiniPlayerCollapsedDrop = 83.dp

/** px the mini player drops while the bar collapses beside it; 0 when nothing provides it */
val LocalMiniNavDrop = staticCompositionLocalOf<() -> Float> { { 0f } }

/** px the mini player's left edge is pushed right by the collapsed bar; 0 when nothing provides it */
val LocalMiniNavShift = staticCompositionLocalOf<() -> Float> { { 0f } }

/** The bar itself, a stadium: 14dp gap above, 74dp bar, 16dp lift below = the 104dp the layout reserves. */
private val NavBarHeight = 74.dp
private val NavBarTopMargin = 14.dp   // the gap between the bar and the mini player above it
private val NavBarHorizontalMargin = 22.dp

/** The Home circle the bar collapses into. */
private val NavHomeSize = 60.dp
private val NavIconSize = 24.dp

/** How much an icon under the finger grows, and how far the capsule stretches while touched. */
// with the refraction shader (Android 13+) the glass does most of the magnifying, so the icons themselves grow less
private val NavLensMagnify = if (android.os.Build.VERSION.SDK_INT >= 33) 0.24f else 0.5f
private const val NavElasticX = 0.045f   // the bar widens a little while it is held
private const val NavElasticY = 0.012f
private const val NavPressRise = 0.085f   // how much taller the bar gets while held (toward the top)
// the press pops open past its size and settles back, like a drop of liquid taking the finger
private val NavPressIn = spring<Float>(dampingRatio = 0.55f, stiffness = 650f)
// the release: an elastic shrink with one small rebound (the bubble lets go, the bar settles back)
private val NavPressOut = spring<Float>(dampingRatio = 0.62f, stiffness = 360f)

/** where the bubble glides to when the finger lifts: critically damped, so it lands on the tab without a wobble */
private val NavBubbleSettle = spring<Float>(dampingRatio = 0.74f, stiffness = 420f)
private val NavBarShape = RoundedCornerShape(50)

/** the glass bubble: room kept between it and the bar's outline, and its width in tabs (resting + while held) */
private val NavBubbleInset = 5.dp
/** held, the bubble reaches this far past the bar's top and bottom, and (at either end) its end */
private val NavBubbleReach = 6.dp
private val NavBubbleEndReach = 2.dp
private const val NavBubbleWidthRest = 0.92f
private const val NavBubbleWidthPress = 0.10f

/** The lit tab's highlight sits this far in from the bar's top, bottom and both ends. */
private val NavHighlightInset = 8.dp

/** Room between a highlight and where its neighbour's would be. */
private val NavHighlightGap = 4.dp

/** How bright an unlit tab's icon is. */
private const val UnlitIconAlpha = 0.7f

/**
 * The highlight's front edge springs to the new tab faster than its back edge, so it stretches
 * toward the tab and settles into it instead of sliding over as a rigid block.
 */
private val NavHighlightLead = spring<Float>(dampingRatio = 0.92f, stiffness = 520f)
private val NavHighlightTrail = spring<Float>(dampingRatio = 0.92f, stiffness = 300f)

@Composable
fun AppNavigationBar(
    navigationItems: List<Screens>,
    currentRoute: String?,
    /** from [owningTab]: the tab the page on screen belongs to */
    selectedTabRoute: String,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    slimNav: Boolean = false,
    /** a second quick tap on Search: straight into typing */
    onSearchDoubleTap: (() -> Unit)? = null,
    /** page-level swipe: the capsule follows it live, same as a finger on the bar itself */
    swipeState: TabSwipeState? = null,
    /** the page's scrolling: collapses the bar into the Home circle, progressively */
    scrollState: NavBarScrollState? = null,
) {
    val glassState = LocalGlassState.current ?: remember { GlassState() }
    val accentWash = rememberChromeAccentWash()
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val selectedIndex = navigationItems.indexOfFirst { it.route == selectedTabRoute }
    val dragIndexState = remember { mutableStateOf<Float?>(null) }
    val pressIndexState = remember { mutableStateOf<Float?>(null) }
    // the one place the lens reads its finger from: a drag on the bar, a plain touch, or the page swipe
    val touchIndex = remember { derivedStateOf { dragIndexState.value ?: pressIndexState.value } }
    val lensIndex = remember(swipeState) {
        derivedStateOf { dragIndexState.value ?: pressIndexState.value ?: swipeState?.fractionalIndex }
    }
    // 0..1 while a finger is on the lens; springs back (elastically) when it lifts
    val press = remember { Animatable(0f) }
    val lastLens = remember { floatArrayOf(0f) }
    // The bubble's own position: follows the finger exactly while touched.
    val bubbleIndex = remember { Animatable(selectedIndex.coerceAtLeast(0).toFloat()) }
    // True from the first touch until the page the gesture chose is on screen (see below). While it is true the
    // bubble stays swollen; it only lets go (elastically) after that, so lifting never shows a half-way snap.
    val holdingState = remember { mutableStateOf(false) }
    val selectedNow by androidx.compose.runtime.rememberUpdatedState(selectedIndex)
    LaunchedEffect(Unit) {
        var lastFinger: Float? = null
        snapshotFlow { lensIndex.value }.collectLatest { finger ->
            if (finger != null) {
                lastFinger = finger
                holdingState.value = true
                bubbleIndex.snapTo(finger)
            } else {
                // The finger is up. The bubble glides onto the tab the gesture chose (not the one that is
                // still lit, which would flick it back), then waits for that page to arrive, and only then
                // does the press let go. A tap on the tab that is already lit has nothing to wait for.
                val last = lastFinger
                lastFinger = null
                if (holdingState.value && last != null) {
                    val target = last.roundToInt().coerceIn(0, (navigationItems.size - 1).coerceAtLeast(0))
                    bubbleIndex.animateTo(target.toFloat(), NavBubbleSettle)
                    withTimeoutOrNull(NavBubbleHoldMaxMillis) { snapshotFlow { selectedNow }.first { it == target } }
                    // a beat for the page's own transition to start, so the bubble lets go as it moves
                    delay(NavBubbleReleaseDelayMillis)
                    holdingState.value = false
                }
                bubbleIndex.animateTo(selectedNow.toFloat(), NavBubbleSettle)
            }
        }
    }
    LaunchedEffect(lensIndex) {
        snapshotFlow { lensIndex.value != null || holdingState.value }.collectLatest { down ->
            press.animateTo(if (down) 1f else 0f, if (down) NavPressIn else NavPressOut)
        }
    }
    // One glossy sweep across the glass when the app has opened (once per process, after the loading screen)
    val sheen = remember { Animatable(0f) }
    val launchIntro = com.danrey.lalasi.ui.launch.LocalLaunchIntro.current
    LaunchedEffect(Unit) {
        if (NavSheenPlayed) return@LaunchedEffect
        if (launchIntro != null && !launchIntro.done) {
            snapshotFlow { launchIntro.done }.first { it }
        }
        delay(350)
        if (NavSheenPlayed) return@LaunchedEffect
        NavSheenPlayed = true
        sheen.animateTo(1f, tween(1100, easing = androidx.compose.animation.core.CubicBezierEasing(0.33f, 0f, 0.2f, 1f)))
    }
    // gradients for the glow and the sweep: built once, only moved while drawing
    val navDensity = androidx.compose.ui.platform.LocalDensity.current
    val glass = remember(navDensity) { NavGlassBrushes(navDensity) }
    // the rainbow glass: one bubble object, one slowly turning phase (read only while drawing)
    val iridescent = rememberIridescentBubble()
    val iridescentRim = rememberIridescentBubble()
    // the rainbow only turns while the bubble is held or settling; at rest the phase stands still, so a still bar
    // and every glass button redraw nothing (no 30 fps loop running forever)
    val glassMoving by remember { derivedStateOf { press.value > 0.01f || holdingState.value } }
    val iridescencePhase = rememberIridescencePhase(active = glassMoving)
    // the bar's own motion blur, cached per step so a frame never allocates an effect
    val blurCache = remember { BlurCache() }
    val refraction = remember { if (android.os.Build.VERSION.SDK_INT >= 33) NavRefraction() else null }
    val layerRect = remember { FloatArray(4) }
    val motion = remember { NavBubbleMotion() }
    val bubbleDrawRect = remember { FloatArray(4) }
    val glowRect = remember { FloatArray(4) }
    // the bar's own outline (a little bigger by the bubble's reach): the bubble is cut to it near the bar's ends
    val navCapsule = remember { Path() }
    val collapse: () -> Float = remember(scrollState) { { navCollapseEase(scrollState?.collapse ?: 0f) } }
    val homeScreen = remember(navigationItems) {
        navigationItems.firstOrNull { it == Screens.Home } ?: navigationItems.firstOrNull()
    }
    // only flips once, so the bar recomposes at that moment and never per scrolled pixel
    val collapsedForTouch by remember(scrollState) {
        derivedStateOf { (scrollState?.collapse ?: 0f) > 0.6f }
    }

    Box(modifier = modifier) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                .padding(start = NavBarHorizontalMargin, end = NavBarHorizontalMargin, top = NavBarTopMargin)
                .align(Alignment.TopCenter)
                .height(NavBarHeight)
        ) {
            val fullWidth = maxWidth
            Box(
                modifier = Modifier
                    // width, height and corner follow the scroll in the layout phase: the capsule narrows
                    // toward the left into a circle of NavHomeSize, nothing else in the layout moves
                    .layout { measurable, constraints ->
                        val c = collapse()
                        val fullW = constraints.maxWidth
                        val fullH = constraints.maxHeight
                        val homePx = NavHomeSize.roundToPx()
                        val w = (fullW + (homePx - fullW) * c).roundToInt()
                        val h = (fullH + (homePx - fullH) * c).roundToInt()
                        val placeable = measurable.measure(Constraints.fixed(w, h))
                        layout(fullW, fullH) { placeable.place(0, (fullH - h) / 2) }
                    }
                    .graphicsLayer {
                        // elastic: the capsule stretches toward the finger and relaxes with a spring
                        val p = press.value
                        val li = lensIndex.value
                        val count = navigationItems.size.coerceAtLeast(1)
                        val frac = if (li != null) (li + 0.5f) / count else 0.5f
                        // held, the bar swells wider and UP (it grows from its bottom edge), liquid-glass style
                        scaleX = 1f + NavElasticX * p
                        scaleY = 1f + NavPressRise * p
                        transformOrigin = TransformOrigin(1f - frac, 1f)
                        translationX = (frac - 0.5f) * 5.dp.toPx() * p
                        // the scroll spring's overshoot, drawn as squash and stretch: past collapsed the
                        // circle squeezes in on its left edge, past whole the bar swells from its centre
                        // motion blur while the bar slides into (or out of) the circle: horizontal only, a few px,
                        // present only while it is actually moving, so a still bar has no extra layer at all
                        if (android.os.Build.VERSION.SDK_INT >= 31) {
                            val step = (((scrollState?.motion ?: 0f) * NavMotionBlurPerVelocity) / NavMotionBlurStepPx)
                                .toInt().coerceIn(0, NavMotionBlurMaxSteps)
                            renderEffect = blurCache.get(step)
                        }
                        val over = navCollapseOvershoot(scrollState?.collapse ?: 0f)
                        if (over != 0f) {
                            val sx = if (over > 0f) -0.9f * over else -0.25f * over
                            scaleX *= 1f + sx
                            scaleY *= 1f - sx * 0.6f
                            transformOrigin = if (over > 0f) TransformOrigin(0f, 0.5f) else TransformOrigin(0.5f, 0.5f)
                        }
                    }
                    .press3D(maxTilt = 1.5f, pressedScale = 0.992f)
                    .dropShadow(NavBarShape, ChromeShadow)
                    // the bubble is drawn OUTSIDE the clip below: held, it swells past the bar's top and bottom and
                    // springs back when it lets go (the press spring overshoots, so it is elastic)
                    .drawWithContent {
                        drawContent()
                        val p = press.value
                        if (p > 0.01f) {
                            navBubbleRect(
                                bubbleDrawRect, size.width, size.height, navigationItems.size, p, bubbleIndex.value,
                                NavBubbleInset.toPx(), NavBubbleReach.toPx(), NavBubbleEndReach.toPx(), motion.stretch,
                            )
                            val bl = bubbleDrawRect[0]
                            val top = bubbleDrawRect[1]
                            val bw = bubbleDrawRect[2]
                            val bh = bubbleDrawRect[3]
                            // near either end of the bar the bubble follows the bar's curve: its top and bottom edge
                            // (and the colour on them) bend down round the end instead of staying straight
                            val nearEnd = bl < size.height || bl + bw > size.width - size.height
                            // how close the bubble is to each end of the bar (0 = a bar-height away or more, 1 = at the end):
                            // it follows the bubble's position, so the end highlight fades in and out as it moves
                            val gapL = (1f - bl / size.height).coerceIn(0f, 1f)
                            val gapR = (1f - (size.width - (bl + bw)) / size.height).coerceIn(0f, 1f)
                            val endLAmt = gapL * gapL * (3f - 2f * gapL)
                            val endRAmt = gapR * gapR * (3f - 2f * gapR)
                            if (nearEnd) {
                                val rp = NavBubbleReach.toPx()
                                navCapsule.reset()
                                navCapsule.addRoundRect(
                                    // the bar's own width and end curve, but taller by the bubble's reach above and below:
                                    // the bubble never pokes out past the bar's end, it bends round it
                                    androidx.compose.ui.geometry.RoundRect(
                                        0f, -rp, size.width, size.height + rp,
                                        size.height / 2f, size.height / 2f + rp,
                                    )
                                )
                            }
                            drawIridescentBubble(
                                bubble = iridescent,
                                topLeft = Offset(bl, top),
                                size = Size(bw, bh),
                                phase = iridescencePhase.value,
                                intensity = p.coerceIn(0f, 1f),
                                bounds = if (nearEnd) navCapsule else null,
                                endLeft = bl < size.height,
                                endRight = bl + bw > size.width - size.height,
                                endLeftAmt = endLAmt,
                                endRightAmt = endRAmt,
                            )
                        }
                    }
                    .clip(NavBarShape)
                    .drawWithContent {
                        drawContent()
                        // the opening sweep: two solid diagonal bands of light crossing the glass (no gradient)
                        val s = sheen.value
                        if (s > 0f && s < 1f) {
                            val x = -glass.sheenWidth + (size.width + glass.sheenWidth) * s
                            val envelope = kotlin.math.sin(Math.PI.toFloat() * s)
                            translate(left = x) {
                                drawPath(glass.sheenWide, Color.White.copy(alpha = 0.10f * envelope))
                                drawPath(glass.sheenThin, Color.White.copy(alpha = 0.22f * envelope))
                            }
                        }
                        // the bar's highlight follows the bubble: it lights up under the finger and slides with it
                        val hp = press.value
                        if (hp > 0.01f) {
                            navBubbleRect(
                                glowRect, size.width, size.height, navigationItems.size, hp, bubbleIndex.value,
                                NavBubbleInset.toPx(), NavBubbleReach.toPx(), NavBubbleEndReach.toPx(), motion.stretch,
                            )
                            val cx = glowRect[0] + glowRect[2] / 2f
                            val a = hp.coerceIn(0f, 1f)
                            translate(left = cx - glass.glowRadius, top = 0f) {
                                drawRect(glass.followGlow, size = Size(glass.glowRadius * 2f, size.height), alpha = 0.34f * a)
                            }
                            // the light along the bar's top and bottom edge: it is the bar's own outline, shown only
                            // under the bubble (stepped fade), so at the ends it runs down round the curve
                            val halfWin = glass.followEdgeWidth / 2f
                            val line = 1.6.dp.toPx()
                            val capR = (size.height / 2f - line / 2f).coerceAtLeast(0f)
                            for (step in NavEdgeSteps) {
                                clipRect(cx - halfWin * step, 0f, cx + halfWin * step, size.height) {
                                    drawRoundRect(
                                        color = Color.White.copy(alpha = 0.22f * a),
                                        topLeft = Offset(line / 2f, line / 2f),
                                        size = Size(size.width - line, size.height - line),
                                        cornerRadius = CornerRadius(capR),
                                        style = BubbleStroke(line),
                                    )
                                }
                            }
                        }
                        drawChromeOutline(Offset.Zero, size, cornerRadius = size.height / 2f)
                        // the bar itself carries the rainbow film at rest; the bubble only comes up when it is pressed
                        drawIridescentRim(iridescentRim, Offset.Zero, size, size.height / 2f, iridescencePhase.value, 0.34f)
                    }
            ) {
                FrostedChromeGlass(
                    state = glassState,
                    pureBlack = pureBlack,
                    accentWash = accentWash,
                    modifier = Modifier.fillMaxSize(),
                )

                // Half the gap is the row's padding and half is inside each slot, so the highlight sits
                // the same distance from the ends as from the top and bottom. The row keeps the bar's
                // full width while the capsule narrows round it (clipped), and fades out as it goes.
                Row(
                    modifier = Modifier
                        .requiredWidth(fullWidth)
                        .fillMaxHeight()
                        .graphicsLayer {
                            alpha = (1f - collapse() * 1.8f).coerceIn(0f, 1f)
                            // Android 13+: the icons under the bubble are refracted by a lens shader while it is held
                            val pv = press.value
                            motion.update(bubbleIndex.value)
                            val rf = refraction
                            if (rf != null && pv > 0.02f && collapse() < 0.01f) {
                                navBubbleRect(
                                    layerRect, size.width, size.height, navigationItems.size, pv, bubbleIndex.value,
                                    NavBubbleInset.toPx(), NavBubbleReach.toPx(), NavBubbleEndReach.toPx(), motion.stretch,
                                )
                                renderEffect = rf.effect(
                                    layerRect[0], layerRect[1], layerRect[0] + layerRect[2], layerRect[1] + layerRect[3],
                                    layerRect[3] / 2f, pv.coerceIn(0f, 1f),
                                )
                            } else {
                                renderEffect = null
                            }
                        }
                        .padding(horizontal = NavHighlightInset - NavHighlightGap / 2)
                        // the lens starts at the touch itself, not after the drag slop; this only
                        // watches (Initial pass, consumes nothing), so taps and drags work as before
                        .pointerInput(navigationItems) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                val count = navigationItems.size
                                if (count > 0 && size.width > 0) {
                                    pressIndexState.value = (down.position.x / size.width * count - 0.5f)
                                        .coerceIn(0f, (count - 1).toFloat())
                                }
                                do {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (change.pressed && count > 0 && size.width > 0) {
                                        pressIndexState.value = (change.position.x / size.width * count - 0.5f)
                                            .coerceIn(0f, (count - 1).toFloat())
                                    }
                                } while (change.pressed)
                                pressIndexState.value = null
                            }
                        }
                        .pointerInput(navigationItems, selectedIndex) {
                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    val count = navigationItems.size
                                    if (count > 0 && size.width > 0) {
                                        dragIndexState.value = (offset.x / size.width * count - 0.5f)
                                            .coerceIn(0f, (count - 1).toFloat())
                                    }
                                },
                                onDragEnd = {
                                    val target = dragIndexState.value?.roundToInt()
                                    dragIndexState.value = null
                                    if (target != null && target in navigationItems.indices && target != selectedIndex) {
                                        onItemClick(navigationItems[target], false)
                                    }
                                },
                                onDragCancel = { dragIndexState.value = null },
                                onHorizontalDrag = { change, _ ->
                                    change.consume()
                                    val count = navigationItems.size
                                    if (count > 0 && size.width > 0) {
                                        dragIndexState.value = (change.position.x / size.width * count - 0.5f)
                                            .coerceIn(0f, (count - 1).toFloat())
                                    }
                                }
                            )
                        }
                        .navHighlight(selectedIndex, navigationItems.size, touchIndex, swipeState, holdingState, iridescentRim, iridescencePhase),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    navigationItems.forEachIndexed { index, screen ->
                        // lit = the tab this page belongs to. what a tap on it does (scroll to the top
                        // on the tab's own page, back to that page from deeper in) is the caller's call
                        NavTab(
                            screen = screen,
                            tabIndex = index,
                            lensIndex = lensIndex,
                            isSelected = screen.route == selectedTabRoute,
                            onItemClick = onItemClick,
                            onDoubleTap = onSearchDoubleTap.takeIf { screen == Screens.Search },
                        )
                    }
                }

                // the circle the tabs have turned into, fading in over the last of the collapse. It wears the icon
                // of the tab you are on (Home, Search, Library ...), so it always says where you are.
                if (homeScreen != null) {
                    val currentScreen = navigationItems.firstOrNull { it.route == selectedTabRoute } ?: homeScreen
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(NavHomeSize)
                            .graphicsLayer {
                                val a = ((collapse() - 0.55f) / 0.45f).coerceIn(0f, 1f)
                                alpha = a
                                val k = 0.7f + 0.3f * a
                                scaleX = k
                                scaleY = k
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        // a change of tab while collapsed (a swipe, a deep link) crossfades the icon
                        androidx.compose.animation.Crossfade(
                            targetState = currentScreen,
                            animationSpec = tween(180),
                            label = "collapsedTabIcon",
                        ) { screen ->
                            val icon = coil3.compose.rememberAsyncImagePainter(model = screen.iconIdActive)
                            Icon(
                                painter = icon,
                                contentDescription = stringResource(screen.titleId),
                                tint = Color.White,
                                modifier = Modifier.size(NavIconSize),
                            )
                        }
                    }
                }

                // collapsed, the whole circle is one button: it opens the bar again, on the tab you are on
                if (collapsedForTouch && homeScreen != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                scope.launch { scrollState?.expandAnimated() }
                            }
                    )
                }
            }
        }
    }
}

/**
 * One highlight behind the lit tab that slides to the next one, instead of each tab fading its own
 * in and out. Only read while drawing, so the slide never recomposes the bar.
 */
@Composable
private fun Modifier.navHighlight(
    selectedIndex: Int,
    tabCount: Int,
    dragIndex: androidx.compose.runtime.State<Float?>,
    swipe: TabSwipeState? = null,
    holding: androidx.compose.runtime.State<Boolean>? = null,
    bubble: IridescentBubble? = null,
    phase: androidx.compose.runtime.State<Float>? = null,
): Modifier {
    val start = remember { Animatable(selectedIndex.coerceAtLeast(0).toFloat()) }
    val end = remember { Animatable(selectedIndex.coerceAtLeast(0).toFloat()) }
    val shown = remember { Animatable(if (selectedIndex >= 0) 1f else 0f) }

    val currentSelected by androidx.compose.runtime.rememberUpdatedState(selectedIndex)
    // The lens hands over to the settled highlight where the finger left it, and springs on from
    // there: to the tab that was chosen, or back to the one that was lit if nothing changed.
    LaunchedEffect(dragIndex) {
        var active = false
        androidx.compose.runtime.snapshotFlow { dragIndex.value }.collect { v ->
            if (v != null) {
                active = true
                start.snapTo(v)
                end.snapTo(v)
            } else if (active) {
                active = false
                // a frame or two, so a tab change this touch caused has arrived before we pick a target
                androidx.compose.runtime.withFrameNanos { }
                androidx.compose.runtime.withFrameNanos { }
                val t = currentSelected
                if (t < 0) {
                    launch { shown.animateTo(0f, tween(220, easing = androidx.compose.animation.core.FastOutSlowInEasing)) }
                } else {
                    launch { start.animateTo(t.toFloat(), NavHighlightTrail) }
                    launch { end.animateTo(t.toFloat(), NavHighlightLead) }
                }
            }
        }
    }

    LaunchedEffect(swipe) {
        if (swipe == null) return@LaunchedEffect
        androidx.compose.runtime.snapshotFlow { swipe.fractionalIndex }.collect { v ->
            if (v != null) {
                start.snapTo(v)
                end.snapTo(v)
            }
        }
    }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex < 0) {
            shown.animateTo(0f, tween(220, easing = androidx.compose.animation.core.FastOutSlowInEasing))
            return@LaunchedEffect
        }
        val target = selectedIndex.toFloat()
        // back from no lit tab, it fades in where it belongs rather than sliding over from the old one
        if (shown.value == 0f) {
            start.snapTo(target)
            end.snapTo(target)
        }
        launch { shown.animateTo(1f, tween(220, easing = androidx.compose.animation.core.FastOutSlowInEasing)) }
        val movingRight = target > (start.value + end.value) / 2f
        launch { start.animateTo(target, if (movingRight) NavHighlightTrail else NavHighlightLead) }
        end.animateTo(target, if (movingRight) NavHighlightLead else NavHighlightTrail)
    }

    return drawBehind {
        if (tabCount == 0) return@drawBehind
        val slot = size.width / tabCount
        val halfGap = NavHighlightGap.toPx() / 2f
        val inset = NavHighlightInset.toPx()
        val height = size.height - inset * 2f
        val radius = CornerRadius(height / 2f)

        // While the finger is down, the glass capsule tracks the finger's tab directly.
        // Drawing it here avoids per-frame composable/layout work and keeps the gesture lightweight.
        // A travelling glass lens follows the finger and elastically morphs between tabs.
        val fingerIndex = dragIndex.value ?: swipe?.fractionalIndex
        // no capsule or light under the finger: the glass bubble drawn by the bar is the only touch effect
        if (fingerIndex != null) return@drawBehind
        // the bubble is still holding the chosen tab: it hands over to this highlight when it lets go
        if (holding?.value == true) return@drawBehind

        val alpha = shown.value
        if (alpha <= 0f) return@drawBehind
        // The two edges spring at different speeds, so the gap between them is the stretch: the
        // front edge runs ahead and the back edge catches up, then it settles back to one slot.
        val lo = minOf(start.value, end.value)
        val hi = maxOf(start.value, end.value)
        val baseWidth = slot - halfGap * 2f
        val left = (lo + 0.5f) * slot - baseWidth / 2f
        val width = (hi - lo) * slot + baseWidth
        // the lit tab at rest: a plain frosted pill with oval ends and a dark edge, no bubble until the bar is pressed
        val pillRadius = CornerRadius(minOf(width * 0.30f, height), height / 2f)
        drawRoundRect(
            color = ChromeHighlightColor,
            topLeft = Offset(left, inset),
            size = Size(width, height),
            cornerRadius = pillRadius,
            alpha = alpha,
        )
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.28f),
            topLeft = Offset(left, inset),
            size = Size(width, height),
            cornerRadius = pillRadius,
            style = BubbleStroke(1.dp.toPx()),
            alpha = alpha,
        )
    }
}

/**
 * A tab's tap, or with [onDoubleTap] a second tap close behind the first: the first still goes to
 * the tab at once, the second does [onDoubleTap] instead. [lastTap] holds the time of the one before.
 */
private fun tabTapped(lastTap: LongArray, doubleTapTimeoutMillis: Long, onDoubleTap: (() -> Unit)?, onTap: () -> Unit) {
    val now = android.os.SystemClock.uptimeMillis()
    if (onDoubleTap != null && now - lastTap[0] < doubleTapTimeoutMillis) {
        lastTap[0] = 0L
        onDoubleTap()
    } else {
        lastTap[0] = now
        onTap()
    }
}

/**
 * One tab of the bar. Its icon swaps to the filled one and brightens as it lights up, and it
 * squishes under a press. [onDoubleTap] is for Search, where two taps open the search field.
 * The Library's icon turns into a download badge while songs download.
 */
@Composable
private fun RowScope.NavTab(
    screen: Screens,
    tabIndex: Int,
    lensIndex: androidx.compose.runtime.State<Float?>,
    isSelected: Boolean,
    onItemClick: (Screens, Boolean) -> Unit,
    onDoubleTap: (() -> Unit)?,
) {
    val viewConfiguration = LocalViewConfiguration.current
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val lastTap = remember { longArrayOf(0L) }
    val lit = animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(240, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "nav_tab_lit"
    )

    // Both icons stay loaded, so lighting a tab never shows an empty frame while its filled icon loads.
    val unlitIcon = coil3.compose.rememberAsyncImagePainter(model = screen.iconIdInactive)
    val litIcon = coil3.compose.rememberAsyncImagePainter(model = screen.iconIdActive)
    val iconSize = NavIconSize

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .pressBounce(interactionSource, pressedScale = 0.94f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    // a light tick for a real switch only, so the tab you're already on stays quiet
                    if (!isSelected) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    tabTapped(lastTap, viewConfiguration.doubleTapTimeoutMillis, onDoubleTap) {
                        onItemClick(screen, isSelected)
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // The label's room is always kept, so the bar never changes size or moves an icon by a jump.
        // The unlit tab shows its icon alone, centred; lighting it floats the icon up and fades the
        // label in beneath it, both read while drawing only.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(screen.titleId),
                fontSize = 9.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier.graphicsLayer {
                    val l = lit.value
                    alpha = l
                    val k = 0.82f + 0.18f * l
                    scaleX = k
                    scaleY = k
                    translationY = (1f - l) * 4.dp.toPx()
                },
            )
            Spacer(modifier = Modifier.height(3.dp))
            Box(
                modifier = Modifier.graphicsLayer {
                    // half the label's room: the lone icon sits in the middle of the capsule
                    var ty = (1f - lit.value) * -7.dp.toPx()
                    // the lens magnifies the icons near the finger, falling off smoothly with distance
                    val f = lensIndex.value
                    if (f != null) {
                        val d = f - tabIndex
                        val m = kotlin.math.exp(-(d * d) / 0.42f)
                        val k = 1f + NavLensMagnify * m
                        scaleX = k
                        scaleY = k
                        ty -= 3.dp.toPx() * m
                    }
                    translationY = ty
                },
            ) {
                if (screen == Screens.Library) {
                    LibraryDownloadIcon { TabIcons(screen, unlitIcon, litIcon, iconSize) { lit.value } }
                } else {
                    TabIcons(screen, unlitIcon, litIcon, iconSize) { lit.value }
                }
            }
        }
    }
}

/** a tab's two icons, the filled one fading in over the outline as the tab lights up */
@Composable
private fun TabIcons(
    screen: Screens,
    unlitIcon: androidx.compose.ui.graphics.painter.Painter,
    litIcon: androidx.compose.ui.graphics.painter.Painter,
    iconSize: androidx.compose.ui.unit.Dp,
    lit: () -> Float,
) {
    Box(contentAlignment = Alignment.Center) {
        Icon(
            painter = unlitIcon,
            contentDescription = stringResource(screen.titleId),
            tint = Color.White,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    alpha = UnlitIconAlpha * (1f - lit())
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                }
        )
        Icon(
            painter = litIcon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    alpha = lit()
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                }
        )
    }
}

/** nested widths (share of the glow window) of the stepped fade of the bar's edge light */
private val NavEdgeSteps = floatArrayOf(1f, 0.6f, 0.3f)

/** how long the bubble keeps hold of the finger's tab, at most, waiting for that page to arrive */
private const val NavBubbleHoldMaxMillis = 450L

/** after the page has arrived, the bubble waits this long before it lets go */
private const val NavBubbleReleaseDelayMillis = 70L

/** motion blur of the bar: px of blur per unit of collapse velocity, in steps, capped */
private const val NavMotionBlurPerVelocity = 1.8f
private const val NavMotionBlurStepPx = 1.5f
private const val NavMotionBlurMaxSteps = 8

/** the opening sweep plays once per process, not every time the bar is composed */
@Volatile
private var NavSheenPlayed = false

/** Blur effects by step, made on first use and reused after, so the draw phase never allocates one per frame. */
private class BlurCache {
    private val effects = arrayOfNulls<androidx.compose.ui.graphics.RenderEffect>(NavMotionBlurMaxSteps + 1)
    fun get(step: Int): androidx.compose.ui.graphics.RenderEffect? {
        if (step <= 0) return null
        return effects[step] ?: BlurEffect(step * NavMotionBlurStepPx, 0f, TileMode.Decal).also { effects[step] = it }
    }
}

/** The bar's glow radius and sweep shapes, built once per density: solid fills only, moved (not rebuilt) while drawing. */

/**
 * How fast the bubble is travelling, smoothed, 0..1. It stretches the bubble along its way (squash and stretch):
 * the faster it moves the longer and flatter it gets, then it snaps back round as it lands.
 */
private class NavBubbleMotion {
    private var lastIndex = 0f
    private var lastNanos = 0L
    var stretch = 0f
        private set

    fun update(index: Float) {
        val now = System.nanoTime()
        val dt = (now - lastNanos) / 1_000_000_000f
        if (lastNanos == 0L || dt > 0.08f) {
            stretch = 0f
        } else if (dt > 0f) {
            val speed = kotlin.math.abs(index - lastIndex) / dt          // tabs per second
            val target = (speed / 5f).coerceIn(0f, 1f)
            stretch += (target - stretch) * (1f - kotlin.math.exp(-dt * 16f))
        }
        lastIndex = index
        lastNanos = now
    }
}

/**
 * The bubble's rectangle in the bar's own coordinates, written to [out] as left, top, width, height.
 * One function for the bubble's drawing, the refraction and the follow-highlight, so all three always agree.
 */
private fun navBubbleRect(
    out: FloatArray, w: Float, h: Float, count: Int, p: Float, index: Float,
    rest: Float, reach: Float, endReach: Float, stretch: Float = 0f,
) {
    val slot = w / count.coerceAtLeast(1)
    val k = p.coerceAtMost(1.15f)
    val top0 = rest + (-reach * k - rest) * k.coerceAtMost(1f)
    // squash and stretch: longer and a little flatter while it travels
    val top = top0 + (h - top0 * 2f) * 0.07f * stretch
    val bh = (h - top * 2f).coerceAtLeast(0f)
    val bw = (slot * (NavBubbleWidthRest + NavBubbleWidthPress * k + 0.34f * stretch)).coerceAtMost(w - rest * 2f)
    val endGap = rest + (-endReach - rest) * k.coerceAtMost(1f)
    val bl = ((index + 0.5f) * slot - bw / 2f).coerceIn(endGap, (w - bw - endGap).coerceAtLeast(endGap))
    out[0] = bl
    out[1] = top
    out[2] = bw
    out[3] = bh
}

/** AGSL liquid lens: the icons under the bubble are bent like a drop of water, and split into colour at its rim. */
private const val NavRefractionAgsl = """
uniform shader content;
uniform float4 bubble;
uniform float radius;
uniform float strength;

half4 main(float2 c) {
    float2 ctr = (bubble.xy + bubble.zw) * 0.5;
    float2 hs = (bubble.zw - bubble.xy) * 0.5;
    float2 p = c - ctr;
    float2 q = abs(p) - (hs - float2(radius));
    float d = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
    if (d >= 0.0) { return content.eval(c); }
    float depth = clamp(-d / radius, 0.0, 1.0);
    float rim = pow(1.0 - depth, 2.2);
    float m = strength * (0.14 + 0.34 * rim);
    float ca = 0.09 * rim * strength;
    half4 cr = content.eval(ctr + p * (1.0 - m * (1.0 + ca)));
    half4 cg = content.eval(ctr + p * (1.0 - m));
    half4 cb = content.eval(ctr + p * (1.0 - m * (1.0 - ca)));
    half4 lens = half4(cr.r, cg.g, cb.b, max(cr.a, max(cg.a, cb.a)));
    return mix(content.eval(c), lens, half(clamp(-d / 1.5, 0.0, 1.0)));
}
"""

@androidx.annotation.RequiresApi(33)
private class NavRefraction {
    private val shader = android.graphics.RuntimeShader(NavRefractionAgsl)
    fun effect(l: Float, t: Float, r: Float, b: Float, radius: Float, strength: Float): androidx.compose.ui.graphics.RenderEffect {
        shader.setFloatUniform("bubble", l, t, r, b)
        shader.setFloatUniform("radius", radius)
        shader.setFloatUniform("strength", strength)
        return android.graphics.RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
    }
}

private class NavGlassBrushes(density: androidx.compose.ui.unit.Density) {
    val glowRadius = with(density) { (NavBarHeight * 1.0f).toPx() }
    val sheenWidth = with(density) { 90.dp.toPx() }
    private val bandH = with(density) { NavBarHeight.toPx() }
    private val skew = with(density) { 26.dp.toPx() }

    /** a leaning parallelogram, [from]..[to] of the way across [sheenWidth] */
    private fun band(from: Float, to: Float) = Path().apply {
        moveTo(sheenWidth * from + skew, 0f)
        lineTo(sheenWidth * to + skew, 0f)
        lineTo(sheenWidth * to, bandH)
        lineTo(sheenWidth * from, bandH)
        close()
    }

    /** the highlight that follows the bubble: a soft glow, and a light edge along the bar's top and bottom */
    val followGlow = androidx.compose.ui.graphics.Brush.radialGradient(
        colors = listOf(Color(0xFFDCEBFF).copy(alpha = 0.55f), Color(0xFFDCEBFF).copy(alpha = 0.16f), Color.Transparent),
        center = Offset(glowRadius, bandH / 2f),
        radius = glowRadius,
    )
    val followEdgeWidth = glowRadius * 1.5f
    val followEdge = androidx.compose.ui.graphics.Brush.horizontalGradient(
        colors = listOf(Color.Transparent, Color.White, Color.Transparent),
        startX = 0f,
        endX = glowRadius * 1.5f,
    )

    val sheenWide = band(0.10f, 0.62f)
    val sheenThin = band(0.72f, 0.84f)
}
