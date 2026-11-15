# LALASI 1.0.0 — Crash Root Cause Report

## Evidence
Source: user-provided `logcat.zip` (Android bugreport dated 2026-11-11).
Application process: `com.danrey.lalasi`.
Observed fatal events:
- 2026-11-11 12:09:24.630
- 2026-11-11 12:09:28.965
- 2026-11-11 12:09:39.394

Exception:
`java.lang.IllegalArgumentException: Only VectorDrawables and rasterized asset types are supported ex. PNG, JPG, WEBP`

Stack:
- `PainterResources_androidKt.loadVectorResource`
- `PainterResources_androidKt.painterResource`
- `com.danrey.lalasi.ui.screens.HomeScreenKt.HomeShell(HomeScreen.kt:536)`
- `HomeScreenKt.HomeScreen(HomeScreen.kt:592)`
- `NavigationBuilder.kt:234`

## Proven root cause
`HomeScreen.kt:536` passed `R.drawable.ic_mochy_mark` to Compose `painterResource()`.
The resource `res/drawable/ic_mochy_mark.xml` is a `<bitmap>` wrapper:
`<bitmap android:src="@drawable/lalasi_official_logo" ... />`
It is not a VectorDrawable, and Compose's Android `painterResource()` rejects this XML resource type.

## Source correction
Replace `painterResource(R.drawable.ic_mochy_mark)` and `painterResource(R.drawable.ic_launcher_mark)` with direct references to the underlying raster asset:
`painterResource(R.drawable.lalasi_official_logo)`

Applied to 11 Kotlin source files, including Home, Profile, settings, playlist, recap, update sheets, share canvas, About, and Discord preview. This addresses the same invalid painter-resource pattern wherever found.

The official logo PNG is retained; no logo artwork was recreated or cropped.

## Verification status
- Static source patch: completed.
- ZIP integrity: to be verified after packaging.
- Gradle compilation: not verified in this environment (Gradle distribution download previously blocked by DNS/network).
- Device runtime retest: not performed. The fix is directly tied to the exact resource and stack trace, but a successful rebuild and launch are still required to confirm resolution.

## Addendum (second pass)
- Second latent crash fixed: `CustomisationIcons.kt` `rememberIconArt()` passed `R.drawable.ic_launcher_mark` (a `<bitmap>` XML) to `painterResource` through an id list, which crashes when the app-icon picker under Penyesuaian opens. Now mapped to `lalasi_official_logo` for preview only; launcher alias ids unchanged.
- Launcher icon: foreground/monochrome PNG re-rendered, logo centered at 56% of canvas (inside the 66/108 adaptive safe zone).
- Not done / not verified: full 8-screen redesign, premium loading screen, Gradle build, device runtime test.

## Addendum (third pass): Beranda & Pengaturan
- Settings hub: Tampilan, Pemutaran, Lirik, Audio, Kualitas Streaming, Penyimpanan, Tentang LALASI, each with subtitle + chevron and wired to its real page (customisation_settings, general_settings, customisation_settings→Lyrics animation, audio_lab, audio_quality, storage_settings, About sheet). Integrations and Modules kept. Developer Information card with Instagram/TikTok links.
- Home header: Settings gear added next to the profile avatar (opens mochy_settings).
- Not done: Home bell (no notification feature exists), full Home restyle. Not compiled.

## Addendum (fourth pass): glass + loading
- LalasiLoadingOverlay rewritten: dark gradient, breathing glow, glass sheen clipped to the mark, progress bar driven by real start-up stages (homeComposed, flight), lifted exit.
- lalasiGlassSurface: vertical gradient body + upper-edge specular band (still no blur/offscreen).
- Not compiled/tested on device.

## Addendum (fifth pass): Cari & Koleksi
- Search: glass search field and glass pill rows; recent searches as chips (tap re-runs, x deletes one) with Clear all (clearSearchHistory); calm empty state.
- Library: category and wide cards use the shared glass surface. Tabs/filter chips were already glass.
- Not done: mic button, "Popular Artists" block, Downloads/Favorites row list as in the mockup. Not compiled.

## Addendum (sixth pass): loading screen simplified
- LalasiLoadingOverlay: removed glow, sheen and looping motion. Flat #050505 (splash colour), mark resolves from blur 14->0 with a 0.96->1 settle, hairline progress driven by real start-up stages, plain fade out. Not compiled.
