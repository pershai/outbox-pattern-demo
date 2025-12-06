package com.softteco.stockservice.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<ProductEntity, String> {

    Optional<ProductEntity> findBySku(String sku);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE ProductEntity p SET p.availableQuantity = p.availableQuantity - :qty WHERE p.sku = :sku AND p.availableQuantity >= :qty")
    int decreaseStock(String sku, int qty);
}
