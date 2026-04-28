#!/usr/bin/env bash
# Drives the restore drill end-to-end into an isolated `homekm-drill` compose project.
# See docs/restore-drill.md for the procedure this implements.
set -euo pipefail

if [ -z "${DB_USER:-}" ] || [ -z "${DB_NAME:-}" ] || [ -z "${MINIO_ACCESS_KEY:-}" ] || [ -z "${MINIO_SECRET_KEY:-}" ]; then
    echo "Set DB_USER, DB_NAME, MINIO_ACCESS_KEY, MINIO_SECRET_KEY first." >&2
    exit 1
fi

PG_DUMP=${1:-}
MINIO_DIR=${2:-}
if [ -z "$PG_DUMP" ] || [ -z "$MINIO_DIR" ]; then
    echo "Usage: $0 <postgres-dump.sql> <minio-mirror-dir>" >&2
    exit 1
fi

PROJECT=homekm-drill

echo "[1/5] Starting drill infra ($PROJECT)…"
docker compose -p "$PROJECT" -f docker-compose.infra.yml up -d
sleep 10

echo "[2/5] Restoring Postgres dump…"
docker compose -p "$PROJECT" -f docker-compose.infra.yml exec -T postgres \
    psql -U "$DB_USER" -d "$DB_NAME" < "$PG_DUMP"

echo "[3/5] Restoring MinIO mirror…"
mc alias set "$PROJECT" http://localhost:9000 "$MINIO_ACCESS_KEY" "$MINIO_SECRET_KEY"
mc mirror --overwrite "$MINIO_DIR" "$PROJECT"/homekm

echo "[4/5] Starting app stack…"
docker compose -p "$PROJECT" -f docker-compose.app.yml up -d --build

echo "[5/5] Waiting for readiness…"
for i in {1..30}; do
    if curl -fsS http://localhost:8080/actuator/health/readiness > /dev/null 2>&1; then
        echo "OK — drill instance is ready on http://localhost:8080"
        echo "Tear down with: docker compose -p $PROJECT -f docker-compose.app.yml down && docker compose -p $PROJECT -f docker-compose.infra.yml down -v"
        exit 0
    fi
    sleep 2
done
echo "Drill instance failed to become ready." >&2
exit 1
