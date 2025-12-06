package com.softteco.outbox.processor;

import com.softteco.outbox.entity.OutboxEventEntity;
import com.softteco.outbox.entity.OutboxStatus;
import com.softteco.outbox.metrics.OutboxMetrics;
import com.softteco.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Processes individual outbox events by publishing them to Kafka.
 * Each event is processed in a separate transaction with retry logic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventProcessor {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxEventRepository outboxEventRepository;
    private final RetryTemplate retryTemplate;
    private final OutboxMetrics metrics;

    /**
     * Process a single outbox event by publishing it to Kafka.
     * Uses a new transaction with retry logic for resilience.
     *
     * @param event the outbox event to process
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(OutboxEventEntity event) {
        try {
            retryTemplate.execute(context -> {
                String topic = extractTopicFromEventType(event.getEventType());
                log.debug("Publishing event {} to topic {}, attempt {}",
                        event.getEventId(), topic, context.getRetryCount() + 1);

                kafkaTemplate.send(topic, event.getEventId().toString(), event.getPayload());
                return null;
            });

            event.setStatus(OutboxStatus.SENT);
            event.setSentAt(Instant.now());
            outboxEventRepository.save(event);

            log.info("Successfully published event {} of type {}",
                    event.getEventId(), event.getEventType());

        } catch (Exception e) {
            event.setStatus(OutboxStatus.ERROR);
            event.setErrorMessage(truncateErrorMessage(e.getMessage()));
            if (event.getRetryCount() == null) {
                event.setRetryCount(0);
            }
            outboxEventRepository.save(event);
            metrics.incrementFailures();
            log.error("Failed to process outbox event {} after retries: {}",
                    event.getEventId(), e.getMessage(), e);
        }
    }

    private String truncateErrorMessage(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 1000 ? message.substring(0, 997) + "..." : message;
    }

    /**
     * Extract Kafka topic name from event type.
     * Converts event type like "orders.created" to topic "orders.created.v1".
     *
     * @param eventType the event type
     * @return the Kafka topic name
     */
    private String extractTopicFromEventType(String eventType) {
        return eventType + ".v1";
    }
}
