# LALASI 1.0.8 — iOS-style Liquid Glass & Loading Motion Revision

## Implemented in this source revision

### Widget glass
- Changed first-run/default widget material settings to blur 52 and tint 32.
- Added a translucent smoky blue-grey optical veil inspired by the supplied reference image.
- Kept the album-art-derived background visible beneath the veil.
- Increased the upper specular reflection and refined the perimeter highlight.
- Kept blur adaptive to the widget's shorter dimension so compact-wide widgets stay clearer than taller surfaces.
- Existing size routing applies the same glass renderer to compact square, compact wide, and full player widgets.

### Launch animation
- Replaced the prior simple reveal with a new single-brand LALASI animation.
- Added a restrained blue-grey radial light bloom behind the logo.
- Added a smooth scale/fade reveal and one subtle moving gloss pass.
- Reveals the exact wordmark `LALASI menulis` with a draw-phase write-on effect.
- Starts immediately when the overlay composes; no extra pre-animation hold is introduced.
- Keeps the transparent/blank Android system splash handoff and does not draw the Lalasi mark.

## Validation
- Source archive integrity checked.
- XML resources remain unchanged by this revision.
- Kotlin compilation was attempted with the Gradle wrapper, but the environment could not resolve `services.gradle.org` to download Gradle 9.3.1. A successful APK build is therefore NOT claimed.
- Runtime smoothness must still be verified on the target Android device.
