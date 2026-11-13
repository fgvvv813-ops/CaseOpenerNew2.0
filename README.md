# CaseOpener New 2.0

Native Android game written in Kotlin + Jetpack Compose.

**Current version: 0.0.005.** In-app game is **online only** and requires registration/login. Existing online accounts use the same encrypted session token format as the preceding version.

## Install

[Latest APK](https://github.com/fgvvv813-ops/CaseOpenerNew2.0/releases/latest)

## Gameplay

- Home, case catalog, case detail with available skins & chances, roulette reveal, inventory, profile/logout
- Authenticated online gameplay and server-controlled currency/drops/sales
- One initial snapshot over WebSocket after connection, then incremental inventory, balance and catalog events
- Reconnection after network interruptions (no recurring user-data polling)
- Telegram-managed cases, skins, images, prices and availability (requires separately deployed Python API)

## Backend

Download `server.py` and `requirements.txt` from the CaseOpener server distribution, or configure your deployment with the supplied files.

The Android app currently uses `http://48.102.160.0:40017` (temporary testing only). Configure HTTPS/WSS before public registration to protect account passwords and sessions. The server **must run with ASGI**; Gunicorn's default sync WSGI worker does not support WebSockets. Example: `uvicorn server:app --host 0.0.0.0 --port 40017 --workers 1`.

Use a single worker for this SQLite + in-process WebSocket broadcast design. Multi-worker operation requires Redis pub/sub.

## Versioning

Bump `appVersionName` and `appVersionCode` in `gradle.properties` for new releases. Add only changes players can see and use to the current `RELEASE_NOTES.md` and in-app notes. Do not mention administration, CI, GitHub Actions, build tooling, APK publication, or other backend internals in player-facing change logs.

## Android continuity

The package ID remains `com.caseopener.newtwo` and debug APKs since 0.0.003 use the same *development-only* signing certificate. Android app updates should retain the encrypted online session when installed over the existing app. The old offline SharedPreferences are deliberately not used or imported; the online server data is preserved. NEVER use the public development signing key for a production release.
