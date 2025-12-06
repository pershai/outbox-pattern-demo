package com.softteco.orderservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.softteco.orderservice.contracts.OrderCreatedEvent;
import com.softteco.orderservice.domain.OrderEntity;
import com.softteco.orderservice.domain.OrderRepository;
import com.softteco.orderservice.domain.OrderStatus;
// Import from shared outbox-commons library
import com.softteco.outbox.entity.OutboxEventEntity;
import com.softteco.outbox.entity.OutboxStatus;
import com.softteco.outbox.repository.OutboxEventRepository;
import com.softteco.orderservice.web.dto.CreateOrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    public static final String EVENT_TYPE_ORDER_CREATED = "orders.created";

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderEntity createOrder(CreateOrderRequest request) {
        OrderEntity order = OrderEntity.builder()
                .customerEmail(request.customerEmail())
                .amount(request.amount())
                .productSku(request.productSku())
                .quantity(request.quantity())
                .status(OrderStatus.CREATED)
                .build();
        OrderEntity savedOrder = Objects.requireNonNull(
                orderRepository.save(order),
                "Saved order must not be null");

        UUID eventId = UUID.randomUUID();
        OrderCreatedEvent event = new OrderCreatedEvent(
                eventId,
                savedOrder.getId(),
                savedOrder.getCustomerEmail(),
                savedOrder.getAmount(),
                savedOrder.getProductSku(),
                savedOrder.getQuantity(),
                savedOrder.getCreatedAt());

        OutboxEventEntity outboxEvent = OutboxEventEntity.builder()
                .eventId(eventId)
                .eventType(EVENT_TYPE_ORDER_CREATED)
                .payload(serialize(event))
                .status(OutboxStatus.NEW)
                .build();

        Objects.requireNonNull(
                outboxEventRepository.save(outboxEvent),
                "Saved outbox event must not be null");

        log.info("Created order {} with outbox event {}", savedOrder.getId(), eventId);
        return savedOrder;
    }

    private String serialize(Object object) {
        try {
            // Convert the object to a JSON string and ensure it's properly formatted for
            // PostgreSQL jsonb
            String json = objectMapper.writeValueAsString(object);
            // PostgreSQL's jsonb type requires valid JSON, so we'll parse it back to ensure
            // it's valid
            objectMapper.readTree(json);
            return json;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }
}
