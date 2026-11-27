# LALASI 2.2.4 (belum dikompilasi; build lewat GitHub Actions lalu uji di HP)

## Adaptive Liquid Neon Side Accents: pergantian warna yang halus
- Saat lagu berganti, semua angka aksen (3 warna, kekuatan glow, terang-gelap cover, mode dua warna, lekukan, ayunan, fase, dan frekuensi kilau tiap sisi) dicampur dari cover lama ke cover baru selama 0,9 detik (ease in-out). Tidak ada kedipan atau lompatan.
- Rim, glow, pantulan, dan pita memakai satu set aksen yang sama, jadi berubah serempak.
- Cover latar island memudar ke cover baru selama 0,6 detik (Crossfade); blur dijalankan sekali pada hasil campuran.
- Selama cover baru dibaca, island tetap menampilkan glow lagu sebelumnya (tidak lagi turun ke warna biru netral di tengah jalan).
- Performa: pembacaan warna terjadi saat menggambar, jadi morph hanya menggambar ulang, tidak merekomposisi.

## Versi
- 2.2.4 (versionCode 224).
