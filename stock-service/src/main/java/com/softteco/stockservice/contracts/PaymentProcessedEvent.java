package com.softteco.stockservice.contracts;

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
}