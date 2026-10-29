package dev.sentinel.engine;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/** The facts about one transaction that rules are allowed to look at. No card numbers, ever. */
public record TxFacts(
        String id,
        String accountId,
        String merchantId,
        BigDecimal amount,
        String currency,
        String country,
        String deviceId,
        Instant occurredAt) {

    public TxFacts {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(merchantId, "merchantId");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(country, "country");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
