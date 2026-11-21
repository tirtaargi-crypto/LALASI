# Profil biasa + loading screen lebih lambat

- `ui/screens/settings/ProfileScreen.kt`: ditulis ulang. Kartu ID, flip, QR/barcode, gosok (scratch) dan recap dihapus
  dari halaman profil. Sekarang: avatar + nama, tombol "Edit profil" (sheet edit lama), 3 statistik
  (waktu dengar, disukai, playlist), dan menu: Statistik, Pengaturan, Versi & tentang, Ganti ke Akun Tamu.
  Data, DataStore key, ProfileStore, EditProfileSheet, navigasi ("stats", "lalasi_settings") tidak diubah.
- `ui/launch/LalasiLoadingOverlay.kt`: logo 520->900 ms, tulisan LALASI 900->1800 ms, tahan 500 ms setelah selesai
  ditulis, fade keluar 280->450 ms.
- `ui/launch/LaunchIntro.kt`: take-off intro menunggu loading selesai (flag `loadingReleased`, timeout 6 dtk
  supaya tidak pernah macet).
- Splash sistem (`values/styles.xml`, `values-v31/styles.xml`, `splash_blank.xml`) TIDAK diubah: tetap transparan.
- Belum di-compile (tidak ada toolchain Gradle di sini). Jalankan `./gradlew :app:compileDebugKotlin`.

## Onboarding
- Kartu ID di onboarding diganti avatar bulat + nama + label "AKUN TAMU" (`OnboardingAvatar.kt`, komposabel `OnbProfileAvatar`).
  Dipakai di halaman profil setup (avatar bisa diketuk untuk ganti foto), layar "Hello!", dan layar "You're good to go".
- Data tetap sama (nama, foto, card number tetap disimpan di DataStore); hanya tampilannya yang berubah.

## About app
- Cincin teks yang berputar di menu About diganti piringan hitam (`ui/screens/update/AboutVinyl.kt`): cakram hitam dengan
  alur halus + celah antar-track, dan dua pantulan cahaya yang berputar pelan (14 dtk/putaran). Hanya lapisan cahaya
  yang berotasi, alurnya digambar sekali. Zoom/blur/fade saat scroll tetap seperti sebelumnya. Ikon logo jadi "label" piringan.
- `VersionRing` tidak dihapus: masih dipakai Update sheet dan Donate sheet.
