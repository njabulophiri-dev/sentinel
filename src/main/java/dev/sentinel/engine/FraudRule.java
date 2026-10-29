package dev.sentinel.engine;

import java.time.Duration;
import java.util.Optional;

/** One check. Rules are small, independent and individually testable. */
public interface FraudRule {

    /** Stable identifier, stored with every hit so analysts can see which rule fired. */
    String id();

    Optional<RuleHit> evaluate(RiskContext context);

    /** How far back in time this rule needs to see. */
    default Duration lookback() {
        return Duration.ZERO;
    }
}
