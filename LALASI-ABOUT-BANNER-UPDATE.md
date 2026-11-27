# About sheet: banner, developer, socials
- Banner artwork (`lalasi_about_banner.webp`) is a full-width banner, not a logo replacement. Eyes are drawn dim in the base image; `lalasi_banner_eye_glow.png` is additively blended on top with an animated alpha (infinite on/off cycle, 3.2 s). The animation value is read only in the draw phase: no recomposition, one small bitmap, no allocations per frame.
- "LALASI by DanRey" signature bottom-left in Cardo (OFL) bold/italic, bundled in res/font.
- Developer card: DanRey, Instagram @devreyw_, TikTok @danhanzyr with brand logos (vector glyphs).
