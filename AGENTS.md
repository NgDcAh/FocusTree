AGENTS.md — Focus Tree Kotlin (Cost-aware Codex Harness)
This is the operating contract for coding agents working in focus-tree-kotlin/.
Keep edits scoped to one feature. Optimize tool calls and context usage without weakening correctness or safety.

Cost-aware workflow
- Start with the handoff and the active feature only; use targeted `rg`/file reads and bounded output. Avoid broad file listings, generated/build directories, and rereading content already captured in the handoff.
- Batch independent reads/searches into one tool call. Do dependent actions sequentially and stop exploring once the active feature's implementation path and acceptance criteria are clear.
- Prefer existing libraries, patterns, and assets. Do not add a dependency, image-generation step, or external service when a small Compose/vector implementation meets the requirement.
- Run the narrowest useful check while iterating; run the required full gate once implementation stabilizes. Do not rerun unchanged failing commands without a new diagnosis.
- Keep state-file updates consolidated at the end. Record concise evidence and exact commands, not full logs.

UI/UX design workflow
- Before implementing a feature that changes or adds a screen, define its UI/UX direction in a short design note or feature evidence: user goal, primary action, content hierarchy, key states (loading/empty/error/active/completed), navigation, and acceptance checks. For non-UI features, skip this step.
- Use an original pixel-art visual direction for product UI: a restrained pixel-inspired palette, crisp geometric forms, and original code-drawn/vector pixel motifs. Keep controls and text legible at Android sizes; do not pixelate body text or sacrifice touch targets, contrast, accessibility semantics, responsive layouts, or dark-theme support.
- Reuse a small set of design tokens and composable patterns. Keep pixel artwork decorative unless its meaning is also conveyed through accessible text/semantics. Do not copy another product's branding, artwork, text, or layouts; do not create raster assets or install design dependencies unless the active feature requires them.
- Review changed screens in light and dark themes and at large font scale, alongside the feature's required emulator evidence. Capture screenshots only when required by acceptance criteria or Definition of Done.
Start of a task
1. Confirm repository root and inspect git status --short (if Git metadata exists). Preserve unrelated changes.
2. Read session-handoff.md for current state; read only the active feature entry in feature_list.json and its dependencies. Do not dump the entire JSON when the relevant entry can be extracted.
3. Consult docs/PRODUCT.md only for product behavior questions, and docs/ARCHITECTURE.md for architecture, storage, dependency or cross-module changes. Read only the needed sections.
4. Inspect the code and tests related to the active feature before modifying anything. Do not run a repository-wide exploration unless dependencies are unclear.
5. Run environment initialization only if the workspace is new, dependencies have changed, environment setup is broken, or the user specifically asks. Prefer the non-verifying setup steps where available; do not assume that init.ps1 is lightweight (README says it runs full verification).
6. Work on exactly one unblocked not-started or in-progress feature. If the handoff names an in-progress feature, continue it. Never silently advance to another feature.
If baseline verification fails, determine whether the failure predates your edits. Do not repair unrelated failures without approval.
Product and safety boundaries
- Build an original offline-first focus-timer and digital-garden app. Do not copy Focus Tree branding, artwork, text, layouts or proprietary behavior.
- No human account is required. Optional sync uses a backend-issued anonymous installation identity. PostgreSQL-backed sync was approved on 2026-11-07 and remains scoped to its tracked features.
- Social gardens, flashcards, app blocking, AI features, ads, subscriptions and payments are out of scope unless explicitly added to feature_list.json.
- Do not add accessibility-service, usage-access, notification-listener or device-admin permissions for the MVP.
- Ask before expanding scope, requesting sensitive permissions, introducing paid infrastructure, performing a destructive migration, signing or publishing the application, or changing the agreed trust boundaries.
- Never commit credentials, signing keys, local SDK paths, generated APKs or user focus-history data.
Architecture invariants
- Kotlin + Jetpack Compose for Android. Keep product code in one :app module, packaged by feature; the approved backend may use a distinct Kotlin :server module.
- Compose UI uses immutable state and explicit events. Composables do not directly access Room, DataStore, clocks or Android services; ViewModels depend on interfaces.
- Room is the Android/offline UI source of truth. DataStore is only for small preferences. PostgreSQL is the backend source of truth for synchronized copies.
- Android communicates with PostgreSQL only through the HTTPS backend API; credentials remain server-side.
- Timer truth comes from persisted timestamps and a testable Clock, not an in-memory decrementing counter. At most one focus session may be active.
- Validate duration before starting. Restore timer state from persisted timestamps. Completion rewards must be idempotent; abandoned/cancelled sessions earn no reward. Clock anomalies must not generate negative durations or duplicate rewards.
- Domain logic must be JVM-testable without an emulator. Pin dependencies in the version catalog; do not add dependencies unnecessarily.
Coding and verification loop
- Make the smallest change that satisfies the active feature's acceptance criteria. Write or update relevant tests for behavior changes.
- During implementation, prefer focused tests / targeted checks; use scripts/verify.ps1 -Scope fast when appropriate.
- Run powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1 -Scope full once after the implementation stabilizes and before marking a feature pass. If a fix changes behavior after that run, rerun the impacted tests and the full gate before completion.
- Full verification includes formatting, Detekt, JVM tests, Android lint and debug build. Do not weaken tests, lint, Detekt or coverage rules to obtain a pass.
- For features affecting Compose UI, lifecycle or permissions, collect the emulator/device evidence specified by the feature and Definition of Done. Check relevant accessibility semantics and dark theme for changed UI.
- Prefer concise command output. On success report exit code and brief summary; on failure inspect the specific error lines and relevant surrounding context. Avoid repeatedly loading the entire build log.
- If the same failure remains unresolved after 3 materially different diagnosis/fix attempts, stop looping. Record reproduction, attempted fixes, logs needed, and mark the feature blocked or leave in-progress as appropriate. Do not call a feature complete without evidence.
- Never repeatedly retry downloads or network-dependent commands without a changed hypothesis.
Definition of Done
Mark the active feature pass only if:
- Every acceptance criterion in feature_list.json is satisfied.
- Relevant tests cover success and failure, plus restoration where applicable.
- Full verification passes, with exact commands and concrete evidence recorded.
- Required emulator/device checks, accessibility and dark-theme checks pass when applicable.
- No unrelated changes or generated artifacts remain.
- feature_list.json, progress.md, and session-handoff.md reflect the outcome.
Session handoff
Update the three state files once at the end, not after every tool call. Keep session-handoff.md concise: active feature, verified status, changed files, tests run/results, blockers, and next action. Preserve essential architectural decisions and unresolved risks; do not duplicate full build logs or restate all completed features.

Final response: feature ID and status; what changed; exact verification summary; unresolved risks or blocker; next action. Be concise and evidence-based.
