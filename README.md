<div align="center">

<img src="assets/banner/lalasi-banner.png" alt="LALASI — Your Music. Your Atmosphere. Your LALASI." width="100%">

# LALASI

**Your Music. Your Atmosphere. Your LALASI.**

Pemutar musik Android dengan antarmuka Liquid Glass, navigasi elastis, dan animasi yang dibuat untuk terasa mulus.

[![Version](https://img.shields.io/badge/version-1.0.9-7C8CFF?style=for-the-badge)](#-download)
[![Platform](https://img.shields.io/badge/Android-7.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](#)
[![Kotlin](https://img.shields.io/badge/Kotlin-Compose-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](#)
[![Media3](https://img.shields.io/badge/Playback-Media3-151922?style=for-the-badge)](#)

[**Download APK**](#-download) · [**Fitur**](#-fitur) · [**Yang baru di 1.0.9**](#-yang-baru-di-109) · [**Build**](#-build-dari-source)

</div>

---

## ✨ Yang baru di 1.0.9

| | |
|---|---|
| 🎬 **Loading screen baru** | Animasi langsung jalan dari frame pertama, tulisan LALASI ditulis pelan lalu langsung masuk ke Home. Tanpa kedip, tanpa stuck. |
| 🪟 **Widget seragam** | 10 widget LALASI memakai tampilan Artwork Player: sampul tajam, highlight dan liquid glass yang sama. Widget lama dihapus. |
| 🫧 **Gelembung navbar** | Gelembung gesture punya highlight solid, dan meluncur halus ke tab tujuan saat dilepas, tanpa flicker. |
| 🔁 **Pindah halaman** | Transisi antar tab diperhalus supaya tidak ada kedip di antara dua halaman. |
| 🔐 **Update tanpa uninstall** | Semua build kini ditandatangani dengan satu kunci tetap, jadi APK baru bisa dipasang langsung di atas versi sebelumnya. |

## 📥 Download

Ambil APK terbaru dari **[Releases](../../releases)** atau dari artifact build di tab **Actions**.

- Nama file: `LALASI-v1.0.9.apk`
- Application ID: `com.danrey.lalasi` · versi `1.0.9` (kode `109`)
- Minimal Android 7.0 (API 24)

### Memperbarui aplikasi

Pasang APK baru langsung di atas yang lama, **tidak perlu uninstall**, selama:

1. Application ID sama (`com.danrey.lalasi`),
2. APK ditandatangani dengan kunci yang sama (`app/persistent-debug.keystore` di repo ini),
3. `versionCode` lebih tinggi dari yang terpasang.

> **Catatan sekali saja:** kalau APK yang sedang terpasang dulu dibuild dengan kunci lain (misalnya kunci acak dari build CI lama), Android akan menolak update dan kamu perlu uninstall **satu kali**. Setelah itu semua versi berikutnya bisa langsung di-update. Cadangkan data dulu bila perlu.

## 🎧 Fitur

- **Pemutaran musik** dengan Media3, pemutaran latar belakang, dan integrasi media session.
- **Pencarian dan jelajah** lewat provider yang terpasang.
- **Library dan playlist**, mini player, dan player layar penuh.
- **Lirik** berjalan (sinkron bila tersedia) dari beberapa provider.
- **Liquid Glass UI**: permukaan kaca translusen, tepi spekular, dan tampilan berbasis sampul album.
- **Navigasi elastis**: capsule bar dengan gelembung gesture, geser antar halaman, dan menciut menjadi lingkaran Home saat scroll.
- **Widget layar utama** (10 varian): Compact, Square, Wide, Expanded, Large Square, Live Lyrics, Compact Lyrics, Wide Lyrics, Minimal, dan Artwork Player.
- **Kustomisasi** tema dan tampilan.

Ketersediaan layanan bergantung pada konfigurasi provider, jaringan, wilayah, dan perubahan layanan pihak ketiga.

## 🖼️ Tampilan

Screenshot belum ditambahkan. Taruh tangkapan layar dari build asli di [`assets/screenshots/`](assets/screenshots/README.md):
`home.png`, `player.png`, `navigation.png`, `lyrics.png`, `widgets.png`, `library.png`, `settings.png`.

## 🛠️ Teknologi

Kotlin · Jetpack Compose · AndroidX Media3 · Room · Hilt · DataStore · Ktor · Coil
`compileSdk 36` · `minSdk 24` · `targetSdk 36` · Gradle Wrapper `9.3.1`

## 🔨 Build dari source

```bash
git clone <URL-REPOSITORY-LALASI>
cd LALASI-main
chmod +x gradlew
./gradlew :app:assembleUniversalGmsDebug
```

APK ada di `app/build/outputs/apk/universalGms/debug/`. Build memakai JDK 21 dan Android SDK 36.
GitHub Actions (`.github/workflows/android-build.yml`) membuat APK yang sama dan mengunggahnya sebagai artifact `LALASI-v1.0.9.apk`.

### Tentang kunci tanda tangan

`app/persistent-debug.keystore` sengaja ikut di repo supaya setiap build (lokal maupun CI) memakai tanda tangan yang sama dan bisa saling meng-update. Kunci ini **hanya untuk build debug/uji**. Untuk rilis publik, siapkan keystore rilis sendiri dan simpan di luar repo (lihat `signingConfigs.release` di `app/build.gradle.kts`, dibaca dari `KEY_ALIAS`, `KEY_PASSWORD`, `STORE_PASSWORD`). Jangan pernah meng-commit kunci rilis atau API key pribadi.

## 🗂️ Struktur proyek

```text
LALASI-main/
├── app/          # aplikasi Android: UI, playback, database, resource
├── providers/    # provider musik, lirik, metadata, dan media
├── modules/      # modul API dan layanan bersama
├── assets/       # logo, banner, social preview, screenshot
├── docs/         # catatan publikasi
└── fastlane/     # metadata dan changelog
```

## 👤 Developer

**danrey** — dirancang dan dikembangkan dengan fokus pada pengalaman musik modern, antarmuka elegan, dan interaksi yang mulus.

- Instagram: [@devreyw_](https://www.instagram.com/devreyw_/)
- TikTok: [@danhanzyr](https://www.tiktok.com/@danhanzyr)

## 🙏 Kredit dan lisensi

LALASI memakai modul provider pihak ketiga dan dependensi open source; penulis, lisensi, dan atribusinya tetap berlaku. Lihat [`LICENSE`](LICENSE) (tidak diubah) serta direktori provider/modul terkait. Periksa semua ketentuan sebelum mendistribusikan ulang repo atau biner hasil modifikasi.

## ⚠️ Disclaimer

LALASI dapat berkomunikasi dengan layanan musik, lirik, metadata, dan media pihak ketiga lewat provider yang dikonfigurasi. Layanan itu dioperasikan secara independen dan dapat berubah. LALASI tidak mengklaim kepemilikan atas katalog atau media pihak ketiga. Pengguna bertanggung jawab mematuhi ketentuan layanan dan hukum setempat.

---

<div align="center">

**LALASI** · A listening experience shaped around your atmosphere.

</div>
