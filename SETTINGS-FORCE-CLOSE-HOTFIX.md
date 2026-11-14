# LALASI 1.0.0 — Settings Force-Close Hotfix

## Root cause
The supplied Android bugreport contains repeated `FATAL EXCEPTION: main` crashes in
`com.danrey.lalasi` with:

`IllegalArgumentException: Only VectorDrawables and rasterized asset types are supported`

The latest occurrence in the report is at 11-11 12:33:09 and points to
`ProfileScreen.kt:782`, where the profile/settings screen loaded
`R.drawable.lalasi_official_logo` through Compose `painterResource()`.
Earlier occurrences point to the same logo resource in `HomeScreen.kt:536`
and `HomeScreen.kt:1848`.

## Fix
- Added `app/src/main/assets/lalasi_official_logo.png`.
- Added `LalasiLogo.kt`, which decodes the PNG from the app assets using
  `BitmapFactory` and caches the resulting `ImageBitmap` with `remember`.
- Replaced all three `painterResource(R.drawable.lalasi_official_logo)` call
  sites in HomeScreen and ProfileScreen with `LalasiLogo`.
- Kept the existing logo artwork, dimensions, and content scaling.

## Verification status
- Source-level checks: all three known logo `painterResource` call sites are removed.
- Build: not verified in this environment. Gradle wrapper requires
  `gradle-9.3.1`, but `services.gradle.org` could not be resolved (DNS/network).
- Device verification: still required on the target TECNO device. Rebuild and
  reinstall the APK before testing the Settings/Adjustment menu.
