package com.softteco.orderservice.web;

import com.softteco.orderservice.domain.OrderEntity;
import com.softteco.orderservice.service.OrderService;
import com.softteco.orderservice.web.dto.CreateOrderRequest;
import com.softteco.orderservice.web.dto.OrderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
        OrderEntity order = orderService.createOrder(request);
        return new OrderResponse(
                order.getId(),
                order.getCustomerEmail(),
                order.getAmount(),
                order.getProductSku(),
                order.getQuantity(),
                order.getStatus(),
                order.getCreatedAt());
    }
}
