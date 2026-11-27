# LALASI 2.0.8 (dari 2.0.7)
Belum dikompilasi di sini. Build lewat GitHub Actions lalu uji di HP.

## Lirik lock screen
- Geser ke atas di mana saja: halaman mengikuti jari, melewati ~28% layar atau flick cepat -> meluncur keluar dan lock screen asli terlihat. Geser pendek -> kembali.
- Setelah digeser, halaman TIDAK muncul lagi saat baris lirik berganti; baru muncul lagi setelah layar mati atau HP dibuka kunci.
- Overlay sekarang menerima sentuhan (FLAG_NOT_TOUCHABLE dihapus); ketukan biasa tidak melakukan apa-apa.
- Jam gaya iPhone: gembok kecil, tanggal "Minggu 22 November", angka besar semibold (HH:mm untuk 24 jam), bayangan lembut. Petunjuk "Geser ke atas untuk membuka" + home bar di bawah, ikut inset navigasi.
- Drag memakai rawY supaya tidak bergetar saat halaman bergerak.

## Poster Studio
- Tidak ada lagi yang otomatis: autoPlaced dihapus, saklar "Auto lean & glint" dihapus, tilt/pose selalu bisa diatur.
- Kilauan: posisi bebas di sepanjang tepi kartu (slider 0..1 searah jarum jam dari tengah sisi atas) + 4 preset sudut; kekuatan cahaya, ukuran, panjang goresan, warna cahaya (dingin-hangat).
- Poster lama tetap terbuka: glintCorner lama dipetakan ke posisi sudutnya. Cache render diganti (pl-edgeglow3) dan overscan dilebarkan agar cahaya besar tidak terpotong.

## Revisi 2
- Poster Studio: bayangan player tidak lagi jatuh di atas subjek (object depth). Subjek digambar ulang polos di zona bayangan player, jadi bayangan hanya terlihat di luar subjek.
- Lirik lock screen: teks "Geser untuk membuka" dengan kilau yang berjalan (gaya slide-to-unlock iPhone) dan home bar yang naik-turun pelan.
- Audio Lab gaya iOS dengan identitas LALASI: judul besar yang melipat jadi judul kecil di top bar, tombol kembali bulat tembus pandang, kartu hero gradien ungu-oranye dengan spektrum, daftar bergrup dengan ikon kotak berwarna + status abu-abu + chevron, teks bahasa Indonesia. Kerangka halaman (AudioLabPage) dipakai semua sub-layar Audio Lab, jadi Efek Audio, Kualitas, dan Output ikut berganti rangka.

## Revisi 3 (2.0.10)
- Lock screen: geser cukup 10% layar atau flick cepat (sebelumnya 28%); geseran dari strip gestur home sistem (yang dibatalkan sistem) kini tetap membuka; ketuk teks "Geser untuk membuka"/home bar juga membuka.
- Audio Lab: teks tidak lagi pecah. Penyebab: jarak huruf global aplikasi -0,05em ikut ke teks kecil; kini diatur 0 pada semua teks halaman ini.

## Revisi 4 (2.0.11)
- Geser dari posisi mana pun di layar lirik: drag mulai dalam 2dp, halaman ditahan sebagai satu tekstur hardware selama digeser (tanpa render ulang blur tiap frame), buka cukup 8% layar atau flick >300 px/s, animasi buka 90-190 ms.
