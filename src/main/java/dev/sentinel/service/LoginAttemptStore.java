package dev.sentinel.service;

import java.time.Duration;

public interface LoginAttemptStore {

    boolean isBlocked(String key, int maxFailures, Duration window);

    Duration retryAfter(String key);

    void recordFailure(String key, Duration window);

    void reset(String key);
}
