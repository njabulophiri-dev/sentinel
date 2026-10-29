package dev.sentinel.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("sentinel.security")
public record SecurityProperties(
        String jwtSecret,
        @DefaultValue("15m") Duration tokenTtl,
        List<String> allowedOrigins) {}
