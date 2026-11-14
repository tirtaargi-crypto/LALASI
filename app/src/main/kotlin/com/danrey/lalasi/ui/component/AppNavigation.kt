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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.CompositingStrategy
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
    "search/" to "search_input",
    "genre/" to "search_input",
    "youtube_browse/" to "search_input",
    "browse/" to "search_input",
    "library/" to "library",
    "cipher_settings" to "settings",
    "playback_diagnostics" to "settings",
    "mochy_settings" to "settings",
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
    var collapse by mutableFloatStateOf(0f)
        private set

    /** the bar only collapses beside a mini player; with none showing, scrolling leaves it whole */
    var canCollapse: Boolean = true

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        val dy = consumed.y
        if (canCollapse && dy != 0f && travelPx > 0f) {
            // content moving up (reading further down) is a negative delta
            collapse = (collapse - dy / travelPx).coerceIn(0f, 1f)
        }
        return Offset.Zero
    }

    /** a tab change brings the bar back to full size */
    fun reset() {
        collapse = 0f
    }

    /** the bar grows back with a spring instead of jumping (tab change, tap on the Home circle) */
    suspend fun expandAnimated() {
        if (collapse <= 0f) return
        androidx.compose.animation.core.animate(
            initialValue = collapse,
            targetValue = 0f,
            animationSpec = spring(dampingRatio = 0.86f, stiffness = 380f),
        ) { v, _ -> collapse = v }
    }
}

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
val MiniPlayerCollapsedShift = 20.dp + 56.dp + 8.dp - 24.dp

/**
 * How far down (dp) the mini player drops when the bar is fully collapsed, so it sits level with the
 * Home circle instead of floating above the bar: the 80dp the bar reserves, less the 8dp under the pill.
 */
val MiniPlayerCollapsedDrop = 72.dp

/** px the mini player drops while the bar collapses beside it; 0 when nothing provides it */
val LocalMiniNavDrop = staticCompositionLocalOf<() -> Float> { { 0f } }

/** px the mini player's left edge is pushed right by the collapsed bar; 0 when nothing provides it */
val LocalMiniNavShift = staticCompositionLocalOf<() -> Float> { { 0f } }

/** The bar itself, a stadium; with its 6dp above and below it fills the 80dp the layout reserves. */
private val NavBarHeight = 68.dp
private val NavBarVerticalMargin = 6.dp
private val NavBarHorizontalMargin = 20.dp

/** The Home circle the bar collapses into. */
private val NavHomeSize = 56.dp
private val NavIconSize = 24.dp

/** How much an icon under the finger grows, and how far the capsule stretches while touched. */
private const val NavLensMagnify = 0.34f
private const val NavElasticX = 0.028f
private const val NavElasticY = 0.018f
private val NavPressIn = spring<Float>(dampingRatio = 0.72f, stiffness = 900f)
private val NavPressOut = spring<Float>(dampingRatio = 0.5f, stiffness = 380f)
private val NavBarShape = RoundedCornerShape(50)

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
    LaunchedEffect(lensIndex) {
        androidx.compose.runtime.snapshotFlow { lensIndex.value != null }.collectLatest { down ->
            press.animateTo(if (down) 1f else 0f, if (down) NavPressIn else NavPressOut)
        }
    }
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
                .padding(horizontal = NavBarHorizontalMargin, vertical = NavBarVerticalMargin)
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
                        scaleX = 1f + NavElasticX * p
                        scaleY = 1f - NavElasticY * p
                        transformOrigin = TransformOrigin(1f - frac, 0.5f)
                        translationX = (frac - 0.5f) * 5.dp.toPx() * p
                    }
                    .press3D(maxTilt = 1.5f, pressedScale = 0.992f)
                    .dropShadow(NavBarShape, ChromeShadow)
                    // Drawn after (over) the clipped bar but outside its clip: while touched, a see-through
                    // glass bubble swells past the capsule's edges, then springs back with the press.
                    .drawWithContent {
                        drawContent()
                        val p = press.value
                        if (p > 0.01f) {
                            val li = lensIndex.value ?: lastLens[0]
                            lastLens[0] = li
                            val slot = size.width / navigationItems.size.coerceAtLeast(1)
                            val bulge = 9.dp.toPx() * p
                            val w = slot * (1.12f + 0.3f * p)
                            val h = size.height + bulge * 2f
                            val left = (li + 0.5f) * slot - w / 2f
                            val corner = CornerRadius(h / 2f)
                            // clear glass body with a bright specular outline (top run + corners catch the light)
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.02f * p),
                                topLeft = Offset(left, -bulge),
                                size = Size(w, h),
                                cornerRadius = corner,
                            )
                            drawChromeOutline(Offset(left, -bulge), Size(w, h), cornerRadius = h / 2f, alpha = p)
                        }
                    }
                    .clip(NavBarShape)
                    .drawWithContent {
                        drawContent()
                        drawChromeOutline(Offset.Zero, size, cornerRadius = size.height / 2f)
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
                        .graphicsLayer { alpha = (1f - collapse() * 1.8f).coerceIn(0f, 1f) }
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
                        .navHighlight(selectedIndex, navigationItems.size, touchIndex, swipeState),
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

                // the Home circle the tabs have turned into, fading in over the last of the collapse
                if (homeScreen != null) {
                    val homeIcon = coil3.compose.rememberAsyncImagePainter(model = homeScreen.iconIdActive)
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
                        Icon(
                            painter = homeIcon,
                            contentDescription = stringResource(homeScreen.titleId),
                            tint = Color.White,
                            modifier = Modifier.size(NavIconSize),
                        )
                    }
                }

                // collapsed, the whole circle is one button: Home, with the bar growing back as it goes
                if (collapsedForTouch && homeScreen != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onItemClick(homeScreen, homeScreen.route == selectedTabRoute)
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

        val alpha = shown.value
        if (alpha <= 0f) return@drawBehind
        // The two edges spring at different speeds, so the gap between them is the stretch: the
        // front edge runs ahead and the back edge catches up, then it settles back to one slot.
        val lo = minOf(start.value, end.value)
        val hi = maxOf(start.value, end.value)
        val baseWidth = slot - halfGap * 2f
        val left = (lo + 0.5f) * slot - baseWidth / 2f
        val width = (hi - lo) * slot + baseWidth
        drawRoundRect(
            color = Color.White.copy(alpha = 0.055f * alpha),
            topLeft = Offset(left - 1.dp.toPx(), inset - 0.5.dp.toPx()),
            size = Size(width + 2.dp.toPx(), height + 1.dp.toPx()),
            cornerRadius = CornerRadius((height + 1.dp.toPx()) / 2f),
        )
        drawRoundRect(
            color = ChromeHighlightColor,
            topLeft = Offset(left, inset),
            size = Size(width, height),
            cornerRadius = radius,
            alpha = alpha,
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.12f * alpha),
            topLeft = Offset(left + 1.dp.toPx(), inset + 1.dp.toPx()),
            size = Size((width - 2.dp.toPx()).coerceAtLeast(0f), height * 0.34f),
            cornerRadius = radius,
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.20f * alpha),
            topLeft = Offset(left + 0.5f, inset + 0.5f),
            size = Size((width - 1f).coerceAtLeast(0f), (height - 1f).coerceAtLeast(0f)),
            cornerRadius = radius,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.8.dp.toPx()),
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
