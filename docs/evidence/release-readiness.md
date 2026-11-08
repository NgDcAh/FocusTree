# MVP Release Readiness Evidence

Date: 2026-11-08. This is a debug readiness review only; no signing or
publication was performed.

## Verification

- `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Scope full` — passed (exit 0; formatting, Detekt, JVM tests, lint, debug build, and backend integration gate).
- The same full gate also passed (exit 0) from a fresh source snapshot with build/cache outputs excluded.
- After pushing initial commit `baa469bab372bced482a25a40ddaf1256040c789` to `main`, a fresh clone of `https://github.com/NgDcAh/FocusTree.git` at that exact revision passed `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Scope full` (exit 0; 82 tasks, full verification gate). The clone's ignored `local.properties` contains only the Android SDK location.
- `:app:connectedDebugAndroidTest -PfocusTreeSyncApiUrl=https://localhost:8443 --console=plain` — passed on Medium_Phone_API_37.0 (21 tests, 0 failures, 0 skipped), including PostgreSQL-backed sync E2E and landscape/portrait session restoration.
- Settings dark theme and 1.5x font-scale checks passed; screenshots are in `docs/evidence/settings/`.
- Lifecycle and notification-permission scenario evidence is in `docs/evidence/lifecycle/`.
- Merged debug manifest declares INTERNET and POST_NOTIFICATIONS plus AndroidX/WorkManager runtime permissions. It has no accessibility-service, usage-access, notification-listener, or device-admin permission. Debug network config disallows cleartext and adds only the local test CA alongside system roots.
- README now documents build, test, privacy, synchronization setup, and MVP limitations.

## Remaining Release Checks

- TalkBack was enabled on the API 37 emulator; Android reported the TalkBack service bound. A swipe-navigation check on Settings exposed the screen labels, theme choices, focus-duration field, sync toggle, and sync status in the accessibility hierarchy. The service was turned off after the check. Spoken audio quality was not independently recorded by the host.
- The commit-backed clean-clone gate passed. No release signing or publication was performed.
- No signed release artifact was built, and nothing was published.
