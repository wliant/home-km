# Parental review queue

| Field | Value |
|---|---|
| Category | Functional · Child-safe mode |
| Priority | P2 |
| Size | S |

**Current state:** No queue. Parents must remember to set `is_child_safe` whenever they create an item kids should see, and there is no central view of recently added items.

**Gap:** No "things waiting for my approval" workflow. Items added by another parent can be invisible to children indefinitely.

**Proposed direction:** Admin-only page `/admin/review-queue` listing recent items not yet reviewed (`child_safe_review_at IS NULL`). One-click "Mark child-safe" / "Mark adult-only" actions. Notification badge on the admin nav when new items are pending. Pairs with `child-safe/moderation-hooks.md` for the auto-flagging.

**References:** `backend/src/main/java/com/homekm/admin/`, `backend/src/main/java/com/homekm/common/ChildSafeService.java`, `frontend/src/features/admin/`, `specs/09-child-safe.md`
