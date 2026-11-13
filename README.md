# CaseOpener New 2.0

Native Android case-opening game built with Kotlin and Jetpack Compose.

## Build

GitHub Actions automatically builds a debug APK on every push to `main` and uploads it as the `CaseOpener-New-2.0-debug-apk` artifact. Open **Actions → Build Android APK → latest successful run → Artifacts** to download the ZIP containing the APK.

For local builds: `gradle :app:assembleDebug` (Gradle 8.9, JDK 17, Android SDK 35).

## Features (v0.1)

- 3 fictional cases and weighted rarity drops
- Animated reveal, inventory and selling
- Offline currency and saved progress

All currency and rewards are virtual, with no cash-out or real-money wagering.
