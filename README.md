# Focus Tree Kotlin

An original, offline-first Android focus timer and digital garden. It is not
affiliated with the commercial Focus Tree product. Focus sessions, garden
growth, history, and preferences remain available on-device through Room and
DataStore. Optional session synchronization uses an HTTPS backend and a
server-issued anonymous installation identity.

## Stack

- Kotlin, Android, Jetpack Compose, and Material 3
- Room for local focus-session data; DataStore for small preferences
- Kotlin HTTP backend with PostgreSQL as the synchronized-copy source of truth
- Gradle wrapper and version-catalog-managed dependencies

## Build and Test

Use JDK 17 and an installed Android SDK. Read `AGENTS.md` when starting a
development session. Initialize a new workspace with:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\init.ps1
```

On macOS/Linux, `./init.sh` initializes the workspace and runs full
verification. Run the full verification gate on Windows with:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Scope full
```

Run Android instrumentation tests on a connected device or emulator with:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
```

For local synchronization development, configure `DATABASE_URL`, `DB_USER`,
and `DB_PASSWORD` in the environment outside the repository. Set the HTTPS
backend URL at build time:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest `
  -PfocusTreeSyncApiUrl=https://your-local-api.example --console=plain
```

Debug builds can trust a locally generated test CA through the debug-only
network security configuration. Keep database credentials, keystores, and
private keys out of source control. Release signing and publication are not
part of this workflow.

## Privacy and Known Limitations

- Synchronization is disabled by default and requires explicit opt-in. Turning
  it off cancels scheduled work and removes the local installation credential.
- The Android app communicates with PostgreSQL only through the configured
  HTTPS backend; database credentials remain server-side.
- The app requests Internet and notification permissions. It does not request
  accessibility-service, usage-access, notification-listener, or device-admin
  access. Notifications are optional and requested when needed.
- There is no human account, social garden, app blocking, AI feature,
  advertising, subscription, or payment functionality.
- Synchronization requires a network and a compatible configured backend; core
  timer and garden use remains available offline.

See `docs/PRODUCT.md` for product scope and `docs/ARCHITECTURE.md` for system
boundaries.
