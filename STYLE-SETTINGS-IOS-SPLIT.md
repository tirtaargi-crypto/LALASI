# Customize → iOS, Lyrics split, refresh, flat Poster Studio

- Settings > Customize is now an iOS inset-grouped list (same scaffold as App behaviour); each row opens `customisation_detail/{section}`.
- Lyrics is a separate page (`lyrics_settings`) with the style picker and links to the lyric switches; the Lyrics row and search results point there.
- Settings search results use the icon + colour of the page they open (no more identical blue gear).
- A Library refresh now also reloads Home (`AppRefreshBus`).
- Poster Studio controls (PosterUiKit.kt): flat white, no gradient/gloss/shadow.
Not compiled here (no Gradle network).
