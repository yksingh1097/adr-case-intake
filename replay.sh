#!/usr/bin/env bash
# Rebuilds out/ from fixtures/, documents/ and the catalog. No API key, no network for the model.
# Usage: ./replay.sh [catalog-path]     (OUT_DIR=some/dir ./replay.sh writes somewhere other than out/)
set -euo pipefail

root="$(cd "$(dirname "$0")" && pwd)"
catalog="${1:-$root/catalog/field_catalog.json}"
out="${OUT_DIR:-$root/out}"

cd "$root/backend"
./mvnw -q -DskipTests package

jar="$(find target -maxdepth 1 -name '*.jar' ! -name '*.original' | head -n 1)"
java -jar "$jar" \
  --spring.main.web-application-type=none \
  --spring.main.banner-mode=off \
  --app.pipeline.enabled=true \
  --app.llm.mode=replay \
  --app.catalog-path="$catalog" \
  --app.documents-dir="$root/documents" \
  --app.fixtures-dir="$root/fixtures" \
  --app.out-dir="$out"
