# Focus Tree sync service

The `:server` module is a Kotlin/JVM HTTP service. It is the only application
module that connects to PostgreSQL. The Android app talks to this API over HTTPS
and never receives database connection details.

## Endpoints

- `GET /health` returns service health.
- `POST /v1/installations` creates an anonymous installation and returns its
  bearer token once.
- `DELETE /v1/installations/me` revokes the authenticated installation token.
- `POST /v1/sessions` accepts a finalized `COMPLETED` or `ABANDONED` session.
  Replaying the same data is idempotent; reusing an ID with different data
  returns `409 Conflict`.
- `GET /v1/sessions?limit=50` returns sessions newest first. `limit` is bounded
  to 1–100. Follow `nextBeforeStartedAtEpochMillis` and `nextBeforeSessionId`
  response values as query parameters to fetch the next page.

Session payloads contain `clientSessionId`, `intention`,
`plannedDurationMillis`, `startedAtEpochMillis`, `finishedAtEpochMillis`, and
`outcome`. In-progress sessions are rejected. The server stores no reward
ledger and never issues focus rewards; rewards remain an idempotent local Room
transaction.

## Run locally

Set `DATABASE_URL`, `DB_USER`, `DB_PASSWORD`, and optionally `PORT`, then run:

```powershell
./gradlew.bat :server:run
```

The service runs Flyway migrations on startup. Deployments must expose the API
through HTTPS. Database credentials must be supplied by the deployment
environment or secret manager and must not be placed in source control or the
Android package.

## Verification

`:server:test` runs service and HTTP route tests. `:server:postgresIntegrationTest`
starts a disposable PostgreSQL container with Testcontainers and verifies the
migration, hashed token authentication/revocation, idempotent upserts, conflict
detection, and paging. The full repository verification script runs both.
