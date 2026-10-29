package dev.sentinel.engine;

import java.util.Objects;

/** One rule saying "this transaction looks risky, and here is why". */
public record RuleHit(String ruleId, int score, String reason) {

    public RuleHit {
        Objects.requireNonNull(ruleId, "ruleId");
        Objects.requireNonNull(reason, "reason");
        if (score < 1 || score > 100) {
            throw new IllegalArgumentException("score must be between 1 and 100");
        }
    }
}
