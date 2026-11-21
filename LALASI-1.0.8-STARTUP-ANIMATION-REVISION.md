# LALASI 1.0.8 — Startup Animation Revision

## Changes
- Removed the extra splash exit fade; the transparent system splash is removed on the first app frame.
- Kept the system splash icon fully transparent so the launcher/Lalasi icon is not shown during startup.
- Removed the secondary mark-flight/intro visual. The only launch visual is the LALASI brand overlay.
- Added the `LALASI menulis` write-on text beneath the LALASI logo.
- Started the brand animation immediately when its Compose overlay enters; removed the artificial hold.
- Kept animation work in graphics/draw phases (alpha, scale, and clipped text reveal) to avoid per-frame layout work.
- Released Home immediately after the short brand animation, with a timeout fallback to avoid startup deadlocks.

## Timing
- Brand reveal: 620 ms.
- Exit fade: 150 ms.
- No extra hold between reveal and exit.

## Verification
- ZIP integrity and modified XML parsing are checked.
- Android Kotlin compilation could not be run in this environment because Gradle 9.3.1 could not be downloaded (`UnknownHostException: services.gradle.org`). Run the project build through Android Studio or GitHub Actions to verify against the configured toolchain.
