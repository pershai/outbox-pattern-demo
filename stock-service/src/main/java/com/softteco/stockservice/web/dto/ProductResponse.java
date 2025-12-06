package com.softteco.stockservice.web.dto;

public record ProductResponse(
        String sku,
        String name,
        Integer availableQuantity
) {
}

