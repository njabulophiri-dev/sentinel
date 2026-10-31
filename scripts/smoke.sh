#!/usr/bin/env bash
# Checks that a deployed Sentinel works end to end.
# Usage: DEMO_USER=analyst DEMO_PASS='...' ./scripts/smoke.sh https://your-app.example.com
set -euo pipefail

BASE="${1:?Usage: DEMO_USER=... DEMO_PASS=... ./scripts/smoke.sh <base-url>}"
: "${DEMO_USER:?set DEMO_USER}"
: "${DEMO_PASS:?set DEMO_PASS}"

step() { printf '\n== %s\n' "$1"; }
fail() { echo "FAILED: $1" >&2; exit 1; }

step "Health (the first request may be slow if the host was asleep)"
curl -fsS --max-time 120 "$BASE/actuator/health" | grep -q '"status":"UP"' || fail "health is not UP"
echo "ok"

step "Dashboard is served"
curl -fsS "$BASE/" | grep -q 'id="root"' || fail "the dashboard page was not served"
echo "ok"

step "API is closed without a token"
code=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/v1/alerts")
[ "$code" = "401" ] || fail "expected 401 without a token, got $code"
echo "ok (401)"

step "Login"
TOKEN=$(curl -fsS -X POST "$BASE/api/v1/auth/login" -H 'Content-Type: application/json' \
  -d "{\"username\":\"$DEMO_USER\",\"password\":\"$DEMO_PASS\"}" | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')
[ -n "$TOKEN" ] || fail "no token returned"
echo "ok"

step "Open alerts"
curl -fsS "$BASE/api/v1/alerts?status=OPEN&size=1" -H "Authorization: Bearer $TOKEN" | grep -o '"totalElements":[0-9]*'

step "Metrics are not public"
code=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/actuator/prometheus")
[ "$code" = "401" ] || [ "$code" = "403" ] || fail "metrics should be protected, got $code"
echo "ok ($code)"

echo; echo "All checks passed."
