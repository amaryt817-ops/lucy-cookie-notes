# Lucy Cookie Notes — Android project

Project Android native (Kotlin) untuk mencatat akun dan cookie Roblox secara lokal.

## Fitur
- Label/kategori bebas: A, B, C, Farming, Main, dan lain-lain.
- Tambah, edit, hapus akun; pindahkan akun ke label lain.
- Cari username/catatan, filter berdasarkan label.
- Cookie disamarkan secara default, dapat ditampilkan sementara.
- Tombol salin cookie.
- Impor banyak akun sekaligus (format `username,cookie,label` per baris).
- Ekspor/format akun pada label aktif: username saja, cookie saja, `username:cookie`, atau `username,cookie,label` untuk dicadangkan/diimpor ulang.
- Penyimpanan terenkripsi AES-256-GCM menggunakan Android Keystore.
- Tidak meminta izin internet dan tidak mengirim data ke server.

## Membuat APK
1. Ekstrak ZIP ini.
2. Buka folder `RobloxCookieNotes` melalui Android Studio (versi yang mendukung Android Gradle Plugin 8.6.1).
3. Tunggu Gradle Sync selesai dan izinkan Android Studio mengunduh komponen build yang diperlukan.
4. Pilih **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. APK debug akan muncul di `app/build/outputs/apk/debug/app-debug.apk`.

## Catatan keamanan penting
Cookie Roblox adalah kredensial login penuh. Jangan kirim cookie melalui chat, screenshot, atau ekspor tanpa perlindungan. Aplikasi ini menyimpan vault terenkripsi pada perangkat, tetapi siapa pun yang dapat membuka aplikasi pada perangkat yang sudah terbuka tetap dapat melihat atau menyalin cookie. Hindari memakai cookie pada perangkat bersama. Clipboard dapat dibaca aplikasi lain yang memiliki akses clipboard; kosongkan clipboard setelah digunakan.

Project ini belum berupa APK siap instal karena lingkungan pembuatan saat ini tidak menyediakan Android SDK/Gradle. Source project disediakan agar dapat dibangun di Android Studio.
