package com.softteco.orderservice.web.dto;

import com.softteco.orderservice.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String customerEmail,
        BigDecimal amount,
        String productSku,
        Integer quantity,
        OrderStatus status,
        Instant createdAt
) {
}

