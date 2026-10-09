# Kosakata Offline (Android)
WebView yang membungkus kosakata_flashcard.html (app/src/main/assets/index.html), sepenuhnya offline.

## Cara A — GitHub (tanpa install apa pun)
1. Buat repo baru di github.com, unggah SEMUA isi folder ini (termasuk folder .github).
2. Tab Actions → "Build APK" → tunggu hijau (± 3–5 menit).
3. Buka run tersebut → Artifacts → KosakataOffline-apk → unduh, ekstrak, pasang app-debug.apk di HP
   (izinkan "pasang dari sumber tidak dikenal").

## Cara B — Android Studio
File → Open folder ini → tunggu sync → Build → Build APK(s).
APK ada di app/build/outputs/apk/debug/app-debug.apk

## Catatan
- Progres tersimpan di penyimpanan app. Data dari versi browser TIDAK otomatis pindah:
  di versi browser pakai Setelan → Cadangkan progres, lalu di app pakai Setelan → pilih file JSON.
- Untuk memperbarui isi app: ganti app/src/main/assets/index.html lalu build ulang.
