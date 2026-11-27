# LALASI 2.3.9 (belum dikompilasi; build lewat GitHub Actions lalu uji di HP)

Hanya menyentuh Lyrics Coding dan Poster Studio. Semua kontrol, pengaturan, dan pilihan yang sudah ada tidak diubah.

## Lyrics Coding
- **Loading saat lagu baru diputar.** Selama lirik lagu masih dicari, halaman menampilkan loading bergaya editor: komentar `// memuat lirik...`, panggilan `await loadLyrics("judul")` dengan kursor berkedip, dan bar yang terisi lalu mulai lagi (warna ikut tema yang dipilih). Tulisan "belum ada lirik untuk lagu ini" hanya muncul kalau pencarian selesai dan lirik memang tidak ada (atau setelah 15 detik tanpa jawaban, misalnya tanpa koneksi).
- Gaya ini sekarang meminta lirik sendiri (sebelumnya hanya menunggu gaya lain yang memintanya), jadi loading pasti berakhir.
- **Kamera lebih hidup.** Setiap mode kini bergerak terus di atas pose aslinya, dan tiap baris baru mendorong kamera masuk lalu melepasnya pelan:
  - Geser: melayang pelan ke kiri-kanan.
  - Sinema: dolly zoom in-out lambat, geser, goyang tipis.
  - 3D Dynamic: orbit (putar Y/X/Z), geser, dan napas zoom.
  - Fokus: dorongan zoom-in tegas di tiap baris, lalu tenang.
  - Zoom: zoom in dan zoom out bergantian, plus dorongan di tiap baris.
- Gerak berhenti saat finale 3D dan tidak jalan di kartu pratinjau Pengaturan.

## Poster Studio
- **Miring otomatis sesuai posisi.** Pemutar di kiri memakai pose "Miring kiri", di kanan "Miring kanan" (nilai yang sama dengan tombol pose), di tengah miring ke belakang tipis. Berlaku saat menambah pemutar, saat pilih komposisi, dan saat tombol reset objek.
- **Melayang jadi default** untuk pemutar (bukan Mati/datar). Bayangan melayang jatuh menjauhi tengah poster, sehingga kartu terlihat melayang ke tengah: pemutar di kanan melayang ke kiri, di kiri melayang ke kanan.
- **Kilau sesuai posisi.** Pemutar di kiri: kilau di sudut atas kiri; di kanan: atas kanan.
- Pemutar yang digeser melewati tengah poster ikut berganti pose, melayang, dan kilau, selama pose-nya masih pose otomatis. Pose yang kamu atur sendiri (tombol pose atau slider) tidak ditimpa. Tombol Posisi cepat kilau dan semua slider tetap ada.
- Komposisi "Minimal Music" tetap datar karena memang dirancang begitu.

## Daftar file
- `ui/player/styles/LyricsCodingPlayerStyle.kt`: halaman loading, gerak kamera.
- `ui/player/styles/AlternatePlayerStyles.kt`: `StyleLyrics` punya penanda `loading`; `rememberStyleLyrics(readings, load)` (gaya lain tidak berubah).
- `ui/screens/playlist/poster/PosterState.kt`: `autoPlayerPose`, `followPlayerSide`, reset pemutar.
- `ui/screens/playlist/poster/PosterPresets.kt`: pose otomatis saat tambah pemutar dan pilih komposisi.
- `ui/screens/playlist/poster/PosterStudioUI.kt`: pose ikut saat pemutar digeser.
- `ui/screens/playlist/poster/PosterRender.kt`: arah bayangan melayang.
