# Service Level Objectives

This document records the household-scale targets the deployment is sized for. Acceptable error rates and latency budgets implicit in the design are made explicit here so operators can decide when a regression is bad enough to roll back, and so alerts (`docker-compose.observability.yml`) have something concrete to fire on.

These numbers suit a 2–6 person household running on a single host. A multi-household deployment should re-derive them.

## Targets

| SLI | SLO | Why |
|-----|-----|-----|
| **API availability** | ≥ 99.5% / month (≈ 3h 39m monthly downtime budget) | Self-hosted, single host. Power and network blips dominate; we don't promise five-nines because we don't run redundant infra. |
| **API read latency p95** | < 500 ms | Empirical: list endpoints with the page-size cap (`Pagination.MAX_SIZE`) and tsvector indexes finish in tens of ms; 500ms is the threshold above which the UI's "Searching…" placeholder visibly lingers. |
| **API write latency p95** | < 1 s | Notes / files / reminders write paths are I/O-bound on Postgres + MinIO. 1s gives headroom for bcrypt at the cost factor we ship (12). |
| **File upload latency p95** | < 2 s for a 10 MB file | Uploads transit MinIO then a synchronous thumbnail pass (image only). 2s is the median home-LAN ceiling. |
| **Push delivery success** | ≥ 95% of fired reminders | Push services (FCM/APNS bridges) drop a long tail; this lives below the API SLO because we cannot retry indefinitely without spamming. |
| **Restore drill cadence** | Monthly, must succeed | If you can't restore, you don't have backups. See `docs/restore-drill.md`. |

## Alert recipes

Rules go into the Prometheus config that Grafana ships with (`docker-compose.observability.yml`). Suggested thresholds:

```yaml
groups:
  - name: homekm-slo
    rules:
      - alert: HomeKmAvailability
        expr: |
          1 - (sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) /
               sum(rate(http_server_requests_seconds_count[5m]))) < 0.995
        for: 10m
        labels: { severity: warning }
        annotations:
          summary: "Home KM availability below 99.5%"

      - alert: HomeKmReadLatencyHigh
        expr: |
          histogram_quantile(0.95,
            sum(rate(http_server_requests_seconds_bucket{method="GET"}[5m])) by (le)
          ) > 0.5
        for: 10m
        labels: { severity: warning }

      - alert: HomeKmPushDeliveryLow
        # push_send_total / push_attempt_total — exposed by ReminderScheduler.
        expr: |
          (sum(rate(homekm_push_send_total{outcome="success"}[1h]))
           / sum(rate(homekm_push_send_total[1h]))) < 0.95
        for: 1h
        labels: { severity: warning }
```

## Recovery objectives

| Objective | Target | Source of truth |
|-----------|--------|-----------------|
| **Recovery Time Objective (RTO)** | 4 hours | Acceptable household downtime — long enough to drive home from work and run the restore drill. |
| **Recovery Point Objective (RPO)** | 1 hour | Maximum data loss tolerated. Backup cadence (`docs/backups.md`) must produce a usable snapshot every hour to satisfy this. |

Backup cadence and restore-drill design must keep both targets achievable. If your real-world backups run nightly, RPO degrades to 24h — adjust this document when you change cadence.

## Revisit annually

These targets reflect April 2026 traffic patterns. When meaningful change happens (more users, larger file volume, observability stack live), revisit and either tighten the numbers or relax them — keeping a stale SLO is worse than not having one.
