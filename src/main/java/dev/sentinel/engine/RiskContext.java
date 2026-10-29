package dev.sentinel.engine;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Everything a rule needs to judge one transaction. The caller loads this data;
 * the engine itself never touches a database, which keeps it fast and easy to test.
 *
 * @param tx           the transaction being assessed
 * @param recent       earlier transactions on the same account, within the engine's lookback
 * @param knownDevices device ids this account has used before
 * @param blacklisted  which of this transaction's account, merchant or device are blacklisted
 */
public record RiskContext(
        TxFacts tx,
        List<TxFacts> recent,
        Set<String> knownDevices,
        Set<BlacklistKind> blacklisted) {

    public RiskContext {
        Objects.requireNonNull(tx, "tx");
        recent = List.copyOf(recent);
        knownDevices = Set.copyOf(knownDevices);
        blacklisted = Set.copyOf(blacklisted);
    }
}
