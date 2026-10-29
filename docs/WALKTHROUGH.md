# Owning this codebase: a guided walkthrough

You can only defend code you understand. This guide is the order to read it in, what to try at each step,
and the questions an interviewer is likely to ask. Budget about a day for the first pass.

## How to work through it

1. Read the file named in each step before you run anything.
2. Predict what the code does, then check by running it or reading the test.
3. Break something on purpose and watch which test fails (the same habit as your bug journal).
4. Write the answer to each question in your own words, out loud, as if to a non-programmer.

## Step 1: The engine (about 90 minutes)

Read in this order: `engine/TxFacts`, `RiskContext`, `FraudRule`, `rules/HighAmountRule`, `FraudEngine`.
Then read `RulesTest` and `FraudEngineTest` and run them: `mvn test -Dtest='RulesTest,FraudEngineTest'`.

Try:
- Change `>` to `>=` in `HighAmountRule` and see which test fails. Which business question does that boundary answer?
- Write a seventh rule of your own (for example, a payment between 02:00 and 04:00) with its tests first.

Be ready to answer:
- Why does the engine receive its data instead of querying for it? (ADR-004)
- Why are scores added and capped, instead of taking the highest one?
- What happens if a rule throws? Why is that the right default for payments? (ADR-007)
- Why are the thresholds 40 and 80, and where would you change them?

## Step 2: Ingestion and idempotency (about 90 minutes)

Read `service/IngestionService`, `RequestHasher`, then the migration `V1__core_schema.sql`.

Try:
- Draw the sequence for a retry that arrives while the first request is still running.
- Find the line that makes the database, not Java, the final judge of "same key".

Be ready to answer:
- What does idempotent mean, and why does a payments API need it? (ADR-005)
- Two identical requests arrive at the same instant. Walk through what happens.
- Why is the request hashed? What goes wrong without it?
- Why `BigDecimal` and `NUMERIC(19,2)` for money? (ADR-002)
- Why is there no card number column?

## Step 3: Security (about 60 minutes)

Read `config/SecurityConfig`, `service/AuthService`, then the role tests at the top of `SentinelApiIT`.

Try:
- Change a role rule in `SecurityConfig` and see which integration test fails.
- Decode a token at jwt.io (use a dev token only) and find the `role` claim.

Be ready to answer:
- Why is CSRF protection switched off here, and when would that be wrong?
- Why do tokens expire after 15 minutes? What can't you do with a JWT? (ADR-006)
- Why does login compare against a dummy hash when the user does not exist?
- How would you add refresh tokens, and what would you rate-limit first?

## Step 4: The API surface (about 45 minutes)

Read `api/ApiModels`, `TransactionController`, `ApiExceptionHandler`, `docs/openapi.yaml`.

Be ready to answer:
- Why separate request and response records from the database entities?
- What is RFC 7807 and what does a client gain from it?
- Why are page sizes capped?
- What does `Idempotent-Replay` tell a client?

## Step 5: Analyst workflow and concurrency (about 45 minutes)

Read `service/AlertService`, `persistence/AlertEntity` (look at `@Version`), `AlertList.tsx`, `AlertDetail.tsx`.

Be ready to answer:
- Two analysts open the same alert and both click "Confirm fraud". What happens, and why?
- Why does `AlertService.list` load transactions and reasons in two extra queries instead of one per row? (the N+1 problem)
- Why does the dashboard ignore a response that arrives after you change the filter?

## Step 6: Run it end to end

```
docker compose up -d postgres
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
./scripts/demo.sh            # in a second terminal
cd frontend && npm install && npm run dev
```

Sign in at http://localhost:5173 as `analyst` / `analyst-dev-pass`, open an alert, and read its reasons.

## Three-minute explanation to practise

"Sentinel scores payments for fraud as they arrive. A merchant sends a transaction with a unique key.
Six small rules each add points for something suspicious, like a very large amount or two countries in an hour.
The total decides approve, review or decline, and every point comes with a plain-English reason.
Anything risky lands in a queue where an analyst confirms fraud or clears it, and every action is audited.
Retries are safe because the database enforces one result per key."

## Honest gaps to be upfront about in interviews

Kafka, Redis, webhooks, load testing and a metrics dashboard are not built. There is no login rate limiting
or refresh token yet, and rule thresholds are configured by hand rather than learned from data. Knowing
what is missing, and why, is part of owning the project.
