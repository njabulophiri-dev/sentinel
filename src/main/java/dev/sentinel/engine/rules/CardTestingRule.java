package dev.sentinel.engine.rules;

import dev.sentinel.engine.FraudRule;
import dev.sentinel.engine.RiskContext;
import dev.sentinel.engine.RuleHit;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/** Flags a burst of tiny payments, the usual pattern when a thief tests a stolen card. */
public final class CardTestingRule implements FraudRule {

    public static final String ID = "CARD_TESTING";

    private final int count;
    private final BigDecimal maxAmount;
    private final Duration window;
    private final int score;

    public CardTestingRule(int count, BigDecimal maxAmount, Duration window, int score) {
        if (count < 2) {
            throw new IllegalArgumentException("count must be at least 2");
        }
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be positive");
        }
        this.count = count;
        this.maxAmount = maxAmount;
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
        if (c.tx().amount().compareTo(maxAmount) > 0) {
            return Optional.empty();
        }
        Instant from = c.tx().occurredAt().minus(window);
        long small = c.recent().stream()
                .filter(t -> !t.occurredAt().isBefore(from))
                .filter(t -> t.amount().compareTo(maxAmount) <= 0)
                .count() + 1;
        if (small >= count) {
            return Optional.of(new RuleHit(ID, score, "%d payments of %s or less in %d minutes (card-testing pattern)"
                    .formatted(small, maxAmount.toPlainString(), window.toMinutes())));
        }
        return Optional.empty();
    }
}
