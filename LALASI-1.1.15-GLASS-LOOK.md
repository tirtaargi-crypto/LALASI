# LALASI 1.1.15 — tampilan kaca navbar / miniplayer / gelembung

Belum dicompile. Jalankan `./gradlew :app:compileDebugKotlin` dulu.

- FrostedGlass.kt: tint kaca 0.40 -> 0.30 (warna konten di belakang lebih tembus), sheen dan garis tepi lebih lembut, highlight tab aktif lebih terang. Dipakai bersama oleh navbar dan miniplayer.
- GlassKit.kt: GlassChromeColor jadi hitam netral (0xFF0B0D12).
- IridescentBubble.kt: palet jadi spektrum (biru, cyan, hijau, kuning, merah, magenta). Warna ada di tepi atas dan bawah gelembung, tepi samping perak tipis. Gelembung dipotong mengikuti outline navbar (`bounds`), jadi di ujung navbar tepi atas/bawah melengkung ikut navbar.
- AppNavigation.kt: garis cahaya atas/bawah yang tadinya lurus kini outline kapsul navbar itu sendiri (ikut melengkung di ujung); rim pelangi navbar saat diam dikecilkan (alpha 0.34); pill tab aktif punya garis tepi perak.

## Revisi 2
- Pelangi gelembung: tiga pita bertumpuk (merah luar, hijau tengah, biru dalam) di tepi atas dan bawah, paling kuat di tengah, mengikuti bentuk gelembung sehingga di ujung navbar ikut melengkung.
- Garis tepi gelap tipis di dalam rim supaya gelembung terlihat seperti lensa bening.
- Highlight tab aktif sekarang memakai gelembung yang sama (lebih tenang) dan dipotong mengikuti outline navbar.

## Revisi 3
- Semua tombol/chip kaca (`lalasiGlassSurface`, `iridescentRim()`: Library, Search, Home, Settings, detail playlist) memakai warna kaca yang sama: dasar hitam netral 0xFF0B0D12, rim pelangi dikecilkan, ditambah garis merah/hijau/biru tipis di tepi atas dan bawah yang melengkung mengikuti ujung tombol.
- Fungsi baru `drawIridescentEdge` di IridescentBubble.kt.

## Revisi 4 — performa
- Animasi pelangi tidak lagi berjalan terus (30 fps) saat diam. Fasenya hanya berputar saat gelembung ditekan/bergerak; saat diam navbar, miniplayer, dan semua tombol kaca tidak menggambar ulang apa-apa. (AppNavigation.kt: `glassMoving`; BottomSheet.kt: fase miniplayer tidak menjalankan loop.)
- Per frame saat gelembung aktif: gambar path turun dari ~11 ke ~8, dan tidak ada alokasi array per frame di `rainbowEdge`. Garis cahaya tepi navbar 4 lapis jadi 3.
- Build tetap release: `isMinifyEnabled`, `isShrinkResources`, `isDebuggable=false`, baseline profile sudah aktif di build.gradle.kts, tidak diubah. Build dengan `./gradlew assembleRelease` (varian universal/foss sesuai konfigurasimu).

## Revisi 5
- Gelembung hanya muncul saat navbar ditekan/ditahan/digeser. Saat diam, tab aktif hanya pill kaca buram berujung oval dengan garis tepi gelap (sesuai gambar referensi), tanpa gelembung dan tanpa pelangi.
- Bentuk gelembung: oval berujung elips (rx = 24% lebar, ry = setengah tinggi), isi kaca buram keputihan, tepi perak/putih tipis. Streak dan titik pantul dihapus.
- Pelangi sesuai referensi: inti gelap, cincin hijau di luar, di dalamnya merah di kiri dan biru di kanan, hanya di tepi atas dan bawah. Saat gelembung di ujung navbar, warna tidak memudar di sisi ujung itu dan ikut melengkung turun mengikuti lengkung navbar.
- Force close saat bagikan lagu lalu pilih video sendiri: semua langkah di thread video kanvas (CanvasVideoSession) sekarang dibungkus try/catch, dan callback pemilih video dibungkus runCatching. Penyebab pastinya belum bisa dipastikan tanpa log crash.

## Revisi 6 (1_1_17)
- Gelembung di ujung navbar dibatasi lebar dan lengkung ujung navbar sendiri (hanya menonjol ke atas/bawah), tidak lagi keluar dari ujung navbar. Warna menipis dua langkah saat turun mengikuti lengkung.
- Pelangi: urutan benar (inti gelap, cincin merah/biru, cincin hijau di tepi), tiap cincin digambar dua kali supaya sisi dalamnya memudar halus.
- CrashHandler: setiap crash juga disimpan ke Download/LALASI-crash-<waktu>.txt (tanpa adb). R8 menyimpan nomor baris supaya jejak crash terbaca.
- Kartu streak memakai kaca gelap yang sama dengan tombol lain.
