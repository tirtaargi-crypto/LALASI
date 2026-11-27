# LALASI 2.3.3 (belum dikompilasi)
Latar cover Dynamic Island: bentuk jam pasir (X) dihapus. Penyebab: heightField di shader melar memakai min() jarak ke dua tepi,
yang membuat lipatan (crease) di diagonal dan garis tengah; pencahayaan 3D menampakkannya sebagai jam pasir/kotak-kotak.
Diganti perkalian dua falloff tepi yang mulus (sin + exp), tanpa lipatan.
