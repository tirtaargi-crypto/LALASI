# LALASI 1.0.8 — Liquid Glass Widget Collection + Loading Rebuild

Status: **source implemented, NOT compiled, NOT run on a device.**
Gradle could not be downloaded in the environment that produced this change (no network), so no Kotlin compilation, APK build,
Logcat check or frame-timing test was possible. Build and test with Android Studio / GitHub Actions before release.

## What was added

### Widget collection (10 real providers; the existing Music Player and Turntable widgets are kept unchanged)
| Picker name | Receiver | Min size (cells) |
|---|---|---|
| LALASI Compact Player | LalasiCompactWidgetReceiver | 3x1 |
| LALASI Square Player | LalasiSquareWidgetReceiver | 2x2 |
| LALASI Wide Player | LalasiWideWidgetReceiver | 4x1 |
| LALASI Expanded Player | LalasiExpandedWidgetReceiver | 4x2 |
| LALASI Large Square | LalasiLargeSquareWidgetReceiver | 4x4 |
| LALASI Live Lyrics | LalasiLyricsWidgetReceiver | 4x3 |
| LALASI Compact Lyrics | LalasiCompactLyricsWidgetReceiver | 3x2 |
| LALASI Wide Lyrics | LalasiWideLyricsWidgetReceiver | 4x2 |
| LALASI Minimal Controls | LalasiMinimalWidgetReceiver | 3x1 |
| LALASI Artwork Player | LalasiArtworkWidgetReceiver | 2x2 |

- Architecture: the project already used `RemoteViews` through `LalasiWidgetManager`; the new widgets extend that
  (no Jetpack Glance, no second playback engine). All controls go through the existing `MusicWidgetReceiver` -> `MusicService` actions.
- Shared design system: one glass renderer (`WidgetStyle.renderGlass`), one control/typography set; artwork darkening and vignette were
  added to that renderer, so the old widgets get the same treatment.
- Controls: Previous, Play/Pause, Next, Favorite (existing), Shuffle and Repeat (new `ACTION_SHUFFLE` / `ACTION_REPEAT`, handled in `MusicService`
  with the same player properties the in-app buttons use).
- Live lyrics: reads the lyrics already stored for the song and uses their own timestamps (+ the song's lyrics offset). Unsynced lyrics are shown as plain lines
  with no fake sync; no lyrics -> "Lirik tidak tersedia". The service wakes at the next line change (never faster than 400 ms).
- Picker: Android 12+ uses a dedicated preview layout per widget (artwork, text, glass, controls); older Android uses a generated PNG per widget.
- Responsive: type scales with the real widget size; secondary controls / progress / side lyric lines are hidden in an orderly way when space is short;
  the play button is resized on Android 12+.
- Performance: a widget is only pushed to the launcher when something it shows changed; artwork and glass bitmaps are cached; lyrics are only read when a lyric widget is placed.

### Loading and startup
- `LalasiLoadingOverlay` rewritten: only the LALASI logo and the single text "LALASI menulis".
  One clock drives all stages (initial 0-220 ms, logo 60-700 ms with fade + 0.94->1.0 scale + one soft highlight, text 420-900 ms fade + 8 dp translation,
  hold to 1200 ms, exit 1200-1400 ms). Read only in graphics-layer/draw lambdas, so it does not recompose.
- Root causes fixed:
  1. The system splash was black (API < 31) / #050505 (API 31+) while the overlay was #080B12 -> a visible colour step at hand-off. Both now use `@color/lalasi_launch_background`.
  2. `LaunchIntroOverlay` stopped waiting for the overlay after 1.5 s and removed it mid-animation. It now waits up to 6 s (the existing `LoadingWaitMillis`).
- The overlay only composes on a fresh launch (`introRequested`), so recomposition or Activity recreation cannot replay it.

## Not done / honest limits
- **Not compiled or run.** Expect to possibly fix small compile errors on first build.
- `RemoteViews` cannot animate: there is no smooth vertical lyric scroll and no Play/Pause morph inside a widget. The lyric text changes exactly on the line boundary,
  and the icon swaps. Real animation would need a different technology and is not faked here.
- Pre-Android-12 picker PNGs are drawn by a script to approximate each layout; they are not screenshots of a real launcher.
- Startup audit covered the splash theme, `MainActivity.installSplashScreen`, `LaunchGate`, `LaunchIntroOverlay` and the loading overlay.
  Database init, media session, font loading and network init were not profiled; `LaunchGate` already defers heavy start work until the intro finishes.
- Cold/warm start, resize on real launchers, Logcat and frame timing were not tested.
- Unused legacy code (the old Lalasi mark flight in `LaunchIntro.kt`, Lalasi drawables) is left in place: it is not drawn, and removing it was outside "preserve existing".
- Only English and Indonesian strings were added for the new widget labels; other languages fall back to English.

## Files
Added
- `app/src/main/kotlin/com/danrey/lalasi/widget/LalasiWidgetVariants.kt` (variants, intents, 10 receivers)
- `app/src/main/kotlin/com/danrey/lalasi/widget/LalasiWidgetRenderer.kt`
- `app/src/main/kotlin/com/danrey/lalasi/widget/WidgetLyricsSource.kt`
- `app/src/test/kotlin/com/danrey/lalasi/widget/WidgetResourcesTest.kt`
- `res/layout/widget_lw_<key>.xml` and `widget_lw_<key>_preview.xml` (x10 each)
- `res/xml/lalasi_widget_<key>_info.xml` and `res/xml-v31/...` (x10 each)
- `res/drawable-nodpi/widget_preview_<key>.png` (x10)
- `res/drawable/`: widget_glass_default, widget_glass_preview, widget_art_default, widget_art_preview, widget_circle_glass_btn, widget_play_btn_glass,
  widget_progress_bar, widget_scrim_bottom, ic_widget_shuffle(_on), ic_widget_repeat(_on/_one)
- `res/values/lalasi_widget_strings.xml`, `res/values-in/lalasi_widget_strings.xml`

Modified
- `AndroidManifest.xml` (10 receivers)
- `widget/MusicWidgetReceiver.kt` (shuffle/repeat actions)
- `widget/MetrolistWidgetManager.kt` (draws the collection, lyrics frame, refresh hint)
- `widget/WidgetStyle.kt` (artwork darkening + vignette)
- `playback/MusicService.kt` (shuffle/repeat actions, extra widget data, lyric-aware refresh delay, widget refresh on shuffle/repeat change)
- `ui/launch/LalasiLoadingOverlay.kt` (rewritten), `ui/launch/LaunchIntro.kt` (release timeout)
- `res/values/colors.xml`, `res/values/styles.xml`, `res/values-v31/styles.xml` (shared launch colour)
