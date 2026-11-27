# LALASI 2.2.6 (belum dikompilasi; build lewat GitHub Actions lalu uji di HP)

## Story Recap: dibangun dari cover lagunya
- Latar tiap slide = cover lagu atau artis terkait, diperbesar dan di-blur, dengan scrim gelap untuk teks dan zoom pelan. Tidak ada gradien preset. Warna aksen (angka #1, batang tertinggi) diambil dari warna cover.
- Intro: dinding 12 cover dari periode itu, digelapkan; judul besar (bulan / tahun) di bawah.
- Waktu dengar: angka jam raksasa, grafik batang 24 jam (jam puncak menyala), lalu lagu / artis / putaran.
- Lagu #1: cover besar 280dp, judul, artis, putaran, waktu dengar.
- Top 5 lagu dan top 5 artis: baris dengan nomor, cover (artis: foto bulat), putaran atau durasi.
- Artis #1: foto besar, durasi, putaran, dan lagu yang paling sering diputar darinya lengkap dengan cover.
- Hari dan jam favorit: grafik batang 7 hari (hari puncak menyala), nama hari, jam puncak.
- Penutup: cover top 5 bertumpuk, ringkasan jam / lagu / artis, tombol Selesai.
- Tipografi editorial: kicker huruf besar rapat, angka ekstra tebal dengan letter-spacing negatif; tanpa kartu, tanpa pil, tanpa emoji.
- Item muncul bertahap (naik + fade, selisih 80 ms); batang tumbuh. Hanya bar progres dan animasi masuk yang bergerak.

## Versi
- 2.2.6 (versionCode 226).
