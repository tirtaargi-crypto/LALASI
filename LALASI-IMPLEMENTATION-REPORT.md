# LALASI — Spotify iOS / Liquid Glass Source Revision

## Source handling
- Modified the supplied Android source project in place; Kotlin + Jetpack Compose architecture, package structure, provider modules, database, Gradle configuration, and assets are retained.
- Updated the shared Search/Library palette to a layered midnight-charcoal treatment. This palette is also consumed by existing settings surfaces.
- Added subtle outlined separation to the existing grouped/classic settings cards and album/playlist detail action controls.
- Retained the existing Home screen's real HomeViewModel data, artwork-led cards, quick access, recent listening, and ContentSafety filtering.
- Retained the existing Search screen's query/results/navigation and its ContentSafety filtering before online results are presented.
- The existing About UI suppresses upstream contributor lists while retaining third-party license notices. Discord screen is not registered in the active navigation graph.

## Protected areas
- No files under `app/src/main/kotlin/com/danrey/lalasi/ui/player/` were modified.
- No playback service, Media3, lyrics provider, lyrics synchronization, queue, or audio engine files were modified.
- SHA-256 comparison against the input archive confirmed all 204 player/lyrics-path files are unchanged.
- No APK/AAB or Gradle application build task was run.

## Important scope limitation
This archive is a source-level visual refinement of an already glass-oriented LALASI source tree, **not** a complete replacement of every eligible screen with a new Spotify-iOS composition. The actual changes in this pass are limited to the shared Search/Library visual palette and shared settings/detail action surfaces. Existing Home, Search, Library, Playlist, Album, Artist, Profile, Settings, and other screens retain their existing layouts and behaviors where no shared style change applies. The metadata filter is conservative and is not image/audio recognition; catalog items without reliable metadata may not be classified. Discord technical implementation files and contributor metadata helpers remain in the repository, but their corresponding user-facing UI entry points are not active/displayed in the inspected UI.

## Validation
- Source archive extracted and inspected.
- Protected player/lyrics source paths compared byte-for-byte with the original archive; unchanged.
- Active navigation checked: no Discord destination registration.
- About UI checked: contributor lists are not rendered; license information remains.
- Static source sanity checks performed on the three modified Kotlin files (balanced braces and parentheses).
- No compile, Gradle build, APK/AAB packaging, emulator run, or device QA was performed, as requested.
