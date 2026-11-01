package dev.sentinel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class LoginAttemptLimiterTest {

    /** A clock the test can move forward by hand. */
    static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-10-29T10:00:00Z");

        void advance(Duration d) { now = now.plus(d); }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private final MutableClock clock = new MutableClock();
    private final LoginAttemptLimiter limiter = new LoginAttemptLimiter(3, Duration.ofMinutes(10), clock);

    private void fail(String key, int times) {
        for (int i = 0; i < times; i++) {
            limiter.recordFailure(key);
        }
    }

    @Test
    void aKeyIsNotBlockedBeforeTheLimit() {
        fail("a", 2);
        assertFalse(limiter.isBlocked("a"));
    }

    @Test
    void aKeyIsBlockedAtTheLimit() {
        fail("a", 3);
        assertTrue(limiter.isBlocked("a"));
    }

    @Test
    void anUnknownKeyIsNeverBlocked() {
        assertFalse(limiter.isBlocked("nobody"));
        assertEquals(Duration.ZERO, limiter.retryAfter("nobody"));
    }

    @Test
    void keysAreIndependent() {
        fail("a", 3);
        assertFalse(limiter.isBlocked("b"));
    }

    @Test
    void failuresExpireAfterTheWindow() {
        fail("a", 3);
        clock.advance(Duration.ofMinutes(10));
        assertFalse(limiter.isBlocked("a"));
    }

    @Test
    void retryAfterSaysHowLongToWait() {
        fail("a", 3);
        clock.advance(Duration.ofMinutes(4));
        assertEquals(Duration.ofMinutes(6), limiter.retryAfter("a"));
    }

    @Test
    void retryAfterWaitsForEnoughOldFailuresToLeaveTheWindow() {
        limiter.recordFailure("a");            // t = 0
        clock.advance(Duration.ofMinutes(3));
        fail("a", 2);                          // t = 3, 3: now blocked
        clock.advance(Duration.ofMinutes(2));  // t = 5
        // Only the first failure (t = 0) has to expire, which happens at t = 10.
        assertEquals(Duration.ofMinutes(5), limiter.retryAfter("a"));
        clock.advance(Duration.ofMinutes(5));  // t = 10
        assertFalse(limiter.isBlocked("a"));
    }

    @Test
    void resetClearsTheFailures() {
        fail("a", 3);
        limiter.reset("a");
        assertFalse(limiter.isBlocked("a"));
    }

    @Test
    void expiredKeysAreForgotten() {
        fail("a", 1);
        clock.advance(Duration.ofMinutes(11));
        assertFalse(limiter.isBlocked("a"));
        assertEquals(0, limiter.trackedKeys());
    }

    @Test
    void theNumberOfTrackedKeysIsCapped() {
        for (int i = 0; i < LoginAttemptLimiter.MAX_TRACKED_KEYS + 500; i++) {
            limiter.recordFailure("key-" + i);
        }
        assertTrue(limiter.trackedKeys() <= LoginAttemptLimiter.MAX_TRACKED_KEYS);
    }

    @Test
    void theLimitsMustMakeSense() {
        assertThrows(IllegalArgumentException.class, () -> new LoginAttemptLimiter(0, Duration.ofMinutes(1), clock));
        assertThrows(IllegalArgumentException.class, () -> new LoginAttemptLimiter(3, Duration.ZERO, clock));
    }
}
