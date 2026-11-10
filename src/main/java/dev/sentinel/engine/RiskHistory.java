package dev.sentinel.engine;

/** Database-computed signals for one transaction, without loading an account's full history. */
public record RiskHistory(
        long recentTransactions,
        long recentSmallTransactions,
        String mostRecentOtherCountry,
        boolean hasPriorDevices,
        boolean currentDeviceKnown) {}
