# LALASI 2.0.0 - Dreamscape Lock Screen

Settings > Customise > Dreamscape Lock Screen (route `dreamscape_settings`).

## What is real
- One toggle plus scene (5), animated lyrics, dynamic artwork, particle and visual intensity, reduce motion, battery-friendly mode. Stored in the existing DataStore (`Dreamscape*Key` in PreferenceKeys.kt).
- `DreamscapeActivity`: a normal activity with `setShowWhenLocked(true)`. It never dismisses the keyguard. It binds (flags 0, never starts) to the existing `MusicService` and uses the existing `PlayerConnection`: no second ExoPlayer, no second service.
- `DreamscapeLauncher` (inside MusicService): one SCREEN_ON receiver. Launches the page only if the feature is on, music is playing, the keyguard is locked and "display over other apps" is allowed (Android needs it to start an activity from the background). Otherwise nothing happens and the system MediaSession lock-screen controls remain the fallback.
- Lyrics come from the existing lyrics in the database (`PlayerConnection.currentLyrics`) and follow the real playback position plus the song's lyrics offset. Untimed or missing lyrics show nothing; no lines are invented.
- Rendering: one Canvas, frame clock runs only while the page is resumed, stops under reduce motion, ~30 fps in battery mode. The page closes on screen off or unlock.
- No Accessibility Service, no wake lock, no new dangerous permission (SYSTEM_ALERT_WINDOW was already declared for the Dynamic Island).

## Not done / limits
- Audio-reactive movement is not wired to the audio spectrum.
- Not compiled or run on a device in this environment; build in GitHub Actions and test on the device.
- Some OEMs block activities over the lock screen; then only the native media controls show.

## Other 2.0.0 changes
- App behaviour page regrouped; settings search animated; version 2.0.0 (versionCode 200).

## Revision: realistic scenes, iOS-style page, island minimise fix
- Dynamic Island minimise: the overlay window no longer switches between wrap-content and full width while the pill moves (that late relayout was the flick to the left). It now has two fixed sizes (closed / open), is centred, and only changes size while the pill is still. Open and close are one elastic spring, with no slide or scale of the contents.
- Dreamscape scenes (Android 13+): AGSL shaders in `DreamscapeShaders.kt` for the sky and clouds, glossy liquid (height field, studio reflection, specular, Fresnel, thin-film tint), deep water (surface, light shafts, caustics), nebula and dusk sea. Older phones keep a Canvas gradient fallback. A shader the GPU rejects falls back instead of crashing.
- Floating islands: drawn in Canvas with depth (haze on far ones), rock with strata and edge light, grass cap, trees, hanging roots, small rocks adrift, a waterfall and sun shafts.
- Page layout follows the iOS lock screen: date and large clock, glass player with scrubber and plain transport glyphs, home indicator, spring on press.
- Still not compiled or run on a device here: build in GitHub Actions and check on a phone.
