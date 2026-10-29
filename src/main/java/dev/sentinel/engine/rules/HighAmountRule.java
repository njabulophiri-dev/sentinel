package dev.sentinel.engine.rules;

import dev.sentinel.engine.FraudRule;
import dev.sentinel.engine.RiskContext;
import dev.sentinel.engine.RuleHit;
import java.math.BigDecimal;
import java.util.Optional;

/** Flags large amounts: "high" is usually a review, "very high" is a decline. */
public final class HighAmountRule implements FraudRule {

    public static final String ID = "HIGH_AMOUNT";

    private final BigDecimal high;
    private final BigDecimal veryHigh;
    private final int highScore;
    private final int veryHighScore;

    public HighAmountRule(BigDecimal high, BigDecimal veryHigh, int highScore, int veryHighScore) {
        if (high.compareTo(veryHigh) > 0) {
            throw new IllegalArgumentException("high must not exceed veryHigh");
        }
        this.high = high;
        this.veryHigh = veryHigh;
        this.highScore = highScore;
        this.veryHighScore = veryHighScore;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Optional<RuleHit> evaluate(RiskContext c) {
        BigDecimal amount = c.tx().amount();
        String currency = c.tx().currency();
        if (amount.compareTo(veryHigh) > 0) {
            return Optional.of(new RuleHit(ID, veryHighScore, "Amount %s %s is above the very-high limit of %s"
                    .formatted(amount.toPlainString(), currency, veryHigh.toPlainString())));
        }
        if (amount.compareTo(high) > 0) {
            return Optional.of(new RuleHit(ID, highScore, "Amount %s %s is above the limit of %s"
                    .formatted(amount.toPlainString(), currency, high.toPlainString())));
        }
        return Optional.empty();
    }
}
