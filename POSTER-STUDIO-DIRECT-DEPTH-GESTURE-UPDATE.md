# POSTER STUDIO — DIRECT DEPTH & GESTURE SIMPLIFICATION

## Changes
- Depth activation is now a direct canvas action on existing CHARACTER and IMAGE layers.
- Tapping or dragging an eligible existing subject applies the existing Quick Depth recipe automatically.
- No extra character/object is created for depth, and no dedicated Depth inspector tab or Depth settings panel is shown.
- Existing depth rendering/model fields remain intact so older saved poster drafts retain their stored appearance.
- Updated the Objects panel hint to explain tap/drag, pinch resize, and rotation.

## Interaction behavior
- One-finger drag moves the selected object.
- Two-finger pinch/rotate continues to resize, rotate, and translate the selected object.
- Background gestures continue to pan/zoom the background when the Background tab is active.
- Existing undo grouping is preserved; the automatic depth application is committed as an undoable edit.

## Validation
- Source-level changes were reviewed.
- Gradle compilation could not be completed in this environment because the Gradle 9.3.1 distribution was not cached and network DNS access to services.gradle.org was unavailable.
- Run `./gradlew :app:compileUniversalGmsDebugKotlin` in Android Studio or CI to complete the build validation.
