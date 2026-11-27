# LALASI 2.3.0 (belum dikompilasi; build lewat GitHub Actions lalu uji di HP)
Perbaikan Dynamic Island kaca cair (DynamicIsland.kt, IslandLiquidFx.kt):
- Garis lurus vertikal dihapus: itu "garis tepi dalam lensa" (2 garis hitam 1px) di drawIslandGlass.
- Latar cover tidak pixel lagi: dulu cover dibaca 192px lalu dikecilkan ke 128px dan direntangkan ke seluruh island.
  Kini dibaca 448px, diolah di 320px (cache 16 entri, ~6,5 MB).
- Blur latar diturunkan (3 pass box radius 5 di 320px, ~separuh kekuatan lama): cover tetap terlihat tapi tidak tajam.
- Efek melar diperkuat dan dibuat terlihat: tarikan samping 0,20+0,42k (sebelumnya 0,12+0,30k), ditambah smear horizontal
  9 tap rapat ke arah tepi terdekat (warna tampak "ditarik" keluar), mengalir pelan.
- Aksen: digeser ke tepi (tengahnya tepat di dinding, jadi setengahnya melewati rim), lebar 15 -> 19dp, blur 10 -> 4dp supaya
  bentuknya lebih jelas, alpha body/halo/inti dinaikkan.
