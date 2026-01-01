package com.alphaskyport.admin.controller;

import com.alphaskyport.admin.dto.InvoiceDTOs.InvoiceListResponse;
import com.alphaskyport.admin.dto.InvoiceDTOs.InvoiceResponse;
import com.alphaskyport.admin.dto.InvoiceDTOs.CreateInvoiceRequest;
import com.alphaskyport.admin.dto.InvoiceDTOs.RecordPaymentRequest;

import com.alphaskyport.admin.model.AdminUser;
import com.alphaskyport.admin.model.InvoiceStatus;
import com.alphaskyport.admin.service.InvoiceService;
import com.alphaskyport.admin.security.CurrentAdmin;
import com.alphaskyport.admin.security.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/invoices")
@RequiredArgsConstructor
@Tag(name = "Admin Invoices", description = "Invoice management endpoints")
public class AdminInvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    @RequiresPermission("invoices:read")
    @Operation(summary = "List invoices", description = "Get paginated list of invoices")
    public ResponseEntity<InvoiceListResponse> getInvoices(
            @RequestParam(required = false) InvoiceStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(invoiceService.getInvoices(status, pageable));
    }

    @GetMapping("/{invoiceId}")
    @RequiresPermission("invoices:read")
    @Operation(summary = "Get invoice", description = "Get invoice details by ID")
    public ResponseEntity<InvoiceResponse> getInvoice(@PathVariable UUID invoiceId) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(invoiceId));
    }

    @GetMapping("/number/{invoiceNumber}")
    @RequiresPermission("invoices:read")
    @Operation(summary = "Get invoice by number", description = "Get invoice details by invoice number")
    public ResponseEntity<InvoiceResponse> getInvoiceByNumber(@PathVariable String invoiceNumber) {
        return ResponseEntity.ok(invoiceService.getInvoiceByNumber(invoiceNumber));
    }

    @GetMapping("/user/{userId}")
    @RequiresPermission("invoices:read")
    @Operation(summary = "Get user invoices", description = "Get invoices for a specific user")
    public ResponseEntity<InvoiceListResponse> getUserInvoices(
            @PathVariable UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(invoiceService.getInvoicesByUser(userId, pageable));
    }

    @GetMapping("/overdue")
    @RequiresPermission("invoices:read")
    @Operation(summary = "Get overdue invoices", description = "Get all overdue invoices")
    public ResponseEntity<List<InvoiceResponse>> getOverdueInvoices() {
        return ResponseEntity.ok(invoiceService.getOverdueInvoices());
    }

    @GetMapping("/outstanding-total")
    @RequiresPermission("invoices:read")
    @Operation(summary = "Get total outstanding", description = "Get total outstanding receivables")
    public ResponseEntity<BigDecimal> getTotalOutstanding() {
        return ResponseEntity.ok(invoiceService.getTotalOutstanding());
    }

    @PostMapping
    @RequiresPermission("invoices:write")
    @Operation(summary = "Create invoice", description = "Create a new invoice")
    public ResponseEntity<InvoiceResponse> createInvoice(
            @Valid @RequestBody CreateInvoiceRequest request,
            @CurrentAdmin AdminUser admin) {
        return ResponseEntity.ok(invoiceService.createInvoice(request, admin));
    }

    @PostMapping("/{invoiceId}/send")
    @RequiresPermission("invoices:write")
    @Operation(summary = "Send invoice", description = "Send invoice to customer")
    public ResponseEntity<InvoiceResponse> sendInvoice(
            @PathVariable UUID invoiceId,
            @CurrentAdmin AdminUser admin) {
        return ResponseEntity.ok(invoiceService.sendInvoice(invoiceId, admin));
    }

    @PostMapping("/payments")
    @RequiresPermission("payments:write")
    @Operation(summary = "Record payment", description = "Record a payment against an invoice")
    public ResponseEntity<InvoiceResponse> recordPayment(
            @Valid @RequestBody RecordPaymentRequest request,
            @CurrentAdmin AdminUser admin) {
        return ResponseEntity.ok(invoiceService.recordPayment(request, admin));
    }

    @PostMapping("/{invoiceId}/cancel")
    @RequiresPermission("invoices:void")
    @Operation(summary = "Cancel invoice", description = "Cancel an invoice")
    public ResponseEntity<InvoiceResponse> cancelInvoice(
            @PathVariable UUID invoiceId,
            @RequestParam String reason,
            @CurrentAdmin AdminUser admin) {
        return ResponseEntity.ok(invoiceService.cancelInvoice(invoiceId, reason, admin));
    }
}
