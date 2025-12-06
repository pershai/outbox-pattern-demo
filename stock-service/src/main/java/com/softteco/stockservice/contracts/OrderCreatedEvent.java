package com.softteco.stockservice.contracts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID eventId,
        UUID orderId,
        String customerEmail,
        BigDecimal amount,
        String productSku,
        Integer quantity,
        Instant createdAt
) {
}

