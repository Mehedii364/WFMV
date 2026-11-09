# WFMV — Wafa File Manager
### Premium Android Smart Storage & Private Space Ecosystem
> **Smart Storage. Intelligent Files. Complete Privacy.**  
> **Developed by Md. Mehedi Hasan**

---

## 🌟 Overview
**WFMV (Wafa File Manager)** is a native Android application built using **Kotlin**, **Jetpack Compose**, and **Material 3**, designed following the **Premium Obsidian Dark** design system.

It features:
- **Advanced File Management**: Full folder navigation, breadcrumb navigation, cut/copy/paste clipboard, multi-selection, batch operations, sorting (name, date, size, type), filter by category, real-time search, file properties, and Android SAF integration.
- **Smart Storage Intelligence**: Total, used, and free storage calculation via `StatFs`, interactive category storage meters (Images, Videos, Audio, Documents, Archives, APKs), and largest file hunter.
- **Safe Junk Analyzer**: Safe analysis of empty directories, zero-byte files, temporary caches, large logs, and downloaded APK installers with full review and user confirmation prior to deletion.
- **Cryptographic Duplicate Finder**: Fast candidate grouping by exact byte length followed by streaming **SHA-256 content verification**, smart selection (keep oldest original / keep newest), side-by-side comparison, and batch cleanup.
- **Wafa Private Space ("Your Personal Digital Sanctuary")**:
  - Independent encrypted workspace with PIN authentication and recovery key fallback (`WFMV-XXXX-XXXX-XXXX`).
  - **AES-256-GCM authenticated encryption** with PBKDF2 key derivation (65,536 iterations).
  - Multi-vault organization (Personal Sanctuary, Confidential Vault, Work Secure).
  - Private Photos/Videos gallery, Private Documents library, and Encrypted Notes manager.
- **Developer Tools**:
  - SHA-256, SHA-1, and MD5 checksum computation.
  - File magic byte / signature detector and extension mismatch checker.
  - JSON and XML syntax validator.
  - Hex stream viewer (offset, hex, ascii).
  - APK metadata inspector (package name, version name/code, min/target SDK, requested permissions).
- **App Storage Insights**: Installed application scanner with package names, versions, APK sizes, and direct intents to launch app or open system App Info.

---

## 🛠️ Architecture & Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin 2.2+ |
| **UI Framework** | Jetpack Compose + Material 3 |
| **Architecture** | MVVM + Clean Architecture |
| **Persistence** | Android Room Database (Offline-First) |
| **Cryptography** | AES-256-GCM + PBKDF2WithHmacSHA256 + CSPRNG SecureRandom |
| **File I/O** | Android Storage Access Framework + NIO Streams + ZipOutputStream |
| **CI / CD** | Automated GitHub Actions with Real Gradle Build & Validation |

---

## 🚀 Building Locally

### Requirements
- Android Studio Ladybug / Meerkat or later
- JDK 17
- Android SDK 35/36
- Minimum SDK: Android 10 (API 29)

### Build Command
To build the debug APK locally:
```bash
./gradlew assembleDebug
```
The real APK will be output to:
```
app/build/outputs/apk/debug/app-debug.apk
```

To run unit tests:
```bash
./gradlew testDebugUnitTest
```

---

## 🤖 Automated CI Build (GitHub Actions)
Every commit to `main` triggers `.github/workflows/android-build.yml`:
1. Compiles the Android project using `./gradlew assembleDebug`.
2. Validates that the APK exists and exceeds minimum size.
3. Performs ZIP integrity test (`unzip -t`).
4. Generates matching SHA-256 hashes.
5. Saves the verified APK to:
   - `APK_DOWNLOAD/app-debug.apk`
   - `.build-outputs/app-debug.apk`
6. Uploads the build artifact to GitHub Actions releases.

---

## 🛡️ Security & Privacy
- **Zero Cloud Leakage**: Core file operations and private vault records are kept strictly local to device storage and authenticated SQLite Room database.
- **No Plaintext Keys**: Master encryption keys are never stored in plaintext; keys are derived on-the-fly from user PIN via PBKDF2 and securely wiped upon vault lock.
- **Zip-Slip Protection**: The archive extraction engine strictly verifies canonical path hierarchies before writing any uncompressed files to prevent path traversal attacks.

---

### Author & Credits
**Md. Mehedi Hasan**  
*Creator and Principal Architect of WFMV (Wafa File Manager)*
