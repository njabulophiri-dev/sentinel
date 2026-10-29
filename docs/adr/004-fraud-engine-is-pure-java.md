# ADR-004: The fraud engine is plain Java with no framework or database access

## Status
Accepted

## Context
The rules are the part of Sentinel that changes most often and matters most. If they query the database
themselves, every rule needs a database to test, the number of queries per request is hard to see, and a
slow rule can quietly slow every payment.

## Decision
`FraudEngine` and its rules live in the `engine` package and receive everything they need in a
`RiskContext` (the transaction, recent history, known devices, blacklist matches). The caller loads that
data with one bounded set of queries. The engine reports how far back it needs to look (`lookback()`).

## Consequences
- Rules are tested with plain unit tests: build a context, call `evaluate`, check the hit.
- The number of database queries per transaction is fixed and visible in one place (`IngestionService`).
- Adding a rule that needs new data means extending `RiskContext` and the loading code, which is a deliberate extra step.
- History is loaded for the longest rule window even if a rule needs less. That is a small, bounded cost.
