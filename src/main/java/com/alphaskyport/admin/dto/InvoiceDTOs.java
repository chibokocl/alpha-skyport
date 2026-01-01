package com.alphaskyport.admin.dto;

import com.alphaskyport.admin.model.InvoiceStatus;
import com.alphaskyport.admin.model.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class InvoiceDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateInvoiceRequest {
        @NotNull(message = "User ID is required")
        private UUID userId;

        private UUID shipmentId;

        @NotNull(message = "Issue date is required")
        private LocalDate issueDate;

        @NotNull(message = "Due date is required")
        private LocalDate dueDate;

        private String currency;
        private String notes;

        @NotEmpty(message = "At least one line item is required")
        private List<LineItemRequest> lineItems;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineItemRequest {
        @NotBlank(message = "Description is required")
        private String description;

        @Positive(message = "Quantity must be positive")
        private BigDecimal quantity;

        @NotNull(message = "Unit price is required")
        @PositiveOrZero(message = "Unit price must be positive or zero")
        private BigDecimal unitPrice;

        @PositiveOrZero(message = "Tax rate must be positive or zero")
        private BigDecimal taxRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateInvoiceRequest {
        private LocalDate dueDate;
        private String notes;
        private List<LineItemRequest> lineItems;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceResponse {
        private UUID invoiceId;
        private String invoiceNumber;
        private UUID userId;
        private String customerName;
        private String customerEmail;
        private UUID shipmentId;
        private String trackingNumber;
        private InvoiceStatus status;
        private BigDecimal subtotal;
        private BigDecimal taxAmount;
        private BigDecimal totalAmount;
        private BigDecimal paidAmount;
        private BigDecimal outstandingAmount;
        private String currency;
        private LocalDate issueDate;
        private LocalDate dueDate;
        private LocalDate paidDate;
        private boolean overdue;
        private String notes;
        private LocalDateTime createdAt;
        private List<LineItemResponse> lineItems;
        private List<PaymentSummary> payments;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineItemResponse {
        private UUID lineItemId;
        private String description;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal taxRate;
        private BigDecimal lineTotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentSummary {
        private UUID paymentId;
        private BigDecimal amount;
        private PaymentMethod method;
        private LocalDate paymentDate;
        private String reference;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecordPaymentRequest {
        @NotNull(message = "Invoice ID is required")
        private UUID invoiceId;

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        private BigDecimal amount;

        @NotNull(message = "Payment method is required")
        private PaymentMethod paymentMethod;

        private String paymentReference;

        @NotNull(message = "Payment date is required")
        private LocalDate paymentDate;

        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceListResponse {
        private List<InvoiceSummary> invoices;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceSummary {
        private UUID invoiceId;
        private String invoiceNumber;
        private String customerName;
        private InvoiceStatus status;
        private BigDecimal totalAmount;
        private BigDecimal outstandingAmount;
        private LocalDate issueDate;
        private LocalDate dueDate;
        private boolean overdue;
    }
}
