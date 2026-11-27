# LALASI 2.2.1 (belum dikompilasi; build lewat GitHub Actions lalu uji di HP)

## Dynamic Island: karakter berbeda untuk tiap album
- Dari cover diambil: warna utama, warna kedua, warna aksen ketiga, seberapa berwarna (energy), seberapa terang (brightness), dan seed dari cover itu sendiri.
- Warna: pita bergradasi pelan dari warna utama ke aksen ke warna dalam sepanjang tubuhnya; cover dengan dua warna yang jelas berbeda memberi kiri dan kanan warna masing-masing.
- Bentuk: lekukan, ayunan, fase, dan lengkungan ujung (mengikuti sudut island) berbeda per album; kiri dan kanan bukan cermin.
- Cahaya: lebar dan sebaran neon mengikuti seberapa berwarna cover; cover gelap = neon murni, cover terang = inti mutiara dan pita sedikit lebih lembut supaya tidak pudar.
- Rim: gradasi warna cover mengelilingi island + glow lembut di sisi dalamnya.
- Performa: tetap satu layer offscreen, tanpa blur; sekitar 4 shader gradien per sisi.

## Versi
- 2.2.1 (versionCode 221).
