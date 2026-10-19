# Sentinel

A real-time fraud detection API, built as part of **10 Weeks of Web Development**.

Merchants send transactions; Sentinel scores them and returns APPROVE, REVIEW or
DECLINE with human-readable reasons.

## Status
Week 1: foundations. See `docs/ROADMAP.md`.

## Prerequisites
JDK 21, Maven 3.9+, Docker.

## Run locally
```
docker compose up -d        # PostgreSQL
mvn spring-boot:run         # the API (health: http://localhost:8080/actuator/health)
```

## Run the Day 1 demo
```
mvn -q compile
java -cp target/classes dev.sentinel.lab.AmountRuleDemo
```

## Run the tests
```
mvn test
```
`MyHashMapTest` starts red on purpose: that is your Day 2 assignment.

## Architecture decisions
See `docs/adr/`.
