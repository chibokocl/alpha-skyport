package com.alphaskyport.logistics.controller;

import com.alphaskyport.logistics.model.PaymentTransaction;
import com.alphaskyport.logistics.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment processing APIs")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/process")
    @Operation(summary = "Process payment", description = "Process a payment for a shipment with idempotency support")
    public ResponseEntity<PaymentTransaction> processPayment(
            @RequestParam UUID shipmentId,
            @RequestParam BigDecimal amount,
            @RequestParam String currency,
            @RequestParam String paymentMethod,
            @RequestParam String idempotencyKey) {

        PaymentTransaction transaction = paymentService.processPayment(
                shipmentId, amount, currency, paymentMethod, idempotencyKey);

        return ResponseEntity.ok(transaction);
    }
}
