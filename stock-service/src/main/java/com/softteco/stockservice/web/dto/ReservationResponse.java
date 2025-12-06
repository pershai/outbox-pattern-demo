package com.softteco.stockservice.web.dto;

import com.softteco.stockservice.domain.ReservationStatus;

import java.time.Instant;
import java.util.UUID;

public record ReservationResponse(
        UUID id,
        UUID orderId,
        String productSku,
        Integer quantity,
        ReservationStatus status,
        Instant createdAt
) {
}

