package dev.sentinel.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Remembers recent failed sign-in attempts per key and says when a key has used up its allowance.
 *
 * <p>It lives in memory, so it works for a single running instance. With several instances each would
 * keep its own count (see ADR-008). The number of tracked keys is capped so a flood of made-up
 * usernames cannot use up memory.
 */
public final class LoginAttemptLimiter {

    static final int MAX_TRACKED_KEYS = 10_000;

    private final int maxFailures;
    private final Duration window;
    private final Clock clock;
    private final Map<String, Deque<Instant>> failures = new HashMap<>();

    public LoginAttemptLimiter(int maxFailures, Duration window, Clock clock) {
        if (maxFailures < 1) {
            throw new IllegalArgumentException("maxFailures must be at least 1");
        }
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be positive");
        }
        this.maxFailures = maxFailures;
        this.window = window;
        this.clock = clock;
    }

    /** True if this key has already failed {@code maxFailures} times within the window. */
    public synchronized boolean isBlocked(String key) {
        Deque<Instant> recent = recentFailures(key);
        return recent != null && recent.size() >= maxFailures;
    }

    /** How long until this key may try again. Zero if it is not blocked. */
    public synchronized Duration retryAfter(String key) {
        Deque<Instant> recent = recentFailures(key);
        if (recent == null || recent.size() < maxFailures) {
            return Duration.ZERO;
        }
        // The key is allowed again once enough old failures have left the window to get below the limit.
        int index = recent.size() - maxFailures;
        Instant blockedUntil = null;
        int i = 0;
        for (Instant failure : recent) {
            if (i++ == index) {
                blockedUntil = failure.plus(window);
                break;
            }
        }
        Duration wait = Duration.between(clock.instant(), blockedUntil);
        return wait.isNegative() ? Duration.ZERO : wait;
    }

    public synchronized void recordFailure(String key) {
        if (!failures.containsKey(key) && failures.size() >= MAX_TRACKED_KEYS) {
            makeRoom();
        }
        failures.computeIfAbsent(key, k -> new ArrayDeque<>()).addLast(clock.instant());
    }

    /** Forget a key's failures, for example after a successful sign-in. */
    public synchronized void reset(String key) {
        failures.remove(key);
    }

    synchronized int trackedKeys() {
        return failures.size();
    }

    /** Returns the failures still inside the window, or null if there are none. Cleans up as it goes. */
    private Deque<Instant> recentFailures(String key) {
        Deque<Instant> deque = failures.get(key);
        if (deque == null) {
            return null;
        }
        Instant cutoff = clock.instant().minus(window);
        while (!deque.isEmpty() && !deque.peekFirst().isAfter(cutoff)) {
            deque.removeFirst();
        }
        if (deque.isEmpty()) {
            failures.remove(key);
            return null;
        }
        return deque;
    }

    /** The map is full: drop expired keys, and if that is not enough, the key that failed least recently. */
    private void makeRoom() {
        Instant cutoff = clock.instant().minus(window);
        failures.values().removeIf(d -> d.isEmpty() || !d.peekLast().isAfter(cutoff));
        if (failures.size() < MAX_TRACKED_KEYS) {
            return;
        }
        String oldestKey = null;
        Instant oldest = null;
        Iterator<Map.Entry<String, Deque<Instant>>> it = failures.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Deque<Instant>> entry = it.next();
            Instant last = entry.getValue().peekLast();
            if (oldest == null || last.isBefore(oldest)) {
                oldest = last;
                oldestKey = entry.getKey();
            }
        }
        if (oldestKey != null) {
            failures.remove(oldestKey);
        }
    }
}
