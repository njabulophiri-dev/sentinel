package dev.sentinel.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
    }
}
