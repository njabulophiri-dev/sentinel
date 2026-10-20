package dev.sentinel.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class TransactionParserTest {

    // ── Positive ──────────────────────────────────────────────────

    @Test
    void parsesAValidLine() {
        Transaction t = TransactionParser.parse("T001,Checkers,Groceries,249.99");
        assertEquals("T001", t.id());
        assertEquals("Checkers", t.merchant());
        assertEquals("Groceries", t.category());
        assertEquals(new BigDecimal("249.99"), t.amount());
    }

    @Test
    void lowercaseIdIsUppercased() {
        Transaction t = TransactionParser.parse("t12,Spar,Groceries,10.00");
        assertEquals("T12", t.id());
    }

    @Test
    void trimsWhitespace() {
        Transaction t = TransactionParser.parse(" T007 , Spar , Groceries , 100.00 ");
        assertEquals("T007", t.id());
        assertEquals("Spar", t.merchant());
        assertEquals("Groceries", t.category());
        assertEquals(new BigDecimal("100.00"), t.amount());
    }

    // ── Line-level rejections ─────────────────────────────────────

    @Test
    void rejectsNullLine() {
        TransactionParseException e = assertThrows(TransactionParseException.class,
                () -> TransactionParser.parse(null));
        assertTrue(e.getMessage().contains("blank"));
    }

    @Test
    void rejectsBlankLine() {
        TransactionParseException e = assertThrows(TransactionParseException.class,
                () -> TransactionParser.parse(" "));
        assertTrue(e.getMessage().contains("blank"));
    }

    @Test
    void rejectsTooFewFields() {
        TransactionParseException e = assertThrows(TransactionParseException.class,
                () -> TransactionParser.parse("T002,Woolworths,Groceries"));
        assertTrue(e.getMessage().contains("fields"));
    }

    @Test
    void rejectsTrailingCommaAfterValidAmount() {
        TransactionParseException e = assertThrows(TransactionParseException.class,
                () -> TransactionParser.parse("T011,Spar,Groceries,10.00,"));
        assertTrue(e.getMessage().contains("fields"));
    }

    // ── Field-level rejections ────────────────────────────────────

    @Test
    void rejectsInvalidId() {
        TransactionParseException e = assertThrows(TransactionParseException.class,
                () -> TransactionParser.parse("X001,Spar,Groceries,10.00"));
        assertTrue(e.getMessage().contains("Invalid id"));
    }

    @Test
    void rejectsIdWithNoDigits() {
        TransactionParseException e = assertThrows(TransactionParseException.class,
                () -> TransactionParser.parse("T,Spar,Groceries,10.00"));
        assertTrue(e.getMessage().contains("Invalid id"));
    }

    @Test
    void rejectsEmptyMerchant() {
        TransactionParseException e = assertThrows(TransactionParseException.class,
                () -> TransactionParser.parse("T009,,Groceries,50.00"));
        assertTrue(e.getMessage().contains("Merchant"));
    }

    @Test
    void rejectsEmptyCategory() {
        TransactionParseException e = assertThrows(TransactionParseException.class,
                () -> TransactionParser.parse("T012,Spar,,10.00"));
        assertTrue(e.getMessage().contains("Category"));
    }

    // ── Amount rejections ─────────────────────────────────────────

    @Test
    void rejectsNonNumericAmount() {
        TransactionParseException e = assertThrows(TransactionParseException.class,
                () -> TransactionParser.parse("T003,Pick n Pay,Groceries,abc"));
        assertTrue(e.getMessage().contains("Invalid amount"));
    }

    @Test
    void rejectsNegativeAmount() {
        TransactionParseException e = assertThrows(TransactionParseException.class,
                () -> TransactionParser.parse("T013,Spar,Groceries,-5.00"));
        assertTrue(e.getMessage().contains("positive"));
    }

    @Test
    void rejectsZeroAmount() {
        TransactionParseException e = assertThrows(TransactionParseException.class,
                () -> TransactionParser.parse("T014,Spar,Groceries,0"));
        assertTrue(e.getMessage().contains("positive"));
    }

    @Test
    void rejectsTooManyDecimals() {
        TransactionParseException e = assertThrows(TransactionParseException.class,
                () -> TransactionParser.parse("T004,Spar,Groceries,10.123"));
        assertTrue(e.getMessage().contains("decimal places"));
    }

    @Test
    void rejectsTrailingCommaAsEmptyAmount() {
        TransactionParseException e = assertThrows(TransactionParseException.class,
                () -> TransactionParser.parse("T010,Spar,Groceries,"));
        assertTrue(e.getMessage().toLowerCase(Locale.ROOT).contains("amount"),
                "Expected an amount-related error, got: " + e.getMessage());
    }
}