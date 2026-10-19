# ADR-002: Represent money with BigDecimal

## Status
Accepted

## Context
Sentinel needs exact currency values. double can't represent decimals exactly e.g 0.1 + 0.2 gives 0.30000000000000004 and the error accumulates when summing transactions. double also can't enforce a 2-decimal (cents) scale.

## Decision
BigDecimal for all currency representations.

## Consequences
- Exact decimal arithmetic: 0.1 + 0.2 is 0.3, sums don't drift, and scale can be validated (e.g. reject 249.999).
- More verbose code: no operators, so a.add(b) instead of a + b. Division needs an explicit RoundingMode and comparison needs compareTo() rather than equals().