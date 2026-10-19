package dev.sentinel.cli;

public class TransactionParseException extends RuntimeException {
    public TransactionParseException(String message) {
        super(message);
    }

    public TransactionParseException(String message, Throwable cause) {
        super(message, cause);
    }
}