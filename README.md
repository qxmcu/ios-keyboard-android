# 🍎 iOS Keyboard for Android (Open-Source Clone)

[![Build & Package APK](https://github.com/org-iosclone/ios-keyboard-android/actions/workflows/build-apk.yml/badge.svg)](https://github.com/org-iosclone/ios-keyboard-android/actions/workflows/build-apk.yml)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Android: 8.0 - 15+](https://img.shields.io/badge/Platform-Android%208.0%20(API%2026)%20--%2015%2B-brightgreen.svg)](https://developer.android.com)
[![Kotlin: 100%](https://img.shields.io/badge/Kotlin-100%25-purple.svg)](https://kotlinlang.org)
[![Privacy: 100% Offline](https://img.shields.io/badge/Privacy-100%25%20Offline%20%26%20Zero--Telemetry-success.svg)](#-privacy-guarantee)

A pixel-perfect, production-grade, rootless Android Input Method Editor (IME) that serves as an exact clone of the modern iOS keyboard. Engineered from the ground up in 100% idiomatic Kotlin with an ultra-low latency direct Canvas rendering engine, authentic Apple Taptic Engine micro-haptics, bundled iOS keystroke audio, decoupled Apple emoji glyph rendering, gesture glide typing, spacebar cursor trackpad, and an offline autocorrection Trie.

---

## 📸 Visual Showcase & UI Architecture

### 1. Pixel-Perfect Keyboard Layout (Light & Dark)
```
+-------------------------------------------------------------------------+
|  "how"         |         "hello" (Autocorrect)        |     "help"      |  <- iOS Suggestion Strip
+-------------------------------------------------------------------------+
|   [ Q ]   [ W ]   [ E ]   [ R ]   [ T ]   [ Y ]   [ U ]   [ I ]   [ O ] |
|     [ A ]   [ S ]   [ D ]   [ F ]   [ G ]   [ H ]   [ J ]   [ K ]   [ L ] 
| [ ⇧ ]   [ Z ]   [ X ]   [ C ]   [ V ]   [ B ]   [ N ]   [ M ]   [ ⌫ ]  |
| [ 123 ]    [ 🌐 ]    [ 🎤 ]    [           space           ]   [ return ]|
+-------------------------------------------------------------------------+
|                            — Home Bar Inset —                           |
+-------------------------------------------------------------------------+
```

### 2. Magnifier Popup & Long-Press Diacritics
```
          .--------.
         /    E     \      <- iOS Magnifier Balloon Bubble (appears above finger)
         \          /
          '-.    .-'
             |  |
             |  |
            [ E ]

         .-------------------------------------.
         |  è  |  é  |  ê  |  ë  |  ē  |  ė  |  ę  |   <- Sliding Accent Popover
         '-------------------------------------'
```

### 3. Spacebar Cursor Trackpad Mode
Hold the `space` bar for >350ms to turn the entire keyboard canvas into a fluid cursor trackpad. Dragging smoothly steers the cursor through host text fields via `InputConnection` selection updates with micro-haptic ticks!

---

## ⚡ Core Technical Features

| Feature | Specification |
|---|---|
| **Direct Canvas Rendering** | Single-view custom canvas with zero XML View overhead. Sub-50ms cold startup, 60/120Hz typing response. |
| **Authentic iOS Geometry** | 5dp corner radii, 1.2dp physical elevation drop shadows, 6dp horizontal key pitch, 10dp vertical row gap. |
| **Themes** | **iOS Light** (`#D1D5DB` chassis, `#FFFFFF` keys) & **iOS Dark** (`#1C1C1E` chassis, `#2C2C2E` keys) with automatic system night-mode sync. |
| **Keystroke Audio** | Low-latency Android `SoundPool` loaded with uncompressed 16-bit PCM clicks (standard, delete, and return/space). |
| **Apple Taptic Engine** | `VibrationEffect.createOneShot` micro-impulse waveforms tailored per keystroke type with intensity slider. |
| **Decoupled Apple Emoji Engine** | Bundled font loader ensuring authentic iOS glyph representation across Samsung, Xiaomi, and Google devices. |
| **Full Emoji Picker** | iOS category strip (Recents, Smileys, People, Animals, Food, Travel, Activities, Objects, Symbols, Flags), real-time search, and skin tone selector popup. |
| **Autocorrect & Predictions** | Offline Trie database with SymSpell Levenshtein edit-distance fuzzy autocorrection and persistent learning. |
| **Continuous Glide Typing** | Translucent blue bezier curve trail with topological path character scoring. |
| **Clipboard History Manager** | SQLite clipboard history drawer with pinning, deletion, and instant one-tap suggestion chips in the top strip. |
| **Inline Translation** | Real-time translation toolbar supporting offline phrase pairs and pluggable LibreTranslate endpoints. |
| **One-Handed Ergonomics** | Left-docked and right-docked one-handed modes with instant toggle buttons. |
| **iOS Settings App** | Modern Jetpack Compose UI clone of iOS Settings for extensive customization. |

---

## 🚀 Installation & Activation Guide

### Method A: Install Standalone APK
1. Download `ios-keyboard-release.apk` from the [GitHub Releases](https://github.com/org-iosclone/ios-keyboard-android/releases) section.
2. Open the downloaded APK on your Android device and install it (allow installation from unknown sources if prompted).
3. Open the **iOS Keyboard** app from your home screen / launcher.
4. Follow the setup prompts:
   - Tap **Enable Keyboard** $\rightarrow$ Toggle on **iOS Keyboard** under *Manage on-screen keyboards*.
   - Tap **Switch Active Keyboard** $\rightarrow$ Select **iOS Keyboard** as your default input method.

### Method B: System Settings Navigation
- **Android 10 - 15+:** `Settings` $\rightarrow$ `System` $\rightarrow$ `Languages & input` $\rightarrow$ `On-screen keyboard` $\rightarrow$ `Manage on-screen keyboards` $\rightarrow$ Turn on **iOS Keyboard**.
- **Samsung One UI:** `Settings` $\rightarrow$ `General management` $\rightarrow$ `Keyboard list and default` $\rightarrow$ Turn on **iOS Keyboard**.
- **Xiaomi MIUI / HyperOS:** `Settings` $\rightarrow$ `Additional settings` $\rightarrow$ `Languages & input` $\rightarrow$ `Current keyboard` $\rightarrow$ Select **iOS Keyboard**.

---

## 🛠️ Building From Source

### Prerequisites
- JDK 17 (Eclipse Temurin, OpenJDK, or Android Studio bundled JDK)
- Android SDK with Platforms `android-35` and Build-Tools `35.0.0`
- Git

### Build Steps

1. **Clone the repository:**
   ```bash
   git clone https://github.com/org-iosclone/ios-keyboard-android.git
   cd ios-keyboard-android
   ```

2. **Build Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```
   Output: `app/build/outputs/apk/debug/app-debug.apk`

3. **Build Release APK:**
   ```bash
   ./gradlew assembleRelease
   ```
   Output: `app/build/outputs/apk/release/app-release.apk`

4. **Install directly via ADB:**
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 📦 Repository Structure

```
├── .github/
│   └── workflows/
│       └── build-apk.yml           # Automated CI/CD: compiles APKs & creates GitHub Releases
├── app/
│   ├── build.gradle.kts            # App-level build config (compileSdk 35, minSdk 26, Compose)
│   ├── proguard-rules.pro          # Code obfuscation and reflection preservation rules
│   └── src/main/
│       ├── AndroidManifest.xml     # IME service permissions & declarations
│       ├── assets/
│       │   ├── dictionaries/       # Curated word frequency lists (EN, ES, FR, DE)
│       │   ├── fonts/              # AppleColorEmoji.ttf font asset
│       │   └── sounds/             # Uncompressed key_click, key_delete, key_return wavs
│       ├── java/org/iosclone/keyboard/
│       │   ├── audio/              # SoundPool audio engine & Taptic Engine vibration pipeline
│       │   ├── clipboard/          # SQLite clipboard persistence & system clipboard listener
│       │   ├── dictionary/         # Character Trie, Levenshtein autocorrect, and learning engine
│       │   ├── emoji/              # Categorized emoji catalog, fuzzy search, and skin tones
│       │   ├── gesture/            # Continuous touch path tracker, bezier renderer, and word matcher
│       │   ├── layout/             # iOS key definition models, layout factory, and one-handed geometry
│       │   ├── service/            # Core Android InputMethodService orchestration
│       │   ├── settings/           # Jetpack Compose iOS Settings clone UI & SharedPreferences
│       │   ├── theme/              # Pixel-perfect iOS Light and Dark color palettes
│       │   ├── translate/          # Inline real-time translation tool (offline + LibreTranslate)
│       │   └── view/               # Ultra-low latency Canvas keyboard, magnifier popup, emoji picker
│       └── res/
│           ├── drawable/           # Crisp vector icons (Shift, Caps, Delete, Globe, Mic, Categories)
│           ├── values/             # Colors, themes, styles, and subtype definitions
│           └── xml/method.xml      # Android IME subtype declarations
├── docs/
│   ├── ARCHITECTURE.md             # Deep dive into Canvas rendering & haptics pipeline
│   ├── THEMES.md                   # Complete color matrix and dimension specifications
│   └── GESTURE_TYPING.md           # Continuous glide gesture mathematical model
├── build.gradle.kts                # Project-level Gradle configuration
├── settings.gradle.kts             # Gradle multi-module project declarations
├── gradle.properties               # Memory allocation and AndroidX flags
├── gradlew / gradlew.bat           # Gradle wrapper scripts
├── LICENSE                         # Apache License 2.0
└── README.md                       # Documentation & showcase
```

---

## 🔒 Privacy Guarantee

> **Privacy is non-negotiable.**

Keyboards hold access to your most intimate and sensitive communications—from passwords and private thoughts to financial details.

- **100% Offline by Default:** The core keyboard, predictive engine, dictionary, gesture typing, and emoji engine operate completely offline with zero network connectivity.
- **Zero Keystroke Logging:** Your typing data is never logged, stored remotely, or transmitted.
- **No Telemetry or Analytics:** No third-party trackers, no Firebase, no crash telemetry, and no advertising SDKs are included.
- **Secure Sandboxed SQLite:** User-learned vocabulary and clipboard history remain strictly in sandboxed private app storage on your physical device.

---

## 📄 License

Licensed under the **Apache License, Version 2.0**. See the [LICENSE](LICENSE) file for complete details.
