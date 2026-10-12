# Roadmap: where each week's code lives

| Week | Package / folder | You build |
|---|---|---|
| 1 | `dev.sentinel`, `dev.sentinel.lab` | Day1 demo, MyHashMap, transaction CLI, race-condition lab |
| 2 | `docs/` , SQL workbook | ERD, OpenAPI contract, indexed velocity queries |
| 3 | `dev.sentinel.api`, `dev.sentinel.persistence`, `db/migration` | DTOs, controllers, exception handler, entities, Flyway, idempotent ingestion |
| 4 | `dev.sentinel.engine` | FraudRule interface, 8+ rules, scorer, alerts and cases |
| 5 | `dev.sentinel.security` | JWT, RBAC, API keys, audit log, rate limiting |
| 6 | `src/test` | Unit, Testcontainers, slice and security tests |
| 7 | `dev.sentinel.messaging` | Kafka, Redis, outbox, webhooks |
| 8 | `Dockerfile`, `.github/workflows`, `ops/` | Container, CI/CD, dashboards, deploy |
| 9 | `docs/` | System design doc, C4 diagrams, final README |
| 10 | n/a | Interview and job launch |

Rule: create each package only when its week starts.
