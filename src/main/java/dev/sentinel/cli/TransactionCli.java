package dev.sentinel.cli;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;

public class TransactionCli {

    public static void main(String[] args) throws IOException {
        List<String> lines = Files.readAllLines(Path.of("data/transactions.csv"));

        List<Transaction> valid = new ArrayList<>();
        int rejected = 0;

        for (int i = 0; i < lines.size(); i++) {
            try {
                valid.add(TransactionParser.parse(lines.get(i)));
            } catch (TransactionParseException e) {
                rejected++;
                System.out.println("Line " + (i + 1) + " rejected: " + e.getMessage());
            }
        }

        System.out.println();
        System.out.println("Transactions valid: " + valid.size());
        System.out.println("Transactions rejected: " + rejected);
        System.out.println();

        SortedMap<String, BigDecimal> totals = TotalsReport.totalsByCategory(valid);

        // Cross-check: sum the map's values and sum the transactions directly.
        // If they disagree, a category was dropped or double-counted. Fail loud.
        BigDecimal fromTotals = totals.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal fromTransactions = valid.stream()
                .map(Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (fromTotals.compareTo(fromTransactions) != 0) {
            throw new IllegalStateException(
                    "Grand total mismatch: map=%s, transactions=%s"
                            .formatted(fromTotals, fromTransactions));
        }

        for (Map.Entry<String, BigDecimal> entry : totals.entrySet()) {
            System.out.printf(Locale.ROOT, "%-15s %10.2f%n",
                    entry.getKey(), entry.getValue());
        }

        System.out.printf(Locale.ROOT, "%-15s %10.2f%n", "Total", fromTotals);
    }
}
