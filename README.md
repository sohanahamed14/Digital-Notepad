# Digital Notepad (v2.0.0)

[![Release](https://img.shields.io/badge/Release-v2.0.0-indigo.svg)](https://digital-notepad.pages.dev)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://digital-notepad.pages.dev)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Hosted](https://img.shields.io/badge/Cloudflare_Pages-digital--notepad.pages.dev-orange.svg)](https://digital-notepad.pages.dev)

A modern, high-performance, privacy-first digital notebook for Android built with **Jetpack Compose**, **Kotlin Coroutines & Flow**, **Room SQLite**, and **Hardware-backed Biometrics**.

🌐 **Website & Download**: [digital-notepad.pages.dev](https://digital-notepad.pages.dev)  
👨‍💻 **Developer**: [Sohan Ahammed](https://sohanahammed14.vercel.app/)

---

## 🚀 What's New in Version 2.0.0

### 1. 📱 Android Home Screen Glance Widgets
- **Pinned Sticky Note Widget**: Pin any note directly onto your Android home screen for glanceable reading.
- **Quick-Add Capture Widget**: Instant one-tap button to open quick capture without searching through apps.

### 2. ☁️ End-to-End Encrypted Cloud Sync (Google Drive / WebDAV)
- **Zero-Knowledge Architecture**: Export and sync `.encbak` files encrypted on-device using **PBKDF2** (10,000 iterations HMAC-SHA256) and **AES-256-GCM**.
- **Passphrase Protection**: Cloud storage providers (Google Drive, WebDAV, Nextcloud, Dropbox) cannot read note content or metadata without your secret master passphrase.

### 3. 🎨 Rich Media Attachments
- **Inline Photo Attachments**: Attach photos directly from your camera roll or camera via standard Markdown syntax.
- **Voice Memo Recorder & Embedded Player**: Record audio notes directly inside notes with live duration visualizer and inline playback controls.
- **Finger-Drawing Sketch Canvas**: Draw hand sketches and diagrams with custom brush sizes, color swatches, and direct PNG embedding.

### 4. 🔒 App Lock / Master PIN
- App-wide security layer requiring **BiometricPrompt** (Fingerprint / Face Unlock) or Device Credential on launch and resume, supplementing the per-note encrypted Vault.

### 5. 📦 Bulk Import / Export
- **Markdown .zip Archive**: Batch export all notes as individual `.md` files organized in a `.zip` archive.
- **Google Keep Import**: Effortlessly import Google Keep exported JSON files.
- **Standard Text / Markdown Import**: Import any `.txt` or `.md` file with automatic title and content parsing.
- **JSON Full Backup & Restore**: Complete database snapshot backup and restore.

### 6. 🏷️ Tag Tree & Multi-Tagging
- Automatic `#hashtag` extraction from note title and content (`#work`, `#todo`, `#ideas`).
- Dynamic interactive tag filter chip bar for instant search and categorization.

---

## 🛠️ Architecture & Tech Stack

- **UI**: 100% Declarative Jetpack Compose + Material 3 Design Tokens
- **Language**: Kotlin 2.0+
- **Database**: Android Room SQLite with reactive Kotlin StateFlow
- **Dependency Injection**: Dagger Hilt
- **Security & Encryption**: AndroidKeyStore, PBKDF2WithHmacSHA256, AES-256-GCM
- **Hardware Integration**: BiometricPrompt, MediaRecorder, MediaPlayer, AppWidgets, Quick Settings Tile

---

## 📥 Installation

1. Download the latest APK from [digital-notepad.pages.dev](https://digital-notepad.pages.dev).
2. Or build locally:
   ```bash
   git clone https://github.com/sohanahamed14/Digital-Notepad.git
   cd Digital-Notepad
   ./gradlew assembleDebug
   ```
3. The APK will be located at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 🧪 Testing

Run all unit tests:
```bash
./gradlew testDebugUnitTest
```

---

## 👨‍💻 Author

**Sohan Ahammed**  
Portfolio: [sohanahammed14.vercel.app](https://sohanahammed14.vercel.app/)  
GitHub: [@sohanahamed14](https://github.com/sohanahamed14)
