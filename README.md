# CaseOpener New 2.0

Native Android game written in Kotlin + Jetpack Compose.

**Current version: 0.0.012.** In-app game is **online only** and requires registration/login. Existing online accounts use the same encrypted session token format as the preceding version.

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

The Android app currently uses `http://93.123.84.239:40017` (temporary testing only). Configure HTTPS/WSS before public registration to protect account passwords and sessions. The server **must run with ASGI**; Gunicorn's default sync WSGI worker does not support WebSockets. Example: `uvicorn server:app --host 0.0.0.0 --port 40017 --workers 1`.

Use a single worker for this SQLite + in-process WebSocket broadcast design. Multi-worker operation requires Redis pub/sub.

## Versioning

Bump `appVersionName` and `appVersionCode` in `gradle.properties` for new releases. Add only changes players can see and use to the current `RELEASE_NOTES.md` and in-app notes. Do not mention administration, CI, GitHub Actions, build tooling, APK publication, or other backend internals in player-facing change logs.

## Android continuity

The package ID remains `com.caseopener.newtwo` and debug APKs since 0.0.003 use the same *development-only* signing certificate. Android app updates should retain the encrypted online session when installed over the existing app. The old offline SharedPreferences are deliberately not used or imported; the online server data is preserved. NEVER use the public development signing key for a production release.

## UI v0.0.006

- New premium dark/neon visuals across login, home, case catalog, case preview, roulette, inventory and player profile.
- Case and skin artwork is server-controlled. No starter-case images are embedded in APK.
- Search, live filters, 18-items-per-page inventory, sort order, rarity labels, player stats computed from real server records, recent owned skins, real logout and a local reduced-motion toggle.
- No invented XP, payment options, skin listings, or other unsupported backend features. Level is derived from completed case openings (20 openings per level).
- Stable applicationId, versionCode and testing signature retained.

## Shared chat (v0.0.008)

- Global chat for all registered players, available as a fifth tab.
- Sends and history over the existing authenticated WebSocket.
- Per-user 5-second write cooldown is enforced server-side using SQLite.
- Messages must be 3–350 Unicode code points, trimmed; duplicates prevented by request UUIDs.
- The server returns the latest 60 messages on demand, then streams changes and supports older-page loading.
- Tap an author to open a public profile with coin balance, join date, number of openings and best ever drop (tracked separately even when sold).
- The app shows unread chat counts outside the chat screen.
- **Server upgrade required:** deploy the separately distributed v0.0.008 bot.py alongside the existing caseopener.db and media directory. Older server versions do not know chat commands.
- **Never commit the private Telegram bot token to this public repository.**

## CS2 item catalogue (v0.0.009)

- The APK contains **no cases, skins or case artwork**. Everything comes from the authenticated FastAPI/WebSocket server. Neutral emoji placeholders show if the server has not provided an image.
- Only own-server media URLs `/api/v1/media/{safe_id}` and `/api/v1/cs2/image/{cs2_id}` are rendered, not arbitrary image URLs from data.
- CS2 catalogue: [skins JSON](data/cs2/skins.json), and additional item categories in `data/cs2/`.
- The server has to be upgraded to version 0.0.009 to provide image proxy and Telegram catalog import. The APK's API base URL remains http://93.123.84.239:40017.
- Backend protects SQLite under a private directory and validates API authentication, file names, image hostnames and payload sizes. TLS termination, server file permissions and reverse-proxy access controls are **still required** for production security.

## Backend 0.0.010 compatibility

Android 0.0.010 sends a pseudonymous device ID on login/register and WebSocket authentication. The separately distributed no-Telegram API uses this to enforce account and best-effort device bans. Hardware identifiers can change after factory resets; modified clients can falsify them. This is not an unbypassable device ban.

The Android app supports `state_changed`, `account_banned` and `chat_deleted` events from the API. Game cases, drop odds and images remain server authoritative.

## Multiplayer trades and notifications (0.0.011)
- Android sends `trade_search`, `trade_target`, `trade_create`, `trade_decide`, `trades_list` over an authenticated WebSocket.
- A trade offers up to six inventory item IDs + coins and requests up to six other item IDs + coins. The server revalidates all ownership and balances at acceptance within one SQLite IMMEDIATE transaction and updates both user revisions.
- Server notifications are durable SQLite records; `notifications_history` paginates and `notifications_read` marks read, with `notification_new` pushed via WebSocket.
- Android 13+ requests notification permission. OS notifications can be shown while the WebSocket is alive. **Delivery when the app is terminated requires an FCM push service and device registration, not included in this release.**
- Coil caches images after their initial download; the app invalidates disk/memory cache keys no longer referenced by server cases/skins or the player's inventory.
- Compatible game API and website files are distributed separately and MUST be deployed on ports 40017 and 40036 respectively.
- Never expose the unauthenticated website, the HMAC signing secret, or the SQLite database on the public internet.

## Android background push (FCM)
The Android app includes an optional Firebase Messaging client and a
FirebaseMessagingService. It works without any Firebase configuration
(foreground WebSocket notifications continue), but background/terminated-app
push is **inactive** until you configure BOTH:
1. GitHub Actions repository secrets FIREBASE_PROJECT_ID, FIREBASE_SENDER_ID,
   FIREBASE_APPLICATION_ID, FIREBASE_API_KEY from an Android app in your
   own Firebase project. Android package: com.caseopener.newtwo.
2. On the game API host: private service account JSON and
   CASEOPENER_FIREBASE_CREDENTIALS=/absolute/path/to/firebase-service-account.json
   plus the separately supplied updated game API.
3. Rebuild APK after adding secrets by rerunning the Android workflow.

Never upload a Firebase service account JSON to GitHub or put it inside an APK.
FCM requires Android notification permission and network/Google Play Services;
Android force-stop and restrictive OEM modes can still prevent prompt delivery.
The game-server database remains authoritative for notification history.
