package dev.sentinel.engine;

import static dev.sentinel.engine.TestData.NOW;
import static dev.sentinel.engine.TestData.context;
import static dev.sentinel.engine.TestData.minutesAgo;
import static dev.sentinel.engine.TestData.tx;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.sentinel.engine.rules.BlacklistRule;
import dev.sentinel.engine.rules.CardTestingRule;
import dev.sentinel.engine.rules.CountryChangeRule;
import dev.sentinel.engine.rules.HighAmountRule;
import dev.sentinel.engine.rules.NewDeviceRule;
import dev.sentinel.engine.rules.VelocityRule;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RulesTest {

    // ── HighAmountRule ────────────────────────────────────────────

    private final HighAmountRule highAmount =
            new HighAmountRule(new BigDecimal("5000"), new BigDecimal("50000"), 40, 80);

    @Test
    void highAmountIgnoresAnAmountExactlyOnTheLimit() {
        assertTrue(highAmount.evaluate(context(tx("A", "5000.00", "ZA", "D", NOW))).isEmpty());
    }

    @Test
    void highAmountFlagsOneCentOverTheLimit() {
        Optional<RuleHit> hit = highAmount.evaluate(context(tx("A", "5000.01", "ZA", "D", NOW)));
        assertEquals(40, hit.orElseThrow().score());
        assertTrue(hit.get().reason().contains("5000.01"));
    }

    @Test
    void highAmountScoresVeryHighAmountsHigher() {
        Optional<RuleHit> hit = highAmount.evaluate(context(tx("A", "60000.00", "ZA", "D", NOW)));
        assertEquals(80, hit.orElseThrow().score());
    }

    // ── VelocityRule ──────────────────────────────────────────────

    private final VelocityRule velocity = new VelocityRule(5, Duration.ofMinutes(10), 35);

    private static List<TxFacts> recentPayments(int count, int minutesAgo) {
        List<TxFacts> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(minutesAgo("R" + i, minutesAgo, "100.00", "ZA"));
        }
        return list;
    }

    @Test
    void velocityAllowsExactlyTheLimit() {
        // 4 earlier + this one = 5, which is the limit
        assertTrue(velocity.evaluate(context(TestData.plain(), recentPayments(4, 3))).isEmpty());
    }

    @Test
    void velocityFlagsOneOverTheLimit() {
        Optional<RuleHit> hit = velocity.evaluate(context(TestData.plain(), recentPayments(5, 3)));
        assertEquals(35, hit.orElseThrow().score());
        assertTrue(hit.get().reason().contains("6 transactions"));
    }

    @Test
    void velocityIgnoresPaymentsOutsideTheWindow() {
        assertTrue(velocity.evaluate(context(TestData.plain(), recentPayments(10, 30))).isEmpty());
    }

    // ── CountryChangeRule ─────────────────────────────────────────

    private final CountryChangeRule countryChange = new CountryChangeRule(Duration.ofMinutes(60), 45);

    @Test
    void countryChangeFlagsAnotherCountryInsideTheWindow() {
        Optional<RuleHit> hit = countryChange.evaluate(
                context(TestData.plain(), List.of(minutesAgo("R", 20, "100.00", "GB"))));
        assertEquals(45, hit.orElseThrow().score());
        assertTrue(hit.get().reason().contains("GB"));
        assertTrue(hit.get().reason().contains("ZA"));
    }

    @Test
    void countryChangeIgnoresTheSameCountry() {
        assertTrue(countryChange.evaluate(
                context(TestData.plain(), List.of(minutesAgo("R", 20, "100.00", "ZA")))).isEmpty());
    }

    @Test
    void countryChangeIgnoresOldTransactions() {
        assertTrue(countryChange.evaluate(
                context(TestData.plain(), List.of(minutesAgo("R", 120, "100.00", "GB")))).isEmpty());
    }

    // ── BlacklistRule ─────────────────────────────────────────────

    private final BlacklistRule blacklist = new BlacklistRule(100);

    @Test
    void blacklistIgnoresCleanTransactions() {
        assertTrue(blacklist.evaluate(context(TestData.plain())).isEmpty());
    }

    @Test
    void blacklistNamesWhatMatched() {
        RiskContext c = new RiskContext(TestData.plain(), List.of(), Set.of(),
                Set.of(BlacklistKind.DEVICE, BlacklistKind.ACCOUNT));
        RuleHit hit = blacklist.evaluate(c).orElseThrow();
        assertEquals(100, hit.score());
        assertEquals("Blacklisted account, device", hit.reason());
    }

    // ── NewDeviceRule ─────────────────────────────────────────────

    private final NewDeviceRule newDevice = new NewDeviceRule(new BigDecimal("1000"), 25);

    private static RiskContext withDevices(TxFacts tx, Set<String> known) {
        return new RiskContext(tx, List.of(), known, Set.of());
    }

    @Test
    void newDeviceFlagsALargePaymentFromAnUnknownDevice() {
        RiskContext c = withDevices(tx("A", "1500.00", "ZA", "DEV-NEW", NOW), Set.of("DEV-1"));
        assertEquals(25, newDevice.evaluate(c).orElseThrow().score());
    }

    @Test
    void newDeviceIgnoresAKnownDevice() {
        RiskContext c = withDevices(tx("A", "1500.00", "ZA", "DEV-1", NOW), Set.of("DEV-1"));
        assertTrue(newDevice.evaluate(c).isEmpty());
    }

    @Test
    void newDeviceIgnoresAccountsWithNoDeviceHistory() {
        RiskContext c = withDevices(tx("A", "1500.00", "ZA", "DEV-NEW", NOW), Set.of());
        assertTrue(newDevice.evaluate(c).isEmpty());
    }

    @Test
    void newDeviceIgnoresSmallPayments() {
        RiskContext c = withDevices(tx("A", "1000.00", "ZA", "DEV-NEW", NOW), Set.of("DEV-1"));
        assertTrue(newDevice.evaluate(c).isEmpty());
    }

    @Test
    void newDeviceIgnoresPaymentsWithNoDeviceId() {
        RiskContext c = withDevices(tx("A", "1500.00", "ZA", null, NOW), Set.of("DEV-1"));
        assertTrue(newDevice.evaluate(c).isEmpty());
    }

    // ── CardTestingRule ───────────────────────────────────────────

    private final CardTestingRule cardTesting =
            new CardTestingRule(4, new BigDecimal("20"), Duration.ofMinutes(10), 40);

    @Test
    void cardTestingFlagsTheFourthSmallPaymentInTheWindow() {
        TxFacts small = tx("A", "5.00", "ZA", "DEV-1", NOW);
        RuleHit hit = cardTesting.evaluate(context(small, recentPayments(3, 2, "5.00"))).orElseThrow();
        assertEquals(40, hit.score());
    }

    @Test
    void cardTestingIgnoresThreeSmallPayments() {
        TxFacts small = tx("A", "5.00", "ZA", "DEV-1", NOW);
        assertTrue(cardTesting.evaluate(context(small, recentPayments(2, 2, "5.00"))).isEmpty());
    }

    @Test
    void cardTestingIgnoresALargePaymentEvenAfterSmallOnes() {
        TxFacts large = tx("A", "500.00", "ZA", "DEV-1", NOW);
        assertTrue(cardTesting.evaluate(context(large, recentPayments(5, 2, "5.00"))).isEmpty());
    }

    private static List<TxFacts> recentPayments(int count, int minutesAgo, String amount) {
        List<TxFacts> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(minutesAgo("S" + i, minutesAgo, amount, "ZA"));
        }
        return list;
    }
}
