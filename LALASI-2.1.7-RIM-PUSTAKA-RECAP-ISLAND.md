# LALASI 2.1.7 (belum dikompilasi; build lewat GitHub Actions lalu uji di HP)

## Dynamic Island: pita 3D kiri-kanan
- `IslandLiquidFx.kt`: pita ditulis ulang jadi tabung 3D lembut di tepi dalam kiri dan kanan (halo, badan, perut terang, inti terang, garis kilau yang mengalir), ujung atas-bawah memudar.
- Warna dari artwork: warna dominan cover dipakai dulu (cover pink = pita pink), baru warna vibrant; kejenuhan minimum diturunkan supaya lebih setia ke artwork.

## Rim pelangi -> rim chip Beranda
- Semua elemen selain gelembung navbar dan efeknya sekarang memakai rim perak tertutup yang sama dengan chip Beranda (Semua / Lagu / Playlist / Album): `lalasiGlassSurface`, `iridescentRim`, dan pil mini player.
- Gelembung navbar, rim badan navbar, dan efek lensa sentuhnya tidak disentuh.

## Pustaka
- Tab Semua / Lokal / Dalam cache / Diimpor: pil gelap pembungkus (track) dihapus. Tiap tab kapsul sendiri: terpilih = perak beku teks gelap, lainnya = kaca tenang + rim perak (seperti chip Beranda).
- Tombol "Manage ..." juga kapsul rim, bukan pil terisi.

## Recap di Profil
- Menu Profil: "Recap bulan ini" dan "Recap tahun ini" (rute `stats` dan `stats?kind=year`).
- Halaman Recap: tombol cepat "Recap bulan ini" / "Recap tahun ini" di bawah judul.

## Hapus identitas lama 100%
- `utils/LegacyNames.kt` dihapus beserta 5 pemakaiannya (migrasi database lama, nama enum lama, format arsip transfer lama). Tidak ada lagi teks nama lama di project.
- Akibat: arsip transfer / backup bernama lama tidak dikenali lagi (package com.danrey.lalasi memang instalasi baru, jadi tidak ada data lama di HP).

## Versi
- 2.1.7 (versionCode 217).
