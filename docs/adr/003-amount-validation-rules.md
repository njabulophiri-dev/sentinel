# ADR-003: Transaction amount validation rules

## Status
Accepted

## Context
The parser must reject bad amounts at the boundary, before they reach scoring or persistence. Refunds and zero-rand auth checks exist in payments but need their own lifecycle, not a signed amount on a purchase.

## Decision
TransactionParser.parse accepts an amount only if it is:
- Positive — signum() > 0 (zero and negatives rejected)
- At most 2 decimal places — scale() <= 2
- Everything else throws TransactionParseException before a Transaction is constructed.

## Consequences
- Invalid amounts can't enter the system; every Transaction holds a valid amount by construction.
- Refunds and zero-rand auths are deferred to a separate path rather than half-supported as signed amounts.
- The rule is honest about its scope: purchases only, for now.
- The 2-decimal limit assumes a two-decimal currency (ZAR, EUR, USD). Currencies with other minor-unit sizes will need the scale to come from the currency.