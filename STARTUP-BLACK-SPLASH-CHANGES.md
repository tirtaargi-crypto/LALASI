# LALASI 1.0.8 - startup black splash handoff (belum di-build / belum diuji di perangkat)

Files changed:
- app/src/main/res/values/colors.xml            lalasi_launch_background #080B12 -> #000000
- app/src/main/res/values/styles.xml            Theme.App.Starting: windowBackground hitam (pre-Android 12)
- ui/launch/LalasiLoadingOverlay.kt             background #000000; first draw -> LaunchGate.onOverlayDrawn()
- ui/launch/LaunchIntro.kt (LaunchGate)         shouldHoldSplash(), onOverlayDrawn(), batas 1 detik (anti-deadlock)
- ui/utils/SplashScreenExit.kt                  setKeepOnScreenCondition { LaunchGate.shouldHoldSplash() }

Tidak dilakukan: audit performa menyeluruh, build Gradle, uji runtime.

## Tambahan: optimasi (audit tahap 1)
- ui/component/LalasiLogo.kt     LalasiLogoCache: logo PNG di-decode sekali per proses (bukan per komposisi di main thread)
- App.kt                         warm-up LalasiLogoCache di Dispatchers.IO saat Application.onCreate
- ui/launch/LalasiLoadingOverlay.kt  ANIMASI BARU (premium): bloom glow + logo rise/scale + gloss terpotong ke piksel logo + hairline + teks terungkap kiri-ke-kanan; 1700 ms; cache logo
  (dioptimasi: brush di-cache, glow hanya mengisi kotaknya, fade-out tanpa layer fullscreen, gloss/mask digeser bukan dibuat ulang tiap frame)
- ui/component/SquigglySlider.kt Path dipakai ulang (reset) alih-alih dialokasi tiap frame saat play

Hasil audit: HeroCarousel, LikedCover, PagesPill, PlayerHandle sudah memakai drawWithCache; baseline-prof, minify,
stability config sudah ada. runBlocking di MusicService/ContextExt dibiarkan (jalur service/background, perubahan berisiko).
Belum dilakukan: build Gradle, profiling Perfetto/Macrobenchmark, uji perangkat.

## Tahap 3: efek elastis + tulisan LALASI ditulis
- ui/component/AppNavigation.kt  NavScrollElasticSpring (damping 0.56, stiffness 400), navCollapseOvershoot/navCollapseElastic;
                                 navbar squash & stretch dari overshoot spring, hanya di graphicsLayer (fase draw)
- MainActivity.kt                mini player (geser & turun) ikut memantul lewat navCollapseElastic (0.8 / 0.5)
- ui/launch/LalasiLoadingOverlay.kt  teks "LALASI menulis" diganti goresan pena L-A-L-A-S-I (PathMeasure) + ujung pena bercahaya;
                                 hairline dihapus. Timeline total tetap 1700 ms.
Splash sistem: Android 12+ tidak bisa dihilangkan; sudah hitam murni, ikon transparan, ditahan sampai frame overlay pertama tergambar.

## Tahap 4: animasi langsung muncul tanpa layar hitam (Android 12+)
- res/drawable/avd_lalasi_splash.xml     BARU: splash animasi vector 1000 ms (logo di-trace dari PNG, IoU 0.98 + goresan L-A-L-A-S-I)
- res/interpolator/lalasi_expo_out.xml   BARU: easing yang sama dgn overlay
- res/values-v31/styles.xml              windowSplashScreenAnimatedIcon -> avd_lalasi_splash, durasi 1000
- ui/launch/LaunchIntro.kt (LaunchGate)  splashAnimated, splashAnimationEnd; splash ditahan sampai animasinya selesai
- ui/launch/LalasiLoadingOverlay.kt      Timing Full (<=Android 11) / FromSplash (12+): overlay meneruskan dari pose akhir splash
- ui/utils/SplashScreenExit.kt           komentar
Android 11 ke bawah: tetap jendela hitam sampai frame pertama, lalu overlay memutar seluruh animasi.
Belum di-build; AVD belum dikompilasi aapt dan belum dilihat di perangkat.

## Tahap 5: warna widget seragam + blur bisa 0
- widget/WidgetStyle.kt            blur 0 = tanpa blur (sampul hanya di-crop & di-scale); blur>0 naik mulus dari 1 px; lapisan abu-abu
                                   kebiruan dihapus supaya warna sampul sama seperti widget Artwork
- widget/WidgetStyleActivity.kt    label nilai blur ("0 (tanpa blur)") dan intensitas warna
- res/drawable/widget_progress_fill|track, widget_turntable_play_bg|nav_bg, widget_play_pill_bg, widget_background:
                                   satu versi putih-kaca untuk semua (varian v31/night/night-v31 dihapus, tidak lagi memakai warna aksen sistem)
- res/layout/widget_music_player.xml  tombol play memakai widget_play_btn_glass seperti widget lain
- res/xml/lalasi_widget_*_info.xml    10 widget LALASI kini memakai layar pengaturan yang sama (configure + reconfigurable)

## Tahap 6: perbaikan kedip dan stuck loading screen
Akar masalah (dari membaca kode):
1. Home membangun konten aslinya SETELAH overlay selesai (rememberLaunchHold menunggu introDone) -> komposisi terberat jatuh tepat di fade-out.
2. Fade-out membuka layar hitam: potongan Home alpha 0 sampai `done` (animasi rise tidak pernah dijalankan lagi) lalu pop.
3. setKeepOnScreenCondition membatalkan draw frame pertama; sinyal overlayDrawn dikirim dari draw -> tidak pernah terkirim -> splash ditahan sampai batas waktu.
Perbaikan:
- ui/launch/LaunchIntro.kt   LaunchGate.releaseHome/onHomeReady/awaitHomeRelease; rememberLaunchHold menunggu rilis (bukan akhir intro);
                             shouldHoldSplash menahan splash sampai animasi AVD selesai DAN Home siap; LaunchIntroState.playRise()
- ui/launch/LalasiLoadingOverlay.kt  urutan: lepas Home -> tunggu frame tenang -> jalankan animasi ringan -> rise Home saat fade;
                             onOverlayDrawn dari komposisi (bukan draw)

## 1.1.0: splash statis + navbar liquid glass (belum di-build / belum diuji di perangkat)
Splash
- res/drawable/lalasi_splash_logo.xml   BARU: logo statis (menggantikan avd_lalasi_splash + interpolator, dihapus); durasi splash 0
- ui/launch/LalasiLoadingOverlay.kt     ditulis ulang: logo statis di posisi/ukuran sama dengan splash sistem, ditahan minimal 650 ms
                                        dan sampai Home selesai dibangun & frame tenang, lalu fade-out 260 ms (per elemen, tanpa layer penuh)
                                        Android 11-: logo fade-in 220 ms dari jendela hitam. Tanpa glow/gloss/pena/blur.
- ui/launch/LaunchIntro.kt              splashHasLogoAnim = false (splash hanya ditahan sampai overlay tergambar)
Navbar (ui/component/AppNavigation.kt)
- gelembung tetap ada sampai halaman tujuan tampil (maks 450 ms), baru menciut elastis (NavPressOut damping 0.62);
  gelembung meluncur ke tab yang dipilih gesture, bukan balik ke tab lama -> tanpa flick; highlight tab disembunyikan selama ditahan
- ditahan: navbar melebar (NavElasticX 0.045) + cahaya radial lembut di bawah gelembung (alpha 0.13, bukan highlight terang)
- gelembung liquid glass: badan transparan, rim luar 1.6dp, rim dalam 4dp, caustic, specular atas
- motion blur horizontal pada navbar saat collapse/expand (RenderEffect API 31+, hanya selama bergerak, efek di-cache per langkah)
- sapuan kilau kaca satu kali setelah aplikasi terbuka
MainActivity.kt
- navbar disembunyikan di Pengaturan dan semua sub-halamannya (SubSettingsRoutes, *_settings); miniplayer turun ke tepi bawah

## 1.1.1 (belum di-build / belum diuji di perangkat)
- ui/utils/AppMotion.kt   motionEnter/motionExit/defaultMotionEnter/defaultMotionExit selalu None: semua transisi halaman dihapus, halaman langsung berganti
- MainActivity.kt         navbar TETAP tampil di halaman profil ("settings"); hanya hilang di halaman yang dibuka dari sana (SubSettingsRoutes, *_settings)
- ui/component/AppNavigation.kt
    gelembung: badan hampir bening + 3 pita tepi yang menebal ke pinggir (efek ketebalan kaca tetes), refraksi di sisi kanan,
    kilau lonjong di atas, rim luar tegas; semua warna solid (tanpa gradient per frame) agar ringan
    titik sentuh: garis meniskus terang di tepi atas & bawah navbar di sekitar gelembung (kesan gelembung menyentuh navbar)
    ikon di bawah jari diperbesar lebih kuat (NavLensMagnify 0.34 -> 0.5)

## 1.1.2 (belum di-build / belum diuji di perangkat)
- GlassKit.kt        GlassChromeBlurRadius 34 -> 12 (blur lebih tipis)
- FrostedGlass.kt    FrostTintAlpha 0.30 -> 0.52 (lebih gelap, tidak pekat); lapisan kilau putih dikurangi
- Dimensions.kt      NavigationBarHeight 80 -> 104 (celah 14 + navbar 74 + naik 16 dari navigasi HP); Slim 64 -> 88
- AppNavigation.kt   navbar 68 -> 74dp, margin samping 20 -> 22, lingkaran Home 56 -> 60; celah atas 14dp ke miniplayer
                     MiniPlayerCollapsedShift/Drop disesuaikan (72 -> 83 dp drop)
- BottomSheet.kt, MorphingCover.kt  PillInset 24 -> 14: miniplayer 8dp lebih lebar tiap sisi dari navbar
- MainActivity.kt    lebar rail landscape tetap 80dp

## 1.1.3 (belum di-build / belum diuji di perangkat)
- MainActivity.kt        tabPageSwipe animationsEnabled = { false }: pindah tab (bar maupun swipe) tidak lagi menggeser/memudarkan halaman
                         (TabPageSwipe.commitTo/settle jalur tanpa animasi) -> gelembung tidak lagi terseret fractionalIndex
- ui/launch/LalasiLoadingOverlay.kt  logo di posisi yang sama dgn splash sistem (offset 13.2dp, bukan lagi di tengah) -> tidak melompat;
                         logo diam sampai frame tenang, lalu tulisan LALASI ditulis pena (PathMeasure, 1000 ms), tahan 260 ms,
                         lalu opacity turun: logo+tulisan lebih dulu, hitam menyusul. Tanpa gradient/blur.
- ui/component/AppNavigation.kt  gelembung digambar DI DALAM clip navbar: atas-bawah rata dgn outline navbar, di ujung mengikuti lengkung navbar;
                         glossy: tutup atas solid + kilau + rim, glow dan sapuan kilau pakai bentuk solid (tanpa gradient); meniskus dihapus

## 1.1.4 (belum di-build / belum diuji di perangkat)
Gelembung kaca pelangi (BARU ui/component/IridescentBubble.kt)
- thin-film (sweep Cyan/Electric Blue/Violet/Lavender/Magenta/Pearl/Icy Silver berputar pelan, 2 lapisan berlawanan arah),
  rim chrome silver, refraksi (busur terang di sisi seberang), glossy cap, specular streak + pantulan kecil yang bergeser,
  bagian tengah tetap bening. Shader/Paint dibuat sekali, tiap frame hanya setLocalMatrix (tanpa alokasi); fase 30 Hz, dibaca di draw saja.
- Navbar: gelembung lebih pendek (0.92-1.02 slot, sebelumnya hingga 1.46), diberi jarak 5dp dari tepi navbar sehingga di tab pertama/terakhir
  tidak keluar/menempel ke ujung; bentuk "bernapas" elastis + spring sentuh yang sudah ada.
- Indikator tab aktif (navHighlight) memakai gelembung yang sama, lebih tenang.
- Mini player: film pelangi + chrome + glint di tepi pill (BottomSheet.kt), memudar saat player dibuka.
Splash & start awal (animasi tidak diubah, hanya waktu tunggu)
- LalasiLoadingOverlay: tahan minimum 350 -> 0 ms, tulis 1000 -> 720 ms, tahan 260 -> 90 ms, fade 340 -> 260 ms,
  tunggu "frame tenang" maks 2000 -> 800 ms (8/5 -> 4/3 frame), splash dilepas setelah 1 frame (bukan 2).
- App.newImageLoader: runBlocking DataStore di main thread diganti nilai yang dibaca di IO saat onCreate (fallback tetap ada).
- Navbar: film pelangi selalu ada di tepi navbar; gelembung (dan indikator gelembung tab aktif) hanya muncul saat ditekan.
- Semua tombol/pill liquid glass (lalasiGlassSurface, pill Back playlist, pill Edit order) memakai film pelangi yang sama.
  Fase pelangi sekarang satu untuk seluruh app (IridescentBubble.kt: SharedPhase), jadi semua permukaan sewarna dan sebaris.

## 1.1.6 (belum di-build / belum diuji di perangkat)
- LalasiLoadingOverlay: tulisan LALASI langsung ditulis di frame pertama (640 ms) saat Home masih skeleton ringan; Home baru dibangun
  setelah tulisan selesai (layar diam), lalu fade 240 ms. Tahan minimal 160 ms. Splash sistem maks ditahan 700 ms (sebelumnya 1000).
- Beranda gaya Spotify (HomeStyle.SPOTIFY, default): chip filter, grid 2 kolom x 4 baris, lalu shelf.
- Streak: utils/StreakManager.kt, ui/component/StreakUi.kt, drawable ic_streak_flame(.xml/_off), streak_strings.xml (en + in).
  Hari dihitung setelah 1 menit musik; animasi di milestone 3, 10, 30, 100, lalu tiap 100. Hook di MusicService.onPlaybackStatsReady.

## 1.1.8 (belum di-build / belum diuji di perangkat)
Navbar liquid glass (AppNavigation.kt, IridescentBubble.kt)
- Refraksi: shader AGSL (Android 13+) pada baris ikon; ikon di bawah gelembung dibengkokkan seperti tetesan air, makin kuat ke tepi,
  dengan pemisahan warna R/G/B (aberasi kromatik). Hanya aktif saat ditekan. Android <13: efek lama (ikon membesar) tetap.
- Tepi gelembung: garis putih kebiruan + dua garis tipis cyan/magenta bergeser (aberasi), pelangi dikurangi jadi semburat tipis.
- Ditekan: navbar melebar dan naik ke atas (tumbuh dari tepi bawah, scaleY +8.5%).
- Highlight di navbar (cahaya lembut + garis kilau atas/bawah) mengikuti gelembung saat digeser.

## 1.1.9 optimasi ukuran + kelancaran (belum di-build / belum diuji di perangkat)
- CI: tambah job build_release (assembleArm64GmsRelease: R8 + shrink + baseline profile, hanya arm64). Job debug universal tetap ada.
- Hapus aset tak terpakai: sans_flex.ttf, google_sans_flex.ttf (8,6 MB), lalasi_wrapped_v1/v2.png (5 MB), thank_you_badge.png, lalasi_discord.png.
- frame_51_3.png -> webp (452 KB -> 55 KB). isCrunchPngs release = true.
- proguard: buang Log.v/d dan Timber.v/d di release, -allowaccessmodification.

## 1.1.10
- CI: job debug dihapus, hanya build_release (arm64).
- Streak dipindah ke item LazyColumn sendiri ("streak") di luar cabang gaya beranda, jadi tampil di semua gaya (Spotify, Lalasi, Listen now, Hop in).

## 1.1.11 widget liquid glass (belum di-build / belum diuji di perangkat)
- WidgetStyle.renderGlass: refractBezel() membengkokkan tepi cover seperti lensa tebal (konten ditarik dari dalam, makin kuat ke tepi) + aberasi kromatik R/G/B; dirender sekali per cover lalu di-cache.
- Garis tepi dalam berwarna biru dingin (kiri atas) dan pink hangat (kanan bawah).
- Tombol widget (play/next/prev/dll): highlight lengkung di atas, glow bawah, rim ganda.

## 1.1.12 (belum di-build / belum diuji di perangkat)
- Picker widget: 10 gambar preview baru (webp, 4 px/dp, tajam) dengan tampilan liquid glass baru. Generator: tools/gen_widget_previews.py (Python + numpy + Pillow).
- Warna efek liquid glass diturunkan (tepi gelembung, aberasi, shader); pelangi tetap ada tapi tipis.
- Widget glass ala iOS 26: bezel refraksi lebih lebar, specular terang kiri-atas / lembut kanan-bawah, bloom, veil bening.
- Gelembung navbar elastis: pop lebih lentur saat ditekan, settle dengan overshoot kecil, squash & stretch mengikuti kecepatan geser.

## 1.1.13 liquid glass iOS 26 untuk semua chrome (belum di-build / belum diuji di perangkat)
- GlassKit: GlassChromeColor jadi gelap lembut kebiruan (#151922), blur 12 -> 14. FrostedGlass: tint 0.52 -> 0.40 (lebih transparan, tetap blur),
  sheen atas lebih terang, bayangan dalam tipis di tepi (rimShade).
- drawChromeOutline ditulis ulang (dipakai navbar, mini player, bottom sheet, lalasiGlassSurface): outline gradien terang di kiri-atas
  & lembut di kanan-bawah, bloom, jejak pelangi sangat tipis, kilau menipis di tepi atas.
- lalasiGlassSurface (chip, baris, tab pustaka, pencarian, pengaturan): badan gelap lembut + sheen, rim pelangi alpha 0.42.
- IridescentBubble.drawRim lebih kalem.

## 1.1.14 (belum di-build / belum diuji di perangkat)
- Navbar yang menciut jadi lingkaran: ikonnya sekarang ikut tab yang sedang dibuka (Beranda, Cari, Pustaka, Kamu), crossfade 180 ms kalau tab berganti.
  Ketuk lingkaran = bar membesar lagi di tab yang sama (dulu selalu pindah ke Beranda).
