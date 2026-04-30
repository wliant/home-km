# Load tests

k6 scripts for the highest-value backend endpoints. They run **on demand** against a staging stack (or a local docker-compose), never in main CI — load tests against the real app are noisy and expensive in tail-latency terms.

## Prerequisites

- [k6](https://k6.io) installed locally (`brew install k6` on macOS).
- Home KM backend reachable at `BASE_URL` (defaults to `http://localhost:8080`).
- A registered admin account whose credentials live in env vars (`LOAD_EMAIL`, `LOAD_PASSWORD`). Create one by running through `/register` once.

## Running

```bash
cd tests/load

# Smoke test — small, validates the script wiring works.
k6 run --vus 1 --duration 30s smoke.js

# Search-heavy mix — typical browse pattern.
k6 run --vus 10 --duration 5m search.js

# Reminder firing — proves the scheduler + outbox handle a burst.
k6 run --vus 5 --duration 2m reminders.js

# Per-endpoint baseline — used for the LOAD-TESTING.md numbers.
k6 run --vus 5 --duration 3m baseline.js
```

Append `--summary-export=results.json` for machine-readable output to compare against the canonical baselines.

## What's covered

- `auth.js` — shared login helper (token cache via setup()).
- `smoke.js` — single VU, hits `/api/auth/me` and `/actuator/health`.
- `search.js` — search ramp at varying query lengths.
- `notes.js` — list-notes pagination, per-note detail.
- `baseline.js` — every endpoint flagged in `docs/slo.md`, threshold gate set to 95% of the SLO.

See `LOAD-TESTING.md` (repo root) for the canonical baseline and how to interpret regressions.
