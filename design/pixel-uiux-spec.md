# Focus Tree Pixel UI/UX Direction

## Product feel

Create a quiet, optimistic focus ritual: the interface should feel like tending a small personal pixel garden, while keeping the timer and next action immediately clear. Use original artwork and plain, friendly language. Avoid visual noise, streak pressure, rankings, or copied game/app conventions.

## Visual system

- Pixel character comes from crisp block geometry, stepped silhouettes, tiny garden details, and restrained 1–2 dp pixel-like borders or corner steps. Keep text in the platform typeface for legibility.
- Palette: leaf green for growth and primary actions; warm paper/cream and muted soil tones in light mode; deep forest/charcoal surfaces and lighter leaf accents in dark mode. Reserve a warm amber for attention and red only for destructive/error states.
- Use a consistent 4 dp spacing base, with 8/12/16/24/32 dp rhythm. Cards use simple surfaces and clear outlines; primary controls remain standard Compose/Material touch targets (minimum 48 dp).
- Pixel motifs are decorative unless paired with text or equivalent semantics. Never rely on color alone to convey status.

## Navigation and screen hierarchy

Primary destinations: **Focus**, **Garden**, **History**, and **Settings**. Keep the active session and start/resume action prominent on Focus. Keep navigation labels visible and accessible.

| Screen | User goal | Content order | Primary action |
| --- | --- | --- | --- |
| Focus setup | Choose a task and duration | Intention, duration presets/custom duration, validation | Start focus |
| Active focus | Stay focused and know time remaining | Intention, large remaining time, subtle growth motif, cancel affordance | None; cancel is explicit and secondary |
| Garden | See completed work represented honestly | Today's total, pixel garden, empty/growing state explanation | Start focus |
| History | Review outcomes and time totals | Today/week totals, reverse-chronological sessions, completed/abandoned labels | Navigate to Focus |
| Settings | Adjust local behavior and sync choice | Theme, default duration, explicit sync status/opt-in | Save/apply locally |

## States and interaction

- Setup: initial selection, invalid custom duration, overlong intention, active-session conflict, start confirmation.
- Focus: restoring, running, backgrounded, completed, and abandoned. Persisted timer truth wins on restoration; never imply reward for cancellation.
- Garden/history: first-use empty state, populated state, and recoverable loading/error state where data access can fail.
- Settings: current preference, saved change, and sync disabled/enabled/status states.
- Give immediate feedback to taps, preserve selection during validation, and make destructive cancellation explicit. Respect system back/navigation and rotation.

## Accessibility and responsive behavior

- Maintain readable contrast in both themes, scalable typography, semantic names/states for controls, and logical screen-reader order.
- Support large font scale without clipping; allow content to scroll. Keep controls at least 48 dp and do not encode meaning only in pixel art or color.
- Decorative art is excluded from accessibility traversal; meaningful growth counts and session outcomes have text equivalents.

## Review checklist

- Review representative UI on an emulator in light theme, dark theme, and large font scale; record screenshots/evidence in the active feature entry.
- Check the primary action, error/empty/active/completed state, touch target, contrast, semantics, and clipping before accepting each screen.
- Build motifs from original Compose drawing/vector shapes. Do not introduce external image assets, design dependencies, or copied visual references unless an active feature requires and licenses them.
