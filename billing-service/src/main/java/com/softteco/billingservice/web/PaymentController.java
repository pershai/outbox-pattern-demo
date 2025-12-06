package com.softteco.billingservice.web;

import com.softteco.billingservice.service.PaymentService;
import com.softteco.billingservice.web.dto.CreatePaymentRequest;
import com.softteco.billingservice.web.dto.PaymentResponse;
import com.softteco.billingservice.web.dto.RefundPaymentRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        return paymentService.processPayment(request);
    }

    @GetMapping("/{id}")
    public PaymentResponse getPayment(@PathVariable String id) {
        return paymentService.getPayment(id);
    }

    @GetMapping
    public List<PaymentResponse> getAllPayments() {
        return paymentService.getAllPayments();
    }

    @PostMapping("/refunds")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse refundPayment(@Valid @RequestBody RefundPaymentRequest request) {
        return paymentService.refundPayment(request);
    }
}
