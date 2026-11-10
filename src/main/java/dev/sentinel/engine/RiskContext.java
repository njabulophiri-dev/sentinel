package dev.sentinel.engine;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Everything a rule needs to judge one transaction. The caller supplies history data;
 * the engine itself never touches a database, which keeps it fast and easy to test.
 *
 * @param tx           the transaction being assessed
 * @param recent       earlier transactions used by rules in in-memory tests
 * @param knownDevices device ids used by rules in in-memory tests
 * @param blacklisted  which of this transaction's account, merchant or device are blacklisted
 * @param history      database-computed signals for production ingestion, or null for in-memory evaluation
 */
public record RiskContext(
        TxFacts tx,
        List<TxFacts> recent,
        Set<String> knownDevices,
        Set<BlacklistKind> blacklisted,
        RiskHistory history) {

    public RiskContext(TxFacts tx, List<TxFacts> recent, Set<String> knownDevices,
                       Set<BlacklistKind> blacklisted) {
        this(tx, recent, knownDevices, blacklisted, null);
    }

    public RiskContext {
        Objects.requireNonNull(tx, "tx");
        recent = List.copyOf(recent);
        knownDevices = Set.copyOf(knownDevices);
        blacklisted = Set.copyOf(blacklisted);
    }
}
