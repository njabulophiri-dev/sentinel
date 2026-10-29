package dev.sentinel.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Fingerprints a request so a retried Idempotency-Key can be told apart from a key reused for a
 * different payload. Fields are length-prefixed so ("ab","c") and ("a","bc") never collide.
 */
public final class RequestHasher {

    private RequestHasher() {}

    public static String hash(String... parts) {
        StringBuilder canonical = new StringBuilder();
        for (String part : parts) {
            String value = part == null ? "" : part;
            canonical.append(value.length()).append(':').append(value).append('|');
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available on the JVM", e);
        }
    }
}
