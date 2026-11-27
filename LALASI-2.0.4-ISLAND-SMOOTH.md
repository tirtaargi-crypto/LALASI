# LALASI 2.0.4 - Dynamic Island: minimise tanpa guncangan, font pill tetap

Belum dikompilasi/dijalankan di sini. Build di CI lalu uji di HP.

- Jendela overlay kini selalu selebar sama saat tertutup dan terbuka (`islandCardWidth` = lebar jendela - 12 dp). Buka/minimise hanya mengubah TINGGI jendela (menempel di atas), tidak pernah lebarnya. Resize lebar jendela inilah penyebab pil tersentak ke kiri setelah jeda.
- Minimise memakai spring tanpa pantulan (NoBouncy); lebar pil juga tanpa pantulan. Jeda sebelum jendela menyusut: 1200 -> 900 ms.
- Lirik di pil: ukuran huruf selalu 12 sp (`PillLyricSp`). Dulu 13/11/10 sp tergantung panjang baris, jadi huruf "tiba-tiba besar". Baris panjang kini bergulir (marquee), bukan mengecil.
- Kartu terbuka sedikit lebih sempit dari sebelumnya (maks 348 dp).
