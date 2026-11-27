# LALASI 2.2.9 (belum dikompilasi; build lewat GitHub Actions lalu uji di HP)
Audit Dynamic Island terhadap spesifikasi "Adaptive Liquid Glass". Hampir semua poin sudah ada sejak 2.2.3-2.2.5
(melar/warp/radial blur AGSL, aksen neon adaptif, morph warna 0,9 dtk, cache warna LruCache).
Perubahan di 2.2.9 (IslandLiquidFx.kt):
- Dither +-0,5/255 di akhir shader melar: menghilangkan banding pada gradasi blur tanpa grain terlihat.
- Android < 13 (tanpa AGSL): peregangan samping berbasis transform GPU, bukan tanpa efek.
- Aksen disamakan dengan referensi: bentuk "kacang" gemuk menempel ke dinding panel (lebar ~2x, ujung membulat, pinggang di tengah), kontur berwarna cover yang terang, tengah susu translucent, glow lembut ke dalam.
- Blur latar cover 14dp -> 22dp (lebih berkabut seperti referensi).
- Cover latar tidak burik: dulu memakai thumbnail kecil apa adanya lalu diperbesar + di-blur; kini diminta 544px (resize) dengan FilterQuality.High. Blur 22dp -> 15dp dan zoom 1.25 -> 1.14 supaya cover masih terbaca seperti referensi, efek melar shader tetap.
- Build fix: RecapStoryScreen.kt memanggil `SlideColumn` yang tidak ada (fungsinya `StorySlideColumn`); 7 pemanggilan diperbaiki.
- Anti-burik: radial blur 5 tap berjarak jauh (hingga ~5% lebar) menghasilkan salinan gambar berundak; kini 9 tap rapat (maks ~1% lebar) di dalam jangkauan blur. Warp dan pull dikurangi (2.2->0.9, 0.22->0.16). Blur latar 26dp, zoom 1.10.
- Aksen presisi: tepi tidak lagi 36 garis lurus bersegi; kini 72 titik dengan kurva kuadratik halus. Digambar dua lapis: lapis lembut (halo, bayangan, badan translusen) di-blur 7dp supaya tepi berundaknya melebur jadi glow mulus, dan lapis tajam (inti, kontur, pantulan) di atasnya agar bentuk tetap presisi.
- Latar cover dibangun ulang: cover diblur gaussian SEKALI (128x128, 3x box blur, di-cache 24 entri ~1,5 MB, thread Default) lalu digambar FilterQuality.High; efek melar/warp (AGSL) bekerja di atas gradasi halus itu, sehingga tidak kotak-kotak. Tidak lagi memakai blur RenderEffect bersarang. Tarikan melar dan warp dinaikkan kembali (0,12+0,30k; 0,9+1,6k) karena sumbernya kini mulus.
- Mode Hitam dan Biru: aksen dihapus, polos.
- Aksen v3: tanpa outline/garis tajam. Satu lapis lembut di-blur 10dp (gaussian glow). Warna diambil dari warna cover yang sudah blur di tepi kiri/kanan (atas, tengah, bawah), jadi gradasinya mengikuti cover; pindah lagu dicampur halus. Beranimasi jelas: lekuk bergelombang, melebar-menyempit, bergeser, plus bercak cahaya yang meluncur naik-turun. Bentuk digeser 20dp dari tepi, lebih lebar; rim panel dilembutkan.
