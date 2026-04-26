# Backend error message localization

| Field | Value |
|---|---|
| Category | Functional · i18n / l10n |
| Priority | P2 |
| Size | S |

**Current state:** `GlobalExceptionHandler` returns hardcoded English `message` strings inside `ErrorResponse`.

**Gap:** Even with frontend i18n, server-side validation messages always come back in English.

**Proposed direction:** Use Spring `MessageSource` with `messages_<locale>.properties`. Resolve locale per request from the `Accept-Language` header (set by the frontend based on the user's chosen locale). Errors return both `code` (stable identifier the frontend already maps) and `message` (localized). Frontend should prefer `code`-based lookup when possible.

**References:** `backend/src/main/java/com/homekm/common/GlobalExceptionHandler.java`, `backend/src/main/java/com/homekm/common/ErrorResponse.java`, `backend/src/main/resources/`, `specs/11-api-conventions.md`
