package dev.sentinel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.jspecify.annotations.Nullable;

/** First-run accounts. A blank password means "do not create this user". */
@ConfigurationProperties("sentinel.bootstrap")
public record BootstrapProperties(
        @Nullable String adminPassword,
        @Nullable String analystPassword,
        @Nullable String servicePassword) {}
