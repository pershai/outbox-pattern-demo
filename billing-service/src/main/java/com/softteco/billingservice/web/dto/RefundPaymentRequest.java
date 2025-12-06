package com.softteco.billingservice.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record RefundPaymentRequest(
    @NotNull
    UUID paymentId,

    @NotNull
    @Positive
    BigDecimal amount
) {
}
