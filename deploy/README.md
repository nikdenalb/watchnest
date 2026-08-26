# Demo deploy (Docker Compose)

Docker Compose stack for a public demo on a Yandex Cloud VM:
Postgres 18, `planner-app`, and nginx (viewer SPA, CMS SPA, reverse proxy).

Local day-to-day development stays on `scripts/dev.*` with native PostgreSQL;
see [`ROOT_README.md`](../ROOT_README.md).

## Layout

| Path | Role |
| --- | --- |
| `compose.yaml` | `db` (Postgres 18) + `app` (Spring) + `web` (nginx + viewer + CMS) |
| `compose.ghcr.yaml` | CD overlay: pull `app`/`web` from private GHCR by git SHA |
| `deploy-demo.sh` | VM pull + `up --no-build` for a git SHA |
| `Dockerfile.app` | Multi-stage Gradle `bootJar` → JRE 25 |
| `Dockerfile.web` | Viewer and CMS Vite builds on Node 24 Alpine → nginx static + reverse proxy |
| `nginx/` | HTTP/HTTPS configs, CMS locations, security-header include, entrypoint; optional TLS via certbot |

Safe env template: [`config/examples/watchnest-demo.env.example`](../config/examples/watchnest-demo.env.example).

## Run (HTTP on VM public IP)

Requires Docker. From the **repository root**:

```bash
cp config/examples/watchnest-demo.env.example .env.demo
# set POSTGRES_PASSWORD
# set WATCHNEST_SERVER_NAME and WATCHNEST_FRONTEND_ORIGIN to the VM public IP
# keep WATCHNEST_SESSION_COOKIE_SECURE=false for HTTP
docker compose --env-file .env.demo -f deploy/compose.yaml up -d --build
```

Open `http://<VM_PUBLIC_IP>/`. CMS editor: `http://<VM_PUBLIC_IP>/cms/`.
Health: `http://<VM_PUBLIC_IP>/actuator/health`.
Nginx proxies only that exact health path; other `/actuator` URLs return 404.
`/cms/api/v1/...` is proxied to the app with the path unchanged (not rewritten
to `/api/v1`). `/cms` redirects to `/cms/`. Responses include
`X-Content-Type-Options: nosniff`,
`Referrer-Policy: strict-origin-when-cross-origin`,
`Content-Security-Policy: frame-ancestors 'none'`, and `X-Frame-Options: DENY`.

## CI images (GHCR)

Pushes to `dev` that pass `CI / full test suite` publish:

- `ghcr.io/nikdenalb/watchnest-app:<git-sha>`
- `ghcr.io/nikdenalb/watchnest-web:<git-sha>`

Packages are private. A green push to `dev` then SSHs to the demo VM, checks
out that SHA, and runs `deploy/deploy-demo.sh` (pull GHCR images, `up --no-build`).
`.env.demo` stays only on the host. Manual `--build` remains an emergency path.

A custom domain and HTTPS are optional later (see root `BACKLOG.md`).

## Optional: HTTPS with a domain

When a domain’s DNS `A` record points at the VM:

```bash
# set WATCHNEST_SERVER_NAME, WATCHNEST_FRONTEND_ORIGIN=https://…,
# WATCHNEST_SESSION_COOKIE_SECURE=true, WATCHNEST_ACME_EMAIL in .env.demo

docker compose --env-file .env.demo -f deploy/compose.yaml --profile certbot run --rm certbot \
  certonly --webroot -w /var/www/certbot \
  -d example.com \
  --email you@example.com \
  --agree-tos --no-eff-email

docker compose --env-file .env.demo -f deploy/compose.yaml up -d --force-recreate web
```

## Notes

- One public origin: the browser talks to nginx.
- Viewer API is same-host `/api/v1/...`.
- CMS API is same-host `/cms/api/v1/...` (path is not stripped or rewritten).
- One web image serves both SPAs: viewer at `/`, CMS dist under `/cms/`.
- Do not publish port `5432` to the internet.
