# LALASI 1.1.18 - Liquid Glass bubble (Home) + Home redesign

NOT COMPILED. The environment this was written in had no Android SDK, no Gradle distribution and no network.
Run first: ./gradlew :app:compileDebugKotlin   then: ./gradlew assembleDebug (or your usual release task)

Files changed (4): ui/component/IridescentBubble.kt, ui/component/TouchFeedback.kt, ui/component/HomeCards.kt, ui/screens/HomeScreen.kt
Not touched: AppNavigation.kt (navbar), BottomSheet.kt / mini player, LalasiGlass.kt, FrostedGlass.kt, theme, any other screen.

Bubble: GlassRimPainter (one closed outline, one seamless SweepGradient mapped to the shape, inside-half stroke under the shape clip),
LiquidGlassIndication (per-node press state, swell + reflection + rim, no idle animation), calmGlassSurface / glassHairline (static).
Home-only switch: LocalHomeLiquidGlass (provided in HomeScreen); Library/artist/album/playlist screens keep the old look.

## Revisi 2 - gelembung seperti referensi navbar
Gelembung tekan di Home sekarang memakai lensa yang sama dengan navbar (IridescentBubble.drawLens): badan kaca buram,
inti gelap, cincin hijau di luar, merah-ke-biru di dalam pada tepi atas/bawah yang melengkung turun di kedua ujung,
tepi krom tipis. Mengembang lebih lebar daripada tinggi dengan pantulan pegas (organik), menjorok keluar dari komponen.
GlassRimPainter dipakai hanya untuk permukaan kaca statis (chip, baris yang sedang diputar, tombol bulat).
Belum dicompile / belum diuji di perangkat.
