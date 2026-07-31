#!/usr/bin/env bash
# Start planner-app (8080) and frontend dev server (5173).
# Usage from repo root: ./scripts/dev.sh

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

BACKEND_PID=""
BACKEND_LOG="$ROOT/scripts/.backend.log"

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
  local url="http://localhost:8080/api/v1/dashboard"
  local attempt

  echo "Waiting for backend at $url ..."
  for attempt in $(seq 1 45); do
    if curl -sf "$url" >/dev/null 2>&1; then
      echo "Backend is ready."
      return 0
    fi
    sleep 2
  done

  echo "Backend did not become ready within 90 seconds." >&2
  if [[ -f "$BACKEND_LOG" ]]; then
    echo "Last backend log lines:" >&2
    tail -n 30 "$BACKEND_LOG" >&2
  fi
  exit 1
}

echo "Starting planner-app on http://localhost:8080 ..."
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
