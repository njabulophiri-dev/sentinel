package dev.sentinel.cli;

import java.math.BigDecimal;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class TotalsReport {

    static SortedMap<String, BigDecimal> totalsByCategory(List<Transaction> transactions) {
        SortedMap<String, BigDecimal> totals = new TreeMap<>();
        for (Transaction tx : transactions) {
            totals.merge(tx.category(), tx.amount(), BigDecimal::add);
        }
        return totals;
    }
}