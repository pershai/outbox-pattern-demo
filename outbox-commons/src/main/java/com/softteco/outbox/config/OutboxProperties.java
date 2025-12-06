package com.softteco.outbox.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the outbox pattern.
 */
@Data
@ConfigurationProperties(prefix = "outbox")
public class OutboxProperties {

    private boolean enabled = true;
    private RelayConfig relay = new RelayConfig();
    private CleanupConfig cleanup = new CleanupConfig();

    @Data
    public static class RelayConfig {
        private boolean enabled = true;
        private long intervalMs = 5000; // 5 seconds
        private int batchSize = 100;
    }

    @Data
    public static class CleanupConfig {
        private boolean enabled = true;
        private int retentionDays = 7;
        private long intervalMs = 3600000; // 1 hour
    }
}
