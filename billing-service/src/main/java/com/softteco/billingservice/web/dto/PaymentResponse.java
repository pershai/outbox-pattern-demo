package com.softteco.billingservice.web.dto;

import com.softteco.billingservice.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        String orderId,
        BigDecimal amount,
        PaymentStatus status,
        Instant processedAt
) {
}
