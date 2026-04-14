#!/usr/bin/env bash
# Compare Java vs Go /api/v1/health JSON shape (code=200, data.ok=true).
# Usage:
#   JAVA_URL=http://127.0.0.1:8080 GO_URL=http://127.0.0.1:8081 ./scripts/parity/compare.sh
set -euo pipefail
JAVA_URL="${JAVA_URL:-http://127.0.0.1:8080}"
GO_URL="${GO_URL:-http://127.0.0.1:8081}"
TOKEN="${AIGC_ACCESS_TOKEN:-dev-local-token}"

jq_expr='.code == 200 and .data.ok == true'

j="$(curl -fsS "${JAVA_URL}/api/v1/health")"
g="$(curl -fsS "${GO_URL}/api/v1/health")"

echo "Java:  $j"
echo "Go:    $g"

echo "$j" | jq -e "$jq_expr" >/dev/null
echo "$g" | jq -e "$jq_expr" >/dev/null

echo "parity: health check OK (both match jq filter)"
