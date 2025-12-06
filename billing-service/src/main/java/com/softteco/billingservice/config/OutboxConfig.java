package com.softteco.billingservice.config;

/**
 * Configuration constants for the outbox pattern implementation.
 * Centralizes magic numbers and configuration values for better
 * maintainability.
 */
public final class OutboxConfig {

    private OutboxConfig() {
        // Prevent instantiation
    }

    // Kafka retry configuration
    public static final long KAFKA_RETRY_INTERVAL_MS = 1000L;
    public static final int KAFKA_MAX_RETRY_ATTEMPTS = 3;

    // Outbox polling configuration
    public static final long OUTBOX_POLLING_INTERVAL_MS = 5000L;
    public static final int OUTBOX_BATCH_SIZE = 50;

    // Cleanup configuration
    public static final int OUTBOX_RETENTION_DAYS = 7;

    // Retry configuration
    public static final int RETRY_MAX_ATTEMPTS = 3;
    public static final long RETRY_INITIAL_DELAY_MS = 1000L;
    public static final double RETRY_MULTIPLIER = 2.0;
}
