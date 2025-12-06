package com.softteco.stockservice.contracts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StockReservedEvent(
        UUID eventId,
        UUID reservationId,
        UUID orderId,
        String productSku,
        Integer quantity,
        BigDecimal amount,
        Instant reservedAt
) {
}

