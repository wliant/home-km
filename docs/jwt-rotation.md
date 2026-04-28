# Rotating `JWT_SECRET`

**When to rotate:**
- You believe the secret may have leaked (commit, copy-paste into a chat, etc.).
- Annually as a hygiene practice.
- Before publishing a new public deployment.

## Procedure

1. **Generate a new secret:**
   ```bash
   openssl rand -base64 48
   ```
2. **Update the secret in your environment.** If you use the Docker secrets pattern (see below), `echo "<new>" | docker secret create jwt_secret_v2 -` then update the compose file to reference `jwt_secret_v2`. Otherwise update `.env`.
3. **Restart the API:**
   ```bash
   docker compose -f docker-compose.app.yml up -d --force-recreate api
   ```
4. **All access tokens are now invalid.** The frontend interceptor sees a 401 and refreshes from the refresh token (which is stored hashed in `refresh_tokens` and unaffected by the JWT secret change). Users stay signed in.
5. **Optional: also invalidate refresh tokens** (forces everyone to log in again):
   ```sql
   UPDATE refresh_tokens SET revoked_at = now() WHERE revoked_at IS NULL;
   ```

## Verification

```bash
curl -fsS -i http://localhost:8080/api/auth/me -H "Authorization: Bearer <old-token>"
# expected: 401 UNAUTHORIZED
```

## Recovery

If you accidentally update `JWT_SECRET` in production *and* lose the new value, all sessions break and you cannot recover the old tokens. Roll forward by:
1. Setting a fresh secret.
2. Forcing all users to log in again (revoke all refresh tokens as above).
