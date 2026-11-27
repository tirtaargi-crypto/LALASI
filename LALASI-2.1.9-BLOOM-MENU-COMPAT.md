# LALASI 2.1.9 (belum dikompilasi; build lewat GitHub Actions lalu uji di HP)

## Menu "High quality bloom"
- Penyebab: di Pengaturan > Lirik, baris ini hanya tautan ke App behaviour, dan di sana barisnya disembunyikan kalau gaya lirik bukan "shaded", jadi ketukan tampak tidak menampilkan apa-apa.
- Perbaikan: saklar aslinya sekarang ada langsung di halaman Lirik, dan di App behaviour baris itu tidak lagi disembunyikan.

## Kompatibilitas data lama tanpa nama lama
- Impor arsip transfer: format berakhiran "-transfer" dari build lama tetap dikenali (tanpa menulis nama lama).
- Restore backup: entri berekstensi .db dianggap file library.
- Hasilnya identitas APK tetap LALASI sepenuhnya, tapi data dari aplikasi asal tetap bisa dipindah.

## Versi
- 2.1.9 (versionCode 219).
