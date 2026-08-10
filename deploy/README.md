# Demo deploy (Docker Compose)

Docker Compose stack for a public demo on a Yandex Cloud VM:
Postgres 18, `planner-app`, and nginx (SPA + reverse proxy).

Local day-to-day development stays on `scripts/dev.*` with native PostgreSQL;
see [`ROOT_README.md`](../ROOT_README.md).

## Layout

| Path | Role |
| --- | --- |
| `compose.yaml` | `db` (Postgres 18) + `app` (Spring) + `web` (nginx + SPA) |
| `Dockerfile.app` | Multi-stage Gradle `bootJar` → JRE 25 |
| `Dockerfile.web` | Vite build → nginx static + reverse proxy |
| `nginx/` | HTTP/HTTPS configs + entrypoint; optional TLS via certbot |

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

Open `http://<VM_PUBLIC_IP>/`. Health: `http://<VM_PUBLIC_IP>/actuator/health`.

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

- One public origin: the browser talks to nginx; the API is same-host `/api/v1/...`.
- Do not publish port `5432` to the internet.
