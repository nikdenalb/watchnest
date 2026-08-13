#!/usr/bin/env bash
set -Eeuo pipefail

sha="${1:-}"
if [[ ! "$sha" =~ ^[0-9a-f]{40}$ ]]; then
  echo "usage: bash deploy/deploy-demo.sh <40-char-git-sha>" >&2
  exit 1
fi

if [[ ! -f .env.demo ]]; then
  echo "missing .env.demo in $(pwd)" >&2
  exit 1
fi

export WATCHNEST_IMAGE_TAG="$sha"

compose=(
  docker compose
  --env-file .env.demo
  -f deploy/compose.yaml
  -f deploy/compose.ghcr.yaml
)

on_error() {
  echo "deploy failed for $sha" >&2
  "${compose[@]}" ps >&2 || true
  "${compose[@]}" logs --tail 80 app web >&2 || true
}
trap 'on_error; exit 1' ERR

"${compose[@]}" config --quiet
"${compose[@]}" pull app web
"${compose[@]}" up -d --no-build --remove-orphans

health_url="http://127.0.0.1/actuator/health"
ok=0
for _ in $(seq 1 24); do
  if curl -fsS "$health_url" >/dev/null; then
    ok=1
    break
  fi
  sleep 5
done
if [[ "$ok" -ne 1 ]]; then
  echo "health check did not return HTTP 200: $health_url" >&2
  on_error
  exit 1
fi

trap - ERR

state_dir="${HOME}/.local/state/watchnest"
mkdir -p "$state_dir"
tmp="${state_dir}/deployed-sha.tmp"
echo "$sha" >"$tmp"
mv "$tmp" "${state_dir}/deployed-sha"

echo "deployed $sha"
