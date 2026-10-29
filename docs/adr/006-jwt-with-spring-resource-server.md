# ADR-006: Stateless JWT using Spring Security's resource server

## Status
Accepted

## Context
The API is called by machines (merchant systems) and by a browser dashboard. Sessions and cookies add
CSRF and scaling concerns. A hand-written token filter is easy to get subtly wrong.

## Decision
Sentinel issues short-lived (15 minute) HS256 JWTs at `/api/v1/auth/login` and validates them with
`spring-boot-starter-oauth2-resource-server`. The role is a claim and becomes a Spring authority.
Passwords are stored as BCrypt hashes. The signing secret comes from `SENTINEL_JWT_SECRET` and the app
refuses to start if it is shorter than 32 bytes. Login takes the same time whether or not the user exists.

## Consequences
- No custom security filter code to maintain or audit.
- Tokens cannot be revoked before they expire. Short lifetimes limit the damage, and refresh tokens are not implemented yet.
- A shared secret means every service that validates tokens can also forge them. Moving to an external identity provider (RS256 and a JWKS endpoint) is the next step if more services are added.
- There is no login rate limiting yet. It is on the roadmap and should be added before any public deployment.
