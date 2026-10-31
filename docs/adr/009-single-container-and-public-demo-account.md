# ADR-009: One container serves the API and the dashboard, with a public demo account

## Status
Accepted

## Context
Running the dashboard as a separate service (nginx or a static host) means a second deployment, cross-origin
requests to configure and one more thing to keep alive. A portfolio demo needs to be one link that works.

## Decision
The Docker build compiles the React dashboard and bundles it into the Spring Boot jar as static files, so a single
container serves both at one origin. For a hosted demo, `sentinel.demo.seed=true` loads a small set of sample
transactions into an empty database, produced by the real engine and the analyst account's password is published.

## Consequences
- One service plus one database to deploy. No CORS needed in production. The same image runs locally.
- The dashboard and API deploy together. A frontend-only change rebuilds the whole image.
- A published analyst password means any visitor can resolve alerts in the demo. That is acceptable because the data
  is sample data and the account cannot ingest transactions or manage the blacklist. Never publish admin or service
  credentials and never point the demo at real data.
- Sample data is only loaded into an empty database, so restarts do not duplicate it. Resetting the demo means
  clearing the database.
