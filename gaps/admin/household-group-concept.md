# Household / group concept

| Field | Value |
|---|---|
| Category | Functional · Admin |
| Priority | P2 |
| Size | M |

**Current state:** All users implicitly belong to one household — there is no `household` entity. This works because the deployment is single-tenant by design (`specs/00-overview.md`).

**Gap:** No subgroups within the household. "Adults", "Kids", "Grandparents visiting" cannot be addressed as sets for reminders, ACLs, or notifications.

**Proposed direction:** Without breaking single-tenancy, introduce `groups` and `group_members` tables. Groups are lightweight membership lists usable wherever a list of users appears (reminder recipients, ACL grantees, mention targets). Built-in groups: "Everyone", "Adults", "Kids" (auto-derived from `is_child`).

**References:** `backend/src/main/java/com/homekm/auth/User.java`, `backend/src/main/java/com/homekm/reminder/ReminderRecipient.java`, `specs/00-overview.md`, `specs/02-auth.md`
