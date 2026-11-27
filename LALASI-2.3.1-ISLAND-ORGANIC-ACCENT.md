# LALASI 2.3.1 (belum dikompilasi; build lewat GitHub Actions lalu uji di HP)
Aksen Dynamic Island kaca cair mengikuti foto referensi (IslandLiquidFx.kt):
- Aksen tidak bergerak: animasinya memakai rememberInfiniteTransition, yang ikut skala animasi sistem (0 / hemat daya = diam).
  Kini digerakkan per frame (withFrameNanos, rememberFlowTime) sehingga selalu bergerak selagi island terbuka. Efek melar latar juga.
- Bentuk organik "kacang": ujung membulat, pinggang yang naik-turun, lebar yang mengembang-menyempit berombak, seluruh bentuk
  bergoyang pelan, bercak cahaya meluncur di dalamnya. Duduk menempel dinding dan sedikit melewati rim.
- Pewarnaan: badan jenuh dari warna cover (atas/tengah/bawah tepi), inti lebih terang seperti susu, glow lembut ke tengah.
- Gaussian blur 5dp (tidak terlalu blur).
