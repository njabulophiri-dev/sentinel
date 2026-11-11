# ADR-008: Shared login throttling with account-safe recovery

## Status
Accepted

## Context
ADR-006 noted that a public deployment needs protection against password guessing. Without it, anyone can try
unlimited passwords against `/api/v1/auth/login` and every attempt also costs CPU because BCrypt is slow on purpose.
Per-process counters are insufficient because attempts can be distributed across application instances or source
addresses.

## Decision
Persist hashed throttle keys in PostgreSQL so counters are shared across application instances:
- 5 failures per client address and username in a 10 minute window.
- 30 failures per client address in a 10 minute window.
- 30 failures for a username across all addresses in a 10 minute window.

IP and address-plus-username limits reject requests before password hashing. The account-wide limit returns
`429 Too Many Requests` for invalid credentials, but a correct password is still accepted and clears the
account's counter. This avoids an attacker permanently locking out a legitimate user while making distributed
guessing against one username visible and rate-limited at the API boundary. It does not reduce BCrypt work for
distributed requests against a targeted account; stronger protection requires step-up authentication or a
managed identity provider.

Throttle keys are SHA-256 hashed before storage. Expired rows are periodically deleted. Throttled attempts are
logged, not written to the audit table, so an attacker cannot fill the audit table.

## Consequences
- PostgreSQL is already required by the application, so no additional service is needed.
- Counters survive restarts and are shared between instances.
- A correct password remains usable during an account-wide throttle; per-address limits and production edge
  protections remain necessary to limit password-hash resource consumption.
- It relies on seeing the real client address. The production profile reads `X-Forwarded-For` but only from
  private-range proxies. Check on each host that the address in the log is the visitor's, not the proxy's.
