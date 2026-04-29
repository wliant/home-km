#!/usr/bin/env bash
# Break-glass admin password reset.
#
# Use when the sole admin has lost their credentials AND no other admin can
# trigger a normal password reset (gaps/auth/account-recovery.md). Runs
# directly on the host, against the running Postgres container.
#
# Usage:
#   scripts/admin-reset.sh <email> [<new_password>]
#
# If new_password is omitted, a 24-character random one is generated and
# printed once. Make sure you can paste it before running.
#
# What it does:
#   1. Looks up the user by email; aborts if not found or not is_admin.
#   2. Bcrypts the new password (cost 12, matches the app default).
#   3. UPDATEs users.password_hash + sets is_active=true so the account
#      can sign in immediately.
#   4. Revokes every refresh token for that user so any leftover sessions
#      from the lost device cannot continue.
#   5. Audits the event into audit_events.
#
# Requirements: docker compose stack running, htpasswd from apache2-utils
# (pre-installed on most distros; brew install httpd on macOS).

set -euo pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
    echo "Usage: $0 <email> [<new_password>]" >&2
    exit 64
fi

EMAIL="$1"
NEW_PASSWORD="${2:-}"

if [[ -z "$NEW_PASSWORD" ]]; then
    NEW_PASSWORD=$(LC_ALL=C tr -dc 'A-Za-z0-9!@#$%^&*' </dev/urandom | head -c 24)
    echo "Generated new password (copy now — only printed once):"
    echo
    echo "    $NEW_PASSWORD"
    echo
fi

if ! command -v htpasswd >/dev/null 2>&1; then
    echo "htpasswd not found. Install apache2-utils (Debian/Ubuntu) or apache2 (brew on macOS)." >&2
    exit 1
fi

# bcrypt with cost 12. The leading $2y$ vs $2a$ encoding doesn't matter — both
# are accepted by Spring Security's BCryptPasswordEncoder.
HASH=$(htpasswd -nbBC 12 "" "$NEW_PASSWORD" | tr -d ':\n' | sed 's|^||')

if [[ -z "${DB_USER:-}" || -z "${DB_NAME:-}" ]]; then
    echo "DB_USER and DB_NAME must be exported (source your .env)." >&2
    exit 1
fi

PGEXEC=(docker compose -f docker-compose.infra.yml exec -T postgres psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1)

USER_ID=$("${PGEXEC[@]}" -tA -c "SELECT id FROM users WHERE email = lower('$EMAIL') AND is_admin = true LIMIT 1")
if [[ -z "$USER_ID" ]]; then
    echo "No admin user with email '$EMAIL' found." >&2
    exit 2
fi

"${PGEXEC[@]}" <<SQL
BEGIN;
UPDATE users
   SET password_hash = '$HASH', is_active = true
 WHERE id = $USER_ID;

UPDATE refresh_tokens
   SET revoked_at = now()
 WHERE user_id = $USER_ID AND revoked_at IS NULL;

INSERT INTO audit_events (actor_id, action, entity_type, entity_id, occurred_at)
VALUES (NULL, 'ADMIN_BREAK_GLASS_RESET', 'user', '$USER_ID', now());
COMMIT;
SQL

echo
echo "Reset complete for user id $USER_ID. Sign in with the password above."
echo "All other refresh tokens for this user have been revoked."
