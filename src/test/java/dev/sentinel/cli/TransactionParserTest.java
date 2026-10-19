package dev.sentinel.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TransactionParserTest {

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
    void rejectsTooFewFields() {
        TransactionParseException e = assertThrows(TransactionParseException.class,
                () -> TransactionParser.parse("T002,Woolworths,Groceries"));
        assertTrue(e.getMessage().contains("fields"));
    }

    @Test
    void rejectsNonNumericAmount() {
        assertThrows(TransactionParseException.class,
                () -> TransactionParser.parse("T003,Pick n Pay,Groceries,abc"));
    }

    @Test
    void rejectsTooManyDecimals() {
        TransactionParseException e = assertThrows(TransactionParseException.class,
                () -> TransactionParser.parse("T004,Spar,Groceries,10.123"));
        assertTrue(e.getMessage().contains("amount"));
    }

    @Test
    void rejectsZeroAmount() {
        TransactionParseException e = assertThrows(TransactionParseException.class,
                () -> TransactionParser.parse("T005,Spar,Groceries,0"));
        assertTrue(e.getMessage().contains("amount"));
    }

    @Test
    void rejectsEmptyMerchant() {
        TransactionParseException e = assertThrows(TransactionParseException.class,
                () -> TransactionParser.parse("T009,,Groceries,50.00"));
        assertTrue(e.getMessage().contains("Merchant"));
    }
}