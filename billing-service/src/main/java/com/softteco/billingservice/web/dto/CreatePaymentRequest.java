package com.softteco.billingservice.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentRequest(
    @NotNull
    UUID orderId,

    @NotNull
    @Positive
    BigDecimal amount
) {
}
