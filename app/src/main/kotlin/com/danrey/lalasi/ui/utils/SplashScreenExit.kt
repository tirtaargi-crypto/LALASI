package com.danrey.lalasi.ui.utils

import androidx.core.splashscreen.SplashScreen
import com.danrey.lalasi.ui.launch.LaunchGate

/**
 * Android 12+: the system splash shows the static LALASI logo (lalasi_splash_logo), held only until the Compose
 * overlay has drawn its first frame (LaunchGate.shouldHoldSplash); the overlay shows the same logo in the same
 * place, so removing the splash shows no step. Android 11 and below: a plain black window, removed on the
 * overlay's first frame.
 */
@Suppress("UNUSED_PARAMETER")
fun SplashScreen.handOffToLaunchIntro(introPlays: () -> Boolean) {
    // stay up (plain black) until the Compose overlay, also plain black, has drawn: no gap between the two
    setKeepOnScreenCondition { LaunchGate.shouldHoldSplash() }
    setOnExitAnimationListener { provider ->
        provider.remove()
        LaunchGate.onSplashGone()
    }
}
