package dev.sentinel.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import org.junit.jupiter.api.Test;

class TotalsReportTest {

    private static Transaction tx(String id, String category, String amount) {
        return new Transaction(id, "Test merchant", category, new BigDecimal(amount));
    }

    @Test
    void sumsAmountsPerCategory() {
        List<Transaction> txs = List.of(
                tx("T001", "Groceries", "249.99"),
                tx("T003", "Groceries", "412.00"),
                tx("T002", "Transport", "89.50"));

        SortedMap<String, BigDecimal> totals = TotalsReport.totalsByCategory(txs);

        assertEquals(new BigDecimal("661.99"), totals.get("Groceries"));
        assertEquals(new BigDecimal("89.50"), totals.get("Transport"));
    }

    @Test
    void emptyListGivesEmptyMap() {
        List<Transaction> txs = List.of();
        SortedMap<String, BigDecimal> totals = TotalsReport.totalsByCategory(txs);
        assertTrue(totals.isEmpty());
    }

    @Test
    void singleTransactionGivesOneEntry() {
        List<Transaction> txs = List.of(tx("T001", "Groceries", "249.99"));

        SortedMap<String, BigDecimal> totals = TotalsReport.totalsByCategory(txs);

        assertEquals(1, totals.size());
        assertEquals(new BigDecimal("249.99"), totals.get("Groceries"));
    }

    @Test
    void categoriesAreInAlphabeticalOrder() {
        List<Transaction> txs = List.of(
                tx("T001", "Transport",     "89.50"),
                tx("T002", "Groceries",     "412.00"),
                tx("T003", "Entertainment", "199.00"));

        SortedMap<String, BigDecimal> totals = TotalsReport.totalsByCategory(txs);

        assertEquals(List.of("Entertainment", "Groceries", "Transport"),
                new ArrayList<>(totals.keySet()));
    }

    @Test
    void totalsMatchTheSampleFile() {
        List<Transaction> txs = List.of(
                tx("T001", "Groceries",     "249.99"),
                tx("T002", "Transport",     "89.50"),
                tx("T003", "Groceries",     "412.00"),
                tx("T004", "Entertainment", "199.00"),
                tx("T005", "Transport",     "850.00"),
                tx("T008", "Transport",     "120.00"),
                tx("T010", "Health",        "310.40"));

        SortedMap<String, BigDecimal> totals = TotalsReport.totalsByCategory(txs);

        assertEquals(new BigDecimal("661.99"),  totals.get("Groceries"));
        assertEquals(new BigDecimal("1059.50"), totals.get("Transport"));
        assertEquals(new BigDecimal("199.00"),  totals.get("Entertainment"));
        assertEquals(new BigDecimal("310.40"),  totals.get("Health"));
        assertEquals(4, totals.size());
    }

    @Test
    void doesNotChangeTheInputList() {
        List<Transaction> txs = List.of(
                tx("T001", "Groceries", "249.99"),
                tx("T002", "Transport", "89.50"));

        assertDoesNotThrow(() -> TotalsReport.totalsByCategory(txs));
    }


}