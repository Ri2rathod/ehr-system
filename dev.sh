#!/usr/bin/env bash
# Starts both development servers from the monorepo root.
set -Eeuo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
backend_pid=""
frontend_pid=""

cleanup() {
  local exit_code=$?
  trap - EXIT INT TERM

  for pid in "$backend_pid" "$frontend_pid"; do
    if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
      kill "$pid" 2>/dev/null || true
    fi
  done

  for pid in "$backend_pid" "$frontend_pid"; do
    if [[ -n "$pid" ]]; then
      wait "$pid" 2>/dev/null || true
    fi
  done

  exit "$exit_code"
}

if [[ ! -x "$project_root/backend/mvnw" ]]; then
  echo "Backend Maven wrapper not found at backend/mvnw." >&2
  exit 1
fi

if ! command -v npm >/dev/null 2>&1; then
  echo "npm is required to start the frontend." >&2
  exit 1
fi

if [[ ! -d "$project_root/frontend/node_modules" ]]; then
  echo "Frontend dependencies are not installed. Run: npm --prefix frontend ci" >&2
  exit 1
fi

trap cleanup EXIT INT TERM

(
  cd "$project_root/backend"
  exec ./mvnw spring-boot:run
) &
backend_pid=$!

(
  cd "$project_root/frontend"
  exec npm run dev
) &
frontend_pid=$!

# If either server stops or fails, stop the other one too.
wait -n "$backend_pid" "$frontend_pid"
