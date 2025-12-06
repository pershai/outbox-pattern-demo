package com.softteco.stockservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.softteco.stockservice.contracts.OrderCreatedEvent;
import com.softteco.stockservice.contracts.StockReservedEvent;
import com.softteco.stockservice.domain.ProcessedEventEntity;
import com.softteco.stockservice.domain.ProcessedEventId;
import com.softteco.stockservice.domain.ProcessedEventRepository;
import com.softteco.stockservice.domain.ProductEntity;
import com.softteco.stockservice.domain.ProductRepository;
import com.softteco.stockservice.domain.ReservationStatus;
import com.softteco.stockservice.domain.StockReservationEntity;
import com.softteco.stockservice.domain.StockReservationRepository;
import com.softteco.outbox.entity.OutboxEventEntity;
import com.softteco.outbox.repository.OutboxEventRepository;
import com.softteco.outbox.entity.OutboxStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockReservationService {

        public static final String EVENT_TYPE_STOCK_RESERVED = "stock.reserved";

        private final ProductRepository productRepository;
        private final StockReservationRepository reservationRepository;
        private final OutboxEventRepository outboxEventRepository;
        private final ProcessedEventRepository processedEventRepository;
        private final ObjectMapper objectMapper;

        @Value("${spring.kafka.consumer.group-id}")
        private String handlerName;

        @Transactional
        public void handleOrder(OrderCreatedEvent event) {
                ProcessedEventId processedEventId = new ProcessedEventId(event.eventId(), handlerName);
                if (processedEventRepository.existsById(processedEventId)) {
                        log.debug("Event {} already processed by {}", event.eventId(), handlerName);
                        return;
                }

                ProductEntity product = productRepository.findBySku(event.productSku())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Product not found: " + event.productSku()));

                if (product.getAvailableQuantity() < event.quantity()) {
                        reservationRepository.save(
                                        reservation(event, ReservationStatus.FAILED));
                        log.warn("Insufficient stock for SKU {}. Needed {}, available {}",
                                        product.getSku(), event.quantity(), product.getAvailableQuantity());
                        return;
                }

                int updatedRows = productRepository.decreaseStock(event.productSku(), event.quantity());
                if (updatedRows == 0) {
                        reservationRepository.save(
                                        reservation(event, ReservationStatus.FAILED));
                        log.warn("Insufficient stock for SKU {} during atomic update. Needed {}",
                                        product.getSku(), event.quantity());
                        return;
                }

                StockReservationEntity reservation = reservationRepository.save(
                                reservation(event, ReservationStatus.RESERVED));

                StockReservedEvent stockReservedEvent = new StockReservedEvent(
                                UUID.randomUUID(),
                                reservation.getId(),
                                event.orderId(),
                                event.productSku(),
                                event.quantity(),
                                event.amount(),
                                reservation.getCreatedAt());

                outboxEventRepository.save(OutboxEventEntity.builder()
                                .eventId(stockReservedEvent.eventId())
                                .eventType(EVENT_TYPE_STOCK_RESERVED)
                                .payload(serialize(stockReservedEvent))
                                .status(OutboxStatus.NEW)
                                .occurredAt(Instant.now())
                                .build());

                processedEventRepository.save(ProcessedEventEntity.builder()
                                .id(processedEventId)
                                .processedAt(Instant.now())
                                .build());

                log.info("Reserved {} units of {} for order {}", event.quantity(), event.productSku(), event.orderId());
        }

        private StockReservationEntity reservation(OrderCreatedEvent event, ReservationStatus status) {
                return StockReservationEntity.builder()
                                .orderId(event.orderId())
                                .productSku(event.productSku())
                                .quantity(event.quantity())
                                .status(status)
                                .createdAt(Instant.now())
                                .build();
        }

        private String serialize(Object payload) {
                try {
                        return objectMapper.writeValueAsString(payload);
                } catch (JsonProcessingException e) {
                        throw new IllegalStateException("Failed to serialize payload", e);
                }
        }
}
