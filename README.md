# CaseOpener New 2.0

Native Kotlin + Jetpack Compose Android case opener. Only virtual currency and fictional prizes.

**Current version: 0.0.004**. Versioning starts from **0.0.001**. The first debug APK was technically called 0.1.0 before adopting this scheme.

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

## Player-facing release notes policy

Only describe things players can see or use in the game's "Что нового?" window, `RELEASE_NOTES.md`, and public changelog. Do **not** list internal GitHub Actions, build tools, CI, Gradle, admin panels, signing, or APK publication as gameplay changes. Use short sections "Добавлено" and "Улучшено" for the CURRENT version only.

## Updating & data integrity

Android application ID remains `com.caseopener.newtwo` and versionCode increases. All debug versions starting with v0.0.003 are signed with the same stable DEVELOPMENT-only test signing certificate, stored in `signing/caseopener-test.jks`. NEVER use this public development key for a production app; migrate to a securely stored release signing key before distributing a production release.

The original v0.0.001/v0.0.002 APKs were signed using ephemeral CI debug keys, so an in-place update from those builds cannot be guaranteed. Never tell users to uninstall the old app if they need its stored progress. For newer builds, install the APK over the existing matching-package, matching-signature app, do not clear app data. In-app backup and restore uses Android's document picker, and the saved file contains local balance, items and statistics.

## Online mode (experimental)

The app connects to `http://48.102.160.0:40017/api/v1`. Launch the separate `server.py` FastAPI backend with `requirements.txt` on your server. The SQLite database is created on first launch.

This is a **temporary cleartext HTTP testing setup**. Passwords and bearer tokens are NOT secure on public HTTP. Before involving real players, put the server behind HTTPS (TLS certificate / reverse proxy), update `OnlineMode.kt` to an HTTPS endpoint, and remove Android's cleartext opt-in.

The online economy is server-authoritative. No client-provided balance/inventory overwrites are accepted. Offline progress is stored locally and intentionally not automatically imported into the online account. Tokens are encrypted via Android Keystore.
