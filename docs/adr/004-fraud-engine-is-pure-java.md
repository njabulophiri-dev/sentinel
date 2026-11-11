# ADR-004: The fraud engine is plain Java with no framework or database access

## Status
Accepted

## Context
The rules are the part of Sentinel that changes most often and matters most. If they query the database
themselves, every rule needs a database to test, the number of queries per request is hard to see, and a
slow rule can quietly slow every payment.

## Decision
`FraudEngine` and its rules live in the `engine` package and receive everything they need in a
`RiskContext` (the transaction, history signals, known-device state, blacklist matches). The caller queries that
data with a fixed set of indexed aggregates and existence checks using the configured rule windows.

## Consequences
- Rules are tested with plain unit tests: build a context, call `evaluate`, check the hit.
- The number of database queries per transaction is fixed and visible in one place (`IngestionService`).
- Adding a rule that needs new data means extending `RiskContext` and the loading code, which is a deliberate extra step.
- Fraud-history query duration is measured by `sentinel.fraud.history.lookup` for operational tuning.
