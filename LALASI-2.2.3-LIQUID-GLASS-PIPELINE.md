# LALASI 2.2.3 (belum dikompilasi; build lewat GitHub Actions lalu uji di HP)

## Liquid glass: empat langkah efek
1. Liquid Stretch: cover ditarik ke samping menuju tepi (tengah tetap).
2. Radial Blur: warna dan detail menyebar dari satu titik di atas tengah (7 sampel berbobot gaussian di widget, 5 di island).
3. Warp Distortion: gambar dibengkokkan mengikuti normal permukaan kaca, jadi bentuknya bergelombang seperti cairan.
4. Gaussian Blur: melembutkan sisa distorsi.

- Island (shader AGSL, API 33+): tahap 1-3 dalam satu shader (5 sampel). Blur dasar cover diturunkan 22dp -> 14dp dan tint teks naik sedikit (0.24 -> 0.28) supaya gambar lebih terbaca tetapi teks tetap jelas.
- Widget: urutan baru stretch -> radial -> warp -> gaussian. Blur akhir lebih ringan dari sebelumnya (radius maks 9px, dulu 14px). Intensitas stretch/radial/warp mengikuti slider "pusaran" (swirl).

## Presisi aksen di sudut island
- Pita kiri-kanan kini mengikuti lengkung sudut island (radius yang sama dengan rim, 34dp atau setengah tinggi), dari ujung atas sampai bawah, bukan garis lurus.

## Versi
- 2.2.3 (versionCode 223).
