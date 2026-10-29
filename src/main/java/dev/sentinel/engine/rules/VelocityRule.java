package dev.sentinel.engine.rules;

import dev.sentinel.engine.FraudRule;
import dev.sentinel.engine.RiskContext;
import dev.sentinel.engine.RuleHit;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/** Flags too many transactions on one account in a short window. */
public final class VelocityRule implements FraudRule {

    public static final String ID = "VELOCITY";

    private final int maxCount;
    private final Duration window;
    private final int score;

    public VelocityRule(int maxCount, Duration window, int score) {
        if (maxCount < 1) {
            throw new IllegalArgumentException("maxCount must be at least 1");
        }
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be positive");
        }
        this.maxCount = maxCount;
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
        long inWindow = c.recent().stream().filter(t -> !t.occurredAt().isBefore(from)).count() + 1;
        if (inWindow > maxCount) {
            return Optional.of(new RuleHit(ID, score, "%d transactions in %d minutes (limit %d)"
                    .formatted(inWindow, window.toMinutes(), maxCount)));
        }
        return Optional.empty();
    }
}
