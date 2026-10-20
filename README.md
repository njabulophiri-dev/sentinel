# Sentinel

A fraud detection API for payment transactions, built in the open as part of
**10 Weeks of Web Development**.

**Goal:** merchants send transactions; Sentinel scores them and returns
APPROVE, REVIEW or DECLINE with human-readable reasons.

## Status

Work in progress, Week 1 (Java foundations).

| Done | Coming next |
|---|---|
| Hand-written `HashMap` with chaining and resizing, 10 tests | Spend totals by category in the CLI |
| Transaction CSV parser with validation, 16 tests | Streams version of the totals |
| CLI that reads a file and reports rejected lines | Spring Boot REST API, PostgreSQL (Weeks 3-4) |
| Spring Boot and PostgreSQL scaffold | Rule engine, security, deployment (Weeks 4-8) |

The full plan is in [`docs/ROADMAP.md`](docs/ROADMAP.md).

## Prerequisites

- JDK 21
- Maven 3.9+
- Docker (only needed to run the API and database)

## Run the tests

```
mvn clean test
```

## Run the transaction CLI

From the project root (the CLI reads `data/transactions.csv` using a relative path):

```
mvn -q compile
java -cp target/classes dev.sentinel.cli.TransactionCli
```

Each line is `id,merchant,category,amount`, for example `T001,Checkers,Groceries,249.99`.
Invalid lines are rejected with a reason and the line number and the run continues.

Known limitation: fields cannot contain commas.

## Run the API

```
docker compose up -d        # PostgreSQL
mvn spring-boot:run         # health check: http://localhost:8080/actuator/health
```

The API has no business endpoints yet.

## Project layout

```
src/main/java/dev/sentinel/
  cli/    transaction parsing and the command-line tool
  lab/    learning exercises: MyHashMap, amount-rule demo
docs/
  adr/            architecture decision records
  bug-journal.md  bugs hit while building, and what they taught
  ROADMAP.md      what gets built in which week
data/             sample transactions
```

## Architecture decisions

See [`docs/adr/`](docs/adr/).