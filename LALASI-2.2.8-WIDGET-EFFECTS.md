# LALASI 2.2.8 (belum dikompilasi)

Referensi = teks empat efek (Liquid Stretch, Radial Blur, Warp Distortion, Gaussian Blur), bukan foto.

## Latar cover widget
- Liquid Stretch: cover tampak tertarik ke samping, kini terlihat jelas (0,06 + 0,16k; di 2.2.7 terlalu lemah, di 2.2.3 terlalu kuat).
- Radial Blur: warna dan detail menyebar dari satu titik; 5 salinan terfilter, jangkauan terjauh sekitar +13%.
- Warp Distortion: bentuk sedikit terdistorsi seperti permukaan cair (mesh warp "pusaran" yang sudah ada).
- Gaussian Blur: hanya melembutkan hasil distorsi, radius sesuai slider (maks 4px).
- Anti-pecah: cover kecil kini diperbesar bertahap 2x (satu lompatan bilinear besar menimbulkan blok persegi), dan cover yang diperbesar banyak mendapat softening tambahan 1-5px agar tidak tampak berpiksel.

## Versi
- 2.2.8 (versionCode 228).
