# ADR-005: Idempotent ingestion with a unique key in the database

## Status
Accepted

## Context
Networks fail. A merchant that does not get a response will retry, and a retry must not create a second
transaction or a second alert.

## Decision
`POST /api/v1/transactions` requires an `Idempotency-Key` header. The key has a `UNIQUE` constraint on
`fraud_transaction`. Each row also stores a hash of the request body.
- Same key and same body: return the stored result with `200` and `Idempotent-Replay: true`.
- Same key and different body: `409 Conflict`.
- Two identical requests at the same instant: the database lets one insert win, the other catches the
  constraint violation and returns the winner's result.

## Consequences
- Correctness comes from the database constraint, not from an in-memory check that could race.
- Keys are stored forever for now. A retention policy is needed before volumes grow.
- The caller must generate a new key for every genuinely new transaction.
