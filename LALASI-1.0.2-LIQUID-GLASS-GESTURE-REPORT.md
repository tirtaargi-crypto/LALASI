# LALASI 1.0.2 — Liquid Glass UI & Gesture Redesign (source edit report)

Status: source edited, NOT compiled. No Gradle/Kotlin toolchain or network was used. Build it yourself and
check `./gradlew :app:compileDebugKotlin` first. Only a static brace/reference check was done.

## Changed files
- ui/component/AppNavigation.kt: navbar rebuilt (collapse, lens, magnification, RGB fringe, elastic press)
- ui/component/BottomSheet.kt: new `collapsedLeftShift` param; pill clip, hairline and shadow narrow from the left
- ui/player/MorphingCover.kt: mini player shifts right in layout phase, new Favorite button in the mini player
- ui/player/BottomSheetPlayer.kt: passes the shift to BottomSheet and MorphingCover
- MainActivity.kt: provides the shift (LocalMiniNavShift), animated expand on tab change

## What it does
1. Navbar: 68dp capsule (+6dp margins = the 80dp the layout already reserved), 20dp side margin, 24dp icons.
2. Collapse on scroll: width/height/corner follow `NavBarScrollState.collapse` (0..1, smoothstep) in the layout
   phase only. Tabs fade out, a Home icon fades in; the capsule ends as a 56dp circle. Tap on the circle = Home and
   the bar springs back open. Tab change also springs it open (was an instant reset).
3. Mini player: its left edge moves right by `MiniPlayerCollapsedShift` (60dp) so it sits beside the Home circle.
   Shift is read in layout/draw (clip shape, hairline, shadow, cover, text), so scrolling does not recompose the player.
   Hidden bar => no shift. Added Favorite (heart) button using the existing `PlayerConnection.toggleLike()`.
4. Lens: starts on touch-down (no drag slop), follows the finger directly (no interpolation), icons magnify with a
   gaussian falloff, red/blue edge fringe for RGB refraction, capsule stretches toward the finger and springs back,
   light spot follows the finger on the top edge. On release the highlight continues from the lens position.
5. Not touched (audited): MediaSession / Media3 `DefaultMediaNotificationProvider` + custom layout in MusicService, and
   the NavHost tab slide/fade transitions (200ms, non-blocking). They already match the brief's structure; I did not
   change them and did not verify artwork/lock-screen behaviour on a device.

## Known risks / to check on device
- Mini player text is narrow while collapsed on ~360dp screens (about 50dp wide, fades at the end).
- "Optical distortion" is approximated by magnification + fringe; there is no real shader refraction.
- `HapticFeedbackType.TextHandleMove` (already used in the file) must exist in your Compose version.
- The mini-player layout change (MorphEndpoints gained like/shift fields) is the riskiest edit; verify the
  open/close morph of the player still looks right.
- No device test of rapid tab switching or scroll-collapse under nested scrollers.

## Revision 2
- Mini player now drops with the collapsing bar (`collapsedDrop`, 72dp at full collapse) so it sits level with the Home circle.
- The bar collapses only while a mini player is showing (`NavBarScrollState.canCollapse`); with none, scrolling never shrinks it, and it springs back open if the player is dismissed.
- Not compiled; check the drop distance on device.

## Revision 3
- FrostedGlass.kt: brighter glass (lighter tint 0.42 -> 0.30 plus a see-through vertical sheen; blur untouched), brighter outline with a specular top run + top-left / bottom-right corner arcs. Shared by navbar and mini player.
- AppNavigation.kt: while touched, a translucent glass bubble (faint fill + hairline, no highlight) swells ~9dp past the capsule edges, drawn outside the clip, and springs back with the press. Removed the old light spots.
- Not compiled; tune alpha values on device.

## Revision 4
- Lens and lit-tab pill: white fills/sheen cut back to near-transparent (hairline kept), so no solid white capsule.
- Page swipe commit (TabPageSwipe.kt): one continuous motion. The page leaves in the finger's direction and fades to invisible, the tab switches while it is invisible, the new page arrives from the opposite side at the same offset and springs to rest (no more jump between 22% offsets). While it runs, `TabSwipeState.committing` makes the NavHost skip its own slide/fade, so two animations no longer fight.
- navHighlight is fed only by bar touch (drag/press) again; the page swipe reaches it through `swipeState`, removing a duplicate per-frame snap and a competing spring after a swipe.
- Not compiled; tab taps on the bar still use the NavHost's 200ms slide/fade.

## Revision 5
- The white capsule was the elastic overshoot bubble, not the tab capsule. The bubble is now clear glass (almost no fill, thin edge, faint red/blue split). The tab/lens capsule is back to its previous look (Revision 4's dimming of it is undone).

## Revision 6
- Navbar elastic bubble: clear glass body + the same bright specular outline as the navbar/mini player (`drawChromeOutline`).
- Home-screen widgets (existing Music, compact, and Turntable RemoteViews widgets) restyled as liquid glass via resources only, no code change: translucent dark body, light sheen, bright edge, white text/icons, glass buttons, white progress bar. Files: res/drawable/widget_*.xml, res/drawable-night/widget_turntable_nav_bg.xml, res/values*/widget_colors.xml, res/values*/colors.xml (widget text colors).
- Limits: a RemoteViews widget cannot blur the wallpaper behind it, so the glass is translucent + sheen, not live blur. Not compiled or checked on a launcher.

## Revision 7
- Removed the white lens capsule (and its red/blue fringe) that was drawn under the finger. While touched, the only effect is the clear glass bubble with the specular outline. The lit-tab highlight is not drawn while a finger/page-swipe is active.

## Revision 8 (widgets like the reference image: two objects)
- Wide widget (widget_music_player.xml): artwork, title/artist, waveform icon, progress bar with elapsed / remaining time, controls like | previous | play (glass circle) | next. The right slot is an empty balance spacer: the app has no shuffle/repeat widget actions, so I did not invent them.
- Square widget (widget_compact_square.xml): artwork on top, previous | play | next underneath.
- Frosted glass body (widget_background.xml): see-through bluish base, bright diagonal sheen, specular edge. New widget_play_circle_glass.xml.
- Code: MetrolistWidgetManager.kt now sets the time labels and wires previous/next to the existing MusicWidgetReceiver ACTION_PREVIOUS / ACTION_NEXT (already handled by MusicService). Not compiled; not seen on a launcher; widgets can't blur the wallpaper.

## Revision 9 (widgets: real blur + customizable liquid colour)
- WidgetStyle.kt (new): renders the widget glass as a bitmap: album art centre-cropped and blurred (downscale/upscale), saturation boost, a flat veil of the liquid colour, two soft colour washes (second shifted 50 deg in hue), diagonal sheen, specular edge. Blur and colour strength are 0..100; colour 0 = auto from the artwork.
- WidgetStyleActivity.kt (new, Compose): colour swatches + Auto + hue slider, blur slider, colour-strength slider, live preview, Apply. Registered in the manifest as the music widget's APPWIDGET_CONFIGURE activity (opens when a widget is added; `reconfigurable` for long-press reconfigure on Android 12+). Settings saved in SharedPreferences `lalasi_widget_style`.
- MetrolistWidgetManager.kt: draws that glass behind the wide and square widgets (cached per art/size/style, sized from the widget's real size). Square layout now used for any widget narrower than 180dp (before, a 2x2 fell into the wide layout).
- widget_music_player.xml / widget_compact_square.xml: now a FrameLayout with a `widget_glass_bg` ImageView under the content.
- Limits: the blur is of the album art, not of the live wallpaper (Android gives widgets no access to what is behind them). Before any song has played there is no art, so the glass is the plain colour wash. The compact-wide and Turntable widgets keep the drawable glass. After Apply, widgets redraw at once only while music is running; otherwise at the next track/update. Not compiled or run.

## Revision 10
- Tab page transitions (NavHost enter/exit/popEnter/popExit) now use `PageMotion` (ui/utils/AppMotion.kt): the new page glides in 1/6 of the width with an emphasized-decelerate curve, grows from 97%, and fades in after a 50ms beat; the old page leaves in 200ms (1/14 width, 98.5%) and fades in 140ms so the two never ghost over each other. Replaces the old 1/8-width default spring + 200ms fades. Not compiled or timed on a device.

## Revision 11 (smooth but light page changes)
- Navigation-bar taps and drags to another tab (from a tab's own page) now go through the same one-page motion as a page swipe (`TabSwipeState.requestSwitch` -> `commitTo`): the old page slides out and fades, the tab switches while it is invisible, the new page springs in. Only one page is on screen at a time, so two heavy pages are no longer composed and animated together. The NavHost's own transition is skipped while it runs (`committing`).
- `PageMotion` (used for the cases that still go through the NavHost: tapping a tab from a deeper page, back navigation) is now translation + alpha only; the scale layers from Revision 10 are removed. Enter 260ms, exit 150/110ms.
- Power-save mode or animations off: switches immediately. Not compiled or timed on a device.
