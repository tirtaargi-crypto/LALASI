# LALASI 2.0.12
Belum dikompilasi di sini. Build lewat GitHub Actions lalu uji di HP.

## Lirik lock screen dihapus
- Dihapus: LockLyricsScreenView, LockLyricsNotifier (notifikasi lirik), LockLyricsSettingsScreen + rute + entri di Gesture/Customisation, 4 kunci preferensi, semua string lock_lyrics_* kecuali label/deskripsi layanan, dan pemakaian notifier di MusicService.
- Dipertahankan: LockLyricsAccessibilityService (nama kelas sama) HANYA sebagai jalur Dynamic Island saat "tampil di atas aplikasi lain" tidak diberikan. Sekarang layanan itu tidak menggambar apa pun sendiri, tidak membaca layar, tanpa receiver dan tanpa pemantau lock screen. Namanya dipertahankan supaya saklar aksesibilitas yang sudah menyala tidak lepas. Labelnya jadi "LALASI Dynamic Island".

## Mode Aurora: lirik
- Tanpa kartu/pembatas: lirik jadi baris bebas. Baris yang dinyanyikan terang dan tajam; baris lain makin redup, kecil, dan blur menurut jaraknya (blur butuh Android 12+, di bawahnya hanya redup).
- Animasi: kolom meluncur 520 ms agar baris aktif mendarat di titik jangkar; baris baru membatalkan luncuran yang masih berjalan (tidak tertinggal); seek/awal tampil melompat langsung; tepi atas-bawah memudar. Hanya daftar yang recompose saat baris berganti, bukan seluruh layar.
- Landscape (dan mode rotate): cover + judul di kiri, lirik penuh tinggi di kanan seperti pemutar media Windows, progress dan tombol di bawah lirik. Portrait: jendela 3 baris di bawah judul.
- Ketuk baris untuk lompat ke baris itu. Scroll manual dimatikan agar tidak bentrok dengan gestur player. Semua operasi scroll dibungkus penangkap error supaya layout yang belum siap tidak membuat crash.
