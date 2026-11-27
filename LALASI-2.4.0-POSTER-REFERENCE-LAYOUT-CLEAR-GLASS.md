# LALASI 2.4.0 (belum dikompilasi; build lewat GitHub Actions lalu uji di HP)

Hanya menyentuh Poster Studio.

- **Tata letak default sesuai foto.** 4 pemutar mengelilingi karakter: kiri-atas, kanan-atas, kiri-bawah, kanan-bawah. Karakter tetap di depan pemutar. Pemutar ke-5 (kalau ada) di bawah tengah. Berlaku untuk "Kosong" dan komposisi yang memakai tata letak sebar lama. Poster landscape tetap memakai grid seimbang.
- **Pose 3D melayang tetap.** Miring kiri/kanan, melayang, bayangan, dan kilau otomatis tidak diubah; hanya posisi dan ukuran yang diganti.
- **Kaca transparan, bukan biru.** Gaya "Arang" dan "Kaca" kini abu netral tembus pandang tanpa semburat warna sampul; sisi tebal kartu abu netral. Gaya Terang dan Vintage tidak diubah.

File: `poster/PosterPresets.kt`, `poster/PosterVectors.kt`, `poster/PosterRender.kt`.
- **Arah melayang dibalik.** Bayangan melayang sekarang jatuh menjauhi tengah poster, jadi kartu di kanan terlihat melayang ke kiri dan kartu di kiri melayang ke kanan (sebelumnya terbalik). Berkas: `poster/PosterRender.kt`.

## 2.4.2
- **Arah miring dibalik** (pose otomatis): pemutar kiri miring +18°, kanan -18°. Melayang tidak disentuh. `poster/PosterState.kt`.
- **Kaca benar-benar transparan.** Penyebab sebelumnya: bayangan dan tepi tebal kartu digambar di belakang kartu sehingga tembus dan terlihat pekat. Sekarang area kartu dilubangi dari bayangan/tepi (hanya bayangan di sekeliling dan rim yang tersisa), dan warna kaca Arang dibuat lebih tipis. `poster/PosterRender.kt`, `poster/PosterVectors.kt`.

## 2.4.3
- **Rasio poster jadi 12 pilihan + Kustom:** 3:4, 4:5, 9:16, 1:1 (lama), plus 2:3, 3:5, 5:7, 9:19,5, 4:3, 3:2, 16:9, 21:9. Tampil di layar buat poster baru dan di tab Foto > Rasio. Poster lama tetap sama (nomor rasio lama tidak berubah). `poster/PosterModel.kt`, `PosterPanels.kt`, `PosterStudioUI.kt`.
