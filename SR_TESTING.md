# Screen-reader testing checklist

Manual matrix for the screen-reader pass. Run before every release; capture issues as gap entries under `gaps/a11y/`. The Playwright suite (`e2e/tests/a11y-sr.spec.ts`) catches regressions of the structural properties between manual passes; the matrix below is the source of truth for "does it actually work for a screen-reader user".

## Tooling

- **macOS / iOS** — VoiceOver. `Cmd+F5` toggles desktop VO; iPhone Settings → Accessibility → VoiceOver. Caption mode helps a sighted reviewer follow along.
- **Windows** — NVDA (free, https://nvaccess.org). Speech Viewer on for the same reason.
- **Chromebook / Android** — ChromeVox / TalkBack. Lower priority for self-hosted households.

## Top 10 flows

For each, listen end-to-end without looking at the screen. Record any place where the announcement is wrong, missing, or out of order.

| # | Flow | Expected key announcements |
|---|------|----------------------------|
| 1 | Sign in (email + password) | "Email, edit text"; "Password, secure edit text"; "Sign in, button"; route change → page title via the polite live region |
| 2 | Register a new account | Same shape as sign-in; password-strength hint reads as a hint, not a notification |
| 3 | Create a note | "Create note, button"; in editor, title field reads first, body field second; save announces "Note saved" |
| 4 | Add a reminder to a note | "Add reminder, button" opens a dialog announced as such; recurrence picker reads as a combo box |
| 5 | Upload a file | File-picker reads its label; progress reads via aria-live; success/failure announced via toast |
| 6 | Search | Search box has a label; "X results" via the existing ICU plural string; type tabs read as tabs |
| 7 | Mark a note child-safe | Toggle reads its current state ("not pressed" → "pressed"); cascade warning reads as alert |
| 8 | Delete + restore from trash | Delete announces a destructive action; trash list reads each item with deleted timestamp; restore announces success |
| 9 | Settings → push notifications | Subscribe button reads its current state; per-event toggles read with their labels and current state |
| 10 | Sign out from sessions panel | "Sign out" button has accessible name; current device labelled "this device" |

## Common defects to look for

- **Unlabelled buttons** — anything with only an icon (`×`, `⋯`). Add `aria-label`.
- **Form fields without `<label htmlFor>`** — covered for auth pages by `gaps/a11y/color-contrast.md`; sweep other forms.
- **Live regions firing too often** — `aria-live=polite` should announce route changes and toasts, not every typed character.
- **Focus loss on navigation** — clicking a link should leave focus on the new page's `<h1>` (`RouteAnnouncer` handles this; verify it actually moves).
- **Modal dialogs without focus trap** — focus must stay inside until Escape or close.
- **Keyboard-only reachability** — every visually-surfaced action reachable via Tab + Enter/Space.
- **Tap-target size** on mobile — minimum 44×44 even for SR-touch users.

## Reporting

For each defect, file a short item under `gaps/a11y/` (or a comment on the running PR) with:

- Page and component
- What VoiceOver/NVDA said vs. what should have been said
- Steps to reproduce
- Severity: blocker (no completion possible) / serious (workaround painful) / minor

Prioritise blockers over serious; minors batch into a "polish pass" gap.

## When to skip a flow

A flow behind a feature flag (e.g. `EMBEDDING_ENABLED` semantic search) doesn't need a pass while disabled. Note that exception in the report so reviewers don't treat the gap as covered.
