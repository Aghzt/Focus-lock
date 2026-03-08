# 🔒 FocusLock - Android App

Aplikasi Android untuk memblokir penggunaan HP selama sesi fokus.
Semua notifikasi dibisukan, layar terkunci penuh, dan pengguna tidak bisa keluar sebelum timer habis.

---

## 📂 Struktur Project

```
FocusLock/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/focuslock/
│       │   ├── MainActivity.kt       ← Layar set timer
│       │   ├── LockActivity.kt       ← Layar kunci + countdown
│       │   └── FocusService.kt       ← Background service
│       └── res/
│           ├── layout/
│           │   ├── activity_main.xml
│           │   └── activity_lock.xml
│           ├── drawable/bg_picker.xml
│           └── values/
│               ├── strings.xml
│               └── themes.xml
├── build.gradle
└── settings.gradle
```

---

## 🚀 Cara Setup di Android Studio

1. **Buka Android Studio** → `File > Open` → pilih folder `FocusLock`
2. Tunggu Gradle sync selesai
3. Sambungkan HP Android (USB Debugging aktif) atau gunakan Emulator
4. Klik **Run ▶** atau tekan `Shift + F10`

### Persyaratan
- Android Studio Hedgehog (2023.1.1) atau lebih baru
- Android SDK 26+ (Android 8.0 Oreo)
- Kotlin plugin terinstal

---

## ✨ Fitur

| Fitur | Keterangan |
|---|---|
| ⏱️ Timer Fleksibel | Pilih jam (0-5) dan menit (0-55, step 5) |
| 🔕 Do Not Disturb | Semua notifikasi dibisukan otomatis |
| 🔒 Screen Pinning | Layar terkunci, tidak bisa keluar ke app lain |
| 💬 Pesan Motivasi | Berubah sesuai progress sesi |
| 📊 Progress Bar | Visual kemajuan sesi |
| ⚠️ Tombol Darurat | Tahan 5 detik untuk keluar di kondisi darurat |
| 🔔 Notifikasi Ongoing | Timer tetap berjalan meski layar mati |

---

## ⚠️ Izin yang Dibutuhkan

1. **Do Not Disturb Access** — Untuk membisukan notifikasi
   - Saat pertama kali buka app, akan diarahkan ke Settings untuk mengizinkan
   - Settings → Apps → Special App Access → Do Not Disturb Access → FocusLock ✅

2. **Foreground Service** — Menjaga timer berjalan di background (auto-granted)

---

## 🔧 Cara Kerja Kunci Layar

App menggunakan **Android Screen Pinning** (`startLockTask()`) yang:
- Menyembunyikan tombol Back, Home, dan Recent Apps
- Mencegah user berpindah ke app lain
- Bisa dinonaktifkan hanya dari dalam app (tombol darurat) atau saat timer habis

> **Catatan:** Screen Pinning adalah fitur bawaan Android dan tidak memerlukan akses root.

---

## 🛠️ Kustomisasi

### Ubah warna tema
Edit `res/values/themes.xml`:
```xml
<item name="colorPrimary">#58A6FF</item>  ← Warna aksen utama
```

### Ubah background
Edit atribut `android:background` di layout XML:
```xml
android:background="#0D1117"  ← Warna background (default: dark)
```

### Ubah durasi maksimal
Edit di `MainActivity.kt`:
```kotlin
binding.pickerHours.maxValue = 5  ← Ubah angka ini
```

---

## 📝 Catatan Penting

- App ini menggunakan **Screen Pinning** bawaan Android, bukan device owner/MDM
- Pengguna masih bisa mematikan HP secara paksa (tekan power lama) — ini adalah batasan hardware
- Untuk penguncian lebih ketat (enterprise), diperlukan Device Owner mode via MDM
