# LALASI 2.1.6 (belum dikompilasi; build lewat GitHub Actions lalu uji di HP)

## Menu Statistik / Recap
- Profil: baris "Statistik dengar" dihapus. "Recap bulan ini" + "Recap tahun ini" jadi satu baris "Recap" yang membuka halaman statistik baru (`stats`, pemilih bulan/tahun ada di dalamnya).
- Titik tiga di bar streak: tinggal "Recap" (-> halaman statistik baru). "Detail streak", "Statistik", "Recap bulan ini/tahunan" dihapus.
- Halaman Streak: menu "Recap kamu <tahun>" membuka halaman statistik baru. Dialog detail streak di Beranda juga ke sana.
- Halaman statistik baru kini mengikuti tema (iOS, Gelap, Terang, Kaca cair): kanvas, kartu, chip, grafik, penggaris bulan, aksen hijau (lebih tua di Terang). File: StatsScreen.kt, StatsCharts.kt, StatsRuler.kt.
- Rute lama `recap` / `recap_year` (layar recap ungu klasik) tidak lagi dibuka dari menu mana pun; kodenya dibiarkan.

## Info streak (tombol i di halaman Streak)
- Dialog teks diganti dialog bergambar: tiap tingkat api digambar nyata (1-9, 10, 30, 60, 100 hari) dengan centang pada tingkat yang sudah dicapai. Tanpa kapsul.
- Dialog "Detail streak" lama (api, angka, rekor, kapsul 7 hari, tombol Lihat recap) dihapus seluruhnya, juga fungsi kapsul mingguannya.

## Tema Terang
- Saklar on/off (`AppSwitch`): jempol selalu putih (sebelumnya jadi hitam di atas trek gelap), trek mati abu terang di Terang.
- Editor widget (palet warna, gaya, slider): mengikuti tema aplikasi, termasuk saat dibuka dari widget dengan aplikasi tertutup.
- Layar Developer: kerudung bar atas ikut kanvas tema (sebelumnya hitam di Terang).

## Gelembung navbar (iOS & Kaca cair)
- Rim pelangi dan efek gelembung navbar kembali di tema iOS dan Kaca cair (`navBubble = true` di AppNavigation). Mini player dan tombol tetap tanpa pelangi.

## Dynamic Island, kaca cair (`IslandLiquidFx.kt`, baru)
- Latar "melar" 3D: cover yang diburamkan ditarik ke samping seperti karet hangat, mengalir pelan, dan disinari seperti kubah tebal (cahaya lembut + kilau). AGSL, Android 13+; di bawahnya tetap cover buram biasa. Tertutup = tanpa shader.
- Dua pita 3D di kiri dan kanan saat island membesar: pita satin yang berputar (lebar mengikuti |cos| putaran, sisi depan dan belakang beda warna), garis kilau, tepi terang, dan cahaya (BlendMode.Plus). Warna dari cover (Palette: vibrant / light vibrant, dinaikkan ke kecerahan menyala). Tumbuh dari atas dan memudar bersama pegas island.
- Pratinjau di Pengaturan ikut menampilkan keduanya.

## Versi
- 2.1.6 (versionCode 216).
