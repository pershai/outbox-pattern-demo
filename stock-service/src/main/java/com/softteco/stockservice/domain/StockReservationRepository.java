package com.softteco.stockservice.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StockReservationRepository extends JpaRepository<StockReservationEntity, UUID> {

    Optional<StockReservationEntity> findByOrderId(UUID orderId);
}

