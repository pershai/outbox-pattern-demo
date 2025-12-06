package com.softteco.billingservice.consumer;

import com.softteco.billingservice.contracts.StockReservedEvent;
import com.softteco.billingservice.service.PaymentProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockReservedListener {

    private final PaymentProcessor paymentProcessor;

    @Value("${spring.kafka.consumer.group-id}")
    private String handlerName;

    @KafkaListener(topics = "${app.kafka.topics.stock-reserved}")
    public void onMessage(@Payload StockReservedEvent event,
                          @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        log.debug("Received stock reserved {} for order {} with key {}", event.eventId(), event.orderId(), key);
        paymentProcessor.capturePayment(event, handlerName);
    }
}

