package dev.sentinel.service;

/** The few kinds of error the API reports on purpose. Anything else is a bug and becomes a 500. */
public final class ApiErrors {

    private ApiErrors() {}

    /** The request is wrong (HTTP 400). */
    public static class BadRequest extends RuntimeException {
        public BadRequest(String message) { super(message); }
    }

    /** The thing asked for does not exist (HTTP 404). */
    public static class NotFound extends RuntimeException {
        public NotFound(String message) { super(message); }
    }

    /** The request clashes with the current state (HTTP 409). */
    public static class Conflict extends RuntimeException {
        public Conflict(String message) { super(message); }
    }
}
