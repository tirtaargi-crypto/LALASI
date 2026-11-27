# LALASI 2.1.8 (belum dikompilasi; build lewat GitHub Actions lalu uji di HP)

## Streak
- Bar Beranda: tinggi 72dp, cahaya api di ujung kiri (warna mengikuti tingkat api), angka besar + "hari", garis tipis menuju milestone berikutnya, cincin "hari ini" di kanan (terisi sesuai menit hari ini, jadi centang bila sudah). Kaca tenang dengan rim perak seperti chip Beranda. Hanya api yang beranimasi.
- Halaman Streak: kartu utama dengan cahaya api lebar di belakangnya, angka besar "hari beruntun", nama tingkat api (kapsul rim), jalur milestone 3 - 10 - 30 - 100 (statis), dua kartu kecil (Rekor terbaik, Hari ini). Menu "Recap kamu <tahun>" tetap paling atas.

## Recap
- Profil: satu menu "Recap". Di halaman Recap ada dua tombol selebar halaman: "Bulan ini" dan "Tahun ini" (di bawah judul, selaras padding halaman).
- Seluruh teks halaman Recap diterjemahkan ke bahasa Indonesia.

## Tema terang dihapus
- Pilihan "Terang" hilang dari Pengaturan > Tema. Pilihan lama yang tersimpan otomatis menjadi Gelap. Seluruh logika terang dimatikan lewat satu titik (`LalasiPalette.light = false`), jadi tidak ada kode yang perlu dibongkar.

## Bloom lirik (kualitas tinggi)
- Mode "High quality bloom": dua cincin sampel (8 titik di radius penuh + 4 titik di ~55% radius, cincin dalam lebih berat) menggantikan 8 titik berjarak sama. Halo jatuh halus, tidak tampak 8 salinan bayangan. Hanya 12 sampel dan hanya di area baris aktif, jadi biaya per frame tetap kecil. Mode biasa tidak berubah.

## Pustaka
- Kartu kategori 84dp / sudut 24dp, kartu lebar 112dp / sudut 26dp, padding teks lebih lega. Tab dan tombol Manage tetap kapsul rim (dari 2.1.7).

## Identitas lama
- Pemindaian ulang seluruh project (teks, biner, nama file): tidak ada lagi nama lama.

## Versi
- 2.1.8 (versionCode 218).
