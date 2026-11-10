package dev.sentinel.service;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcLoginAttemptStore implements LoginAttemptStore {

    private static final long CLEANUP_INTERVAL_MILLIS = Duration.ofMinutes(1).toMillis();

    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final AtomicLong nextCleanup = new AtomicLong();

    public JdbcLoginAttemptStore(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Override
    public boolean isBlocked(String key, int maxFailures, Duration window) {
        List<Integer> counts = jdbc.query(
                "select failures from login_attempt where key_hash = ? and expires_at > ?",
                (rs, row) -> rs.getInt(1), hash(key), Timestamp.from(clock.instant()));
        return !counts.isEmpty() && counts.get(0) >= maxFailures;
    }

    @Override
    public Duration retryAfter(String key) {
        List<Instant> expirations = jdbc.query(
                "select expires_at from login_attempt where key_hash = ?",
                (rs, row) -> rs.getTimestamp(1).toInstant(), hash(key));
        if (expirations.isEmpty()) {
            return Duration.ZERO;
        }
        Duration remaining = Duration.between(clock.instant(), expirations.get(0));
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    @Override
    public void recordFailure(String key, Duration window) {
        Instant now = clock.instant();
        jdbc.update("""
                insert into login_attempt (key_hash, window_started_at, expires_at, failures)
                values (?, ?, ?, 1)
                on conflict (key_hash) do update set
                  failures = case
                    when login_attempt.expires_at <= ? then 1
                    else login_attempt.failures + 1
                  end,
                  window_started_at = case
                    when login_attempt.expires_at <= ? then excluded.window_started_at
                    else login_attempt.window_started_at
                  end,
                  expires_at = case
                    when login_attempt.expires_at <= ? then excluded.expires_at
                    else login_attempt.expires_at
                  end
                """, hash(key), Timestamp.from(now), Timestamp.from(now.plus(window)),
                Timestamp.from(now), Timestamp.from(now), Timestamp.from(now));
        cleanupExpired();
    }

    @Override
    public void reset(String key) {
        jdbc.update("delete from login_attempt where key_hash = ?", hash(key));
    }

    private void cleanupExpired() {
        long now = clock.millis();
        long scheduled = nextCleanup.get();
        if (now >= scheduled && nextCleanup.compareAndSet(scheduled, now + CLEANUP_INTERVAL_MILLIS)) {
            jdbc.update("delete from login_attempt where expires_at <= ?", Timestamp.from(clock.instant()));
        }
    }

    private static String hash(String key) {
        return RequestHasher.hash("login-throttle", key);
    }
}
