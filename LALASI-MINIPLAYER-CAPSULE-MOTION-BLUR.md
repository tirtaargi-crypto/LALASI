# Mini player di samping navbar: sudut membulat + motion blur lebih jelas, mulus, ringan

Belum di-build / belum diuji di perangkat.

## Perbaikan build (log GitHub Actions terbaru)
- ui/component/HomeCards.kt: `drawOutline` tidak ter-resolve -> tambah import androidx.compose.ui.graphics.drawOutline
- ui/screens/equalizer/EqualizerScreen.kt:191: `androidx.compose.ui.graphics.LalasiInk` (salah paket) -> `LalasiInk`

## Perilaku
- Saat navbar mengecil jadi lingkaran Home dan mini player bergeser ke sampingnya, sudut mini player ikut membulat
  dari 20 dp menjadi kapsul penuh (32 dp), mengikuti progres collapse yang sama dengan geser/turun mini player.
  Hairline, rim iridescent, dan bayangan memakai radius yang sama (bayangan: 9 langkah, tanpa alokasi per frame).
- Motion blur horizontal pada mini player selama bergerak: 1 px per langkah sampai 14 px (halus), dari kecepatan collapse
  (navBarScroll.motion). Lebih kuat dari blur navbar (4 vs 1.8 px per satuan kecepatan).

## Kenapa ringan
- Tanpa gerakan: tidak ada layer tambahan sama sekali (drawContent biasa).
- Saat bergerak: hanya strip mini player (lebar layar x 64 dp) yang direkam ke GraphicsLayer kecil lalu di-blur;
  bukan layer sheet setinggi layar.
- RenderEffect di-cache per langkah (15 buah, dibuat saat pertama dipakai); baca kecepatan hanya di fase draw
  (tidak ada recomposition saat scroll). Hanya Android 12+ (API 31); di bawahnya tidak ada blur.
- Blur hilang saat sheet terbuka > 2%.

## File
- ui/component/BottomSheet.kt   collapsedRound, collapsedMotion, pillRadius(), PillBlurCache, blur di drawWithContent, bayangan
- ui/component/AppNavigation.kt LocalMiniNavRound, LocalMiniNavMotion
- MainActivity.kt               miniNavRound (ease collapse x tinggi navbar), miniNavMotion, dipasang di CompositionLocalProvider
- ui/player/BottomSheetPlayer.kt meneruskan ke BottomSheet

Angka bisa disetel di BottomSheet.kt: PillMotionBlurPerVelocity (kekuatan), PillMotionBlurMaxSteps (batas px), PillCollapsedCornerRadius.
