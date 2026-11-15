// customize's animation and element blur settings, mirrored here by mainactivity so draw code and nav lambdas can read them

package com.danrey.lalasi.ui.utils

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.MotionDurationScale
import androidx.navigation.NavBackStackEntry

// the speeds the animation slider stops at, slowest to fastest
val AnimationSpeeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

object AppMotion {
    /** false: nothing animates, every compose animation jumps straight to its end */
    var enabled by mutableStateOf(true)

    /** how much faster than android's own animation speed things move, 1 is just android's */
    var speed by mutableFloatStateOf(1f)

    /** false: lyric lines, title morphs, menus and page wipes stay sharp, glass blur isn't touched */
    var elementBlur by mutableStateOf(true)

    private var systemScale by mutableFloatStateOf(1f)

    /** handed to the window's recomposer, so every animation in the app reads it each frame */
    val durationScale: MotionDurationScale = object : MotionDurationScale {
        override val scaleFactor: Float
            get() = if (!enabled) 0f else systemScale / speed.coerceAtLeast(0.1f)
    }

    // keeps android's animator duration scale in step, the way compose's own scale did before we replaced it
    fun followSystemScale(context: Context): ContentObserver {
        val resolver = context.contentResolver
        fun read() = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        systemScale = read()
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                systemScale = read()
            }
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        return observer
    }
}

/** how a speed reads on screen: 1x, 1.25x, 0.5x */
fun animationSpeedLabel(speed: Float): String {
    val text = if (speed % 1f == 0f) speed.toInt().toString() else speed.toString().trimEnd('0')
    return "${text}x"
}

/** [transition], or no transition at all while animations are switched off */
fun AnimatedContentTransitionScope<NavBackStackEntry>.motionEnter(
    transition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?,
): EnterTransition? = if (AppMotion.enabled) transition() else EnterTransition.None

/** [transition], or no transition at all while animations are switched off */
fun AnimatedContentTransitionScope<NavBackStackEntry>.motionExit(
    transition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?,
): ExitTransition? = if (AppMotion.enabled) transition() else ExitTransition.None

/** The NavHost's own defaults, which can't be null: [transition], or none while animations are off */
fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultMotionEnter(
    transition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition,
): EnterTransition = if (AppMotion.enabled) transition() else EnterTransition.None

/** The NavHost's own defaults, which can't be null: [transition], or none while animations are off */
fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultMotionExit(
    transition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition,
): ExitTransition = if (AppMotion.enabled) transition() else ExitTransition.None


/**
 * How a page changes when you switch tabs. The new page glides in from the side it belongs to, easing out
 * slowly (emphasized decelerate), swelling very slightly from 97%, and fading in a beat after it starts moving.
 * The old page leaves fast and short, so the two never sit on top of each other as ghosts.
 * [direction] is +1 for a page that comes from / goes to the right, -1 for the left.
 */
object PageMotion {
    private val EmphasizedDecelerate = androidx.compose.animation.core.CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    private val EmphasizedAccelerate = androidx.compose.animation.core.CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    // Only translation and alpha: both are applied to the page's layer without re-laying-out or
    // re-drawing it, so a heavy page costs no more per frame than a light one. No scaling.
    fun enter(direction: Int): androidx.compose.animation.EnterTransition =
        androidx.compose.animation.slideInHorizontally(
            animationSpec = androidx.compose.animation.core.tween(260, easing = EmphasizedDecelerate),
        ) { direction * it / 10 } +
            androidx.compose.animation.fadeIn(
                androidx.compose.animation.core.tween(
                    durationMillis = 200,
                    delayMillis = 30,
                    easing = androidx.compose.animation.core.LinearOutSlowInEasing,
                ),
            )

    fun exit(direction: Int): androidx.compose.animation.ExitTransition =
        androidx.compose.animation.slideOutHorizontally(
            animationSpec = androidx.compose.animation.core.tween(150, easing = EmphasizedAccelerate),
        ) { direction * it / 18 } +
            androidx.compose.animation.fadeOut(
                androidx.compose.animation.core.tween(
                    durationMillis = 110,
                    easing = androidx.compose.animation.core.LinearEasing,
                ),
            )
}
