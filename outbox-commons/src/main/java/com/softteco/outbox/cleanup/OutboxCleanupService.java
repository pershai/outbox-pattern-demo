package com.softteco.outbox.cleanup;

import com.softteco.outbox.entity.OutboxStatus;
import com.softteco.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Scheduled service to clean up old successfully sent outbox events.
 * Prevents database bloat and maintains query performance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxCleanupService {

    private final OutboxEventRepository outboxEventRepository;

    @Value("${outbox.cleanup.retention-days:7}")
    private int retentionDays;

    /**
     * Runs daily at 2 AM to clean up old sent events.
     * Configurable via outbox.cleanup.cron (default: 0 0 2 * * *).
     */
    @Scheduled(cron = "${outbox.cleanup.cron:0 0 2 * * *}")
    @Transactional
    public void cleanupOldSentEvents() {
        Instant cutoffTime = Instant.now().minus(retentionDays, ChronoUnit.DAYS);

        log.info("Starting outbox cleanup for events older than {}", cutoffTime);

        int deletedCount = outboxEventRepository.deleteByStatusAndSentAtBefore(
                OutboxStatus.SENT,
                cutoffTime);

        log.info("Cleaned up {} old outbox events", deletedCount);
    }
}
