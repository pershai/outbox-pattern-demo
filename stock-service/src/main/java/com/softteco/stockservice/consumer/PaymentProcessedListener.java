package com.softteco.stockservice.consumer;

import com.softteco.stockservice.contracts.PaymentProcessedEvent;
import com.softteco.stockservice.domain.ProductRepository;
import com.softteco.stockservice.domain.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProcessedListener {

    private final ProductRepository productRepository;
    private final StockReservationRepository stockReservationRepository;

    @KafkaListener(topics = "${app.kafka.topics.payment-processed}")
    @Transactional
    public void onPaymentProcessed(@Payload PaymentProcessedEvent event) {
        log.info("Processing payment processed event for order {} and payment {}",
                event.orderId(), event.paymentId());

        // Update the product quantity
        productRepository.findBySku(event.productSku()).ifPresent(product -> {
            product.decrease(event.quantity());
            productRepository.save(product);
            log.info("Updated product {} quantity by {} for order {}",
                    event.productSku(), -event.quantity(), event.orderId());
        });
    }
}