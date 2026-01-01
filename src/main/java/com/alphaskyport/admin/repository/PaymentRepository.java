package com.alphaskyport.admin.repository;

import com.alphaskyport.admin.model.Payment;
import com.alphaskyport.admin.model.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByInvoice_InvoiceId(UUID invoiceId);

    Page<Payment> findByPaymentDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);

    List<Payment> findByPaymentMethod(PaymentMethod method);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.paymentDate BETWEEN :startDate AND :endDate AND p.status = 'COMPLETED'")
    BigDecimal sumCompletedPaymentsBetweenDates(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT p.paymentMethod, SUM(p.amount), COUNT(p) FROM Payment p " +
           "WHERE p.paymentDate BETWEEN :startDate AND :endDate AND p.status = 'COMPLETED' " +
           "GROUP BY p.paymentMethod")
    List<Object[]> sumByPaymentMethodBetweenDates(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.invoice.invoiceId = :invoiceId AND p.status = 'COMPLETED'")
    BigDecimal sumCompletedPaymentsForInvoice(@Param("invoiceId") UUID invoiceId);
}
