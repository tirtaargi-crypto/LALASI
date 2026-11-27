# LALASI 2.2.0 (belum dikompilasi; build lewat GitHub Actions lalu uji di HP)

## Dynamic Island (mengikuti foto referensi)
- Penyebab "tidak mirip": pita hanya digambar di gaya Liquid Glass, sedangkan gaya bawaan adalah Hitam, jadi pita tidak pernah terlihat.
- Gaya bawaan sekarang Liquid Glass; pita juga muncul di gaya Hitam dan Biru.
- Pita digambar ulang: pita pink lebar berpinggiran lembut menempel di dinding kiri dan kanan, inti mutiara terang, kilau tipis yang mengalir. Warna diambil dari artwork dan dibuat lebih jenuh.
- Rim tipis berwarna artwork mengelilingi island saat terbuka.
- Performa: satu layer offscreen + satu mask gradien per sisi, tanpa blur.

## Streak
- Lingkaran centang di chip Beranda dihapus.
- Tipografi dirapikan: angka dan "hari beruntun" satu baris dengan baseline sama di chip; di halaman, angka 76sp lalu "hari beruntun" di bawahnya, nama tingkat api berwarna api, lineHeight tiap teks diatur.

## Recap gaya story (seperti Spotify Wrapped)
- Halaman Recap punya kartu gradien "Putar story Recap bulan ini / tahun ini" (mengikuti tombol Bulan ini / Tahun ini).
- Story layar penuh: bar progres di atas, ketuk kanan lanjut, ketuk kiri mundur, tahan untuk jeda, tutup dengan X. Slide: intro, waktu dengar, lagu #1, top 5 lagu, artis #1, top 5 artis, hari dan jam favorit, penutup.
- Hanya bar tipis yang digambar ulang saat story berjalan.

## Versi
- 2.2.0 (versionCode 220).
