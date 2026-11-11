# Sentinel

A fraud detection API for payment transactions, with an analyst dashboard.
Merchants send transactions; Sentinel scores each one in real time and answers
**APPROVE**, **REVIEW** or **DECLINE**, with a plain-English reason for every point of risk.
Risky transactions land in a queue where analysts confirm fraud or clear them.

**Stack:** Java 21, Spring Boot 3.5, PostgreSQL, Flyway, Spring Security (JWT), React + TypeScript, Docker, GitHub Actions.

**Live demo:** see [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) for how it is deployed (https://sentinel-gq67.onrender.com/).

## What it does

- **Fraud engine:** six rules (high amount, velocity, country change, blacklist, new device, card testing). Scores add up to 0-100.
- **Idempotent ingestion:** retrying a request with the same `Idempotency-Key` never creates a second transaction.
- **Explainable decisions:** every rule that fired is stored with its score and reason.
- **Analyst workflow:** an alert queue, a detail view and confirm-fraud or false-positive resolutions with notes.
- **Security:** JWT login, three roles (service, analyst, admin), BCrypt passwords, login throttling (`429` after repeated failures), CORS allow-list, append-only audit log.
- **Operations:** health probes, Prometheus metrics, a request id in every log line, graceful shutdown.

**Performance scope:** history lookups use database-side summaries and expose timing metrics but this project has not been load-tested and publishes no throughput or latency claims.

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the design and [`docs/openapi.yaml`](docs/openapi.yaml) for the API.

## Run it

You need JDK 21, Maven 3.9+, Docker and Node 22 (for the dashboard).

**Everything in containers**

```
docker compose up --build
```

One container serves both the API and the dashboard. Open http://localhost:8080 and sign in as
`analyst` / `analyst-dev-pass`.

**For development**

```
docker compose up -d postgres
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run     # API on http://localhost:8080
./scripts/demo.sh                                   # sends sample transactions
cd frontend && npm install && npm run dev           # dashboard on http://localhost:5173
```

The `dev` profile uses public, throwaway passwords and secrets. Never use it for anything real. Outside
development you must set `SENTINEL_JWT_SECRET` (32 bytes or more) and the `SENTINEL_*_PASSWORD` variables.

## Try the API

```
TOKEN=$(curl -s -X POST localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"service","password":"service-dev-pass"}' | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')

curl -s -X POST localhost:8080/api/v1/transactions \
  -H "Authorization: Bearer $TOKEN" -H 'Idempotency-Key: demo-1' -H 'Content-Type: application/json' \
  -d '{"externalId":"E1","accountId":"ACC-1","merchantId":"MER-1","amount":7500.00,
       "currency":"ZAR","country":"ZA","deviceId":"DEV-1","occurredAt":"2026-10-26T10:00:00Z"}'
```

## Tests

```
mvn test       # unit tests: the engine, the parser, the hash map. Fast, no Docker
mvn verify     # adds integration tests against a real PostgreSQL (needs Docker)
cd frontend && npm test
```

## Project layout

```
src/main/java/dev/sentinel/
  engine/       fraud rules and scoring (plain Java, no framework)
  persistence/  JPA entities and repositories
  service/      use cases: ingestion, alerts, blacklist, login, audit
  api/          controllers, request and response shapes, error handling
  config/       security, properties, wiring
  cli/          the CSV transaction tool from week 1
  lab/          learning exercises (a hand-written HashMap)
src/main/resources/db/migration/   Flyway migrations
frontend/                          React dashboard (bundled into the jar by the Dockerfile)
docs/                              architecture, ADRs, OpenAPI, deployment guide, bug journal, walkthrough
render.yaml                        deployment blueprint for Render
```

## Deploy it

[`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) has the steps, the environment variables and a smoke test
(`scripts/smoke.sh`) that checks a deployment end to end. The production profile (`prod`) contains no secrets:
everything sensitive comes from the environment.

## Decisions

Architecture decision records live in [`docs/adr/`](docs/adr/). The bug journal in
[`docs/bug-journal.md`](docs/bug-journal.md) records what went wrong while building and what it taught.
[`docs/WALKTHROUGH.md`](docs/WALKTHROUGH.md) is a guided reading order with questions for each part.

## Roadmap

Not built yet: Kafka for asynchronous processing, Redis for hot counters,
webhooks with the outbox pattern, refresh tokens, load tests and a Grafana dashboard.
