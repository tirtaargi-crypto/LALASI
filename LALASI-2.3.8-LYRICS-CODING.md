# LALASI 2.3.8 (belum dikompilasi; build lewat GitHub Actions lalu uji di HP)

## Perbaikan build 2.3.7
- SettingsGroup.kt: `collectIsPressedAsState` dipanggil sebagai extension (`rowSource.collectIsPressedAsState()`) dengan import-nya.
- AuroraPlayerStyle.kt baris 403: lambda seek diberi hasil `Unit` supaya tipenya `() -> Unit`, bukan `Unit?`.

## Player baru: Lyrics Coding
Dibuat dari `Lyrics_Coding.html`, ditulis ulang dengan Compose (file baru `ui/player/styles/LyricsCodingPlayerStyle.kt`).
- Tiap baris lirik dibungkus pernyataan kode (Python, JavaScript, CSS, Rust, SQL, Go) dan diwarnai seperti tema sintaks.
- Baris yang sedang dinyanyikan diketik huruf demi huruf mengikuti posisi lagu, dengan kursor berkedip. Baris di atasnya memudar dan mengabur menurut jaraknya, baris berikutnya belum muncul.
- Kamera: Geser, Sinema, 3D Dynamic, Fokus, Zoom. Setelah baris terakhir selesai diketik, halaman miring ke finale 3D.
- Warna: Monokai, Dracula, Nord, Gruvbox, Mono. Efek: Normal, Blank, Error, Sensor, Glitch, Neon, Terminal. Ukuran teks: Kecil, Sedang, Besar.
- Popup saran ala VS Code muncul di atas kata yang sedang diketik pada setiap baris ketiga.
- Pilihan disimpan di preferensi dan diubah lewat tombol `</>` di kanan atas nama file.
- Lirik, offset lirik, progres, dan tombol putar/sebelumnya/berikutnya memakai yang sudah ada di app (komponen Aurora dibuka jadi `internal`: `AuroraProgress`, `AuroraTimes`, `AuroraTransport`, `glideTo`).
- Ketuk baris untuk lompat ke baris itu. Lagu tanpa lirik tersinkron menampilkan satu baris komentar.

## Dihapus dari versi HTML (sudah ada di app)
Kolom cari lirik (lrclib), pilih file audio, tempel LRC, lirik demo, slider offset, layar penuh, pemilih font web, dan auto-hide dock.
Font kode memakai monospace bawaan Android (font web tidak ikut dibundel).

## Daftar file
- Baru: `LyricsCodingPlayerStyle.kt`.
- `PreferenceKeys.kt`: nilai enum `LYRICS_CODING`, ikut `isOffered` dan `selectorColumns`, lima kunci preferensi `LyricsCoding*Key`.
- `AlternatePlayerStyles.kt`: dispatch ke gaya baru.
- `CustomisationPreviews.kt`: pratinjau mini di Pengaturan.
