# ADR-008: Login throttling kept in memory

## Status
Accepted

## Context
ADR-006 noted that a public deployment needs protection against password guessing. Without it, anyone can try
unlimited passwords against `/api/v1/auth/login` and every attempt also costs CPU because BCrypt is slow on purpose.

## Decision
Count failed sign-ins in memory (`LoginAttemptLimiter`) with two limits over a 10 minute window:
- 5 failures per client address and username. This stops guessing against one account from one place.
- 30 failures per client address. This stops one place from trying many usernames.

When a limit is reached the API answers `429 Too Many Requests` with a `Retry-After` header, before checking
any password. A successful sign-in clears that user's count. Throttled attempts are logged, not written to the
audit table, so an attacker cannot fill the database. The number of tracked keys is capped.

## Consequences
- No new infrastructure. It works as soon as the app starts.
- Counts are lost on restart and each running instance counts separately. With several instances an attacker
  gets the limit multiplied by the instance count. A shared store such as Redis fixes that and is on the roadmap.
- Keying on address plus username means an attacker cannot lock a real user out from a different address.
  The cost is that a distributed attack from many addresses is only slowed by the per-address limit.
- It relies on seeing the real client address. The production profile reads `X-Forwarded-For` but only from
  private-range proxies. Check on each host that the address in the log is the visitor's, not the proxy's.
