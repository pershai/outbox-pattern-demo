package com.softteco.billingservice.contracts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentProcessedEvent(
        UUID eventId,
        UUID orderId,
        UUID paymentId,
        BigDecimal amount,
        String productSku,
        Integer quantity,
        Instant processedAt
) {
    public static PaymentProcessedEvent of(UUID orderId, UUID paymentId, BigDecimal amount, String productSku, Integer quantity) {
        return new PaymentProcessedEvent(
                UUID.randomUUID(),
                orderId,
                paymentId,
                amount,
                productSku,
                quantity,
                Instant.now()
        );
    }
}