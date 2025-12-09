package com.alphaskyport.logistics.service;

import com.alphaskyport.logistics.model.PaymentTransaction;
import com.alphaskyport.logistics.model.Shipment;
import com.alphaskyport.logistics.repository.PaymentTransactionRepository;
import com.alphaskyport.logistics.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ShipmentRepository shipmentRepository;

    @Transactional
    public PaymentTransaction processPayment(UUID shipmentId, BigDecimal amount, String currency, String paymentMethod,
            String idempotencyKey) {
        // 1. Idempotency Check
        Optional<PaymentTransaction> existingTransaction = paymentTransactionRepository
                .findByIdempotencyKey(idempotencyKey);
        if (existingTransaction.isPresent()) {
            return existingTransaction.get();
        }

        // 2. Lock Shipment
        Shipment shipment = shipmentRepository.findByIdWithLock(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found"));

        // 3. Create Transaction
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setShipment(shipment);
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setAmount(amount);
        transaction.setCurrency(currency);
        transaction.setTransactionType("payment");
        transaction.setPaymentMethod(paymentMethod);
        transaction.setTransactionStatus("completed"); // Simulating instant success
        transaction.setProcessedAt(LocalDateTime.now());

        paymentTransactionRepository.save(transaction);

        // 4. Update Shipment Financials
        BigDecimal newAmountPaid = shipment.getAmountPaid().add(amount);
        shipment.setAmountPaid(newAmountPaid);

        BigDecimal newAmountDue = shipment.getTotalCost().subtract(newAmountPaid);
        // Ensure due is not negative (overpayment) - business rule?
        // For now, allow negative to indicate credit, or clamp to zero?
        // Schema constraint: amount_due >= 0. So we must clamp or throw.
        if (newAmountDue.compareTo(BigDecimal.ZERO) < 0) {
            // throw new RuntimeException("Overpayment not allowed");
            // Or just set to zero and maybe handle refund later?
            // Let's stick to strict schema.
            throw new RuntimeException("Payment amount exceeds amount due");
        }
        shipment.setAmountDue(newAmountDue);

        if (newAmountDue.compareTo(BigDecimal.ZERO) == 0) {
            shipment.setPaymentStatus("paid");
        } else {
            shipment.setPaymentStatus("partial");
        }

        shipmentRepository.save(shipment);

        return transaction;
    }
}
