package com.alphaskyport.admin.controller;

import com.alphaskyport.admin.model.AdminUser;
import com.alphaskyport.admin.service.AdminQuoteService;
import com.alphaskyport.admin.security.CurrentAdmin;
import com.alphaskyport.admin.security.RequiresPermission;
import com.alphaskyport.logistics.model.Quote;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/quotes")
@RequiredArgsConstructor
@Tag(name = "Admin Quotes", description = "Quote management endpoints")
public class AdminQuoteController {

    private final AdminQuoteService quoteService;

    @GetMapping
    @RequiresPermission("quotes:read")
    @Operation(summary = "List quotes", description = "Get paginated list of quotes")
    public ResponseEntity<Page<Quote>> getQuotes(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        if (status != null) {
            return ResponseEntity.ok(quoteService.getQuotesByStatus(status, pageable));
        }
        return ResponseEntity.ok(quoteService.getAllQuotes(pageable));
    }

    @GetMapping("/{quoteId}")
    @RequiresPermission("quotes:read")
    @Operation(summary = "Get quote", description = "Get quote details by ID")
    public ResponseEntity<Quote> getQuote(@PathVariable UUID quoteId) {
        return ResponseEntity.ok(quoteService.getQuoteById(quoteId));
    }

    @GetMapping("/pending")
    @RequiresPermission("quotes:read")
    @Operation(summary = "Get pending quotes", description = "Get all pending quotes")
    public ResponseEntity<List<Quote>> getPendingQuotes() {
        return ResponseEntity.ok(quoteService.getPendingQuotes());
    }

    @GetMapping("/expiring")
    @RequiresPermission("quotes:read")
    @Operation(summary = "Get expiring quotes", description = "Get quotes expiring soon")
    public ResponseEntity<List<Quote>> getExpiringQuotes(
            @RequestParam(defaultValue = "48") int hours) {
        return ResponseEntity.ok(quoteService.getExpiringQuotes(hours));
    }

    @GetMapping("/stats")
    @RequiresPermission("quotes:read")
    @Operation(summary = "Get quote stats", description = "Get quote statistics")
    public ResponseEntity<Map<String, Object>> getQuoteStats() {
        return ResponseEntity.ok(quoteService.getQuoteStats());
    }

    @PostMapping("/{quoteId}/adjust-price")
    @RequiresPermission("quotes:write")
    @Operation(summary = "Adjust price", description = "Adjust quote price")
    public ResponseEntity<Quote> adjustPrice(
            @PathVariable UUID quoteId,
            @RequestParam BigDecimal newAmount,
            @RequestParam String reason,
            @RequestParam(defaultValue = "correction") String adjustmentType,
            @CurrentAdmin AdminUser admin) {
        return ResponseEntity.ok(quoteService.adjustPrice(quoteId, newAmount, reason, adjustmentType, admin));
    }

    @PostMapping("/{quoteId}/approve")
    @RequiresPermission("quotes:approve")
    @Operation(summary = "Approve quote", description = "Approve a pending quote")
    public ResponseEntity<Quote> approveQuote(
            @PathVariable UUID quoteId,
            @CurrentAdmin AdminUser admin) {
        return ResponseEntity.ok(quoteService.approveQuote(quoteId, admin));
    }

    @PostMapping("/{quoteId}/reject")
    @RequiresPermission("quotes:approve")
    @Operation(summary = "Reject quote", description = "Reject a pending quote")
    public ResponseEntity<Quote> rejectQuote(
            @PathVariable UUID quoteId,
            @RequestParam String reason,
            @CurrentAdmin AdminUser admin) {
        return ResponseEntity.ok(quoteService.rejectQuote(quoteId, reason, admin));
    }

    @PostMapping("/{quoteId}/extend")
    @RequiresPermission("quotes:write")
    @Operation(summary = "Extend validity", description = "Extend quote validity period")
    public ResponseEntity<Quote> extendValidity(
            @PathVariable UUID quoteId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime newValidUntil,
            @CurrentAdmin AdminUser admin) {
        return ResponseEntity.ok(quoteService.extendValidity(quoteId, newValidUntil, admin));
    }

    @PostMapping("/{quoteId}/assign")
    @RequiresPermission("quotes:write")
    @Operation(summary = "Assign quote", description = "Assign quote to an admin")
    public ResponseEntity<Void> assignQuote(
            @PathVariable UUID quoteId,
            @RequestParam UUID assignTo,
            @CurrentAdmin AdminUser admin) {
        quoteService.assignQuote(quoteId, assignTo, admin);
        return ResponseEntity.ok().build();
    }
}
