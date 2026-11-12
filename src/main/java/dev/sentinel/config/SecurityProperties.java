package dev.sentinel.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.jspecify.annotations.Nullable;

@ConfigurationProperties("sentinel.security")
public record SecurityProperties(
        @Nullable String jwtSecret,
        @DefaultValue("15m") Duration tokenTtl,
        @Nullable List<String> allowedOrigins) {}
