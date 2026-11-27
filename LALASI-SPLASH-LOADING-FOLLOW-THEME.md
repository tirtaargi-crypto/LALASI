# Splash + loading screen mengikuti tema (terang/gelap)

Belum di-build / belum diuji di perangkat (tidak ada Gradle di lingkungan ini).

## Perilaku
- Tema gelap (default): sama seperti sebelumnya - latar hitam, logo putih, pena putih.
- Tema terang: latar #F7F8FB (sama dengan background light scheme), logo dan animasi tulisan LALASI gelap (#111216).

## File
- ui/launch/LaunchTheme.kt (BARU)        cermin pilihan tema (SharedPreferences "launch_theme", dibaca sekali per proses) -
                                         DataStore tidak bisa dibaca sinkron sebelum frame pertama
- ui/launch/LalasiLoadingOverlay.kt      latar, tint logo (ColorFilter.tint), goresan pena + ujung pena memakai warna sesuai tema
- utils/AppIcons.kt (syncSplash)         memilih Theme.App.Starting.Light / Theme.App.Starting lewat setSplashScreenTheme (Android 13+)
- MainActivity.kt                        saat tema berubah: LaunchTheme.save + syncSplash, jadi peluncuran berikutnya sudah benar
- res/values/colors.xml                  lalasi_launch_background_light, lalasi_launch_ink_light
- res/values/styles.xml, values-v31/styles.xml  Theme.App.Starting.Light (latar terang; v31: logo gelap lalasi_splash_logo_light)
- res/drawable/lalasi_splash_logo_light.xml (BARU)  logo splash vektor berwarna gelap

## Batasan
- Splash sistem hanya bisa dipilih per-peluncuran di Android 13+. Android 12/12L dan 11 ke bawah tetap memakai splash/jendela hitam,
  lalu overlay Compose langsung terang (ada perpindahan hitam -> terang yang singkat bila tema terang dipakai).
- Tema yang dipilih diterapkan ke splash mulai peluncuran BERIKUTNYA setelah tema diganti (setSplashScreenTheme berlaku untuk start berikutnya).
  Pengguna yang sudah memakai tema terang: peluncuran pertama setelah update masih gelap, sesudahnya terang.

## Perbaikan build (log GitHub Actions)
- ui/screens/recap/RecapScreen.kt: `private class RecapPalette` bentrok nama dengan `internal class RecapPalette` di RecapVisuals.kt
  (error "Cannot access class RecapPalette: it is private in file", c0..c3 unresolved). Diganti nama menjadi ClassicRecapPalette.
