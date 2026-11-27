# LALASI 2.3.7 (belum dikompilasi; build lewat GitHub Actions lalu uji di HP)

## Dynamic Island: hanya hitam
- Gaya Liquid Glass dan Biru dihapus. `DynamicIslandStyle` kini hanya `DEFAULT` (hitam polos); nilai lama yang tersimpan otomatis jatuh ke hitam.
- DynamicIsland.kt: kode kaca cair (backdrop cover, rim, ribbon, efek melar) dihapus dari island; permukaan selalu `Color.Black`.
- Pengaturan > Dynamic Island: daftar gaya (Default/Liquid glass/Blue) dihapus; latar pratinjau jadi abu gelap-hitam.
- IslandLiquidFx.kt dibiarkan karena masih dipakai halaman Recap.

## Player: Flagship, Aurora, Poster
- Pemilih player hanya menampilkan Flagship, Aurora, Poster. Flagship jadi default.
- Gaya lain (Classic, Turntable, Video, iPod, Timeline, Lyrics stage, Bedside, Module) tidak bisa dipilih lagi. Pilihan lama yang tersimpan otomatis dialihkan ke Flagship (`PlayerStyle.offeredOrFlagship()`), termasuk status/nav bar di MainActivity.
- Catatan: nilai enum dan file sumber gaya lama masih ada di source supaya build tidak pecah; tidak ada jalur UI yang mencapainya.

## Aurora (portrait gaya iOS; landscape tidak diubah)
- Cover besar yang mengecil saat pause dan membal keluar saat play (spring), sudut 16dp.
- Judul/artis rata kiri dengan tombol lirik bulat di sampingnya.
- Kontrol transport berupa glyph polos besar (prev / play-pause / next) tanpa lingkaran, menekan dengan spring dan sorotan bulat tipis.
- Menekan tombol lirik atau baris lirik membuka halaman lirik Flagship (`StyleLyricsPage`). Di landscape, tap baris tetap melompat ke baris itu.

## Pengaturan utama dan grup
- Tombol kembali bukan lagi panah biru polos: lingkaran terisi, ikon putih, menekan dengan spring.
- Label kembali ("‹ Audio Lab") tidak lagi biru dan tidak bisa diklik seperti tautan.
- Setiap baris pengaturan kini memberi isian abu saat ditekan (tanpa ripple bergaya tautan).

## Audio Lab
- Kartu "Pengolahan aktif": tinggi tetap 136dp diganti tinggi minimum, line height eksplisit, judul sampai 2 baris, detail sampai 3 baris.
- Baris Audio Lab: status panjang (mis. "Monochrome · Lossless") membungkus 2 baris dan rata kanan, bukan terpotong "...".
