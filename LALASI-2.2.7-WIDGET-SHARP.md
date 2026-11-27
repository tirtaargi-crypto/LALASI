# LALASI 2.2.7 (belum dikompilasi)

## Latar cover widget: tajam lagi, tidak pecah
- Radial blur sebelumnya mengambil sampel per piksel dengan pembulatan, sehingga hasilnya berpiksel (blok) dan lambat di bitmap besar. Diganti zoom blur: lima salinan cover yang sedikit membesar dari titik di atas tengah dirata-rata lewat gambar bitmap terfilter, jadi halus di semua ukuran. Jangkauan maksimal +3% per langkah.
- Stretch dikurangi (0,08 + 0,20k -> 0,03 + 0,09k) supaya cover tetap mudah dikenali.
- Gaussian akhir hanya melembutkan sisa distorsi: radius maksimal 4px (sebelumnya 9px; sebelum pipeline baru 14px).
- Urutan tetap: stretch -> radial -> warp -> gaussian.

## Versi
- 2.2.7 (versionCode 227).
