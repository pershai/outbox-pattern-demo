package com.softteco.stockservice.consumer;

import com.softteco.stockservice.contracts.OrderCreatedEvent;
import com.softteco.stockservice.service.StockReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedListener {

    private final StockReservationService stockReservationService;

    @KafkaListener(topics = "${app.kafka.topics.orders-created}")
    public void onMessage(@Payload OrderCreatedEvent event) {
        log.debug("Received order {} for SKU {} qty {}", event.orderId(), event.productSku(), event.quantity());
        stockReservationService.handleOrder(event);
    }
}

