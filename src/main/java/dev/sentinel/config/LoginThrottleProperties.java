package dev.sentinel.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** How many failed sign-ins are allowed before further attempts are refused for a while. */
@ConfigurationProperties("sentinel.login-throttle")
public record LoginThrottleProperties(
        @DefaultValue("5") int maxFailuresPerUser,
        @DefaultValue("30") int maxFailuresPerIp,
        @DefaultValue("10m") Duration window) {}
