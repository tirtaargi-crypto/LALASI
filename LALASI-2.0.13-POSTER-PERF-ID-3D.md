# LALASI 2.0.13 - Poster Studio
Belum dikompilasi di sini. Build lewat GitHub Actions lalu uji di HP.

## Optimasi (frame drop / delay)
- Slider kini ikut mode render kecil (50% ukuran) selama digeser, lalu kembali penuh saat jari diangkat (sebelumnya hanya gestur kanvas).
- Cache aset preview mengikuti memori HP (22% heap, 56-128 MB), tidak lagi tetap 56 MB; foto/karakter besar tidak lagi dibuang lalu di-decode ulang tiap frame.
- Sumber gambar preview di-decode maks 1280 px (export tetap 2048 px) - memori jauh lebih kecil.
- Bayangan player diblur dari salinan setengah ukuran lalu digambar 2x: kerja blur ~4x lebih ringan tiap kali ukuran player berganti.

## Tampilan
- Player lebih gelap dan lebih 3D: warna kartu lebih pekat, sisi kartu jatuh ke bayangan, tepi bawah-kanan punya garis tebal, bayangan dasar lebih kuat, dan player baru langsung agak miring.
- Menu baru di tab Foto: "Geser & ukuran latar" (kiri-kanan, atas-bawah, ukuran).
- Semua menu Poster Studio berbahasa Indonesia; teks penjelasan panjang dihapus atau dipendekkan. Nama font dan teks desain poster tetap.

## Revisi 2 (2.0.14)
- Semua teks penjelasan di Poster Studio dihapus (hanya label dan tombol yang tersisa).
- Depth karakter (subjek foto) kini menjadi LAPISAN di tab Lapisan ("Depth karakter"). Subjek digambar di posisi lapisan itu: yang ada di atasnya menutupi subjek, yang di bawahnya tertutup. Objek baru (stiker, teks, pemutar) otomatis masuk di atasnya, dan lapisan bisa digeser naik/turun atau disembunyikan. Poster lama tetap tampil sama (lapisan dibuat tepat di atas pemutar/teks/stiker terakhir). Lapisan ini hilang otomatis kalau "Subjek menonjol" dimatikan.
