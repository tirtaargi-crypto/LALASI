# LALASI 2.0.2 - Notch, camera-aware lyrics, real lock screen features

Not compiled or run on a device in this environment (no Gradle / network here). Build in GitHub Actions and test on a phone.

## Dynamic Island
- Two shapes, Settings > Customise > Dynamic Island > Shape: **Dynamic Island** (floating pill) and **Notch** (flat top flush with the top edge, round bottom corners; open card hangs from the top and starts under the status bar). Key: `DynamicIslandShapeKey`.
- The **title is gone** from the minimised island. With no lyric line it rests at real size: cover | camera | bars, 126 dp wide for the island, 150 dp for the notch. It grows only for a lyric line.
- **Lyrics around the camera** (`DynamicIslandCameraKey`, on by default): the line is split at a word boundary into two balanced halves, one each side of the camera, so no letter sits behind the lens. Both sides get equal room, so the camera is always at the pill's centre. A half still too long at 10 sp scrolls instead of being cut.
- Camera width: detected from the display cutout (Android 11+), else 28 dp; "Detect the camera width" off gives a manual dp stepper (`DynamicIslandCameraGapKey`, -1 = auto).
- Settings previews (small island, position phone) show the chosen shape, the camera gap and no title.
- Position stepper is shared: for the notch 0 dp means flush with the screen top.

## Lock screen
Android does not let an app replace the lock screen, so two real mechanisms are used instead of one overlay:
1. **Lyrics notification on the system lock screen** (`LockScreenLyricsKey`, off by default): `LockLyricsNotifier` in MusicService posts the current timed line as a public notification, replaced line by line, only while the phone is locked with the screen on. Removed on unlock / screen off. No timed lyrics = nothing posted. Needs the notification permission the app already uses.
2. **Real notifications as liquid glass cards** on the Dreamscape page (`DreamscapeNotificationsKey`, off by default): `LockNotificationService` (NotificationListenerService). The user grants "notification access" on the system page. Tap = the phone's own unlock (`requestDismissKeyguard`), then the notification's own intent; swipe sideways = clears the real notification.
   - Left out: LALASI's own, ongoing, group summaries, calls, media, navigation, secret, and silent/minimised ones.
   - Privacy: nothing stored on disk or sent; list is in memory only; off in LALASI = nothing copied. Private notifications show only the app name (`DreamscapeHideSensitiveKey`, on by default); the phone's own "hide sensitive content" and "no notifications on lock screen" settings are respected.

## Files
New: `playback/LockNotificationService.kt`, `playback/LockLyricsNotifier.kt`, `ui/dreamscape/DreamscapeNotifications.kt`.
Changed: `DynamicIsland.kt`, `IslandOverlayController.kt`, `GestureScreen.kt`, `DreamscapeScreen/Activity/Scene.kt`, `DreamscapeSettingsScreen.kt`, `MusicService.kt`, `PreferenceKeys.kt`, `AndroidManifest.xml` (listener service), strings (en + id), version 2.0.2 (202).

## Limits to check on the phone
- The status bar takes touches, so only the 10 dp lip under it opens the notch.
- A camera that is not centred is not followed: the pill stays centred.
- Some OEMs block activities over the lock screen or limit notification access; the lyrics notification still works there.
- Opening a notification from the page after unlock depends on the phone allowing the launch; test it.
