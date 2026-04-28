# TLS reverse proxy

Home KM ships with a plain-HTTP nginx in `frontend/Dockerfile` so it works out of the box on a LAN and inside a private network. For any deployment reachable beyond `localhost` you must terminate TLS at a reverse proxy in front of the stack — service workers, push notifications, the PWA install prompt, and `Strict-Transport-Security` all require HTTPS.

## When you need this

- Internet-exposed installs
- LAN installs accessed by hostname (e.g. `https://homekm.lan`) on iOS/Safari, which gates push and Add-to-Home-Screen behind a secure context
- Anywhere the browser address bar shows something other than `http://localhost`

## Recommended: Caddy with automatic Let's Encrypt

Caddy auto-provisions and renews certificates. Add `caddy` as a sibling service running on the same Docker network as the frontend.

`docker-compose.tls.yml`:

```yaml
services:
  caddy:
    image: caddy:2-alpine
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./Caddyfile:/etc/caddy/Caddyfile:ro
      - caddy_data:/data
      - caddy_config:/config
    networks:
      - infra-net

volumes:
  caddy_data:
  caddy_config:

networks:
  infra-net:
    name: homekm-infra-net
    external: true
```

`Caddyfile`:

```caddy
homekm.example.com {
    encode zstd gzip
    reverse_proxy frontend:80 {
        header_up X-Forwarded-Proto {scheme}
        header_up X-Forwarded-For {remote_host}
        header_up X-Real-IP {remote_host}
    }
}
```

Bring it up after the app stack:

```bash
docker compose -f docker-compose.tls.yml up -d
```

Caddy obtains the certificate on first request, renews automatically, and removes the need for manual `add_header Strict-Transport-Security` because nginx already emits it (`frontend/nginx.conf.template`).

## LAN install with a private CA

For a household-only install where you control DNS:

1. Issue a certificate from a private CA (`step-ca`, `mkcert`, or `smallstep`) for `homekm.lan` plus its subdomains
2. Mount the cert + key into nginx (or a fronting Caddy/Traefik) and listen on `:443`
3. Distribute the CA certificate to family devices once

`step-ca` and `mkcert` both produce browser-trusted certs after one-time CA install.

## Cloudflare Tunnel (zero port-forward)

If you do not want to expose ports on your home router:

```bash
cloudflared tunnel create homekm
cloudflared tunnel route dns homekm homekm.example.com
cloudflared tunnel run --url http://frontend:80 homekm
```

TLS terminates at Cloudflare's edge; the tunnel carries plaintext over an authenticated mTLS channel back to the host.

## What `nginx.conf.template` already does

The shipped frontend nginx already emits HSTS and a strict CSP (see §"Security headers" in `specs/12-infrastructure.md`). The reverse proxy above only needs to:

- Terminate TLS
- Forward `X-Forwarded-Proto: https` so backend tooling that builds links picks up the right scheme
- Pass `X-Forwarded-For` for accurate client IPs in logs and rate-limit decisions

Do **not** add a duplicate `Strict-Transport-Security` at the proxy — the inner nginx already sets it. Duplicate `add_header` calls combine, but identical values are wasteful.

## Verifying

```bash
curl -sI https://homekm.example.com/ | grep -E "Strict-Transport-Security|Content-Security-Policy"
```

Both headers should be present. If `Strict-Transport-Security` is missing, the proxy is not forwarding the inner nginx response headers (Cloudflare strips some by default — re-enable in the Transform Rules dashboard).
