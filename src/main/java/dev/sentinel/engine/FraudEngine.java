package dev.sentinel.engine;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Runs every rule, adds up the scores (capped at 100) and turns the total into a decision.
 *
 * <p>If a rule throws, the exception propagates and the request fails. That is deliberate:
 * a payments system should fail loudly rather than quietly approve what it could not check.
 */
public final class FraudEngine {

    private final List<FraudRule> rules;
    private final int reviewThreshold;
    private final int declineThreshold;
    private final Duration lookback;

    public FraudEngine(List<FraudRule> rules, int reviewThreshold, int declineThreshold) {
        if (rules == null || rules.isEmpty()) {
            throw new IllegalArgumentException("At least one rule is required");
        }
        if (reviewThreshold <= 0 || reviewThreshold >= declineThreshold || declineThreshold > 100) {
            throw new IllegalArgumentException("Thresholds must satisfy 0 < review < decline <= 100");
        }
        Set<String> seen = new HashSet<>();
        for (FraudRule rule : rules) {
            if (!seen.add(rule.id())) {
                throw new IllegalArgumentException("Duplicate rule id: " + rule.id());
            }
        }
        this.rules = List.copyOf(rules);
        this.reviewThreshold = reviewThreshold;
        this.declineThreshold = declineThreshold;
        this.lookback = rules.stream()
                .map(FraudRule::lookback)
                .max(Comparator.naturalOrder())
                .orElse(Duration.ZERO);
    }

    /** How much history the caller must load before calling {@link #assess}. */
    public Duration lookback() {
        return lookback;
    }

    public Assessment assess(RiskContext context) {
        List<RuleHit> hits = new ArrayList<>();
        int total = 0;
        for (FraudRule rule : rules) {
            Optional<RuleHit> hit = rule.evaluate(context);
            if (hit.isPresent()) {
                hits.add(hit.get());
                total += hit.get().score();
            }
        }
        int score = Math.min(100, total);
        Decision decision = score >= declineThreshold ? Decision.DECLINE
                : score >= reviewThreshold ? Decision.REVIEW
                : Decision.APPROVE;
        return new Assessment(decision, score, hits);
    }
}
