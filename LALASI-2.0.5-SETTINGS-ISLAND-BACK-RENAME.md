# LALASI 2.0.5
Belum dikompilasi. Jalankan `./gradlew :app:assembleArm64GmsRelease` lalu uji di HP.
- Pengaturan utama dibangun di atas SubSettingsScaffold/SettingsGroup (sama dengan halaman grup).
- Dynamic Island: lebih lebar, lebar pill stabil, tanpa pantulan, bisa digambar lewat akses aksesibilitas.
- Back: BackMotion (predictive back ala Android 16) di NavigationBuilder.
- Rename lalasi -> lalasi (kecuali URL GitHub, lalasi.apk, lalasi.db, lalasi-transfer, lalasi-web-player-query, lalasi.png).

## Revisi 2
- Lirik lock screen: full layar (LockLyricsScreenView) dengan latar cover blur + aurora dari warna cover, jam gaya iOS, lirik besar rata kiri; hilang seketika saat dibuka kunci; toggle langsung berlaku.
- Ikon pencarian baru (ios_search.xml) di semua halaman pengaturan.
- Baris pengaturan: indentasi seragam, judul + nilai slider tidak lagi pecah.
- Player Aurora: warna hanya dari cover, gaya Now Playing iOS.

## Revisi 3
- Rename tuntas: tidak ada lagi "nama lama" di project (kompatibilitas nama lama dihapus di 2.1.7).
- Poster Studio: player otomatis miring ke tengah (kiri->Lean right, kanan->Lean left), glint otomatis ke sudut sisi tengah, ukuran glint penuh, glow menyesuaikan. Saklar "Auto lean & glint".
