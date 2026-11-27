# LALASI 2.1.1 (belum dikompilasi; build lewat GitHub Actions lalu uji di HP)

## Ganti tema
- Tema berlaku langsung tanpa buka ulang. Sebelum tema ditulis, layar disalin (PixelCopy) ke ImageView, lalu memudar 360 ms
  saat aplikasi berganti tema di bawahnya (`ui/theme/ThemeTransition.kt`). Tidak ada kedip.
- `LalasiPalette.style` menggantikan flag `light`; semua warna adaptif membacanya, 5 `remember` yang menyimpan warna kini ikut kunci tema.
- iOS: kanvas hitam pekat, panel abu-abu berkelompok (systemGray6), sudut lebih bulat.
- Kaca cair: latar cahaya bergerak (`LiquidBackdrop`), semua panel netral jadi kaca buram putih, sudut sangat bulat.
- Kartu pilihan tema kini menggambar pratinjau tiap tema yang benar-benar berbeda.

## Dynamic Island, kaca cair
- Tanpa pink/ungu dan tanpa film pelangi. Hanya cahaya putih: pita lensa di kedua ujung, kilau atas, kaustik bawah, tepi terang kiri-atas dan kanan-bawah. Warna hanya dari cover yang diburamkan.

## Widget
- Pusaran latar (melar): cover di belakang kaca dipelintir dan ditarik (mesh warp). Slider baru "Pusaran latar (melar)" di editor widget (bawaan 60%).
- Warna: pasangan biru/pink tetap dihapus; kilau tepi dan cahaya kedua kini turunan warna cover.
- HD: bitmap kaca dirender 1.0x densitas (sebelumnya 0.75x), dibatasi 1.6 MP.

## Mode Terang
- Bayangan navbar/mini player, naungan hero dan bar atas Beranda, isi gelembung navbar, dan gambar Audio Lab kini ikut tema.

## Poster Studio
- Geser latar: batas geser diberi kelonggaran seperempat poster dan rentang slider disamakan dengan batas sebenarnya (sebelumnya slider bisa mati).

## Streak
- Kartu baru: angka besar, api di atas cahaya hangat, bar menuju milestone, 7 hari sebagai kapsul (hari hari ini terisi dari bawah).

## Artwork Beranda tidak penuh (pita abu-abu)
- Dua sebab di `HomeCoverCard`: (1) cover proporsi tinggi/lebar tetap digambar persegi (`aspectRatio(1f)` di `ItemThumbnail`) di dalam kartu non-persegi; (2) di grid, lebar kartu dipaksa induk sehingga `size()` mengalah dan cover playlist tetap ukuran lama. Kini kartu mengisi lebar + menjaga rasio, dan gambar/cover mengikuti ukuran kartu yang sebenarnya.

## 2.1.3
- Cover playlist di grid Beranda (`HomeHopInCard`) tadinya digambar tetap 132dp di kotak yang lebih lebar; kini mengikuti ukuran kotak.
- Refresh tema: 60+ nilai warna yang dihitung sekali (di `object` / top-level: AudioLabKit, SearchDesign, DiscordSettingCards, TextStyle toast/profil dll) kini getter, jadi ikut berganti. Transisi menunggu tema benar-benar terpasang sebelum memudar.
- iOS & Kaca cair: rim/gelembung pelangi di navbar, mini player, dan tombol dihapus. iOS = garis tipis abu-abu + sorotan abu-abu; Kaca cair = cahaya putih. Latar aurora lebih terang, skema warna Kaca cair tanpa ungu.

## 2.1.4 Streak
- Kartu streak jadi satu bar kecil (60dp) bersudut membulat: api + "N hari". Tanpa baris hari-hari. Warna mengikuti tema (abu iOS, kaca buram Kaca cair, putih lembut Terang).
- Api baru: cahaya hangat melebar ke samping (ke angka), inti kuning, kedip berbeda-beda, percikan naik memudar. Api mati (belum dengar hari ini) abu-abu tanpa cahaya.
- Tap = halaman recap. Tiga titik / tahan lama = menu: Recap bulan ini, Recap tahunan, Statistik, Detail streak.

## 2.1.5 Halaman Streak
- Tap bar streak membuka halaman baru (`ui/screens/streak/StreakScreen.kt`, rute `streak`): menu "Recap kamu <tahun>" (ikon trending up), api sesuai hari + tombol info, lalu Playlist dan Riwayat diputar.
- Api: tiap hari ganjil warnanya bergeser (hue rotate), 10 hari membesar, 30 hari warna berubah + dua api kecil, 60 hari cincin cahaya, 100 hari terbesar.
- Streak tampil di Beranda tepat di bawah chip Semua/Lagu/Playlist/Album, dan di Profil.
- Bayangan navbar/mini player di tema Terang: lebih lebar, tipis, kebiruan.
