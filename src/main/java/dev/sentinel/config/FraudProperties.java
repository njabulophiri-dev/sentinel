package dev.sentinel.config;

import java.math.BigDecimal;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Tunable fraud thresholds. Change them in application.yml or with SENTINEL_FRAUD_* environment variables. */
@ConfigurationProperties("sentinel.fraud")
public record FraudProperties(
        @DefaultValue("40") int reviewThreshold,
        @DefaultValue("80") int declineThreshold,
        @DefaultValue("5000") BigDecimal highAmount,
        @DefaultValue("50000") BigDecimal veryHighAmount,
        @DefaultValue("5") int velocityMaxCount,
        @DefaultValue("10m") Duration velocityWindow,
        @DefaultValue("60m") Duration countryWindow,
        @DefaultValue("1000") BigDecimal newDeviceMinAmount,
        @DefaultValue("4") int cardTestingCount,
        @DefaultValue("20") BigDecimal cardTestingMaxAmount,
        @DefaultValue("10m") Duration cardTestingWindow) {}
