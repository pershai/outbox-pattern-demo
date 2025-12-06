package com.softteco.billingservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softteco.billingservice.contracts.StockReservedEvent;
import com.softteco.billingservice.domain.PaymentEntity;
import com.softteco.billingservice.domain.PaymentRepository;
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

class BillingServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
    }

    @Test
    void shouldProcessPaymentOnStockReserved() throws Exception {
        // Given
        StockReservedEvent event = new StockReservedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "TEST-SKU",
                2,
                BigDecimal.valueOf(100.00),
                Instant.now());

        String json = objectMapper.writeValueAsString(event);

        // When
        kafkaTemplate.send("stock.reserved.v1", json);

        // Then
        // Verify Payment in DB
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<PaymentEntity> payments = paymentRepository.findAll();
            assertThat(payments).hasSize(1);
            assertThat(payments.get(0).getOrderId()).isEqualTo(event.orderId());
            assertThat(payments.get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
            assertThat(payments.get(0).getStatus())
                    .isEqualTo(com.softteco.billingservice.domain.PaymentStatus.CAPTURED);
            assertThat(payments.get(0).getProcessedAt()).isNotNull();
        });
    }

    @Test
    void shouldHandleDuplicateEvents_Idempotency() throws Exception {
        // Given - same reservation ID
        UUID reservationId = UUID.randomUUID();
        StockReservedEvent event = new StockReservedEvent(
                UUID.randomUUID(),
                reservationId,
                UUID.randomUUID(),
                "TEST-SKU",
                2,
                BigDecimal.valueOf(100.00),
                Instant.now());

        String json = objectMapper.writeValueAsString(event);

        // When - send same event twice
        kafkaTemplate.send("stock.reserved.v1", json);
        kafkaTemplate.send("stock.reserved.v1", json);

        // Then - only ONE payment captured
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<PaymentEntity> payments = paymentRepository.findAll();
            assertThat(payments).hasSize(1);
        });

        // Verify only one payment for the given amount
        List<PaymentEntity> payments = paymentRepository.findAll();
        assertThat(payments.get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
    }

    @Test
    void shouldProcessMultiplePaymentsForDifferentOrders() throws Exception {
        // Given
        StockReservedEvent event1 = new StockReservedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "TEST-SKU-1",
                1,
                BigDecimal.valueOf(50.00),
                Instant.now());

        StockReservedEvent event2 = new StockReservedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "TEST-SKU-2",
                2,
                BigDecimal.valueOf(150.00),
                Instant.now());

        String json1 = objectMapper.writeValueAsString(event1);
        String json2 = objectMapper.writeValueAsString(event2);

        // When
        kafkaTemplate.send("stock.reserved.v1", json1);
        kafkaTemplate.send("stock.reserved.v1", json2);

        // Then
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<PaymentEntity> payments = paymentRepository.findAll();
            assertThat(payments).hasSize(2);
        });

        // Verify both payments processed correctly
        List<PaymentEntity> payments = paymentRepository.findAll();
        assertThat(payments)
                .extracting(PaymentEntity::getAmount)
                .containsExactlyInAnyOrder(
                        BigDecimal.valueOf(50.00),
                        BigDecimal.valueOf(150.00));
    }
}
