#!/usr/bin/env bash
# Sends a few sample transactions to a locally running Sentinel (dev profile) and lists the alerts.
# Usage: ./scripts/demo.sh [base-url]
set -euo pipefail

BASE="${1:-http://localhost:8080}"

token() {
  curl -fsS -X POST "$BASE/api/v1/auth/login" -H 'Content-Type: application/json' \
    -d "{\"username\":\"$1\",\"password\":\"$2\"}" | sed -E 's/.*"accessToken":"([^"]+)".*/\1/'
}

SERVICE_TOKEN=$(token service service-dev-pass)
ANALYST_TOKEN=$(token analyst analyst-dev-pass)
RUN=$(date +%s)
NOW=$(date -u +%Y-%m-%dT%H:%M:%SZ)

send() { # label account amount country
  echo "--- $1"
  curl -sS -X POST "$BASE/api/v1/transactions" \
    -H "Authorization: Bearer $SERVICE_TOKEN" \
    -H "Idempotency-Key: demo-$RUN-$1" \
    -H 'Content-Type: application/json' \
    -d "{\"externalId\":\"EXT-$RUN-$1\",\"accountId\":\"$2\",\"merchantId\":\"MER-DEMO\",\"amount\":$3,\"currency\":\"ZAR\",\"country\":\"$4\",\"deviceId\":\"DEV-1\",\"occurredAt\":\"$NOW\"}"
  echo
}

send plain    "ACC-$RUN-A" 149.99   ZA
send large    "ACC-$RUN-B" 7500.00  ZA
send huge     "ACC-$RUN-C" 60000.00 ZA

echo "--- same key again (should be a replay of the 'large' result)"
curl -sS -i -X POST "$BASE/api/v1/transactions" \
  -H "Authorization: Bearer $SERVICE_TOKEN" \
  -H "Idempotency-Key: demo-$RUN-large" \
  -H 'Content-Type: application/json' \
  -d "{\"externalId\":\"EXT-$RUN-large\",\"accountId\":\"ACC-$RUN-B\",\"merchantId\":\"MER-DEMO\",\"amount\":7500.00,\"currency\":\"ZAR\",\"country\":\"ZA\",\"deviceId\":\"DEV-1\",\"occurredAt\":\"$NOW\"}" \
  | grep -iE '^(HTTP|idempotent-replay)'

echo "--- open alerts"
curl -sS "$BASE/api/v1/alerts?status=OPEN&size=5" -H "Authorization: Bearer $ANALYST_TOKEN"
echo
