package com.danrey.lalasi.ui.component

import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.foundation.gestures.awaitEachGesture
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sign

/**
 * Single source of truth for the page-level tab swipe. The page offset and the indicator index are
 * both derived from the real finger position, so the page and the bottom-bar capsule never drift apart.
 * Both values are plain state that is only read inside graphicsLayer / drawBehind, so a drag does not
 * recompose any screen.
 */
@Stable
class TabSwipeState {
    /** page offset in px, already run through the elastic curve */
    var offsetX by mutableFloatStateOf(0f)
        internal set

    /** page width in px, set by the gesture layer */
    var widthPx by mutableFloatStateOf(1f)
        internal set

    /** fractional tab index the capsule should sit at while a swipe is active; null when idle */
    var fractionalIndex by mutableStateOf<Float?>(null)
        internal set

    internal var settleJob: Job? = null

    /** true while the swipe's own commit animation runs; the NavHost then skips its own slide/fade */
    var committing: Boolean = false

    /** a tab the navigation bar (tap or drag) wants to go to; consumed by [tabPageSwipe] */
    internal var pendingSwitch by mutableStateOf<Int?>(null)

    /** Go to tab [index] with the same one-page-at-a-time motion as a page swipe. */
    fun requestSwitch(index: Int) {
        pendingSwitch = index
    }
}

private const val CommitFraction = 0.28f      // of page width
private const val FlingVelocityDp = 700f      // dp/s
private const val MaxFollowFraction = 0.62f   // elastic asymptote for a valid direction
private const val EdgeFollowFraction = 0.10f  // elastic asymptote at the first/last tab

private val SettleSpring = spring<Float>(dampingRatio = 0.94f, stiffness = 360f) // quiet, controlled settle
private val IndexSpring = spring<Float>(dampingRatio = 0.94f, stiffness = 420f)

/** x = raw finger travel, returns the displayed travel: 1:1 at the start, then it stiffens */
internal fun elastic(raw: Float, max: Float): Float {
    if (max <= 0f) return 0f
    return sign(raw) * max * (1f - exp(-abs(raw) / max))
}

/**
 * Swipe the page horizontally to move to the neighbouring tab.
 *
 * - Touch slop + direction lock: horizontal only; a vertical scroll never starts it.
 * - Ownership: it listens on the Main pass, so a child that already consumed the pointer
 *   (seek bar, slider, horizontal lists, bottom sheet drag) keeps the gesture and this never starts.
 * - A new touch during a settling animation cancels it and takes over from the current offset.
 * - Release: commit past [CommitFraction] of the width or on a fast fling in the same direction,
 *   otherwise the page springs back and the active tab does not change.
 */
@Composable
fun Modifier.tabPageSwipe(
    state: TabSwipeState,
    tabCount: Int,
    currentIndex: () -> Int,
    enabled: () -> Boolean,
    animationsEnabled: () -> Boolean,
    onSwitch: (Int) -> Unit,
): Modifier {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val flingPx = with(density) { FlingVelocityDp.dp.toPx() }
    val onSwitchState = androidx.compose.runtime.rememberUpdatedState(onSwitch)

    // switches asked for by the navigation bar: one page on screen at a time, instead of two animating at once
    androidx.compose.runtime.LaunchedEffect(state) {
        androidx.compose.runtime.snapshotFlow { state.pendingSwitch }.collect { target ->
            if (target != null) {
                state.pendingSwitch = null
                val start = currentIndex()
                if (target != start && target in 0 until tabCount) {
                    state.settleJob?.cancel()
                    commitTo(
                        state, state.offsetX, start, target,
                        state.widthPx.coerceAtLeast(1f), animationsEnabled(), onSwitchState.value,
                    )
                }
            }
        }
    }

    return this
        .pointerInput(tabCount) {
            trackTabSwipe(state, scope, tabCount, currentIndex, enabled, animationsEnabled, flingPx, onSwitchState)
        }
        .graphicsLayer {
            val w = state.widthPx.coerceAtLeast(1f)
            val p = (abs(state.offsetX) / w).coerceIn(0f, 1f)
            translationX = state.offsetX
            // the page fades out as it leaves, so the swap at the far end is invisible
            val f = ((p - 0.12f) / 0.28f).coerceIn(0f, 1f)
            alpha = (1f - 0.18f * p) * (1f - f * f * (3f - 2f * f))
            val s = 1f - 0.012f * p
            scaleX = s
            scaleY = s
        }
}

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.trackTabSwipe(
    state: TabSwipeState,
    scope: kotlinx.coroutines.CoroutineScope,
    tabCount: Int,
    currentIndex: () -> Int,
    enabled: () -> Boolean,
    animationsEnabled: () -> Boolean,
    flingPx: Float,
    onSwitch: androidx.compose.runtime.State<(Int) -> Unit>,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
        if (!enabled() || tabCount < 2) return@awaitEachGesture
        state.widthPx = size.width.toFloat().coerceAtLeast(1f)

        // a touch during a settle takes over from where the page is now
        val interrupted = state.settleJob?.isActive == true
        state.settleJob?.cancel()

        var overSlopSeen = 0f
        val slop = awaitHorizontalTouchSlopOrCancellation(down.id) { change: PointerInputChange, over: Float ->
            change.consume()
            overSlopSeen = over
        } ?: run {
            // not ours (vertical, or a child consumed it): restore anything left from an interrupted settle
            if (interrupted) {
                state.settleJob = scope.launch { settle(state, state.offsetX, currentIndex(), currentIndex().toFloat()) }
            }
            return@awaitEachGesture
        }

        val startIndex = currentIndex()
        val width = state.widthPx
        val tracker = VelocityTracker()
        tracker.addPointerInputChange(slop)
        var raw = state.offsetX + overSlopSeen

        fun targetFor(r: Float): Int = if (r < 0f) startIndex + 1 else startIndex - 1
        fun apply(r: Float) {
            val target = targetFor(r)
            val valid = target in 0 until tabCount
            state.offsetX = elastic(r, width * if (valid) MaxFollowFraction else EdgeFollowFraction)
            // capsule follows the real finger: travel of one page width == one tab
            state.fractionalIndex = (startIndex - state.offsetX / width).coerceIn(0f, (tabCount - 1).toFloat())
        }
        apply(raw)

        val finished = horizontalDrag(slop.id) { change ->
            change.consume()
            tracker.addPointerInputChange(change)
            raw += change.positionChange().x
            apply(raw)
        }

        val velocity = tracker.calculateVelocity().x
        val target = targetFor(state.offsetX)
        val validTarget = target in 0 until tabCount
        val travelled = abs(state.offsetX) / width
        val flungSameWay = abs(velocity) > flingPx && sign(velocity) == sign(state.offsetX)
        val commit = finished && validTarget && (travelled >= CommitFraction || flungSameWay)

        val from = state.offsetX
        state.settleJob = scope.launch {
            if (commit) {
                commitTo(state, from, startIndex, target, width, animationsEnabled(), onSwitch.value)
            } else {
                // threshold not met / cancelled / edge: back to the original page, tab unchanged
                settle(state, from, startIndex, startIndex.toFloat(), animationsEnabled())
            }
        }
    }
}

/** spring the page and the capsule back to rest at [indexTarget] */
private suspend fun settle(state: TabSwipeState, from: Float, startIndex: Int, indexTarget: Float, animated: Boolean = true) {
    if (!animated) {
        state.offsetX = 0f
        state.fractionalIndex = null
        return
    }
    val fromIndex = state.fractionalIndex ?: startIndex.toFloat()
    animate(0f, 1f, animationSpec = SettleSpring) { t, _ ->
        // t runs 0 -> 1 with a controlled spring; both values are driven by the same curve
        state.offsetX = from * (1f - t)
        state.fractionalIndex = fromIndex + (indexTarget - fromIndex) * t.coerceIn(0f, 1f)
    }
    state.offsetX = 0f
    state.fractionalIndex = null
}

/**
 * Finish the navigation: the old page leaves in the direction of the finger, the destination is
 * opened (NavHost keeps owning the real back stack and the tab's saved state), and the new page
 * settles in from the opposite side. One continuous surface, one animation per phase.
 */
private suspend fun commitTo(
    state: TabSwipeState,
    from: Float,
    startIndex: Int,
    target: Int,
    width: Float,
    animated: Boolean,
    onSwitch: (Int) -> Unit,
) {
    val dir = sign(-(target - startIndex).toFloat()) // swipe left -> negative offset
    if (!animated) {
        onSwitch(target)
        state.offsetX = 0f
        state.fractionalIndex = null
        return
    }
    val fromIndex = state.fractionalIndex ?: startIndex.toFloat()
    state.committing = true
    try {
        // phase 1: the page keeps going the way the finger sent it and fades out (alpha reaches 0 by 0.4 of the width)
        val exit = dir * width * 0.42f
        animate(from, exit, animationSpec = tween(130, easing = androidx.compose.animation.core.FastOutLinearInEasing)) { v, _ ->
            state.offsetX = v
        }
        // the destination becomes the real current page here, while the page is invisible
        onSwitch(target)
        // phase 2: the new page arrives from the opposite side, equally invisible at first, and springs to rest
        val enterFrom = -exit
        val mid = state.fractionalIndex ?: fromIndex
        state.offsetX = enterFrom
        animate(0f, 1f, animationSpec = IndexSpring) { t, _ ->
            state.offsetX = enterFrom * (1f - t)
            state.fractionalIndex = mid + (target - mid) * t.coerceIn(0f, 1f)
        }
        state.offsetX = 0f
        state.fractionalIndex = null
    } finally {
        state.committing = false
    }
}
