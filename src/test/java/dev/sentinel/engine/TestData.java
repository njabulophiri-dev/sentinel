package dev.sentinel.engine;

import dev.sentinel.engine.rules.BlacklistRule;
import dev.sentinel.engine.rules.CardTestingRule;
import dev.sentinel.engine.rules.CountryChangeRule;
import dev.sentinel.engine.rules.HighAmountRule;
import dev.sentinel.engine.rules.NewDeviceRule;
import dev.sentinel.engine.rules.VelocityRule;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/** Small builders so each test reads as a story, not as setup. */
final class TestData {

    static final Instant NOW = Instant.parse("2026-10-26T10:00:00Z");

    private TestData() {}

    static TxFacts tx(String id, String amount, String country, String device, Instant at) {
        return new TxFacts(id, "ACC-1", "MER-1", new BigDecimal(amount), "ZAR", country, device, at);
    }

    /** A plain R100 payment from South Africa, "now". */
    static TxFacts plain() {
        return tx("T-NOW", "100.00", "ZA", "DEV-1", NOW);
    }

    static TxFacts minutesAgo(String id, int minutes, String amount, String country) {
        return tx(id, amount, country, "DEV-1", NOW.minus(Duration.ofMinutes(minutes)));
    }

    static RiskContext context(TxFacts tx) {
        return new RiskContext(tx, List.of(), Set.of(), Set.of());
    }

    static RiskContext context(TxFacts tx, List<TxFacts> recent) {
        return new RiskContext(tx, recent, Set.of(), Set.of());
    }

    static FraudEngine defaultEngine() {
        return new FraudEngine(List.of(
                new HighAmountRule(new BigDecimal("5000"), new BigDecimal("50000"), 40, 80),
                new VelocityRule(5, Duration.ofMinutes(10), 35),
                new CountryChangeRule(Duration.ofMinutes(60), 45),
                new BlacklistRule(100),
                new NewDeviceRule(new BigDecimal("1000"), 25),
                new CardTestingRule(4, new BigDecimal("20"), Duration.ofMinutes(10), 40)),
                40, 80);
    }
}
