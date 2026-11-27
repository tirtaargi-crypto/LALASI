# LALASI 2.0.3 - Lirik Lock Screen (Aksesibilitas), gaya Aurora, perapihan menu dan pencarian

Status build: kode ini BELUM dikompilasi di lingkungan penyusunan (tanpa jaringan/Gradle). Jalankan build CI dan uji di HP sebelum dianggap berhasil.

## 1. Dreamscape dihapus, diganti Lirik Lock Screen
Dihapus: seluruh `ui/dreamscape/*`, `playback/DreamscapeLauncher.kt`, `playback/LockNotificationService.kt` (sumber error build `visibilityOverride`),
`ui/screens/settings/DreamscapeSettingsScreen.kt`, activity Dreamscape dan listener notifikasi di manifest, semua `Dreamscape*Key` dan string `dreamscape_*`.

Baru:
- `playback/LockLyricsAccessibilityService.kt`: layanan aksesibilitas yang menggambar baris lirik (overlay `TYPE_ACCESSIBILITY_OVERLAY`) di atas layar kunci. Tidak membaca layar (`canRetrieveWindowContent=false`, `onAccessibilityEvent` kosong).
- `playback/LockLyricsBus.kt`: jembatan baris lirik dari pemutar ke overlay + cek apakah akses aksesibilitas sudah dinyalakan.
- `res/xml/lock_lyrics_accessibility.xml` + service di manifest.
- `ui/screens/settings/LockLyricsSettingsScreen.kt` (Pengaturan > Kustomisasi > Lirik Lock Screen): saklar, tombol ke halaman aksesibilitas, posisi (di bawah jam / tengah / bawah), ukuran teks, baris berikutnya, judul dan artis.
- `LockLyricsNotifier.kt`: tetap menjadi penggerak (posisi lagu nyata, lirik bertimestamp). Bila layanan aksesibilitas belum menyala, baris yang sama tampil sebagai notifikasi biasa. Perbaikan: indeks baris di luar daftar (lagu melewati baris terakhir) tidak lagi bisa menyebabkan crash.
- Tampil hanya saat HP terkunci, layar menyala, lagu diputar, dan lirik bertimestamp. Tanpa lirik bertimestamp tidak ada yang ditampilkan.

## 2. Menu pengaturan dan pemutaran
- `SettingsGroup.kt`: garis pemisah kini mulai tepat di bawah teks (sebelumnya meleset 2 dp, dan baris sub-opsi memakai inset yang salah); satu gaya judul untuk semua baris (Inter, 16/15 sp, line height tetap); deskripsi dan judul seksi satu keluarga huruf dan line height.
- `PlayerActionMenu.kt`: tinggi baris menu tetap (min 52 dp) sehingga baris bersaklar/berlabel tidak lebih tinggi; jarak trailing 10 dp; huruf Inter 15 sp; judul seragam kapitalisasi ("Lyrics translation", "AI translation", "Add to liked", "Remove from library", "Edit player", dst).
- `PlaylistSheetKit.kt`: judul kartu aksi memakai gaya huruf yang sama.

## 3. Pencarian
- `SubSettingsSearchBar` (halaman grup pengaturan) kini digambar persis seperti kolom pencarian Pengaturan utama: pil 40 dp, ikon kaca pembesar, teks 17 sp, placeholder abu-abu, outline biru saat fokus, tombol hapus yang tumbuh/menghilang.
- Isi halaman grup berubah ukuran dengan `animateContentSize(250 ms)`, sama dengan halaman utama; baris masuk/keluar sudah 250 ms.

## 4. Gaya pemutar baru: Aurora
- `ui/player/styles/AuroraPlayerStyle.kt`: sampul sebagai kartu kaca melayang dengan cahaya warna sampul, latar sampul blur yang bergeser pelan, baris lirik yang sedang dinyanyikan di pil frosted, seek bar tipis (ketuk/geser), tombol transport, menu •••; mendukung mode horizontal dan putar. Memakai kerangka gaya yang ada (`StyleFrame`, carousel lagu).
- Terdaftar di `PlayerStyle.AURORA`, pemilih gaya (kolom setelah Turntable), dan kartu pratinjau. Tidak ada gaya lain yang dihapus atau diubah.
- Blur latar butuh Android 12+; di bawahnya latar tampil tanpa blur.

## Versi
2.0.3 (203).
