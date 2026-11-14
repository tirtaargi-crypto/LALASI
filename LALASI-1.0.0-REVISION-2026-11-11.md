# LALASI 1.0.0 — Logcat & Frame-Pacing Revision

Date: 2026-11-11
Package: `com.danrey.lalasi`
Input: `LALASI-1.0.0-REDESIGN-COMPLETE-SOURCE.zip` + `logcatt.zip`

## 1. Logcat findings

### Confirmed fatal crash
The supplied Android 16 bugreport contains repeated `FATAL EXCEPTION: main` events for the same Compose resource exception:

`java.lang.IllegalArgumentException: Only VectorDrawables and rasterized asset types are supported ex. PNG, JPG, WEBP`

The first stack points to `HomeScreenKt.HomeShell(HomeScreen.kt:536)` and subsequent events also point to the scrolled Home header around `HomeScreen.kt:1848`. The offending resource in the earlier installed revision was an XML `<bitmap>` wrapper (`ic_mochy_mark` / `ic_launcher_mark`) passed to Compose `painterResource()`.

The supplied source archive already contains the corresponding correction: Home and the other affected UI call sites use the underlying `lalasi_official_logo.png` raster directly. A source-wide audit found 414 `painterResource(R.drawable...)` references, three XML bitmap wrappers, and **zero** references that pass those wrappers to `painterResource()`.

### Other observed events
- The bugreport contains an installation failure `INSTALL_FAILED_UPDATE_INCOMPATIBLE` because the installed package and attempted update had different signing certificates. This is a signing/install mismatch, not an app runtime crash. Uninstalling the old signed build (which removes local app data) or signing updates with the same keystore is required to update normally.
- One captured ANR trace for `com.danrey.lalasi` shows the main thread idle in `MessageQueue.next()` / `Looper.loop()`, not blocked in application code. It does not identify a reproducible app ANR or a specific cause of dropped frames.
- The device bugreport records a 20 ms game-rotation-vector sensor sampling interval. This is an observed periodic workload, not proof that it caused every reported frame drop.

## 2. Changes in this revision

- Reduced device-tilt sensor sampling from `SENSOR_DELAY_GAME` to a fixed 33,333 µs interval (about 30 Hz), appropriate for subtle card parallax while reducing callback pressure during Compose rendering and navigation.
- Added a 0.08-degree dead zone to ignore imperceptible sensor noise and avoid unnecessary graphics-state invalidations.
- Preserved lifecycle registration: sensor listening still starts on `ON_RESUME`, stops on `ON_PAUSE`, and unregisters on disposal.
- Preserved the existing LALASI Soft Black / restrained Liquid Glass design tokens and the existing Home, Search, Collection, Profile, Now Playing, Lyrics, Ambient, and Settings implementations. No navigation route, database, provider, Media3 player, or playback behavior was replaced.

## 3. Reference-design coverage

The source already contains dedicated implementations for the eight screens shown in the supplied reference image. This revision keeps those screens and their current shared visual system; it does not claim a verified pixel-for-pixel rebuild of every component. The screenshot is used as the visual target for the existing screen families, including the player, lyrics, Ambient, and settings pages.

## 4. Validation

- Source-wide invalid bitmap-wrapper painter audit: PASS (0 invalid wrapper references).
- Official logo asset: PASS (PNG, 1200 × 800).
- Source ZIP extraction / repackaging: PASS.
- Gradle/Kotlin compilation: NOT VERIFIED. The wrapper requires Gradle 9.3.1; bootstrap failed with `UnknownHostException: services.gradle.org` in this environment.
- APK generation and physical-device runtime/frame-timing tests: NOT PERFORMED. No claim is made that this revision has passed a device build or that frame drops are fully eliminated.

## 5. Device verification checklist

1. Build using the project's Gradle 9.3.1 wrapper and a configured Android SDK / NDK.
2. Install using the same signing key as the currently installed build, or intentionally uninstall the old build first (backup local data before uninstalling).
3. Launch repeatedly; confirm Home skeleton and full Home render without the `painterResource` exception.
4. Navigate Home → Search → Collection → Profile and open player, lyrics, Ambient, and Settings.
5. Capture a fresh Logcat and Macrobenchmark/Perfetto frame timeline on the target TECNO device. Compare cold startup, warm startup, navigation jank, and frame overrun counts before/after.
