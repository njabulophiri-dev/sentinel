package dev.sentinel.lab;

import java.math.BigDecimal;
import java.util.List;

/** Week 1, Day 1: records, sealed types, pattern-matching switch. */
public class AmountRuleDemo {

    record Money(BigDecimal amount, String currency) {
    }

    sealed interface Decision permits Approve, Review, Decline {
    }

    record Approve() implements Decision {
    }

    record Review(List<String> reasons) implements Decision {
    }

    record Decline(List<String> reasons) implements Decision {
    }

    static String describe(Decision d) {
        return switch (d) {
            case Approve a -> "Approved";
            case Review r -> "Review: " + r.reasons();
            case Decline r -> "Decline: " + r.reasons();
        };
    }

    static Decision checkAmount(Money money) {
        BigDecimal limit = new BigDecimal("5000");
        if (money.amount().compareTo(new BigDecimal("50000")) > 0) {
            return new Decline(List.of("Amount over R50000"));
        } else if (money.amount().compareTo(limit) > 0) {
            return new Review(List.of("Amount over R5000"));
        }

        return new Approve();
    }

    public static void main(String[] args) {
        String[] amounts = {"199.99", "5000.00", "5000.01", "60000.00"};

        for (String a : amounts) {
            Money m = new Money(new BigDecimal(a), "ZAR");
            System.out.println(a + " -> " + describe(checkAmount(m)));
        }
    }
}
