#!/usr/bin/env bash
# Start planner-app (8080) and frontend dev server (5173).
# Usage from repo root: ./scripts/dev.sh  or  ./gradlew dev
#
# Local full-stack uses the persistent profile (PostgreSQL).
# Copy config/examples/planner-app.env.example to .env.planner-app and set real values.

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

BACKEND_PID=""
BACKEND_LOG="$ROOT/scripts/.backend.log"
ENV_FILE="$ROOT/.env.planner-app"

load_dotenv() {
  local file="$1"
  local line key value
  while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line#"${line%%[![:space:]]*}"}"
    line="${line%"${line##*[![:space:]]}"}"
    [[ -z "$line" || "$line" == \#* ]] && continue
    [[ "$line" != *=* ]] && continue
    key="${line%%=*}"
    value="${line#*=}"
    key="${key%"${key##*[![:space:]]}"}"
    key="${key#"${key%%[![:space:]]*}"}"
    value="${value%"${value##*[![:space:]]}"}"
    value="${value#"${value%%[![:space:]]*}"}"
    if [[ "${value}" =~ ^\".*\"$ || "${value}" =~ ^\'.*\'$ ]]; then
      value="${value:1:${#value}-2}"
    fi
    export "$key=$value"
  done <"$file"
}

cleanup() {
  if [[ -n "$BACKEND_PID" ]] && kill -0 "$BACKEND_PID" 2>/dev/null; then
    echo ""
    echo "Stopping backend (PID $BACKEND_PID)..."
    kill "$BACKEND_PID" 2>/dev/null || true
    wait "$BACKEND_PID" 2>/dev/null || true
  fi
}

trap cleanup EXIT INT TERM

wait_for_backend() {
  local url="http://localhost:8080/actuator/health"
  local attempt

  echo "Waiting for backend at $url ..."
  for attempt in $(seq 1 60); do
    if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
      echo "Backend process exited early." >&2
      if [[ -f "$BACKEND_LOG" ]]; then
        echo "Last backend log lines:" >&2
        tail -n 40 "$BACKEND_LOG" >&2
      fi
      exit 1
    fi
    if curl -sf "$url" >/dev/null 2>&1; then
      echo "Backend is ready."
      return 0
    fi
    sleep 2
  done

  echo "Backend did not become ready within 120 seconds. Is PostgreSQL running?" >&2
  if [[ -f "$BACKEND_LOG" ]]; then
    echo "Last backend log lines:" >&2
    tail -n 40 "$BACKEND_LOG" >&2
  fi
  exit 1
}

if [[ -f "$ENV_FILE" ]]; then
  echo "Loading environment from .env.planner-app ..."
  load_dotenv "$ENV_FILE"
else
  echo "WARNING: .env.planner-app not found."
  echo "Copy config/examples/planner-app.env.example to .env.planner-app and set real DB values."
fi

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-persistent}"

echo "Starting planner-app on http://localhost:8080 (profile=${SPRING_PROFILES_ACTIVE}) ..."
echo "Requires local PostgreSQL when profile is persistent."
./gradlew :planner-app:bootRun >"$BACKEND_LOG" 2>&1 &
BACKEND_PID=$!

wait_for_backend

cd "$ROOT/frontend"
if [[ ! -d node_modules ]]; then
  echo "Installing frontend dependencies..."
  npm install
fi

echo "Starting frontend on http://localhost:5173 ..."
echo "Press Ctrl+C to stop both servers."
npm run dev
