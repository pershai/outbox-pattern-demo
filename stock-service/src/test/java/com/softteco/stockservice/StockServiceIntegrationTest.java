package com.softteco.stockservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softteco.stockservice.contracts.OrderCreatedEvent;
import com.softteco.stockservice.domain.ProductEntity;
import com.softteco.stockservice.domain.ProductRepository;
import com.softteco.stockservice.domain.StockReservationEntity;
import com.softteco.stockservice.domain.StockReservationRepository;
import com.softteco.outbox.entity.OutboxEventEntity;
import com.softteco.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class StockServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockReservationRepository stockReservationRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        stockReservationRepository.deleteAll();
        outboxEventRepository.deleteAll();

        ProductEntity product = new ProductEntity();
        product.setSku("TEST-SKU");
        product.setAvailableQuantity(100);
        productRepository.save(product);
    }

    @Test
    void shouldReserveStockOnOrderCreated() throws Exception {
        // Given
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "test@example.com",
                BigDecimal.TEN,
                "TEST-SKU",
                2,
                Instant.now());

        String json = objectMapper.writeValueAsString(event);

        // When
        kafkaTemplate.send("orders.created.v1", json);

        // Then
        // 1. Verify Stock Reservation in DB
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<StockReservationEntity> reservations = stockReservationRepository.findAll();
            assertThat(reservations).hasSize(1);
            assertThat(reservations.get(0).getProductSku()).isEqualTo("TEST-SKU");
            assertThat(reservations.get(0).getQuantity()).isEqualTo(2);

            // Verify product quantity decreased
            ProductEntity updatedProduct = productRepository.findBySku("TEST-SKU").orElseThrow();
            assertThat(updatedProduct.getAvailableQuantity()).isEqualTo(98); // 100 - 2
        });

        // 2. Verify Outbox Event (StockReserved)
        List<OutboxEventEntity> outboxEvents = outboxEventRepository.findAll();
        assertThat(outboxEvents).hasSize(1);
        assertThat(outboxEvents.get(0).getEventType()).isEqualTo("stock.reserved");
        assertThat(outboxEvents.get(0).getStatus()).isEqualTo(com.softteco.outbox.entity.OutboxStatus.NEW);
        assertThat(outboxEvents.get(0).getPayload()).isNotNull();
    }

    @Test
    void shouldFailWhenInsufficientStock() throws Exception {
        // Given - product has 100 units
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "test@example.com",
                BigDecimal.TEN,
                "TEST-SKU",
                150, // More than available
                Instant.now());

        String json = objectMapper.writeValueAsString(event);

        // When
        kafkaTemplate.send("orders.created.v1", json);

        // Then
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<StockReservationEntity> reservations = stockReservationRepository.findAll();
            assertThat(reservations).hasSize(1);
            assertThat(reservations.get(0).getStatus())
                    .isEqualTo(com.softteco.stockservice.domain.ReservationStatus.FAILED);
        });

        // Verify NO outbox event created for failed reservation
        List<OutboxEventEntity> outboxEvents = outboxEventRepository.findAll();
        assertThat(outboxEvents).isEmpty();

        // Verify product quantity unchanged
        ProductEntity product = productRepository.findBySku("TEST-SKU").orElseThrow();
        assertThat(product.getAvailableQuantity()).isEqualTo(100);
    }

    @Test
    void shouldHandleDuplicateEvents_Idempotency() throws Exception {
        // Given
        UUID eventId = UUID.randomUUID();
        OrderCreatedEvent event = new OrderCreatedEvent(
                eventId,
                UUID.randomUUID(),
                "test@example.com",
                BigDecimal.TEN,
                "TEST-SKU",
                5,
                Instant.now());

        String json = objectMapper.writeValueAsString(event);

        // When - send same event twice
        kafkaTemplate.send("orders.created.v1", json);
        kafkaTemplate.send("orders.created.v1", json);

        // Then - only ONE reservation created
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<StockReservationEntity> reservations = stockReservationRepository.findAll();
            assertThat(reservations).hasSize(1);
        });

        // Only ONE outbox event
        List<OutboxEventEntity> outboxEvents = outboxEventRepository.findAll();
        assertThat(outboxEvents).hasSize(1);

        // Stock decreased only ONCE (5 units, not 10)
        ProductEntity product = productRepository.findBySku("TEST-SKU").orElseThrow();
        assertThat(product.getAvailableQuantity()).isEqualTo(95); // 100 - 5
    }

    @Test
    void shouldVerifyOutboxEventStructure() throws Exception {
        // Given
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "test@example.com",
                BigDecimal.valueOf(50.00),
                "TEST-SKU",
                3,
                Instant.now());

        String json = objectMapper.writeValueAsString(event);

        // When
        kafkaTemplate.send("orders.created.v1", json);

        // Then
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<OutboxEventEntity> outboxEvents = outboxEventRepository.findAll();
            assertThat(outboxEvents).hasSize(1);

            OutboxEventEntity outboxEvent = outboxEvents.get(0);
            assertThat(outboxEvent.getEventType()).isEqualTo("stock.reserved");
            assertThat(outboxEvent.getStatus()).isEqualTo(com.softteco.outbox.entity.OutboxStatus.NEW);
            assertThat(outboxEvent.getPayload()).isNotNull();
            assertThat(outboxEvent.getOccurredAt()).isNotNull();
            assertThat(outboxEvent.getEventId()).isNotNull();

            // Verify payload contains stock reservation data
            assertThat(outboxEvent.getPayload()).contains("TEST-SKU");
            assertThat(outboxEvent.getPayload()).contains("50.00");
        });
    }
}
