# LALASI 2.3.6 (belum dikompilasi)
Efek melar latar cover Dynamic Island disamakan dengan foto referensi (diam, tanpa animasi):
- Tarikan ke samping dibuat monoton: sx-0.5 = u*(1-k*(2u)^2) dengan k=0.30*amt (<1/3). Rumus lama (k sampai 0.55) berbalik arah
  di dekat tepi (lipatan/ghosting). Kini di tepi kiri-kanan cover terentang ~10x horizontal: goresan warna panjang yang lembut.
- Smear horizontal sangat pendek (0.002+0.010k) supaya goresan halus.
- Blur cover sedikit lebih banyak (2 pass radius 4 di 512px, ~0,7% lebar) seperti referensi; masih HD.
