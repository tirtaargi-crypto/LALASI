# POSTER STUDIO — Real 3D pose + depth belongs to objects

## 3D pose (PosterRender.kt, PosterPanels.kt)
- Tilt now uses a real pinhole-perspective projection (rotate Y then X, project, `setPolyToPoly`) instead of `android.graphics.Camera` at a huge distance (which was almost orthographic -> "flat").
- Camera distance scales with the object's own size, so preview == export at any resolution (the old code mixed bitmap px with output px).
- Turned objects get face shading (far edge darkens, near edge gets a sheen).
- Turned players get real card thickness (edge stacked backwards in perspective, colour follows player style).
- Pose presets are stronger and now: Flat, Lean left, Lean right, Floating, Tabletop, Hero (gentler angles, ±45 sliders). Player card is shorter (aspect 1.6). Also available for characters/images. Tilt sliders go to +-60.

## Depth is for objects, not players (PosterModel.kt `renderOrder`, PosterRender.kt)
- Characters/images with depth are always drawn in front of players: they cover the card and cast their shadow on it; players no longer cast shadow over them. Hit-testing uses the same order.
- Depth objects get an extra tight contact shadow and a slightly stronger pop-out.

Not compiled here (no Gradle network): run `./gradlew :app:compileUniversalGmsDebugKotlin`.
