package com.alphaskyport.admin.controller;

import com.alphaskyport.admin.model.AdminUser;
import com.alphaskyport.admin.service.AdminShipmentService;
import com.alphaskyport.admin.security.CurrentAdmin;
import com.alphaskyport.admin.security.RequiresPermission;
import com.alphaskyport.logistics.model.Shipment;
import com.alphaskyport.logistics.model.ShipmentTrackingEvent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/shipments")
@RequiredArgsConstructor
@Tag(name = "Admin Shipments", description = "Shipment management endpoints")
public class AdminShipmentController {

    private final AdminShipmentService shipmentService;

    @GetMapping
    @RequiresPermission("shipments:read")
    @Operation(summary = "List shipments", description = "Get paginated list of shipments")
    public ResponseEntity<Page<Shipment>> getShipments(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        if (status != null) {
            return ResponseEntity.ok(shipmentService.getShipmentsByStatus(status, pageable));
        }
        return ResponseEntity.ok(shipmentService.getAllShipments(pageable));
    }

    @GetMapping("/{shipmentId}")
    @RequiresPermission("shipments:read")
    @Operation(summary = "Get shipment", description = "Get shipment details by ID")
    public ResponseEntity<Shipment> getShipment(@PathVariable UUID shipmentId) {
        return ResponseEntity.ok(shipmentService.getShipmentById(shipmentId));
    }

    @GetMapping("/tracking/{trackingNumber}")
    @RequiresPermission("shipments:read")
    @Operation(summary = "Get by tracking number", description = "Get shipment by tracking number")
    public ResponseEntity<Shipment> getShipmentByTrackingNumber(@PathVariable String trackingNumber) {
        return ResponseEntity.ok(shipmentService.getShipmentByTrackingNumber(trackingNumber));
    }

    @GetMapping("/active")
    @RequiresPermission("shipments:read")
    @Operation(summary = "Get active shipments", description = "Get all active shipments")
    public ResponseEntity<List<Shipment>> getActiveShipments() {
        return ResponseEntity.ok(shipmentService.getActiveShipments());
    }

    @GetMapping("/today/pickups")
    @RequiresPermission("shipments:read")
    @Operation(summary = "Get today's pickups", description = "Get shipments scheduled for pickup today")
    public ResponseEntity<List<Shipment>> getTodaysPickups() {
        return ResponseEntity.ok(shipmentService.getTodaysPickups());
    }

    @GetMapping("/today/deliveries")
    @RequiresPermission("shipments:read")
    @Operation(summary = "Get today's deliveries", description = "Get shipments expected for delivery today")
    public ResponseEntity<List<Shipment>> getTodaysDeliveries() {
        return ResponseEntity.ok(shipmentService.getTodaysDeliveries());
    }

    @GetMapping("/delayed")
    @RequiresPermission("shipments:read")
    @Operation(summary = "Get delayed shipments", description = "Get shipments that are delayed")
    public ResponseEntity<List<Shipment>> getDelayedShipments() {
        return ResponseEntity.ok(shipmentService.getDelayedShipments());
    }

    @GetMapping("/{shipmentId}/tracking")
    @RequiresPermission("shipments:read")
    @Operation(summary = "Get tracking history", description = "Get shipment tracking events")
    public ResponseEntity<List<ShipmentTrackingEvent>> getTrackingHistory(@PathVariable UUID shipmentId) {
        return ResponseEntity.ok(shipmentService.getTrackingHistory(shipmentId));
    }

    @GetMapping("/stats")
    @RequiresPermission("shipments:read")
    @Operation(summary = "Get shipment stats", description = "Get shipment statistics")
    public ResponseEntity<Map<String, Object>> getShipmentStats() {
        return ResponseEntity.ok(shipmentService.getShipmentStats());
    }

    @GetMapping("/status-counts")
    @RequiresPermission("shipments:read")
    @Operation(summary = "Get status counts", description = "Get shipment counts by status")
    public ResponseEntity<Map<String, Long>> getStatusCounts() {
        return ResponseEntity.ok(shipmentService.getShipmentsByStatusCount());
    }

    @PutMapping("/{shipmentId}/status")
    @RequiresPermission("shipments:write")
    @Operation(summary = "Update status", description = "Update shipment status")
    public ResponseEntity<Shipment> updateStatus(
            @PathVariable UUID shipmentId,
            @RequestParam String newStatus,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "true") boolean notifyCustomer,
            @CurrentAdmin AdminUser admin) {
        return ResponseEntity.ok(shipmentService.updateStatus(
                shipmentId, newStatus, location, description, notifyCustomer, admin));
    }

    @PostMapping("/{shipmentId}/tracking-event")
    @RequiresPermission("shipments:write")
    @Operation(summary = "Add tracking event", description = "Add a manual tracking event")
    public ResponseEntity<Void> addTrackingEvent(
            @PathVariable UUID shipmentId,
            @RequestParam String status,
            @RequestParam(required = false) String location,
            @RequestParam String description,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime eventTime,
            @CurrentAdmin AdminUser admin) {
        shipmentService.addTrackingEvent(shipmentId, status, location, description, eventTime, admin);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{shipmentId}/expected-delivery")
    @RequiresPermission("shipments:write")
    @Operation(summary = "Update expected delivery", description = "Update expected delivery date")
    public ResponseEntity<Shipment> updateExpectedDelivery(
            @PathVariable UUID shipmentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newDate,
            @RequestParam String reason,
            @CurrentAdmin AdminUser admin) {
        return ResponseEntity.ok(shipmentService.updateExpectedDelivery(shipmentId, newDate, reason, admin));
    }

    @PostMapping("/bulk/status")
    @RequiresPermission("shipments:write")
    @Operation(summary = "Bulk update status", description = "Update status for multiple shipments")
    public ResponseEntity<List<Shipment>> bulkUpdateStatus(
            @RequestBody List<UUID> shipmentIds,
            @RequestParam String newStatus,
            @RequestParam(required = false) String description,
            @CurrentAdmin AdminUser admin) {
        return ResponseEntity.ok(shipmentService.bulkUpdateStatus(shipmentIds, newStatus, description, admin));
    }
}
