package dev.sentinel.engine;

import static dev.sentinel.engine.TestData.NOW;
import static dev.sentinel.engine.TestData.context;
import static dev.sentinel.engine.TestData.defaultEngine;
import static dev.sentinel.engine.TestData.tx;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.sentinel.engine.rules.BlacklistRule;
import dev.sentinel.engine.rules.HighAmountRule;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FraudEngineTest {

    @Test
    void aPlainPaymentIsApprovedWithScoreZero() {
        Assessment a = defaultEngine().assess(context(TestData.plain()));
        assertEquals(Decision.APPROVE, a.decision());
        assertEquals(0, a.score());
        assertTrue(a.hits().isEmpty());
    }

    @Test
    void aHighAmountAloneGoesToReview() {
        Assessment a = defaultEngine().assess(context(tx("A", "7500.00", "ZA", "DEV-1", NOW)));
        assertEquals(Decision.REVIEW, a.decision());
        assertEquals(40, a.score());
        assertEquals("HIGH_AMOUNT", a.hits().get(0).ruleId());
    }

    @Test
    void aVeryHighAmountIsDeclined() {
        Assessment a = defaultEngine().assess(context(tx("A", "60000.00", "ZA", "DEV-1", NOW)));
        assertEquals(Decision.DECLINE, a.decision());
        assertEquals(80, a.score());
    }

    @Test
    void aBlacklistedAccountIsDeclined() {
        RiskContext c = new RiskContext(TestData.plain(), List.of(), Set.of(), Set.of(BlacklistKind.ACCOUNT));
        Assessment a = defaultEngine().assess(c);
        assertEquals(Decision.DECLINE, a.decision());
        assertEquals(100, a.score());
    }

    @Test
    void scoresAreCappedAtOneHundred() {
        RiskContext c = new RiskContext(tx("A", "60000.00", "ZA", "DEV-1", NOW), List.of(), Set.of(),
                Set.of(BlacklistKind.MERCHANT));
        Assessment a = defaultEngine().assess(c);
        assertEquals(100, a.score());
        assertEquals(2, a.hits().size());
    }

    @Test
    void everyContributingRuleIsReportedInOrder() {
        RiskContext c = new RiskContext(tx("A", "7500.00", "ZA", "DEV-1", NOW),
                List.of(TestData.minutesAgo("R", 5, "100.00", "GB")), Set.of(), Set.of());
        Assessment a = defaultEngine().assess(c);
        assertEquals(List.of("HIGH_AMOUNT", "COUNTRY_CHANGE"), a.hits().stream().map(RuleHit::ruleId).toList());
        assertEquals(Decision.DECLINE, a.decision()); // 40 + 45 = 85
    }

    @Test
    void lookbackIsTheLongestRuleWindow() {
        assertEquals(Duration.ofMinutes(60), defaultEngine().lookback());
    }

    @Test
    void thresholdsMustBeOrdered() {
        FraudRule rule = new BlacklistRule(100);
        assertThrows(IllegalArgumentException.class, () -> new FraudEngine(List.of(rule), 80, 40));
        assertThrows(IllegalArgumentException.class, () -> new FraudEngine(List.of(rule), 0, 80));
        assertThrows(IllegalArgumentException.class, () -> new FraudEngine(List.of(rule), 40, 101));
    }

    @Test
    void ruleIdsMustBeUnique() {
        FraudRule a = new HighAmountRule(new BigDecimal("1"), new BigDecimal("2"), 10, 20);
        FraudRule b = new HighAmountRule(new BigDecimal("3"), new BigDecimal("4"), 10, 20);
        assertThrows(IllegalArgumentException.class, () -> new FraudEngine(List.of(a, b), 40, 80));
    }

    @Test
    void anEngineNeedsAtLeastOneRule() {
        assertThrows(IllegalArgumentException.class, () -> new FraudEngine(List.of(), 40, 80));
    }

    @Test
    void aFailingRulePropagatesInsteadOfSilentlyApproving() {
        FraudRule broken = new FraudRule() {
            @Override public String id() { return "BROKEN"; }
            @Override public Optional<RuleHit> evaluate(RiskContext context) {
                throw new IllegalStateException("boom");
            }
        };
        FraudEngine engine = new FraudEngine(List.of(broken), 40, 80);
        assertThrows(IllegalStateException.class, () -> engine.assess(context(TestData.plain())));
    }
}
