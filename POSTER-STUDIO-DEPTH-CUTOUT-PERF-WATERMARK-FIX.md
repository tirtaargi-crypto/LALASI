# Poster Studio – depth, autocut, performance, watermark

## 1. Depth on the background subject
- Tap or drag the background photo -> its subject is cut out once (cached PNG, same aspect as the photo) and drawn as a 3D pop-out
  *in front of* music players, text and stickers (drawn right after the last of them). Shadow cast onto what is behind, rim light, slight lift.
- Follows photo pan/zoom exactly (same rect as the photo), uses the photo's brightness/contrast/overlay, stays sharp when the photo is blurred.
- Background tab -> "Depth (subject in front)": on/off, depth slider, Detect again, Refine with brush.
- New doc fields (saved in drafts): bgCutUri, bgPop, bgPopIntensity.

## 2. Auto cutout
- PosterStudioSegmentation.kt: ML Kit first (explicit MlKitContext init); any exception or empty/full mask -> built-in contrast-based fallback.
  The "f4.I.M ... null object reference" error is an R8-obfuscated ML Kit internal in the minified build -> keep rules added in app/proguard-rules.pro.

## 3. Responsiveness
- Preview renders with conflate instead of collectLatest: a render is never cancelled mid-way, so frames appear during drag/resize/swipe.
- Each object now uses an offscreen layer limited to its own bounds (+shadow/outline reach) instead of the whole poster.

## 4. Watermark / logo
- Uses the official lalasi_official_logo (cropped, tinted to the card's ink colour) instead of the old purple icon.png.
- Player card gets a dedicated footer strip (PLAYER_ASPECT 1.78 -> 1.92); the mark is centred, ~90% opacity, no longer overlapping controls.

Not compiled here (no network for Gradle). Build: ./gradlew :app:compileUniversalFossDebugKotlin, and test on a release/minified build for the cutout.

## Depth cleanup (follow-up)
- Cut-out mask is refined with a guided filter against the photo's own edges (hair/clothing), narrower smoothstep, inward bias against background fringe.
- Background pop-out no longer scales up (it was ~2% larger, causing a doubled edge); shadow is a small soft contact shadow; faint rim light.
- Cut-outs made earlier keep the old mask: use Background -> Depth -> "Detect again".

## Responsiveness + simpler controls (follow-up 2)
- CORRECTION: the earlier "conflate" change had landed on the draft auto-save flow, not the preview. Auto-save is back to collectLatest (debounce); the preview now really uses conflate.
- Gestures edit a cheap `preview` document (StudioState.gLive/gEdit) and commit on finger-up -> panels/inspector no longer recompose on every pointer event.
- While a finger is down the poster renders at ~55% size; full quality returns on release. Hit-test masks are not rebuilt while dragging. Boxes are stored as fractions so both sizes mix.
- Handles are bigger (visual 13->17, touch radius 20->34).
- Bottom bar: 7 tabs that fit the screen (no sideways scroll); Frames merged into Effects ("Effects | Frames & overlays"); "Background" -> "Photo".
- Object actions: big labelled buttons for Copy, Delete, Front, Back, Lock, Flip; the rest behind "More".

## Player, glint, auto depth, background effects, buttons (follow-up 3)
- Player layout: blocks (title/artist, progress, controls, volume, output pill, footer) flow down from the artwork and share the spare height; no overlap at any artwork size.
- Thin rim highlight (bright top-left, fading round the card) + soft top sheen. Light glint in a corner (4-point star, bloom, satellites): on/off + corner choice in the player's Player tab.
- Depth is automatic: the photo's subject is cut out as soon as a photo is in the poster (non-blocking "Mendeteksi subjek" tag); never re-enabled after the user switches it off. Photo drags in any tab (pinch zoom on the Photo tab); a new photo starts at 1.15x so it can move in all directions. Cut-outs covering <2% or >80% of the photo are rejected.
- New characters get depth + shadow automatically.
- Background effect (Photo tab): 12 effects with thumbnails + amount slider, applied to the photo only.
- Buttons: gold gradient PillButton, glass GhostButton, StudioChip, gold sliders/switches, section headers with accent, highlighted active tab (PosterUiKit.kt).

## iOS player, automatic shadow, tactile controls (follow-up 4)
- Player redrawn in the iOS "Now Playing" style: frosted-glass card tinted by the artwork colour, floating rounded artwork with gloss, star + ellipsis glass discs next to the title, scrubber with knob, shadowed transport glyphs, volume row, bottom row (lyrics / output / queue), LALASI footer. 3D read: diagonal specular streak, bright top-left rim, dark inner bottom edge.
- Player shadow is automatic (PosterRender.drawObject): contact + key + long ambient layers, follows pose/rotation. The Shadow tab is gone for players.
- Player tab: "3D pose" one-tap presets (Flat, Lean left, Lean right, Floating).
- Controls are no longer Material buttons (PosterUiKit.kt): gold keycap PillButton (bevel, gloss, collapsing shadow, spring press + haptic tick), smoked-glass GhostButton, raised selected StudioChip, icon-first ActionTile, floating dock for the tabs with the active tool lifted on a gold key.
- Fewer controls: Position X/Y sliders removed (dragging does it).

## Depth only where it covers (follow-up 5)
- The photo's subject is now drawn only inside the outline of the players/text/stickers (rotated + tilted like the layer, feathered edge). Everywhere else the poster is the plain photo.
- Dragging the photo re-evaluates this every frame: when the subject reaches a player it steps in front and covers it, with a soft shadow cast onto the player; when it doesn't reach anything, no depth is drawn at all.
- No player/text/sticker on the poster -> no depth (nothing to cover). Subject bounds are cached per cut-out (PosterAssets.popBounds).

## Glint redesign (follow-up 6)
- The corner glint is now a specular on the card rim instead of a star "sticker": a thin arc of light on the edge highlight that fades along both edges, a small hot point with hairline flares, and a tiny speck. Mirrored for the other corners. Same on/off + corner chips.

## Colour (follow-up 7)
- Gold accent replaced with a cool silver to match LALASI's black UI and white logo: silver keycap (white -> ice -> steel), ice-blue accent text/outlines (#CFE0FF), dark-navy ink on keys.
