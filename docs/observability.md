# Observability

The default deploy is bare: structured JSON logs to stdout, Prometheus metrics on `/actuator/prometheus`, no aggregation. To run a full local stack, opt into `docker-compose.observability.yml`.

## What you get

| Component | Purpose | URL |
|-----------|---------|-----|
| Prometheus | Metrics scrape + 14d retention | http://localhost:9090 |
| Grafana | Dashboards (system + product) | http://localhost:3001 (`admin` / `$GRAFANA_PASSWORD`) |
| Loki | Log aggregation, 30d retention | http://localhost:3100 |
| Promtail | Tails Docker container logs into Loki | (sidecar) |
| Tempo | OTel trace collector | http://localhost:3200, OTLP gRPC :4317 |

## Bring it up

```bash
docker compose -f docker-compose.observability.yml up -d
```

Then wire the API to ship traces:
```bash
# .env
OTEL_EXPORTER_OTLP_ENDPOINT=http://tempo:4317
OTEL_SERVICE_NAME=homekm-api
docker compose -f docker-compose.app.yml up -d --force-recreate api
```

The API image ships with the OpenTelemetry Java agent at `/opentelemetry-javaagent.jar`. The entrypoint attaches it only when `OTEL_EXPORTER_OTLP_ENDPOINT` is set, so dev/test runs stay lean.

## Dashboards

Two are provisioned automatically (`infra/grafana/provisioning/dashboards/`):

- **Home KM — System**: JVM heap, HTTP req/s by status, HikariCP pool, GC pause.
- **Home KM — Product**: logins/24h, notes created, files uploaded, 5xx by route.

Customize by editing the JSON; Grafana picks up changes in 30s.

## Log aggregation

Promtail tails Docker container logs and labels them with the service name. Useful queries in Grafana → Explore → Loki:

```logql
{service="api"} |= "ERROR"
{service="api"} |~ "(?i)slow query|duration:"
{service="postgres"} |= "duration:"
```

## Frontend errors (optional)

Set `VITE_SENTRY_DSN` in the build to ship JS errors and Web Vitals to a self-hosted [GlitchTip](https://glitchtip.com/) instance (Sentry-compatible). Off by default. See `frontend/src/lib/errorTracking.ts` and the Settings → Privacy toggle for opt-out.

## Out of scope (yet)

- Alerting rules. Wire Alertmanager + a webhook later.
- USE/RED method dashboards by service.
- Trace-to-log correlation. The MDC `requestId` is already attached to log lines and can be used as a trace identifier.
