package com.softteco.billingservice.service;

import com.softteco.billingservice.domain.PaymentEntity;
import com.softteco.billingservice.domain.PaymentRepository;
import com.softteco.billingservice.domain.PaymentStatus;
import com.softteco.billingservice.web.dto.CreatePaymentRequest;
import com.softteco.billingservice.web.dto.PaymentResponse;
import com.softteco.billingservice.web.dto.RefundPaymentRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentResponse processPayment(CreatePaymentRequest request) {
        log.info("Processing payment for order {}: ${}", request.orderId(), request.amount());

        // In a real application, you would integrate with a payment gateway here
        boolean paymentSuccessful = simulatePaymentProcessing();

        var payment = PaymentEntity.builder()
                .orderId(request.orderId())
                .amount(request.amount())
                .status(paymentSuccessful ? PaymentStatus.CAPTURED : PaymentStatus.FAILED)
                .processedAt(Instant.now())
                .build();

        PaymentEntity savedPayment = Objects.requireNonNull(
                paymentRepository.save(payment),
                "Saved payment must not be null");
        log.info("Payment processed with status: {}", savedPayment.getStatus());

        return mapToResponse(savedPayment);
    }

    public PaymentResponse getPayment(String id) {
        Objects.requireNonNull(id, "Payment ID must not be null");
        UUID uuid = UUID.fromString(id);

        return paymentRepository.findById(uuid)
                .map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + id));
    }

    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public PaymentResponse refundPayment(RefundPaymentRequest request) {
        Objects.requireNonNull(request.paymentId(), "Payment ID must not be null");
        log.info("Refunding payment {}: ${}", request.paymentId(), request.amount());

        PaymentEntity payment = paymentRepository.findById(request.paymentId())
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + request.paymentId()));

        if (payment.getStatus() != PaymentStatus.CAPTURED) {
            throw new IllegalStateException("Only captured payments can be refunded");
        }

        // In a real application, you would integrate with a payment gateway here
        boolean refundSuccessful = simulatePaymentProcessing();

        if (refundSuccessful) {
            payment.setStatus(PaymentStatus.REFUNDED);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }

        PaymentEntity savedPayment = paymentRepository.save(payment);
        log.info("Refund processed with status: {}", savedPayment.getStatus());

        return mapToResponse(savedPayment);
    }

    private boolean simulatePaymentProcessing() {
        // In a real application, this would call a payment gateway
        // For demo purposes, we'll simulate a 95% success rate
        return Math.random() < 0.95;
    }

    private PaymentResponse mapToResponse(PaymentEntity payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId().toString(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getProcessedAt());
    }
}
