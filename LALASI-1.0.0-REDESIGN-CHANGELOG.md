# LALASI 1.0.0 — Redesign Revision Changelog

## Source preservation
- Kept the existing Android project, Kotlin/Jetpack Compose architecture, application ID `com.danrey.lalasi`, namespace, version code/name, provider modules, Room database, Hilt setup, Media3/ExoPlayer playback, MediaSession, background playback, and existing navigation graph.
- Kept the supplied LALASI logo assets and their existing resource IDs. No logo redraw or alternate logo was introduced.

## Changes in this revision
- Refined the dark palette from absolute black to a soft-black foundation (`#08090B`) with charcoal surfaces (`#0B0C0F`, `#17191D`) and more restrained secondary text.
- Changed the fourth top-level navigation item from the old settings/brand-mark presentation to **Profile / Profil**, using the existing profile glyph. The route remains compatible (`settings`) and still opens the existing LALASI profile screen; the Settings entry remains reachable from the profile page.
- Added the Profile navigation label to the default and Indonesian resources.
- Preserved the existing frosted-glass navigation/player chrome and the existing player, lyrics, ambient, library, search, and settings implementations rather than replacing their working repositories or playback wiring.

## Validation performed
- Source archive extracted successfully.
- Verified the application ID and version in `app/build.gradle.kts` remain `com.danrey.lalasi`, `1.0.0`, code `100`.
- Verified the official logo asset is present at `app/src/main/res/drawable-nodpi/lalasi_official_logo.png` and existing mark resource IDs remain in place.
- XML parsing and static source checks are recorded in `BUILD-AND-TEST-REPORT.md`.
- Gradle build was attempted with `./gradlew --offline :app:assembleUniversalFossDebug`; it could not start because Gradle 9.3.1 is not cached and the environment cannot resolve `services.gradle.org` (`UnknownHostException`). No APK was produced.

## Explicitly unverified / remaining work
- Kotlin compilation, Android resource linking, APK installation, device screenshots, playback behavior, lyrics synchronization, Ambient Mode behavior, memory use, frame timing, and accessibility were not runtime-verified.
- This revision does not claim that every legacy screen has been visually rebuilt to pixel-level parity with the supplied eight-screen reference. The existing functional screens and glass/navigation infrastructure were retained; a complete visual pass and device QA still require a working Android build environment and runtime device/emulator.
