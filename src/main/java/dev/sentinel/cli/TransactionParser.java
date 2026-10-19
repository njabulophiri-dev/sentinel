package dev.sentinel.cli;

import java.math.BigDecimal;
import java.util.Locale;

public class TransactionParser {

    static Transaction parse(String line) {
        if (line == null || line.isBlank()) {
            throw new TransactionParseException("Line is null or blank");
        }

        String[] parts = line.split(",");
        if (parts.length != 4) {
            throw new TransactionParseException(
                    "Expected 4 fields, got " + parts.length + " in: " + line);
        }

        String id       = parts[0].trim().toUpperCase(Locale.ROOT);
        String merchant = parts[1].trim();
        String category = parts[2].trim();

        if (!id.matches("T\\d+"))
            throw new TransactionParseException("Invalid id: " + id);
        if (merchant.isEmpty())
            throw new TransactionParseException("Merchant is empty");
        if (category.isEmpty())
            throw new TransactionParseException("Category is empty");

        BigDecimal amount;
        try {
            amount = new BigDecimal(parts[3].trim());
        } catch (NumberFormatException e) {
            throw new TransactionParseException(
                    "Invalid amount: " + parts[3].trim(), e);
        }
        if (amount.signum() <= 0 || amount.scale() > 2)
            throw new TransactionParseException("Invalid amount: " + parts[3].trim());

        return new Transaction(id, merchant, category, amount);
    }

    public static void main(String[] args) {
        String line = "t001 , Spar, Groceries, 100.00 ";
        System.out.println(parse(line));
    }
}