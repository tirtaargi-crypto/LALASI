# LALASI 1.0.9 — masuk halaman mulus, widget seragam, navbar bubble

Status: sumber diubah, BELUM di-compile dan BELUM diuji di perangkat (tidak ada Gradle/jaringan di lingkungan ini).
Jalankan `./gradlew :app:compileDebugKotlin` dulu.

## 1. Loading screen: tidak berkedip, tidak stuck, langsung masuk
Akar masalah (dari kode 1.0.8):
- Android 12+: splash sistem memutar AVD 1000 ms, lalu overlay Compose menunggu Home siap (sampai 2,5 dtk) SEBELUM animasinya jalan -> terasa stuck.
- Home baru dibangun/di-"rise" saat overlay fade-out (alpha 0 -> pop) -> kedip dan frame drop tepat di pergantian.
Perbaikan:
- Tidak ada tampilan splash terpisah: animasi loading langsung jalan dari frame pertama.
  - Android 12+: splash sistem memutar `avd_lalasi_splash` (hanya logo: fade-in + naik, ~560 ms). Digambar render thread sistem, jadi tetap mulus walau aplikasi sedang sibuk start. Tulisan dikeluarkan dari AVD supaya bisa ditulis pelan oleh overlay.
  - Android 11 ke bawah: window hitam polos sampai frame pertama, lalu overlay menampilkan logo (fade 560 ms) dan seterusnya.
  - Overlay mulai dari pose akhir AVD (skala 0.6) lalu membesar pelan ke 0.85. Splash ditahan hanya selama animasi logo (600 ms) dan sampai overlay tergambar (2 frame); tidak lagi menunggu Home.
- `LalasiLoadingOverlay.kt`: timeline baru, tulisan LALASI jauh lebih lambat:
  - 0-700 ms gloss di atas logo (ringan); Home dibangun penuh di belakang overlay hitam, sudah dalam posisi akhir (tanpa rise/fade-in).
  - Pena baru mulai saat main thread tenang (maks. tunggu 2 dtk), lalu menulis 1700 ms (sebelumnya 780 ms) tanpa kerja berat lain.
  - Selesai ditulis -> jeda 40 ms -> fade 150 ms -> Home sudah tampil utuh.
  - Glow tetap "bernapas" selama menunggu supaya layar tidak terlihat beku.
- Total normal: sekitar 2,6 dtk.

## 2. Widget
- Widget lama "Music Player" dan "Turntable" dihapus dari picker (`AndroidManifest.xml`). `MusicWidgetReceiver` tetap terdaftar hanya untuk menerima aksi tombol widget baru.
- 10 widget LALASI memakai tampilan "LALASI Artwork Player": sampul tajam (tanpa blur), perlakuan warna, highlight, dan tepi liquid glass yang sama.
  Hanya scrim keterbacaan teks yang berbeda per varian (Artwork 0, lainnya 110, lirik 150). Tombol tidak diubah.
- Efek samping: slider blur di pengaturan widget tidak lagi berpengaruh pada widget (semua mengikuti tampilan Artwork).

## 3. Navbar bubble (gesture)
- Badan bubble tidak lagi transparan: gradasi putih terang (alpha 0,30 -> 0,16 -> 0,22) + outline spekular.
- Saat jari dilepas: bubble meluncur ke tab terpilih dengan spring critically damped (sebelumnya menempel di posisi jari terakhir lalu loncat) dan press-out tidak lagi memantul (damping 0,5 -> 0,86).

## 4. Ganti halaman / scroll
- `PageMotion`: fade halaman masuk tanpa delay, fade halaman keluar ditunda 50 ms -> tidak ada momen kedua halaman sama-sama transparan (kedip antar tab).
- Scroll Home sudah memakai derivedStateOf, key, dan contentType; tidak ada perubahan di sana. Pengurangan jank terbesar ada di poin 1 (Home tidak lagi dibangun saat animasi bergerak).

## 5. Versi dan update tanpa uninstall
- `app/build.gradle.kts`: versionName 1.0.9, versionCode 109 (sebelumnya 1.0.2 / 102).
- Akar masalah uninstall: workflow CI membuat debug keystore baru di tiap run, jadi tiap APK punya tanda tangan berbeda.
  Sekarang `app/persistent-debug.keystore` (baru, ikut di repo; `.gitignore` sudah diberi pengecualian) dipakai oleh semua build type.
- Workflow: langkah "Generate debug keystore" dihapus; APK diberi nama `LALASI-v1.0.9.apk`.
- Update berhasil jika Application ID sama + kunci sama + versionCode lebih tinggi. Kalau APK yang terpasang sekarang
  ditandatangani kunci lain, uninstall DIPERLUKAN SEKALI; setelah itu update langsung.
- README.md ditulis ulang (versi 1.0.9, download, cara update, build, catatan kunci). `fastlane/.../changelogs/109.txt` ditambahkan.

## File diubah
res/values-v31/styles.xml, res/values/styles.xml, res/drawable/avd_lalasi_splash.xml (hanya logo), AndroidManifest.xml, ui/launch/LaunchIntro.kt, ui/launch/LalasiLoadingOverlay.kt,
ui/component/AppNavigation.kt, ui/utils/AppMotion.kt, widget/WidgetStyle.kt, widget/LalasiWidgetRenderer.kt

## Belum dilakukan
Build, uji perangkat, profiling frame (Perfetto/JankStats). Kalau masih ada frame drop di perangkat, kirim log/Perfetto-nya.

## Catatan hand-off logo
Logo di AVD adalah vector hasil trace dari PNG (kemiripan sekitar 98%), sedangkan overlay memakai PNG aslinya.
Bisa ada selisih tepi sangat tipis saat serah-terima; belum dilihat di perangkat.
