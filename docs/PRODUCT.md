# Product Specification — Focus Tree

## Product Statement

Focus Tree is an original, offline-first Android application that helps a person
finish intentional focus sessions and see those sessions accumulate as a calm
digital garden, with optional PostgreSQL-backed synchronization through a
dedicated backend API.

The product promise is: **set one intention, focus for a bounded period, and
receive honest visual evidence of the work completed.**

## Target User

A student or knowledge worker who wants a lightweight focus ritual without
creating an account or sharing personal activity data.

## MVP User Journey

1. The user opens the app and sees today's garden and focus total.
2. The user enters an optional intention and chooses a supported duration.
3. The user starts a focus session.
4. The timer remains correct through rotation, backgrounding, and process
   recreation.
5. The user completes or abandons the session.
6. A completed session adds one growth item to the garden and appears in
   history; an abandoned session appears without a reward.
7. The user can review daily and weekly focus totals.
8. If sync is enabled, finalized sessions are uploaded when connectivity is
   available and restored on the same anonymous installation identity.

## MVP Requirements

### Session setup

- Offer predefined durations of 15, 25, 45, and 60 minutes.
- Allow a custom duration from 5 through 120 minutes.
- Allow an optional intention of at most 120 characters.
- Prevent a second session from starting while one is active.

### Active session

- Show intention, remaining time, and a clear cancel action.
- Derive remaining time from persisted timestamps rather than counting ticks as
  durable state.
- Restore the active session after configuration change and process recreation.
- Notify the user when a backgrounded session completes, after obtaining the
  Android notification permission where required.

### Completion and garden

- Persist the outcome exactly once.
- Award one deterministic growth item for a completed session.
- Do not award growth for cancellation or abandonment.
- Display today's completed growth items in a simple original garden scene.
- Use app-created shapes/vectors or properly licensed assets only.

### History and statistics

- Show completed and abandoned sessions in reverse chronological order.
- Show focused minutes for today and the current seven-day period.
- Statistics must be computed from persisted session records.

### Preferences

- Store theme preference and default focus duration locally.
- Support system, light, and dark themes.

### Optional synchronization

- Keep every focus flow usable while offline; sync is never required to run a
  timer or inspect local history.
- Sync is disabled by default and can be enabled explicitly in settings.
- Use an anonymous installation identity and server-issued bearer token; do not
  require a human account for the MVP.
- Send finalized session records through an HTTPS JSON API. The Android app
  never receives PostgreSQL credentials or connects to PostgreSQL directly.
- Retry safely with an outbox and idempotency keys so repeated requests cannot
  duplicate sessions or rewards.
- Allow the user to disable sync and remove the local installation credential.

## Quality Attributes

- **Correctness:** timer and rewards survive lifecycle changes without
  duplication.
- **Privacy:** focus history remains on device unless the user enables sync;
  synced records contain no intention text in server logs.
- **Offline:** every focus, garden, history, statistics, and settings flow works
  without network access; synchronization resumes later.
- **Accessibility:** controls have semantic labels, scalable text, sufficient
  contrast, and do not rely only on color.
- **Performance:** cold start and navigation must not perform blocking database
  work on the main thread.
- **Testability:** timer and reward rules are deterministic under a fake clock.

## Non-goals

- Cloning the commercial Focus Tree application.
- Flashcards, AI quizzes, social/shared gardens, leaderboards, friends.
- Blocking other applications or monitoring application usage.
- Human accounts, cross-user sharing, public cloud backup browsing, ads,
  purchases, and subscriptions.
- Complex 3D rendering or a remote asset economy.

## Product Metrics for Development

- Zero duplicate completion rewards in lifecycle/restart tests.
- All timer domain tests deterministic with no real waiting.
- A new session can be started in three user actions or fewer from the home
  screen.
- Every completion shown in the garden is traceable to one persisted session.

## External Reference Boundary

Public descriptions of the commercial product may be used only to understand
the broad category. They are not implementation specifications:

- https://play.google.com/store/apps/details?id=focustree.app
- https://focustree.webflow.io/
