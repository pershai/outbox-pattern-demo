package com.softteco.outbox.metrics;

import com.softteco.outbox.entity.OutboxStatus;
import com.softteco.outbox.repository.OutboxEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Metrics collection for outbox event processing.
 */
@Component
@RequiredArgsConstructor
public class OutboxMetrics {

    private final MeterRegistry meterRegistry;
    private final OutboxEventRepository outboxEventRepository;

    private Counter processedCounter;
    private Counter failureCounter;

    public void init() {
        // Counters
        processedCounter = Counter.builder("outbox.relay.processed")
                .description("Total events processed by relay")
                .register(meterRegistry);

        failureCounter = Counter.builder("outbox.relay.failures")
                .description("Total relay processing failures")
                .register(meterRegistry);

        // Gauges for event counts by status
        Gauge.builder("outbox.events.new", outboxEventRepository,
                repo -> repo.countByStatus(OutboxStatus.NEW))
                .description("Number of NEW outbox events")
                .register(meterRegistry);

        Gauge.builder("outbox.events.sent", outboxEventRepository,
                repo -> repo.countByStatus(OutboxStatus.SENT))
                .description("Number of SENT outbox events")
                .register(meterRegistry);

        Gauge.builder("outbox.events.error", outboxEventRepository,
                repo -> repo.countByStatus(OutboxStatus.ERROR))
                .description("Number of ERROR outbox events")
                .register(meterRegistry);
    }

    public void incrementProcessed() {
        if (processedCounter != null) {
            processedCounter.increment();
        }
    }

    public void incrementFailures() {
        if (failureCounter != null) {
            failureCounter.increment();
        }
    }
}
