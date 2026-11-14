# Changelog — LALASI 1.0.0

## Crash fix
- Fixed Compose painter loading of bitmap-wrapper XML drawables by referencing the underlying `lalasi_official_logo.png` raster asset directly.
- Applied consistently to all Kotlin `painterResource()` usages found for `ic_mochy_mark` and `ic_launcher_mark`.
- Preserved the original official logo asset and existing project architecture.
