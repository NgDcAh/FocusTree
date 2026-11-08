# Architecture — Focus Tree Android

## Architectural Style

Use a single Android application module with package-by-feature organization,
unidirectional data flow, and clear UI/domain/data boundaries. The approved
PostgreSQL integration adds one Kotlin server module because it is a separate
runtime and security boundary; do not split the Android app into extra modules.

```text
Compose screen
    ↓ events              ↑ immutable UI state
ViewModel
    ↓ use-case/repository interfaces
Domain timer and reward rules
    ↓ repository ports
Room / DataStore / Android notification adapter
    ↓ finalized-session outbox
HTTPS sync client → Kotlin server API → PostgreSQL
```

The Android app never connects directly to PostgreSQL. Only the server module
knows database credentials, which come from environment variables.

## Planned Package Layout

```text
app/src/main/java/<package>/
├── app/                    # Application, navigation, dependency wiring
├── core/
│   ├── database/           # Room database, entities, DAO, migrations
│   ├── model/              # Shared immutable domain models
│   ├── time/               # Clock abstraction and Android implementation
│   ├── notifications/      # Completion notification adapter
│   └── ui/                 # Theme and small shared UI primitives
├── feature/
│   ├── home/
│   ├── timer/
│   ├── garden/
│   ├── history/
│   └── settings/
└── MainActivity.kt
```

## Core Domain Model

Suggested concepts, subject to refinement during `FT-FND-001`:

```text
FocusSession
- id
- intention
- plannedDuration
- startedAt
- expectedEndAt
- finishedAt
- outcome: ACTIVE | COMPLETED | ABANDONED
- rewardGranted

TimerSnapshot
- sessionId
- state
- remainingDuration
- progress
```

Persist instants/durations in an unambiguous machine representation. Convert to
local dates only at presentation/query boundaries and test daylight-saving/time
zone changes.

## Timer Design

The source of truth is the active session's timestamps. A coroutine ticker may
refresh the UI, but it cannot own elapsed time.

```text
remaining = max(expectedEndAt - clock.now(), zero)
```

Completion must be idempotent: a transaction changes an active session to
completed and grants its reward no more than once. Process recreation must load
the active session and reconcile it against the clock.

Use a foreground service only if required by validated background behavior and
current Android platform policy. Keep the domain timer independent from that
adapter. Never keep an Activity or Compose reference in a service.

## Data Ownership

- Room owns focus sessions and their outcomes.
- Room remains the source of truth for Android UI and offline behavior.
- PostgreSQL owns synchronized server copies keyed by anonymous installation ID
  and client-generated session ID.
- Garden/statistics are projections of session records; avoid a second mutable
  source of truth unless performance measurements require it.
- DataStore owns theme and default-duration preferences.
- Android Keystore-backed storage owns the anonymous sync bearer token; never
  store it in source code or ordinary preferences.
- ViewModels expose `StateFlow` and collect repositories with lifecycle-aware
  Compose APIs.

## Sync and PostgreSQL Boundary

- Add a Kotlin `:server` module only after the Android foundation passes.
- Use a small HTTP API; Android sends finalized sessions, never SQL.
- Register an anonymous installation and return a revocable bearer token. Human
  accounts remain out of scope.
- Persist installations and finalized focus sessions in PostgreSQL. Enforce a
  unique `(installation_id, client_session_id)` key and terminal outcome checks.
- Use Flyway migrations and environment-provided connection settings. Do not
  commit database URLs, passwords, or production certificates.
- Use a Room outbox for retryable uploads. Successful server upserts are
  idempotent, and server responses never issue a second completion reward.
- Treat sync as eventually consistent. Timer correctness and reward issuance
  remain local transactions and never wait on the network.

## Dependency Direction

Allowed:

```text
UI → domain interfaces ← data implementations
app wiring → all concrete implementations
```

Forbidden:

- Composable → DAO/DataStore/service
- DAO → ViewModel/UI
- Domain model → Android UI class
- Feature package → another feature's internal implementation
- Android client → PostgreSQL/JDBC driver
- Server route → Android/Compose class

## Error Handling

- Convert storage/platform failures into typed domain/UI errors.
- Never silently turn a failed persistence write into a successful completion.
- User messages should be actionable and must not expose stack traces.
- Log technical context without intention text or other user-entered content.

## Testing Strategy

- Pure JVM tests: timer calculations, state transitions, reward idempotency,
  statistics and validation.
- Room tests: DAO queries, transactions and migrations.
- ViewModel tests: state/event behavior with fake repositories and fake clock.
- Compose/instrumentation tests: start/cancel/complete journeys and restoration.
- Manual device check: notification and real background lifecycle behavior.
- PostgreSQL integration tests: migrations, installation authentication,
  idempotent session upserts, paging, and retry behavior.
- Sync tests: offline outbox, reconnect, duplicate delivery, invalid token, and
  server failure without blocking local focus flows.

Tests must use a fake clock or test scheduler; never wait real focus durations.

## Build Quality Gates

The Android scaffold must provide these Gradle tasks:

- `ktlintCheck`
- `detekt`
- `testDebugUnitTest`
- `lintDebug`
- `assembleDebug`

Dependency versions must be pinned in `gradle/libs.versions.toml`. Add CI only
after the local full verification pipeline is green.

When the server feature becomes active, the full gate must also run server unit
tests and PostgreSQL integration tests against an ephemeral test database.
