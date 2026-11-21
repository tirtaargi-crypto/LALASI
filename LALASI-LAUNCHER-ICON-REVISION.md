# LALASI launcher icon revision

- Replaced the unrelated outline/play and anime artwork in every launcher-icon option with the supplied official LALASI brand mark.
- Added white and dark LALASI mark colourways so custom backgrounds automatically use a contrasting mark (dark mark on pale yellow/sky/cream; white mark on darker colours).
- Updated adaptive launcher icon resources for Android 8+ and matching legacy layer-list resources for Android 7.x.
- Updated the Android 13+ monochrome icon layer to use the LALASI mark silhouette.
- Updated the Compose icon picker previews to load the same generated LALASI artwork used by the launcher resources, avoiding preview/installed-icon mismatches.
- Kept existing activity-alias names and saved icon identifiers intact so existing user selections remain compatible.

Validation performed: all XML resources parse successfully and adaptive icon drawable references exist. A Gradle/Kotlin build could not be run because the Gradle distribution host was unreachable in this environment.
