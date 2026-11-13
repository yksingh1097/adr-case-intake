#!/usr/bin/env bash
# Starts the backend and the UI from the committed out/ and documents/. No API key needed.
# Backend: http://localhost:8080   UI: http://localhost:5173
set -euo pipefail

root="$(cd "$(dirname "$0")" && pwd)"

if [ ! -d "$root/frontend/node_modules" ]; then
  (cd "$root/frontend" && npm ci)
fi

# stop the backend when the UI stops (kill 0 signals every process this script started)
trap 'kill 0' EXIT

(cd "$root/backend" && ./mvnw -q spring-boot:run) &
cd "$root/frontend"
npm run dev
