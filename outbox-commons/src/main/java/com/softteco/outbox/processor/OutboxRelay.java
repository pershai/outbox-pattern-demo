package com.softteco.outbox.processor;

import com.softteco.outbox.entity.OutboxEventEntity;
import com.softteco.outbox.entity.OutboxStatus;
import com.softteco.outbox.repository.OutboxEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scheduled component that polls the outbox table for new events
 * and forwards them for publishing.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelay {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventProcessor outboxEventProcessor;
    private final AtomicInteger pendingEvents;

    public OutboxRelay(OutboxEventRepository outboxEventRepository, 
                      OutboxEventProcessor outboxEventProcessor,
                      MeterRegistry meterRegistry) {
        this.outboxEventRepository = outboxEventRepository;
        this.outboxEventProcessor = outboxEventProcessor;
        this.pendingEvents = new AtomicInteger(0);
        meterRegistry.gauge("outbox.pending.events", this.pendingEvents);
    }

    /**
     * Periodically fetch and forward new outbox events for publishing.
     * Configurable via outbox.relay.interval (default: 2 seconds).
     */
    @Scheduled(fixedDelayString = "${outbox.relay.interval:PT2S}")
    public void forward() {
        List<OutboxEventEntity> events = outboxEventRepository
                .findBatch(OutboxStatus.NEW, 50);
        
        pendingEvents.set(events.size());

        if (events.isEmpty()) {
            return;
        }

        log.debug("Forwarding {} outbox events", events.size());
        events.forEach(outboxEventProcessor::process);
    }
}
