package dev.sentinel.engine;

import org.jspecify.annotations.Nullable;

/** Database-computed signals for one transaction, without loading an account's full history. */
public record RiskHistory(
        long recentTransactions,
        long recentSmallTransactions,
        @Nullable String mostRecentOtherCountry,
        boolean hasPriorDevices,
        boolean currentDeviceKnown) {}
