package dev.sentinel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** First-run accounts. A blank password means "do not create this user". */
@ConfigurationProperties("sentinel.bootstrap")
public record BootstrapProperties(String adminPassword, String analystPassword, String servicePassword) {}
