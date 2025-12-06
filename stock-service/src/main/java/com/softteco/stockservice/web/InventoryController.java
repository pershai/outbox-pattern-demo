package com.softteco.stockservice.web;

import com.softteco.stockservice.domain.ProductRepository;
import com.softteco.stockservice.domain.StockReservationRepository;
import com.softteco.stockservice.web.dto.ProductResponse;
import com.softteco.stockservice.web.dto.ReservationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stock")
@RequiredArgsConstructor
public class InventoryController {

        private final ProductRepository productRepository;
        private final StockReservationRepository reservationRepository;

        @GetMapping("/products")
        public List<ProductResponse> products() {
                return productRepository.findAll().stream()
                                .map(product -> new ProductResponse(
                                                product.getSku(),
                                                product.getName(),
                                                product.getAvailableQuantity()))
                                .toList();
        }

        @GetMapping("/reservations")
        public List<ReservationResponse> reservations() {
                return reservationRepository.findAll().stream()
                                .map(reservation -> new ReservationResponse(
                                                reservation.getId(),
                                                reservation.getOrderId(),
                                                reservation.getProductSku(),
                                                reservation.getQuantity(),
                                                reservation.getStatus(),
                                                reservation.getCreatedAt()))
                                .toList();
        }
}
