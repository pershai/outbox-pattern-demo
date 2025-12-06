package com.softteco.stockservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "products")
public class ProductEntity {

    @Id
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    public void decrease(int qty) {
        this.availableQuantity = this.availableQuantity - qty;
    }
}

