package dev.sentinel.engine.rules;

import dev.sentinel.engine.FraudRule;
import dev.sentinel.engine.RiskContext;
import dev.sentinel.engine.RuleHit;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Flags a payment above a threshold from a device the account has never used.
 * An account with no device history is skipped, otherwise every first payment would look suspicious.
 */
public final class NewDeviceRule implements FraudRule {

    public static final String ID = "NEW_DEVICE";

    private final BigDecimal minAmount;
    private final int score;

    public NewDeviceRule(BigDecimal minAmount, int score) {
        this.minAmount = minAmount;
        this.score = score;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Optional<RuleHit> evaluate(RiskContext c) {
        String device = c.tx().deviceId();
        if (device == null || c.knownDevices().isEmpty() || c.knownDevices().contains(device)) {
            return Optional.empty();
        }
        if (c.tx().amount().compareTo(minAmount) <= 0) {
            return Optional.empty();
        }
        return Optional.of(new RuleHit(ID, score, "First payment above %s from a new device"
                .formatted(minAmount.toPlainString())));
    }
}
