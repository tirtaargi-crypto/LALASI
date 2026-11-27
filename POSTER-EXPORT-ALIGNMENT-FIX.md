# Poster Studio export alignment revision

- Normalized `Camera` perspective distance to the source object's local bitmap width. This removes the resolution-dependent perspective/tilt difference between editor preview and high-resolution export.
- Export now commits any active gesture transaction before taking the document snapshot, so the saved image uses the exact latest editor state.
- New player additions reflow into an upright two-column composition: left-top, right-top, left-bottom, right-bottom. Player width adapts to poster aspect ratio and all four share the same size; the fifth slot is centered.
- The balanced preset layout also clears residual X/Y tilt so the cards remain aligned.

Validation: source-level checks and ZIP integrity are performed. Android Gradle compilation may require downloading the configured Gradle distribution and is not claimed unless it completes successfully.

## Compile compatibility follow-up

Replaced the unavailable `TextButtonDefaults` reference with the Material 3
`ButtonDefaults.textButtonColors` API in `PosterStudioUI.kt`, preserving the
same white text-button content color.
