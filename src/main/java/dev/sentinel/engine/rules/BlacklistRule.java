package dev.sentinel.engine.rules;

import dev.sentinel.engine.BlacklistKind;
import dev.sentinel.engine.FraudRule;
import dev.sentinel.engine.RiskContext;
import dev.sentinel.engine.RuleHit;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;

/** Flags a transaction whose account, merchant or device is on the blacklist. */
public final class BlacklistRule implements FraudRule {

    public static final String ID = "BLACKLIST";

    private final int score;

    public BlacklistRule(int score) {
        this.score = score;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Optional<RuleHit> evaluate(RiskContext c) {
        if (c.blacklisted().isEmpty()) {
            return Optional.empty();
        }
        String what = c.blacklisted().stream()
                .sorted(Comparator.comparing(BlacklistKind::name))
                .map(k -> k.name().toLowerCase())
                .collect(Collectors.joining(", "));
        return Optional.of(new RuleHit(ID, score, "Blacklisted " + what));
    }
}
