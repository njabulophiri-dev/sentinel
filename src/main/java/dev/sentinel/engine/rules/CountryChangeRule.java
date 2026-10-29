package dev.sentinel.engine.rules;

import dev.sentinel.engine.FraudRule;
import dev.sentinel.engine.RiskContext;
import dev.sentinel.engine.RuleHit;
import dev.sentinel.engine.TxFacts;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;

/** Flags an account that appears in two different countries within a short window ("impossible travel"). */
public final class CountryChangeRule implements FraudRule {

    public static final String ID = "COUNTRY_CHANGE";

    private final Duration window;
    private final int score;

    public CountryChangeRule(Duration window, int score) {
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be positive");
        }
        this.window = window;
        this.score = score;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Duration lookback() {
        return window;
    }

    @Override
    public Optional<RuleHit> evaluate(RiskContext c) {
        Instant from = c.tx().occurredAt().minus(window);
        Optional<TxFacts> other = c.recent().stream()
                .filter(t -> !t.occurredAt().isBefore(from))
                .filter(t -> !t.country().equals(c.tx().country()))
                .max(Comparator.comparing(TxFacts::occurredAt));
        return other.map(t -> new RuleHit(ID, score, "Account used in %s and %s within %d minutes"
                .formatted(t.country(), c.tx().country(), window.toMinutes())));
    }
}
