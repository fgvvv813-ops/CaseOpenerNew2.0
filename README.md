# CaseOpener New 2.0

Native Kotlin + Jetpack Compose Android case opener. Only virtual currency and fictional prizes.

**Current version: 0.0.002**. Versioning starts from **0.0.001**. The first debug APK was technically called 0.1.0 before adopting this scheme.

## Install APK

[Download latest release](https://github.com/fgvvv813-ops/CaseOpenerNew2.0/releases/latest) — open **Assets** and download the versioned `.apk`.

[View build status](https://github.com/fgvvv813-ops/CaseOpenerNew2.0/actions/workflows/android.yml)

## Features

- 3 cases and rarity-based rewards
- Inventory, selling, virtual coins, saved offline progress
- Responsive layouts for phones, landscape and tablets
- "Что добавлено в обновлении?" dialog shows only notes for the installed version

## Version management

Update `appVersionName` and `appVersionCode` in `gradle.properties` for each new release. Also update **RELEASE_NOTES.md**, **CHANGELOG.md** and `releaseNotesByVersion` in `MainActivity.kt`. The in-app dialog looks up exact `BuildConfig.VERSION_NAME` and never shows an older version's notes.

Every push to `main` builds a debug APK with GitHub Actions and creates/updates the corresponding versioned GitHub Release.

Local build: JDK 17, Android SDK 35, Gradle 8.9; `gradle :app:assembleDebug`.
