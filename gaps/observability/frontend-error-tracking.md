# Frontend error tracking + web vitals

| Field | Value |
|---|---|
| Category | Non-functional · Observability |
| Priority | P1 |
| Size | S |
| Status | Closed |

**Current state:** `ErrorBoundary` catches React render errors but only displays a fallback. No error reporting reaches the operator. No web-vitals (LCP, CLS, INP) measurement.

**Gap:** Frontend regressions go unreported until users complain. No data on real-world performance.

**Proposed direction:** Self-hosted GlitchTip or Sentry (Sentry's open-source license remains compatible for self-hosted single-tenant) integrated via `@sentry/react`. Configure `tracesSampleRate` and `replaysOnErrorSampleRate` low enough to be cheap. Capture web-vitals via `web-vitals` package and forward to the same backend. Make it opt-out for users who prefer zero telemetry, off by default unless configured.

**References:** `frontend/src/components/ErrorBoundary.tsx`, `frontend/src/main.tsx`, `frontend/package.json`, `specs/14-frontend-architecture.md`
