// billing-service/src/main/java/com/softteco/billingservice/service/PaymentProcessor.java
package com.softteco.billingservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.softteco.billingservice.contracts.PaymentProcessedEvent;
import com.softteco.billingservice.contracts.StockReservedEvent;
import com.softteco.billingservice.domain.PaymentEntity;
import com.softteco.billingservice.domain.PaymentRepository;
import com.softteco.billingservice.domain.PaymentStatus;
import com.softteco.billingservice.domain.ProcessedEventEntity;
import com.softteco.billingservice.domain.ProcessedEventId;
import com.softteco.billingservice.domain.ProcessedEventRepository;
import com.softteco.outbox.entity.OutboxEventEntity;
import com.softteco.outbox.repository.OutboxEventRepository;
import com.softteco.outbox.entity.OutboxStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentProcessor {

        public static final String EVENT_TYPE_PAYMENT_PROCESSED = "payment.processed";

        private final PaymentRepository paymentRepository;
        private final ProcessedEventRepository processedEventRepository;
        private final OutboxEventRepository outboxEventRepository;
        private final ObjectMapper objectMapper;

        @Value("${spring.kafka.consumer.group-id}")
        private String handlerName;

        @Transactional
        public void capturePayment(StockReservedEvent event, String handler) {
                ProcessedEventId id = new ProcessedEventId(event.reservationId(), handler);
                if (processedEventRepository.existsById(id)) {
                        log.debug("Event {} already processed by {}", event.eventId(), handler);
                        return;
                }

                PaymentEntity payment = PaymentEntity.builder()
                                .orderId(event.orderId())
                                .amount(event.amount())
                                .status(PaymentStatus.CAPTURED)
                                .processedAt(Instant.now())
                                .build();
                PaymentEntity savedPayment = Objects.requireNonNull(
                                paymentRepository.save(payment),
                                "Saved payment must not be null");

                // Create and save the outbox event
                PaymentProcessedEvent paymentProcessedEvent = PaymentProcessedEvent.of(
                                event.orderId(),
                                savedPayment.getId(),
                                event.amount(),
                                event.productSku(),
                                event.quantity());

                try {
                        Objects.requireNonNull(
                                        outboxEventRepository.save(OutboxEventEntity.builder()
                                                        .eventId(paymentProcessedEvent.eventId())
                                                        .eventType(EVENT_TYPE_PAYMENT_PROCESSED)
                                                        .payload(objectMapper.writeValueAsString(paymentProcessedEvent))
                                                        .status(OutboxStatus.NEW)
                                                        .occurredAt(Instant.now())
                                                        .build()),
                                        "Saved outbox event must not be null");
                } catch (JsonProcessingException e) {
                        throw new RuntimeException("Failed to serialize payment processed event", e);
                }

                ProcessedEventEntity processedEvent = ProcessedEventEntity.builder()
                                .id(id)
                                .processedAt(Instant.now())
                                .build();
                Objects.requireNonNull(
                                processedEventRepository.save(processedEvent),
                                "Saved processed event must not be null");

                log.info("Captured payment {} for order {} (event {})", savedPayment.getId(), event.orderId(),
                                event.eventId());
        }
}