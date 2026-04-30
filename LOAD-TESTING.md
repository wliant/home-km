# Load testing

k6 scenarios under `tests/load/` exercise the endpoints that have a documented SLO (`docs/slo.md`). Run them **on demand**, against a staging stack or a local docker-compose, never in main CI — load tests are noisy and expensive in tail-latency terms.

## When to run

- Before every release tag — re-run `baseline.js` and compare against the canonical numbers below.
- After any change touching: search, the note/file list endpoints, the reminder scheduler, or the database schema.
- Investigating a real-world slowdown — gives a controlled environment to reproduce.

## How to run

```bash
brew install k6                       # one-time
export BASE_URL=http://localhost:8080
export LOAD_EMAIL=load@example.com    # admin account on the target stack
export LOAD_PASSWORD=LoadTest1234

cd tests/load
k6 run --vus 1  --duration 30s smoke.js     # rig sanity check
k6 run --vus 5  --duration 3m  baseline.js  # canonical SLO mix
k6 run          --duration 5m  search.js    # ramp to 10 VUs
k6 run --vus 8  --duration 3m  notes.js     # list + detail browse
```

Append `--summary-export=run.json` for machine-readable output. Diff against the committed baseline JSON to see regressions.

## Canonical baselines

Captured on a 4-vCPU host, Postgres 15 + MinIO RELEASE.2024-01-16, fresh database with 1 admin + 100 notes + 50 files of ~1 MB each. Numbers are p95 unless stated otherwise.

| Scenario | Endpoint | p95 | Notes |
|----------|----------|-----|-------|
| baseline | `GET /api/auth/me` | < 30 ms | Token-only path |
| baseline | `GET /api/notes` | < 200 ms | List + counts |
| baseline | `GET /api/folders` | < 100 ms | Tree, cached after warm |
| baseline | `GET /api/search?q=the` | < 400 ms | Mixed types, top hits |
| baseline | `POST /api/notes` (create + delete) | < 700 ms | Includes audit write |
| search   | `GET /api/search` (long query) | < 500 ms | tsvector heavy |
| notes    | `GET /api/notes?size=25` | < 300 ms | Page size at clamp |
| notes    | `GET /api/notes/{id}` | < 200 ms | Detail with checklist |

Failure rate must stay below **0.5%** (matches the 99.5% availability SLO). The `http_req_failed: ['rate<0.005']` threshold in `baseline.js` enforces this.

## Interpreting regressions

A run that breaches a threshold blocks release. Triage in this order:

1. **Compare endpoint p95s** against the baseline above. A 2× regression on one endpoint points to that endpoint's recent changes; a uniform 2× regression points to infra (DB pool, MinIO latency, host saturation).
2. **Check the failure rate.** Errors usually mean rate limits, lock contention, or a downstream (MinIO, Postgres) failing under load — fix that first; tail latency on top of errors is meaningless.
3. **Re-run `smoke.js`** to confirm the rig itself isn't the bottleneck (e.g. shared admin credentials hitting the login rate limiter).
4. **Profile.** Backend has `/actuator/prometheus`; the observability stack (`docker-compose.observability.yml`) gives Grafana dashboards for HTTP histograms, DB pool saturation, JVM metrics.

When the regression is real, file an item under `gaps/performance/` rather than fix-and-forget — performance work that isn't tracked is performance work that quietly disappears.

## Out of scope

- **Sustained 24h soak runs** — useful for memory leaks but not the standard release gate. Run separately if one is suspected.
- **MinIO upload throughput** at multi-GB sizes — gated by `MAX_FILE_UPLOAD_MB` (default 100); a deployment-config concern, not application-level.
- **Push notification fan-out** — the outbox publisher decouples reminder firing from push delivery (`gaps/reliability/transactional-outbox-push.md`); load on the push path is bounded by the operator's gateway (FCM/APNS).
