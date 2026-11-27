# LALASI 2.1.0 (belum dikompilasi; build lewat GitHub Actions lalu uji di HP)

## Recap lagu (gaya Spotify Wrapped)
- Layar baru `RecapScreen` (route `recap` bulanan, `recap_year` tahunan): intro, total waktu dengar, artis #1, top 5 artis, lagu #1, top 5 lagu, hari dan jam puncak, penutup. Ketuk kanan/kiri untuk maju/mundur, otomatis lanjut tiap 6 detik.
- Memakai `RecapRepository` yang sudah ada (tabel event); tidak ada data baru.
- Kartu streak di Beranda kini bisa diketuk: muncul detail streak + tombol "Lihat recap".
- Profil: baris "Recap bulan ini" dan "Recap tahun ini".

## Dynamic Island, kaca cair
- Gaya `LIQUID_GLASS` kini seperti lensa kaca tebal: pita warna di kedua ujung, garis tepi dalam, kilau atas. Hanya permukaan, bukan tombol.

## Widget
- Editor widget (WidgetStyleActivity, juga dari Pengaturan > Tampilan > Gaya widget): gaya (kaca, kaca bening, gelap, terang, warna cover), warna (dari cover / varian + kustom), kilau kaca %, ketebalan kaca (lensa + tepi) %, frost (blur cover) %, bentuk cover (membulat, kotak, lingkaran).
- Frost kini berlaku di semua widget (sebelumnya dipaksa 0 di widget LALASI). Bawaan frost 35%.

## Tema
- Pengaturan > Tampilan > Tema: iOS, Gelap (bawaan), Terang, Kaca cair. Disimpan di `AppThemeStyleKey`.
- Terang mengubah skema warna Material saja; layar yang menulis warna putih langsung (pemutar, beranda) belum ikut terang.

## Tema Terang / Gelap menyeluruh (revisi)
- Palet adaptif baru `ui/theme/Adaptive.kt`: `LalasiInk` (teks/ikon: putih di tema gelap, hampir hitam di Terang), `lalasiAdapt(hex)` (warna netral gelap/terang dicerminkan di Terang), `LalasiBlack` (kanvas halaman), dan `GlassChromeColor` kini ikut tema.
- Diterapkan ke ~170 file: navbar, mini player, player, menu/sheet, pengaturan, profil, beranda, daftar, equalizer, dsb. Di tema gelap hasilnya identik dengan sebelumnya.
- Player Terang: kerudung terang di atas latar warna cover dan bayangan kontrol berwarna putih, teks/ikon gelap.
- Sengaja tetap gelap (menimpa gambar/video): Dynamic Island, halaman detail playlist/album/artis, gaya player lain (Turntable, iPod, Aurora, Bedside, Poster), mode video, share, onboarding, layar recap, perayaan streak.
