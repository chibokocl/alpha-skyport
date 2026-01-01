package com.alphaskyport.admin.repository;

import com.alphaskyport.admin.model.Invoice;
import com.alphaskyport.admin.model.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID>, JpaSpecificationExecutor<Invoice> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    List<Invoice> findByUser_UserId(UUID userId);

    Page<Invoice> findByUser_UserId(UUID userId, Pageable pageable);

    List<Invoice> findByStatus(InvoiceStatus status);

    Page<Invoice> findByStatus(InvoiceStatus status, Pageable pageable);

    @Query("SELECT i FROM Invoice i WHERE i.status NOT IN ('PAID', 'CANCELLED', 'REFUNDED') AND i.dueDate < :today")
    List<Invoice> findOverdueInvoices(@Param("today") LocalDate today);

    @Query("SELECT i FROM Invoice i WHERE i.shipment.shipmentId = :shipmentId")
    List<Invoice> findByShipmentId(@Param("shipmentId") UUID shipmentId);

    @Query("SELECT SUM(i.totalAmount) FROM Invoice i WHERE i.status = 'PAID' AND i.paidDate BETWEEN :startDate AND :endDate")
    BigDecimal sumPaidAmountBetweenDates(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(i.totalAmount - i.paidAmount) FROM Invoice i WHERE i.status NOT IN ('PAID', 'CANCELLED', 'REFUNDED')")
    BigDecimal sumOutstandingAmount();

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.status = :status")
    long countByStatus(@Param("status") InvoiceStatus status);

    @Query(value = "SELECT generate_invoice_number()", nativeQuery = true)
    String generateInvoiceNumber();

    @Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.lineItems WHERE i.invoiceId = :id")
    Optional<Invoice> findByIdWithLineItems(@Param("id") UUID id);

    @Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.payments WHERE i.invoiceId = :id")
    Optional<Invoice> findByIdWithPayments(@Param("id") UUID id);
}
