# ADR-007: A failing rule fails the request

## Status
Accepted

## Context
If a rule throws an exception, the engine could skip it and carry on, or it could stop. Skipping means a
bug in one rule silently turns into "approve", with no one the wiser.

## Decision
Exceptions from rules propagate. The request fails with a `500` problem response, nothing is stored, and
the caller can retry with the same Idempotency-Key.

## Consequences
- A broken rule is noticed immediately, in errors and logs, instead of showing up later as missed fraud.
- A bug in one rule makes ingestion unavailable until it is fixed. In production that would call for alerting on the error rate.
- Whether to degrade instead (for example, route to review) is a business decision to revisit with real traffic.
